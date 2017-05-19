package com.teddyteh.kmusage;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.teddyteh.kmscraper.KMadapter;
import com.teddyteh.kmscraper.adapter.KMLoginException;
import com.teddyteh.kmscraper.adapter.KMUnavailableException;
import com.teddyteh.kmscraper.adapter.KMexception;
import com.teddyteh.kmscraper.model.History;
import com.teddyteh.kmscraper.model.Usage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;

/**
 * Created by teddy on 13/5/2017.
 */

public class MainPresenterImpl implements MainPresenter {
    MainView mainView;

    GooglePlayDriver mPlayDriver;
    FirebaseJobDispatcher mDispatcher;

    AccountManager mAccountManager;
    String mAccountType;
    Account mAccount;
    String mUser;
    String mPassword;
    String mNickName;
    KMadapter adapter;

    int mSync = 60 * 60;      //  Sync frequency in secs;

    public MainPresenterImpl(MainView mainView) {
        this.mainView = mainView;

        startUp();
    }

    public void startUp() {
        // Create the job dispatcher used for background sync
        mPlayDriver = new GooglePlayDriver(MainActivity.getContext());
        mDispatcher = new FirebaseJobDispatcher(mPlayDriver);

        // Get the tvAccount credentials (user & tvPassword)
        mAccountType = MainActivity.getContext().getString(R.string.account_type);
        mAccountManager = AccountManager.get(MainActivity.getContext());
        getCredentials();

        // Enable background sync
        JobService.scheduleAll(MainActivity.getContext());
    }

    @Override
    public void populateSpinner() {
        List<String> accountsArray = new ArrayList<String>();

        // Get all accounts
        Account[] acc = mAccountManager.getAccountsByType(mAccountType);

        // Add each account to spinner
        for (Account a : acc) {
            // Get account nickanme
            String nickname = mAccountManager.getUserData(a, AccountDetailsActivity.NICK_NAME);

            accountsArray.add(nickname);
        }
        accountsArray.add("Add account..");

        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(MainActivity.getContext(), R.layout.fragment_main_accounts_spinner, accountsArray);

        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mainView.fillSpinner(spinAdapter, new SpinnerListener());
    }

    @Override
    public void refresh() {
        mainView.setRefreshing(false);

        if (mUser != null && mPassword != null) {
            try {
                mainView.setLoading(true);

                KMadapter result = RetrieveTasks.getCachedData(MainActivity.getContext(), mUser, mPassword);
                Date maxTS = new Date();
                maxTS.setTime(maxTS.getTime() - mSync * 1000);
                if (result.isValid() && result.getTS().after(maxTS)) {
                    adapter = result;

                    drawGraph();
                    fillList();

                    mainView.setLoading(false);
                } else {
                    RetrieveData data = new RetrieveData();
                    data.execute(mUser, mPassword);
                }
            } catch (KMexception | CancellationException ex) {
                Log.e(MainActivity.TAG, "Background retrieve cancelled", ex);
            }
        }
    }

    public void drawGraph() {
        final int DURATION = 30;

        try {
            List<Usage> dataUsageList = adapter.getDataUsage();
            long quota = ((long) adapter.getQuota()) * 1024L * 1024L;
            Date renewalDate = adapter.getData_renews();
            long dataUsed = adapter.getNational_data();

            // Start of period date (renews - 30 days)
            Calendar cal = Calendar.getInstance();
            cal.setTime(renewalDate);
            cal.add(Calendar.DATE, -DURATION);
            Date startOfPeriod = new Date(cal.getTimeInMillis());

            // Days passed
            Date today = new Date();
            today.setDate(today.getDate() + 1);
            long diff = today.getTime() - startOfPeriod.getTime();
            int elapsedDays = (int) (diff / (1000 * 60 * 60 * 24));

            DataPoint[] historyDataPoints = makeHistoryDataPoints(dataUsageList, quota, startOfPeriod, elapsedDays);
            LineGraphSeries<DataPoint> historySeries = new LineGraphSeries<>(historyDataPoints);
            DataPoint[] projectedDataPoints = makeProjectedDataPoints(quota, dataUsed, elapsedDays);
            LineGraphSeries<DataPoint> projectedSeries = new LineGraphSeries<>(projectedDataPoints);

            Date firstDay = getFirstDay(dataUsageList);
            Date lastDay = adapter.getData_renews();

            mainView.graphDraw(historySeries, projectedSeries, firstDay, lastDay, quota);
        } catch (KMexception ex) {
            Log.e(MainActivity.TAG, "Error accessing adapter", ex);
            throw new RuntimeException("Error accessing adapter", ex);
        }
    }

