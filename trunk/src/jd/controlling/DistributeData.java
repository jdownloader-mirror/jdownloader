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


package jd.controlling;

import java.net.URLDecoder;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.event.ControlEvent;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

/**
 * Diese Klasse läuft in einem Thread und verteilt den Inhalt der Zwischenablage
 * an (unter Umständen auch mehrere) Plugins Die gefundenen Treffer werden
 * ausgeschnitten.
 * 
 * @author astaldo
 */
public class DistributeData extends ControlMulticaster {
    /**
     * Der Logger
     */
    private static Logger            logger = JDUtilities.getLogger();

    /**
     * Die zu verteilenden Daten
     */
    private String                   data;

    private Vector<DownloadLink>     linkData;

    /**
     * Erstellt einen neuen Thread mit dem Text, der verteilt werden soll. Die
     * übergebenen Daten werden durch einen URLDecoder geschickt.
     * 
     * @param data Daten, die verteilt werden sollen
     */
    public DistributeData(String data) {
        super("JD-DistributeData");
        this.data = data;
        try {
            // this.data = URLDecoder.decode(this.data, "UTF-8");
        }
        catch (Exception e) {
            logger.warning("text not url decodeable");
        }
    }

    public void run() {
        Vector<DownloadLink> links = findLinks();
        Collections.sort(links);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_DISTRIBUTE_FINISHED, links));
    }

    /**
     * Ermittelt über die Plugins alle Passenden Links und gibt diese in einem
     * Vector zurück
     * 
     * @return link-Vector
     */
    public Vector<DownloadLink> findLinks() {

        Vector<DownloadLink> links = new Vector<DownloadLink>();
        if (JDUtilities.getPluginsForHost() == null) return new Vector<DownloadLink>();
        Vector<String> foundpassword = Plugin.findPasswords(data);


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
            logger.info("hanlde "+decrypted);
            Iterator<PluginForHost> iteratorHost = JDUtilities.getPluginsForHost().iterator();
            while (iteratorHost.hasNext()) {

                try {
    				PluginForHost pHost = (PluginForHost) iteratorHost.next();
    				if (pHost.canHandle(decrypted.getDownloadURL())) {
                        Vector<DownloadLink> dLinks = pHost.getDownloadLinks(decrypted.getDownloadURL());
                      
                        for (int c = 0; c < dLinks.size(); c++) {

                            dLinks.get(c).addSourcePluginPasswords(foundpassword);
                            dLinks.get(c).addSourcePluginPasswords(decrypted.getSourcePluginPasswords());
                            dLinks.get(c).setSourcePluginComment(decrypted.getSourcePluginComment());

                        }

                        links.addAll(dLinks);
                    }
                }
                catch (Exception e) {
                    logger.severe("Decrypter/Search Fehler: " + e.getMessage());
                }
				
			}
        }
        // Danach wird der (noch verbleibende) Inhalt der Zwischenablage an die
        // Plugins der Hoster geschickt
        Iterator<PluginForHost> iteratorHost = JDUtilities.getPluginsForHost().iterator();
        while (iteratorHost.hasNext()) {
			PluginForHost pHost = (PluginForHost) iteratorHost.next();
            if (pHost.canHandle(data)) {
                Vector<DownloadLink> dl = pHost.getDownloadLinks(data);
                if (foundpassword.size()>0) {
                	Iterator<DownloadLink> iter = dl.iterator();
                	while (iter.hasNext()) {
						((DownloadLink) iter.next()).addSourcePluginPasswords(foundpassword);
					}
                }
                links.addAll(dl);
                data = pHost.cutMatches(data);
            }
		}
logger.info("Distribute links : "+links);
        return links;
    }

    private Vector<DownloadLink> handleDecryptPlugins() {
        
        Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
        if ( JDUtilities.getPluginsForDecrypt() == null) return decryptedLinks;
        Iterator<PluginForDecrypt> iteratorDecrypt = JDUtilities.getPluginsForDecrypt().iterator();
        while (iteratorDecrypt.hasNext()) {
			PluginForDecrypt pDecrypt = (PluginForDecrypt) iteratorDecrypt.next();
            if (pDecrypt.canHandle(data)) {

                try {
                    pDecrypt = pDecrypt.getClass().newInstance();

                    fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_DECRYPT_ACTIVE, pDecrypt));
                    Vector<String> decryptableLinks = pDecrypt.getDecryptableLinks(data);
                    data = pDecrypt.cutMatches(data);

                    decryptedLinks.addAll(pDecrypt.decryptLinks(decryptableLinks));

                    fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_DECRYPT_INACTIVE, pDecrypt));
                }
                catch (Exception e) {

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
     * Sucht in dem übergebenen vector nach weiteren decryptbaren Links und
     * decrypted diese
     * 
     * @param decryptedLinks
     * @return
     */
    private boolean deepDecrypt(Vector<DownloadLink> decryptedLinks) {
        if (decryptedLinks.size() == 0) return false;
        boolean hasDecryptedLinks = false;
        logger.info("Deep decrypt");
        for (int i = decryptedLinks.size() - 1; i >= 0; i--) {
            DownloadLink link = decryptedLinks.get(i);
            String url = link.getDownloadURL();

            if (url != null) {
                url = Plugin.getHttpLinkList(url);

                try {
                    url = URLDecoder.decode(url, "UTF-8");
                }
                catch (Exception e) {
                    logger.warning("text not url decodeable");
                }
            }

            boolean canDecrypt = false;
            Iterator<PluginForDecrypt> iteratorDecrypt = JDUtilities.getPluginsForDecrypt().iterator();
            while (iteratorDecrypt.hasNext()) {
				PluginForDecrypt pDecrypt = (PluginForDecrypt) iteratorDecrypt.next();
                if (pDecrypt.canHandle(url)) {
                    try {
                        pDecrypt = pDecrypt.getClass().newInstance();

                        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_DECRYPT_ACTIVE, pDecrypt));

                        Vector<String> decryptableLinks = pDecrypt.getDecryptableLinks(url);
                        url = pDecrypt.cutMatches(url);
                        Vector<DownloadLink> links = pDecrypt.decryptLinks(decryptableLinks);
                        logger.info("Got links: " + links);

                        // Reicht die passwörter weiter
                        for (int t = 0; t < links.size(); t++) {
                            links.get(t).addSourcePluginPasswords(link.getSourcePluginPasswords());
                        }
                        decryptedLinks.addAll(links);

                        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_DECRYPT_INACTIVE, pDecrypt));
                        canDecrypt = true;
                        break;
                    }
                    catch (Exception e) {

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
     * Bringt alle links in data in eine einheitliche Form
     */
    private void reformDataString() {
        if (data != null) {
            data = Plugin.getHttpLinkList(data);

            try {
                this.data = URLDecoder.decode(this.data, "UTF-8");
            }
            catch (Exception e) {
                logger.warning("text not url decodeable");
            }
        }
    }


    public Vector<DownloadLink> getLinkData() {
        return linkData;
    }

    public void setLinkData(Vector<DownloadLink> linkData) {
        this.linkData = linkData;
    }
}