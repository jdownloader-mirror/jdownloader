package jd.utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Logger;

import jd.plugins.HTTPPost;
import jd.plugins.Plugin;
import jd.plugins.RequestInfo;

public class CES extends Thread {
    private static final long MAX_TIME = 5 * 60 * 1000;
    private static final String PATTERN_TITLE = "<title>CaptchaExchangeServer - v° (°) by DVK</title>";
    private static final String PATTERN_STATS_RECEIVE = "Your balance is <b>°</b> points. You have received <b>°</b> captchas, recognized <b>°</b>, errors <b>° (°%)</b>";
    private static final String PATTERN_STATS_SENT = "You have sent on a server of <b>°</b> captchas, from them has been recognized <b>°</b>, errors <b>° (°%)</b>.";
    private static final String PATTERN_STATS_QUEUE = "There are <b>°</b> registered users now in queue on this server.";
    private static final String PATTERN_QUEUE = "<p>Your Username is <b>\"°\"</b>, your balance is <b>°</b> points. You have received <b>°</b> captchas, recognized <b>°</b>, errors <b>° (°%)</b>.<br>You have sent on a server of <b>°</b> captchas, from them has been recognized <b>°</b>, errors <b>° (°%)</b>.<br></p><p><br><div id=\"ta\">You are staying in queue to captcha recognition for <b>°</b> seconds.</div><br>There are <b>°</b> users in queue and <b>°</b> before you. (<a href=°>[show]</a>)<br><br>Please, wait a little more... (estimated waiting time: °.)<a href=°>[ reload ]";
    private static final long RECEIVE_INTERVAL = 15 * 1000;
    private static final long CHECK_INTERVAL = 15 * 1000;
    private static final int FLAG_ABORT_RECEIVING = 0;

    public static void main(String[] args) {
        File file = new File("C:/Users/coalado/.jd_home/captchas/rapidshare.com/21.04.2008_17.19.51.jpg");

        CES ces = new CES(file);
        ces.setLogins("coalado", "fdsfd");
        // ces.enterQueueAndWait();
        // if (ces.sendCaptcha()) {
        // ces.waitForAnswer();
        // }

        // ces.register("coalado");

        ces.login();
        JDUtilities.getLogger().info(ces.getServer());

    }

    private String cesVersion;
    private String cesDate;
    private String captchaID;
    private String userBalance;
    private boolean cesStatus;
    private String cesCode;
    private String statusText;
    private String captchaCode;
    private String price;
    private String recUser;
    private String recTime;
    private long startTime;
    private File file;
    private String md5;
    private String server = "http://dvk.com.ua/rapid/";
    private String boundary = "---------------------------19777693011381";
    private String user;
    private String pass;
    private int save = 0;
    private int type = 1;
    private String key = "jDoWnLoaDer106";
    private String regKey = "RapidByeByeBye";
    private Logger logger;
    private String balance;
    private String receivedCaptchas;
    private String recognizedCaptchas;
    private String receiveErrors;
    private String receiveErrorsPercent;
    private String sentCaptchasNum;
    private String sendRecognized;
    private String sentErrors;
    private Object sendErrorsPercent;
    private String queueLength;
    private String queuePosition;
    private String eta;
    private int abortFlag = -1;

    public CES(File captcha) {
        this.file = captcha;
        this.md5 = JDUtilities.getLocalHash(file);
        logger = JDUtilities.getLogger();

    }

    public void enterQueueAndWait() {
        while (true) {
            try {

                // http://dvk.com.ua/rapid/index.php?Nick=coalado&Pass=aCvtSmZwNCqm1&State=1
                if (this.abortFlag == FLAG_ABORT_RECEIVING) {
                    Plugin.postRequest(new URL(server + "index.php?Nick=" + user + "&Pass=" + pass + "&State=1"), "logout=Stop+captcha+recognition&State=4&Nick=" + user + "&Pass=" + pass);
                    return;
                }
                RequestInfo ri = Plugin.getRequest(new URL(server + "index.php?Nick=" + user + "&Pass=" + pass + "&State=1"));
                logger.info(ri.getHtmlCode());

                String[] answer;

                answer = Plugin.getSimpleMatches(ri.getHtmlCode(), PATTERN_QUEUE);
                this.balance = answer[1];
                this.receivedCaptchas = answer[2];
                this.recognizedCaptchas = answer[3];
                this.receiveErrors = answer[4];
                this.receiveErrorsPercent = answer[5];
                this.sentCaptchasNum = answer[6];
                this.sendRecognized = answer[7];
                this.sentErrors = answer[8];
                this.sendErrorsPercent = answer[9];
                this.queueLength = answer[11];
                this.queuePosition = answer[12];
                this.eta = answer[14];

                long i = RECEIVE_INTERVAL;

                while (i > 0) {
                    Thread.sleep(500);

                    i -= 500;

                    if (this.abortFlag == FLAG_ABORT_RECEIVING) {
                        Plugin.postRequest(new URL(server + "index.php?Nick=" + user + "&Pass=" + pass + "&State=1"), "logout=Stop+captcha+recognition&State=4&Nick=" + user + "&Pass=" + pass);
                        return;
                    }
                }

            } catch (Exception e) {

            }
        }

    }

    public void abortReceiving() {
        this.abortFlag = FLAG_ABORT_RECEIVING;
    }

