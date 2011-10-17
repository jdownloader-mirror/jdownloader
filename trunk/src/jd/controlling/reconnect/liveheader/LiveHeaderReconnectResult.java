package jd.controlling.reconnect.liveheader;

import jd.controlling.reconnect.ReconnectResult;
import jd.controlling.reconnect.liveheader.remotecall.RouterData;

public class LiveHeaderReconnectResult extends ReconnectResult {

    private RouterData routerData;

    public LiveHeaderReconnectResult(RouterData routerData2) {
        routerData = routerData2;
    }

    public void setRouterData(RouterData test) {
        this.routerData = test;
    }

    public RouterData getRouterData() {
        return routerData;
    }

}
