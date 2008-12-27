//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.router;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.PasswordAuthentication;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTextField;

import jd.gui.skins.simple.SimpleGUI;

import jd.gui.skins.simple.components.CountdownConfirmDialog;

import jd.nutils.jobber.JDRunnable;

import jd.nutils.Threader;

import jd.config.Configuration;
import jd.controlling.interaction.HTTPLiveHeader;
import jd.gui.skins.simple.ProgressDialog;
import jd.gui.skins.simple.Progressor;
import jd.gui.skins.simple.config.ConfigurationDialog;
import jd.gui.skins.simple.config.FengShuiConfigPanel;
import jd.gui.skins.simple.config.GUIConfigEntry;
import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.Reconnecter;

public class GetRouterInfo {
    private class InternalAuthenticator extends Authenticator {
        private String username, password;

        public InternalAuthenticator(String user, String pass) {
            username = user;
            password = pass;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password.toCharArray());
        }
    }

    private Threader threader = null;
    private Threader th2 = null;
    private boolean cancel = false;
    private CountdownConfirmDialog confirm = null;

    public static boolean isFritzbox(String iPaddress) {
        Browser br = new Browser();
        try {
            String html = br.getPage("http://" + iPaddress);
            if (html.matches("(?is).*fritz.?box.*")) return true;
        } catch (Exception e) {
        }
        return false;
    }

    public void cancel() {
        cancel = true;
        if (threader != null) {
            try {
                threader.interrupt();
            } catch (Exception e) {
                // TODO: handle exception
            }
        } else if (th2 != null) {
            try {
                th2.interrupt();
            } catch (Exception e) {
                // TODO: handle exception
            }
        }

    }

    public static boolean isUpnp(String iPaddress, String port) {
        // curl "http://fritz.box:49000/upnp/control/WANIPConn1" -H
        // "Content-Type: text/xml; charset="utf-8"" -H
        // "SoapAction: urn:schemas-upnp-org:service:WANIPConnection:1#GetStatusInfo"
        // -d "" -s

        Browser br = new Browser();
        try {

            HashMap<String, String> h = new HashMap<String, String>();
            h.put("SoapAction", "urn:schemas-upnp-org:service:WANIPConnection:1#GetStatusInfo");
            h.put("CONTENT-TYPE", "text/xml; charset=\"utf-8\"");
            br.setHeaders(h);
            HTTPConnection con = br.openPostConnection("http://" + iPaddress + ":" + port + "/upnp/control/WANIPConn1", "<?xml version='1.0' encoding='utf-8'?> <s:Envelope s:encodingStyle='http://schemas.xmlsoap.org/soap/encoding/' xmlns:s='http://schemas.xmlsoap.org/soap/envelope/'> <s:Body> <u:GetStatusInfo xmlns:u='urn:schemas-upnp-org:service:WANIPConnection:1' /> </s:Body> </s:Envelope>");

            if (con.getHeaderField(null).contains("200")) return true;
        } catch (Exception e) {
        }
        return false;
    }

    public static boolean validateIP(String iPaddress) {
        final Pattern IP_PATTERN = Pattern.compile("\\b(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b");
        if (IP_PATTERN.matcher(iPaddress).matches())
            return true;
        else {
            try {
                if (InetAddress.getByName(iPaddress).isReachable(1500)) { return true; }
            } catch (UnknownHostException e) {

                e.printStackTrace();
            } catch (IOException e) {

                e.printStackTrace();
            }
        }
        return false;
    }

    public String adress = null;

    public boolean testAll = false;

    @SuppressWarnings("unchecked")
    public static ArrayList<InetAddress> getInterfaces() {
        ArrayList<InetAddress> ret = new ArrayList<InetAddress>();
        try {
            Enumeration e = NetworkInterface.getNetworkInterfaces();

            while (e.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) e.nextElement();

                Enumeration e2 = ni.getInetAddresses();

                while (e2.hasMoreElements()) {
                    InetAddress ip = (InetAddress) e2.nextElement();
                    if (ip.isLoopbackAddress()) break;
                    if (ip.getHostAddress().matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) ret.add(ip);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    private Logger logger = JDUtilities.getLogger();

    private String loginPass;

    private String loginUser;

    public String password = null;

    private Progressor progressBar;

    private Vector<String[]> routerDatas = null;

    public String username = null;

    public GetRouterInfo(Progressor progress) {
        progressBar = progress;
        if (progressBar != null) {
            progressBar.setMaximum(100);
        }
    }

    public static boolean checkport(String host, int port) {
        Socket sock;
        try {
            sock = new Socket(host, port);
            sock.setSoTimeout(200);
            return true;
        } catch (UnknownHostException e) {
        } catch (IOException e) {
        }
        return false;

    }

    public static boolean checkport80(String host) {
        return checkport(host, 80);
    }

    public String getAdress() {
        if (adress != null && !adress.matches("\\s*")) {
            setProgress(100);
            return adress;
        }
        try {

            setProgressText("try to find the router ip");
            String _255 = "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
            String exIP = "(?:" + _255 + "\\.){3}" + _255;
            Pattern pat = Pattern.compile("^\\s*(?:0\\.0\\.0\\.0\\s*){1,2}(" + exIP + ").*");
            Process proc;
            proc = Runtime.getRuntime().exec("netstat -rn");
            InputStream inputstream = proc.getInputStream();
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
            String line;
            while ((line = bufferedreader.readLine()) != null) {
                Matcher m = pat.matcher(line);
                if (m.matches()) {
                    setProgress(100);
                    return m.group(1);
                }
            }
        } catch (Exception e) {
        }
        if (new File("/sbin/route").exists()) {
            try {
                String OS = System.getProperty("os.name").toLowerCase();
                if (OS.indexOf("mac") > -1) {
                    String routingt = JDUtilities.runCommand("/sbin/route", new String[] { "-n", "get", "default" }, "/", 2);
                    Pattern pattern = Pattern.compile("gateway: (\\S*)", Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(routingt);
                    while (matcher.find()) {
                        String hostname = matcher.group(1).trim();
                        if (!hostname.matches("[\\s]*\\*[\\s]*")) {
                            setProgressText("testing " + hostname);
                            try {
                                if (InetAddress.getByName(hostname).isReachable(1500)) {
                                    if (checkport80(hostname)) {
                                        adress = hostname;
                                        setProgress(100);
                                        return adress;
                                    }
                                }
                            } catch (UnknownHostException e) {

                                e.printStackTrace();
                            } catch (IOException e) {

                                e.printStackTrace();
                            }
                        }

                    }
                } else {
                    String routingt = JDUtilities.runCommand("/sbin/route", null, "/", 2).replaceFirst(".*\n.*", "");
                    Pattern pattern = Pattern.compile(".{16}(.{16}).*", Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(routingt);
                    while (matcher.find()) {
                        String hostname = matcher.group(1).trim();
                        if (!hostname.matches("[\\s]*\\*[\\s]*")) {
                            setProgressText("testing " + hostname);
                            try {
                                if (InetAddress.getByName(hostname).isReachable(1500)) {
                                    if (checkport80(hostname)) {
                                        adress = hostname;
                                        setProgress(100);
                                        return adress;
                                    }
                                }
                            } catch (UnknownHostException e) {

                                e.printStackTrace();
                            } catch (IOException e) {

                                e.printStackTrace();
                            }
                        }

                    }
                }
            } catch (Exception e) {
                // TODO: handle exception
            }

        }
        final Vector<String> hosts = new Vector<String>();
        hosts.add("fritz.fonwlan.box");
        hosts.add("speedport.ip");
        hosts.add("fritz.box");
        hosts.add("dsldevice.lan");
        hosts.add("speedtouch.lan");
        hosts.add("mygateway1.ar7");
        hosts.add("fritz.fon.box");
        hosts.add("home");
        hosts.add("arcor.easybox");
        hosts.add("fritz.slwlan.box");
        hosts.add("eumex.ip");
        hosts.add("easy.box");
        hosts.add("my.router");
        hosts.add("fritz.fon");
        hosts.add("router");
        hosts.add("mygateway.ar7");
        hosts.add("login.router");
        hosts.add("SX541");
        hosts.add("SE515.home");
        hosts.add("sinus.ip");
        hosts.add("fritz.wlan.box");
        hosts.add("my.siemens");
        hosts.add("local.gateway");
        hosts.add("congstar.box");
        hosts.add("login.modem");
        hosts.add("homegate.homenet.telecomitalia.it");
        hosts.add("SE551");
        hosts.add("home.gateway");
        hosts.add("alice.box");
        hosts.add("www.brntech.com.tw");
        hosts.add("buffalo.setup");
        hosts.add("vood.lan");
        hosts.add("DD-WRT");
        hosts.add("versatel.modem");
        hosts.add("myrouter.home");
        hosts.add("MyDslModem.local.lan");
        hosts.add("alicebox");
        hosts.add("HSIB.home");
        hosts.add("AolynkDslRouter.local.lan");
        hosts.add("SL2141I.home");
        hosts.add("e.home");
        hosts.add("dsldevice.domain.name");
        hosts.add("192.168.1.1");
        hosts.add("192.168.2.1");
        hosts.add("192.168.0.1");
        hosts.add("192.168.178.1");
        hosts.add("192.168.1.254");
        hosts.add("10.0.0.138");
        hosts.add("10.0.0.2");
        hosts.add("192.168.123.254");
        hosts.add("192.168.1.2");
        hosts.add("192.168.0.254");
        hosts.add("10.0.0.1");
        hosts.add("192.168.10.1");
        hosts.add("192.168.220.1");
        hosts.add("192.168.254.254");
        hosts.add("192.168.0.2");
        hosts.add("192.168.100.1");
        hosts.add("192.168.1.100");
        hosts.add("10.1.1.1");
        hosts.add("192.168.0.100");
        hosts.add("192.168.3.1");
        hosts.add("192.168.5.1");
        hosts.add("192.168.2.2");
        hosts.add("192.168.11.1");
        hosts.add("192.168.1.10");
        hosts.add("192.168.0.10");
        hosts.add("192.168.0.253");
        hosts.add("192.168.7.1");
        hosts.add("192.168.182.1");
        hosts.add("192.168.2.254");
        hosts.add("192.168.178.2");
        hosts.add("192.168.15.1");
        hosts.add("192.168.1.5");
        hosts.add("192.168.0.3");
        hosts.add("192.168.123.1");
        hosts.add("192.168.1.253");
        hosts.add("192.168.0.99");
        hosts.add("172.16.0.1");
        hosts.add("192.168.4.1");
        String ip;

        for (InetAddress ia : getInterfaces()) {
            try {

                if (GetRouterInfo.validateIP(ia.getHostAddress() + "")) {
                    ip = ia.getHostAddress();

                    if (ip != null && ip.lastIndexOf(".") != -1) {
                        String host = ip.substring(0, ip.lastIndexOf(".")) + ".";
                        for (int i = 0; i < 255; i++) {
                            String lhost = host + i;
                            if (!lhost.equals(ip) && !hosts.contains(lhost)) {
                                hosts.add(lhost);
                            }

                        }
                    }
                }
                hosts.remove(ia.getHostName());
                hosts.remove(ia.getAddress());
            } catch (Exception exc) {
            }
        }

        final int size = hosts.size();
        threader = new Threader();
        for (int i = 0; i < size && !cancel; i++) {
            final int d = i;
            threader.add(new JDRunnable() {

                public void go() throws Exception {

                    final String hostname = hosts.get(d);
                    setProgressText("testing " + hostname);
                    try {
                        if (InetAddress.getByName(hostname).isReachable(1500)) {
                            if (checkport80(hostname)) {
                                adress = hostname;
                                setProgress(100);
                                threader.interrupt();
                            }
                        }

                    } catch (IOException e) {
                    }
                    setProgress(d * 100 / size);

                }
            });

        }
        threader.startAndWait();
        setProgress(100);
        return adress;

    }

    @SuppressWarnings("unchecked")
    public static Map sortByIntegrety(Map<RInfo, Integer> map) {
        LinkedList list = new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                if (((Comparable) ((Map.Entry<RInfo, Integer>) (o1)).getValue()).equals(((Map.Entry<RInfo, Integer>) (o2)).getValue())) {
                    return ((Comparable) ((Map.Entry<RInfo, Integer>) (o2)).getKey().getIntegrety()).compareTo(((Map.Entry<RInfo, Integer>) (o1)).getKey().getIntegrety());
                } else
                    return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
            }
        });
        LinkedHashMap result = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private RInfo checkrouters(HashMap<RInfo, Integer> routers) {
        int retries = JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_HTTPSEND_RETRIES, 5);
        int wipchange = JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_HTTPSEND_WAITFORIPCHANGE, 20);
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_RETRIES, 0);
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_WAITFORIPCHANGE, 10);
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_USER, username);
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_PASS, password);
        int size = routers.size();
        int i = 0;
        for (Entry<RInfo, Integer> info2 : routers.entrySet()) {
            if (cancel) return null;
            if (info2.getKey().getReconnectMethode() != null) {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_RECONNECT_TYPE, RouterInfoCollector.RECONNECTTYPE_LIVE_HEADER);
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_REQUESTS, info2.getKey().getReconnectMethode());
            } else if (info2.getKey().getReconnectMethodeClr() != null) {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_RECONNECT_TYPE, RouterInfoCollector.RECONNECTTYPE_CLR);
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_REQUESTS_CLR, info2.getKey().getReconnectMethodeClr());
            } else
                continue;
            setProgressText("Testing router: " + info2.getKey().getRouterName());
            setProgress(i++ * 100 / size);

            JDUtilities.getConfiguration().save();
            if (Reconnecter.waitForNewIP(1)) {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_RETRIES, retries);
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_WAITFORIPCHANGE, wipchange);
                JDUtilities.getConfiguration().save();
                setProgress(100);
                return info2.getKey();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public RInfo getRouterData() {

        setProgressText("collect routerinformations");

        final RInfo infos = RouterInfoCollector.getRInfo(RouterInfoCollector.RInfo_ROUTERSEARCH);
        infos.setReconnectMethode(null);
        infos.setReconnectMethodeClr(null);
        th2 = new Threader();
        final class isalvs {
            boolean isAlv = true;
        }
        final isalvs isalv = new isalvs();
        final JDRunnable jupnp = new JDRunnable() {

            public void go() throws Exception {

                try {
                    UPnPInfo upnp = new UPnPInfo(infos.getRouterIP());
                    if (upnp.met != null) {
                        infos.setUPnPSCPDs(upnp.SCPDs);
                        infos.setReconnectMethode(upnp.met);

                    }
                } catch (Exception e) {
                    // TODO: handle exception
                }
                isalv.isAlv = false;
            }

        };

        th2.getBroadcaster().addListener(th2.new WorkerListener() {

            @Override
            public void onThreadException(Threader th, JDRunnable job, Exception e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onThreadFinished(Threader th, JDRunnable runnable) {
                if (runnable == jupnp) {
                    isalv.isAlv = false;
                    th2.notify();
                }

            }

            @Override
            public void onThreadStarts(Threader threader, JDRunnable runnable) {
                // TODO Auto-generated method stub

            }
        });
        th2.add(jupnp);
        th2.add(new JDRunnable() {

            public void go() throws Exception {
                try {

                    HashMap<RInfo, Integer> routers = new HashMap<RInfo, Integer>();
                    HashMap<RInfo, Integer> experimentalRouters = new HashMap<RInfo, Integer>();
                    int upnp = 0;
                    Browser br = new Browser();
                    HashMap<String, String> he = new HashMap<String, String>();
                    if (infos.getRouterHost() != null) he.put("RouterHost", infos.getRouterHost());
                    if (infos.getRouterHost() != null) he.put("RouterMAC", infos.getRouterMAC());
                    if (infos.getPageHeader() != null) he.put("PageHeader", SQLRouterData.replaceTimeStamps(infos.getPageHeader()));
                    if (infos.getRouterErrorPage() != null) he.put("RouterErrorPage", SQLRouterData.replaceTimeStamps(infos.getRouterErrorPage()));
                    he.put("HTMLTagCount", "" + infos.countHtmlTags());
                    ArrayList<RInfo> ra;
                    try {
                        setProgressText("download similar routermethods");
                        String st = br.postPage("http://service.jdownloader.net/routerdb/getRouters.php", he);
                        // String st =
                        // br.postPage("http://localhost/router/getRouters.php",
                        // he);
                        // System.out.println(st);
                        ra = (ArrayList<RInfo>) JDUtilities.xmlStringToObjekt(st);

                    } catch (Exception e) {
                        return;
                    }
                    setProgressText("sort routermethods");
                    for (RInfo info : ra) {
                        // System.out.println(info.getReconnectMethode());
                        if (info.isHaveUpnpReconnect()) upnp++;

                        if (info.getReconnectMethodeClr() != null) {
                            Integer b = info.compare(infos);
                            info.setIntegrety(200);
                            routers.put(info, b);
                        } else if (info.getReconnectMethode() != null) {
                            Integer b = info.compare(infos);
                            // System.out.println(info.getRouterName());
                            if (info.getIntegrety() > 3) {
                                routers.put(info, b);
                            } else {
                                experimentalRouters.put(info, b);
                            }
                        }
                    }

                    routers = (HashMap<RInfo, Integer>) sortByIntegrety(routers);
                    HashMap<String, RInfo> methodes = new HashMap<String, RInfo>();
                    Iterator<Entry<RInfo, Integer>> inter = routers.entrySet().iterator();
                    while (inter.hasNext()) {
                        Map.Entry<jd.router.RInfo, java.lang.Integer> entry = (Map.Entry<jd.router.RInfo, java.lang.Integer>) inter.next();
                        RInfo meth = methodes.get(entry.getKey().getReconnectMethode());
                        if (meth != null) {
                            meth.setIntegrety(meth.getIntegrety() + entry.getKey().getIntegrety());
                            inter.remove();
                        } else
                            methodes.put(entry.getKey().getReconnectMethode(), entry.getKey());
                    }
                    routers = (HashMap<RInfo, Integer>) sortByIntegrety(routers);
                    experimentalRouters = (HashMap<RInfo, Integer>) sortByIntegrety(experimentalRouters);
                    methodes = new HashMap<String, RInfo>();
                    inter = experimentalRouters.entrySet().iterator();
                    while (inter.hasNext()) {
                        Map.Entry<jd.router.RInfo, java.lang.Integer> entry = (Map.Entry<jd.router.RInfo, java.lang.Integer>) inter.next();
                        RInfo meth = methodes.get(entry.getKey().getReconnectMethode());
                        if (meth != null) {
                            meth.setIntegrety(meth.getIntegrety() + entry.getKey().getIntegrety());
                            inter.remove();
                        } else
                            methodes.put(entry.getKey().getReconnectMethode(), entry.getKey());
                    }
                    experimentalRouters = (HashMap<RInfo, Integer>) sortByIntegrety(experimentalRouters);
                    if (upnp > 0) {
                        while (isalv.isAlv) {
                            try {
                                wait();
                            } catch (Exception e) {
                                // TODO: handle exception
                            }

                        }
                        if (infos.getReconnectMethode() == null) {

                            confirm = new CountdownConfirmDialog(SimpleGUI.CURRENTGUI.getFrame(), JDLocale.LF("gui.config.liveHeader.warning.upnpinactive", "Bitte aktivieren sie fals vorhanden Upnp in den Netzwerkeinstellungen ihres Routers <br><a href=\"http://%s\">zum Router</a><br><a href=\"http://wiki.jdownloader.org/index.php?title=Router_Upnp\">Wikiartikel: Upnp Routern</a><br>drücken sie Ok wenn sie Upnp aktiviert haben oder abbrechen wenn sie fortfahren wollen!", infos.getRouterIP()), 600, false, CountdownConfirmDialog.STYLE_CANCEL|CountdownConfirmDialog.STYLE_OK|CountdownConfirmDialog.STYLE_STOP_COUNTDOWN|CountdownConfirmDialog.STYLE_NOTALWAYSONTOP);
                            if (confirm.result) {
                                try {
                                    setProgressText("testing upnp");
                                    for (int i = 0; i < 30 && !cancel; i++) {
                                        setProgress(i++ * 100 / 30);
                                        UPnPInfo upnpd = new UPnPInfo(infos.getRouterIP(), 10000);
                                        if (upnpd.met != null) {
                                            infos.setUPnPSCPDs(upnpd.SCPDs);
                                            infos.setReconnectMethode(upnpd.met);
                                            break;
                                        }
                                    }
                                } catch (Exception e) {
                                    // TODO: handle exception
                                }
                            }
                        }
                    }
                    if (infos.getReconnectMethode() != null) return;
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_IP, infos.getRouterIP());
                    RInfo router = checkrouters(routers);
                    if (router == null && !cancel) {
                        if (experimentalRouters.size() > 0) {
                            boolean conf = JDUtilities.getGUI().showConfirmDialog(JDLocale.L("gui.config.liveHeader.warning.experimental", "Möchten sie experimentelle Reconnectmethoden testen?\r\nExperimentelle Reconnectmethoden sind nicht so zuverlässig\r\nund beeinflussen möglicherweise die Routereinstellungen"));
                            if (conf) router = checkrouters(experimentalRouters);
                        }
                    }
                    setProgress(100);
                    if (router != null) {
                        infos.setRouterName(router.getRouterName());
                        infos.setReconnectMethode(router.getReconnectMethode());
                        infos.setReconnectMethodeClr(router.getReconnectMethodeClr());
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                setProgress(100);
            }
        });
        th2.startAndWait();
        if (infos.getReconnectMethode() != null || infos.getReconnectMethodeClr() != null)
            return infos;
        else
            return null;
    }

    public Vector<String[]> getRouterDatas() {
        if (routerDatas != null) return routerDatas;
        if (getAdress() == null) return null;

        try {
            // progress.setStatusText("Load possible RouterDatas");
            Authenticator.setDefault(new InternalAuthenticator(loginUser, loginPass));

            Browser br = new Browser();
            String html = br.getPage("http://" + adress).toLowerCase();
            Vector<String[]> routerData = new HTTPLiveHeader().getLHScripts();
            Vector<String[]> retRouterData = new Vector<String[]>();
            for (int i = 0; i < routerData.size(); i++) {
                String[] dat = routerData.get(i);
                if (dat.length > 3) if (html.contains(dat[0].toLowerCase()) || html.contains(dat[1].toLowerCase()) || html.matches(dat[3])) {
                    retRouterData.add(dat);
                } else {
                    if (html.contains(dat[0].toLowerCase()) || html.contains(dat[1].toLowerCase())) {
                        retRouterData.add(dat);
                    }
                }
            }
            routerDatas = retRouterData;
            return retRouterData;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void setLoginPass(String text) {
        loginPass = text;
    }

    public void setLoginUser(String text) {
        loginUser = text;
    }

    private void setProgress(int val) {
        if (progressBar != null) {
            progressBar.setValue(val);
        } else {
            logger.info(val + "%");
        }
    }

    private void setProgressText(String text) {
        if (progressBar != null) {
            progressBar.setString(text);
            progressBar.setStringPainted(true);
        } else {
            logger.info(text);
        }
    }

    public static void autoConfig(final Object pass, final Object user, final Object ip, final Object routerScript) {

        final ProgressDialog progress = new ProgressDialog(ConfigurationDialog.PARENTFRAME, JDLocale.L("gui.config.liveHeader.progress.message", "jDownloader sucht nach Ihren Routereinstellungen"), null, false, true);
        final GetRouterInfo routerInfo = new GetRouterInfo(progress);
        final Thread th = new Thread() {
            @Override
            public void run() {
                String pw = "";
                String username = "";
                String ipadresse = null;
                if (pass instanceof GUIConfigEntry) {
                    pw = (String) ((GUIConfigEntry) pass).getText();
                    username = (String) ((GUIConfigEntry) user).getText();
                    ipadresse = (String) ((GUIConfigEntry) ip).getText();
                } else if (pass instanceof JTextField) {
                    pw = (String) ((JTextField) pass).getText();
                    username = (String) ((JTextField) user).getText();
                    ipadresse = (String) ((JTextField) ip).getText();
                }
                if (ipadresse != null && !ipadresse.matches("\\s*")) JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP, ipadresse);

                routerInfo.setLoginPass(pw);
                routerInfo.setLoginUser(username);
                if (username != null && !username.matches("[\\s]*")) {
                    routerInfo.username = username;
                }
                if (pw != null && !pw.matches("[\\s]*")) {
                    routerInfo.password = pw;
                }
                RInfo data = routerInfo.getRouterData();
                if (data == null) {
                    progress.setVisible(false);
                    progress.dispose();
                    JDUtilities.getGUI().showMessageDialog(JDLocale.L("gui.config.liveHeader.warning.notFound", "jDownloader konnte ihre Routereinstellung nicht automatisch ermitteln."));
                    return;
                }
                if (routerScript != null && routerScript instanceof GUIConfigEntry) {
                    ((GUIConfigEntry) routerScript).setData(data.getReconnectMethode());
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_ROUTERNAME, data.getRouterName());
                }

                else if (routerScript != null && routerScript instanceof FengShuiConfigPanel) {
                    FengShuiConfigPanel m = ((FengShuiConfigPanel) routerScript);
                    m.routername.setText(data.getRouterName());
                    m.Reconnectmethode = data.getReconnectMethode();
                    m.ReconnectmethodeClr = data.getReconnectMethodeClr();
                }
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_ROUTERNAME, data.getRouterName());
                progress.setVisible(false);
                progress.dispose();
                JDUtilities.getGUI().showMessageDialog(JDLocale.L("gui.config.liveHeader.warning.yourRouter", "Sie haben einen") + " " + data.getRouterName());

            }
        };
        th.start();
        progress.setThread(th);
        progress.setVisible(true);
        new Thread(new Runnable() {
            public void run() {
                while (th.isAlive()) {
                    try {
                        th.wait();
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
                routerInfo.cancel();
            }
        }).start();

    }

    public static void main(String[] args) {
        System.out.println(new GetRouterInfo(null).getAdress());
    }
}
