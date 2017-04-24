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


import android.content.Context;
import android.util.Log;

import com.teddyteh.kmscraper.KMadapter;
import com.teddyteh.kmscraper.adapter.KMLoginException;
import com.teddyteh.kmscraper.adapter.KMUnavailableException;
import com.teddyteh.kmscraper.adapter.KMexception;
import com.teddyteh.kmscraper.adapter.KMscrape;

/**
 * Created by matthew on 2/04/2017.
 */

public class RetrieveTasks {

    public final static String TAG = "RetrieveTasks";

    /*
     *  Generic method to retrieve the Kogan Mobile tvAccount data.  While this will query a SQLite
     *  cache, it may perform newtork IO, so it can never be called from the main thread, but always
     *  from an AsyncTask
     */
    public static KMadapter getCachedData(Context context, String user, String pwd) throws KMLoginException {
        KMadapter adapter = new KMscrape(context, user, pwd);
        Log.i(TAG, "Retrieve account data (name: " + user + ")");
        try {
            adapter.getCachedData();
        } catch (KMLoginException ex) {
            //  get new tvAccount credentials
            Log.e(TAG, "Login failure for user '" + user + "'");
            throw new KMLoginException();
        } catch (KMexception ex) {
            Log.e(TAG, ex.toString(), ex);
        }

        return adapter;
    }

    public static KMadapter getData(Context context, String user, String pwd) throws KMLoginException, KMUnavailableException {
        KMadapter adapter = new KMscrape(context, user, pwd);
        Log.i(TAG, "Retrieve account data (name: " + user + ")");
        try {
            adapter.getData();
        } catch (KMLoginException ex) {
            //  get new tvAccount credentials
            Log.e(TAG, "Login failure for user '" + user + "'");
            throw new KMLoginException();
        } catch (KMUnavailableException ex) {
            Log.e(TAG, "Server unavailable", ex);
            throw new KMUnavailableException(ex);
        } catch (KMexception ex) {
            Log.e(TAG, ex.toString(), ex);
        }

        return adapter;
    }

    public static KMadapter getHistoricalDataUsage(Context context, String user, String pwd) throws KMLoginException {
        KMadapter adapter = new KMscrape(context, user, pwd);
        Log.i(TAG, "Retrieve historical usage (name: " + user + ")");
        try {
            adapter.getHistoricalDataUsage();
        } catch (Exception ex) {
            Log.e(TAG, ex.toString(), ex);
        }

        return adapter;
    }

    public static int testLogin(Context context, String user, String pwd) throws KMexception {
        KMadapter adapter = new KMscrape(context, user, pwd);
        Log.i(TAG, "Validate login credentials (name: " + user + ")");
        return adapter.testLogin();
    }
}
