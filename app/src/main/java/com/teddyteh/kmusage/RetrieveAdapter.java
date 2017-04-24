package com.teddyteh.kmusage;

/*
Copyright (C) 2017  Teddy Teh

        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import android.os.AsyncTask;
import android.util.Log;

import com.teddyteh.kmscraper.KMadapter;
import com.teddyteh.kmscraper.adapter.KMLoginException;
import com.teddyteh.kmscraper.adapter.KMUnavailableException;

/**
 * Scrape the KoganMobile web site using Android Async api.
 * Retrieve result using get()
 */

class RetrieveAdapter extends AsyncTask<String, Void, KMadapter> {

    private final String TAG = "RetrieveAdapter";
    private KMadapter adapter = null;

    protected KMadapter doInBackground(String... param) {
        Log.d(TAG, "Running on thread = " + ThreadUtils.getThreadId());
        String account = param[0];
        String pwd = param[1];

        try {
            adapter = RetrieveTasks.getCachedData(MainActivity.getContext(), account, pwd);
            if (!adapter.isValid())
                adapter = RetrieveTasks.getData(MainActivity.getContext(), account, pwd);
        } catch (KMLoginException | KMUnavailableException ex) {
            Log.e(TAG, "Login failure for user '" + account + "'");
        }

        return adapter;
    }
}
