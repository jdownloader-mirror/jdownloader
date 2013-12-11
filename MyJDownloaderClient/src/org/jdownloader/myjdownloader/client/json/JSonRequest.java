package org.jdownloader.myjdownloader.client.json;

/**
 * Copyright (c) 2009 - 2013 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.net.httpserver.requests
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */


/**
 * @author daniel
 * 
 */
public class JSonRequest extends AbstractJsonData{

    private String   url;
    private long     rid;
    private Object[] params;
    private int apiVer=-1;

    public int getApiVer() {
        return apiVer;
    }

    public void setApiVer(final int apiVer) {
        this.apiVer = apiVer;
    }

    public JSonRequest(/* Storable */) {
    }

    public Object[] getParams() {
        return params;
    }

    public long getRid() {
        return rid;
    }

    public String getUrl() {
        return url;
    }

    public void setParams(final Object[] params) {
        this.params = params;
    }

    public void setRid(final long timestamp) {
        rid = timestamp;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
