package jd.controlling.reconnect.plugins.liveheader;

import jd.config.Configuration;
import jd.utils.JDUtilities;

import org.appwork.storage.config.defaults.AbstractDefaultFactory;

public class DefaultUsername extends AbstractDefaultFactory<String> {

    @Override
    public String getDefaultValue() {
        return JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_USER);
    }

}
