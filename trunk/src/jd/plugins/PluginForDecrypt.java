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

package jd.plugins;

import java.awt.Color;
import java.io.File;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import jd.PluginWrapper;
import jd.config.MenuItem;
import jd.controlling.CaptchaController;
import jd.controlling.LinkGrabberController;
import jd.controlling.ProgressController;
import jd.event.ControlEvent;
import jd.gui.skins.simple.components.JLinkButton;
import jd.http.Encoding;
import jd.nutils.Formatter;
import jd.nutils.jobber.JDRunnable;
import jd.nutils.jobber.Jobber;
import jd.parser.Regex;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Dies ist die Oberklasse für alle Plugins, die Links entschlüsseln können
 * 
 * @author astaldo
 */
public abstract class PluginForDecrypt extends Plugin {

    public PluginForDecrypt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private CryptedLink curcryptedLink = null;

    private static HashMap<Class<? extends PluginForDecrypt>, Long> LAST_STARTED_TIME = new HashMap<Class<? extends PluginForDecrypt>, Long>();
    private Long WAIT_BETWEEN_STARTS = 0L;

    private static int OPEN_CLICK_N_LOAD = 0;

    public synchronized long getLastTimeStarted() {
        if (!LAST_STARTED_TIME.containsKey(this.getClass())) { return 0; }
        return Math.max(0, (LAST_STARTED_TIME.get(this.getClass())));
    }

    public synchronized void putLastTimeStarted(long time) {
        LAST_STARTED_TIME.put(this.getClass(), time);
    }

    public void setStartIntervall(long interval) {
        WAIT_BETWEEN_STARTS = interval;
    }

    public boolean waitForNextStartAllowed(CryptedLink link) throws InterruptedException {
        String temp = link.getProgressController().getStatusText();
        long time = Math.max(0, WAIT_BETWEEN_STARTS - (System.currentTimeMillis() - getLastTimeStarted()));
        if (time > 0) {
            try {
                this.sleep(time, link);
            } catch (InterruptedException e) {
                link.getProgressController().setStatusText(temp);
                throw e;
            }
            link.getProgressController().setStatusText(temp);
            return true;
        } else {
            link.getProgressController().setStatusText(temp);
            return false;
        }
    }

    public void sleep(long i, CryptedLink link) throws InterruptedException {
        String before = link.getProgressController().getStatusText();
        while (i > 0) {
            i -= 1000;
            link.getProgressController().setStatusText(before + " " + JDLocale.LF("gui.downloadlink.status.wait", "wait %s min", Formatter.formatSeconds(i / 1000)));
            Thread.sleep(1000);
        }
        link.getProgressController().setStatusText(before);
    }

    /**
     * Diese Methode entschlüsselt Links.
     * 
     * @param cryptedLinks
     *            Ein Vector, mit jeweils einem verschlüsseltem Link. Die
     *            einzelnen verschlüsselten Links werden aufgrund des Patterns
     *            {@link jd.plugins.Plugin#getSupportedLinks()
     *            getSupportedLinks()} herausgefiltert
     * @return Ein Vector mit Klartext-links
     */

    protected DownloadLink createDownloadlink(String link) {
        DownloadLink dl = new DownloadLink(null, null, getHost(), Encoding.urlDecode(link, true), true);
        return dl;
    }

    // @Override
    public ArrayList<MenuItem> createMenuitems() {
        return null;
    }

    /**
     * Die Methode entschlüsselt einen einzelnen Link.
     */
    public abstract ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception;

