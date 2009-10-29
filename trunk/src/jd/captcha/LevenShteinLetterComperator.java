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

package jd.captcha;
import java.util.List;
import jd.captcha.pixelgrid.Letter;

public class LevenShteinLetterComperator {
    private boolean[][][][] letterDB;
    private JAntiCaptcha jac;
    public boolean onlySameWidth = false;
    public int costs = 6;
    public boolean detectVerticalOffset = false;
    public boolean detectHorizonalOffset = false;

    public void run(Letter letter) {
        if (letterDB.length == 0 || letter.getWidth() == 0 || letter.getHeight() == 0) return;
        boolean[][][] b = getBooleanArrays(letter);

        // dimension/=b[0].length;
        // System.out.println(this.costs+":"+dimension);

        int best = 0;
        int bestdist = Integer.MAX_VALUE;
        int[] bestOffset = null;
        for (int i = 0; i < letterDB.length; i++) {
            if (onlySameWidth && jac.letterDB.get(i).getWidth() != letter.getWidth()) continue;
            int[] dist = getLevenshteinDistance(b, letterDB[i], bestdist);
            if (dist != null && bestdist > dist[0]) {
                bestOffset = dist;
                bestdist = dist[0];
                best = i;
            }
        }
        if (bestOffset == null) return;
        Letter bestLetter = jac.letterDB.get(best);
        // LetterComperator r = new
        // LetterComperator(letter,bestBiggest.detected.getB() );

        letter.detected = new LetterComperator(letter, bestLetter);
        letter.detected.setOffset(new int[] { bestOffset[1], bestOffset[2] });

        // 75 weil zeilen und reihen gescannt werden
        letter.detected.setValityPercent(((double) 75 * bestdist / costs) / ((double) letter.getArea()));

        letter.setDecodedValue(bestLetter.getDecodedValue());
    }

