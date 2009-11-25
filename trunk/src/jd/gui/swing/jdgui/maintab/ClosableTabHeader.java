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

package jd.gui.swing.jdgui.maintab;

import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;
import jd.gui.swing.jdgui.views.ClosableView;
import net.miginfocom.swing.MigLayout;

public class ClosableTabHeader extends JPanel {

    private static final long serialVersionUID = 4463352125800695922L;

    public ClosableTabHeader(ClosableView view) {
        setLayout(new MigLayout("ins 0", "[grow,fill]"));

        final JButton closeIcon = new JButton(view.getCloseAction());
        closeIcon.setContentAreaFilled(false);
        closeIcon.setBorderPainted(false);
        closeIcon.addMouseListener(new JDMouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeIcon.setContentAreaFilled(true);
                closeIcon.setBorderPainted(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeIcon.setContentAreaFilled(false);
                closeIcon.setBorderPainted(false);
            }
        });
        closeIcon.setHideActionText(true);
        putClientProperty("paintActive", Boolean.TRUE);

        JLabel l1 = new JLabel(view.getTitle());
        l1.setIcon(view.getIcon());
        l1.setOpaque(false);

        add(l1);
        add(closeIcon, "dock east, hidemode 3,gapleft 5,height 16!, width 16!");
        setOpaque(false);
    }
}
