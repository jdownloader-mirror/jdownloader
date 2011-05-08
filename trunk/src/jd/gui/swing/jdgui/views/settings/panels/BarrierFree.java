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

package jd.gui.swing.jdgui.views.settings.panels;

import javax.swing.ImageIcon;

import jd.gui.swing.jdgui.views.settings.components.Spinner;

import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate.T;
import org.jdownloader.images.Theme;
import org.jdownloader.translate.JDT;

public class BarrierFree extends AbstractConfigPanel {

    private static final long serialVersionUID = 1L;
    private Spinner           captchaSize;

    public String getTitle() {
        return JDT._.gui_settings_barrierfree_title();
    }

    public BarrierFree() {
        super();
        captchaSize = new Spinner(50, 500);
        captchaSize.setFormat("#'%'");
        this.addHeader(getTitle(), Theme.getIcon("settings/barrierfreesettings", 32));
        this.addDescription(JDT._.gui_settings_barrierfree_description());
        this.addPair(T._.gui_config_barrierfree_captchasize(), captchaSize);

    }

    @Override
    public ImageIcon getIcon() {
        return Theme.getIcon("settings/barrierfreesettings", 32);
    }

    @Override
    protected void onShow() {
    }

    @Override
    protected void onHide() {
    }
}