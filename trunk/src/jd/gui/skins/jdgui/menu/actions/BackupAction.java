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

package jd.gui.skins.jdgui.menu.actions;

import java.awt.event.ActionEvent;

import jd.gui.swing.components.Balloon;
import jd.update.JDUpdateUtils;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class BackupAction extends StartAction {
    /**
     * 
     */

    private static boolean inprogress = false;
    private static final long serialVersionUID = 823930266263085474L;

    public BackupAction() {
        super("action.backup", "gui.images.save");
    }

    public void actionPerformed(ActionEvent e) {
        if (inprogress) return;
        new Thread() {
            public void run() {
                inprogress = true;
                JDUpdateUtils.backupDataBase();
                Balloon.show(JDL.L("gui.balloon.backup.title","Backup"), JDTheme.II("gui.images.save", 32, 32), JDL.LF("gui.backup.finished", "Linklist successfully backuped!"));
                inprogress = false;
            }
        }.start();
    }
}
