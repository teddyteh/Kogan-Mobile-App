package com.teddyteh.kmscraper.adapter;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by matthew on 21/03/2017.
 */

class KMpage {

    public static final String TAG = "KMpage";
    private String url;
    private int code;
    private String cookie;
    private String location;
    private String body;

    KMpage(String url, int code, String cookie, String location, String body) throws KMexception {
        this.url = url;
        this.code = code;
        this.cookie = cookie;
        this.location = null;
        this.body = body;

        if (location != null) {
            try {
                URL base = new URL(url);
                URL loc = new URL(base, location);
                this.location = loc.toString();
            } catch (MalformedURLException e) {
                Log.e(TAG, "Invalid URL: '" + url + "'", e);
                throw new KMexception(TAG + ": fatal error on URL '" + url + "'", e);
            }
        }
    }

    public String getUrl() {
        return url;
    }

    public int getCode() {
        return code;
    }

    public String getCookie() {
        return cookie;
    }

    public String getBody() {
        return body;
    }

    public String getLocation() {
        return location;
    }
}