    /**
     * Die Methode entschlüsselt einen einzelnen Link. Alle steps werden
     * durchlaufen. Der letzte step muss als parameter einen Vector<String> mit
     * den decoded Links setzen
     * 
     * @param cryptedLink
     *            Ein einzelner verschlüsselter Link
     * 
     * @return Ein Vector mit Klartext-links
     */
    public ArrayList<DownloadLink> decryptLink(CryptedLink cryptedLink) {
        curcryptedLink = cryptedLink;
        ProgressController progress;
        progress = new ProgressController("Decrypter: " + getLinkName());
        progress.setStatusText("decrypt-" + getHost() + ": " + getLinkName());
        curcryptedLink.setProgressController(progress);
        try {
            while (waitForNextStartAllowed(curcryptedLink)) {
            }
        } catch (InterruptedException e) {
            return new ArrayList<DownloadLink>();
        }
        putLastTimeStarted(System.currentTimeMillis());
        ArrayList<DownloadLink> tmpLinks = null;
        try {
            tmpLinks = decryptIt(curcryptedLink, progress);
        } catch (SocketTimeoutException e2) {
            progress.setStatusText("Serverproblem?");
            progress.setColor(Color.RED);
            progress.finalize(15000l);
            return new ArrayList<DownloadLink>();
        } catch (UnknownHostException e) {
            progress.setStatusText("No InternetConnection?");
            progress.setColor(Color.RED);
            progress.finalize(15000l);
            return new ArrayList<DownloadLink>();
        } catch (DecrypterException e) {
            tmpLinks = new ArrayList<DownloadLink>();
            progress.setStatusText(this.getHost() + ": " + e.getErrorMessage());
            progress.setColor(Color.RED);
            tryClickNLoad(cryptedLink);
            progress.finalize(15000l);
        } catch (InterruptedException e2) {
            tmpLinks = new ArrayList<DownloadLink>();
        } catch (Exception e) {
            progress.finalize();
            logger.log(Level.SEVERE, "Exception occured", e);
        }
        if (tmpLinks == null) {
            logger.severe("Decrypter out of date: " + this);
            logger.severe("Decrypter out of date: " + getVersion());
            progress.setStatusText("Decrypter out of date: " + this.getHost());

            tryClickNLoad(cryptedLink);

            progress.setColor(Color.RED);
            progress.finalize(15000l);
            return new ArrayList<DownloadLink>();
        }

        if (tmpLinks.size() == 0) {
            progress.finalize();
            return new ArrayList<DownloadLink>();
        }

        progress.finalize();
        return tmpLinks;
    }

    protected String getCaptchaCode(String methodname, File captchaFile, CryptedLink param) throws DecrypterException {
        return this.getCaptchaCode(methodname, captchaFile, 0, param, null, null);
    }

    protected String getCaptchaCode(File captchaFile, CryptedLink param) throws DecrypterException {
        return this.getCaptchaCode(this.getHost(), captchaFile, 0, param, null, null);
    }

    /**
     * 
     * @param method
     *            Method name (name of the captcha method)
     * @param file
     *            (imagefile)
     * @param flag
     *            (Flag of UserIO.FLAGS
     * @param link
     *            (CryptedlinkO)
     * @param defaultValue
     *            (suggest this code)
     * @param explain
     *            (Special captcha? needs explaination? then use this parameter)
     * @return
     * @throws DecrypterException
     */
    protected String getCaptchaCode(String method, File file, int flag, CryptedLink link, String defaultValue, String explain) throws DecrypterException {
        link.getProgressController().setStatusText(JDLocale.LF("gui.linkgrabber.waitinguserio", "Waiting for user input: %s", method));
        String cc = new CaptchaController(method, file, defaultValue, explain).getCode(flag);
        link.getProgressController().setStatusText(null);
        if (cc == null) throw new DecrypterException(DecrypterException.CAPTCHA);
        return cc;
    }

