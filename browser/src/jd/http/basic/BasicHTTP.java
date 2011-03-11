package jd.http.basic;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import jd.http.HTTPConnectionFactory;
import jd.http.HTTPProxy;
import jd.http.URLConnectionAdapter;
import jd.http.URLConnectionAdapter.RequestMethod;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.utils.Application;
import org.appwork.utils.net.DownloadProgress;

public class BasicHTTP {

    private static final Object CALL_LOCK = new Object();

    public static void main(final String[] args) throws MalformedURLException, IOException, InterruptedException {

        // final BasicHTTP client = new BasicHTTP();
        // client.setProxy(prox);
        // System.out.println(client.getPage(new
        // URL("http://ipcheck0.jdowfnloader.org")));

        // System.out.println(new BasicHTTP().postPage(new
        // URL("http://ipcheck0.jdownloader.org"), "BKA"));
    }

    private final HashMap<String, String> requestHeader;

    private URLConnectionAdapter connection;

    private int connectTimeout = 15000;

    private int readTimeout = 30000;

    private HTTPProxy proxy;

    public BasicHTTP() {
        requestHeader = new HashMap<String, String>();

    }

    public void clearRequestHeader() {
        requestHeader.clear();
    }

    /**
     * @param url
     * @param progress
     * @param file
     * @throws InterruptedException
     * @throws IOException
     */
    public void download(final URL url, final DownloadProgress progress, final File file) throws IOException, InterruptedException {
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(file));
            try {
                this.download(url, progress, 0, out);
            } catch (final IOException e) {
                try {
                    out.close();
                } catch (final Throwable t) {
                }

                if (file.length() > 0) {
                    final IOException ex = new BasicHTTPException(connection, e);
                    file.delete();
                    throw ex;
                }
            }
        } finally {
            try {
                out.close();
            } catch (final Throwable t) {
            }
        }
    }

    public byte[] download(final URL url, final DownloadProgress progress, final long maxSize) throws IOException, InterruptedException {

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            this.download(url, progress, maxSize, baos);
        } catch (final IOException e) {
            if (baos.size() > 0) { throw new BasicHTTPException(connection, e); }
        }
        try {
            baos.close();
        } catch (final Throwable t) {
        }
        return baos.toByteArray();
    }

    /**
     * 
     * Please do not forget to close the output stream.
     * 
     * @param url
     * @param progress
     * @param maxSize
     * @param baos
     * @throws IOException
     * @throws InterruptedException
     */
    public void download(final URL url, final DownloadProgress progress, final long maxSize, final OutputStream baos) throws IOException, InterruptedException {
        BufferedInputStream input = null;
        GZIPInputStream gzi = null;
        try {

            connection = HTTPConnectionFactory.createHTTPConnection(url, proxy);
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);
            connection.setRequestProperty("Accept-Language", TranslationFactory.getDesiredLanguage());
            connection.setRequestProperty("User-Agent", "AppWork " + Application.getApplication());
            connection.setRequestProperty("Connection", "Close");
            for (final Entry<String, String> next : requestHeader.entrySet()) {
                connection.setRequestProperty(next.getKey(), next.getValue());
            }
            connection.connect();

            if (connection.getHeaderField("Content-Encoding") != null && connection.getHeaderField("Content-Encoding").equalsIgnoreCase("gzip")) {
                input = new BufferedInputStream(gzi = new GZIPInputStream(connection.getInputStream()));
            } else {
                input = new BufferedInputStream(connection.getInputStream());
            }

            if (maxSize > 0 && connection.getContentLength() > maxSize) { throw new IOException("Max size exeeded!"); }
            if (progress != null) {
                progress.setTotal(connection.getContentLength());
            }
            final byte[] b = new byte[32767];
            int len;
            long loaded = 0;
            while ((len = input.read(b)) != -1) {
                if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
                if (len > 0) {
                    baos.write(b, 0, len);
                    loaded += len;
                    if (maxSize > 0 && loaded > maxSize) { throw new IOException("Max size exeeded!"); }
                }
                if (progress != null) {
                    progress.increaseLoaded(len);
                }
            }

        } finally {
            try {
                input.close();
            } catch (final Exception e) {
            }
            try {
                gzi.close();
            } catch (final Exception e) {
            }
            try {
                connection.disconnect();
            } catch (final Throwable e) {
            }

        }
    }

    public URLConnectionAdapter getConnection() {
        return connection;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public String getPage(final URL url) throws IOException, InterruptedException {
        synchronized (BasicHTTP.CALL_LOCK) {
            BufferedReader in = null;
            InputStreamReader isr = null;
            try {

                connection = HTTPConnectionFactory.createHTTPConnection(url, proxy);
                connection.setConnectTimeout(connectTimeout);
                connection.setReadTimeout(readTimeout);
                connection.setRequestProperty("Accept-Language", TranslationFactory.getDesiredLanguage());

                connection.setRequestProperty("User-Agent", "AppWork " + Application.getApplication());
                connection.setRequestProperty("Connection", "Close");
                connection.setRequestProperty("Accept-Charset", "UTF-8");
                for (final Entry<String, String> next : requestHeader.entrySet()) {
                    connection.setRequestProperty(next.getKey(), next.getValue());
                }
                int lookupTry = 0;
                while (true) {
                    try {
                        connection.connect();
                        break;
                    } catch (final UnknownHostException e) {
                        if (++lookupTry > 3) { throw e; }
                        /* dns lookup failed, short wait and try again */
                        Thread.sleep(200);
                    }
                }

                in = new BufferedReader(isr = new InputStreamReader(connection.getInputStream(), "UTF-8"));

                String str;
                final StringBuilder sb = new StringBuilder();
                while ((str = in.readLine()) != null) {
                    if (sb.length() > 0) {
                        sb.append("\r\n");
                    }
                    sb.append(str);

                }

                return sb.toString();

            } finally {
                try {
                    in.close();
                } catch (final Throwable e) {
                }
                try {
                    isr.close();
                } catch (final Throwable e) {
                }
                try {
                    connection.disconnect();
                } catch (final Throwable e) {
                }

            }
        }
    }

    public HTTPProxy getProxy() {
        return proxy;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * @return
     */
    public HashMap<String, String> getRequestHeader() {
        return requestHeader;
    }

    public String getRequestHeader(final String key) {
        return requestHeader.get(key);
    }

    public String getResponseHeader(final String string) {
        synchronized (BasicHTTP.CALL_LOCK) {
            if (connection == null) { return null; }
            return connection.getHeaderField(string);

        }
    }

    public URLConnectionAdapter openGetConnection(final URL url) throws IOException, InterruptedException {
        return this.openGetConnection(url, readTimeout);
    }

    public URLConnectionAdapter openGetConnection(final URL url, final int readTimeout) throws IOException, InterruptedException {
        synchronized (BasicHTTP.CALL_LOCK) {
            try {

                connection = HTTPConnectionFactory.createHTTPConnection(url, proxy);
                connection.setConnectTimeout(connectTimeout);
                connection.setReadTimeout(readTimeout < 0 ? this.readTimeout : readTimeout);
                connection.setRequestProperty("Accept-Language", TranslationFactory.getDesiredLanguage());
                connection.setRequestProperty("User-Agent", "AppWork " + Application.getApplication());
                connection.setRequestProperty("Connection", "Close");
                connection.setRequestProperty("Accept-Charset", "UTF-8");
                for (final Entry<String, String> next : requestHeader.entrySet()) {
                    connection.setRequestProperty(next.getKey(), next.getValue());
                }
                int lookupTry = 0;
                while (true) {
                    try {
                        connection.connect();
                        break;
                    } catch (final UnknownHostException e) {
                        if (++lookupTry > 3) { throw e; }
                        /* dns lookup failed, short wait and try again */
                        Thread.sleep(200);
                    }
                }
                return connection;
            } finally {
                try {
                    connection.disconnect();
                } catch (final Throwable e2) {
                }

            }
        }
    }

    public URLConnectionAdapter openPostConnection(final URL url, final String postData, final HashMap<String, String> header) throws IOException, InterruptedException {
        synchronized (BasicHTTP.CALL_LOCK) {
            OutputStreamWriter writer = null;
            OutputStream outputStream = null;
            try {

                connection = HTTPConnectionFactory.createHTTPConnection(url, proxy);
                connection.setConnectTimeout(connectTimeout);
                connection.setReadTimeout(readTimeout);
                connection.setRequestMethod(RequestMethod.POST);

                connection.setRequestProperty("Accept-Language", TranslationFactory.getDesiredLanguage());
                connection.setRequestProperty("User-Agent", "AppWork " + Application.getApplication());
                connection.setRequestProperty(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, postData.getBytes().length + "");

                connection.setRequestProperty("Connection", "Close");
                /* connection specific headers */
                if (header != null) {
                    for (final Entry<String, String> next : header.entrySet()) {
                        connection.setRequestProperty(next.getKey(), next.getValue());
                    }
                }
                for (final Entry<String, String> next : requestHeader.entrySet()) {
                    connection.setRequestProperty(next.getKey(), next.getValue());
                }

                int lookupTry = 0;
                while (true) {
                    try {
                        connection.connect();
                        break;
                    } catch (final UnknownHostException e) {
                        if (++lookupTry > 3) { throw e; }
                        /* dns lookup failed, short wait and try again */
                        Thread.sleep(200);
                    }
                }
                outputStream = connection.getOutputStream();
                writer = new OutputStreamWriter(outputStream);
                writer.write(postData);
                writer.flush();

                return connection;

            } finally {
                try {
                    connection.disconnect();
                } catch (final Throwable e2) {
                }
                try {
                    writer.close();
                } catch (final Throwable e) {
                }
                try {
                    outputStream.close();
                } catch (final Throwable e) {
                }
            }
        }
    }

    public String postPage(final URL url, final String data) throws IOException, InterruptedException {
        synchronized (BasicHTTP.CALL_LOCK) {
            OutputStreamWriter writer = null;
            BufferedReader reader = null;
            OutputStream outputStream = null;
            InputStreamReader isr = null;
            try {
                connection = HTTPConnectionFactory.createHTTPConnection(url, proxy);
                connection.setConnectTimeout(connectTimeout);
                connection.setReadTimeout(readTimeout);
                connection.setRequestMethod(RequestMethod.POST);
                connection.setRequestProperty("Accept-Language", TranslationFactory.getDesiredLanguage());
                connection.setRequestProperty("User-Agent", "AppWork " + Application.getApplication());
                connection.setRequestProperty(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, data.getBytes().length + "");

                connection.setRequestProperty("Connection", "Close");

                for (final Entry<String, String> next : requestHeader.entrySet()) {
                    connection.setRequestProperty(next.getKey(), next.getValue());
                }

                int lookupTry = 0;
                while (true) {
                    try {
                        connection.connect();
                        break;
                    } catch (final UnknownHostException e) {
                        if (++lookupTry > 3) { throw e; }
                        /* dns lookup failed, short wait and try again */
                        Thread.sleep(200);
                    }
                }
                outputStream = connection.getOutputStream();
                writer = new OutputStreamWriter(outputStream);
                writer.write(data);
                writer.flush();
                reader = new BufferedReader(isr = new InputStreamReader(connection.getInputStream(), "UTF-8"));
                final StringBuilder sb = new StringBuilder();
                String str;
                while ((str = reader.readLine()) != null) {
                    if (sb.length() > 0) {
                        sb.append("\r\n");
                    }
                    sb.append(str);

                }

                return sb.toString();

            } finally {
                try {
                    reader.close();
                } catch (final Throwable e) {
                }
                try {
                    isr.close();
                } catch (final Throwable e) {
                }
                try {
                    writer.close();
                } catch (final Throwable e) {
                }
                try {
                    outputStream.close();
                } catch (final Throwable e) {
                }
                try {
                    connection.disconnect();
                } catch (final Throwable e) {
                }

            }
        }
    }

    public void putRequestHeader(final String key, final String value) {
        requestHeader.put(key, value);
    }

    public void setConnectTimeout(final int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setProxy(final HTTPProxy proxy) {
        this.proxy = proxy;
    }

    public void setReadTimeout(final int readTimeout) {
        this.readTimeout = readTimeout;
    }

}