    public void fillList() {
        ArrayList<ListItem> list = new ArrayList<ListItem>();
        try {
            String daysUsed = (30 - adapter.getDataDaysRemaining()) + " of 30 days (" + percent(30 - adapter.getDataDaysRemaining(), 30) + "%)";
            String timeUpd = fmtTime(adapter.getTS());
            String dataUpd = fmtTime(adapter.getMostRecentDataUsage());

            list.add(new ListItem("Account name", adapter.getName()));
            list.add(new ListItem("Mobile number", adapter.getNumber()));
            list.add(new ListItem("Days used", daysUsed));
            list.add(new ListItem("Data renews", dateFmt(adapter.getData_renews())));
            list.add(new ListItem("Plan", adapter.getPlan()));
            list.add(new ListItem("Expires", dateFmt(adapter.getPlan_expires())));
            list.add(new ListItem("Last data", dataUpd));
            list.add(new ListItem("Last updated", timeUpd));

            mainView.fillList(list);
        } catch (KMexception ex) {
            Log.e(MainActivity.TAG, "Error accessing adapter", ex);
            throw new RuntimeException("Error accessing adapter", ex);
        }
    }

    @Override
    public void onDestroy() {
        if (mDispatcher != null) {
            Log.i(MainActivity.TAG, "Background scrape service cancelled");
            mDispatcher.cancel("RetrieveFirebaseService");
        }
    }

    @Override
    public void forceRefresh() {
        refresh();
    }

    private void getCredentials() {
        Account[] acc = mAccountManager.getAccountsByType(mAccountType);
        if (acc.length == 0) {
            Log.e(MainActivity.TAG, "No accounts of type " + mAccountType + " found");
            mAccountManager.addAccount(
                    MainActivity.getContext().getString(R.string.account_type),
                    MainActivity.getContext().getString(R.string.authtoken_type),
                    null,
                    new Bundle(),
                    mainView.getActivity(),
                    new OnAccountAddComplete(),
                    null);
        } else {
            mAccount = acc[0];
            mPassword = mAccountManager.getPassword(mAccount);
            mUser = mAccount.name;
            mNickName = mAccountManager.getUserData(mAccount, AccountDetailsActivity.NICK_NAME);
        }
    }

    private DataPoint[] makeHistoryDataPoints(List<Usage> dataUsageList, long quota, Date startOfPeriod, int elapsedDays) {
        Map<Date, Long> map = getCumulativeUsage(getUsage(dataUsageList, startOfPeriod), quota, elapsedDays);

        DataPoint[] points = new DataPoint[elapsedDays];

        int counter = 0;
        for (Map.Entry<Date, Long> entry : map.entrySet()) {
            if (counter == elapsedDays)
                break;

            Date date = entry.getKey();
            Long usage = entry.getValue();

            points[counter] = new DataPoint(date, bytesToMegabytes(usage));

            counter++;
        }

        return points;
    }

    private Map<Date, Long> getUsage(List<Usage> dataUsageList, Date startOfPeriod) {
        HashMap<Date, Long> map = new HashMap<>();

        Calendar cal = Calendar.getInstance(); // this would default to now
        cal.setTime(startOfPeriod);
        // Create 30 empty entries
        for (int i = 0; i < 30; i++) {
            Date day = makeDay(new Date(cal.getTimeInMillis()));
            map.put(day, 0L);
            cal.add(Calendar.DATE, 1);
        }

        // map individual TS to given day, summarising volume
        for (Usage u : dataUsageList) {
            Date day = makeDay(u.getTs());
            Long usage = u.getVolume();

            if (map.containsKey(day)) {
                Long total = map.get(day);
                total += usage;
                map.put(day, total);
            }
        }

        SortedMap<Date, Long> sortedMap = new TreeMap<>(map);

        return sortedMap;
    }

    private Map<Date, Long> getCumulativeUsage(Map<Date, Long> dataUsageList, long quota, int daysPassed) {
        int counter = 0;

        for(Map.Entry<Date, Long> entry : dataUsageList.entrySet()) {
            if (counter == daysPassed)
                break;

            Date key = entry.getKey();
            long value = entry.getValue();

            // usage for the first day
            if (counter == 0) {
                dataUsageList.put(key, quota - value);
            }
            if (counter > 0) {
                // usage up until the day before the current entry
                long previousDay = dataUsageList.get(dataUsageList.keySet().toArray()[counter-1]);
                dataUsageList.put(key, previousDay - value);
            }

            counter++;
        }

        return dataUsageList;
    }

    private DataPoint[] makeProjectedDataPoints(long quota, long dataUsed, int elapsedDays) {
        long averageDailyData = dataUsed / (long) elapsedDays;
        int daysLeft = 30 - elapsedDays + 1;

        HashMap<Date, Long> map = new HashMap<>();

        Calendar calendar = Calendar.getInstance(); // this defaults to now
        // Create 30 empty entries
        for (int i = 0; i <= daysLeft; i++) {
            Date day = makeDay(new Date(calendar.getTimeInMillis()));
            map.put(day, 0L);
            calendar.add(Calendar.DATE, 1);
        }

        SortedMap<Date, Long> sortedMap = new TreeMap<>(map);

        DataPoint[] points = new DataPoint[daysLeft+1];

        int counter = 0;
        for (Map.Entry<Date, Long> entry : sortedMap.entrySet()) {
            if (counter == daysLeft+1)
                break;

            Date date = entry.getKey();
            Long usage = entry.getValue();

            if (counter == 0) {
                long value = bytesToMegabytes(quota-dataUsed);
                points[0] = new DataPoint(date, value);
            }
            if (counter > 0) {
                long value = bytesToMegabytes(quota-dataUsed-(averageDailyData * counter));
                points[counter] = new DataPoint(date, value);
            }

            counter++;
        }

        return points;
    }

