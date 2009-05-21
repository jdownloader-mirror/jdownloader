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

//
//    Alle Ausgaben sollten lediglich eine Zeile lang sein, um die kompatibilität zu erhöhen.
//

package jd.plugins.optional;

import java.awt.event.ActionEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Level;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.MenuItem;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.DistributeData;
import jd.controlling.JDLogger;
import jd.controlling.reconnect.Reconnecter;
import jd.event.ControlListener;
import jd.http.Encoding;
import jd.nutils.Formatter;
import jd.nutils.httpserver.Handler;
import jd.nutils.httpserver.HttpServer;
import jd.nutils.httpserver.Request;
import jd.nutils.httpserver.Response;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.WebUpdate;

public class JDRemoteControl extends PluginOptional implements ControlListener {
    public JDRemoteControl(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getIconKey() {

        return "gui.images.network";
    }

    class Serverhandler implements Handler {

        public void handle(Request request, Response response) {
            response.setReturnType("text/html");
            response.setReturnStatus(Response.OK);

            // ---------------------------------------
            // Help
            // ---------------------------------------

            if (request.getRequestUrl().equals("/help")) {

                Vector<String> commandvec = new Vector<String>();
                Vector<String> infovector = new Vector<String>();

                commandvec.add(" ");
                infovector.add("<br /><b>Get Values:</b><br />&nbsp;");

                commandvec.add("/get/speed");
                infovector.add("Get current Speed");

                commandvec.add("/get/ip");
                infovector.add("Get IP");

                commandvec.add("/get/config");
                infovector.add("Get Config");

                commandvec.add("/get/version");
                infovector.add("Get Version");

                commandvec.add("/get/rcversion");
                infovector.add("Get RemoteControl Version");

                commandvec.add("/get/speedlimit");
                infovector.add("Get current Speedlimit");

                commandvec.add("/get/isreconnect");
                infovector.add("Get If Reconnect");

                commandvec.add("/get/downloads/currentcount");
                infovector.add("Get amount of current downloads");
                commandvec.add("/get/downloads/currentlist");
                infovector.add("Get Current Downloads List");

                commandvec.add("/get/downloads/allcount");
                infovector.add("Get amount of downloads in list");
                commandvec.add("/get/downloads/alllist");
                infovector.add("Get list of downloads in list");

                commandvec.add("/get/downloads/finishedcount");
                infovector.add("Get amount of finished Downloads");
                commandvec.add("/get/downloads/finishedlist");
                infovector.add("Get finished Downloads List");

                commandvec.add(" ");
                infovector.add("<br /><b>Actions:</b><br />&nbsp;");

                commandvec.add("/action/start");
                infovector.add("Start DLs");

                commandvec.add("/action/pause");
                infovector.add("Pause DLs");

                commandvec.add("/action/stop");
                infovector.add("Stop DLs");

                commandvec.add("/action/toggle");
                infovector.add("Toggle DLs");

                commandvec.add("/action/update/force(0|1)/");
                infovector.add("Do Webupdate <br />" + "force1 activates auto-restart if update is possible");

                commandvec.add("/action/reconnect");
                infovector.add("Do Reconnect");

                commandvec.add("/action/restart");
                infovector.add("Restart JD");

                commandvec.add("/action/shutdown");
                infovector.add("Shutdown JD");

                commandvec.add("/action/set/download/limit/%X%");
                infovector.add("Set Downloadspeedlimit %X%");

                commandvec.add("/action/set/download/max/%X%");
                infovector.add("Set max sim. Downloads %X%");

                commandvec.add("/action/add/links/grabber(0|1)/start(0|1)/%X%");
                infovector.add("Add Links %X% to Grabber<br />" + "Options:<br />" + "grabber(0|1): Show/Hide LinkGrabber<br />" + "start(0|1): Start DLs after insert<br />" + "Sample:<br />" + "/action/add/links/grabber0/start1/http://tinyurl.com/6o73eq http://tinyurl.com/4khvhn<br />" + "Don't forget Space between Links!");

                commandvec.add("/action/add/container/%X%");
                infovector.add("Add Container %X%<br />" + "Sample:<br />" + "/action/add/container/C:\\container.dlc");

                commandvec.add("/action/save/container/%X%");
                infovector.add("Save DLC-Container with all Links to %X%<br /> " + "Sample see /action/add/container/%X%");

                commandvec.add("/action/set/reconnectenabled/(true|false)");
                infovector.add("Set Reconnect enabled or not");

                commandvec.add("/action/set/premiumenabled/(true|false)");
                infovector.add("Set Use Premium enabled or not");

                response.addContent("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"\"http://www.w3.org/TR/html4/strict.dtd\"><html><head><title>JDRemoteControl Help</title><style type=\"text/css\">a {    font-size: 14px;    text-decoration: none;    background: none;    color: #599ad6;}a:hover {    text-decoration: underline;    color:#333333;}body {    color: #333333;    background:#f0f0f0;    font-family: Verdana, Arial, Helvetica, sans-serif;    font-size: 14px;    vertical-align: top;  }</style></head><body><p><br /><b>JDRemoteControl " + getVersion() + "<br /><br />Usage:</b><br />&nbsp;<br />1)Replace %X% with your value<br />Sample: /action/save/container/C:\\backup.dlc <br />2)Replace (true|false) with true or false<br /><table border=\"0\" cellspacing=\"5\">");
                for (int commandcount = 0; commandcount < commandvec.size(); commandcount++) {
                    response.addContent("<tr><td valign=\"top\"><a href=\"http://127.0.0.1:" + getPluginConfig().getIntegerProperty("PORT", 10025) + commandvec.get(commandcount) + "\">" + commandvec.get(commandcount) + "</a></td><td valign=\"top\">" + infovector.get(commandcount) + "</td></tr>");
                }
                response.addContent("</table><br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;</p></body></html>");
            }

            // ---------------------------------------
            // Get
            // ---------------------------------------

            // Get IP
            else if (request.getRequestUrl().equals("/get/ip")) {
                response.addContent(JDUtilities.getIPAddress(null));
            }

            // Get Config
            else if (request.getRequestUrl().equals("/get/config")) {
                Property config = JDUtilities.getConfiguration();
                response.addContent("<pre>");
                if (request.getParameters().containsKey("sub")) {

                    config = SubConfiguration.getConfig(((String) request.getParameters().get("sub")).toUpperCase());
                }
                Entry<String, Object> next;
                for (Iterator<Entry<String, Object>> it = config.getProperties().entrySet().iterator(); it.hasNext();) {
                    next = it.next();
                    response.addContent(next.getKey() + " = " + next.getValue() + "\r\n");
                }
                response.addContent("</pre>");
            }

            // Get Version
            else if (request.getRequestUrl().equals("/get/version")) {
                response.addContent(JDUtilities.JD_VERSION + JDUtilities.getRevision());
            }

            // Get RemoteControlVersion
            else if (request.getRequestUrl().equals("/get/rcversion")) {
                response.addContent(getVersion());
            }

            // Get SpeedLimit
            else if (request.getRequestUrl().equals("/get/speedlimit")) {
                response.addContent(SubConfiguration.getConfig("DOWNLOAD").getProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, "0"));
            }

            // Get Current DLs COUNT
            else if (request.getRequestUrl().equals("/get/downloads/currentcount")) {
                int counter = 0;
                FilePackage filePackage;
                DownloadLink dLink;
                Integer Package_ID;
                Integer Download_ID;

                for (Package_ID = 0; Package_ID < JDUtilities.getController().getPackages().size(); Package_ID++) {
                    filePackage = JDUtilities.getController().getPackages().get(Package_ID);
                    for (Download_ID = 0; Download_ID < filePackage.getDownloadLinkList().size(); Download_ID++) {

                        dLink = filePackage.getDownloadLinkList().get(Download_ID);
                        if (dLink.getLinkStatus().isPluginActive()) {
                            counter++;
                        }
                    }
                }
                response.addContent(counter);
            }

            // Get Current DLs
            else if (request.getRequestUrl().equals("/get/downloads/currentlist")) {
                String output = "";

                FilePackage filePackage;
                DownloadLink dLink;
                Integer Package_ID;
                Integer Download_ID;

                for (Package_ID = 0; Package_ID < JDUtilities.getController().getPackages().size(); Package_ID++) {
                    filePackage = JDUtilities.getController().getPackages().get(Package_ID);

                    /* Paket Infos */
                    output = output + "<package";// Open Package
                    output = output + " package_name=\"" + filePackage.getName() + "\"";
                    output = output + " package_id=\"" + Package_ID.toString() + "\"";
                    output = output + " package_percent=\"" + f.format(filePackage.getPercent()) + "\"";
                    output = output + " package_linksinprogress=\"" + filePackage.getLinksInProgress() + "\"";
                    output = output + " package_linkstotal=\"" + filePackage.size() + "\"";
                    output = output + " package_ETA=\"" + Formatter.formatSeconds(filePackage.getETA()) + "\"";
                    output = output + " package_speed=\"" + Formatter.formatReadable(filePackage.getTotalDownloadSpeed()) + "/s\"";
                    output = output + " package_loaded=\"" + Formatter.formatReadable(filePackage.getTotalKBLoaded()) + "\"";
                    output = output + " package_size=\"" + Formatter.formatReadable(filePackage.getTotalEstimatedPackageSize()) + "\"";
                    output = output + " package_todo=\"" + Formatter.formatReadable(filePackage.getTotalEstimatedPackageSize() - filePackage.getTotalKBLoaded()) + "\"";
                    output = output + " >";// Close Package

                    for (Download_ID = 0; Download_ID < filePackage.getDownloadLinkList().size(); Download_ID++) {

                        dLink = filePackage.getDownloadLinkList().get(Download_ID);
                        if (dLink.getLinkStatus().isPluginActive()) {
                            /* Download Infos */
                            output = output + "<file";// Open File
                            output = output + " file_name=\"" + dLink.getName() + "\"";
                            output = output + " file_id=\"" + Download_ID.toString() + "\"";
                            output = output + " file_package=\"" + Package_ID.toString() + "\"";
                            output = output + " file_percent=\"" + f.format(dLink.getDownloadCurrent() * 100.0 / Math.max(1, dLink.getDownloadSize())) + "\"";
                            output = output + " file_hoster=\"" + dLink.getHost() + "\"";
                            output = output + " file_status=\"" + dLink.getLinkStatus().getStatusString().toString() + "\"";
                            output = output + " file_speed=\"" + dLink.getDownloadSpeed() + "\"";
                            output = output + " > ";// Close File
                        }
                    }
                    output = output + "</package> ";// Close Package
                }
                response.addContent(output);
            }

            // Get DLList COUNT
            else if (request.getRequestUrl().equals("/get/downloads/allcount")) {
                int counter = 0;
                FilePackage filePackage;
                Integer Package_ID;
                Integer Download_ID;

                for (Package_ID = 0; Package_ID < JDUtilities.getController().getPackages().size(); Package_ID++) {
                    filePackage = JDUtilities.getController().getPackages().get(Package_ID);

                    for (Download_ID = 0; Download_ID < filePackage.getDownloadLinkList().size(); Download_ID++) {
                        counter++;
                    }
                }
                response.addContent(counter);
            }

            // Get DLList
            else if (request.getRequestUrl().equals("/get/downloads/alllist")) {
                StringBuilder output = new StringBuilder();

                FilePackage filePackage;
                DownloadLink dLink;
                Integer Package_ID;
                Integer Download_ID;

                for (Package_ID = 0; Package_ID < JDUtilities.getController().getPackages().size(); Package_ID++) {
                    filePackage = JDUtilities.getController().getPackages().get(Package_ID);

                    /* Paket Infos */
                    output.append("<package");// Open Package
                    output.append(" package_name=\"" + filePackage.getName() + "\"");
                    output.append(" package_id=\"" + Package_ID.toString() + "\"");
                    output.append(" package_percent=\"" + f.format(filePackage.getPercent()) + "\"");
                    output.append(" package_linksinprogress=\"" + filePackage.getLinksInProgress() + "\"");
                    output.append(" package_linkstotal=\"" + filePackage.size() + "\"");
                    output.append(" package_ETA=\"" + Formatter.formatSeconds(filePackage.getETA()) + "\"");
                    output.append(" package_speed=\"" + Formatter.formatReadable(filePackage.getTotalDownloadSpeed()) + "/s\"");
                    output.append(" package_loaded=\"" + Formatter.formatReadable(filePackage.getTotalKBLoaded()) + "\"");
                    output.append(" package_size=\"" + Formatter.formatReadable(filePackage.getTotalEstimatedPackageSize()) + "\"");
                    output.append(" package_todo=\"" + Formatter.formatReadable(filePackage.getTotalEstimatedPackageSize() - filePackage.getTotalKBLoaded()) + "\"");
                    output.append(" >");// Close Package

                    for (Download_ID = 0; Download_ID < filePackage.getDownloadLinkList().size(); Download_ID++) {

                        dLink = filePackage.getDownloadLinkList().get(Download_ID);
                        /* Download Infos */
                        output.append("<file");// Open File
                        output.append(" file_name=\"" + dLink.getName() + "\"");
                        output.append(" file_id=\"" + Download_ID.toString() + "\"");
                        output.append(" file_package=\"" + Package_ID.toString() + "\"");
                        output.append(" file_percent=\"" + f.format(dLink.getDownloadCurrent() * 100.0 / Math.max(1, dLink.getDownloadSize())) + "\"");
                        output.append(" file_hoster=\"" + dLink.getHost() + "\"");
                        output.append(" file_status=\"" + dLink.getLinkStatus().getStatusString().toString() + "\"");
                        output.append(" file_speed=\"" + dLink.getDownloadSpeed() + "\"");
                        output.append(" > ");// Close File
                    }
                    output.append("</package> ");// Close Package
                }
                response.addContent(output.toString());
            }

            // Get finished DLs COUNT
            else if (request.getRequestUrl().equals("/get/downloads/finishedcount")) {
                int counter = 0;
                FilePackage filePackage;
                DownloadLink dLink;
                Integer Package_ID;
                Integer Download_ID;

                for (Package_ID = 0; Package_ID < JDUtilities.getController().getPackages().size(); Package_ID++) {
                    filePackage = JDUtilities.getController().getPackages().get(Package_ID);

                    for (Download_ID = 0; Download_ID < filePackage.getDownloadLinkList().size(); Download_ID++) {

                        dLink = filePackage.getDownloadLinkList().get(Download_ID);
                        if (dLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                            counter++;
                        }
                    }
                }
                response.addContent(counter);
            }

            // Get finished DLs
            else if (request.getRequestUrl().equals("/get/downloads/finishedlist")) {
                StringBuilder output = new StringBuilder();

                FilePackage filePackage;
                DownloadLink dLink;
                Integer Package_ID;
                Integer Download_ID;

                for (Package_ID = 0; Package_ID < JDUtilities.getController().getPackages().size(); Package_ID++) {
                    filePackage = JDUtilities.getController().getPackages().get(Package_ID);

                    /* Paket Infos */
                    output.append("<package");// Open Package
                    output.append(" package_name=\"" + filePackage.getName() + "\"");
                    output.append(" package_id=\"" + Package_ID.toString() + "\"");
                    output.append(" package_percent=\"" + f.format(filePackage.getPercent()) + "\"");
                    output.append(" package_linksinprogress=\"" + filePackage.getLinksInProgress() + "\"");
                    output.append(" package_linkstotal=\"" + filePackage.size() + "\"");
                    output.append(" package_ETA=\"" + Formatter.formatSeconds(filePackage.getETA()) + "\"");
                    output.append(" package_speed=\"" + Formatter.formatReadable(filePackage.getTotalDownloadSpeed()) + "/s\"");
                    output.append(" package_loaded=\"" + Formatter.formatReadable(filePackage.getTotalKBLoaded()) + "\"");
                    output.append(" package_size=\"" + Formatter.formatReadable(filePackage.getTotalEstimatedPackageSize()) + "\"");
                    output.append(" package_todo=\"" + Formatter.formatReadable(filePackage.getTotalEstimatedPackageSize() - filePackage.getTotalKBLoaded()) + "\"");
                    output.append(" >");// Close Package

                    for (Download_ID = 0; Download_ID < filePackage.getDownloadLinkList().size(); Download_ID++) {

                        dLink = filePackage.getDownloadLinkList().get(Download_ID);
                        if (dLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                            /* Download Infos */
                            output.append("<file");// Open File
                            output.append(" file_name=\"" + dLink.getName() + "\"");
                            output.append(" file_id=\"" + Download_ID.toString() + "\"");
                            output.append(" file_package=\"" + Package_ID.toString() + "\"");
                            output.append(" file_percent=\"" + f.format(dLink.getDownloadCurrent() * 100.0 / Math.max(1, dLink.getDownloadSize())) + "\"");
                            output.append(" file_hoster=\"" + dLink.getHost() + "\"");
                            output.append(" file_status=\"" + dLink.getLinkStatus().getStatusString().toString() + "\"");
                            output.append(" file_speed=\"" + dLink.getDownloadSpeed() + "\"");
                            output.append(" > ");// Close File
                        }
                    }
                    output.append("</package> ");// Close Package
                }
                response.addContent(output.toString());
            }

            // Get current Speed
            else if (request.getRequestUrl().equals("/get/speed")) {
                response.addContent(JDUtilities.getController().getSpeedMeter() / 1000);
            }

            // Get IsReconnect
            else if (request.getRequestUrl().equals("/get/isreconnect")) {
                response.addContent(!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false));
            }

            // ---------------------------------------
            // Control
            // ---------------------------------------

            // Do Start Download
            else if (request.getRequestUrl().equals("/action/start")) {
                JDUtilities.getController().startDownloads();

                response.addContent("Downloads started");
            }

            // Do Pause Download
            else if (request.getRequestUrl().equals("/action/pause")) {
                JDUtilities.getController().pauseDownloads(true);
                response.addContent("Downloads paused");
            }

            // Do Stop Download
            else if (request.getRequestUrl().equals("/action/stop")) {
                JDUtilities.getController().stopDownloads();
                response.addContent("Downloads stopped");
            }

            // Do Toggle Download
            else if (request.getRequestUrl().equals("/action/toggle")) {
                JDUtilities.getController().toggleStartStop();
                response.addContent("Downloads toggled");
            }

            // Do Make Webupdate
            else if (request.getRequestUrl().matches("[\\s\\S]*?/action/update/force[01]{1}/[\\s\\S]*")) {

                Integer force = Integer.parseInt(new Regex(request.getRequestUrl(), "[\\s\\S]*?/action/update/force([01]{1})/[\\s\\S]*").getMatch(0));
                if (force == 1) {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, true);
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_WEBUPDATE_DISABLE, false);
                }

