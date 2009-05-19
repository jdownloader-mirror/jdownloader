//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.http.download;

import java.io.Serializable;

public class ChunkProgress implements Serializable {

    public long getEnd() {
        return end;
    }

    public long getStart() {
        return start;
    }

    /**
     * 
     */
    private static final long serialVersionUID = 9203094151658724279L;
    private long end = 0;
    private long start = 0;

    public void setStart(long start) {
        this.start = start;

    }

    public void setEnd(long end) {
        this.end = end;

    }

    public String toString() {
        return "Chunk " + start + " - " + end;

    }
}
