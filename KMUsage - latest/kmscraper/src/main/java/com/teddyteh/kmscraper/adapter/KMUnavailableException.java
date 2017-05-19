package com.teddyteh.kmscraper.adapter;

/**
 * Created by matthew on 19/04/2017.
 */

public class KMUnavailableException extends KMexception {
    public KMUnavailableException() {
        super();
    }

    public KMUnavailableException(Throwable ex) {
        super(ex);
    }

    public KMUnavailableException(String msg) {
        super(msg);
    }

    public KMUnavailableException(String msg, Throwable ex) {
        super(msg, ex);
    }
}
