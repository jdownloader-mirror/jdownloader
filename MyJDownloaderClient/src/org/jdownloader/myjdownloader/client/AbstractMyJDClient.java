package org.jdownloader.myjdownloader.client;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jdownloader.myjdownloader.client.exceptions.APIException;
import org.jdownloader.myjdownloader.client.exceptions.AuthException;
import org.jdownloader.myjdownloader.client.exceptions.ChallengeFailedException;
import org.jdownloader.myjdownloader.client.exceptions.EmailInvalidException;
import org.jdownloader.myjdownloader.client.exceptions.EmailNotAllowedException;
import org.jdownloader.myjdownloader.client.exceptions.EmailNotValidatedException;
import org.jdownloader.myjdownloader.client.exceptions.ExceptionResponse;
import org.jdownloader.myjdownloader.client.exceptions.MyJDownloaderException;
import org.jdownloader.myjdownloader.client.exceptions.OverloadException;
import org.jdownloader.myjdownloader.client.exceptions.TokenException;
import org.jdownloader.myjdownloader.client.exceptions.TooManyRequestsException;
import org.jdownloader.myjdownloader.client.json.CaptchaChallenge;
import org.jdownloader.myjdownloader.client.json.ConnectResponse;
import org.jdownloader.myjdownloader.client.json.ErrorResponse;
import org.jdownloader.myjdownloader.client.json.JSonRequest;
import org.jdownloader.myjdownloader.client.json.ObjectData;
import org.jdownloader.myjdownloader.client.json.RegisterPayload;
import org.jdownloader.myjdownloader.client.json.RegisterResponse;
import org.jdownloader.myjdownloader.client.json.RequestIDOnly;
import org.jdownloader.myjdownloader.client.json.RequestIDValidator;

public abstract class AbstractMyJDClient {

    public static String byteArrayToHex(final byte[] digest) {
        final StringBuilder ret = new StringBuilder();
        String tmp;
        for (final byte d : digest) {
            tmp = Integer.toHexString(d & 0xFF);
            if (tmp.length() < 2) {
                ret.append('0');
            }
            ret.append(tmp);
        }
        return ret.toString();
    }

    public static byte[] hexToByteArray(final String s) {
        final int len = s.length();
        final byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static byte[] hmac(final byte[] key, final byte[] content) throws NoSuchAlgorithmException, InvalidKeyException {
        final Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        final SecretKeySpec secret_key = new SecretKeySpec(key, "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return sha256_HMAC.doFinal(content);
    }

    private String serverRoot = "http://api.jdownloader.org";
    private String email;

    private long   counter;

    private byte[] serverEncryptionToken;
    private byte[] deviceSecret;
    private byte[] deviceEncryptionToken;
    private String sessionToken;
    private String regainToken;

    public AbstractMyJDClient() {

        counter = System.currentTimeMillis();

    }

    protected abstract byte[] base64decode(String base64encodedString);

    protected abstract String base64Encode(byte[] encryptedBytes);

    @SuppressWarnings("unchecked")
    public <T> T callAction(final String action, final Class<T> returnType, final Object... args) throws MyJDownloaderException, APIException {
        return (T) callActionInternal(action, returnType, args);

    }

    protected Object callActionInternal(final String action, final Type returnType, final Object... args) throws MyJDownloaderException, APIException {
        try {
            final String query = "/t_" + sessionToken + action;
            final String[] params = new String[args != null ? args.length : 0];
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    params[i] = objectToJSon(args[i]);
                }
            }
            final JSonRequest payload = new JSonRequest();
            payload.setUrl(action);
            payload.setRid(inc());
            payload.setParams(params);
            final String json = objectToJSon(payload);
            final String ret = internalPost(query, encrypt(json, deviceEncryptionToken));
            final String dec = decrypt(ret, deviceEncryptionToken);

            final ObjectData data = this.jsonToObject(dec, ObjectData.class);

            // ugly!!! but this will be changed when we have a proper remoteAPI response format

            return this.jsonToObject(objectToJSon(data.getData()) + "", returnType);

        } catch (final ExceptionResponse e) {
            handleInvalidResponseCodes(e);
            throw e;
        } catch (final MyJDownloaderException e) {
            throw e;
        } catch (final Exception e) {
            throw APIException.get(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> T callServer(String query, final String postData, final Class<T> class1) throws MyJDownloaderException {
        try {
            query += query.contains("?") ? "&" : "?";
            final long i = inc();
            query += "rid=" + i;
            final String encrypted = internalPost(query + "&signature=" + sign(serverEncryptionToken, query), postData);
            final Object ret = this.jsonToObject(decrypt(encrypted, serverEncryptionToken), class1);
            System.out.println(objectToJSon(ret));
            if (ret instanceof RequestIDValidator) {
                if (((RequestIDValidator) ret).getRid() != i) { throw new BadResponseException("RID Mismatch"); }
            }
            return (T) ret;
        } catch (final ExceptionResponse e) {
            handleInvalidResponseCodes(e);
            throw e;
        } catch (final Exception e) {
            throw MyJDownloaderException.get(e);
        }
    }

    public void connect(final String email, final String password) throws MyJDownloaderException {
        try {
            this.email = email;

            // localSecret = createSecret(username, password, "jd");
            final byte[] loginSecret = createSecret(email, password, "server");
            deviceSecret = createSecret(email, password, "device");
            final long rid = inc();
            final StringBuilder query = new StringBuilder().append("/my/clientconnect?email=").append(email).append("&rid").append(rid);

            final String signature = sign(loginSecret, query.toString());
            query.append("&signature=").append(signature);

            final String encrypted = internalPost(query.toString(), "");
            final ConnectResponse ret = this.jsonToObject(decrypt(encrypted, loginSecret), ConnectResponse.class);
            if (ret.getRid() != rid) { throw new BadResponseException("RID Mismatch"); }

            serverEncryptionToken = updateEncryptionToken(loginSecret, hexToByteArray(ret.getSessiontoken()));
            deviceEncryptionToken = updateEncryptionToken(deviceSecret, hexToByteArray(ret.getSessiontoken()));
            sessionToken = ret.getSessiontoken();
            regainToken = ret.getRegaintoken();
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);

        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);

        } catch (final InvalidKeyException e) {
            throw new RuntimeException(e);

        } catch (final NoSuchPaddingException e) {

            throw new BadResponseException("Response Decryption Failed", e);

        } catch (final InvalidAlgorithmParameterException e) {

            throw new BadResponseException("Response Decryption Failed", e);

        } catch (final IllegalBlockSizeException e) {
            throw new BadResponseException("Response Decryption Failed", e);

        } catch (final BadPaddingException e) {
            throw new BadResponseException("Response Decryption Failed", e);

        }

    }

    private byte[] updateEncryptionToken(final byte[] oldSecret, final byte[] update) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(oldSecret);
        md.update(update);
        return md.digest();
    }

