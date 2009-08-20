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

package jd.plugins.optional.schedule.modules;

import jd.config.Configuration;
import jd.controlling.reconnect.Reconnecter;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class DisableReconnect implements SchedulerModuleInterface {
    private static final long serialVersionUID = -4388497540511505008L;

    public void execute(String parameter) {
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true)) Reconnecter.toggleReconnect();
    }

    public String getName() {
        return "plugin.optional.schedular.module.disableReconnect";
    }

    public boolean needParameter() {
        return false;
    }

    public String getTranslation() {
        return JDL.L(getName(), "Disable Reconnect");
    }

    public boolean checkParameter(String parameter) {
        return false;
    }
}
