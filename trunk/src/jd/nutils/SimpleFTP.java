/* 
Copyright Paul James Mutton, 2001-2004, http://www.jibble.org/

This file is part of SimpleFTP.

This software is dual-licensed, allowing you to choose between the GNU
General Public License (GPL) and the www.jibble.org Commercial License.
Since the GPL may be too restrictive for use in a proprietary application,
a commercial license is also provided. Full license information can be
found at http://www.jibble.org/licenses/

$Author: pjm2 $
$Id: SimpleFTP.java,v 1.2 2004/05/29 19:27:37 pjm2 Exp $

 */
package jd.nutils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.StringTokenizer;

import jd.parser.Regex;

/**
 * SimpleFTP is a simple package that implements a Java FTP client. With
 * SimpleFTP, you can connect to an FTP server and upload multiple files.
 * <p>
 * Copyright Paul Mutton, <a
 * href="http://www.jibble.org/">http://www.jibble.org/</a>
 */
public class SimpleFTP {
    private static boolean DEBUG = true;
    private BufferedReader reader = null;
    private Socket socket = null;
    private BufferedWriter writer = null;
    private String dir = "/";

    /**
     * Create an instance of SimpleFTP.
     */
    public SimpleFTP() {
    }

    /**
     * Enter ASCII mode for sending text files. This is usually the default
     * mode. Make sure you use binary mode if you are sending images or other
     * binary data, as ASCII mode is likely to corrupt them.
     */
    public synchronized boolean ascii() throws IOException {
        sendLine("TYPE A");
        String response = readLine();
        return response.startsWith("200 ");
    }

    /**
     * Enter binary mode for sending binary files.
     */
    public synchronized boolean bin() throws IOException {
        sendLine("TYPE I");
        String response = readLine();
        return response.startsWith("200 ");
    }

    /**
     * Connects to the default port of an FTP server and logs in as
     * anonymous/anonymous.
     */
    public synchronized void connect(String host) throws IOException {
        connect(host, 21);
    }

    /**
     * Connects to an FTP server and logs in as anonymous/anonymous.
     */
    public synchronized void connect(String host, int port) throws IOException {
        connect(host, port, "anonymous", "anonymous");
    }

    /**
     * Connects to an FTP server and logs in with the supplied username and
     * password.
     */
    public synchronized void connect(String host, int port, String user, String pass) throws IOException {
        if (socket != null) { throw new IOException("SimpleFTP is already connected. Disconnect first."); }
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        String response = readLine();
        if (!response.startsWith("220 ")) { throw new IOException("SimpleFTP received an unknown response when connecting to the FTP server: " + response); }
        sendLine("USER " + user);
        response = readLine();
        if (!response.startsWith("331 ")) { throw new IOException("SimpleFTP received an unknown response after sending the user: " + response); }
        sendLine("PASS " + pass);
        response = readLine();
        if (!response.startsWith("230 ")) { throw new IOException("SimpleFTP was unable to log in with the supplied password: " + response); }
        sendLine("PWD");
        response = readLine();
        if (!response.startsWith("257 ")) { throw new IOException("PWD COmmand not understood " + response); }

        // Response: 257 "/" is the current directory
        dir = new Regex(response, "\"(.*)\"").getMatch(0);
        dir = dir;
        // Now logged in.
    }

    /**
     * Changes the working directory (like cd). Returns true if
     * successful.RELATIVE!!!
     */
    public synchronized boolean cwd(String dir) throws IOException {
        dir = dir.replaceAll("[\\\\|//]+?", "/");
        sendLine("CWD " + dir);
        String response = readLine();
        boolean ret = response.startsWith("250 ");
        if (!ret) return ret;
        if (!dir.endsWith("/") && !dir.endsWith("\\")) dir += "/";
        if (dir.startsWith("/")) {
            this.dir = dir;
        } else {
            this.dir += dir;
        }

        return ret;
    }

    /**
     * Disconnects from the FTP server.
     */
    public synchronized void disconnect() throws IOException {
        try {
            sendLine("QUIT");
        } finally {
            socket = null;
        }
    }

    /**
     * Returns the working directory of the FTP server it is connected to.
     */
    public synchronized String pwd() throws IOException {
        sendLine("PWD");
        String dir = null;
        String response = readLine();
        if (response.startsWith("257 ")) {
            int firstQuote = response.indexOf('\"');
            int secondQuote = response.indexOf('\"', firstQuote + 1);
            if (secondQuote > 0) {
                dir = response.substring(firstQuote + 1, secondQuote);
            }
        }
        return dir;
    }