                new WebUpdate().doWebupdate(true);

                response.addContent("Do Webupdate...");
            }

            // Do Reconnect
            else if (request.getRequestUrl().equals("/action/reconnect")) {
                response.addContent("Do Reconnect...");

                boolean tmp = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, true);
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, false);

                if (JDUtilities.getController().stopDownloads()) {
                    Reconnecter.waitForNewIP(1);
                    JDUtilities.getController().startDownloads();
                } else {
                    Reconnecter.waitForNewIP(1);
                }

                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, tmp);

            }

            // Do Restart JD
            else if (request.getRequestUrl().equals("/action/restart")) {
                response.addContent("Restarting...");

                class JDClose implements Runnable {
                    JDClose() {
                        new Thread(this).start();
                    }

                    public void run() {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {

                            logger.log(Level.SEVERE, "Exception occured", e);
                        }
                        JDUtilities.restartJD();
                    }
                }
                @SuppressWarnings("unused")
                JDClose jdshutdown = new JDClose();
            }

            // Do Shutdown JD
            else if (request.getRequestUrl().equals("/action/shutdown")) {

                response.addContent("Shutting down...");

                class JDClose implements Runnable {
                    JDClose() {
                        new Thread(this).start();
                    }

                    public void run() {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {

                            logger.log(Level.SEVERE, "Exception occured", e);
                        }
                        JDUtilities.getController().exit();
                    }
                }
                @SuppressWarnings("unused")
                JDClose jdshutdown = new JDClose();
            }

            // Set Downloadlimit
            else if (request.getRequestUrl().matches("(?is).*/action/set/download/limit/[0-9]+.*")) {
                Integer newdllimit = Integer.parseInt(new Regex(request.getRequestUrl(), "[\\s\\S]*/action/set/download/limit/([0-9]+).*").getMatch(0));
                logger.fine("RemoteControl - Set max. Downloadspeed: " + newdllimit.toString());
                SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, newdllimit.toString());
                SubConfiguration.getConfig("DOWNLOAD").save();
                response.addContent("newlimit=" + newdllimit);
            }

            // Set max. sim. Downloads
            else if (request.getRequestUrl().matches("(?is).*/action/set/download/max/[0-9]+.*")) {
                Integer newsimdl = Integer.parseInt(new Regex(request.getRequestUrl(), "[\\s\\S]*/action/set/download/max/([0-9]+).*").getMatch(0));
                logger.fine("RemoteControl - Set max. sim. Downloads: " + newsimdl.toString());
                SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, newsimdl.toString());
                SubConfiguration.getConfig("DOWNLOAD").save();
                response.addContent("newmax=" + newsimdl);
            }

            // Add Link(s)
            else if (request.getRequestUrl().matches("(?is).*/action/add/links/grabber[01]{1}/start[01]{1}/[\\s\\S]+")) {
                String link = new Regex(request.getRequestUrl(), "[\\s\\S]*?/action/add/links/grabber[01]{1}/start[01]{1}/(.*)").getMatch(0);
                if (request.getParameters().size() > 0) {
                    link += "?";
                    Iterator<String> it = request.getParameters().keySet().iterator();
                    while (it.hasNext()) {
                        String help = it.next();
                        link += help;
                        if (!request.getParameter(help).equals("")) {
                            link += "=" + request.getParameter(help);
                        }
                        if (it.hasNext()) link += "&";
                    }
                }
                // response.addContent(link);
                Integer showgrab = Integer.parseInt(new Regex(request.getRequestUrl(), "[\\s\\S]*?/action/add/links/grabber([01]{1})/start[01]{1}/[\\s\\S]+").getMatch(0));
                Boolean hidegrabber = false;
                if (showgrab == 0) {
                    hidegrabber = true;
                }
                // response.addContent(hidegrabber.toString());
                Integer stdl = Integer.parseInt(new Regex(request.getRequestUrl(), "[\\s\\S]*?/action/add/links/grabber[01]{1}/start([01]{1})/[\\s\\S]+").getMatch(0));
                Boolean startdl = false;
                if (stdl == 1) {
                    startdl = true;
                }
                // response.addContent(startdl.toString());
                // link = Encoding.htmlDecode(link);
                // wegen leerzeichen etc, die ja in urls verändert werden...

                new DistributeData(link, hidegrabber, startdl).start();
                response.addContent("Link(s) added. (" + link + ")");
            }

            // Open DLC Container
            else if (request.getRequestUrl().matches("(?is).*/action/add/container/[\\s\\S]+")) {
                String dlcfilestr = new Regex(request.getRequestUrl(), "[\\s\\S]*/action/add/container/([\\s\\S]+)").getMatch(0);
                dlcfilestr = Encoding.htmlDecode(dlcfilestr);
                // wegen leerzeichen etc, die ja in urls verändert werden...
                JDUtilities.getController().loadContainerFile(new File(dlcfilestr));
                response.addContent("Container opened. (" + dlcfilestr + ")");
            }

            // Save Linklist as DLC Container
            else if (request.getRequestUrl().matches("(?is).*/action/save/container/[\\s\\S]+")) {
                String dlcfilestr = new Regex(request.getRequestUrl(), "[\\s\\S]*/action/save/container/([\\s\\S]+)").getMatch(0);
                dlcfilestr = Encoding.htmlDecode(dlcfilestr);
                // wegen leerzeichen etc, die ja in urls verändert werden...
                JDUtilities.getController().saveDLC(new File(dlcfilestr), JDUtilities.getDownloadController().getAllDownloadLinks());
                response.addContent("Container saved. (" + dlcfilestr + ")");
            }

            // Set ReconnectEnabled
            else if (request.getRequestUrl().matches("(?is).*/action/set/reconnectenabled/.*")) {
                boolean newrc = Boolean.parseBoolean(new Regex(request.getRequestUrl(), "[\\s\\S]*/action/set/reconnectenabled/(.*)").getMatch(0));
                boolean disprc = newrc;
                if (newrc == false) {
                    newrc = true;
                } else {
                    newrc = false;
                }
                logger.fine("RemoteControl - Set ReConnect: " + disprc);
                if (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false) == newrc) {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, newrc);
                    JDUtilities.getConfiguration().save();

                    response.addContent("reconnect=" + disprc + " (CHANGED=true)");
                } else {
                    response.addContent("reconnect=" + disprc + " (CHANGED=false)");
                }
            }

            // Set use premium
            else if (request.getRequestUrl().matches("(?is).*/action/set/premiumenabled/.*")) {
                boolean newuseprem = Boolean.parseBoolean(new Regex(request.getRequestUrl(), "[\\s\\S]*/action/set/premiumenabled/(.*)").getMatch(0));
                logger.fine("RemoteControl - Set Premium: " + newuseprem);
                if (newuseprem ^ JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, newuseprem)) {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, newuseprem);
                    JDUtilities.getConfiguration().save();
                    response.addContent("newprem=" + newuseprem + " (CHANGED=true)");
                } else {
                    response.addContent("newprem=" + newuseprem + " (CHANGED=false)");
                }

            } else {
                response.addContent("JDRemoteControl - Malformed Request. use /help");
            }
        }
    }

    public static int getAddonInterfaceVersion() {
        return 3;
    }

    private DecimalFormat f = new DecimalFormat("#0.00");

    private HttpServer server;

    public void actionPerformed(ActionEvent e) {
        if (server == null) { return; }
        try {
            if (server.isStarted()) {
                server.sstop();
                JDUtilities.getGUI().showMessageDialog(getHost() + " " + JDLocale.L("plugins.optional.remotecontrol.stopped", "stopped."));
            } else {
                server = new HttpServer(getPluginConfig().getIntegerProperty("PORT", 10025), new Serverhandler());
                server.start();
                JDUtilities.getGUI().showMessageDialog(getHost() + " " + JDLocale.L("plugins.optional.remotecontrol.startedonport", "started on port") + " " + getPluginConfig().getIntegerProperty("PORT", 10025) + "\n http://127.0.0.1:" + getPluginConfig().getIntegerProperty("PORT", 10025) + JDLocale.L("plugins.optional.remotecontrol.help", "/help for Developer Information."));
            }
        } catch (Exception ex) {
            JDLogger.exception(ex);
        }
    }

    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        menu.add(new MenuItem(JDLocale.L("plugins.optional.remotecontrol.toggle", "Start/Stop RemoteControl"), 0).setActionListener(this));
        return menu;
    }

    public String getHost() {
        return JDLocale.L("plugins.optional.remotecontrol.name", "RemoteControl");
    }

    public String getRequirements() {
        return "JRE 1.5+";
    }

    public String getVersion() {
        return getVersion("$Revision$");
    }

    public boolean initAddon() {
        logger.info("RemoteControl OK");
        initRemoteControl();
        JDUtilities.getController().addControlListener(this);
        return true;
    }

    public void initRemoteControl() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), "PORT", JDLocale.L("plugins.optional.RemoteControl.port", "Port:"), 1000, 65500));
        cfg.setDefaultValue(10025);

        try {
            server = new HttpServer(getPluginConfig().getIntegerProperty("PORT", 10025), new Serverhandler());
            server.start();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception occured", e);
        }
    }

    public void onExit() {

    }
}