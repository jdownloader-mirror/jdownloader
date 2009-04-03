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

package jd.plugins;

import java.util.Date;

import jd.config.Property;
import jd.parser.Regex;

public class AccountInfo extends Property {

    private static final long serialVersionUID = 1825140346023286206L;
    private transient PluginForHost plugin;
    private Account account;
    private boolean valid = true;
    private transient long validUntil = -1;
    private transient long trafficLeft = -1;
    private transient long trafficMax = -1;
    private transient long filesNum = -1;
    private transient long premiumPoints = -1;
    private transient long newPremiumPoints = -1;
    private transient long accountBalance = -1;
    private transient long usedSpace = -1;
    private transient long trafficShareLeft = -1;
    private transient boolean expired = false;
    private transient String status;
    private transient long createTime;

    public AccountInfo(PluginForHost plugin, Account account) {
        this.plugin = plugin;
        this.account = account;
        this.createTime = System.currentTimeMillis();
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public Account getAccount() {
        return account;
    }

    /**
     * Gibt zurück wieviel (in Cent) Geld gerade auf diesem Account ist
     * 
     * @return
     */
    public long getAccountBalance() {
        return accountBalance;
    }

    /**
     * Gibt zurück wieviele Files auf dem Account hochgeladen sind
     * 
     * @return
     */
    public long getFilesNum() {
        return filesNum;
    }

    public long getNewPremiumPoints() {
        return newPremiumPoints;
    }

    public PluginForHost getPlugin() {
        return plugin;
    }

    /**
     * Gibt an wieviele PremiumPunkte der Account hat
     * 
     * @return
     */
    public long getPremiumPoints() {
        return premiumPoints;
    }

    public String getStatus() {
        return status;
    }

    /**
     * Gibt an wieviel Traffic noch frei ist (in bytes)
     * 
     * @return
     */
    public long getTrafficLeft() {
        return trafficLeft;
    }

    public long getTrafficMax() {
        return trafficMax;
    }

    /**
     * Gibt zurück wieviel Trafficshareonch übrig ist (in bytes). Trafficshare
     * ist Traffic, den man über einen PremiumAccount den Freeusern zur
     * Verfügung stellen kann. -1: Feature ist nicht unterstützt
     * 
     * @return
     */
    public long getTrafficShareLeft() {
        return trafficShareLeft;
    }

    /**
     * Gibt zurück wieviel Platz (bytes) die Oploads auf diesem Account belegen
     * 
     * @return
     */
    public long getUsedSpace() {
        return usedSpace;
    }

    /**
     * Gibt einen Timestamp zurück zu dem der Account auslaufen wird bzw.
     * ausgelaufen ist.(-1 für Nie)
     * 
     * @return
     */
    public long getValidUntil() {
        return validUntil;
    }

    /**
     * Gibt zurück ob der Account abgelaufen ist
     * 
     * @return
     */
    public boolean isExpired() {
        return expired || (this.getValidUntil() != -1 && this.getValidUntil() < new Date().getTime());
    }

    /**
     * Gibt zurück ob es sich um einen Gültigen Account handelt, logins richtige
     * etc.
     * 
     * @return
     */
    public boolean isValid() {
        return valid;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public void setAccountBalance(long parseInt) {
        this.accountBalance = parseInt;
    }

    public void setAccountBalance(String string) {
        this.setAccountBalance((long) (Double.parseDouble(string) * 100));
    }

    public void setExpired(boolean b) {
        this.expired = b;
        if (b) this.setTrafficLeft(-1);
    }

    public void setFilesNum(long parseInt) {
        this.filesNum = parseInt;
    }

    public void setNewPremiumPoints(long newPremiumPoints) {
        this.newPremiumPoints = newPremiumPoints;
    }

    public void setPlugin(PluginForHost plugin) {
        this.plugin = plugin;
    }

    public void setPremiumPoints(long parseInt) {
        this.premiumPoints = parseInt;
    }

    public void setPremiumPoints(String string) {
        this.setPremiumPoints(Integer.parseInt(string.trim()));
    }

    public void setStatus(String string) {
        this.status = string;
    }

    public void setTrafficLeft(long size) {
        this.trafficLeft = size;
    }

    public void setTrafficLeft(String freeTraffic) {
        this.setTrafficLeft(Regex.getSize(freeTraffic));
    }

    public void setTrafficMax(long trafficMax) {
        this.trafficMax = trafficMax;
    }

    public void setTrafficShareLeft(long size) {
        this.trafficShareLeft = size;
    }

    public void setUsedSpace(long usedSpace) {
        this.usedSpace = usedSpace;
    }

    public void setUsedSpace(String string) {
        this.setUsedSpace(Regex.getSize(string));
    }

    public void setValid(boolean b) {
        this.valid = b;
    }

    /**
     * -1 für Niemals ablaufen
     * 
     * @param validUntil
     */
    public void setValidUntil(long validUntil) {
        this.validUntil = validUntil;
        if (validUntil != -1 && validUntil < new Date().getTime()) this.setExpired(true);
    }
}
