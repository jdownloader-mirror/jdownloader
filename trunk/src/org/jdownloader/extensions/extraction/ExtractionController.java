//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.jdownloader.extensions.extraction;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import jd.config.SubConfiguration;
import jd.controlling.DownloadWatchDog;
import jd.controlling.IOEQ;
import jd.controlling.JDLogger;
import jd.controlling.PasswordListController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.extensions.extraction.translate.T;

/**
 * Responsible for the coorect procedure of the extraction process. Contains one
 * IExtraction instance.
 * 
 * @author botzi
 * 
 */
public class ExtractionController extends QueueAction<Void, RuntimeException> {
    private ArrayList<ExtractionListener> listener         = new ArrayList<ExtractionListener>();
    private ArrayList<String>             passwordList;
    private int                           passwordListSize = 0;
    private SubConfiguration              config           = null;
    private Exception                     exception;
    private boolean                       removeAfterExtraction;
    private Archive                       archive;
    private IExtraction                   extractor;
    private Logger                        logger;
    private ScheduledFuture<?>            timer;

    ExtractionController(Archive archiv, Logger logger) {
        this.archive = archiv;

        extractor = archive.getExtractor();
        extractor.setArchiv(archiv);
        extractor.setExtractionController(this);

        config = SubConfiguration.getConfig(T._.plugins_optional_extraction_name());

        this.logger = logger;
        extractor.setLogger(logger);
        passwordList = new ArrayList<String>();
    }

    /**
     * Adds an listener to the current unpack process.
     * 
     * @param listener
     */
    void addExtractionListener(ExtractionListener listener) {
        this.removeExtractionListener(listener);
        this.listener.add(listener);
    }

    /**
     * Removes an listener from the current unpack process.
     * 
     * @param listener
     */
    private void removeExtractionListener(ExtractionListener listener) {
        this.listener.remove(listener);
    }

    /**
     * Checks if the extracted file(s) has enough space. Only works with Java 6
     * or higher.
     * 
     * @return True if it's enough space.
     */
    private boolean checkSize() {
        return DownloadWatchDog.getInstance().checkFreeDiskSpace(archive.getExtractTo(), archive.getSize());
    }

    private boolean checkPassword(String pw) {
        if (pw == null || pw.equals("")) return false;

        return extractor.findPassword(pw);
    }

