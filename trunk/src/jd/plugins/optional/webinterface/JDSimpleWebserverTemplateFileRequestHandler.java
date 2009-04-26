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

package jd.plugins.optional.webinterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import jd.OptionalPluginWrapper;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDController;
import jd.controlling.LinkGrabberController;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberFilePackage;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberPanel;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadInterface.Chunk;
import jd.plugins.optional.webinterface.template.Template;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class JDSimpleWebserverTemplateFileRequestHandler {

    private DecimalFormat f = new DecimalFormat("#0");
    private JDSimpleWebserverResponseCreator response;

    private Vector<Object> v_info = new Vector<Object>();
    private LinkGrabberController lgi;

    /**
     * Create a new handler that serves files from a base directory
     * 
     * @param base
     *            directory
     */
    public JDSimpleWebserverTemplateFileRequestHandler(JDSimpleWebserverResponseCreator response) {
        lgi = LinkGrabberController.getInstance();
        this.response = response;
    }

    private void add_all_info(Template t, HashMap<String, String> requestParameter) {
        FilePackage fp;
        String[] ids;
        String Single_Status;
        Integer package_id = 0;
        if (requestParameter.containsKey("all_info")) {
            ids = requestParameter.get("all_info").toString().split("[+]", 2);
            package_id = Formatter.filterInt(ids[0].toString());
            fp = JDUtilities.getController().getPackages().get(package_id);

            addEntry("name", fp.getName());
            addEntry("comment", fp.getComment());
            addEntry("dldirectory", fp.getDownloadDirectory());
            addEntry("packagesize", Formatter.formatReadable(fp.getTotalEstimatedPackageSize()) + " " + fp.getTotalEstimatedPackageSize() + " KB");
            addEntry("loaded", Formatter.formatReadable(fp.getTotalKBLoaded()) + " " + fp.getTotalKBLoaded() + " KB");
            addEntry("links", "");

            DownloadLink next = null;
            int i = 1;
            for (Iterator<DownloadLink> it = fp.getDownloadLinks().iterator(); it.hasNext(); i++) {
                Hashtable<Object, Object> h_info = new Hashtable<Object, Object>();
                next = it.next();
                if (next.isEnabled()) {
                    switch (next.getLinkStatus().getLatestStatus()) {
                    case LinkStatus.FINISHED:
                        Single_Status = "finished";
                        break;
                    case LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS:
                        Single_Status = "running";
                        break;
                    default:
                        Single_Status = "activated";
                    }
                } else {
                    Single_Status = "deactivated";
                }
                double percent = next.getPercent() / 100.0;

                h_info.put("info_percent", f.format(percent));
                h_info.put("download_status", Single_Status);
                h_info.put("info_var", i + ". " + next.getName());
                h_info.put("info_value", Formatter.formatReadable(next.getDownloadSpeed()) + "/s " + f.format(next.getPercent() / 100.0) + " %| " + next.getDownloadCurrent() + "/" + next.getDownloadSize() + " bytes");
                h_info.put("download_id", i - 1);/*
                                                  * von 0 anfangen für js
                                                  * skripte
                                                  */
                v_info.addElement(h_info);
            }
            t.setParam("all_infos", v_info);
        }
    }

    private void add_linkadder_page(Template t, HashMap<String, String> requestParameter) {
        Vector<Object> v, v2 = new Vector<Object>();
        Hashtable<Object, Object> h, h2 = new Hashtable<Object, Object>();
        v = new Vector<Object>();

        LinkGrabberFilePackage filePackage;
        DownloadLink dLink;
        Integer Package_ID;
        Integer Download_ID;
        synchronized (lgi.getPackages()) {
            for (Package_ID = 0; Package_ID < lgi.getPackages().size(); Package_ID++) {
                filePackage = lgi.getPackages().get(Package_ID);

                h = new Hashtable<Object, Object>();
                /* Paket Infos */
                h.put("download_name", filePackage.getName());

                h.put("package_id", Package_ID.toString());

                v2 = new Vector<Object>();

                for (Download_ID = 0; Download_ID < filePackage.getDownloadLinks().size(); Download_ID++) {
                    dLink = filePackage.getDownloadLinks().get(Download_ID);

                    /* Download Infos */
                    h2 = new Hashtable<Object, Object>();

                    h2.put("package_id", Package_ID.toString());
                    h2.put("download_id", Download_ID.toString());
                    h2.put("download_name", dLink.getName());
                    if (dLink.isAvailabilityChecked() && dLink.isAvailable()) {
                        h2.put("download_status", "online");
                    } else {
                        h2.put("download_status", "offline");
                    }

                    h2.put("download_hoster", dLink.getHost());

                    v2.addElement(h2);
                }
                h.put("downloads", v2);
                v.addElement(h);
            }
        }
        // t.setParam("message_status", "show");
        // t.setParam("message", "great work");
        t.setParam("pakete", v);
        if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
            t.setParam("message_status", "show");
            t.setParam("message", "LinkGrabber still Running! Please Reload Page in few Secs!");
        }

    }

    @SuppressWarnings("unchecked")
    private void add_password_list(Template t, HashMap<String, String> requestParameter) {
        String pwlist = "";

        for (OptionalPluginWrapper wrapper : OptionalPluginWrapper.getOptionalWrapper()) {
            if (wrapper.isEnabled() && wrapper.getPlugin().getClass().getName().endsWith("JDUnrar")) {
                Object obj = wrapper.getPlugin().interact("getPasswordList", null);
                if (obj != null && obj instanceof ArrayList) {
                    ArrayList<String> arrayList = new ArrayList<String>();
                    arrayList.addAll((Collection<? extends String>) obj);
                    for (String pw : arrayList) {
                        if (!pw.trim().equals("")) {
                            pwlist += System.getProperty("line.separator") + pw;
                        }
                    }
                }
                break;
            }
        }

        t.setParam("password_list", pwlist);
    }

    private void add_single_info(Template t, HashMap<String, String> requestParameter) {

        /* überprüft ob single_info vorhanden und füllt ggf. dieses template */
        DownloadLink downloadLink;
        Integer download_id = 0;
        Integer package_id = 0;
        String[] ids;
        String Single_Status;
        if (requestParameter.containsKey("single_info")) {
            ids = requestParameter.get("single_info").toString().split("[+]", 2);
            package_id = Formatter.filterInt(ids[0].toString());
            download_id = Formatter.filterInt(ids[1].toString());
            downloadLink = JDUtilities.getController().getPackages().get(package_id).getDownloadLinks().get(download_id);

            addEntry("file", new File(downloadLink.getFileOutput()).getName() + " @ " + downloadLink.getHost());
            if (downloadLink.getFilePackage() != null && downloadLink.getFilePackage().getPassword() != null) {
                addEntry(JDLocale.L("gui.linkinfo.password", "Passwort"), downloadLink.getFilePackage().getPassword());
            }
            if (downloadLink.getFilePackage() != null && downloadLink.getFilePackage().getComment() != null) {
                addEntry(JDLocale.L("gui.linkinfo.comment", "Kommentar"), downloadLink.getFilePackage().getComment());
            }
            if (downloadLink.getFilePackage() != null) {
                addEntry(JDLocale.L("gui.linkinfo.package", "Packet"), downloadLink.getFilePackage().getName());
            }
            if (downloadLink.getDownloadSize() > 0) {
                addEntry(JDLocale.L("gui.linkinfo.filesize", "Dateigröße"), Formatter.formatReadable(downloadLink.getDownloadSize()));
            }
            if (downloadLink.isAborted()) {
                addEntry(JDLocale.L("gui.linkinfo.download", "Download"), JDLocale.L("linkinformation.download.aborted", "Abgebrochen"));
            }
            if (downloadLink.isAvailabilityChecked()) {
                addEntry(JDLocale.L("gui.linkinfo.available", "Verfügbar"), downloadLink.isAvailable() ? JDLocale.L("gui.linkinfo.available.ok", "Datei OK") : JDLocale.L("linkinformation.available.error", "Fehler!"));
            } else {
                addEntry(JDLocale.L("gui.linkinfo.available", "Verfügbar"), JDLocale.L("gui.linkinfo.available.notchecked", "noch nicht überprüft"));
            }
            if (downloadLink.getDownloadSpeed() > 0) {
                addEntry(JDLocale.L("gui.linkinfo.speed", "Geschwindigkeit"), Formatter.formatReadable(downloadLink.getDownloadSpeed()) + " /s");
            }
            if (downloadLink.getFileOutput() != null) {
                addEntry(JDLocale.L("gui.linkinfo.saveto", "Speichern in"), downloadLink.getFileOutput());
            }
            if (downloadLink.getPlugin().getRemainingHosterWaittime() > 0) {
                addEntry(JDLocale.L("gui.linkinfo.waittime", "Wartezeit"), downloadLink.getPlugin().getRemainingHosterWaittime() + " sek");
            }
            if (downloadLink.getLinkStatus().isPluginActive()) {
                addEntry(JDLocale.L("gui.linkinfo.download", "Download"), JDLocale.L("gui.linkinfo.download.underway", " ist in Bearbeitung"));
            } else {
                addEntry(JDLocale.L("gui.linkinfo.download", "Download"), JDLocale.L("gui.linkinfo.download.notunderway", " ist nicht in Bearbeitung"));
            }
            if (!downloadLink.isEnabled()) {
                addEntry(JDLocale.L("gui.linkinfo.download", "Download"), JDLocale.L("gui.linkinfo.download.deactivated", " ist deaktiviert"));
            } else {
                addEntry(JDLocale.L("gui.linkinfo.download", "Download"), JDLocale.L("gui.linkinfo.download.activated", " ist aktiviert"));
            }
            addEntry(JDLocale.L("gui.linkinfo.download", "Status"), downloadLink.getLinkStatus().getStatusString());

            if (downloadLink.isEnabled()) {
                switch (downloadLink.getLinkStatus().getLatestStatus()) {
                case LinkStatus.FINISHED:
                    Single_Status = "finished";
                    break;
                case LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS:
                    Single_Status = "running";
                    break;
                default:
                    Single_Status = "activated";
                }
            } else {
                Single_Status = "deactivated";
            }
            DownloadInterface dl;
            if (downloadLink.getLinkStatus().isPluginActive() && (dl = downloadLink.getDownloadInstance()) != null) {
                addEntry(JDLocale.L("linkinformation.download.chunks.label", "Chunks"), "");
                int i = 1;
                for (Iterator<Chunk> it = dl.getChunks().iterator(); it.hasNext(); i++) {
                    Hashtable<Object, Object> h_info = new Hashtable<Object, Object>();
                    Chunk next = it.next();
                    double percent = next.getPercent() / 100.0;
                    h_info.put("download_status", Single_Status);
                    h_info.put("info_var", JDLocale.L("download.chunks.connection", "Verbindung") + " " + i);
                    h_info.put("info_value", Formatter.formatReadable((int) next.getBytesPerSecond()) + "/s " + f.format(next.getPercent() / 100.0) + " %");
                    h_info.put("info_percent", f.format(percent));
                    h_info.put("download_id", i - 1);/*
                                                      * von 0 anfangen für js
                                                      * skripte
                                                      */
                    v_info.addElement(h_info);
                }
            }
            t.setParam("single_infos", v_info);
        }
        ;
    }

    /*
     * private void addEntryandPercent(String var, String value, double percent)
     * { Hashtable<Object, Object> h_info = new Hashtable<Object, Object>();
     * h_info.put("info_var", var); h_info.put("info_value", value);
     * h_info.put("info_percent", f.format(percent)); v_info.addElement(h_info);
     * }
     */
    private void add_status_page(Template t, HashMap<String, String> requestParameter) {
        Vector<Object> v, v2 = new Vector<Object>();
        Hashtable<Object, Object> h, h2 = new Hashtable<Object, Object>();
        v = new Vector<Object>();
        String value;
        FilePackage filePackage;
        DownloadLink dLink;
        Integer Package_ID;
        Integer Download_ID;
        Double percent = 0.0;
        for (Package_ID = 0; Package_ID < JDUtilities.getController().getPackages().size(); Package_ID++) {
            filePackage = JDUtilities.getController().getPackages().get(Package_ID);

            h = new Hashtable<Object, Object>();
            int status[] = { 0, 0, 0, 0 };
            /* Paket Infos */
            h.put("download_name", filePackage.getName());

            value = "";
            percent = filePackage.getPercent();
            h.put("download_status_percent", f.format(percent));

            if (filePackage.getLinksInProgress() > 0) {
                value = filePackage.getLinksInProgress() + "/" + filePackage.size() + " " + JDLocale.L("gui.treetable.packagestatus.links_active", "aktiv");
            }
            if (filePackage.getTotalDownloadSpeed() > 0) {
                value = "[" + filePackage.getLinksInProgress() + "/" + filePackage.size() + "] " + "ETA " + Formatter.formatSeconds(filePackage.getETA()) + " @ " + Formatter.formatReadable(filePackage.getTotalDownloadSpeed()) + "/s";
            }

            h.put("package_id", Package_ID.toString());
            h.put("download_hoster", value);
            h.put("download_status_text", f.format(percent) + " % (" + Formatter.formatReadable(filePackage.getTotalKBLoaded()) + " / " + Formatter.formatReadable(filePackage.getTotalEstimatedPackageSize()) + ")");

            v2 = new Vector<Object>();

            for (Download_ID = 0; Download_ID < filePackage.getDownloadLinks().size(); Download_ID++) {
                dLink = filePackage.getDownloadLinks().get(Download_ID);

                // Download Infos
                percent = (double) (dLink.getDownloadCurrent() * 100.0 / Math.max(1, dLink.getDownloadSize()));

                h2 = new Hashtable<Object, Object>();
                h2.put("download_status_percent", f.format(percent));
                h2.put("package_id", Package_ID.toString());
                h2.put("download_id", Download_ID.toString());
                h2.put("download_name", dLink.getName());

                h2.put("download_hoster", dLink.getHost());

                if (dLink.isAvailabilityChecked() && !dLink.isAvailable()) {
                    status[0] = 1;
                    h2.put("download_status", "offline");
                } else if (dLink.isEnabled()) {

                    switch (dLink.getLinkStatus().getLatestStatus()) {
                    case LinkStatus.ERROR_FILE_NOT_FOUND:
                        status[0] = 1;
                        h2.put("download_status", "offline");
                        break;
                    case LinkStatus.FINISHED:
                        status[3] = 1;
                        h2.put("download_status", "finished");
                        break;

                    case LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS:
                        status[2] = 1;
                        h2.put("download_status", "running");
                        break;

                    default:
                        status[1] = 1;
                        h2.put("download_status", "activated");
                    }
                } else {
                    status[0] = 1;
                    h2.put("download_status", "deactivated");
                }

                h2.put("download_status_text", f.format(percent) + "% " + dLink.getLinkStatus().getStatusString());
                v2.addElement(h2);
            }

            if (status[3] == 1 && status[2] == 0 && status[1] == 0 && status[0] == 0) {
                h.put("download_status", "finished");
            } else if (status[2] == 1) {
                h.put("download_status", "running");
            } else if (status[1] == 1) {
                h.put("download_status", "activated");
            } else if (status[0] == 1) {
                h.put("download_status", "deactivated");
            }

            h.put("downloads", v2);
            v.addElement(h);
        }
        t.setParam("config_current_speed", JDUtilities.getController().getSpeedMeter() / 1024);

        t.setParam("config_max_downloads", SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2));
        t.setParam("config_max_speed", SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0));

        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false) == true) {
            t.setParam("config_autoreconnect", "");
        } else {
            t.setParam("config_autoreconnect", "checked");
        }

        if (JDUtilities.getController().getDownloadStatus() == JDController.DOWNLOAD_RUNNING) {
            t.setParam("config_startstopbutton", "stop");
        } else {
            t.setParam("config_startstopbutton", "start");
        }

        // t.setParam("message_status", "show");
        // t.setParam("message", "great work");

        t.setParam("pakete", v);
    }

    private void addEntry(String var, String value) {
        Hashtable<Object, Object> h_info = new Hashtable<Object, Object>();
        h_info.put("info_var", var);
        h_info.put("info_value", value);
        v_info.addElement(h_info);
    }

    @SuppressWarnings("deprecation")
    public void handleRequest(String url, HashMap<String, String> requestParameter) {
        try {
            Template t = new Template(JDUtilities.getResourceFile("plugins/webinterface/" + url).getAbsolutePath());

            t.setParam("webinterface_version", JDWebinterface.instance.getPluginID());
            t.setParam("page_refresh", JDWebinterface.getRefreshRate());

            boolean hasUnrar = false;
            for (OptionalPluginWrapper wrapper : OptionalPluginWrapper.getOptionalWrapper()) {
                if (wrapper.isEnabled() && wrapper.getPlugin().getClass().getName().endsWith("JDUnrar")) {
                    hasUnrar = true;
                    break;
                }
            }
            t.setParam("unrar_available", hasUnrar ? "unrarAvailable" : "unrarUnavailable");

            if (url.startsWith("single_info.tmpl") == true) {
                add_single_info(t, requestParameter);
            }
            if (url.startsWith("all_info.tmpl") == true) {
                add_all_info(t, requestParameter);
            }
            if (url.startsWith("index.tmpl") == true) {
                add_status_page(t, requestParameter);
            }
            if (url.startsWith("passwd.tmpl") == true) {
                add_password_list(t, requestParameter);
            }
            if (url.startsWith("link_adder.tmpl") == true) {
                add_linkadder_page(t, requestParameter);
            }

            response.addContent(t.output());
            response.setOk();
        } catch (FileNotFoundException e) {

            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
        } catch (IllegalStateException e) {

            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
        } catch (IOException e) {

            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
        }
    }
}