    private byte[] createSecret(final String username, final String password, final String domain) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        final MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest((username + password + domain).getBytes("UTF-8"));
    }

    private String decrypt(final String encrypted, final byte[] keyAndIV) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException {
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        final IvParameterSpec ivSpec = new IvParameterSpec(Arrays.copyOfRange(keyAndIV, 0, 16));
        final SecretKeySpec skeySpec = new SecretKeySpec(Arrays.copyOfRange(keyAndIV, 16, 32), "AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
        final byte[] crypted = base64decode(encrypted);
        final byte[] decryptedBytes = cipher.doFinal(crypted);
        return new String(decryptedBytes, "UTF-8");
    }

    // @SuppressWarnings("unchecked")
    // private <T> T jsonToObjectGeneric(String dec, Class<T> clazz) {
    // return (T) jsonToObject(dec, clazz);
    // }

    public void disconnect() throws MyJDownloaderException {

        final String query = "/my/disconnect?clienttoken=" + sessionToken;

        this.callServer(query, null, ConnectResponse.class);

    }

    protected String encrypt(final String createPayloadString, final byte[] keyAndIV) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        final IvParameterSpec ivSpec = new IvParameterSpec(Arrays.copyOfRange(keyAndIV, 0, 16));
        final SecretKeySpec skeySpec = new SecretKeySpec(Arrays.copyOfRange(keyAndIV, 16, 32), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);

        final byte[] encryptedBytes = cipher.doFinal(createPayloadString.getBytes("UTF-8"));
        return base64Encode(encryptedBytes);
    }

    /**
     * Downloads a CaptchaChallenge from the server
     * 
     * @return
     * @throws MyJDownloaderException
     */
    public CaptchaChallenge getChallenge() throws MyJDownloaderException {
        return this.jsonToObject(internalPost("/captcha/getCaptcha", ""), CaptchaChallenge.class);
    }

    public String getServerRoot() {
        return serverRoot;
    }

    protected void handleInvalidResponseCodes(final ExceptionResponse e) throws MyJDownloaderException {
        if (e != null && e.getContent() != null && e.getContent().trim().length() != 0) {
            final ErrorResponse error = this.jsonToObject(e.getContent(), ErrorResponse.class);
            try {
                switch (error.getSrc()) {

                case DEVICE:

                    break;

                case MYJD:
                    switch (error.getType()) {
                    case AUTH_FAILED:
                        throw new AuthException();
                    case ERROR_EMAIL_NOT_CONFIRMED:
                        throw new EmailNotValidatedException();
                    case OFFLINE:
                        throw new RuntimeException("Not Implemented: offline");
                    case TOKEN_INVALID:
                        throw new TokenException();
                    case UNKNOWN:
                        throw new RuntimeException("Not Implemented: unkown");
                    case CHALLENGE_FAILED:
                        throw new ChallengeFailedException();
                    case EMAIL_FORBIDDEN:
                        throw new EmailNotAllowedException();
                    case EMAIL_INVALID:
                        throw new EmailInvalidException();
                    case OVERLOAD:
                        throw new OverloadException();
                    case TOO_MANY_REQUESTS:
                        throw new TooManyRequestsException();
                    }
                    break;

                }
            } catch (final MyJDownloaderException e1) {
                e1.setSource(error.getSrc());
                throw e1;
            }
        }

        switch (e.getResponseCode()) {
        case 403:
            throw new AuthException();
        case 503:
            throw new OverloadException();
        case 401:
            throw new EmailNotValidatedException();
        case 407:
            throw new TokenException();

        }
    }

    private long inc() {
        return counter++;
    }

    protected abstract <T> T jsonToObject(String dec, Type clazz);

    @SuppressWarnings("unchecked")
    public <T> T link(final Class<T> class1, final String namespace) {

        return (T) Proxy.newProxyInstance(class1.getClassLoader(), new Class<?>[] { class1 }, new InvocationHandler() {

            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                try {
                    final String action = "/" + namespace + "/" + method.getName();
                    final Type returnType = method.getGenericReturnType();
                    return AbstractMyJDClient.this.callActionInternal(action, returnType, args);

                } catch (final Throwable e) {
                    throw new RuntimeException(e);
                }

            }

        });
    }

    protected abstract String objectToJSon(Object payload);

    private String internalPost(final String url, final String objectToJSon) throws MyJDownloaderException {
        try {
            return post(url, objectToJSon);
        } catch (final ExceptionResponse e) {
            handleInvalidResponseCodes(e);
            throw e;
        }

    }

    abstract protected String post(String query, String object) throws ExceptionResponse;

    /**
     * register for a new MyJDownloader Account. If there is a registration problem, this method throws an MyJDownloaderException
     * 
     * @see #getChallenge()
     * @see #requestConfirmationEmail(CaptchaChallenge);
     * @param email
     * @param pass
     * @param challenge
     * @throws APIException
     * @throws MyJDownloaderException
     */
    public void register(final CaptchaChallenge challenge) throws MyJDownloaderException {

        final String encrypted = jsonPost("/my/register", new RegisterPayload(email, AbstractMyJDClient.byteArrayToHex(serverEncryptionToken), challenge.getCaptchaChallenge(), challenge.getCaptchaResponse()));

        final RegisterResponse ret = this.jsonToObject(encrypted, RegisterResponse.class);

    }

    private String jsonPost(final String path, final Object... params) throws MyJDownloaderException {
        final JSonRequest re = new JSonRequest();
        re.setRid(inc());
        re.setParams(params);
        re.setUrl(path);
        return internalPost(path, objectToJSon(re));
    }

    public void confirmEmail(final String key) throws MyJDownloaderException {
        final RequestIDOnly response = this.callServer("/my/confirmemail?email=" + email + "&key=" + key.trim(), null, RequestIDOnly.class);
        System.out.println(response);
    }

    public void changePassword(final String newPassword, final String key) throws MyJDownloaderException {
        byte[] newserverEncryptionToken;
        try {
            newserverEncryptionToken = createSecret(email, newPassword, "server");
        } catch (final Exception e) {
            throw new RuntimeException(e);

        } 
        final RequestIDOnly response = this.callServer("/my/changepassword?email=" + email + "&secretServer=" + byteArrayToHex(newserverEncryptionToken) + "&key=" + key, null, RequestIDOnly.class);
        connect(email, newPassword);
        System.out.println(response);

    }

    public void requestConfirmationEmail() throws MyJDownloaderException {

        final RequestIDOnly response = this.callServer("/my/requestemailconfirmation?email=" + email, null, RequestIDOnly.class);
        System.out.println(response);

    }

    public void requestPasswordChangeEmail() throws MyJDownloaderException {

        final RequestIDOnly response = this.callServer("/my/requestpasswordchangeemail?email=" + email, null, RequestIDOnly.class);
        System.out.println(response);

    }

    public void setServerRoot(final String serverRoot) {
        this.serverRoot = serverRoot;
    }

    private String sign(final byte[] key, final String data) throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
        return AbstractMyJDClient.byteArrayToHex(AbstractMyJDClient.hmac(key, data.getBytes("UTF-8")));
    }

}
