//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.

package jd.plugins;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Download {
    public static final int           STATUS_INITIALIZED                     = 0;

    // Errorids unter 100 sind für DownloadLink reserviert
    public static final int           ERROR_OUTPUTFILE_OWNED_BY_ANOTHER_LINK = DownloadLink.STATUS_ERROR_OUTPUTFILE_OWNED_BY_ANOTHER_LINK;

    public static final int           ERROR_OUTPUTFILE_INVALID               = 100;

    public static final int           ERROR_OUTPUTFILE_ALREADYEXISTS         = DownloadLink.STATUS_ERROR_ALREADYEXISTS;

    public static final int           ERROR_CHUNK_INCOMPLETE                 = DownloadLink.STATUS_DOWNLOAD_INCOMPLETE;

    public static final int           ERROR_FILE_NOT_FOUND                   = DownloadLink.STATUS_ERROR_FILE_NOT_FOUND;

    public static final int           ERROR_SECURITY                         = DownloadLink.STATUS_ERROR_SECURITY;

    public static final int           ERROR_UNKNOWN                          = DownloadLink.STATUS_ERROR_UNKNOWN;

    public static final int           ERROR_COULD_NOT_RENAME                 = 101;

    public static final int           ERROR_ABORTED_BY_USER                  = 102;

    public static final int           ERROR_TOO_MUCH_BUFFERMEMORY            = 103;

    public static final int           ERROR_CHUNKLOAD_FAILED                 = DownloadLink.STATUS_ERROR_CHUNKLOAD_FAILED;

    public static final int           ERROR_NO_CONNECTION                    = 104;

    public static final int           ERROR_TIMEOUT_REACHED                  = 105;

    private static final int          ERROR_LOCAL_IO                         = 106;

    private static final int ERROR_NIBBLE_LIMIT_REACHED = 107;

    private DownloadLink              downloadLink;

    private HTTPConnection            connection;

    private int                       status                                 = STATUS_INITIALIZED;

    private int                       chunksDownloading                      = 0;

    private int                       chunkNum                               = 1;

    private int                       readTimeout                            = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_READ_TIMEOUT, 10000);

    private int                       requestTimeout                         = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_CONNECT_TIMEOUT, 10000);

    private int                       chunksInProgress                       = 0;

    private Vector<Integer>           errors                                 = new Vector<Integer>();

    private Vector<Chunk>             chunks                                 = new Vector<Chunk>();

    private boolean                   resume                                 = false;

    private boolean                   speedLimited                           = true;

    private PluginForHost                    plugin;

    private int                       bytesLoaded                            = 0;

    private volatile RandomAccessFile outputFile;

    private FileChannel               outputChannel;

    private int                       maxBytes                               = -1;

    private long                      fileSize                               = -1;

    private boolean                   abortByError                           = false;

    private int                       preBytes                               = 0;

    private long                      writeTimer                             = System.currentTimeMillis();

    private long                      writeCount                             = 0;

    private long                      hdWritesPerSecond;

    private MappedByteBuffer          wrBuf;

    private long                      debugtimer;
    private boolean speedDebug=true;
    public static Logger              logger                                 = JDUtilities.getLogger();

    public Download(PluginForHost plugin, DownloadLink downloadLink, HTTPConnection urlConnection) {
        this.downloadLink = downloadLink;
        downloadLink.setDownloadInstance(this);
        this.connection = urlConnection;
        this.plugin = plugin;
    }

    /**
     * Gibt die Anzahl der Chunks an die dieser Download verwenden soll. Chu8nks
     * können nur vor dem Downloadstart gesetzt werden!
     * 
     * @param num
     */
    public void setChunks(int num) {
        if (status != STATUS_INITIALIZED) {
            logger.severe("CHunks musst be set before starting download");
            return;
        }
        if (num <= 0) {
            logger.severe("Chunks value must be >=1");
            return;
        }
        this.chunkNum = num;
    }

    /**
     * Schreibt den puffer eines chunks in die zugehörige Datei
     * 
     * @param buffer
     * @param currentBytePosition
     */
    // private synchronized void addBytes(Chunk chunk, int s) {
    // try {
    // if ((System.currentTimeMillis() - writeTimer) >= 1000) {
    // this.hdWritesPerSecond = writeCount / 1;
    // writeTimer = System.currentTimeMillis();
    // writeCount = 0;
    // logger.info("HD ZUgriffe: " + hdWritesPerSecond);
    // }
    // this.writeCount++;
    // // currentBytePosition=-1;
    // this.outputFile.seek(chunk.currentBytePosition);
    //
    // int length = chunk.buffer.limit() - chunk.buffer.position();
    // // byte[] tmp = new byte[length];
    // // logger.info("wrote " + length + " at " + currentBytePosition);
    // // buffer.get(tmp, buffer.position(), length);
    // outputChannel.write(chunk.buffer);
    // // this.outputFile.write(tmp);
    // }
    // catch (Exception e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // error(ERROR_LOCAL_IO);
    // }
    //
    // }
    private synchronized void addBytes(Chunk chunk) {
        try {
            if(speedDebug){
            if ((System.currentTimeMillis() - writeTimer) >= 1000) {
                this.hdWritesPerSecond = writeCount / 1;
                writeTimer = System.currentTimeMillis();
                writeCount = 0;
                logger.info("HD ZUgriffe: " + hdWritesPerSecond);
            }
            this.writeCount++;
            }
            wrBuf.position((int) chunk.currentBytePosition);
            wrBuf.put(chunk.buffer);
            if (maxBytes > 0 && (chunk.currentBytePosition + chunk.buffer.capacity()) > maxBytes) {
                error(ERROR_NIBBLE_LIMIT_REACHED);
            }
            if (chunk.getID() >= 0) downloadLink.getChunksProgress()[chunk.getID()] = (int) chunk.currentBytePosition + chunk.buffer.capacity();

        }
        catch (Exception e) {

            e.printStackTrace();
            error(ERROR_LOCAL_IO);
        }

    }

    /**
     * File soll resumed werden
     * 
     * @param value
     */
    public void setResume(boolean value) {
        this.resume = value;
    }

    /**
     * Ist resume aktiv?
     * 
     * @return
     */
    public boolean isResume() {
        return resume;
    }

    /**
     * über error() kann ein fehler gemeldet werden. DIe Methode entscheided
     * dann ob dieser fehler zu einem Abbruch führen muss
     * 
     * @param id
     */
    private void error(int id) {
        logger.severe("Error occured: " + id);
        if (errors.indexOf(id) < 0) errors.add(id);
        if (id == ERROR_TOO_MUCH_BUFFERMEMORY) {
            terminate(id);

        }
        switch (id) {
            case ERROR_UNKNOWN:
            case ERROR_TIMEOUT_REACHED:
            case ERROR_FILE_NOT_FOUND:
            case ERROR_TOO_MUCH_BUFFERMEMORY:
            case ERROR_LOCAL_IO:
            case ERROR_NO_CONNECTION:
            case ERROR_SECURITY:
            case ERROR_CHUNKLOAD_FAILED:
            case ERROR_NIBBLE_LIMIT_REACHED:
                terminate(id);

        }

    }

    /**
     * Bricht den Download komplett ab.
     * 
     * @param id
     */
    private void terminate(int id) {

        logger.severe("A critical Downloaderror occured. Terminate...");
        this.abortByError = true;

    }

    /**
     * Startet den Download. Nach dem Aufruf dieser Funktion können keine
     * Downlaodparameter mehr gesetzt werden bzw bleiben wirkungslos.
     * 
     * @return
     */
    public boolean startDownload() {
        debugtimer = System.currentTimeMillis();
        if (JDUtilities.getController().isLocalFileInProgress(downloadLink)) {
            logger.severe("File already is in progress. " + downloadLink.getFileOutput());
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_OUTPUTFILE_OWNED_BY_ANOTHER_LINK);
            error(ERROR_OUTPUTFILE_OWNED_BY_ANOTHER_LINK);
            if (!handleErrors()) return false;
        }
        File fileOutput = new File(downloadLink.getFileOutput());
        if (fileOutput == null || fileOutput.getParentFile() == null) {
            error(ERROR_OUTPUTFILE_INVALID);
            if (!handleErrors()) return false;
        }
        if (!fileOutput.getParentFile().exists()) {
            fileOutput.getParentFile().mkdirs();
        }

        if (fileOutput.exists()) {

            logger.severe("File already exists. " + fileOutput);
            String todo = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_FILE_EXISTS, JDLocale.L("system.download.triggerfileexists.skip", "Link überspringen"));

            if (!todo.equals(JDLocale.L("system.download.triggerfileexists.skip", "Link überspringen"))) {

                if (new File(downloadLink.getFileOutput()).delete()) {
                    logger.severe("--->Overwritten");
                }
                else {
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_ALREADYEXISTS);
                    error(ERROR_OUTPUTFILE_ALREADYEXISTS);
                    if (!handleErrors()) return false;
                }

            }

        }

        File part = new File(downloadLink.getFileOutput() + ".part");
        if (part.exists()) {
            logger.info("PART file exists...overwrite or resume...");
        }
        if (this.maxBytes > 0) {
            logger.info("Nibble feature active: " + maxBytes + " rest chunks to 1");
            chunkNum = 1;
        }
        try {
            // this.outputFile = new RandomAccessFile(part, "rwd");
            this.outputFile = new RandomAccessFile(downloadLink.getFileOutput() + ".part", "rw");
            // outputFile.setLength(connection.getContentLength());
            outputChannel = outputFile.getChannel();
            long fileSize = getFileSize();
            wrBuf = outputChannel.map(FileChannel.MapMode.READ_WRITE, 0, (int) fileSize);
            outputChannel.close();
            outputFile.close();
            downloadLink.setStatus(DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS);

            boolean correctChunks = JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty("PARAM_DOWNLOAD_AUTO_CORRECTCHUNKS", true);
            if (correctChunks) {

                // logger.finer("Linkspeed: " + allowedLinkSpeed);
                int tmp = Math.min(Math.max(1, (int) (fileSize / Chunk.MIN_CHUNKSIZE)), chunkNum);
                if (tmp != chunkNum) {

                    logger.finer("Corrected Chunknum: " + chunkNum + " -->" + tmp);
                    // chunkNum = tmp;
                }
            }

            int[] chunksProgress = downloadLink.getChunksProgress();
            boolean isProgressStatusValid = false;
            int[][] loadedRanges = null;
           
                if (chunksProgress != null&&this.isResume()&&plugin.getFreeConnections()>=chunkNum) {
                try {
                    logger.info("Try to resume download");
                    wrBuf.load();
                    logger.info("Old Chunknum: " + chunksProgress.length);

                    int bc;
                    boolean ch;
                    int bytesLoaded = 0;
                    isProgressStatusValid = true;
                    int supposedParts = (int) (fileSize / chunksProgress.length);
                    int[] startBytes = new int[chunksProgress.length];
                    loadedRanges = new int[chunksProgress.length][2];
                    for (int i = 0; i < chunksProgress.length; i++) {
                        if (i > 0 && chunksProgress[i] <= chunksProgress[i - 1]) {
                            isProgressStatusValid = false;
                            if(speedDebug)logger.info("Resumeerror: chunksorder error at chunk " + i);
                            break;
                        }
                        bc = 0;
                        ch = false;
                        startBytes[i] = i * supposedParts;
                        if(speedDebug)logger.info("Check Startposition " + i + " :" + i * supposedParts);
                        wrBuf.position(i * supposedParts + bc);
                        while (true) {

                            // logger.info(wrBuf.position()+"->"+wrBuf.limit()+"("+wrBuf.capacity()+")");
                            byte b = wrBuf.get();
                            if (b != 0) {
                                ch = true;
                                loadedRanges[i][0] = i * supposedParts + bc;
                                if(speedDebug)logger.info("ok at " + (i * supposedParts + bc));
                                break;
                            }
                            bc++;
                            if (i * supposedParts + bc >= chunksProgress[i]) break;
                        }
                        if (!ch) {
                            isProgressStatusValid = false;
                            if(speedDebug)logger.info("Chunk check failed (forwardsearch)" + i);
                            break;
                        }

                    }

                    if (isProgressStatusValid) {
                        for (int i = 0; i < chunksProgress.length; i++) {
                            bc = 1;

                            if (i > 0 && chunksProgress[i] <= chunksProgress[i - 1]) {
                                isProgressStatusValid = false;
                                break;
                            }
                            if (chunksProgress[i] == 0) continue;
                            ch = false;

                            while (true) {
                                wrBuf.position(chunksProgress[i] - bc);

                                bc++;
                                byte b = wrBuf.get();
                                if (b != 0) {
                                    ch = true;
                                    if(speedDebug)logger.info("Chunk " + i + " OK. " + "Found data at " + (chunksProgress[i] - bc) + " : " + b);
                                    loadedRanges[i][1] = chunksProgress[i] - bc;
                                    if(speedDebug)logger.info("Verified Range " + loadedRanges[i][0] + "- " + loadedRanges[i][1] + " as correctly loaded");

                                    bytesLoaded += Math.max(0, loadedRanges[i][1] - loadedRanges[i][0]);

                                    break;
                                }
                                if ((chunksProgress[i] - bc) < 0 || (i > 0 && (chunksProgress[i] - bc) <= chunksProgress[i - 1])) break;
                            }

                            if (!ch) {
                                isProgressStatusValid = false;
                                if(speedDebug)logger.info("Chunk check failed " + i);
                                break;
                            }

                        }

                    }
                    if (isProgressStatusValid) {
                        this.bytesLoaded = bytesLoaded;
                        logger.info("Cached Chunkprogress is valid. Resuming...");
                        chunkNum = chunksProgress.length;
                    }
                    else {
                        logger.info("Cached Chunkprogress is invalid. Overwriting...");
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

            }
            if (isProgressStatusValid) {

                logger.finer("Start Download in " + chunkNum + " chunks. Resume Ranges");
                downloadLink.setDownloadMax((int) fileSize);

                for (int i = 0; i < chunkNum; i++) {
                    if (i == (chunkNum - 1)) {
                        addChunk(new Chunk(loadedRanges[i][1], fileSize-1, connection));
                    }
                    else {
                        addChunk(new Chunk(loadedRanges[i][1], loadedRanges[i + 1][0], connection));
                    }

                }
            }
            else {
                chunkNum=Math.min(chunkNum, plugin.getFreeConnections());
                // logger.info("Filsize: " + fileSize);
                long parts = fileSize > 0 ? fileSize / chunkNum : -1;
                if (parts == -1) {
                    logger.warning("Could not get Filesize.... reset chunks to 1");
                    chunkNum = 1;
                }
                logger.finer("Start Download in " + chunkNum + " chunks. Chunksize: " + parts);
                downloadLink.setDownloadMax((int) fileSize);
                downloadLink.setChunksProgress(new int[chunkNum]);
                for (int i = 0; i < chunkNum; i++) {
                    if (i == (chunkNum - 1)) {
                        addChunk(new Chunk(i * parts, fileSize-1, connection));
                    }
                    else {
                        addChunk(new Chunk(i * parts, (i + 1) * parts, connection));
                    }

                }
            }
            waitForChunks();
            logger.info("Errors: " + this.errors);
            int bugCount = 0;
            //workaround für einen bug in der map implementation von java.
            while (true) {
                try {
                    this.wrBuf.force();
                    wrBuf = null;
                    chunks=null;
                    System.gc();
                    break;
                }
                catch (Exception e) {
                    e.printStackTrace();
                    bugCount++;
                    if (bugCount > 100) {

                        // error(ERROR_LOCAL_IO);

                    }
                    Thread.sleep(100);

                }

            }

            // gc muss hier aufgerufe werden weil jd sonst die Datei hinter
            // wrBuf nicht freigibt.

            // System.runFinalization();
            if (!handleErrors()) {

                return false;
            }
            if (!part.renameTo(new File(downloadLink.getFileOutput()))) {
                logger.severe("Could not rename file " + fileOutput + " to " + downloadLink.getFileOutput());
                error(ERROR_COULD_NOT_RENAME);

                return false;
            }
            if (!handleErrors()) {

                return false;
            }

            return true;
        }

        catch (Exception e) {

            e.printStackTrace();
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            plugin.getCurrentStep().setParameter(e.getLocalizedMessage());
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            wrBuf = null;
            // gc muss hier aufgerufe werden weil jd sonst die Datei hinter
            // wrBuf nicht freigibt.
            System.gc();
            System.runFinalization();
            return false;
        }

    }

    /**
     * Gibt eine bestmögliche abschätzung der Dateigröße zurück
     * 
     * @return
     */
    private long getFileSize() {
        if (connection.getContentLength() > 0) {
            // logger.info("1 " + connection.getHeaderFields());

            return connection.getContentLength();
        }
        if (fileSize > 0) {
            // logger.info("2");
            return fileSize;
        }
        if (downloadLink.getDownloadMax() > 0) {
            // logger.info("3");
            return downloadLink.getDownloadMax();

        }
        return -1;
    }

    /**
     * Setzt im Downloadlink und PLugin die entsprechende Fehlerids
     * 
     * @return
     */
    public boolean handleErrors() {
        downloadLink.setStatus(DownloadLink.STATUS_DONE);

        if (errors.contains(ERROR_ABORTED_BY_USER)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_TODO);
            downloadLink.setStatus(DownloadLink.STATUS_TODO);
            return false;
        }
        if (errors.contains(ERROR_TOO_MUCH_BUFFERMEMORY)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_CHUNKLOAD_FAILED);
            return false;
        }
        if (errors.contains(ERROR_CHUNKLOAD_FAILED)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_CHUNKLOAD_FAILED);

            return false;
        }

        if (errors.contains(ERROR_COULD_NOT_RENAME)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_RETRY);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
            return false;
        }
        if (errors.contains(ERROR_FILE_NOT_FOUND)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
            return false;
        }
        if (errors.contains(ERROR_OUTPUTFILE_ALREADYEXISTS)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_ALREADYEXISTS);

        
            return false;
        }

        if (errors.contains(ERROR_OUTPUTFILE_OWNED_BY_ANOTHER_LINK)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_OUTPUTFILE_OWNED_BY_ANOTHER_LINK);
            return false;
        }

        if (errors.contains(ERROR_SECURITY)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_SECURITY);
            return false;
        }

        if (errors.contains(ERROR_UNKNOWN)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
        }
        if (errors.contains(ERROR_TIMEOUT_REACHED)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_NOCONNECTION);
            return false;
        }
        
        if (errors.contains(ERROR_NIBBLE_LIMIT_REACHED)) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
            downloadLink.setStatusText("Nibbling aborted after "+this.maxBytes+" bytes");
            return false;
        }
        
        if (abortByError) {
            plugin.getCurrentStep().setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
        }
        plugin.getCurrentStep().setStatus(PluginStep.STATUS_DONE);
        // downloadLink.setStatus(DownloadLink.STATUS_DONE);
        downloadLink.setStatus(DownloadLink.STATUS_DONE);
        return true;
    }

    /**
     * Wartet bis alle Chunks fertig sind, aktuelisiert den downloadlink
     * regelmäsig und fordert beim Controller eine aktualisierung des links an
     */
    private void waitForChunks() {

        while (chunksInProgress > 0) {
            try {
                Thread.sleep(200);

            }
            catch (InterruptedException e) {
            }

            downloadLink.setDownloadCurrent(this.bytesLoaded);
            JDUtilities.getController().requestDownloadLinkUpdate(downloadLink);
            // logger.info("UüdatebytesLoaded " + bytesLoaded);
            assignChunkSpeeds();

        }

    }

    /**
     * Verteilt den verfügbraen downloadspeed auf die Chunks
     */
    private void assignChunkSpeeds() {
        int MAX_ALLOWED_OVERHEAD = 10 * 1024;
        int allowedLinkSpeed = downloadLink.getMaximalspeed();
        int mChunk = (int) ((allowedLinkSpeed / chunkNum) * 0.4);
        int currentSpeed = 0;
        Chunk next;
        synchronized (chunks) {
            Iterator<Chunk> it = chunks.iterator();
            while (it.hasNext()) {
                next = it.next();
                if (next.isAlive()) {
                    currentSpeed += next.bytesPerSecond;
                }

            }
            int overhead = allowedLinkSpeed - currentSpeed;
            if (Math.abs(overhead) < MAX_ALLOWED_OVERHEAD) return;
            // logger.info("Speed: "+currentSpeed+" overhead_: "+overhead);
            it = chunks.iterator();
            while (it.hasNext()) {
                next = it.next();
                if (next.isAlive()) {
                    next.setMaximalSpeed(Math.max(mChunk, (int) next.bytesPerSecond + overhead / Math.max(1, getRunningChunks())));
                }
            }
        }

    }

    /**
     * Fügt einen Chunk hinzu und startet diesen
     * 
     * @param chunk
     */
    private void addChunk(Chunk chunk) {
        this.chunks.add(chunk);
        chunksInProgress++;
        if (chunkNum == 1) {
            chunk.start();
        }
        else {
            chunk.start();
        }

    }

    /**
     * Gibt den aktuellen readtimeout zurück
     * 
     * @return
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Setzt den aktuellen readtimeout(nur vor dem dl start)
     * 
     * @param readTimeout
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * Gibt den requesttimeout zurück
     * 
     * @return
     */
    public int getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * Setzt vor ! dem download dden requesttimeout. Sollte nicht zu niedrig
     * sein weil sonst das automatische kopieren der Connections fehl schlägt.,
     * 
     * @param requestTimeout
     */
    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    /**
     * Addiert alle chunkspeeds und giubt diese an den downloadlink weiter
     */
    public void updateSpeed() {
        int speed = 0;
        synchronized (chunks) {
            Iterator<Chunk> it = chunks.iterator();
            while (it.hasNext())
                speed += it.next().bytesPerSecond;
        }

        downloadLink.addSpeedValue(speed);

    }

    /**
     * Gibt die aufgetretenen Fehler zurück
     * 
     * @return
     */

    public Vector<Integer> getErrors() {
        return errors;
    }

    /**
     * Gibt die Anzahl der verwendeten Chunks zurück
     * 
     * @return
     */
    public int getChunkNum() {

        return chunkNum;
    }

    /**
     * Gibt zurück wieviele Chunks gerade am arbeiten sind
     * 
     * @return
     */
    public int getRunningChunks() {
        return this.chunksInProgress;
    }

    /**
     * Gibt zurüc wieviele Chunks tatsächlich in der Downloadphase sind
     * 
     * @return
     */
    public int getChunksDownloading() {
        return chunksDownloading;
    }

    /**
     * Wird hier ein wert >0 Angegeben läd die instanz nur bis zu dieser
     * byteanzahl
     * 
     * @param integerProperty
     */
    public void setMaxBytesToLoad(int integerProperty) {
        this.maxBytes = integerProperty;

    }

    /**
     * Gibt zurück wieviele bytes schon geladen wurden
     * 
     * @return
     */
    public int getBytesLoaded() {
        return bytesLoaded;
    }

    /**
     * Setzt die filesize.
     * 
     * @param length
     */
    public void setFilesize(long length) {
        this.fileSize = length;

    }

    /**
     * Machne Hoster wollen das resumen erlauben, aber chunkload verbieten.
     * Deshalb akzeptieren sie keine range:0-** Um Trotzdem Chunkload nutzen zu
     * können werden die ersten bytes normal geladen. Und der rest normal über
     * chunks.
     * 
     * @param i
     */
    public void setLoadPreBytes(int i) {
        this.preBytes = i;

    }

    /**
     * Chunk Klasse verwaltet eine einzellne Downloadverbindung.
     * 
     * @author coalado
     * 
     */
    class Chunk extends Thread {
        // Wird durch die speedbegrenzung ein chunk uter diesen wert geregelt,
        // so wird er weggelassen.
        // sehr niedrig geregelte chunks haben einen kleinen buffer und eine
        // sehr hohe intervalzeit.
        // Das führt zu verstärkt intervalartigem laden und ist ungewünscht
        public static final int  MIN_CHUNKSIZE  = 5 * 1024 * 1024;

        private static final int MAX_BUFFERSIZE = 6 * 1024 * 1024;

        private static final int MIN_BUFFERSIZE = 1024;

        private static final int TIME_BASE      = 2000;

        private long             startByte;

        private long             endByte;

        private HTTPConnection   connection;

        private long             currentBytePosition;

        private long             bytesPerSecond = -1;

        private double           bufferTimeFaktor;

        private long             desiredBps;

        private int              maxSpeed;

        private ByteBuffer       buffer;

        private int              id             = -1;

        /**
         * die connection wird entsprechend der start und endbytes neu
         * aufgebaut.
         * 
         * @param startByte
         * @param endByte
         * @param connection
         */
        public Chunk(long startByte, long endByte, HTTPConnection connection) {
            this.startByte = startByte;
            this.endByte = endByte;
            this.connection = connection;

            if (this.startByte == 0 && preBytes > 0 && chunkNum > 1) {
                this.startByte = preBytes;
                this.loadStartBytes(preBytes);
            }
            // maxSpeed=Integer.MAX_VALUE;
            currentBytePosition = this.startByte;
            if (startByte >= endByte && endByte > 0) {
                logger.severe("Startbyte has to be less than endByte");
            }
        }
public void finalize(){
    if(speedDebug)logger.info("Finalized: "+downloadLink+" : "+this.getID());
}
        public int getID() {
            if (id < 0) {
                if(speedDebug)logger.info("INIT " + chunks.indexOf(this));
                id = chunks.indexOf(this);
            }
            return id;
        }

        /**
         * Gibt dem Chunk sein speedlimit vor. der chunk versucht sich an dieser
         * Grenze einzuregeln
         * 
         * @param i
         */
        public void setMaximalSpeed(int i) {
            maxSpeed = i;
            // logger.info(chunks.indexOf(this) + " chunkspeed: " + i);

        }

        /**
         * Gibt die Speedgrenze an.
         * 
         * @return
         */
        public int getMaximalSpeed() {
            if (this.maxSpeed <= 0) {
                this.maxSpeed = downloadLink.getMaximalspeed() / getRunningChunks();
                if(speedDebug)logger.info("Def speed: " + downloadLink.getMaximalspeed() + "/" + getRunningChunks() + "=" + maxSpeed);

            }
            if(speedDebug)logger.info("return speed: min " + maxSpeed + " - " + (this.desiredBps * 1.5));
            if (desiredBps < 1024) return maxSpeed;
            return Math.min(maxSpeed, (int) (this.desiredBps * 1.5));
        }

        /**
         * Einige Anbieter erlauben das resumen von files, aber nicht
         * multistreamloading. Dazu verbieten sie die range 0-xxx. Um das zu
         * umgehen werden die ersten bytes via preloading geladen und der erste
         * chunk fängt bei 1-xxx an
         * 
         * @param preBytes
         */
        private void loadStartBytes(int preBytes) {

            try {
                buffer = ByteBuffer.allocateDirect(preBytes);
                byte[] b = new byte[preBytes];
                InputStream inputStream = connection.getInputStream();
                int i = 0;
                while (preBytes > 0) {
                    int tmp = inputStream.read(b, i, preBytes);
                    i += tmp;
                    preBytes -= tmp;
                    logger.info("Preloaded " + i + " bytes " + new String(b));

                }
                logger.info("Preloading produced " + inputStream.available() + " bytes overhead");
                inputStream.close();
                connection.getHTTPURLConnection().disconnect();
                buffer.put(b);
                buffer.flip();
                addBytes(this);
                bytesLoaded += i;

            }
            catch (IOException e) {
                error(ERROR_CHUNKLOAD_FAILED);
                e.printStackTrace();
            }

        }

        /**
         * Kopiert die verbindung. Es wird bis auf die Range und timeouts exakt
         * die selbe verbindung nochmals aufgebaut
         * 
         * @param connection
         * @return
         */
        private HTTPConnection copyConnection(HTTPConnection connection) {

            try {
                URL link = connection.getURL();
                HTTPConnection httpConnection = new HTTPConnection(link.openConnection());
                httpConnection.setReadTimeout(getReadTimeout());
                httpConnection.setConnectTimeout(getRequestTimeout());
                httpConnection.setInstanceFollowRedirects(false);
                Map<String, List<String>> request = connection.getRequestProperties();
                if(speedDebug)logger.info("" + request);
                if (request != null) {
                    Set<Entry<String, List<String>>> requestEntries = request.entrySet();
                    Iterator<Entry<String, List<String>>> it = requestEntries.iterator();
                    String value;
                    while (it.hasNext()) {
                        Entry<String, List<String>> next = it.next();

                        value = next.getValue().toString();
                        httpConnection.setRequestProperty(next.getKey(), value.substring(1, value.length() - 1));
                    }
                }

                if (chunkNum > 1) {
                    httpConnection.setRequestProperty("Range", "bytes=" + startByte + "-" + endByte);

                    // logger.info(chunks.indexOf(this) + " - " +
                    // httpConnection.getRequestProperties() + "");
                }
                if (connection.getHTTPURLConnection().getDoOutput()) {
                    httpConnection.setDoOutput(true);
                    httpConnection.connect();
                    httpConnection.post(connection.getPostData());
                    httpConnection.getHTTPURLConnection();

                }
                else {
                    httpConnection.connect();
                }
                if(speedDebug)logger.info("ChunkHeaders: " + httpConnection.getHeaderFields());

                return httpConnection;

            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * Thread runner
         */
        public void run() {
            if (startByte >= endByte) {
                chunksInProgress--;
                return;
            }
            plugin.setCurrentConnections(plugin.getCurrentConnections()+1);
            logger.finer("Start Chunk " + startByte + " - " + endByte);
            if (chunkNum > 1) this.connection = copyConnection(connection);
            if (connection == null) {
                error(ERROR_CHUNKLOAD_FAILED);
                logger.severe("ERROR Chunk (connection copy failed) " + chunks.indexOf(this));
                chunksInProgress--;
                plugin.setCurrentConnections(plugin.getCurrentConnections()-1);
                return;
            }

            if (chunkNum > 1 && (connection.getHeaderField("Content-Range") == null || connection.getHeaderField("Content-Range").length() == 0)) {
                error(ERROR_CHUNKLOAD_FAILED);
                logger.severe("ERROR Chunk " + chunks.indexOf(this));
                chunksInProgress--;
                plugin.setCurrentConnections(plugin.getCurrentConnections()-1);
                return;

            }
            // Content-Range=[133333332-199999999/200000000]}

            String[] range = Plugin.getSimpleMatches("[" + connection.getHeaderField("Content-Range") + "]", "[°-°/°]");
            if(speedDebug)logger.info("Range Header " + connection.getHeaderField("Content-Range"));
            
            
            
            if (range == null && chunkNum > 1) {
                error(ERROR_CHUNKLOAD_FAILED);
                logger.severe("ERROR Chunk " + chunks.indexOf(this));
                chunksInProgress--;
                plugin.setCurrentConnections(plugin.getCurrentConnections()-1);
                return;

            }
            else if (range != null) {

                this.startByte = JDUtilities.filterInt(range[0]);
                this.endByte = JDUtilities.filterInt(range[1]);

                if(speedDebug)logger.finer("Resulting Range" + startByte + " - " + endByte);
            }
            logger.finer("Start Chunk " + chunks.indexOf(this));
            // try {
            // Thread.sleep(chunks.indexOf(this)*TIME_BASE);
            // }
            // catch (InterruptedException e) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
            // }
            chunksDownloading++;
            download();
            this.bytesPerSecond = 0;
            this.desiredBps = 0;
            chunksDownloading--;
            logger.finer("END Chunk " + chunks.indexOf(this));

            if (plugin.aborted || downloadLink.isAborted()) {
                error(ERROR_ABORTED_BY_USER);
            }
            logger.finer("Chunk finished " + getBytesLoaded());
            chunksInProgress--;
            plugin.setCurrentConnections(plugin.getCurrentConnections()-1);

        }

        /**
         * Über buffersize und timeinterval wird die dwonloadgeschwindigkeit
         * eingestellt. Eine zu hohe INtervalzeit sorgt für stark
         * intervalartiges laden und unregelmäsige gui aktualisierungen. Der
         * Download "ruckelt". Zu kleine INtervalzeiten belasten die Festplatte
         * sehr
         * 
         * @return
         */
        private int getTimeInterval() {
            if (!downloadLink.isLimited()) return TIME_BASE;

            return Math.min(TIME_BASE * 5, (int) (TIME_BASE * this.bufferTimeFaktor));

        }

        /**
         * Gibt zurück ob der chunk von einem externen eregniss unterbrochen
         * wurde
         * 
         * @return
         */
        private boolean isExternalyAborted() {
            return plugin.aborted || abortByError || downloadLink.isAborted();
        }

        /**
         * Die eigentliche downloadfunktion
         */
        private void download() {
            int bufferSize = 1;
            try {
                bufferSize = getBufferSize(getMaximalSpeed());

                // logger.info(bufferSize+" - "+this.getTimeInterval());
                buffer = ByteBuffer.allocateDirect(bufferSize);

            }
            catch (OutOfMemoryError e) {
                error(ERROR_TOO_MUCH_BUFFERMEMORY);
                return;
            }

            InputStream inputStream = null;
            ReadableByteChannel source = null;

            try {
                connection.setReadTimeout(getReadTimeout());
                connection.setConnectTimeout(getRequestTimeout());

                inputStream = connection.getInputStream();
                source = Channels.newChannel(inputStream);

                buffer.clear();

                long deltaTime;
                long timer;

                int bytes;
                int block = 0;
                int tempBuff = 0;
                long addWait;
                byte b[] = new byte[1];
                int read = 0;
                int ti = 0;
                while (!isExternalyAborted()) {
                    bytes = 0;
                    ti = getTimeInterval();
                    timer = System.currentTimeMillis();
                    if(speedDebug)logger.info("load Block buffer: " + buffer.hasRemaining() + "/" + buffer.capacity() + " interval: " + ti);
                    while (buffer.hasRemaining() && !isExternalyAborted() && (System.currentTimeMillis() - timer) < ti) {
                        block = 0;

                        // Prüft ob bytes zum Lesen anliegen.
                        if (inputStream.available() > 0) {
                            // kann den connectiontimeout nicht auswerten
                            block = source.read(buffer);
                        }
                        else {

                            // logger.info(""+inputStream.getClass());

                            // wertet den Timeout der connection aus
                            // (HTTPInputStream)
                            read = inputStream.read(b, 0, 1);
                            if (read > 0) {
                                buffer.put(b);
                                block = 1;
                                // Pause falls das Ende nicht erreicht worden
                                // ist. Die Schelife läuft zu schnell
                                // logger.info("Pause");
                                // Thread.sleep(25);
                            }
                            else if (read < 0) {
                                block = -1;
                               
                                break;
                            }
                        }

                        if (block == -1) {
                         
                            break;
                        }
                        bytesLoaded += block;

                        bytes += block;
                    }
                    if (block == -1 && bytes == 0) break;
                    buffer.flip();
                    addBytes(this);

                    buffer.clear();
                    currentBytePosition += bytes;

                    if (block == -1 || isExternalyAborted()) break;
                    /*
                     * War der download des buffers zu schnell, wird heir eine
                     * pause eingelegt
                     */
                    deltaTime = Math.max(System.currentTimeMillis() - timer, 1);
                    desiredBps = (1000 * (long) bytes) / deltaTime;
                    if(speedDebug)logger.info("des " + desiredBps + " - loaded: " + (System.currentTimeMillis() - timer) + " - " + bytes);
                    tempBuff = getBufferSize(getMaximalSpeed());
                    if (Math.abs(bufferSize - tempBuff) > 1000) {
                        bufferSize = tempBuff;
                        try {
                            buffer = ByteBuffer.allocateDirect(Math.max(1, bufferSize));

                        }
                        catch (Exception e) {
                            error(ERROR_TOO_MUCH_BUFFERMEMORY);
                            return;
                        }

                        buffer.clear();
                    }
                    try {
                        // 0.995 ist eine Anpassung an die Zeit, die die
                        // unerfasste Schleife noch frisst. das macht am ende
                        // ein paar wenige bytes/sekunde in der speederfassung
                        // aus.
                        addWait = (long) (0.995 * (ti - (System.currentTimeMillis() - timer)));
                        if(speedDebug)logger.info("Wait " + addWait);
                        if (addWait > 0) Thread.sleep(addWait);
                    }
                    catch (Exception e) {
                    }
                    deltaTime = System.currentTimeMillis() - timer;

                    this.bytesPerSecond = (1000 * (long) bytes) / deltaTime;
                    updateSpeed();

                    if(speedDebug)logger.info(downloadLink.getSpeedMeter().getSpeed() + " loaded" + bytes + " b in " + (deltaTime) + " ms: " + bytesPerSecond + "(" + desiredBps + ") ");

                }
                if (currentBytePosition < endByte && endByte > 0) {

                    inputStream.close();
                    source.close();

                    logger.info(" incomplete download: bytes loaded: " + currentBytePosition + "/" + endByte);
                    error(ERROR_CHUNK_INCOMPLETE);
                }

                inputStream.close();
                source.close();

            }
            catch (FileNotFoundException e) {
                logger.severe("file not found. " + e.getLocalizedMessage());
                error(ERROR_FILE_NOT_FOUND);
            }
            catch (SecurityException e) {
                logger.severe("not enough rights to write the file. " + e.getLocalizedMessage());
                error(ERROR_SECURITY);

            }
            catch (IOException e) {
                if (e.getMessage().indexOf("timed out") >= 0) {
                    error(ERROR_TIMEOUT_REACHED);
                    ;

                }
                else {
                    logger.severe("error occurred while writing to file. " + e.getLocalizedMessage());
                    error(ERROR_SECURITY);
                }
            }
            catch (Exception e) {

                e.printStackTrace();
                error(ERROR_UNKNOWN);
            }

            try {
                if (inputStream != null) inputStream.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (source != null) source.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }

        /**
         * Schränkt die Buffergröße ein.
         * 
         * @param maxspeed
         * @return
         */
        private int getBufferSize(int maxspeed) {
            if(speedDebug)logger.info("speed " + maxspeed);
            if (!downloadLink.isLimited()) return MAX_BUFFERSIZE;
            maxspeed *= (TIME_BASE / 1000);
            int max = Math.max(MIN_BUFFERSIZE, maxspeed);
            int bufferSize = Math.min(MAX_BUFFERSIZE, max);
            // logger.info(MIN_BUFFERSIZE+"<>"+maxspeed+"-"+MAX_BUFFERSIZE+"><"+max);
            this.bufferTimeFaktor = Math.max(0.1, (double) bufferSize / maxspeed);
            if(speedDebug)logger.info("Maxspeed= " + maxspeed + " buffer=" + bufferSize + "time: " + getTimeInterval());
            return bufferSize;
        }

        /**
         * Gibt die aktuelle downloadgeschwindigkeit des chunks zurück
         * 
         * @return
         */
        public long getBytesPerSecond() {
            return bytesPerSecond;
        }

        /**
         * Gibt eine Abschätzung zurück wie schnell der Chunk laden könnte wenn
         * man ihn nicht bremsen würde.
         * 
         * @return
         */
        public long getDesiredBps() {
            return desiredBps;
        }

    }

}
