package com.teddyteh.kmusage;

/**
 * Created by teddy on 11/5/2017.
 */

public interface MainPresenter {
    void populateSpinner();

    void refresh();

    void onDestroy();

    void forceRefresh();
}
