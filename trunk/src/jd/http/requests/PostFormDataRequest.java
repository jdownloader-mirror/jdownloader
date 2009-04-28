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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;

import jd.http.Browser;
import jd.http.URLConnectionAdapter;

/**
 * Extending the Request calss, this class is able to to HTML Formdata Posts.
 * 
 * @author coalado
 * 
 */
public class PostFormDataRequest extends Request {

    private String boundary;
    private ArrayList<FormData> formDatas;
    private OutputStream output;
    private String encodeType = "multipart/form-data";

    public String getEncodeType() {
        return encodeType;
    }

    public void setEncodeType(String encodeType) {
        this.encodeType = encodeType;
    }

    public PostFormDataRequest(String url) throws MalformedURLException {
        super(url);
        generateBoundary();
        this.formDatas = new ArrayList<FormData>();

    }

    //@Override
    public void postRequest(URLConnectionAdapter httpConnection) throws IOException {
        httpConnection.setDoOutput(true);

        output = httpConnection.getOutputStream();

        for (int i = 0; i < this.formDatas.size(); i++) {
            write(formDatas.get(i));

        }
        OutputStreamWriter writer = new OutputStreamWriter(output);
        writer.write(this.boundary);
        writer.write("--\r\n");
        writer.flush();

        if (output != null) {
            output.flush();
            output.close();
        }

    }

    private void write(FormData formData) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(output);
        writer.write(this.boundary);
        writer.write("\r\n");
        BufferedOutputStream outputByteWriter;
        switch (formData.getType()) {
        case VARIABLE:
            writer.write("Content-Disposition: form-data; name=\"" + formData.getName() + "\"");
            writer.write("\r\n\r\n");
            writer.write(formData.getValue() + "\r\n");
            break;
        case DATA:
            writer.write("Content-Disposition: form-data; name=\"" + formData.getName() + "\"; filename=\"" + formData.getValue() + "\"");
            writer.write("\r\nContent-Type: " + formData.getDataType() + "\r\n\r\n");
            writer.flush();
            outputByteWriter = new BufferedOutputStream(output);
            outputByteWriter.write(formData.getData(), 0, formData.getData().length);

            writer.write("\r\n");
            writer.flush();
            outputByteWriter.flush();

            break;
        case FILE:
            writer.write("Content-Disposition: form-data; name=\"" + formData.getName() + "\"; filename=\"" + formData.getValue() + "\"");
            writer.write("\r\nContent-Type: " + formData.getDataType() + "\r\n\r\n");
            writer.flush();
            byte[] b = new byte[1024];
            InputStream in;
            in = new FileInputStream(formData.getFile());
            outputByteWriter = new BufferedOutputStream(output);
            int n;
            while ((n = in.read(b)) > -1) {
                outputByteWriter.write(b, 0, n);
            }
            outputByteWriter.flush();
            in.close();

            writer.write("\r\n");
            writer.flush();

            break;
        }

        writer.flush();

    }

    //@Override
    public void preRequest(URLConnectionAdapter httpConnection) throws IOException {
        httpConnection.setRequestMethod("POST");
        httpConnection.setRequestProperty("Content-Type", encodeType + "; boundary=" + boundary.substring(2));

    }

    private void generateBoundary() {
        long range = (999999999999999l - 100000000000000l);
        long rand = (long) (Math.random() * range) + 100000000000000l;
        boundary = "----------" + rand;

        // boundary="-----------------------------41184676334";

    }

    public static void main(String args[]) throws IOException {
        try {
            Browser br = new Browser();
            br.setDebug(true);
            String[] data = br.getPage("http://rapidshare.com/cgi-bin/upload.cgi?intsysdata=1").split("\\,");
            PostFormDataRequest r = (PostFormDataRequest) br.createPostFormDataRequest("http://rs" + data[0].trim() + "cg.rapidshare.com/cgi-bin/upload.cgi");
            File file = new File("G:\\pluginressourcen\\JDLowSpeed_2009-02-11_v3 (LIGHT).jdu");
            r.addFormData(new FormData("toolmode2", "1"));
            r.addFormData(new FormData("filecontent", file.getName(), file));
            r.addFormData(new FormData("freeaccountid", ""));
            r.addFormData(new FormData("password", ""));

            r.connect();
            System.out.println(r.getHttpConnection());
            String code = r.read();
            System.out.println(code);

        } catch (Exception e) {
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);

        }

    }

    public void addFormData(FormData fd) {
        this.formDatas.add(fd);

    }

    public String getPostDataString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < this.formDatas.size(); i++) {
            write(formDatas.get(i), sb);

        }
        sb.append(this.boundary);
        sb.append("--\r\n");

        return sb + "";

    }

    private void write(FormData formData, StringBuffer sb) {

        switch (formData.getType()) {
        case VARIABLE:
            sb.append("Content-Disposition: form-data; name=\"" + formData.getName() + "\"");
            sb.append("\r\n\r\n");
            sb.append(formData.getValue() + "\r\n");
            break;
        case DATA:
            sb.append("Content-Disposition: form-data; name=\"" + formData.getName() + "\"; filename=\"" + formData.getValue() + "\"");
            sb.append("\r\nContent-Type: " + formData.getDataType() + "\r\n\r\n");

            sb.append("[....." + formData.getData().length + " Byte DATA....]\r\n");

            break;
        case FILE:
            sb.append("Content-Disposition: form-data; name=\"" + formData.getName() + "\"; filename=\"" + formData.getValue() + "\"");
            sb.append("\r\nContent-Type: " + formData.getDataType() + "\r\n\r\n");

            sb.append("[....." + formData.getFile().length() + " FileByte DATA....]");
            sb.append("\r\n");
            break;
        }

    }

}
