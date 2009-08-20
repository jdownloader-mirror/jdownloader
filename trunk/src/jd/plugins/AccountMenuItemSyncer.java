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

package jd.plugins;

import java.util.HashMap;

import jd.config.MenuAction;
import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.gui.swing.GuiRunnable;

/**
 * This calss maps a MenItem to an account, and help to synchronize states like
 * enabled/selected
 * 
 * @author Coalado
 * 
 */
public class AccountMenuItemSyncer implements AccountControllerListener {
    private static AccountMenuItemSyncer INSTANCE = null;
    private HashMap<Account, MenuAction> map;

    private AccountMenuItemSyncer() {
        map = new HashMap<Account, MenuAction>();
        AccountController.getInstance().addListener(this);
    }

    public synchronized static AccountMenuItemSyncer getInstance() {
        if (INSTANCE == null) INSTANCE = new AccountMenuItemSyncer();
        return INSTANCE;
    }

    public void map(Account a, MenuAction m) {

        map.put(a, m);

    }

    public void onAccountControllerEvent(final AccountControllerEvent event) {

        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                MenuAction item = map.get(event.getAccount());
                if (item != null) item.setSelected(event.getAccount().isEnabled());
                return null;
            }

        }.start();

    }

    public boolean vetoAccountGetEvent(String host, Account account) {
        // TODO Auto-generated method stub
        return false;
    }

    public MenuAction get(Account a) {
        // TODO Auto-generated method stub
        return map.get(a);
    }

    // 

}
