package jd.plugins.optional.webinterface;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jd.config.SubConfiguration;
import jd.parser.Regex;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;

public class JDSimpleWebserver extends Thread {

    private ServerSocket Server_Socket;

    private boolean Server_Running = true;

    private Logger logger = JDUtilities.getLogger();

    public static int CURRENT_CLIENT_COUNTER = 0;

    private static int max_clientCounter = 0;

    private static String AuthUser = "";
    private static boolean NeedAuth = false;

    public JDSimpleWebserver() {

        SubConfiguration subConfig = JDUtilities.getSubConfig("WEBINTERFACE");
        max_clientCounter = subConfig.getIntegerProperty(JDWebinterface.PROPERTY_CONNECTIONS, 10);
        AuthUser = "Basic " + JDUtilities.Base64Encode(subConfig.getStringProperty(JDWebinterface.PROPERTY_USER, "JD") + ":" + subConfig.getStringProperty(JDWebinterface.PROPERTY_PASS, "JD"));
        NeedAuth = subConfig.getBooleanProperty(JDWebinterface.PROPERTY_LOGIN, true);
        try {
            Server_Socket = new ServerSocket(subConfig.getIntegerProperty(JDWebinterface.PROPERTY_PORT, 8765));
            logger.info("Webinterface: Server started");
            start();
        } catch (IOException e) {
            logger.severe("WebInterface: Server failed to start!");
        }
    }