    public void run(final Letter[] letters) {
        Thread[] ths = new Thread[letters.length];
        final LevenShteinLetterComperator lv = this;
        for (int i = 0; i < ths.length; i++) {
            final int j = i;
            ths[i] = new Thread(new Runnable() {
                public void run() {
                    lv.run(letters[j]);
                    synchronized (this) {
                        notify();
                    }
                }

            });
            ths[i].start();

        }
        for (Thread thread : ths) {
            while (thread.isAlive()) {
                synchronized (thread) {
                    try {
                        thread.wait();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void run(List<Letter> letters) {
        run(letters.toArray(new Letter[] {}));
    }

    public LevenShteinLetterComperator(JAntiCaptcha jac) {
        letterDB = new boolean[jac.letterDB.size()][][][];
        this.jac = jac;
        for (int i = 0; i < letterDB.length; i++) {
            letterDB[i] = getBooleanArrays(jac.letterDB.get(i));
        }
    }

    private boolean[][][] getBooleanArrays(Letter letter) {
        int w = letter.getWidth(), h = letter.getHeight();
        if (w == 0 || h == 0) return null;
        boolean[][] leth1 = new boolean[w][h];
        int avg = (int) (letter.getAverage() * letter.owner.getJas().getDouble("RelativeContrast"));
        for (int x = 0; x < leth1.length; x++) {
            for (int y = 0; y < leth1[0].length; y++) {
                leth1[x][y] = letter.grid[x][y] < avg;
            }
        }
        boolean[][] leth12 = new boolean[h][w];
        for (int y = 0; y < leth1[0].length; y++) {
            for (int x = 0; x < leth1.length; x++) {
                leth12[y][x] = leth1[x][y];
            }
        }
        return new boolean[][][] { leth1, leth12 };
    }

    public double getLevenshteinDistance(Letter a, Letter b) {
        boolean[][][] ba = getBooleanArrays(a);
        boolean[][][] bb = getBooleanArrays(b);
        int[] d = getLevenshteinDistance(ba, bb, Integer.MAX_VALUE);
        if (d != null) {
            a.detected = new LetterComperator(a, b);
            a.detected.setOffset(new int[] { d[1], d[2] });
            // 75 weil zeilen und reihen gescannt werden
            double ret = (double) (((double) 75 * d[0] / costs) / ((double) a.getArea()));
            a.detected.setValityPercent(ret);
            return ret;
        }
        return Double.MAX_VALUE;
    }

    private int getBounds(boolean[][] lengthLong, boolean[][] lengthShort, boolean detectOffset) {
        int d = lengthLong.length - lengthShort.length;

        if (!detectOffset) {
            return d / 2;
        } else {
            int bestDist = Integer.MAX_VALUE;
            int besti = 0;
            int lsm1 = lengthShort.length - 1;
            for (int i = 0; i < d; i++) {
                int dist = getLevenshteinDistance(lengthLong[i], lengthShort[0]);
                dist += getLevenshteinDistance(lengthLong[i + lsm1], lengthShort[lsm1]);

                if (dist < bestDist) {
                    bestDist = dist;
                    besti = i;
                }

            }
            return besti;

        }

    }

    private int getBoundDiff(boolean[][] bba1, int start, int end) {
        int res = 0;
        for (int i = 0; i < start; i++) {
            for (int c = 0; c < bba1[i].length; c++) {
                if (bba1[i][c]) res++;
            }
        }
        for (int i = end; i < bba1.length; i++) {
            for (int c = 0; c < bba1[i].length; c++) {
                if (bba1[i][c]) res++;
            }
        }
        return res;
    }

    private int[] getLevenshteinDistance(boolean[][][] ba, boolean[][][] bb, int best) {
        int res = 0;
        if (ba == null || bb == null) return null;
        int bounds1 = 0;
        int diff1 = ba[0].length - bb[0].length;
        boolean swV = false;
        boolean swH = false;

        if (diff1 > 0) {
            bounds1 = getBounds(ba[0], bb[0], detectVerticalOffset);
        } else if (diff1 < 0) {
            boolean[][] bac = bb[0];
            bb[0] = ba[0];
            ba[0] = bac;
            swV = true;
            bounds1 = getBounds(ba[0], bb[0], detectVerticalOffset);
        } else
            bounds1 = 0;
        res += getBoundDiff(ba[0], bounds1, bb[0].length) * costs;
        if (best < res) return null;

        int bounds2 = 0;
        int diff2 = ba[1].length - bb[1].length;

        if (diff2 > 0) {
            bounds2 = getBounds(ba[1], bb[1], detectHorizonalOffset);
        } else if (diff2 < 0) {
            boolean[][] bac = bb[1];
            bb[1] = ba[1];
            ba[1] = bac;
            swH = true;
            bounds2 = getBounds(ba[1], bb[1], detectHorizonalOffset);
        } else
            bounds2 = 0;
        res += getBoundDiff(ba[1], bounds2, bb[1].length) * costs;

        if (best < res) return null;

        // res += (((Math.abs(ba[0].length - bb[0].length) *
        // Math.max(ba[1].length,
        // bb[1].length))+(Math.abs(ba[1].length - bb[1].length) *
        // Math.max(ba[0].length, bb[0].length)))/dimension);

        // System.out.println(ba[0].length+":"+bb[0].length+":"+bounds1[1]+":"+bounds1[0]);
        for (int c = 0; c < bb[0].length; c++) {
            // System.out.println(c-bounds1[0]);
            res += getLevenshteinDistance(ba[0][c + bounds1], bb[0][c]);
            if (best < res) return null;
        }
        for (int c = 0; c < bb[1].length; c++) {
            res += getLevenshteinDistance(ba[1][c + bounds2], bb[1][c]);
            if (best < res) return null;
        }
        return new int[] { res, swV ? -bounds1 : bounds1, swH ? -bounds2 : bounds2 };
    }

    private int getLevenshteinDistance(boolean[] l1, boolean[] l2) {
        if (l1 == null || l2 == null) { throw new IllegalArgumentException("Letter must not be null"); }

        int n = l1.length;
        int m = l2.length;

        if (n == 0) {
            return m;
        } else if (m == 0) { return n; }
        int p[], d[], c[];
        {
            int n1 = n + 1;
            p = new int[n1]; // 'previous' cost array, horizontally
            d = new int[n1]; // cost array, horizontally
            c = new int[n1]; // 'previous previous' cost array, horizontally
        }
        // indexes into strings s and t
        int i; // iterates through s
        int j; // iterates through t
        int j1, j2, i1, i2;
        boolean t_j;

        int cost = 0; // cost

        for (i = 1; i <= n; i++) {
            p[i] = i;
        }
        c=p;
        for (j = 1; j <= m; j++) {
            j1 = j;
            j2 = --j1;
            j2--;
            t_j = l2[j1];

            d[0] = j;

            for (i = 1; i <= n; i++) {
                i1 = i-1;
                cost = (l1[i1] == t_j) ? 1 : 0;
                // minimum of cell to the left+1, to the top+1, diagonally left
                // and up +cost
                d[i] = Math.min(d[i1] + costs, Math.min(p[i] + costs, p[i1] + cost * costs));
                // Damerau
                if ((i > 1) && (j > 1) && (l1[i1] == l2[j2]) && (l1[i2 = i1 - 1] == l2[j1])) {
                    d[i] = Math.min(d[i], c[i2] + cost);
                }
            }
            // previous of previous for Damerau
            for (i = 1; i <= n; i++) {
                c[i] = p[i];
            }
            // copy current distance counts to 'previous row' distance counts
            int[] _d = p;
            p = d;
            d = _d;
        }

        // our last action in the above loop was to switch d and p, so p now
        // actually has the most recent cost counts
        return p[n];

    }

}
