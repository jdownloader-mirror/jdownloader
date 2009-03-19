package jd.nutils;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.Icon;

import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;

public class Image {
    public static ImageIcon iconToImage(Icon icon) {
        if (icon instanceof ImageIcon) {
            return ((ImageIcon) icon);
        } else {
            int w = icon.getIconWidth();
            int h = icon.getIconHeight();
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            BufferedImage image = gc.createCompatibleImage(w, h);
            Graphics2D g = image.createGraphics();
            icon.paintIcon(null, g, 0, 0);
            g.dispose();
            return new ImageIcon(image);
        }
    }

    public static ImageIcon getFileIcon(String ext) {

        File file = null;
        try {
            file = File.createTempFile("icon", "." + ext);
            sun.awt.shell.ShellFolder shellFolder = sun.awt.shell.ShellFolder.getShellFolder(file);
            return new ImageIcon(shellFolder.getIcon(true));

        } catch (Throwable e) {

            FileSystemView view = FileSystemView.getFileSystemView();
            return iconToImage(view.getSystemIcon(file));

        } finally {
            file.delete();
        }

    }
}
