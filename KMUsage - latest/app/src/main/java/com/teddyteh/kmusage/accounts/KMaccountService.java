package com.teddyteh.kmusage.accounts;

/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.teddyteh.kmusage.AccountDetailsActivity;

public class KMaccountService extends Service {
    public static final String ACCOUNT_NAME = "Kogan Mobile";
    private static final String TAG = "KMaccountService";
    private Authenticator mAuthenticator;

    /**
     * Obtain a handle to the {@link Account} used for sync in this application.
     * <p>
     * <p>It is important that the accountType specified here matches the value in your sync adapter
     * configuration XML file for android.accounts.AccountAuthenticator (often saved in
     * res/xml/syncadapter.xml). If this is not set correctly, you'll receive an error indicating
     * that "caller uid XXXXX is different than the authenticator's uid".
     *
     * @param accountType AccountType defined in the configuration XML file for
     *                    android.accounts.AccountAuthenticator (e.g. res/xml/syncadapter.xml).
     * @return Handle to application's account (not guaranteed to resolve unless CreateSyncAccount()
     * has been called)
     */
    public static Account GetAccount(String accountType) {
        // Note: Normally the account name is set to the user's identity (username or email
        // address). However, since we aren't actually using any user accounts, it makes more sense
        // to use a generic string in this case.
        //
        // This string should *not* be localized. If the user switches locale, we would not be
        // able to locate the old account, and may erroneously register multiple accounts.
        final String accountName = ACCOUNT_NAME;
        return new Account(accountName, accountType);
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "KMaccountService Service created");
        mAuthenticator = new Authenticator(this);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "KMaccountService Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
//        if (intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT))
//            return null;
        return mAuthenticator.getIBinder();
    }

    public class Authenticator extends AbstractAccountAuthenticator {
        protected Context m_Context;

        public Authenticator(Context context) {
            super(context);
            m_Context = context;
        }

        @Override
        public Bundle editProperties(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                     String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle addAccount(AccountAuthenticatorResponse response,
                                 String accountType, String authTokenType,
                                 String[] requiredFeatures, Bundle options)
                throws NetworkErrorException {
            // We absolutely cannot add an account without some information
            // from the user; so we're definitely going to return an Intent
            // via KEY_INTENT
            final Bundle bundle = new Bundle();

            // We're going to use a LoginActivity to talk to the user (mContext
            // we'll have noted on construction).
            final Intent intent = new Intent(m_Context, AccountDetailsActivity.class);

            // We can configure that activity however we wish via the
            // Intent.  We'll set ARG_IS_ADDING_NEW_ACCOUNT so the Activity
            // knows to ask for the account name as well
            intent.putExtra(AccountDetailsActivity.ARG_ACCOUNT_TYPE, accountType);
            intent.putExtra(AccountDetailsActivity.ARG_AUTH_TYPE, authTokenType);
            intent.putExtra(AccountDetailsActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);

            // It will also need to know how to send its response to the
            // account manager; LoginActivity must derive from
            // AccountAuthenticatorActivity, which will want this key set
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                    response);

            // Wrap up this intent, and return it, which will cause the
            // intent to be run
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
            return bundle;
        }

        @Override
        public Bundle confirmCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                         Account account, Bundle bundle)
                throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle getAuthToken(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                   Account account, String s, Bundle bundle)
                throws NetworkErrorException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getAuthTokenLabel(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle updateCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                        Account account, String s, Bundle bundle)
                throws NetworkErrorException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle hasFeatures(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                  Account account, String[] strings)
                throws NetworkErrorException {
            throw new UnsupportedOperationException();
        }
    }

}