    private String readLine() throws IOException {
        String line = reader.readLine();
        if (DEBUG) {
            System.out.println("< " + line);
        }
        return line;
    }

    public boolean remove(String string) throws IOException {
        sendLine("DELE " + string);
        String response = readLine();
        if (response.startsWith("250")) { return true; }
        return false;
    }

    public boolean rename(String from, String to) throws IOException {
        sendLine("RNFR " + from);
        String response = readLine();
        if (!response.startsWith("350")) { return false; }
        sendLine("RNTO " + to);
        response = readLine();
        if (response.startsWith("250")) { return true; }
        return false;
    }

    /**
     * Sends a raw command to the FTP server.
     */
    private void sendLine(String line) throws IOException {
        if (socket == null) { throw new IOException("SimpleFTP is not connected."); }
        try {

            writer.write(line + "\r\n");
            writer.flush();
            if (DEBUG) {
                System.out.println("> " + line);
            }
        } catch (IOException e) {
            socket = null;
            throw e;
        }
    }

    /**
     * Sends a file to be stored on the FTP server. Returns true if the file
     * transfer was successful. The file is sent in passive mode to avoid NAT or
     * firewall problems at the client end.
     */
    public synchronized boolean stor(File file) throws IOException {
        if (file.isDirectory()) { throw new IOException("SimpleFTP cannot upload a directory."); }
        String filename = file.getName();
        return stor(new FileInputStream(file), filename);
    }

    /**
     * Sends a file to be stored on the FTP server. Returns true if the file
     * transfer was successful. The file is sent in passive mode to avoid NAT or
     * firewall problems at the client end.
     */
    public synchronized boolean stor(InputStream inputStream, String filename) throws IOException {
        BufferedInputStream input = new BufferedInputStream(inputStream);
        InetSocketAddress pasv = pasv();
        sendLine("STOR " + filename);
        Socket dataSocket = new Socket(pasv.getHostName(), pasv.getPort());
        String response = readLine();
        if (!response.startsWith("150 ") && !response.startsWith("125 ")) { throw new IOException("SimpleFTP was not allowed to send the file: " + response); }
        BufferedOutputStream output = new BufferedOutputStream(dataSocket.getOutputStream());
        byte[] buffer = new byte[4096];
        int bytesRead = 0;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        output.flush();
        output.close();
        input.close();
        response = readLine();
        return response.startsWith("226 ");
    }

    private InetSocketAddress pasv() throws IOException {
        sendLine("PASV");
        String response = readLine();
        if (!response.startsWith("227 ")) { throw new IOException("SimpleFTP could not request passive mode: " + response); }
        String ip = null;
        int port = -1;
        int opening = response.indexOf('(');
        int closing = response.indexOf(')', opening + 1);
        if (closing > 0) {
            String dataLink = response.substring(opening + 1, closing);
            StringTokenizer tokenizer = new StringTokenizer(dataLink, ",");
            try {
                ip = tokenizer.nextToken() + "." + tokenizer.nextToken() + "." + tokenizer.nextToken() + "." + tokenizer.nextToken();
                port = Integer.parseInt(tokenizer.nextToken()) * 256 + Integer.parseInt(tokenizer.nextToken());
                return new InetSocketAddress(ip, port);
            } catch (Exception e) {
                throw new IOException("SimpleFTP received bad data link information: " + response);
            }
        }
        throw new IOException("SimpleFTP received bad data link information: " + response);
    }

    /**
     * creates directories
     * 
     * @param cw
     * @return
     * @throws IOException
     */
    public boolean mkdir(String cw) throws IOException {
        String tmp = this.dir;
        cw = cw.replace("\\", "/");
        if (cw.startsWith(this.dir)) cw = cw.substring(this.dir.length());
        boolean ret = true;
        String ddir = tmp;
        String[] dirs = cw.split("[\\\\|/]{1}");

        for (String d : dirs) {
            if (d == null || d.trim().length() == 0) continue;

            sendLine("MKD " + d);
            String response = readLine();
            if (!response.startsWith("257 ") && !response.startsWith("550 ")) {

                ret = false;
                break;
            }
            ddir += d + "/";
            cwd(ddir);
        }

        this.cwd(tmp);

        return ret;
    }

    public boolean cwdAdd(String cw) throws IOException {
        if (cw.startsWith("/") || cw.startsWith("\\")) cw = cw.substring(1);
        return cwd(dir + cw);

    }

    public String getDir() {
        return dir;
    }

