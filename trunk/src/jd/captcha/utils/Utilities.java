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

package jd.captcha.utils;

import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import jd.controlling.JDLogger;
import jd.gui.skins.jdgui.swing.GuiRunnable;
import jd.utils.JDUtilities;

/**
 * Diese Klasse beinhaltet mehrere Hilfsfunktionen
 * 
 * @author JD-Team
 */
public class Utilities {

    private static Logger logger = JDLogger.getLogger();

    public static Logger getLogger() {
        return logger;
    }

    public static boolean isLoggerActive() {
        return JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL;
    }

    public static boolean checkJumper(int x, int from, int to) {
        return x >= from && x <= to;
    }

    /**
     * Zeigt einen Directory Chooser an
     * 
     * @param path
     * @return User Input /null
     */
    public static File directoryChooser() {
        JFileChooser fc = new JFileChooser();
        fc.setApproveButtonText("OK");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) return fc.getSelectedFile();
        return null;
    }

    public static String getMethodDir() {
        return JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/" + JDUtilities.getJACMethodsDirectory();
    }

    /**
     * Gibt die default GridBagConstants zurück
     * 
     * @param x
     * @param y
     * @param width
     * @param height
     * @return Default GridBagConstraints
     */
    public static GridBagConstraints getGBC(int x, int y, int width, int height) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;
        gbc.gridheight = height;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(1, 1, 1, 1);
        return gbc;
    }

    public static int getJumperStart(int from, int to) {
        return from + (to - from) / 2;
    }

    public static int getPercent(int a, int b) {
        if (b == 0) return 100;
        return a * 100 / b;
    }

    /**
     * Lädt file als Bildatei und wartet bis file geladen wurde. gibt file als
     * Image zurück
     * 
     * @param file
     * @return Neues Bild
     */
    public static Image loadImage(final File file) {
        GuiRunnable<Image> run = new GuiRunnable<Image>() {
            // @Override
            @Override
            public Image runSave() {
                JFrame jf = new JFrame();
                Image img = jf.getToolkit().getImage(file.getAbsolutePath());
                MediaTracker mediaTracker = new MediaTracker(jf);
                mediaTracker.addImage(img, 0);
                try {
                    mediaTracker.waitForID(0);
                } catch (InterruptedException e) {
                    return null;
                }

                mediaTracker.removeImage(img);
                return img;
            }
        };
        return run.getReturnValue();
    }

    public static int nextJump(int x, int from, int to, int step) {
        int start = Utilities.getJumperStart(from, to);
        int ret;
        if (x == start) {
            ret = start + step;
            if (ret > to) {
                ret = start - step;
            }
        } else if (x > start) {
            int dif = x - start;
            ret = start - dif;

        } else {
            int dif = start - x + step;
            ret = start + dif;
            if (ret > to) {
                ret = start - dif;
            }
        }

        return ret;

    }

    /**
     * Dreht die Koordinaten x und y um den Mittelpunkt nullX und nullY umd en
     * Winkel winkel
     * 
     * @param x
     * @param y
     * @param nullX
     * @param nullY
     * @param winkel
     * @return neue Koordinaten
     */
    public static int[] turnCoordinates(int x, int y, int nullX, int nullY, double winkel) {
        winkel /= 180.0;
        int newX = x - nullX;
        int newY = y - nullY;
        double aktAngle = Math.atan2(newY, newX);

        int[] ret = new int[2];
        double radius = Math.sqrt(newX * newX + newY * newY);
        int yTrans = (int) Math.round(radius * Math.sin((aktAngle + winkel * Math.PI)));
        int xTrans = (int) Math.round(radius * Math.cos((aktAngle + winkel * Math.PI)));
        ret[0] = xTrans + nullX;
        ret[1] = yTrans + nullY;
        return ret;
    }

}