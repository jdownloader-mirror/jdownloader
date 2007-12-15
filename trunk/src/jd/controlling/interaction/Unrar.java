package jd.controlling.interaction;

import java.io.File;
import java.io.Serializable;
import java.util.Vector;

import jd.config.Configuration;
import jd.controlling.JDController;
import jd.plugins.DownloadLink;
import jd.unrar.JUnrar;
import jd.utils.JDUtilities;

/**
 * Diese Klasse fürs automatische Entpacken
 * 
 * @author DwD
 */
public class Unrar extends Interaction implements Serializable {
    private static Unrar INSTANCE = null;

    // DIese Interaction sollte nur einmal exisitieren und wird deshalb über
    // eine factory aufgerufen.
    private Unrar() {
        super();
    }

    public static Unrar getInstance() {
        if (INSTANCE == null) {
            if (JDUtilities.getConfiguration().getProperty(Configuration.PARAM_UNRAR_INSTANCE, null) != null) {
                INSTANCE = (Unrar) JDUtilities.getConfiguration().getProperty(Configuration.PARAM_UNRAR_INSTANCE, null);
                return INSTANCE;
            }
            INSTANCE = new Unrar();
        }
        return INSTANCE;

    }

    /**
     * 
     */
    private static final long   serialVersionUID         = 2467582501274722811L;

    /**
     * serialVersionUID
     */
    private static boolean      IS_RUNNING               = false;

    private static final String NAME                     = JDUtilities.getResourceString("unrar.name");

    public static final String  PROPERTY_UNRARCOMMAND    = "UNRAR_PROPERTY_UNRARCMD";

    public static final String  PROPERTY_AUTODELETE      = "UNRAR_PROPERTY_AUTODELETE";

    public static final String  PROPERTY_OVERWRITE_FILES = "UNRAR_PROPERTY_OVERWRITE_FILES";

    public static final String  PROPERTY_MAX_FILESIZE    = "UNRAR_PROPERTY_MAX_FILESIZE";

    public static final String  PROPERTY_ENABLED_TYPE         = "UNRAR_PROPERTY_ENABLED";

    public static final String ENABLED_TYPE_NEVER = JDUtilities.getResourceString("unrar.interaction.never");

    public static final String ENABLED_TYPE_ALWAYS = JDUtilities.getResourceString("unrar.interaction.always");

    public static final String ENABLED_TYPE_LINKGRABBER = JDUtilities.getResourceString("unrar.interaction.linkgrabber");

    @Override
    public boolean doInteraction(Object arg) {
        start();
        return true;

    }
    
    public boolean getWaitForTermination(){
        return false;
    }

    @Override
    public String toString() {
        return JDUtilities.getResourceString("unrar.interaction.name");
    }

    @Override
    public String getInteractionName() {
        return NAME;
    }

    private JUnrar getUnrar() {
        JDController controller = JDUtilities.getController();
        DownloadLink dLink = controller.getLastFinishedDownloadLink();
        String password = null;
        if (dLink != null) password = dLink.getFilePackage().getPassword();

        JUnrar unrar = new JUnrar();
        if (password != null && !password.matches("[\\s]*")) {
            if (!password.matches("\\{\".*\"\\}")) unrar.standardPassword = password;
            unrar.addToPasswordlist(password);
        }
        unrar.overwriteFiles = JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_OVERWRITE_FILES, false);
        unrar.autoDelete = JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_AUTODELETE, false);
        unrar.unrar = JDUtilities.getConfiguration().getStringProperty(Unrar.PROPERTY_UNRARCOMMAND);
        unrar.maxFilesize = JDUtilities.getConfiguration().getIntegerProperty(Unrar.PROPERTY_MAX_FILESIZE, 2);
        return unrar;
    }

    @Override
    public void run() {
        if (!IS_RUNNING) {
            IS_RUNNING = true;
            JUnrar unrar = getUnrar();
            Vector<DownloadLink> finishedLinks = JDUtilities.getController().getFinishedLinks();
            Vector<String> folders = new Vector<String>();
            for (int i = 0; i < finishedLinks.size(); i++) {
                logger.info("finished File: " + finishedLinks.get(i).getFileOutput());
                File folder = new File(finishedLinks.get(i).getFileOutput()).getParentFile();
                logger.info("Folder: " + folder);
                if (folder.exists()) {
                    if (folders.indexOf(folder.getAbsolutePath()) == -1) {
                        logger.info("Add unrardir: " + folder.getAbsolutePath());
                        folders.add(folder.getAbsolutePath());
                    }
                }
            }
            folders.add(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY));
            logger.info("dirs: " + folders);
            unrar.setFolders(folders);
            Vector<String> newFolderList = unrar.unrar();
            // Entpacken bis nichts mehr gefunden wurde

            if (newFolderList != null && newFolderList.size() > 0) {
                unrar = getUnrar();
                unrar.setFolders(newFolderList);
                newFolderList = unrar.unrar();
            }
            if (newFolderList != null && newFolderList.size() > 0) {
                unrar = getUnrar();
                unrar.setFolders(newFolderList);
                newFolderList = unrar.unrar();
            }

      
            IS_RUNNING = false;
            this.setCallCode(Interaction.INTERACTION_CALL_SUCCESS);
            Interaction.handleInteraction(Interaction.INTERACTION_AFTER_UNRAR, null);
        }
        else {
            this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
            logger.warning("UNrar Skipped. THere is already an unrar Process running");
        }
    }

    @Override
    public void initConfig() {

    }

    @Override
    public void resetInteraction() {}
}
