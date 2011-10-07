package org.jdownloader;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.swing.ImageIcon;

import jd.controlling.FavIconController;
import jd.controlling.FavIconRequestor;

import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.images.Interpolation;

public class DomainInfo implements FavIconRequestor {
    private static final long CACHE_TIMEOUT = 30000;
    private static final int  WIDTH         = 16;
    private static final int  HEIGHT        = 16;

    private DomainInfo(String tld) {
        this.tld = tld;
    }

    private String tld;

    public String getTld() {
        return tld;
    }

    protected MinTimeWeakReference<ImageIcon> hosterIcon          = null;
    protected boolean                         hosterIconRequested = false;

    public void setTld(String tld) {
        this.tld = tld;
    }

    /**
     * Returns a
     * 
     * @return
     */
    public synchronized ImageIcon getFavIcon() {
        ImageIcon ia = null;
        if (hosterIcon != null) {
            ia = hosterIcon.get();
            // cleanup;
            if (ia == null) {
                resetFavIcon();
            } else {
                return ia;
            }
        }
        if (!hosterIconRequested) {
            hosterIconRequested = true;
            // load it
            ia = FavIconController.getFavIcon(getTld(), this, true);
            if (ia != null) {
                ia = setFavIcon(ia);
                return ia;
            }
        }
        /* use default favicon */
        ia = setFavIcon(null);
        return ia;

    }

    private String cleanString(String host) {
        return host.replaceAll("[a-z0-9\\-\\.]", "");
    }

    /**
     * Creates a dummyHosterIcon
     */
    public BufferedImage createDefaultFavIcon() {
        int w = 16;
        int h = 16;
        int size = 9;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        final BufferedImage image = gc.createCompatibleImage(w, h, Transparency.BITMASK);

        String dummy = cleanString(getTld()).toUpperCase();
        if (dummy.length() > 2) dummy = dummy.substring(0, 2);

        Graphics2D g = image.createGraphics();
        g.setFont(new Font("Arial", Font.BOLD, size));
        int ww = g.getFontMetrics().stringWidth(dummy);
        g.setColor(Color.WHITE);
        g.fillRect(1, 1, w - 2, h - 2);
        g.setColor(Color.BLACK);
        g.drawString(dummy, (w - ww) / 2, 2 + size);
        g.dispose();
        return image;
    }

    /* reset customized favicon */
    public void resetFavIcon() {
        hosterIconRequested = false;
        hosterIcon = null;
    }

    public synchronized ImageIcon setFavIcon(ImageIcon icon) {
        if (icon == null) {
            icon = new ImageIcon(createDefaultFavIcon());
        } else {
            icon = new ImageIcon(IconIO.getScaledInstance(icon.getImage(), WIDTH, HEIGHT, Interpolation.BICUBIC, true));
        }
        this.hosterIcon = new MinTimeWeakReference<ImageIcon>(icon, CACHE_TIMEOUT, getTld());
        return icon;
    }

    private static HashMap<String, DomainInfo> CACHE = new HashMap<String, DomainInfo>();

    public static DomainInfo getInstance(String host) {
        // WARNING: can be a memleak
        synchronized (CACHE) {
            DomainInfo ret = CACHE.get(host);
            if (ret == null) {
                CACHE.put(host, ret = new DomainInfo(host));
            }
            return ret;
        }
    }

}
