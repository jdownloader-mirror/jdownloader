package org.jdownloader.settings;

import javax.swing.LookAndFeel;

import org.appwork.storage.config.StorageHandler;
import org.appwork.storage.config.ValidationException;
import org.appwork.utils.Application;

/**
 * Validator Implementation
 * 
 * @author thomas
 * 
 */
public class GraphicalUserInterfaceSettingsValidator implements GraphicalUserInterfaceSettings {

    public StorageHandler<?> getStorageHandler() {
        return null;
    }

    public void setActiveConfigPanel(String name) {
    }

    public String getActiveConfigPanel() {
        return null;
    }

    public String getThemeID() {
        return null;
    }

    public void setThemeID(String themeID) {
        if (!Application.getResource("themes/" + themeID).exists()) {
            throw new ValidationException(Application.getResource("themes/" + themeID) + " must exist");
        } else if (!Application.getResource("themes/" + themeID).isDirectory()) { throw new ValidationException(Application.getResource("themes/" + themeID) + " must be a directory"); }
    }

    public boolean isBalloonNotificationEnabled() {
        return false;
    }

    public boolean isConfigViewVisible() {
        return false;
    }

    public void setConfigViewVisible(boolean b) {
    }

    public boolean isLogViewVisible() {
        return false;
    }

    public void setLogViewVisible(boolean b) {
    }

    public String getLookAndFeel() {
        return null;
    }

    public boolean isWindowDecorationEnabled() {
        return false;
    }

    public void setWindowDecorationEnabled(boolean b) {
    }

    public int getDialogDefaultTimeout() {
        return 0;
    }

    public void setDialogDefaultTimeout(int value) {
    }

    public void setBalloonNotificationEnabled(boolean b) {
    }

    public void setLookAndFeel(String laf) {
        if (laf == null || laf.trim().length() == 0) return;
        Class<?> c = null;
        try {
            // MetalLookAndFeel.class.getName();
            c = Class.forName(laf);
        } catch (Throwable e) {

            throw new ValidationException(laf + " is not a valid LookAndFeel Class path. Example: javax.swing.plaf.metal.MetalLookAndFeel");
        }
        if (!LookAndFeel.class.isAssignableFrom(c)) { throw new ValidationException(laf + " is not a Look and feel. Example: javax.swing.plaf.metal.MetalLookAndFeel"); }

    }

}
