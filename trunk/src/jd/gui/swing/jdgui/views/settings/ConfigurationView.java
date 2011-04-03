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

package jd.gui.swing.jdgui.views.settings;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JComponent;

import jd.gui.swing.jdgui.GraphicalUserInterfaceSettings;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.ClosableView;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

import org.appwork.storage.config.JsonConfig;

public class ConfigurationView extends ClosableView {

    private static final long        serialVersionUID = -5607304856678049342L;

    private static final String      IDENT_PREFIX     = "jd.gui.swing.jdgui.views.configurationview.";

    private static ConfigurationView INSTANCE         = null;

    public synchronized static ConfigurationView getInstance() {
        if (INSTANCE == null) INSTANCE = new ConfigurationView();
        return INSTANCE;
    }

    private ConfigurationView() {
        super();
        setBackground(new Color(0xF5FCFF));
        init();
        this.setContent(new ConfigurationPanel());
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
        JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);

        SwitchPanel panel = this.getContent();
        if (panel != null) panel.setShown();
    }

    /**
     * Overwritten, because the ConfigurationView has its own mechanism for
     * distributing the {@link SwitchPanel#setShown()} event. Otherwise
     * <b>all</b> {@link ConfigPanel}s would be noticed when opening the
     * {@link ConfigurationView}.
     */
    @Override
    protected void distributeView(JComponent switchPanel) {
    }

    @Override
    public String getID() {
        return "configview";
    }

}
