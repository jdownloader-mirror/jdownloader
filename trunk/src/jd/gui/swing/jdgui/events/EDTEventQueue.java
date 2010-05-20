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

package jd.gui.swing.jdgui.events;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.event.MouseEvent;

public class EDTEventQueue extends EventQueue {

    private final QuickHelp qh;
    private final ContextMenu cm;

    public EDTEventQueue() {
        super();

        qh = new QuickHelp();
        cm = new ContextMenu();
    }

    @Override
    protected void dispatchEvent(AWTEvent e) {
        if (e instanceof MouseEvent) qh.dispatchMouseEvent((MouseEvent) e);

        super.dispatchEvent(e);

        if (e instanceof MouseEvent) cm.dispatchMouseEvent((MouseEvent) e);
    }

}