    private Date makeDay(Date ts) {
        if (ts == null) return null;
        ts.setHours(0);
        ts.setMinutes(0);
        ts.setSeconds(0);

        return ts;
    }

    private long bytesToMegabytes(long bytes) {
        long MEGABYTE = 1024L * 1024L;
        long b = bytes / MEGABYTE;

        return b;
    }

    private Date getFirstDay(List<Usage> dataUsageList) {
        Date date = null;
        for (Usage u : dataUsageList) {
            if (date == null || u.getTs().before(date))
                date = u.getTs();
        }
        return makeDay(date);
    }

    /*
     *  Take two integers and return an int giving the percent
     */
    private int percent(int x, int y) {
        double pc = (double) x / (double) y * 100.0d;
        return (int) pc;
    }

    /*
     *  Format a date into dayname monname day
     */
    private String dateFmt(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("EEE MMM d");
        return df.format(date);
    }

    private String fmtTime(Date ts) {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm MMM d");
        return df.format(ts);
    }

    private class OnAccountAddComplete implements AccountManagerCallback<Bundle> {
        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            Bundle bundle;
            try {
                bundle = result.getResult();
            } catch (OperationCanceledException ex) {
                Log.e(MainActivity.TAG, "Account add cancelled", ex);
                return;
            } catch (AuthenticatorException ex) {
                Log.e(MainActivity.TAG, "Account add error", ex);
                return;
            } catch (IOException ex) {
                Log.e(MainActivity.TAG, "Account add error", ex);
                return;
            }
            mAccount = new Account(
                    bundle.getString(AccountManager.KEY_ACCOUNT_NAME),
                    bundle.getString(AccountManager.KEY_ACCOUNT_TYPE)
            );
            Log.d(MainActivity.TAG, "Added account " + mAccount.name + ", fetching");
            mUser = mAccount.name;
            mPassword = mAccountManager.getPassword(mAccount);
            mNickName = mAccountManager.getUserData(mAccount, AccountDetailsActivity.NICK_NAME);
            JobService.scheduleAll(MainActivity.getContext());
        }
    }

    private class RetrieveData extends AsyncTask<String, Void, KMadapter> {
        private final String TAG = "RetrieveData";
        private KMadapter result = null;

        public RetrieveData() {

        }

        @Override
        protected void onPreExecute() {
            mainView.setLoading(true);
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
            mainView.setLoading(false);

            adapter = result;

            String msg;
            if (adapter != null && adapter.isValid()) {
                List<History> history = adapter.getHistoricalDataUsage();

            } else {
                // The login failed
                // This code should NEVER be executed.  The userid/password should have
                // been validated when the addAccount() was executed.
                Log.e(TAG, "Unexpected login failure - has the password changed?");
                Toast.makeText(MainActivity.getContext(), R.string.login_failure, Toast.LENGTH_LONG).show();
//                msg = "Kogan Mobile login failure";
//                CoordinatorLayout layout = (CoordinatorLayout) findViewById(R.id.main_layout);
//                TextView tvError = new TextView(MainActivity.getContext());
//                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
//                tvError.setText(getString(R.string.login_failure));
//                tvError.setTextSize(20.0f);
//                tvError.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
//                tvError.setLayoutParams(params);
//                layout.addView(tvError, 0);
            }
        }
    }

    private class SpinnerListener implements AdapterView.OnItemSelectedListener {
        private SpinnerListener() {
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // Get all accounts
            Account[] acc = mAccountManager.getAccountsByType(mAccountType);

            // Add an account
            if (position == (parent.getAdapter().getCount() - 1)) {
                if (acc.length > 0) {
                    mAccountManager.addAccount(
                        MainActivity.getContext().getString(R.string.account_type),
                        MainActivity.getContext().getString(R.string.authtoken_type),
                        null,
                        new Bundle(),
                        mainView.getActivity(),
                        new OnAccountAddComplete(),
                        null);
                }
            } else {
                // Update selected account
                mAccount = acc[position];
                mPassword = mAccountManager.getPassword(mAccount);
                mUser = mAccount.name;
                mNickName = mAccountManager.getUserData(mAccount, AccountDetailsActivity.NICK_NAME);

                // Refresh
                refresh();

                //Toast.makeText(parent.getContext(), "Showing info for " + mNickName, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    protected class ListItem {
        private String name;
        private String desc;

        public ListItem(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }

        public String getName() {
            return this.name;
        }

        public String getDesc() {
            return this.desc;
        }
    }
}
