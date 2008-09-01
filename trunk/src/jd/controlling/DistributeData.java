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

package jd.controlling;

import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.event.ControlEvent;
import jd.parser.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

/**
 * Diese Klasse läuft in einem Thread und verteilt den Inhalt der Zwischenablage
 * an (unter Umständen auch mehrere) Plugins Die gefundenen Treffer werden
 * ausgeschnitten.
 * 
 * @author astaldo
 */
public class DistributeData extends ControlBroadcaster {
    /**
     * Der Logger
     */
    private static Logger logger = JDUtilities.getLogger();

    /**
     * Aufruf von Clipboard Überwachung
     */
    private boolean clipboard = false;

    /**
     * Die zu verteilenden Daten
     */
    private String data;

    /**
     * keinen Linkgrabber öffnen sondern direkt hinzufügen
     */
    private boolean hideGrabber;

    private Vector<DownloadLink> linkData;

    /**
     * Download nach Beendigung starten
     */
    private boolean startDownload;

    /**
     * Erstellt einen neuen Thread mit dem Text, der verteilt werden soll. Die
     * übergebenen Daten werden durch einen URLDecoder geschickt.
     * 
     * @param data
     *            Daten, die verteilt werden sollen
     */
    public DistributeData(String data) {
        super("JD-DistributeData");
        this.data = data;
        try {
            this.data = URLDecoder.decode(this.data, "UTF-8");
        } catch (Exception e) {
            logger.warning("text not url decodeable");
        }
    }

    public DistributeData(String data, boolean fromClipboard) {
        super("JD-DistributeData");
        this.data = data;
        try {
            this.data = URLDecoder.decode(this.data, "UTF-8");
        } catch (Exception e) {
            logger.warning("text not url decodeable");
        }
        if (fromClipboard) {
            clipboard = true;
        }
    }

    public DistributeData(String data, boolean hideGrabber, boolean startDownload) {
        super("JD-DistributeData");
        this.data = data;
        try {
            this.data = URLDecoder.decode(this.data, "UTF-8");
        } catch (Exception e) {
            logger.warning("text not url decodeable");
        }
        this.hideGrabber = hideGrabber;
        this.startDownload = startDownload;
    }

    /**
     * Sucht in dem übergebenen vector nach weiteren decryptbaren Links und
     * decrypted diese
     * 
     * @param decryptedLinks
     * @return
     */
    private boolean deepDecrypt(ArrayList<DownloadLink> decryptedLinks) {
        if (decryptedLinks.size() == 0) { return false; }
        boolean hasDecryptedLinks = false;

        for (int i = decryptedLinks.size() - 1; i >= 0; i--) {
            DownloadLink link = decryptedLinks.get(i);
            String url = link.getDownloadURL();

            if (url != null) {
                url = HTMLParser.getHttpLinkList(url);

                try {
                    url = URLDecoder.decode(url, "UTF-8");
                } catch (Exception e) {
                    logger.warning("text not url decodeable");
                }
            }

            boolean canDecrypt = false;
            Iterator<PluginForDecrypt> iteratorDecrypt = JDUtilities.getPluginsForDecrypt().iterator();
            while (iteratorDecrypt.hasNext()) {
                PluginForDecrypt pDecrypt = iteratorDecrypt.next();
                if (pDecrypt.canHandle(url)) {
                    try {
                        pDecrypt = pDecrypt.getClass().newInstance();

                        CryptedLink[] decryptableLinks = pDecrypt.getDecryptableLinks(url);
                        url = pDecrypt.cutMatches(url);
                        /*reicht die Decrypter Passwörter weiter*/
                        for (CryptedLink clink:decryptableLinks){
                            clink.setDecrypterPassword(link.getDecrypterPassword());
                        }
                        ArrayList<DownloadLink> links = pDecrypt.decryptLinks(decryptableLinks);

                        // Reicht die passwörter weiter
                        for (int t = 0; t < links.size(); t++) {
                            links.get(t).addSourcePluginPasswords(link.getSourcePluginPasswords());
                        }
                        decryptedLinks.addAll(links);

                        canDecrypt = true;
                        break;
                    } catch (Exception e) {

                        e.printStackTrace();
                    }
                }
            }

            if (canDecrypt) {
                decryptedLinks.remove(i);
                hasDecryptedLinks = true;
            }

        }
        return hasDecryptedLinks;
    }

