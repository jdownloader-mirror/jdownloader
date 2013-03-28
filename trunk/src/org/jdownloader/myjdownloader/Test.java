package org.jdownloader.myjdownloader;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.jackson.JacksonMapper;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.LoginDialog;
import org.appwork.utils.swing.dialog.LoginDialog.LoginData;
import org.jdownloader.api.captcha.CaptchaAPI;
import org.jdownloader.api.captcha.CaptchaJob;
import org.jdownloader.api.jd.JDAPI;
import org.jdownloader.myjdownloader.client.AbstractMyJDClient;
import org.jdownloader.myjdownloader.client.exceptions.APIException;
import org.jdownloader.myjdownloader.client.exceptions.InvalidResponseCodeException;
import org.jdownloader.myjdownloader.client.exceptions.MyJDownloaderException;
import org.jdownloader.myjdownloader.client.exceptions.MyJDownloaderUnexpectedIOException;

public class Test {
    /**
     * @param args
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws APIException
     * @throws MyJDownloaderException
     * @throws DialogCanceledException
     * @throws DialogClosedException
     */
    public static void main(String[] args) throws APIException, MyJDownloaderException, DialogClosedException, DialogCanceledException {
        final Browser br = new Browser();
        br.setAllowedResponseCodes(200);
        JSonStorage.setMapper(new JacksonMapper());
        // br.forceDebug(true);
        // Log.L.setLevel(Level.ALL);
        AbstractMyJDClient api = new AbstractMyJDClient() {

            @Override
            protected String post(String query, String object) throws MyJDownloaderException {
                try {

                    String ret = br.postPageRaw(getServerRoot() + query, object == null ? "" : object);

                    URLConnectionAdapter con = br.getRequest().getHttpConnection();

                    if (con != null && con.getResponseCode() > 0 && con.getResponseCode() != 200) {

                    throw new InvalidResponseCodeException(con.getResponseCode());

                    }
                    System.out.println(con);
                    System.out.println(ret);
                    return ret;
                } catch (BrowserException e) {

                    if (e.getConnection() != null && e.getConnection().getResponseCode() > 0 && e.getConnection().getResponseCode() != 200) {

                    throw new InvalidResponseCodeException(e.getConnection().getResponseCode());

                    }

                    throw new MyJDownloaderUnexpectedIOException(e);
                } catch (IOException e) {

                    throw new MyJDownloaderUnexpectedIOException(e);
                } finally {
                    URLConnectionAdapter con = br.getRequest().getHttpConnection();
                    System.out.println(con);
                }

            }

            @Override
            protected byte[] base64decode(String base64encodedString) {
                return Base64.decode(base64encodedString);
            }

            @Override
            protected String base64Encode(byte[] encryptedBytes) {
                return Base64.encodeToString(encryptedBytes, false);
            }

            @Override
            protected String objectToJSon(Object payload) {
                return JSonStorage.toString(payload);
            }

            @Override
            protected <T> T jsonToObject(String dec, Type clazz) {
                return (T) JSonStorage.restoreFromString(dec, new TypeRef<T>(clazz) {
                });
            }

        };
        LoginData li = Dialog.getInstance().showDialog(new LoginDialog(0));
        api.connect(li.getUsername(), li.getPassword());
        JDAPI jda;

        // List<FilePackageAPIStorable> ret = api.link(DownloadsAPI.class, "downloads").queryPackages(new APIQuery());
        List<CaptchaJob> list = api.link(CaptchaAPI.class, "captcha").list();
        System.out.println(list);
        // System.out.println(ret);
        api.disconnect();
        // System.out.println(jdapi.uptime());
    }
}
