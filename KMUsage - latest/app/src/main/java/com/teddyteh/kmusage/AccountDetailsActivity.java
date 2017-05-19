package com.teddyteh.kmusage;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.teddyteh.kmscraper.KMadapter;
import com.teddyteh.kmscraper.adapter.KMexception;

import java.util.concurrent.ExecutionException;

public class AccountDetailsActivity extends AccountAuthenticatorActivity {

    public static final String TAG = "AccountDetails";

    public static final String ARG_ACCOUNT_NAME = "com.teddyteh.ACCOUNT_NAME";
    public static final String ARG_ACCOUNT_TYPE = "com.teddyteh.ACCOUNT_TYPE";
    public static final String ARG_AUTH_TYPE = "com.teddyteh.AUTH_TYPE";
    public static final String ARG_IS_ADDING_NEW_ACCOUNT = "com.teddyteh.IS_ADDING_NEW_ACCOUNT";
    public static final String PARAM_USER_PASS = "com.teddyteh.USER_PASS";
    public static final String NICK_NAME = "com.teddyteh.NICK_NAME";

    TextView tvAccount;
    TextView tvPassword;
    TextView tvNickName;
    TextView tvStatusMsg;
    Button btnLogIn;
    Button btnTest;
    ProgressBar progressBar;
    private AccountManager mAccountManager;
    private String mAccountName;
    private String mPassword;
    private String mNickName;
    private String mAccountType;
    private String mAuthType;
    private String mAuthTokenType;
    private AccountAuthenticatorResponse mResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Running on thread = " + ThreadUtils.getThreadId());
        setContentView(R.layout.activity_account_details);

        tvAccount = (TextView) findViewById(R.id.account);
        tvPassword = (TextView) findViewById(R.id.password);
        btnLogIn = (Button) findViewById(R.id.btnLogin);
        tvNickName = (TextView) findViewById(R.id.nickName);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        final Activity activity = this;

        // Set a placeholder nickname to the account number the user has entered
        tvAccount.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                tvNickName.setText(s);
            }
        });

        btnLogIn.setOnClickListener(new View.OnClickListener() {
            private boolean isNumeric(String str) {
                return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
            }

            private boolean validatePhoneNumber(String number) {
                if (!isNumeric(number) || number.length() != 10 || !number.startsWith("04"))
                    return false;

                return true;
            }

            private boolean validateAccountNumber(String number) {
                if (!isNumeric(number) || number.length() != 9 || number.startsWith("0"))
                    return false;

                return true;
            }

            @Override
            public void onClick(View v) {
                Log.d(TAG, "Login button pressed");

                // Initialise variables
                int loginResult = 0;

                // Get user inputs
                mAccountName = tvAccount.getText().toString();
                mPassword = tvPassword.getText().toString();
                mNickName = tvNickName.getText().toString();

                /**
                 * Validate user inputs
                 **/
                if (!validatePhoneNumber(mAccountName) && !validateAccountNumber((mAccountName))) {
                    // Username is invalid
                    Toast.makeText(activity, "Invalid username", Toast.LENGTH_LONG).show();
                } else if (!isNumeric(mPassword) || tvPassword.length() != 6) {
                    // Password is invalid
                    Toast.makeText(activity, "Invalid password", Toast.LENGTH_LONG).show();
                } else {
                    // User inputs are valid, attempt to login
                    progressBar.setVisibility(View.VISIBLE);
                    TestLogin test = new TestLogin(activity);
                    test.execute(mAccountName, mPassword, mNickName);
                    try {
                        loginResult = test.get();
                    } catch (InterruptedException | ExecutionException e) {
                        Log.e(TAG, "Error testing login", e);
                        loginResult = -1;
                    }
                    progressBar.setVisibility(View.GONE);

                    // Actually login if test is successful. Otherwise report the error
                    switch (loginResult) {
                        case 0:
                            Toast.makeText(activity, R.string.invalid_password, Toast.LENGTH_LONG).show();
                            break;
                        case 1:
                            final Intent res = new Intent();
                            res.putExtra(AccountManager.KEY_ACCOUNT_NAME, mAccountName);
                            res.putExtra(AccountManager.KEY_ACCOUNT_TYPE, mAccountType);
                            res.putExtra(PARAM_USER_PASS, mPassword);
                            res.putExtra(AccountDetailsActivity.NICK_NAME, mNickName);
                            finishLogin(res);
                            finish();
                            break;
                        default:
                            Toast.makeText(activity, R.string.server_unavailable, Toast.LENGTH_LONG).show();
                    }

                }
            }
        });

        // Setup authenticator hooks
        mAccountManager = AccountManager.get(getBaseContext());
        mAccountName = getIntent().getStringExtra(ARG_ACCOUNT_NAME);
        mAccountType = getIntent().getStringExtra(ARG_ACCOUNT_TYPE);
        mAuthType = getIntent().getStringExtra(ARG_AUTH_TYPE);
    }

    private void finishLogin(Intent intent) {
        //String accountPassword = intent.getStringExtra(PARAM_USER_PASS);
        String accountPassword = mPassword;
        final Account account = new Account(
                mAccountName,
                intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));

        if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            // Creating the account
            // Password is optional to this call, safer not to send it really.
            Bundle extraData = new Bundle();
            extraData.putString(AccountDetailsActivity.NICK_NAME, mNickName);
            mAccountManager.addAccountExplicitly(account, accountPassword, extraData);
        } else {
            // Password change only
            mAccountManager.setPassword(account, accountPassword);
        }
        // Our base class can do what Android requires with the
        // KEY_ACCOUNT_AUTHENTICATOR_RESPONSE extra that onCreate has
        // already grabbed
        setAccountAuthenticatorResult(intent.getExtras());
        // Tell the tvAccount manager settings page that all went well
        setResult(RESULT_OK, intent);
    }

    public abstract class TextValidator implements TextWatcher {
        private final TextView textView;

        public TextValidator(TextView textView) {
            this.textView = textView;
        }

        public abstract void validate(TextView textView, String text);

        @Override
        final public void afterTextChanged(Editable s) {
            String text = textView.getText().toString();
            validate(textView, text);
        }

        @Override
        final public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Don't care */ }

        @Override
        final public void onTextChanged(CharSequence s, int start, int before, int count) { /* Don't care */ }
    }

    public class TestLogin extends AsyncTask<String, Void, Integer> {
        Activity activity;
        int result;

        public TestLogin(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }


        @Override
        protected Integer doInBackground(String... param) {
            Log.d(TAG, "TestLogin running on thread = " + ThreadUtils.getThreadId());
            String account = param[0];
            String pwd = param[1];
            result = 0;
            try {
                result = RetrieveTasks.testLogin(activity, account, pwd);
            } catch (KMexception ex) {
                Log.e(TAG, "Error testing login", ex);
                result = -1;
            }
            return result;
        }

        protected void onPostExecute(KMadapter result) {
            progressBar.setVisibility(View.GONE);
        }
    }
}