    /**
     * Ermittelt über die Plugins alle Passenden Links und gibt diese in einem
     * Vector zurück
     * 
     * @param (optional)
     *            searchpw (true,false))
     * @return link-Vector
     */
    public Vector<DownloadLink> findLinks() {
        return findLinks(true);
    }

    public Vector<DownloadLink> findLinks(boolean searchpw) {

        Vector<DownloadLink> links = new Vector<DownloadLink>();
        if (JDUtilities.getPluginsForHost() == null) { return new Vector<DownloadLink>(); }
        Vector<String> foundpassword = new Vector<String>();
        if (searchpw == true) {
            foundpassword = HTMLParser.findPasswords(data);
        }

        // Zuerst wird data durch die Such Plugins geschickt.
        // decryptedLinks.addAll(handleSearchPlugins());

        reformDataString();

        // es werden die entschlüsselten Links (soweit überhaupt
        // vorhanden)
        // an die HostPlugins geschickt, damit diese einen Downloadlink
        // erstellen können

        // Edit Coa:
        // Hier werden auch die SourcePLugins in die Downloadlinks gesetzt

        Iterator<DownloadLink> iterator = handleDecryptPlugins().iterator();

        while (iterator.hasNext()) {
            DownloadLink decrypted = iterator.next();

            Iterator<PluginForHost> iteratorHost = JDUtilities.getPluginsForHost().iterator();
            while (iteratorHost.hasNext()) {

                try {
                    PluginForHost pHost = iteratorHost.next();
                    if (pHost.canHandle(decrypted.getDownloadURL())) {
                        Vector<DownloadLink> dLinks = pHost.getDownloadLinks(decrypted.getDownloadURL(), decrypted.getFilePackage() != JDUtilities.getController().getDefaultFilePackage() ? decrypted.getFilePackage() : null);

                        for (int c = 0; c < dLinks.size(); c++) {

                            dLinks.get(c).addSourcePluginPasswords(foundpassword);
                            dLinks.get(c).addSourcePluginPasswords(decrypted.getSourcePluginPasswords());
                            dLinks.get(c).setSourcePluginComment(decrypted.getSourcePluginComment());
                            dLinks.get(c).setName(decrypted.getName());
                            dLinks.get(c).setStaticFileName(decrypted.getStaticFileName());
                            dLinks.get(c).setBrowserUrl(decrypted.getBrowserUrl());
                            dLinks.get(c).setProperties(decrypted.getProperties());
                            dLinks.get(c).getLinkStatus().setStatusText(decrypted.getLinkStatus().getStatusString());
                            dLinks.get(c).setDownloadSize(decrypted.getDownloadSize());
                            dLinks.get(c).setSubdirectory(decrypted);

                        }

                        links.addAll(dLinks);
                    }
                } catch (Exception e) {
                    logger.severe("Decrypter/Search Fehler: " + e.getMessage());
                    e.printStackTrace();
                }

            }
        }
        // Danach wird der (noch verbleibende) Inhalt der Zwischenablage an die
        // Plugins der Hoster geschickt
        Iterator<PluginForHost> iteratorHost = JDUtilities.getPluginsForHost().iterator();
        while (iteratorHost.hasNext()) {
            PluginForHost pHost = iteratorHost.next();
            if (pHost.canHandle(data)) {
                Vector<DownloadLink> dl = pHost.getDownloadLinks(data, null);
                if (foundpassword.size() > 0) {
                    Iterator<DownloadLink> iter = dl.iterator();
                    while (iter.hasNext()) {
                        iter.next().addSourcePluginPasswords(foundpassword);
                    }
                }
                links.addAll(dl);
                data = pHost.cutMatches(data);
            }
        }

        data = data.replaceAll("http://", "httpviajd://");
        iteratorHost = JDUtilities.getPluginsForHost().iterator();
        while (iteratorHost.hasNext()) {
            PluginForHost pHost = iteratorHost.next();
            if (pHost.canHandle(data)) {
                Vector<DownloadLink> dl = pHost.getDownloadLinks(data, null);
                if (foundpassword.size() > 0) {
                    Iterator<DownloadLink> iter = dl.iterator();
                    while (iter.hasNext()) {
                        iter.next().addSourcePluginPasswords(foundpassword);
                    }
                }
                links.addAll(dl);
                data = pHost.cutMatches(data);
            }
        }
        data = data.replaceAll("httpviajd://", "http://");

        return links;
    }

