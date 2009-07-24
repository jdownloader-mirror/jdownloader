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

package jd.gui.skins.jdgui.menu.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.config.MenuItem;
import jd.controlling.JDLogger;
import jd.utils.JDUtilities;

public class JDMenuAction extends AbstractAction {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private MenuItem menuItem;

    public JDMenuAction(MenuItem mi) {
        super();
        menuItem = mi;

        if (JDUtilities.getJavaVersion() >= 1.6) {
            putValue(SELECTED_KEY, mi.isSelected());
        }
        putValue(AbstractAction.SMALL_ICON, mi.getIcon());
        putValue(AbstractAction.LARGE_ICON_KEY, mi.getIcon());
        putValue(NAME, mi.getTitle());

    }

    public void actionPerformed(ActionEvent e) {
        if (menuItem.getActionListener() == null) {
            JDLogger.getLogger().warning("no Actionlistener for " + menuItem.getTitle());
            return;
        }
        menuItem.getActionListener().actionPerformed(new ActionEvent(menuItem, menuItem.getActionID(), menuItem.getTitle()));
    }

    public int getActionID() {
        return menuItem.getActionID();
    }

    // @Override
    public boolean isEnabled() {
        return menuItem.isEnabled();
    }
}
