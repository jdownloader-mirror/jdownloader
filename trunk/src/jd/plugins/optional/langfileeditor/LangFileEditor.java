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

package jd.plugins.optional.langfileeditor;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.Main;
import jd.PluginWrapper;
import jd.config.MenuItem;
import jd.event.ControlEvent;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

/**
 * Editor for jDownloader language files. Gets JDLocale.L() and JDLocale.LF()
 * entries from source and compares them to the keypairs in the language file.
 * 
 * @author eXecuTe
 * @author Greeny
 */
public class LangFileEditor extends PluginOptional {

    private LFETaskPane tp = null;

    public LangFileEditor(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public boolean initAddon() {
        if (SimpleGUI.CURRENTGUI == null) {
            JDUtilities.getController().addControlListener(this);
        } else {
            initLFE();
        }
        return true;
    }

    private void initLFE() {
        final Object lock = new Object();
        EventQueue.invokeLater(new GuiRunnable() {
            private static final long serialVersionUID = 8726498576488124702L;

            public void run() {
                if (tp == null) tp = new LFETaskPane(getHost(), JDTheme.II("gui.images.jd_logo", 24, 24));
                SimpleGUI.CURRENTGUI.getTaskPane().add(tp);
                synchronized (lock) {
                    lock.notify();
                }
            }
        });

        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_INIT_COMPLETE && event.getSource() instanceof Main) {
            initLFE();
            JDUtilities.getController().removeControlListener(this);
            return;
        }
        super.controlEvent(event);
    }

    @Override
    public void onExit() {
        SimpleGUI.CURRENTGUI.getTaskPane().remove(tp);
        JDUtilities.getController().removeControlListener(this);
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        return null;
    }

    @Override
    public String getCoder() {
        return "Greeny";
    }

    @Override
    public String getHost() {
        return JDLocale.L("plugins.optional.langfileeditor.name", "Language File Editor");
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    public static int getAddonInterfaceVersion() {
        return 2;
    }

}