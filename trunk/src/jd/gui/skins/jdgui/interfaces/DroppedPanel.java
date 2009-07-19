package jd.gui.skins.jdgui.interfaces;

import jd.gui.skins.jdgui.borders.JDBorderFactory;

/**
 * A JPanel with an Dropshadow border on top
 */
public abstract class DroppedPanel extends SwitchPanel {
    public DroppedPanel() {
        this.setBorder(JDBorderFactory.createInsideShadowBorder(5, 0, 0, 0));
    }
}
   
