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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.teddyteh.kmscraper.KMadapter;
import com.teddyteh.kmscraper.adapter.KMLoginException;
import com.teddyteh.kmscraper.adapter.KMUnavailableException;

/**
 * Created by matthew on 2/04/2017.
 */

public class RetrieveFirebaseService extends JobService {

    private final static String TAG = "RetrieveFirebaseService";
    KMadapter adapter;
    private AsyncTask<String, Void, KMadapter> mBackgroundTask;

    /*
     *  This method is called by the Firebase job mDispatcher to start the job. This
     *  method runs on the apps main thread, so another thread is spawned to perform the
     *  network and database IO.
     */
    @Override
    public boolean onStartJob(final JobParameters job) {

        final Bundle jobBundle = job.getExtras();
        final String user = jobBundle.getString("USER");
        final String pwd = jobBundle.getString("PASSWORD");

        mBackgroundTask = new AsyncTask<String, Void, KMadapter>() {

            boolean loginFailed;    // used to cause job to be retried

            @Override
            protected KMadapter doInBackground(String... param) {
                String user = param[0];
                String pwd = param[1];
                loginFailed = true;
                Context context = RetrieveFirebaseService.this;
                try {
                    if (user != null && pwd != null) {
                        adapter = RetrieveTasks.getData(context, user, pwd);
                        loginFailed = false;
                    }
                } catch (KMLoginException | KMUnavailableException ex) {
                    loginFailed = true;
                    Log.e(TAG, "Login failure for user '" + user + "'");
                }
                return adapter;
            }

            /*
             *  Once the async task is finished, inform the JobManager that the job is complete
             *  The boolean flag indicates that the job must be rescheduled, perhaps after network
             *  errors.  Here a login failure will cause it to be rerun
             */
            protected void onPostExecute(String... param) {
                jobFinished(job, loginFailed);
            }
        };
        Log.i(TAG, "Background scrape of Kogan Mobile running");
        mBackgroundTask.execute(user, pwd);
        return false;
    }

    @Override
    public boolean onStopJob(final JobParameters job) {
        return false;
    }
}
