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

package jd.dynamics;

import jd.config.Property;
import jd.controlling.DynamicPluginInterface;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;

public class FallbackBackup extends DynamicPluginInterface {

    @Override
    public void execute() {

        JDController.getInstance().addControlListener(new ControlListener() {

            public void controlEvent(ControlEvent event) {
                if (event.getID() == 34 && event.getParameter() instanceof Property) {
                    ((Property) event.getParameter()).setProperty("value", "2009");
                }
            }

        });

    }
}