    @Override
    public Void run() {
        try {
            fireEvent(ExtractionConstants.WRAPPER_START_OPEN_ARCHIVE);
            logger.info("Start unpacking of " + archive.getFirstDownloadLink().getFileOutput());

            for (DownloadLink l : archive.getDownloadLinks()) {
                if (!new File(l.getFileOutput()).exists()) {
                    logger.info("Could not find archive file " + l.getFileOutput());
                    archive.addCrcError(l);
                }
            }
            if (archive.getCrcError().size() > 0) {
                fireEvent(ExtractionConstants.WRAPPER_FILE_NOT_FOUND);
                return null;
            }

            File dl = ExtractionExtension.getIntance().getExtractToPath(archive.getFirstDownloadLink());
            archive.setExtractTo(dl);

            for (DownloadLink link1 : archive.getDownloadLinks()) {
                link1.setProperty(ExtractionConstants.DOWNLOADLINK_KEY_EXTRACTEDPATH, dl.getAbsolutePath());
            }

            if (extractor.prepare()) {
                if (!checkSize()) {
                    fireEvent(ExtractionConstants.NOT_ENOUGH_SPACE);
                    logger.info("Not enough harddisk space for unpacking archive " + archive.getFirstDownloadLink().getFileOutput());
                    extractor.close();
                    return null;
                }

                if (archive.isProtected() && archive.getPassword().equals("")) {
                    String dlpw = archive.getFirstDownloadLink().getFilePackage().getPassword();
                    if (dlpw != null) passwordList.add(dlpw);
                    passwordList.addAll(FilePackage.getPasswordAuto(archive.getFirstDownloadLink().getFilePackage()));
                    dlpw = archive.getFirstDownloadLink().getStringProperty("pass", null);
                    if (dlpw != null) passwordList.add(dlpw);
                    // passwordList.addAll(PasswordListController.getInstance().getPasswordList());
                    // passwordList.add(extractor.getArchiveName(archive.getFirstDownloadLink()));
                    // passwordList.add(new
                    // File(archive.getFirstDownloadLink().getFileOutput()).getName());
                    passwordListSize = passwordList.size() + PasswordListController.getInstance().getPasswordList().size() + 2;

                    fireEvent(ExtractionConstants.WRAPPER_CRACK_PASSWORD);
                    logger.info("Start password finding for " + archive.getFirstDownloadLink().getFileOutput());

                    for (String password : passwordList) {
                        if (checkPassword(password)) {
                            break;
                        }
                    }

                    if (archive.getPassword().equals("")) {
                        for (String password : PasswordListController.getInstance().getPasswordList()) {
                            if (checkPassword(password)) {
                                break;
                            }
                        }

                        if (archive.getPassword().equals("")) {
                            passwordList.clear();
                            passwordList.add(extractor.getArchiveName(archive.getFirstDownloadLink()));
                            passwordList.add(new File(archive.getFirstDownloadLink().getFileOutput()).getName());

                            for (String password : passwordList) {
                                if (checkPassword(password)) {
                                    break;
                                }
                            }
                        }
                    }

                    if (archive.getPassword().equals("")) {
                        fireEvent(ExtractionConstants.WRAPPER_PASSWORD_NEEDED_TO_CONTINUE);
                        logger.info("Found no password in passwordlist " + archive.getFirstDownloadLink().getFileOutput());

                        if (!extractor.findPassword(archive.getPassword())) {
                            fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                            logger.info("No password found for " + archive.getFirstDownloadLink().getFileOutput());
                            extractor.close();
                            return null;
                        }
                        PasswordListController.getInstance().addPassword(archive.getPassword(), true);
                    }

                    fireEvent(ExtractionConstants.WRAPPER_PASSWORD_FOUND);
                    logger.info("Found password for " + archive.getFirstDownloadLink().getFileOutput());
                }
                fireEvent(ExtractionConstants.WRAPPER_OPEN_ARCHIVE_SUCCESS);

                if (!archive.getExtractTo().exists()) {
                    if (!archive.getExtractTo().mkdirs()) {
                        JDLogger.getLogger().warning("Could not create subpath");
                        fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                    }
                }

                logger.info("Execute unpacking of " + archive.getFirstDownloadLink().getFileOutput());

                timer = IOEQ.TIMINGQUEUE.scheduleWithFixedDelay(new Runnable() {

                    public void run() {
                        fireEvent(ExtractionConstants.WRAPPER_ON_PROGRESS);
                    }

                }, 1, 2, TimeUnit.SECONDS);
                try {
                    extractor.extract();
                } finally {
                    timer.cancel(false);
                }
                extractor.close();
                switch (archive.getExitCode()) {
                case ExtractionControllerConstants.EXIT_CODE_SUCCESS:
                    logger.info("Unpacking successful for " + archive.getFirstDownloadLink().getFileOutput());
                    if (!archive.getGotInterrupted() && removeAfterExtraction) {
                        removeArchiveFiles();
                    }
                    fireEvent(ExtractionConstants.WRAPPER_FINISHED_SUCCESSFUL);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_CRC_ERROR:
                    JDLogger.getLogger().warning("A CRC error occurred when unpacking " + archive.getFirstDownloadLink().getFileOutput());
                    fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED_CRC);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_USER_BREAK:
                    JDLogger.getLogger().info("User interrupted unpacking of " + archive.getFirstDownloadLink().getFileOutput());
                    fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR:
                    JDLogger.getLogger().warning("Could not create Outputfile for" + archive.getFirstDownloadLink().getFileOutput());
                    fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_WRITE_ERROR:
                    JDLogger.getLogger().warning("Unable to write unpacked data on harddisk for " + archive.getFirstDownloadLink().getFileOutput());
                    this.exception = new ExtractionException("Write to disk error");
                    fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR:
                    JDLogger.getLogger().warning("A unknown fatal error occurred while unpacking " + archive.getFirstDownloadLink().getFileOutput());
                    fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_WARNING:
                    JDLogger.getLogger().warning("Non fatal error(s) occurred while unpacking " + archive.getFirstDownloadLink().getFileOutput());
                    fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                default:
                    fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                }
                return null;
            } else {
                extractor.close();
                fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
            }
        } catch (Exception e) {
            extractor.close();
            this.exception = e;
            JDLogger.exception(e);
            fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
        } finally {
            fireEvent(ExtractionConstants.REMOVE_ARCHIVE_METADATA);
        }
        return null;
    }

    /**
     * Deletes the archive files.
     */
    private void removeArchiveFiles() {
        for (DownloadLink link : archive.getDownloadLinks()) {
            if (!new File(link.getFileOutput()).delete()) {
                JDLogger.getLogger().warning("Could not delete archive: " + link.getFileOutput());
            }
        }
    }

    /**
     * Returns a thrown exception.
     * 
     * @return The thrown exception.
     */
    Exception getException() {
        return exception;
    }

    /**
     * 
     * Returns the current password finding process.
     * 
     * @return
     */
    int getCrackProgress() {
        return extractor.getCrackProgress();
    }

    /**
     * Fires an event.
     * 
     * @param status
     */
    public void fireEvent(int status) {
        for (ExtractionListener listener : this.listener) {
            listener.onExtractionEvent(status, this);
        }
    }

    /**
     * Gets the passwordlist size
     * 
     * @return
     */
    public int getPasswordListSize() {
        return passwordListSize;
    }

    /**
     * Should the archives be deleted after extracting.
     * 
     * @param setProperty
     */
    void setRemoveAfterExtract(boolean setProperty) {
        this.removeAfterExtraction = setProperty;
    }

    /**
     * Starts the extracting progress.
     */
    public void go() throws Exception {
        run();
    }

    /**
     * Returns the {@link Archive}.
     * 
     * @return
     */
    public Archive getArchiv() {
        return archive;
    }

    /**
     * Returns the Configuration.
     * 
     * @return
     */
    public SubConfiguration getConfig() {
        return config;
    }

    /**
     * Sets a exeption that occurs during unpacking.
     * 
     * @param e
     */
    public void setExeption(Exception e) {
        exception = e;
    }

}