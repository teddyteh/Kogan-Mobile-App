package com.teddyteh.kmscraper.adapter;

/**
 * Created by matthew on 21/03/2017.
 */

public class KMLoginException extends KMexception {

    static final long serialVersionUID = 1L;
    KMpage page = null;

    public KMLoginException() {
        super();
    }

    public KMLoginException(String message) {
        super(message);
    }

    public KMLoginException(Throwable cause) {
        super(cause);
    }

    public KMLoginException(String message, Throwable cause) {
        super(message, cause);
    }

    public KMLoginException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
