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

package tests.singletests;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import jd.controlling.DistributeData;
import jd.controlling.SingleDownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.HosterInfo;
import jd.plugins.DownloadLink.AvailableStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tests.utils.TestUtils;

public class SingleHoster {
    private HashMap<String, String> links;

    /**
     * change this to the host that is tested, test links are taken from
     * http://jdownloader
     * .net:8081/knowledge/wiki/development/intern/testlinks/hoster/HOST
     */
    private static final String HOST = "badongo.com";

    @Before
    public void setUp() {
        TestUtils.mainInit();
        TestUtils.initGUI();
        TestUtils.initDecrypter();
        TestUtils.initContainer();
        TestUtils.initHosts();
        TestUtils.finishInit();

        links = TestUtils.getHosterLinks(HOST);
        TestUtils.log("Found " + links.size() + " test link(s) for host " + HOST);
    }

    @Test
    public void findDownloadLink() {
        try {
            for (Entry<String, String> next : links.entrySet()) {
                getDownloadLink(next.getValue());
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    @Test
    public void availableCheck() {

        for (Entry<String, String> next : links.entrySet()) {
            try {
                DownloadLink dlink = getDownloadLink(next.getValue());
                TestUtils.log("Testing link: " + next.getValue());
                if (next.getKey().equalsIgnoreCase(TestUtils.HOSTER_LINKTYPE_NORMAL + 1)) {
                    if (dlink.getAvailableStatus() != AvailableStatus.TRUE) {
                        fail(TestUtils.log(next.getKey() + " Downloadlink " + next.getValue() + " is marked as NOT AVAILABLE"));
                    }
                } else if (next.getKey().equalsIgnoreCase(TestUtils.HOSTER_LINKTYPE_FNF + 1)) {
                    if (dlink.getAvailableStatus() != AvailableStatus.FALSE) {
                        fail(TestUtils.log(next.getKey() + " Downloadlink " + next.getValue() + " is marked as " + dlink.getAvailableStatus()));
                    }
                } else if (next.getKey().equalsIgnoreCase(TestUtils.HOSTER_LINKTYPE_ABUSED + 1)) {
                    if (dlink.getAvailableStatus() != AvailableStatus.FALSE) {
                        fail(TestUtils.log(next.getKey() + " Downloadlink " + next.getValue() + " is marked as " + dlink.getAvailableStatus()));
                    }
                } else if (next.getKey().equalsIgnoreCase(TestUtils.HOSTER_LINKTYPE_ERROR_TEMP + 1)) {
                    if (dlink.getAvailableStatus() != AvailableStatus.TRUE) {
                        fail(TestUtils.log(next.getKey() + " Downloadlink " + next.getValue() + " is marked as " + dlink.getAvailableStatus()));
                    }
                }
            } catch (TestException e) {
                fail(e.getMessage());
            }
        }

    }

    @Test
    public void downloadFree() {
        for (Entry<String, String> next : links.entrySet()) {
            if (next.getKey().equalsIgnoreCase(TestUtils.HOSTER_LINKTYPE_NORMAL + 1)) {
                download(next.getValue());
            }
        }
    }

    private void download(String url) {

        try {
            DownloadLink dlink = getDownloadLink(url);
            if (dlink.getAvailableStatus() != AvailableStatus.TRUE) {
                fail(TestUtils.log("Downloadlink " + url + " is marked as NOT AVAILABLE"));
            }
            SingleDownloadController download = new SingleDownloadController(dlink);

            dlink.getLinkStatus().setActive(true);
            if (new File(dlink.getFileOutput()).delete()) {
                TestUtils.log("Removed local testfile: " + dlink.getFileOutput());
            }

            download.start();
            long waittimeuntilstart = 60 * 3 * 1000;
            HosterInfo pluginInfo = dlink.getPlugin().getHosterInfo();
            if (pluginInfo != null && pluginInfo.getFreeMaxWaittime() > 0) {
                waittimeuntilstart = pluginInfo.getFreeMaxWaittime() + 5000;
            }

            long start = System.currentTimeMillis();
            while (true) {

                Thread.sleep(1000);
                System.out.println(dlink.getLinkStatus().getStatusString());

                if (dlink.getDownloadCurrent() > 0 && !dlink.getLinkStatus().getStatusString().startsWith("Connecting")) {
                    TestUtils.log("Download started correct");
                    download.abortDownload();
                    return;
                }
                if (start + waittimeuntilstart < System.currentTimeMillis()) {

                    if (dlink.getDownloadCurrent() <= 0) {
                        fail(TestUtils.log("Download did not start correctly"));
                        download.abortDownload();
                        return;
                    } else {
                        TestUtils.log("Download started correct");
                        download.abortDownload();
                        return;
                    }
                }

            }

        } catch (TestException e) {
            fail(e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private DownloadLink getDownloadLink(String url) throws TestException {

        DistributeData distributeData = new DistributeData(url);
        ArrayList<DownloadLink> links = distributeData.findLinks();
        if (links == null || links.size() == 0) {
            throw new TestException("No plugin found for " + url);

        } else {
            if (!url.toLowerCase().contains(links.get(0).getPlugin().getHost())) {
                throw new TestException("Wrong plugin found for " + url + " (" + links.get(0).getPlugin().getHost() + ")");
            } else {
                return links.get(0);
            }
        }

    }

    @After
    public void tearDown() throws Exception {
        // JDUtilities.getController().exit();
    }
}