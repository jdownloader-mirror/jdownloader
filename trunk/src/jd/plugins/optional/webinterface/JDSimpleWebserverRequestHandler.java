package jd.plugins.optional.webinterface;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.DistributeData;
import jd.event.ControlEvent;
import jd.gui.skins.simple.LinkGrabber;
import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.unrar.JUnrar;
import jd.utils.JDUtilities;

public class JDSimpleWebserverRequestHandler {

    private HashMap<String, String> headers;
    private JDSimpleWebserverResponseCreator response;

    private SubConfiguration guiConfig = JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
    private static final String PROPERTY_AUTOPACKAGE = "PROPERTY_AUTOPACKAGE";

    public static final String PROPERTY_AUTOPACKAGE_LIMIT = "AUTOPACKAGE_LIMIT";

    public static final String PROPERTY_ONLINE_CHECK = "DO_ONLINE_CHECK";

    private Logger logger = JDUtilities.getLogger();

    public JDSimpleWebserverRequestHandler(HashMap<String, String> headers, JDSimpleWebserverResponseCreator response) {
        this.response = response;
        this.headers = headers;
    }

    private String removeExtension(String a) {
        // logger.finer("file " + a);
        if (a == null) return a;
        a = a.replaceAll("\\.part([0-9]+)", "");
        a = a.replaceAll("\\.html", "");
        a = a.replaceAll("\\.htm", "");
        int i = a.lastIndexOf(".");
        // logger.info("FOund . " + i);
        String ret;
        if (i <= 1 || (a.length() - i) > 5) {
            ret = a.toLowerCase().trim();
        } else {
            // logger.info("Remove ext");
            ret = a.substring(0, i).toLowerCase().trim();
        }

        if (a.equals(ret)) return ret;
        return (ret);

    }

    private int comparepackages(String a, String b) {

        int c = 0;
        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            if (a.charAt(i) == b.charAt(i)) c++;
        }

