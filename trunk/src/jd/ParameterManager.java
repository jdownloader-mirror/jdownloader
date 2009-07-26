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

package jd;

import java.io.File;
import java.util.Vector;
import java.util.logging.Logger;

import jd.captcha.JACController;
import jd.controlling.DistributeData;
import jd.controlling.JDController;
import jd.controlling.PasswordListController;
import jd.controlling.reconnect.Reconnecter;
import jd.gui.UIConstants;
import jd.gui.UserIF;
import jd.utils.JDUtilities;

public class ParameterManager {

    private static Logger logger = jd.controlling.JDLogger.getLogger();
    private static boolean startDownload = false;
    private static final long serialVersionUID = 1L;

    public static void showCmdHelp() {
        String[][] help = new String[][] { { JDUtilities.getJDTitle(), "JD-Team" }, { "http://jdownloader.org/\t\t", "http://board.jdownloader.org" + System.getProperty("line.separator") }, { "-h/--help\t", "Show this help message" }, { "-a/--add-link(s)", "Add links" }, { "-c/--add-container(s)", "Add containers" }, { "-d/--start-download", "Start download" }, { "-D/--stop-download", "Stop download" }, { "-H/--hide\t", "Don't open Linkgrabber when adding Links" }, { "-m/--minimize\t", "Minimize download window" }, { "-f/--focus\t", "Get jD to foreground/focus" }, { "-s/--show\t", "Show JAC prepared captchas" }, { "-t/--train\t", "Train a JAC method" }, { "-r/--reconnect\t", "Perform a Reconnect" }, { "-C/--captcha <filepath or url> <method>", "Get code from image using JAntiCaptcha" }, { "-p/--add-password(s)", "Add passwords" },
                { "-n --new-instance", "Force new instance if another jD is running" } };

        for (String helpLine[] : help) {
            System.out.println(helpLine[0] + "\t" + helpLine[1]);
        }

    }

    public static void processParameters(String[] input) {

        boolean addLinksSwitch = false;
        boolean addContainersSwitch = false;
        boolean addPasswordsSwitch = false;

        Vector<String> linksToAdd = new Vector<String>();
        Vector<String> containersToAdd = new Vector<String>();

        boolean hideGrabber = false;

        JDController controller = JDUtilities.getController();

        for (String currentArg : input) {

            if (currentArg.equals("--help") || currentArg.equals("-h")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;

                ParameterManager.showCmdHelp();

            } else if (currentArg.equals("--add-links") || currentArg.equals("--add-link") || currentArg.equals("-a")) {

                addLinksSwitch = true;
                addContainersSwitch = false;
                addPasswordsSwitch = false;
                logger.info(currentArg + " parameter");

            } else if (currentArg.equals("--add-containers") || currentArg.equals("--add-container") || currentArg.equals("-co")) {

                addContainersSwitch = true;
                addLinksSwitch = false;
                addPasswordsSwitch = false;
                logger.info(currentArg + " parameter");

            } else if (currentArg.equals("--add-passwords") || currentArg.equals("--add-password") || currentArg.equals("-p")) {

                addContainersSwitch = false;
                addLinksSwitch = false;
                addPasswordsSwitch = true;
                logger.info(currentArg + " parameter");
            } else if (currentArg.equals("--start-download") || currentArg.equals("-d")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;

                logger.info(currentArg + " parameter");
                startDownload = true;

            } else if (currentArg.equals("--stop-download") || currentArg.equals("-D")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;

                logger.info(currentArg + " parameter");
                if (controller.getDownloadStatus() == JDController.DOWNLOAD_RUNNING) {
                    JDUtilities.getController().toggleStartStop();
                }

            } else if (currentArg.equals("--show") || currentArg.equals("-s")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;

                JACController.showDialog(false);

            } else if (currentArg.equals("--train") || currentArg.equals("-t")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;

                JACController.showDialog(true);

            } else if (currentArg.equals("--minimize") || currentArg.equals("-m")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;
                UserIF.getInstance().setFrameStatus(UIConstants.WINDOW_STATUS_MINIMIZED);

                logger.info(currentArg + " parameter");

            } else if (currentArg.equals("--focus") || currentArg.equals("-f")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;
                logger.info(currentArg + " parameter");
                UserIF.getInstance().setFrameStatus(UIConstants.WINDOW_STATUS_FOREGROUND);

            } else if (currentArg.equals("--hide") || currentArg.equals("-H")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;
                logger.info(currentArg + " parameter");
                hideGrabber = true;

            } else if (currentArg.equals("--reconnect") || currentArg.equals("-r")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;
                logger.info(currentArg + " parameter");
                Reconnecter.waitForNewIP(1);

            } else if (addLinksSwitch && currentArg.charAt(0) != '-') {

                linksToAdd.add(currentArg);

            } else if (addContainersSwitch && currentArg.charAt(0) != '-') {

                if (new File(currentArg).exists()) {
                    containersToAdd.add(currentArg);
                } else {
                    logger.warning("Container does not exist");
                }

            } else if (addPasswordsSwitch && !(currentArg.charAt(0) == '-')) {

                logger.info("Add password: " + currentArg);

                PasswordListController.getInstance().addPassword(currentArg);

            } else if (currentArg.contains("http://") && !(currentArg.charAt(0) == '-')) {

                addContainersSwitch = false;
                addLinksSwitch = false;
                addPasswordsSwitch = false;
                linksToAdd.add(currentArg);

            } else if (new File(currentArg).exists() && !(currentArg.charAt(0) == '-')) {

                addContainersSwitch = false;
                addLinksSwitch = false;
                addPasswordsSwitch = false;
                containersToAdd.add(currentArg);

            } else {
                addContainersSwitch = false;
                addLinksSwitch = false;
                addPasswordsSwitch = false;

            }

        }

        if (linksToAdd.size() > 0) {
            logger.info("Links to add: " + linksToAdd.toString());
        }
        if (containersToAdd.size() > 0) {
            logger.info("Containers to add: " + containersToAdd.toString());
        }

        for (int i = 0; i < containersToAdd.size(); i++) {
            controller.loadContainerFile(new File(containersToAdd.get(i)), hideGrabber, startDownload);
        }
        StringBuilder adder = new StringBuilder();
        for (String string : linksToAdd) {
            adder.append(string);
            adder.append('\n');
        }
        String linksToAddString = adder.toString().trim();
        if (!linksToAddString.equals("")) {
            new DistributeData(linksToAddString, hideGrabber).start();
        }
        if (startDownload) {
            if (controller.getDownloadStatus() == JDController.DOWNLOAD_NOT_RUNNING) {
                JDController.getInstance().startDownloads();
            }
        }
    }

}
