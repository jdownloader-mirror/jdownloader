package jd.gui.skins.simple.components.Linkgrabber;

import java.util.ArrayList;
import java.util.HashSet;

import jd.config.SubConfiguration;
import jd.controlling.LinkGrabberController;
import jd.parser.Regex;

public class LinkGrabberConstants {

    public static final String PARAM_ONLINECHECK = "PARAM_ONLINECHECK";
    public static final String CONFIG = "LINKGRABBER";
    public static final String IGNORE_LIST = "IGNORE_LIST";
    private static final HashSet<String> extensionFilter = new HashSet<String>();

    public static HashSet<String> getExtensionFilter() {
        synchronized (extensionFilter) {
            if (LinkGrabberController.getInstance().size() == 0) extensionFilter.clear();
            return extensionFilter;
        }
    }

    public static SubConfiguration getConfig() {
        return SubConfiguration.getConfig(LinkGrabberConstants.CONFIG);
    }

    public static boolean isLinkCheckEnabled() {
        return getConfig().getBooleanProperty(LinkGrabberConstants.PARAM_ONLINECHECK, true);
    }

    public static String[] getLinkFilterPattern() {
        String filter = getConfig().getStringProperty(LinkGrabberConstants.IGNORE_LIST, null);
        if (filter == null) return null;

        String[] lines = Regex.getLines(filter);
        ArrayList<String> ret = new ArrayList<String>();
        for (String line : lines) {
            if (line.trim().startsWith("#") || line.trim().length() == 0) continue;
            ret.add(line.trim());
        }
        return ret.toArray(new String[] {});
    }

}
