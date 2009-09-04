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

package jd.controlling;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import jd.nutils.Formatter;

/**
 * Mit dieser Klasse können die Logmeldungen anders dargestellt werden. Der Code
 * wurde hauptsächlich aus SimpleFormatter übernommen. Lediglich die Format
 * Methode wurde ein wenig umgestellt.
 * 
 * 
 */
public class LogFormatter extends SimpleFormatter {
    // private final static String format = "{0,date} {0,time}";
    private Object args[] = new Object[1];
    Date dat = new Date();
    // private MessageFormat formatter;
    // Line separator string. This is the value of the line.separator
    // property at the moment that the SimpleFormatter was created.
    private String lineSeparator = System.getProperty("line.separator");
    private StringBuilder sb;

    public synchronized String format(LogRecord record) {

        sb = new StringBuilder();

        // Minimize memory allocations here.
        dat.setTime(record.getMillis());
        args[0] = dat;

        // text = new StringBuffer();
        // if (formatter == null) {
        // formatter = new MessageFormat(format);
        // }
        // formatter.format(args, text, null);
        String message = formatMessage(record);
        // sb.append(text);
        // sb.append(" - ");
        if (JDLogger.getLogger().getLevel() == Level.ALL) {
            sb.append(Formatter.formatMilliseconds(record.getMillis()));
            sb.append(" - ");
            sb.append(record.getLevel().getName());
            sb.append(" [");
            if (record.getSourceClassName() != null) {
                sb.append(record.getSourceClassName());
            } else {
                sb.append(record.getLoggerName());
            }
            if (record.getSourceMethodName() != null) {
                sb.append("(");
                sb.append(record.getSourceMethodName());
                sb.append(")");
            }
            sb.append("] ");

            sb.append("-> ");
        } else {

            if (record.getSourceClassName() != null) {
                sb.append(record.getSourceClassName().substring(record.getSourceClassName().lastIndexOf(".") + 1));
            } else {
                sb.append(record.getLoggerName());
            }

            sb.append("-> ");
        }
        sb.append(message);
        sb.append(lineSeparator);
        if (record.getThrown() != null) {
            sb.append(JDLogger.getStackTrace(record.getThrown()));
        }
        return sb.toString();
    }

}
