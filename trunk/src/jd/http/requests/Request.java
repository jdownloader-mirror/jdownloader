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

package jd.http.requests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.http.Cookie;
import jd.http.Encoding;
import jd.http.JDProxy;
import jd.http.RequestHeader;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;

public abstract class Request {
    // public static int MAX_REDIRECTS = 30;

    /**
     * Gibt eine Hashmap mit allen key:value pairs im query zurück
     * 
     * @param query
     *            kann ein reines query ein (&key=value) oder eine url mit query
     * @return
     * @throws MalformedURLException
     */

    public static HashMap<String, String> parseQuery(String query) throws MalformedURLException {
        if (query == null) { return null; }
        HashMap<String, String> ret = new HashMap<String, String>();
        if (query.toLowerCase().trim().startsWith("http")) {
            query = new URL(query).getQuery();
        }

        if (query == null) { return ret; }
        String[][] split = new Regex(query.trim(), "&?(.*?)=(.*?)($|&(?=.*?=.+))").getMatches();
        if (split != null) {
            for (int i = 0; i < split.length; i++) {
                ret.put(split[i][0], split[i][1]);
            }
        }
        return ret;
    }

    private int connectTimeout;
    private ArrayList<Cookie> cookies = null;
    private int followCounter = 0;
    private boolean followRedirects = false;

    private RequestHeader headers;
    private String htmlCode;
    protected URLConnectionAdapter httpConnection;

    private long readTime = -1;
    private int readTimeout;
    private boolean requested = false;
    private long requestTime = -1;

    private URL url;
    private JDProxy proxy;
    private URL orgURL;

    private static String http2JDP(String string) {
        if (string.startsWith("http")) { return ("jdp" + string.substring(4)); }
        return string;
    }

    private static String jdp2http(String string) {
        if (string.startsWith("jdp")) { return ("http" + string.substring(3)); }
        return string;
    }

    public Request(String url) throws MalformedURLException {

        this.url = new URL(Encoding.urlEncode_light(http2JDP(url)));
        this.orgURL = new URL(jdp2http(url));
        readTimeout = SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_READ_TIMEOUT, 100000);