    public Vector<DownloadLink> getLinkData() {
        return linkData;
    }

    private ArrayList<DownloadLink> handleDecryptPlugins() {

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (JDUtilities.getPluginsForDecrypt() == null) { return decryptedLinks; }
        Iterator<PluginForDecrypt> iteratorDecrypt = JDUtilities.getPluginsForDecrypt().iterator();
        while (iteratorDecrypt.hasNext()) {
            PluginForDecrypt pDecrypt = iteratorDecrypt.next();
            if (pDecrypt.canHandle(data)) {

                try {
                    pDecrypt = pDecrypt.getClass().newInstance();

                    CryptedLink[] decryptableLinks = pDecrypt.getDecryptableLinks(data);
                    data = pDecrypt.cutMatches(data);

                    decryptedLinks.addAll(pDecrypt.decryptLinks(decryptableLinks));

                } catch (Exception e) {

                    e.printStackTrace();
                }
            }
        }
        int i = 1;
        while (deepDecrypt(decryptedLinks)) {
            i++;
            logger.info("Deepdecrypt depths: " + i);
        }
        return decryptedLinks;
    }

    /**
     * Bringt alle links in data in eine einheitliche Form
     */
    private void reformDataString() {
        if (data != null) {
//            String Temp = HTMLParser.getHttpLinkList(data);
//
            try {
//                if (Temp == "") {
                    data = URLDecoder.decode(data, "UTF-8");
//                } else {
//                    data = URLDecoder.decode(Temp, "UTF-8");
//                }
            } catch (Exception e) {
                logger.warning("text not url decodeable");
            }
        }
    }

    @Override
    public void run() {

        Vector<DownloadLink> links = findLinks();

        if (links.size() == 0 && !clipboard) {

            logger.info("No supported links found -> search for links in source code of all urls");
            String[] urls = HTMLParser.getHttpLinks(data, null);

            if (urls.length > 0) {
                data = "";
            }

            for (String url : urls) {

                try {

                    RequestInfo requestInfo = HTTP.getRequest(new URL(url));
                    data += requestInfo.getHtmlCode() + " ";

                } catch (Exception e) {

                }

            }

            links = findLinks();

        }

        Collections.sort(links);

        if (hideGrabber && startDownload) {
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DISTRIBUTE_FINISHED_HIDEGRABBER_START, links));
        } else if (hideGrabber) {
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DISTRIBUTE_FINISHED_HIDEGRABBER, links));
        } else if (startDownload) {
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DISTRIBUTE_FINISHED, links));
            if (JDUtilities.getController().getDownloadStatus() == JDController.DOWNLOAD_NOT_RUNNING) {
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_STARTSTOP_DOWNLOAD, this));
            }
        } else {
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DISTRIBUTE_FINISHED, links));
        }
    }

    public void setLinkData(Vector<DownloadLink> linkData) {
        this.linkData = linkData;
    }
}