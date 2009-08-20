//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.views;

import javax.swing.Icon;

import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.views.sidebars.configuration.ConfigSidebar;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class ConfigurationView extends View {

    private static final long serialVersionUID = -5607304856678049342L;

    /**
     * DO NOT MOVE THIS CONSTANT. IT's important to have it in this file for the
     * LFE to parse JDL Keys correct
     */
    private static final String IDENT_PREFIX = "jd.gui.swing.jdgui.views.configurationview.";

    public ConfigurationView() {
        super();
        sidebar.setBorder(null);
        // init config with 2 seconds delay to avoid gui locks at startup
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {
                        setSideBar(ConfigSidebar.getInstance(ConfigurationView.this));
                        return null;
                    }

                }.start();
            }
        }.start();
  

    }

    @Override
    public Icon getIcon() {
        return JDTheme.II("gui.images.taskpanes.configuration", ICON_SIZE, ICON_SIZE);
    }

    @Override
    public String getTitle() {
        return JDL.L(IDENT_PREFIX + "tab.title", "Settings");
    }

    @Override
    public String getTooltip() {
        return JDL.L(IDENT_PREFIX + "tab.tooltip", "All options and settings for JDownloader");
    }

    @Override
    protected void onHide() {

    }

    @Override
    protected void onShow() {

    }

}