    public void run() {
        while (Server_Running) {
            try {
                while (getCurrentClientCounter() >= max_clientCounter) {
                    try {
                        /* logger.info("warte"); */
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                    ;
                }
                ;
                Socket Client_Socket = Server_Socket.accept();
                // logger.info("WebInterface: Client[" +
                // getCurrentClientCounter() + "/" + max_clientCounter +
                // "] connecting from " + Client_Socket.getInetAddress());

                Thread client_thread = new Thread(new JDRequestHandler(Client_Socket));
                client_thread.start();

            } catch (IOException e) {
                logger.severe("WebInterface: Client-Connection failed");
            }
        }
    }

    private class JDRequestHandler implements Runnable {

        private Socket Current_Socket;

        private Logger logger = JDUtilities.getLogger();

        public JDRequestHandler(Socket Client_Socket) {
            this.Current_Socket = Client_Socket;
        }

        public void run() {
            addToCurrentClientCounter(1);
            run0();
            addToCurrentClientCounter(-1);

        }

        public String readline(BufferedInputStream reader) {
            /* ne eigene readline für BufferedInputStream */
            /*
             * BufferedReader hat nur böse Probleme mit dem Verarbeiten von
             * FileUploads gehabt
             */
            int max_buf = 1024;
            byte[] buffer = new byte[max_buf];
            int index = 0;
            int byteread = 0;
            try {

                while ((byteread = reader.read()) != -1) {
                    if (byteread == 10 || byteread == 13) {
                        reader.mark(0);
                        if ((byteread = reader.read()) != -1) {
                            if (byteread == 13 || byteread == 10) {
                                break;
                            } else {
                                reader.reset();
                                break;
                            }
                        }
                    }
                    if (index > max_buf) return null;
                    buffer[index] = (byte) byteread;
                    index++;
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return new String(buffer).substring(0, index);
        }

        public void run0() {
            try {
                InputStream requestInputStream = Current_Socket.getInputStream();
                BufferedInputStream reader = new BufferedInputStream(requestInputStream);

                String line = null;
                HashMap<String, String> headers = new HashMap<String, String>();

                while ((line = readline(reader)) != null && line.trim().length() > 0) {
                    String key = null;
                    String value = null;
                    if (line.indexOf(": ") > 0) {
                        key = line.substring(0, line.indexOf(": ")).toLowerCase();
                        value = line.substring(line.indexOf(": ") + 2);
                    } else {
                        key = null;
                        value = line;
                    }
                    headers.put(key, value);
                }

                if (headers.containsKey(null)) {
                    String Method = headers.get(null).split(" ")[0];
                    if (Method.compareToIgnoreCase("get") == 0 || Method.compareToIgnoreCase("post") == 0) {
                        /* get oder post header gefunden */
                        if (headers.containsKey("content-type")) {
                            if (headers.get("content-type").compareToIgnoreCase("application/x-www-form-urlencoded") == 0) {
                                if (headers.containsKey("content-length")) {
                                    /*
                                     * POST Form Daten in GET Format übersetzen,
                                     * damit der RequestParams Parser nicht
                                     * geändert werden muss
                                     */
                                    int post_len = new Integer(headers.get("content-length"));
                                    int post_len_toread = new Integer(post_len);
                                    int post_len_read = new Integer(0);
                                    byte[] cbuf = new byte[post_len];
                                    int indexstart = 0;
                                    while (post_len_toread > 0) {
                                        if ((post_len_read = reader.read(cbuf, indexstart, post_len_toread)) == -1) break;
                                        indexstart = indexstart + post_len_read;
                                        post_len_toread = post_len_toread - post_len_read;
                                    }
                                    String RequestParams = new String(cbuf).trim();
                                    if (indexstart == post_len) {
                                        /*
                                         * alten POST aus Header Liste holen,
                                         * neuen zusammenbauen
                                         */
                                        String request = headers.get(null);
                                        String[] requ = request.split(" ");
                                        if (Method.compareToIgnoreCase("post") == 0) {
                                            headers.put(null, requ[0] + " " + requ[1] + "?" + RequestParams + " " + requ[2]);
                                        } else
                                            logger.severe("POST Daten bei nem GET aufruf???");
                                    } else {
                                        logger.severe("POST Fehler postlen soll = " + post_len + " postlen gelesen = " + post_len_read);
                                    }

                                }
                            } else if (headers.get("content-type").contains("multipart/form-data")) {
                                /*
                                 * POST Form Daten in GET Format übersetzen,
                                 * damit der RequestParams Parser nicht geändert
                                 * werden muss
                                 * 
                                 * Zusätzlich das File auslesen (die komplette
                                 * Verarbeiten findet auf Hex statt!!)
                                 */
                                if (headers.containsKey("content-length")) {
                                    int post_len = new Integer(headers.get("content-length"));
                                    int post_len_toread = new Integer(post_len);
                                    int post_len_read = new Integer(0);
                                    byte[] cbuf = new byte[post_len];
                                    int indexstart = 0;
                                    String limiter = new Regex(headers.get("content-type"), Pattern.compile("boundary=(.*)", Pattern.CASE_INSENSITIVE)).getFirstMatch();
                                    if (limiter != null) {
                                        /*
                                         * nur weitermachen falls ein limiter
                                         * vorhanden ist
                                         */
                                        limiter = "--" + limiter;
                                        limiter = JDHexUtils.getHexString(limiter);
                                        while (post_len_toread > 0) {
                                            if ((post_len_read = reader.read(cbuf, indexstart, post_len_toread)) == -1) break;
                                            indexstart = indexstart + post_len_read;
                                            post_len_toread = post_len_toread - post_len_read;
                                        }
                                        if (indexstart == post_len) {
                                            String RequestParams = "";
                                            /*
                                             * momentan wird multipart nur für
                                             * containerupload genutzt, daher
                                             * form-data parsing unnötig
                                             *                                           
                                             */
                                            String MultiPartData[][] = new Regex(JDHexUtils.getHexString(cbuf), Pattern.compile(limiter + JDHexUtils.getHexString("\r") + "{0,1}" +  JDHexUtils.getHexString("\n") + "{0,1}" + JDHexUtils.REGEX_MATCH_ALL_HEX +"(?=" + "" + JDHexUtils.getHexString("\r")  + "{0,1}"  + JDHexUtils.getHexString("\n") + "{0,1}" + limiter + ")", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatches();
                                            for (int i = 0; i < MultiPartData.length; i++) {
                                                if (MultiPartData[i][0].contains(JDHexUtils.getHexString("Content-Disposition: form-data; name=\"container\""))) {
                                                    String containertyp = new Regex(MultiPartData[i][0], Pattern.compile(JDHexUtils.getHexString("filename=\"") + JDHexUtils.REGEX_FIND_ALL_HEX + JDHexUtils.getHexString(".") + JDHexUtils.REGEX_MATCH_ALL_HEX + JDHexUtils.getHexString("\""), Pattern.CASE_INSENSITIVE)).getFirstMatch();
                                                    if (containertyp != null) containertyp = new String(JDHexUtils.getByteArray(containertyp));
                                                    if (containertyp != null && (containertyp.contains("dlc") || containertyp.contains("ccf") || containertyp.contains("rsdf")||true)) {
                                                        File containerfile = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + "." + containertyp);
                                                        if (JDUtilities.savetofile(containerfile, JDHexUtils.getByteArray(MultiPartData[i][0].substring(MultiPartData[i][0].indexOf(JDHexUtils.getHexString("\r\n\r\n")) + 8)))) {
                                                            /*
                                                             * RequestParameter
                                                             * zusammenbauen
                                                             */
                                                            RequestParams = "do=Upload&file=" + JDUtilities.urlEncode(containerfile.getName());
                                                            break;
                                                        }
                                                    } else {
                                                        if (containertyp != null) logger.severe("unknown container typ: " + containertyp);
                                                    }
                                                }
                                            }
                                            /*
                                             * alten POST aus Header Liste
                                             * holen, neuen zusammenbauen
                                             */
                                            String request = headers.get(null);
                                            String[] requ = request.split(" ");
                                            if (Method.compareToIgnoreCase("post") == 0) {
                                                headers.put(null, requ[0] + " " + requ[1] + "?" + RequestParams + " " + requ[2]);
                                            } else
                                                logger.severe("POST Daten bei nem GET aufruf???");
                                        } else {
                                            logger.severe("POST Fehler postlen soll = " + post_len + " postlen gelesen = " + post_len_read);
                                        }
                                    }
                                }
                            }
                        }

                        JDSimpleWebserverResponseCreator response = new JDSimpleWebserverResponseCreator();
                        JDSimpleWebserverRequestHandler request = new JDSimpleWebserverRequestHandler(headers, response);
                        OutputStream outputStream = Current_Socket.getOutputStream();
                        if (NeedAuth == true) {/* need authorization */
                            if (headers.containsKey("authorization")) {
                                if (JDSimpleWebserver.AuthUser.equals(headers.get("authorization"))) {
                                    /*
                                     * send authorization granted
                                     */
                                    /* logger.info("pass stimmt"); */
                                    request.handle();

                                } else { /* send authorization failed */
                                    response.setAuth_failed();
                                }
                            } else { /* send autorization needed */
                                response.setAuth_needed();
                            }
                        } else { /* no autorization needed */
                            request.handle();
                        }

                        response.writeToStream(outputStream);
                        outputStream.close();
                    }
                } else {
                    /* kein get oder post header */
                    logger.severe("kein post oder get header");
                }
                Current_Socket.close();

            } catch (SocketException e) {
                logger.severe("WebInterface: Socket error");
            } catch (IOException e) {
                logger.severe("WebInterface: I/O Error");
            }
        }
    }

    /**
     * greift Threadsafe auf den clientcounter zu
     * 
     * @return
     */
    public synchronized int getCurrentClientCounter() {
        return CURRENT_CLIENT_COUNTER;
    }

    /**
     * Fügt einen Wert zum aktuellen Clientzähler hinzu
     * 
     * @param i
     * @return
     */
    public synchronized int addToCurrentClientCounter(int i) {
        CURRENT_CLIENT_COUNTER += i;
        return CURRENT_CLIENT_COUNTER;
    }

    /**
     * setzt den aktuellen Client Zähler.
     * 
     * @param current_clientCounter
     */
    public synchronized void setCurrentClientCounter(int cc) {
        CURRENT_CLIENT_COUNTER = cc;
    }

}
