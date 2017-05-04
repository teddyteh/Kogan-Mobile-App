package com.teddyteh.kmusage;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Trigger;
import com.teddyteh.kmscraper.KMadapter;
import com.teddyteh.kmscraper.adapter.KMLoginException;
import com.teddyteh.kmscraper.adapter.KMUnavailableException;
import com.teddyteh.kmscraper.adapter.KMexception;
import com.teddyteh.kmscraper.model.History;
import com.teddyteh.kmscraper.model.Usage;
import com.teddyteh.kmusage.fragments.AboutFragment;
import com.teddyteh.kmusage.fragments.AnalyticsFragment;
import com.teddyteh.kmusage.fragments.MainFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CancellationException;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private static Context m_Context;
    GooglePlayDriver mPlayDriver;
    FirebaseJobDispatcher mDispatcher;
    String mAccountType;
    AccountManager mAccountManager;
    Account mAccount;
    private KMadapter adapter;
    private String mUser;
    private String mPassword;
    private String mNickName;
    // Shared Prefs
    private int mSync;      //  Sync frequency in secs;
    // GUI references
    private ProgressBar mProgress;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private Spinner mSpinner;

    public static Context getContext() {
        return m_Context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // hide title from toolbar

        mProgress = (ProgressBar) findViewById(R.id.mProgress);
        mProgress.setVisibility(View.GONE);

        // Setup static context which can be referenced in other tasks
        m_Context = getApplicationContext();

        // Create the job dispatcher used for background sync
        mPlayDriver = new GooglePlayDriver(MainActivity.this);
        mDispatcher = new FirebaseJobDispatcher(mPlayDriver);

        // Retrieve the saved prefs - sync freq (in secs)
        getSharedPrefs();

        // Get the tvAccount credentials (user & tvPassword)
        mAccountType = getString(R.string.account_type);
        mAccountManager = AccountManager.get(this);
        getCredentials();

        // Enable background sync
        scheduleJob(mUser, mPassword, mSync);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        // TODO - Save/restore app state in saveInstanceState
        // Instead of constantly querying the database or (worse) going
        // to scrape the web pages, save the data in the bundle
        super.onResume();
        populateSpinner();
        refreshLayout();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDispatcher != null) {
            Log.i(TAG, "Background scrape service cancelled");
            mDispatcher.cancel("RetrieveFirebaseService");
        }

    }

    private void getSharedPrefs() {
        // TODO - retrieve shared prefs
        mSync = 60 * 60;        // Sync every 60 mins
    }

    private void getCredentials() {
        Account[] acc = mAccountManager.getAccountsByType(mAccountType);
        if (acc.length == 0) {
            Log.e(TAG, "No accounts of type " + mAccountType + " found");
            mAccountManager.addAccount(
                    getString(R.string.account_type),
                    getString(R.string.authtoken_type),
                    null,
                    new Bundle(),
                    this,
                    new OnAccountAddComplete(),
                    null);
        } else {
            mAccount = acc[0];
            mPassword = mAccountManager.getPassword(mAccount);
            mUser = mAccount.name;
            mNickName = mAccountManager.getUserData(mAccount, AccountDetailsActivity.NICK_NAME);
        }
    }

    private void scheduleJob(String user, String password, int sync) {
        int minSync = (sync * 90) / 100;
        int maxSync = (minSync * 115) / 100;
        Bundle jobBundle = new Bundle();
        jobBundle.putString("USER", user);
        jobBundle.putString("PASSWORD", password);
        mDispatcher.mustSchedule(
                mDispatcher.newJobBuilder()
                        .setService(RetrieveFirebaseService.class)
                        .setTag("RetrieveFirebaseService")
                        .setRecurring(true)
                        .setTrigger(Trigger.executionWindow(minSync, maxSync))
//                         .setTrigger(Trigger.executionWindow(60, 90)) // testing only
                        .setExtras(jobBundle)
                        .build()
        );
    }

    public void refreshLayout() {
        if (mUser != null && mPassword != null) {
            try {
                KMadapter result = RetrieveTasks.getCachedData(getApplicationContext(), mUser, mPassword);
                Date maxTS = new Date();
                maxTS.setTime(maxTS.getTime() - mSync * 1000);
                if (result.isValid() && result.getTS().after(maxTS)) {
                    adapter = result;

                    setupViewPager();
                } else {
                    RetrieveData data = new RetrieveData(this);
                    data.execute(mUser, mPassword);
                }
            } catch (KMexception | CancellationException ex) {
                Log.e(TAG, "Background retrieve cancelled", ex);
            }
        }
    }

    public void populateSpinner() {

        mSpinner = (Spinner) findViewById(R.id.accounts_spinner);

        List<String> spinnerArray = new ArrayList<String>();

        Account[] acc = mAccountManager.getAccountsByType(mAccountType);
        for (Account a : acc) {
            String nickname = mAccountManager.getUserData(a, AccountDetailsActivity.NICK_NAME);

            spinnerArray.add(nickname);
        }
        spinnerArray.add("Add account..");

        ArrayAdapter<String> spinAdapter = new ArrayAdapter<String>(
                this, R.layout.fragment_main_accounts_spinner, spinnerArray);

        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinner.setAdapter(spinAdapter);
        mSpinner.setOnItemSelectedListener(new ItemListener(this));
    }

    public void setupViewPager() {
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class ItemListener implements AdapterView.OnItemSelectedListener {

        Activity mCallingActivity;

        private ItemListener(Activity activity) {
            mCallingActivity = activity;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // Get all accounts
            Account[] acc = mAccountManager.getAccountsByType(mAccountType);

            // Add an account
            if (position == (parent.getAdapter().getCount() - 1)) {
                if (acc.length > 0) {
                    mAccountManager.addAccount(
                            getString(R.string.account_type),
                            getString(R.string.authtoken_type),
                            null,
                            new Bundle(),
                            mCallingActivity,
                            new OnAccountAddComplete(),
                            null);
                }
            } else {
                // Show info for an account


                // Update selected account
                mAccount = acc[position];
                mPassword = mAccountManager.getPassword(mAccount);
                mUser = mAccount.name;
                mNickName = mAccountManager.getUserData(mAccount, AccountDetailsActivity.NICK_NAME);

                // Refresh
                refreshLayout();

                //Toast.makeText(parent.getContext(), "Showing info for " + mNickName, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    public class RetrieveData extends AsyncTask<String, Void, KMadapter> {
        private final String TAG = "RetrieveData";
        private KMadapter result = null;
        private Activity activity;

        public RetrieveData(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            mProgress.setVisibility(View.VISIBLE);
        }

        /*
         *  This code excutes on the background thread.  It retrieves the data either out
         *  of the SQLite cache or by scraping the KM web site
         */
        @Override
        protected KMadapter doInBackground(String... param) {
            String account = param[0];
            String pwd = param[1];

            try {
                result = RetrieveTasks.getData(MainActivity.getContext(), account, pwd);
            } catch (KMLoginException | KMUnavailableException ex) {
                Log.e(TAG, "Login failure for mUser '" + account + "'");
            }
            return result;
        }

        /*
         *  This code executes on the main thread and thus can manipulate the View.  It will
         *  draw the screen, rendering the data retrieved in doInBackground()
         */
        @Override
        protected void onPostExecute(KMadapter result) {
            mProgress.setVisibility(View.GONE);
            adapter = result;

            String msg;
            if (adapter != null && adapter.isValid()) {
                List<History> history = adapter.getHistoricalDataUsage();
                setupViewPager();
            } else {
                // The login failed
                // This code should NEVER be executed.  The userid/password should have
                // been validated when the addAccount() was executed.
                Log.e(TAG, "Unexpected login failure - has the password changed?");
                Toast.makeText(activity, R.string.login_failure, Toast.LENGTH_LONG).show();
                msg = "Kogan Mobile login failure";
                CoordinatorLayout layout = (CoordinatorLayout) findViewById(R.id.main_content);
                TextView tvError = new TextView(MainActivity.getContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                tvError.setText(getString(R.string.login_failure));
                tvError.setTextSize(20.0f);
                tvError.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                tvError.setLayoutParams(params);
                layout.addView(tvError, 0);
            }
        }

    }

    /*
     *  Called after an account has been added.  Extracts the user/password and
     *  stores them in the instance variables
     */
    private class OnAccountAddComplete implements AccountManagerCallback<Bundle> {
        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            Bundle bundle;
            try {
                bundle = result.getResult();
            } catch (OperationCanceledException ex) {
                Log.e(TAG, "Account add cancelled", ex);
                return;
            } catch (AuthenticatorException ex) {
                Log.e(TAG, "Account add error", ex);
                return;
            } catch (IOException ex) {
                Log.e(TAG, "Account add error", ex);
                return;
            }
            mAccount = new Account(
                    bundle.getString(AccountManager.KEY_ACCOUNT_NAME),
                    bundle.getString(AccountManager.KEY_ACCOUNT_TYPE)
            );
            Log.d(TAG, "Added account " + mAccount.name + ", fetching");
            mUser = mAccount.name;
            mPassword = mAccountManager.getPassword(mAccount);
            mNickName = mAccountManager.getUserData(mAccount, AccountDetailsActivity.NICK_NAME);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
//            return PlaceholderFragment.newInstance(position + 1);
            switch (position) {
                case 0:
                    return MainFragment.newInstance(adapter);
                case 1:
                    return AnalyticsFragment.newInstance(adapter);
                case 2:
                    return AboutFragment.newInstance(0);
                default:
                    return AboutFragment.newInstance(0);
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
    }
}