        connectTimeout = SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_CONNECT_TIMEOUT, 100000);

        initDefaultHeader();

    }

    public void setProxy(JDProxy proxy) {
        this.proxy = proxy;

    }

    public JDProxy getProxy() {
        return proxy;
    }

    public String printHeaders() {
        return httpConnection.toString();
    }

    public Request(URLConnectionAdapter con) {
        httpConnection = con;
        collectCookiesFromConnection();
    }

    public static ArrayList<Cookie> parseCookies(String cookieString, String host, String Date) {
        ArrayList<Cookie> cookies = new ArrayList<Cookie>();

        String header = cookieString;

        String path = null;
        String expires = null;
        String domain = null;
        HashMap<String, String> tmp = new HashMap<String, String>();
        /* einzelne Cookie Elemente */
        StringTokenizer st = new StringTokenizer(header, ";");
        while (true) {

            String key = null;
            String value = null;
            String cookieelement = null;
            if (st.hasMoreTokens()) {
                cookieelement = st.nextToken().trim();
            } else {
                break;
            }
            /* Key and Value */
            StringTokenizer st2 = new StringTokenizer(cookieelement, "=");
            if (st2.hasMoreTokens()) key = st2.nextToken().trim();
            if (st2.hasMoreTokens()) value = st2.nextToken().trim();

            if (key != null) {
                if (key.equalsIgnoreCase("path")) {
                    path = value;
                    continue;
                }
                if (key.equalsIgnoreCase("expires")) {
                    expires = value;
                    continue;
                }
                if (key.equalsIgnoreCase("domain")) {
                    domain = value;
                    continue;
                }

                tmp.put(key, value);
            } else {
                break;
            }

        }

        for (Iterator<Entry<String, String>> it = tmp.entrySet().iterator(); it.hasNext();) {
            Entry<String, String> next = it.next();
            Cookie cookie = new Cookie();
            /*
             * cookies ohne value sind keine cookies
             */
            if (next.getValue() == null) continue;
            cookies.add(cookie);
            cookie.setHost(host);
            cookie.setPath(path);
            cookie.setDomain(domain);
            cookie.setExpires(expires);
            cookie.setValue(next.getValue());
            cookie.setKey(next.getKey());
            cookie.setHostTime(Date);
        }

        return cookies;

    }

    @SuppressWarnings("unchecked")
    private void collectCookiesFromConnection() {

        List<String> cookieHeaders = (List<String>) httpConnection.getHeaderFields().get("Set-Cookie");
        String Date = httpConnection.getHeaderField("Date");
        if (cookieHeaders == null) { return; }
        if (cookies == null) {
            cookies = new ArrayList<Cookie>();
        }

        String host = httpConnection.getURL().getHost();

        for (int i = cookieHeaders.size() - 1; i >= 0; i--) {
            String header = cookieHeaders.get(i);

            cookies.addAll(parseCookies(header, host, Date));
        }

    }

    public Request connect() throws IOException {
        requested = true;
        openConnection();
        postRequest(httpConnection);

        collectCookiesFromConnection();
        // while (followRedirects && httpConnection.getHeaderField("Location")
        // != null ) {
        // followCounter++;
        // if (followCounter >= MAX_REDIRECTS) { throw new
        // IOException("Connection redirects too often. Max (" + MAX_REDIRECTS +
        // ")");
        //
        // }
        // url = new URL(httpConnection.getHeaderField("Location"));
        // openConnection();
        // postRequest(httpConnection);
        // }
        return this;
    }

    public boolean containsHTML(String html) {
        if (htmlCode == null) { return false; }
        return htmlCode.contains(html);
    }

    public void setCookies(ArrayList<Cookie> cookies) {
        this.cookies = cookies;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public long getContentLength() {
        if (httpConnection == null) { return -1; }
        return httpConnection.getLongContentLength();
    }

    public ArrayList<Cookie> getCookies() {
        if (cookies == null) {
            cookies = new ArrayList<Cookie>();
        }
        return cookies;
    }

    // public static boolean isExpired(String cookie) {
    // if (cookie == null) return false;
    //
    // try {
    // return (new Date().compareTo()) > 0;
    // } catch (Exception e) {
    // return false;
    // }
    // }

    public String getCookieString() {

        return getCookieString(cookies);

    }

    public static String getCookieString(HashMap<String, Cookie> cookies) {
        if (cookies == null) { return null; }

        StringBuilder buffer = new StringBuilder();
        boolean first = true;

        for (Iterator<Entry<String, Cookie>> it = cookies.entrySet().iterator(); it.hasNext();) {
            Cookie cookie = it.next().getValue();

            // Pfade sollten verarbeitet werden...TODO
            if (cookie.isExpired()) {
                continue;
            }

            if (first) {
                first = false;
            } else {
                buffer.append("; ");
            }
            buffer.append(cookie.getKey());
            buffer.append("=");
            buffer.append(cookie.getValue());
        }
        return buffer.toString();
    }

    public static String getCookieString(ArrayList<Cookie> cookies) {
        if (cookies == null) { return null; }

        StringBuilder buffer = new StringBuilder();
        boolean first = true;

        for (Cookie cookie : cookies) {

            // Pfade sollten verarbeitet werden...TODO
            if (cookie.isExpired()) {
                continue;
            }

            if (first) {
                first = false;
            } else {
                buffer.append("; ");
            }
            buffer.append(cookie.getKey());
            buffer.append("=");
            buffer.append(cookie.getValue());
        }
        return buffer.toString();
    }

    public int getFollowCounter() {
        return followCounter;
    }

    public RequestHeader getHeaders() {
        return headers;
    }

    public String getHtmlCode() {

        return htmlCode;
    }

    public URLConnectionAdapter getHttpConnection() {
        return httpConnection;
    }

    public String getLocation() {
        if (httpConnection == null) { return null; }
        String red = httpConnection.getHeaderField("Location");
        String encoding = httpConnection.getHeaderField("Content-Type");
        if (red == null || red.length() == 0) return null;
        if (encoding != null && encoding.contains("UTF-8")) red = Encoding.UTF8Decode(red, "ISO-8859-1");
        try {
            new URL(red);
        } catch (Exception e) {
            String path = this.getHttpConnection().getURL().getFile();
            if (!path.endsWith("/")) {

                int lastSlash = path.lastIndexOf("/");
                if (lastSlash > 0) {

                    path = path.substring(0, path.lastIndexOf("/"));
                } else {
                    path = "";
                }

            }
            red = "http://" + this.getHttpConnection().getURL().getHost() + (red.charAt(0) == '/' ? red : path + "/" + red);
        }
        return Encoding.urlEncode_light(red);

    }

    public long getReadTime() {
        return readTime;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public long getRequestTime() {
        return requestTime;
    }

    public String getResponseHeader(String key) {
        if (httpConnection == null) { return null; }
        return httpConnection.getHeaderField(key);
    }

    @SuppressWarnings("unchecked")
    public Map<String, List<String>> getResponseHeaders() {
        if (httpConnection == null) { return null; }
        return httpConnection.getHeaderFields();
    }

    public URL getUrl() {
        return orgURL;
    }

    public URL getJDPUrl() {
        return url;
    }

    private boolean hasCookies() {

        return cookies != null && !cookies.isEmpty();
    }

    private void initDefaultHeader() {

        headers = new RequestHeader();
        headers.put("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/2008111317 Ubuntu/8.04 (hardy) Firefox/3.0.4");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers.put("Accept-Language", "de, en-gb;q=0.9, en;q=0.8");
        headers.put("Accept-Encoding", "gzip,deflate");
        headers.put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        headers.put("Pragma", "no-cache");

        headers.put("Cache-Control", "no-cache");

        headers.put("Pragma", "no-cache");
        headers.put("Connection", "close");

    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public boolean isRequested() {
        return requested;
    }

    public String load() throws IOException {
        requestConnection();
        return htmlCode;
    }

    public boolean matches(Pattern pat) {
        return new Regex(htmlCode, pat).matches();
    }

    public boolean matches(String pat) {
        return new Regex(htmlCode, pat).matches();
    }

    private void openConnection() throws IOException {

        // if (request.getHttpConnection().getResponseCode() == 401 &&
        // logins.containsKey(request.getUrl().getHost())) {
        // this.getHeaders().put("Authorization", "Basic " +
        // Encoding.Base64Encode(logins.get(request.getUrl().getHost())[0] + ":"
        // + logins.get(request.getUrl().getHost())[1]));
        //
        // request.getHttpConnection().disconnect();
        // return this.getPage(string);
        //
        // }

        long tima = System.currentTimeMillis();

        // der aufruf ist ohne proxy
        // der hier mit proxy..
        // da k�nnte man sich mal schlauch machen.. welche proxy typen da
        // unterst�tzt werden
        if (!headers.contains("Host")) {
            headers.setAt(0, "Host", url.getHost());
        }
        if (proxy != null) {

            httpConnection = (URLConnectionAdapter) url.openConnection(proxy);

        } else {
            httpConnection = (URLConnectionAdapter) url.openConnection();

        }
        httpConnection.setRequest(this);
        httpConnection.setInstanceFollowRedirects(followRedirects);
        requestTime = System.currentTimeMillis() - tima;
        httpConnection.setReadTimeout(readTimeout);
        httpConnection.setConnectTimeout(connectTimeout);

        if (headers != null) {

            for (int i = 0; i < headers.size(); i++) {

                httpConnection.setRequestProperty(headers.getKey(i), headers.getValue(i));
            }
        }
        preRequest(httpConnection);
        if (hasCookies()) {
            httpConnection.setRequestProperty("Cookie", getCookieString());
        }

    }

    public abstract void postRequest(URLConnectionAdapter httpConnection) throws IOException;

    abstract public void preRequest(URLConnectionAdapter httpConnection) throws IOException;

    public String read() throws IOException {
        long tima = System.currentTimeMillis();
        this.htmlCode = read(httpConnection);
        readTime = System.currentTimeMillis() - tima;

        return htmlCode.toString();
    }

    public static String read(URLConnectionAdapter con) throws IOException {
        BufferedReader rd;
        if (con.getHeaderField("Content-Encoding") != null && con.getHeaderField("Content-Encoding").equalsIgnoreCase("gzip")) {

            rd = new BufferedReader(new InputStreamReader(new GZIPInputStream(con.getInputStream())));

        } else {
            String cs = con.getCharset();
            if (cs == null) {
                rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } else {
                try {
                    rd = new BufferedReader(new InputStreamReader(con.getInputStream(), cs));

                } catch (Exception e) {
                    jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
                    System.err.println(con);
                    try {
                        rd = new BufferedReader(new InputStreamReader(con.getInputStream(), cs.replace("-", "")));
                    } catch (Exception e2) {

                        rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    }
                }

            }

        }
        String line;
        StringBuilder htmlCode = new StringBuilder();
        while ((line = rd.readLine()) != null) {
            htmlCode.append(line + "\r\n");
        }
        rd.close();
        return htmlCode.toString();
    }

    private void requestConnection() throws IOException {
        connect();
        htmlCode = read();

    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    // public void setProxy(String ip, String port) throws
    // NumberFormatException, MalformedURLException {
    // proxyip = ip;
    // proxyport = port;
    // if (ip == null || port == null) return;
    // url = new URL("http", proxyip, Integer.parseInt(proxyport),
    // url.toString());
    //
    // }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    // @Override
    public String toString() {
        if (!requested) { return "Request not sent yet"; }

        if (htmlCode == null || htmlCode.length() == 0) {
            if (getLocation() != null) { return "Not HTML Code. Redirect to: " + getLocation(); }
            return "No htmlCode read";
        }

        return htmlCode;
    }

    public void setHtmlCode(String htmlCode) {
        this.htmlCode = htmlCode;
    }

    @SuppressWarnings("unchecked")
    public Request toHeadRequest() throws MalformedURLException {
        Request ret = new Request(this.getUrl() + "") {

            // @Override
            public void postRequest(URLConnectionAdapter httpConnection) throws IOException {
            }

            // @Override
            public void preRequest(URLConnectionAdapter httpConnection) throws IOException {
                httpConnection.setRequestMethod("HEAD");
            }

        };
        ret.connectTimeout = this.connectTimeout;

        ret.cookies = (ArrayList<Cookie>) this.getCookies().clone();
        ret.followRedirects = this.followRedirects;
        ret.headers = (RequestHeader) this.getHeaders().clone();
        ret.setProxy(proxy);
        ret.readTime = this.readTimeout;

        ret.httpConnection = this.httpConnection;

        return ret;

    }

    public Request cloneRequest() {
        // TODO Auto-generated method stub
        return null;
    }

}