    private void tryClickNLoad(CryptedLink cryptedLink) {

        if (this.isClickNLoadEnabled() && OPEN_CLICK_N_LOAD >= 0 && OPEN_CLICK_N_LOAD <= 25) {
            synchronized (JDUtilities.userio_lock) {
                if (OPEN_CLICK_N_LOAD < 0) return;
                boolean open = JDUtilities.getGUI().showConfirmDialog(JDLocale.LF("gui.plugins.decrypt.askclicknload", "The decrypter %s seems to be outdated, but supports Click'n'Load. Open the website now?", this.getHost()));
                if (open) {
                    try {

                        JLinkButton.openURL(cryptedLink.getCryptedUrl());
                        // JLinkButton.openURL(
                        // "http://jdownloader.org/clicknload-redirect/"
                        // +Encoding.urlEncode
                        // (cryptedLink.getCryptedUrl().replace("http://",
                        // "")));

                        // JLinkButton.openURL(
                        // "http://jdownloader.org/clicknload-redirect/" +
                        // Encoding.urlEncode
                        // (cryptedLink.getCryptedUrl().replace("http://",
                        // "")));

                        OPEN_CLICK_N_LOAD++;
                    } catch (Exception e) {
                        open = false;
                    }
                }
                if (!open) {
                    OPEN_CLICK_N_LOAD = -1;

                }
            }
        }

    }

    protected boolean isClickNLoadEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    public ArrayList<DownloadLink> decryptLinks(CryptedLink[] cryptedLinks) {
        fireControlEvent(ControlEvent.CONTROL_PLUGIN_ACTIVE, cryptedLinks);
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        Jobber decryptJobbers = new Jobber(4);

        class DThread extends Thread implements JDRunnable {
            private CryptedLink decryptableLink = null;
            private PluginForDecrypt plg = null;

            public DThread(CryptedLink decryptableLink, PluginForDecrypt plg) {
                this.decryptableLink = decryptableLink;
                this.plg = plg;
            }

            // @Override
            public void run() {
                if (LinkGrabberController.isFiltered(decryptableLink)) return;
                ArrayList<DownloadLink> links = plg.decryptLink(decryptableLink);
                for (DownloadLink link : links) {
                    link.setBrowserUrl(decryptableLink.getCryptedUrl());
                }
                synchronized (decryptedLinks) {
                    decryptedLinks.addAll(links);
                }
            }

            public void go() throws Exception {
                run();
            }
        }

        for (int b = cryptedLinks.length - 1; b >= 0; b--) {
            DThread dthread = new DThread(cryptedLinks[b], (PluginForDecrypt) wrapper.getNewPluginInstance());
            decryptJobbers.add(dthread);
        }
        int todo = decryptJobbers.getJobsAdded();
        decryptJobbers.start();
        while (decryptJobbers.getJobsFinished() != todo) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
        }
        decryptJobbers.stop();
        fireControlEvent(ControlEvent.CONTROL_PLUGIN_INACTIVE, decryptedLinks);

        return decryptedLinks;
    }

    /**
     * Sucht in data nach allen passenden links und gibt diese als vektor zurück
     * 
     * @param data
     * @return
     */
    public CryptedLink[] getDecryptableLinks(String data) {
        String[] hits = new Regex(data, getSupportedLinks()).getColumn(-1);
        ArrayList<CryptedLink> chits = new ArrayList<CryptedLink>();
        if (hits != null && hits.length > 0) {

            for (int i = hits.length - 1; i >= 0; i--) {
                String file = hits[i];
                file = file.trim();
                while (file.charAt(0) == '"') {
                    file = file.substring(1);
                }
                while (file.charAt(file.length() - 1) == '"') {
                    file = file.substring(0, file.length() - 1);
                }
                hits[i] = file;

            }

            for (String hit : hits) {
                chits.add(new CryptedLink(hit));
            }
        }
        return chits.toArray((new CryptedLink[chits.size()]));
    }
    
    protected void setBrowserExclusive() {
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
    }

    /**
     * Gibt den namen des internen CryptedLinks zurück
     * 
     * @return encryptedLink
     */
    public String getLinkName() {
        if (curcryptedLink == null) return "";
        try {
            return new URL(curcryptedLink.toString()).getFile();
        } catch (MalformedURLException e) {
            return "";
        }
    }

}
