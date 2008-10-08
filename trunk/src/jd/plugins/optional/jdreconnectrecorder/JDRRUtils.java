package jd.plugins.optional.jdreconnectrecorder;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.http.Encoding;
import jd.parser.Regex;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;

public class JDRRUtils {
    
    public static String readline(BufferedInputStream in) {
        StringBuffer data = new StringBuffer("");
        int c;
        try {
            in.mark(1);
            if (in.read() == -1)
                return null;
            else
                in.reset();
            while ((c = in.read()) >= 0) {
                if ((c == 0) || (c == 10) || (c == 13))
                    break;
                else
                    data.append((char) c);
            }
            if (c == 13) {
                in.mark(1);
                if (in.read() != 10) in.reset();
            }
        } catch (Exception e) {
        }
        return data.toString();
    }

    public static InputStream newInputStream(final ByteBuffer buf) {
        return new InputStream() {
            public synchronized int read() throws IOException {
                if (!buf.hasRemaining()) { return -1; }
                return buf.get();
            }

            public synchronized int read(byte[] bytes, int off, int len) throws IOException {
                // Read only what's left
                if (!buf.hasRemaining()) { return -1; }
                len = Math.min(len, buf.remaining());
                buf.get(bytes, off, len);
                return len;
            }
        };
    }

    public static void createStep(HashMap<String, String> headers, String postdata, Vector<String> steps) {
        if (!new Regex(headers.get(null), ".*?\\.(gif|jpg|png|bmp).*?").matches()) {
            StringBuffer hlh = new StringBuffer();
            hlh.append("    [[[STEP]]]" + "\r\n");
            hlh.append("        [[[REQUEST]]]" + "\r\n");
            hlh.append("        " + headers.get(null) + "\r\n");
            hlh.append("        Host: %%%routerip%%%" + "\r\n");
            if (headers.containsKey("authorization")) {
                String auth = new Regex(headers.get("authorization"), "Basic (.+)").getMatch(0);
                if (auth != null) JDRR.auth = Encoding.Base64Decode(auth.trim());
                hlh.append("        Authorization: Basic %%%basicauth%%%" + "\r\n");
            }
            if (headers.get(null).contains("POST") && postdata != null) {
                hlh.append("\r\n");
                hlh.append(postdata.trim());
                hlh.append("\r\n");
            }
            hlh.append("        [[[/REQUEST]]]" + "\r\n");
            hlh.append("    [[[/STEP]]]" + "\r\n");
            steps.add(hlh.toString());
        }
    }

    public static void rewriteLocationHeader(ProxyThread instance) {
        String location = JDHexUtils.toString(new Regex(instance.buffer, Pattern.compile(JDHexUtils.getHexString("Location: ") + "(.*?)" + JDHexUtils.REGEX_HTTP_NEWLINE, Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0));
        if (location != null) {
            if (new Regex(location, "http://(.*?)/?").getMatch(0) != null) {
                String oldlocation = location;
                JDUtilities.getLogger().info("Rewriting Location Header");
                // JDUtilities.getLogger().info("Old Location: " +
                // oldlocation);
                location = new Regex(location, "http://.*?/(.+)").getMatch(0);
                if (location != null) {
                    location = "http://localhost:" + JDUtilities.getSubConfig("JDRR").getIntegerProperty(JDRR.PROPERTY_PORT, 8972) + "/" + location;
                } else {
                    location = "http://localhost:" + JDUtilities.getSubConfig("JDRR").getIntegerProperty(JDRR.PROPERTY_PORT, 8972) + "/";
                }
                // JDUtilities.getLogger().info("New Location: " +
                // location);
                instance.buffer = instance.buffer.replaceAll(JDHexUtils.getHexString("Location: " + oldlocation), JDHexUtils.getHexString("Location: " + location));
                instance.renewbuffer = true;
            }
        }
    }
}
