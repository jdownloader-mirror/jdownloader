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

package jd.nutils.httpserver;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;

import jd.nutils.Executer;

public class Response {
    private StringBuilder data = new StringBuilder();

    private HashMap<String, String> headers = new HashMap<String, String>();

    private String returnStatus = "200 OK";

    private String returnType = "text/html";

    public final static String OK = "200 OK";

    public final static String ERROR = "404 ERROR";

    public Response() {
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public void setReturnStatus(String returnStatus) {
        this.returnStatus = returnStatus;
    }

    public void addContent(Object content) {
        data.append(content.toString());
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public void writeToStream(OutputStream out) throws IOException {
        StringBuilder help = new StringBuilder();
        help.append("HTTP/1.1 " + returnStatus + "\r\n");
        help.append("Connection: close\r\n");
        help.append("Server: jDownloader HTTP Server\r\n");
        help.append("Content-Type: " + returnType + "\r\n");
        help.append("Content-Length: " + data.toString().getBytes(Executer.CODEPAGE).length + "\r\n");

        Iterator<String> it = headers.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            help.append(key + ": " + headers.get(it) + "\r\n");
        }

        help.append("\r\n");

        out.write(help.toString().getBytes(Executer.CODEPAGE));
        out.write(data.toString().getBytes(Executer.CODEPAGE));
    }
}