    public boolean login() {
        RequestInfo ri;
        try {

            ri = Plugin.postRequest(new URL(server), null, null, null, "Nick=" + user + "&Pass=" + pass, true);
            String error = Plugin.getSimpleMatch(ri.getHtmlCode(), "<font color=\"red\"><h2>°</h2></font>", 0);
            logger.info(ri.getHtmlCode());
            this.statusText = error;
            if (error != null) {

            return false; }

            String[] answer = Plugin.getSimpleMatches(ri.getHtmlCode(), PATTERN_TITLE);
            this.cesVersion = answer[0];
            this.cesDate = answer[1];
            answer = Plugin.getSimpleMatches(ri.getHtmlCode(), PATTERN_STATS_RECEIVE);
            this.balance = answer[0];
            this.receivedCaptchas = answer[1];
            this.recognizedCaptchas = answer[2];
            this.receiveErrors = answer[3];
            this.receiveErrorsPercent = answer[4];
            answer = Plugin.getSimpleMatches(ri.getHtmlCode(), PATTERN_STATS_SENT);
            this.sentCaptchasNum = answer[0];
            this.sendRecognized = answer[1];
            this.sentErrors = answer[2];
            this.sendErrorsPercent = answer[3];
            answer = Plugin.getSimpleMatches(ri.getHtmlCode(), PATTERN_STATS_QUEUE);

            this.queueLength = answer[0];

        } catch (Exception e) {

        }
        return true;
    }

    public String register(String user) {

        RequestInfo ri;
        try {
            ri = Plugin.postRequest(new URL(server), null, null, null, "Nick=" + user + "&Pass=RapidByeByeBye", true);
            String pass = Plugin.getSimpleMatch(ri.getHtmlCode(), "You password is <b>°<b></h2>", 0);
            if (pass == null) {

                String error = Plugin.getSimpleMatch(ri.getHtmlCode(), "<font color=\"red\"><h2>°</h2></font>", 0);
                this.statusText = error;
            }
            return pass;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String waitForAnswer() {
        // <html><head></head><body>CaptchaExchangeServer - v2.06 (29-04-08
        // 02:00) by DVK<br>dvk_ok001; Captcha is loaded with ID(510), your
        // balance (0), please wait recognition...<br></body></html>
        this.startTime = System.currentTimeMillis();
        try {
            logger.info("WAIT FOR REC");
            while (true) {
                Thread.sleep(CHECK_INTERVAL);
                if (System.currentTimeMillis() - startTime > MAX_TIME) {
                    cesStatus = false;
                    statusText = "Timeout of " + MAX_TIME;
                    return null;
                }
                RequestInfo ri = Plugin.postRequest(new URL(server + "test.php"), null, null, null, "Nick=" + user + "&Pass=" + pass + "&Test=" + captchaID, true);
                logger.info(ri + "");
                if (ri.containsHTML("dvk_ok002")) {

                    String[] answer = Plugin.getSimpleMatches(ri.getHtmlCode(), "<html><head></head><body>CaptchaExchangeServer - v° (°) by DVK<br>dvk_ok002; AccessCode(°), price ° point, \"°\" recognized in ° seconds early, balance (°).<br></body></html>");
                    this.cesVersion = answer[0];
                    this.cesDate = answer[1];
                    this.cesStatus = true;
                    this.captchaCode = answer[2];
                    this.price = answer[3];
                    this.recUser = answer[4];
                    this.recTime = answer[5];

                    this.userBalance = answer[5];
                    return captchaCode;

                }

            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void setLogins(String user, String pass) {
        this.user = user;
        this.pass = pass;

    }

    private boolean sendCaptcha() {
        HTTPPost up = new HTTPPost(server + "post.php", true);
        up.setBoundary(boundary);
        up.doUpload();
        up.connect();
        up.setForm("Captcha");
        up.sendFile(file.getAbsolutePath(), file.getName());
        up.sendVariable("Nick", user);
        up.sendVariable("Pass", pass);
        up.sendVariable("Save", save + "");
        up.sendVariable("Link", "");
        up.sendVariable("Type", type + "");
        up.sendVariable("Key", getKey());
        up.sendVariable("Specs", "No premium user.\\s*(.*?)<img\\s+src=\"(http:\\/\\/rs.*?)\"[^>]*>(<br>)*(.*?)<input");

        RequestInfo ri = up.getRequestInfo();
        // <html><head></head><body>CaptchaExchangeServer - v2.06 (29-04-08
        // 02:00) by DVK<br>dvk_ok001; Captcha is loaded with ID(580281), your
        // balance (96), please wait recognition...<br></body></html>
        up.close();
        logger.info(ri.getHtmlCode());
        if (ri.containsHTML("dvk_ok001")) {
            String[] answer = Plugin.getSimpleMatches(ri.getHtmlCode(), "<html><head></head><body>CaptchaExchangeServer - v° (°) by DVK<br>dvk_ok°; Captcha is loaded with ID(°), your balance (°), please wait recognition...<br></body></html>");
            this.cesVersion = answer[0];
            this.cesDate = answer[1];
            this.cesStatus = true;
            this.cesCode = answer[2];
            this.captchaID = answer[3];
            this.userBalance = answer[4];
            return true;
        } else {
            this.cesStatus = false;
            String[] answer = Plugin.getSimpleMatches(ri.getHtmlCode(), "<html><head></head><body>CaptchaExchangeServer - v2.06 (29-04-08 02:00) by DVK<br>dvk_°;°<br></body></html>");
            this.cesVersion = answer[0];
            this.cesDate = answer[1];

            this.cesCode = answer[2];
            this.statusText = answer[3];

            return false;
        }
    }

    public String getStatus() {

        return (cesStatus ? "SUCCESS" : "ERROR") + ": " + "[" + cesCode + "]" + this.cesVersion + " (" + cesDate + ")" + statusText + " CaptchaID:" + captchaID;
    }

    private String getKey() {
        return JDUtilities.getMD5(key + md5);

    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

}