        if (Math.min(a.length(), b.length()) == 0) return 0;
        // logger.info("comp: " + a + " <<->> " + b + "(" + (c * 100) /
        // (b.length()) + ")");
        return (c * 100) / (b.length());
    }

    private String getSimString(String a, String b) {

        String ret = "";
        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            if (a.charAt(i) == b.charAt(i)) {
                ret += a.charAt(i);
            } else {
                // return ret;
            }
        }
        return ret;
    }

    private void attachLinkTopackage(DownloadLink link) {
        synchronized (JDWebinterface.Link_Adder_Packages) {
            int bestSim = 0;
            int bestIndex = -1;
            // logger.info("link: " + link.getName());
            for (int i = 0; i < JDWebinterface.Link_Adder_Packages.size(); i++) {

                int sim = comparepackages(JDWebinterface.Link_Adder_Packages.get(i).getName(), removeExtension(link.getName()));
                if (sim > bestSim) {
                    bestSim = sim;
                    bestIndex = i;
                }
            }
            // logger.info("Best sym: "+bestSim);
            if (bestSim < guiConfig.getIntegerProperty(PROPERTY_AUTOPACKAGE_LIMIT, 98)) {

                FilePackage fp = new FilePackage();
                fp.setName(removeExtension(link.getName()));
                fp.add(link);
                JDWebinterface.Link_Adder_Packages.add(fp);
            } else {
                // logger.info("Found package "
                // +JDWebinterface.Link_Adder_Packages.get(bestIndex).getName());
                String newpackageName = getSimString(JDWebinterface.Link_Adder_Packages.get(bestIndex).getName(), removeExtension(link.getName()));
                JDWebinterface.Link_Adder_Packages.get(bestIndex).setName(newpackageName);
                JDWebinterface.Link_Adder_Packages.get(bestIndex).add(link);

            }
        }
    }

    @SuppressWarnings("static-access")
    public void handle() {

        String request = headers.get(null);
        // logger.info(request);
        String[] requ = request.split(" ");

        String method = requ[0];
        String cPath = requ[1];
        String protocol = requ[2];
        String path, querry;
        path = cPath.substring(1);
        String[] params;
        HashMap<String, String> requestParameter = new HashMap<String, String>();

        /* bekanntgebung der mehrfach belegbaren parameter */
        requestParameter.put("package_all_downloads_counter", "0");
        requestParameter.put("package_single_download_counter", "0");
        requestParameter.put("package_all_add_counter", "0");
        requestParameter.put("package_single_add_counter", "0");

        if (cPath.indexOf("?") >= 0) {
            querry = cPath.substring(cPath.indexOf("?") + 1);
            path = cPath.substring(1, cPath.indexOf("?"));
            params = querry.split("\\&");

            for (String entry : params) {
                entry = entry.trim();
                int index = entry.indexOf("=");
                String key = entry;

                String value = null;
                if (index >= 0) {
                    key = entry.substring(0, index);
                    value = entry.substring(index + 1);
                }

                if (requestParameter.containsKey(key) || requestParameter.containsKey(key + "_counter")) {
                    /*
                     * keys mit _counter können mehrfach belegt werden, müssen
                     * vorher aber bekannt gegeben werden
                     */
                    if (requestParameter.containsKey(key + "_counter")) {
                        Integer keycounter = 0;
                        keycounter = JDUtilities.filterInt(requestParameter.get(key + "_counter"));
                        keycounter++;
                        requestParameter.put(key + "_counter", keycounter.toString());
                        requestParameter.put(key + "_" + keycounter.toString(), value);
                    }
                    ;

                } else
                    requestParameter.put(key, value);
            }
        }
        /*logger.info(requestParameter.toString());*/
        String url = path.replaceAll("\\.\\.", "");
        
        if (url.startsWith("link_adder.tmpl")==true){
            /*packages-namen des link-adders aktuell halten*/
        synchronized (JDWebinterface.Link_Adder_Packages) {            
            for (int i = 0; i <= JDWebinterface.Link_Adder_Packages.size(); i++) {
                if (requestParameter.containsKey("adder_package_name_" + i)) {
                    JDWebinterface.Link_Adder_Packages.get(i).setName(JDUtilities.htmlDecode(requestParameter.get("adder_package_name_" + i).toString()));
                }
            }
        }
        }
        
        /* parsen der paramter */
        if (requestParameter.containsKey("do")) {
            if (requestParameter.get("do").compareToIgnoreCase("submit") == 0) {
                logger.info("submit wurde gedrückt");
                if (requestParameter.containsKey("speed")) {
                    int setspeed = JDUtilities.filterInt(requestParameter.get("speed"));
                    if (setspeed < 0) setspeed = 0;
                    JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, setspeed);
                }
                ;

                if (requestParameter.containsKey("maxdls")) {
                    int maxdls = JDUtilities.filterInt(requestParameter.get("maxdls"));
                    if (maxdls < 1) maxdls = 1;
                    JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, maxdls);
                }

                if (requestParameter.containsKey("autoreconnect")) {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
                } else
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, true);

                if (requestParameter.containsKey("package_single_add_counter")) {
                    synchronized (JDWebinterface.Link_Adder_Packages) {
                        /* aktionen in der adder liste ausführen */
                        Integer download_id = 0;
                        Integer package_id = 0;
                        String[] ids;
                        int counter_max = JDUtilities.filterInt(requestParameter.get("package_single_add_counter"));
                        int counter_index = 0;
                        DownloadLink link;
                        int index;
                        Vector<DownloadLink> links = new Vector<DownloadLink>();
                        for (counter_index = 1; counter_index <= counter_max; counter_index++) {
                            if (requestParameter.containsKey("package_single_add_" + counter_index)) {
                                ids = requestParameter.get("package_single_add_" + counter_index).toString().split("[+]", 2);
                                package_id = JDUtilities.filterInt(ids[0].toString());
                                download_id = JDUtilities.filterInt(ids[1].toString());
                                links.add(JDWebinterface.Link_Adder_Packages.get(package_id).get(download_id));
                            }
                        }
                        if (requestParameter.containsKey("selected_dowhat_link_adder")) {
                            String dowhat = requestParameter.get("selected_dowhat_link_adder");
                            if (dowhat.compareToIgnoreCase("remove") == 0) {
                                /* entfernen */
                                logger.info("entfernen aus add liste");
                                for (Iterator<DownloadLink> it = links.iterator(); it.hasNext();) {
                                    link = it.next();
                                    link.getFilePackage().remove(link);
                                }
                            } else if (dowhat.compareToIgnoreCase("add") == 0) {
                                /* link adden */
                                logger.info("adden aus add liste");
                                for (Iterator<DownloadLink> it = links.iterator(); it.hasNext();) {
                                    link = it.next();
                                    FilePackage fp = null;

                                    for (int i = 0; i < JDUtilities.getController().getPackages().size(); i++) {
                                        /*
                                         * files mit selben packages namen
                                         * sollen auch ins gleiche package?!
                                         */
                                        if (link.getFilePackage().getName().compareToIgnoreCase(JDUtilities.getController().getPackages().get(i).getName()) == 0) {
                                            fp = JDUtilities.getController().getPackages().get(i);
                                            /*
                                             * package bereits im controller
                                             * gefunden
                                             */
                                        }
                                    }
                                    if (fp == null) {
                                        /* neues package erzeugen */
                                        fp = new FilePackage();
                                        fp.setName(link.getFilePackage().getName());                                        
                                        /* use packagename as subfolder */
                                        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, false)) {
                                            File file = new File(new File(fp.getDownloadDirectory()), fp.getName());
                                            if (!file.exists()) {
                                                file.mkdirs();
                                            }
                                            if (file.exists()) {
                                                fp.setDownloadDirectory(file.getAbsolutePath());
                                            } else {
                                                fp.setDownloadDirectory(fp.getDownloadDirectory());
                                            }
                                        }
                                    }
                                    fp.add(link);
                                    link.setFilePackage(fp);
                                    JDUtilities.getController().addLink(link);
                                }
                                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));

                            }
                            /* leere packages aus der add liste entfernen */
                            /*von oben nach unten, damit keine fehler entstehen, falls mittendrin was gelöscht wird*/
                            for (index = JDWebinterface.Link_Adder_Packages.size()-1; index >=0 ; index--) {
                                if (JDWebinterface.Link_Adder_Packages.get(index).size() == 0) JDWebinterface.Link_Adder_Packages.remove(index);
                            }
                        }
                    }
                }
                ;

                if (requestParameter.containsKey("package_single_download_counter")) {
                    /* aktionen in der download-liste ausführen */
                    Integer download_id = 0;
                    Integer package_id = 0;
                    String[] ids;
                    int counter_max = JDUtilities.filterInt(requestParameter.get("package_single_download_counter"));
                    int counter_index = 0;
                    DownloadLink link;
                    Vector<DownloadLink> links = new Vector<DownloadLink>();
                    for (counter_index = 1; counter_index <= counter_max; counter_index++) {
                        if (requestParameter.containsKey("package_single_download_" + counter_index)) {
                            ids = requestParameter.get("package_single_download_" + counter_index).toString().split("[+]", 2);
                            package_id = JDUtilities.filterInt(ids[0].toString());
                            download_id = JDUtilities.filterInt(ids[1].toString());

                            links.add(JDUtilities.getController().getPackages().get(package_id).getDownloadLinks().get(download_id));
                        }
                    }

                    if (requestParameter.containsKey("selected_dowhat_index")) {
                        String dowhat = requestParameter.get("selected_dowhat_index");
                        if (dowhat.compareToIgnoreCase("activate") == 0) {
                            /* aktivieren */
                            for (Iterator<DownloadLink> it = links.iterator(); it.hasNext();) {
                                link = it.next();
                                link.setEnabled(true);
                            }
                            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
                        }
                        if (dowhat.compareToIgnoreCase("deactivate") == 0) {
                            /* deaktivieren */
                            for (Iterator<DownloadLink> it = links.iterator(); it.hasNext();) {
                                link = it.next();
                                link.setEnabled(false);
                            }
                            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
                        }
                        if (dowhat.compareToIgnoreCase("reset") == 0) {
                            /*
                             * reset
                             */
                            for (Iterator<DownloadLink> it = links.iterator(); it.hasNext();) {
                                link = it.next();
                                link.setStatus(DownloadLink.STATUS_TODO);
                                link.setStatusText("");
                                link.reset();
                            }
                            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
                        }
                        if (dowhat.compareToIgnoreCase("remove") == 0) {
                            /*
                             * entfernen
                             */
                            JDUtilities.getController().removeDownloadLinks(links);
                            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));
                        }
                        if (dowhat.compareToIgnoreCase("abort") == 0) {
                            /*
                             * abbrechen
                             */
                            for (Iterator<DownloadLink> it = links.iterator(); it.hasNext();) {
                                link = it.next();
                                link.setAborted(true);
                            }
                            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
                        }

                    }

                }

            } else if (requestParameter.get("do").compareToIgnoreCase("reconnect+now") == 0) {
                logger.info("reconnect now wurde gedrückt");
                JDUtilities.getController().requestReconnect();
            } else if (requestParameter.get("do").compareToIgnoreCase("close+jd") == 0) {
                logger.info("close jd wurde gedrückt");
                class JDClose implements Runnable { /* zeitverzögertes beenden */
                    JDClose() {
                        new Thread(this).start();
                    }

                    public void run() {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        JDUtilities.getController().exit();
                    }
                }
                @SuppressWarnings("unused")
                JDClose jds = new JDClose();

            } else if (requestParameter.get("do").compareToIgnoreCase("start+downloads") == 0) {
                logger.info("start wurde gedrückt");
                JDUtilities.getController().startDownloads();
            } else if (requestParameter.get("do").compareToIgnoreCase("stop+downloads") == 0) {
                logger.info("stop wurde gedrückt");
                JDUtilities.getController().stopDownloads();
            } else if (requestParameter.get("do").compareToIgnoreCase("restart+jd") == 0) {
                logger.info("restart wurde gedrückt");
                class JDRestart implements Runnable {
                    /*
                     * zeitverzögertes neustarten
                     */
                    JDRestart() {
                        new Thread(this).start();
                    }

                    public void run() {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        JDUtilities.restartJD();
                    }
                }
                @SuppressWarnings("unused")
                JDRestart jdr = new JDRestart();

            } else if (requestParameter.get("do").compareToIgnoreCase("add") == 0) {
                logger.info("add wurde gedrückt");

                if (requestParameter.containsKey("addlinks")) {
                    /*
                     * TODO: mehr add features
                     */
                    String AddLinks = JDUtilities.htmlDecode(requestParameter.get("addlinks"));
                    Vector<DownloadLink> LinkstoAdd = new DistributeData(AddLinks).findLinks();
                    for (int i = 0; i < LinkstoAdd.size(); i++) {
                        attachLinkTopackage(LinkstoAdd.get(i));
                    }
                }
            }
            ;

        }
        /* passwortliste verändern */
        if (requestParameter.containsKey("passwd")) {
            if (requestParameter.get("passwd").compareToIgnoreCase("save") == 0) {
                logger.info("passwd save wurde gedrückt");
                if (requestParameter.containsKey("password_list")) {
                    String password_list = JDUtilities.htmlDecode(requestParameter.get("password_list"));
                    JUnrar unrar = new JUnrar(false);
                    unrar.editPasswordlist(JDUtilities.splitByNewline(password_list));
                }
            }
        }       

        File fileToRead = JDUtilities.getResourceFile("plugins/webinterface/" + url);
        if (!fileToRead.isFile()) {
            /*
             * default soll zur index.tmpl gehen, fall keine angabe gemacht
             * wurde
             */
            String tempurl = url + "index.tmpl";
            File fileToRead2 = JDUtilities.getResourceFile("plugins/webinterface/" + tempurl);
            if (fileToRead2.isFile()) {
                url = tempurl;
                fileToRead = JDUtilities.getResourceFile("plugins/webinterface/" + url);
            }
            ;
        }

        if (!fileToRead.exists()) {
            response.setNotFound(url);
        } else {
            if (url.endsWith(".tmpl")) {
                JDSimpleWebserverTemplateFileRequestHandler filerequest;
                filerequest = new JDSimpleWebserverTemplateFileRequestHandler(this.response);
                filerequest.handleRequest(url, requestParameter);
            } else {
                JDSimpleWebserverStaticFileRequestHandler filerequest;
                filerequest = new JDSimpleWebserverStaticFileRequestHandler(this.response);
                filerequest.handleRequest(url, requestParameter);
            }
            ;
        }
        // logger.info("RequestParams: " + requestParameter);

    }
}