    /**
     * UPloads varios files to a single remotefolder
     * 
     * @param ip
     * @param port
     * @param user
     * @param password
     * @param destfolder
     * @param src
     * @throws IOException
     */
    public static void upload(String ip, int port, String user, String password, String destfolder, File... src) throws IOException {

        SimpleFTP ftp = new SimpleFTP();
        ftp.connect(ip, port, user, password);
        ftp.bin();
        ftp.cwd(destfolder);
        for (File f : src) {
            ftp.stor(f);
        }

        // Quit from the FTP server.
        ftp.disconnect();

    }

    /**
     * Uploades files to a remotefolder and downloads them again to check for
     * transfer errors
     * 
     * @param ip
     * @param port
     * @param user
     * @param password
     * @param destfolder
     * @param src
     * @throws IOException
     */
    public static void uploadtoFolderSecure(String ip, int port, String user, String password, String destfolder, File... src) throws IOException {

        SimpleFTP ftp = new SimpleFTP();
        ftp.connect(ip, port, user, password);
        ftp.bin();
        ftp.cwd(destfolder);
        for (File f : src) {
            ftp.stor(f);

            File dummy = File.createTempFile("simpleftp_secure", null);

            ftp.download(f.getName(), dummy);

            if (!JDHash.getMD5(dummy).equalsIgnoreCase(JDHash.getMD5(f))) {
                throw new IOException("MD5 check failed for: " + f);
            } else {
                if (DEBUG) {
                    System.out.println("---- MD5 OK: /" + ftp.getDir() + "" + f.getName() + " -----");
                }
            }
            dummy.delete();
        }

        // Quit from the FTP server.
        ftp.disconnect();

    }

    public static void uploadSecure(String ip, int port, String user, String password, String destfolder, File root, File... list) throws IOException {
        SimpleFTP ftp = new SimpleFTP();
        ftp.connect(ip, port, user, password);
        ftp.bin();

        for (File f : list) {
            if (!f.getAbsolutePath().startsWith(root.getAbsolutePath())) { throw new IOException(f + " is not part of " + root); }
            String subfolder = f.getParentFile().getAbsolutePath().replace(root.getAbsolutePath(), "");
            if (!ftp.cwd(mergeFolders(destfolder, subfolder))) {
                ftp.mkdir(mergeFolders(destfolder, subfolder));
                if (!ftp.cwd(mergeFolders(destfolder, subfolder))) { throw new IOException("Unexpected error"); }
            }

            File dummy = File.createTempFile("simpleftp_secure", null);
            try {
                ftp.download(f.getName(), dummy);
                if (JDHash.getMD5(dummy).equalsIgnoreCase(JDHash.getMD5(f))) {
                    if (DEBUG) {
                        System.out.println("---- Skip .MD5 ok: " + ftp.getDir() + "" + f.getName() + " -----");
                    }
                    continue;

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            dummy.delete();
            ftp.stor(f);

            ftp.download(f.getName(), dummy);

            if (!JDHash.getMD5(dummy).equalsIgnoreCase(JDHash.getMD5(f))) {
                throw new IOException("MD5 check failed for: " + f);
            } else {
                if (DEBUG) {
                    System.out.println("---- MD5 OK: /" + ftp.getDir() + "" + f.getName() + " -----");
                }
            }
            dummy.delete();
        }

        // Quit from the FTP server.
        ftp.disconnect();

    }

    /**
     * m,erges to folderparts and takes care that there is no double "/"
     * 
     * @param dirs
     * @return
     */
    private static String mergeFolders(String... dirs) {
        String res = dirs[0];

        for (int i = 1; i < dirs.length; i++) {
            while (res.endsWith("/") || res.endsWith("\\"))
                res = res.substring(0, res.length() - 1);
            while (dirs[i].startsWith("/") || dirs[i].startsWith("\\"))
                dirs[i] = dirs[i].substring(1);
            res += "/" + dirs[i];

        }
        return res;

    }

    public void download(String filename, File file) throws IOException {
        InetSocketAddress pasv = pasv();

        sendLine("RETR " + filename);

        Socket dataSocket = new Socket(pasv.getHostName(), pasv.getPort());
        BufferedInputStream input = new BufferedInputStream(dataSocket.getInputStream());
        DataOutputStream out = new DataOutputStream(new FileOutputStream(file));

        String response = readLine();
        if (!response.startsWith("150")) { throw new IOException("Unexpected Response: " + response); }
        byte[] buffer = new byte[4096];
        int bytesRead = 0;
        while ((bytesRead = input.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);

        }

        out.flush();
        out.close();
        input.close();
        response = readLine();
        if (!response.startsWith("226")) { throw new IOException("Download failed: " + response); }

    }

}