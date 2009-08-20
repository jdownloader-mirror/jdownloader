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
import jd.config.SubConfiguration;
import jd.utils.locale.JDL;

public class SetChunck implements SchedulerModuleInterface {
    private static final long serialVersionUID = -986046937528397324L;

    public void execute(String parameter) {
        SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, Integer.parseInt(parameter));
        SubConfiguration.getConfig("DOWNLOAD").save();
    }

    public String getName() {
        return "plugin.optional.schedular.module.setChuncks";
    }

    public boolean needParameter() {
        return true;
    }

    public String getTranslation() {
        return JDL.L(getName(), "Set Chuncks");
    }

    public boolean checkParameter(String parameter) {
        try {
            Integer.parseInt(parameter);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
