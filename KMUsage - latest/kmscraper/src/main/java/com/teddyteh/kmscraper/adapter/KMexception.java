package com.teddyteh.kmscraper.adapter;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;

public class KMexception extends Exception {

    private static final long serialVersionUID = 1L;
    private KMpage page = null;

    public KMpage getPage() {
        return page;
    }

    public KMexception() {
        super();
    }

    public KMexception(String message) {
        super(message);
    }

    public KMexception(String message, KMpage page) {
        super(message);
        this.page = page;
    }

    public KMexception(Throwable cause) {
        super(cause);
    }

    public KMexception(String message, Throwable cause) {
        super(message, cause);
    }

    @SuppressLint("NewApi")
    public KMexception(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
