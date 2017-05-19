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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Trigger;

/**
 * Manage Firebase job services.  Whenever KM android accounts get created/deleted,
 * cancel all Firebase jobs and reschedule new ones based on the (possibly) new
 * account details
 */

public class JobService {

    private final static String TAG = JobService.class.getSimpleName();

    public static void scheduleAll(Context context){
        // Create the job dispatcher used for background sync
        GooglePlayDriver playDriver = new GooglePlayDriver(context);
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(playDriver);

        //  How often should the account usage data be retrieved (in secs)
        //  Allow 25% leeway
        int sync = 60 * 60;
        int minSync = (sync * 90) / 100;
        int maxSync = (minSync * 115) / 100;

        //  Cancel all Firebase jobs owned by this application
        dispatcher.cancelAll();

        //  Extract all the known KM accounts
        String accountType = context.getString(R.string.account_type);
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(accountType);

        //  Loop through the accounts starting a retrieval service for each
        for(Account account: accounts) {
            String userid = account.name;
            String password = accountManager.getPassword(account);
            if (password != null) {
                Bundle jobBundle = new Bundle();
                jobBundle.putString("USER", userid);
                jobBundle.putString("PASSWORD", password);
                Job job = dispatcher.newJobBuilder()
                        .setService(RetrieveFirebaseService.class)
                        .setTag("KMusage_" + userid)     //  Name used to cancel job if account deleted
                        .setRecurring(true)
                        .setTrigger(Trigger.executionWindow(minSync, maxSync))
                        .setExtras(jobBundle)
                        .build();
                dispatcher.mustSchedule(job);
                Log.i(TAG, "Retrieval of KM account '" + userid + "' scheduled for interval between " + minSync + " and " + maxSync + " seconds");
            } else
                Log.i(TAG, "Retrieval of KM account '" + userid + "' skipped - no password available");
        }

    }
}
