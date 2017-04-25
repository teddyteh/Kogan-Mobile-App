package com.teddyteh.kmscraper.adapter;

import org.jsoup.nodes.Document;

/**
 * Created by matthew on 21/03/2017.
 */

public class KMDocumentException extends KMexception {

    private static final long serialVersionUID = 1L;
    private Document doc = null;

    public KMDocumentException() {
        // TODO Auto-generated constructor stub
    }

    public KMDocumentException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public KMDocumentException(String message, Document doc) {
        super(message);
        this.doc = doc;
    }

    public KMDocumentException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

    public KMDocumentException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public KMDocumentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        // TODO Auto-generated constructor stub
    }

    public Document getDoc() {
        return doc;
    }

}
