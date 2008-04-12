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
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.router;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.controlling.interaction.HTTPLiveHeader;
import jd.gui.skins.simple.ProgressDialog;

import jd.plugins.Plugin;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class GetRouterInfo {
    public String            password    = null;

    public String            username    = null;

    public String            adress      = null;

    public boolean           cancel      = false;

    private Logger           logger      = JDUtilities.getLogger();

    private Vector<String[]> routerDatas = null;

    private ProgressDialog   progressBar;

    public GetRouterInfo(ProgressDialog progress) {
        this.progressBar = progress;
        if (progressBar != null) progressBar.setMaximum(100);
    }

    private void setProgressText(String text) {

        if (progressBar != null) {
            progressBar.setString(text);
            this.progressBar.setStringPainted(true);
        }
        else {
            logger.info(text);
        }
    }

    private void setProgress(int val) {

        if (progressBar != null) {
            progressBar.setValue(val);

        }
        else {
            logger.info(val + "%");
        }
    }

    private boolean checkport80(String host) {
        Socket sock;
        try {
            sock = new Socket(host, 80);
            sock.setSoTimeout(200);
            return true;
        }
        catch (UnknownHostException e) {
        }
        catch (IOException e) {
        }
        return false;

    }

    public String getAdress() {
        if (adress != null) return adress;
        setProgressText("try to find the router ip");
        // String[] hosts = new String[]{"192.168.2.1", "192.168.1.1",
        // "192.168.0.1", "fritz.box"};

        if (new File("/sbin/route").exists()) {
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
                                return adress;
                            }
                        }
                    }
                    catch (UnknownHostException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

            }
        }
        Vector<String> hosts = new Vector<String>();
        if (!hosts.contains("192.168.2.1")) hosts.add("192.168.2.1");
        if (!hosts.contains("192.168.1.1")) hosts.add("192.168.1.1");
        if (!hosts.contains("192.168.0.1")) hosts.add("192.168.0.1");
        if (!hosts.contains("fritz.box")) hosts.add("fritz.box");
        String ip = null;
        try {
            InetAddress myAddr = InetAddress.getLocalHost();
            ip = myAddr.getHostAddress();
        }
        catch (UnknownHostException exc) {
        }
        if (ip != null) {
            String host = ip.replace("\\.[\\d]+$", ".");
            for (int i = 1; i < 255; i++) {
                String lhost = host + i;
                if (!lhost.equals(ip) && !hosts.contains(ip)) {
                    hosts.add(ip);
                }

            }
        }
        int size = hosts.size();

        for (int i = 0; i < size && !cancel; i++) {
            setProgress(i * 100 / size);
            final String hostname = hosts.get(i);
            setProgressText("testing " + hostname);
            try {
                if (InetAddress.getByName(hostname).isReachable(1500)) {
                    if (checkport80(hostname)) {
                        adress = hostname;
                        setProgress(100);
                        return adress;
                    }
                }

            }
            catch (IOException e) {
            }
        }
        setProgress(100);
        return null;

    }

    public Vector<String[]> getRouterDatas() {
        if (routerDatas != null) return routerDatas;
        if (getAdress() == null) return null;
        try {
            // progress.setStatusText("Load possible RouterDatas");
            RequestInfo request = Plugin.getRequest(new URL("http://" + adress));
            String html = request.getHtmlCode().toLowerCase();
            Vector<String[]> routerData = new HTTPLiveHeader().getLHScripts();
            Vector<String[]> retRouterData = new Vector<String[]>();
            for (int i = 0; i < routerData.size(); i++) {
                String[] dat = routerData.get(i);
                if (html.matches(dat[3])) retRouterData.add(dat);
            }
            routerDatas = retRouterData;
            return retRouterData;
        }
        catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }

    private boolean isEmpty(String arg) {
        if (arg == null || arg.matches("[\\s]*")) return true;
        return false;

    }

    public String[] getRouterData() {
        setProgressText("Get Routerdata");
        if (getRouterDatas() == null) {
            return null;
        }
        int retries = JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_HTTPSEND_RETRIES, 5);
        int wipchange = JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_HTTPSEND_WAITFORIPCHANGE, 20);
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_RETRIES, 0);
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_WAITFORIPCHANGE, 10);
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_USER, username);
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_PASS, password);
        final int size = routerDatas.size();
        for (int i = 0; i < size && !cancel; i++) {
            final String[] data = routerDatas.get(i);
            setProgressText("Testing router: " + data[1]);
            setProgress(i * 100 / size);

            if (isEmpty(username)) {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_USER, data[4]);
            }
            else {
                data[4] = username;
            }
            if (isEmpty(password)) {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_PASS, data[5]);
            }
            else {
                data[5] = password;
            }
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_REQUESTS, data[2]);
            JDUtilities.saveConfig();
            if (JDUtilities.getController().requestReconnect()) {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_RETRIES, retries);
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_WAITFORIPCHANGE, wipchange);
                JDUtilities.saveConfig();
                setProgress(100);
                return data;
            }
        }
        setProgress(100);
        return null;
    }

}
