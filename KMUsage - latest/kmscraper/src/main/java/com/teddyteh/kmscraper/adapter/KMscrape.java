package com.teddyteh.kmscraper.adapter;

import android.content.Context;
import android.util.Log;

import com.teddyteh.kmscraper.KMadapter;
import com.teddyteh.kmscraper.model.DatabaseManager;
import com.teddyteh.kmscraper.model.History;
import com.teddyteh.kmscraper.model.Summary;
import com.teddyteh.kmscraper.model.Usage;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.lang.String;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class KMscrape implements KMadapter {

    public static final String TAG = "KMscrape";
    private final String LOGIN = "https://accounts.koganmobile.com.au/customer/login";
    private final String SUMMARY = "https://accounts.koganmobile.com.au/customer/summary";
    private final String USAGE = "https://accounts.koganmobile.com.au/service/usage";

    private boolean m_Valid = false;
    private String m_Userid = null;
    private String m_Password = null;
    private String m_Cookie = null;
    private String m_CSRFtoken = null;
    private KMusage m_Usage = null;

    private Context m_Context;
    private Summary m_Summary;
    private List<Usage> allUsageList;
    private List<Usage> callUsageList;
    private List<Usage> textUsageList;
    private List<Usage> dataUsageList;
    private List<History> historicalUsageList;
    private Date mMostRecentDataUsage;

    private long national_data = 0L;

    public KMscrape(Context context, String userid, String pwd) {
        m_Userid = userid;
        m_Password = pwd;
        m_Valid = false;
        allUsageList = new ArrayList<Usage>();
        callUsageList = new ArrayList<Usage>();
        textUsageList = new ArrayList<Usage>();
        dataUsageList = new ArrayList<Usage>();
        historicalUsageList = new ArrayList<History>();

        DatabaseManager.init(context);
        m_Context = context;
    }

    /*
     *  Retrieve account data, first from the cache, then if not
     *  found, retrieve by scraping the KM web site
     */
    public void getCachedData() throws KMexception {
        final long MAX_AGE = (60 * 60 * 1000);      // Max age of one hour

        if (m_Userid == null || m_Password == null) {
            Log.i(TAG, "Unable to logon - No account credentials");
            throw new KMLoginException("No account credentails");
        }

        m_Summary = DatabaseManager.getInstance().getSummary(m_Userid);
        long age = (new Date()).getTime() - ((m_Summary != null) ? m_Summary.getTs().getTime() : 0);

        //  If a valid m_Summary entry exists, fetch the usage entries and use the cached data
        if (m_Summary != null && age < MAX_AGE) {
            Log.d(TAG, "Data retrieved from cache");
            allUsageList = DatabaseManager.getInstance().getUsage(m_Userid);
            processUsageList();
            m_Valid = true;
        }
    }

    /*
     *  Retrieve account data bu scraping the KM web site
     */
    public void getData() throws KMexception {
        Document htmlDocument;
        String htmlContentInStringFormat;

        if (m_Userid == null || m_Password == null) {
            Log.i(TAG, "Unable to logon - No account credentials");
            throw new KMLoginException("No account credentails");
        }

        m_Valid = doLogin(m_Userid, m_Password);
        if (!m_Valid) {
            Log.i(TAG, "Login failure for user " + m_Userid);
            throw new KMLoginException("Login failure for user " + m_Userid);
        }

        KMpage summPage = this.fetchPage(SUMMARY, m_Cookie, null);
        if (summPage.getCookie() != null) {
            m_Cookie = summPage.getCookie();
        }
        htmlContentInStringFormat = summPage.getBody();
        htmlDocument = Jsoup.parse(htmlContentInStringFormat, SUMMARY);
        KMsummary m_Summary = new KMsummary(htmlDocument);
        this.m_Summary = new Summary(m_Userid);
        this.m_Summary.setName(m_Summary.getName());
        this.m_Summary.setNumber(m_Summary.getNumber());
        this.m_Summary.setPlan(m_Summary.getPlan());
        this.m_Summary.setDataRenews(m_Summary.getData_renews());
        this.m_Summary.setPlanRenews(m_Summary.getPlan_expires());
        this.m_Summary.setQuota((int) m_Summary.getQuota());
        this.m_Summary.setDataPC(m_Summary.getData_percent());

        String urlParams = "csrfToken=" + m_CSRFtoken + "&usage-history-form[type]=All&usage-history-form[months]=12";
        KMpage usagePage = this.fetchPage(USAGE, m_Cookie, urlParams);
        htmlContentInStringFormat = usagePage.getBody();
        htmlDocument = Jsoup.parse(htmlContentInStringFormat, USAGE);
        m_Usage = new KMusage(htmlDocument, m_Summary.getData_renews(), m_Userid);
        allUsageList = m_Usage.getEntries();
        historicalUsageList = m_Usage.getHistory();
        processUsageList();
        save();
    }

    public int testLogin() throws KMexception {
        if (m_Userid == null || m_Password == null) {
            Log.i(TAG, "Unable to logon - No account credentials");
            throw new KMLoginException("No account credentails");
        }

        int result = (doLogin(m_Userid, m_Password)) ? 1 : 0;
        return result;
    }

    private void save() throws KMexception {
        DatabaseManager.getInstance().doTransaction(
                new Callable<Void>() {
                    @Override
                    public Void call() throws KMexception {
                        try {
                            DatabaseManager.getInstance().delSummary(m_Summary);
                            DatabaseManager.getInstance().addSummary(m_Summary);
                            DatabaseManager.getInstance().delUsage(getUser());
                            for (Usage usage : allUsageList) {
                                DatabaseManager.getInstance().addUsage(usage);
                            }
                            DatabaseManager.getInstance().delHistory(getUser());
                            for (History history : historicalUsageList) {
                                DatabaseManager.getInstance().addHistory(history);
                            }

                        } catch (Exception e) {
                            throw new KMexception("Transaction error: '" + e.toString() + "'", e);
                        }
                        return null;
                    }
                });
    }

    /*
     *  Process the list of all usage entries.  Assign to the data/voice/sms sublists
     *  and compute the m_Summary totals
     */
    private void processUsageList() {
        int calls = 0;
        int sms = 0;
        long totalVolume = 0;
        Date dataUsage = null;
        for (Usage usage : allUsageList) {
            switch (usage.getType()) {
                case "V":
                    calls++;
                    callUsageList.add(usage);
                    break;
                case "T":
                    sms++;
                    textUsageList.add(usage);
                    break;
                case "D":
                    totalVolume += usage.getVolume();
                    if (dataUsage != null && usage.getTs().after(dataUsage))
                        dataUsage = usage.getTs();
                    if (dataUsage == null) dataUsage = usage.getTs();
                    dataUsageList.add(usage);
            }
        }
        m_Summary.setCalls(calls);
        m_Summary.setSms(sms);
        national_data = totalVolume;
        m_Summary.setData(national_data);
        mMostRecentDataUsage = dataUsage;
    }

	/*
     * 	Login to KoganMobile
	 *
	 * 	1.	Retrieve the KM login page and extract the CSRF token
	 * 	2.	Post to the login page with the Userid/Password/CSRF token
	 */

    private boolean doLogin(String user, String pwd) throws KMexception {

        Document doc;
        String htmlString;
        String csrfToken = null;

	    /*
         * Retrieve the login page from KM.  Check that a valid page was fetched by
	     * 	a.	the HTTP code is 200
	     * 	b.	the body exists and is not empty
	     */
        Log.d(TAG, "Login user: " + user);
        KMpage postPage = null;
        KMpage loginPage = null;
        try {
            loginPage = this.fetchPage(LOGIN, m_Cookie, null);
        } catch (KMUnavailableException ex) {
            Log.e(TAG, "Server unavailable", ex);
            throw new KMUnavailableException(ex.getMessage());
        }
        if (loginPage.getCookie() != null) {
            m_Cookie = loginPage.getCookie();
        }
        Log.d(TAG, "Login page resp code = " + loginPage.getCode());
        if (loginPage.getCode() != 200 || loginPage.getBody() == null || loginPage.getBody().equals("")) {
            Log.e(TAG, "Fetch login page failed");
            throw new KMexception("Unable to fetch login page", loginPage);
        }

		/*
		 * Parse the login page to extract the CSRF token.  Check that it is valid by
		 * 	a.	the value exists and is not empty
		 * 	b.	it has a length of ?? chars
		 */
        htmlString = loginPage.getBody();
        doc = Jsoup.parse(htmlString);
        csrfToken = doc.select("input[name=csrfToken]").attr("value");
        Log.d(TAG, "CSRF token = '" + csrfToken + "'");
        if (csrfToken == null || csrfToken.equals("")) {
            Log.e(TAG, "CSRF token not found in login page");
            throw new KMexception("Unable extract CSRF token", loginPage);
        }
        m_CSRFtoken = csrfToken;

		/*
		 * POST to the login page, passing CSRF token/user/pwd
		 * Check that the HTTP code is 302 and Location is '/customer/m_Summary'
		 */
        Log.d(TAG, "POST login info for user: " + user + " token: " + csrfToken);
        String urlParams = "csrfToken=" + csrfToken + "&user_name=" + user + "&user_password=" + pwd;
        postPage = this.fetchPage(LOGIN, m_Cookie, urlParams);
        Log.d(TAG, "POST login page resp code = " + postPage.getCode() + " Location = " + postPage.getLocation());
        if (postPage.getCode() == 302 && postPage.getLocation() != null && postPage.getLocation().equals(SUMMARY))
            return true;

        /*
         * Analyse the login failure and throw error
         */
        Log.e(TAG, "Login failed for user: " + user);
        htmlString = postPage.getBody();
        doc = Jsoup.parse(htmlString);
        Elements err = doc.select("ul[id=flashes]").select("li");
        String errorMsg = err.text();
        csrfToken = doc.select("input[name=csrfToken]").attr("value");
        if (csrfToken == null || csrfToken.equals("")) {
            Log.e(TAG, "CSRF taken not found in login page");
            throw new KMexception("Unable extract CSRF token", postPage);
        }
        if (errorMsg.equals("Invalid username or password")) {
            Log.i(TAG, "Invalid username or password");
            throw new KMLoginException("Invalid username or password");
        }

        throw new KMexception("Login failed", postPage);
    }

    /*
     * Fetch a webpage, handling the following special cases
     * 	a.	Do not follow redirects, instead save the new location
     *  b.  By default, use a GET method
     *  c.  If URL params are supplied, do a POST method
     */
    private KMpage fetchPage(String urlStr, String cookie, String urlParams) throws KMexception {
        URL url;
        InputStream stream = null;
        int code = -1;
        String location = null;
        String body = null;
        KMpage res = null;
        Log.d(TAG, "Fetch page " + urlStr);
        String action = (urlParams == null) ? "GET" : "POST";

        try {
            // get URL content
            url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod(action);
            if (cookie != null) {
                conn.addRequestProperty("Cookie", cookie);
            }
            if (urlParams != null) {
                byte[] postData = urlParams.getBytes(StandardCharsets.UTF_8);
                int postDataLength = postData.length;
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("charset", "utf-8");
                conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                conn.setUseCaches(false);
                conn.setUseCaches(false);
                conn.setDoOutput(true);
                try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream())) {
                    writer.write(urlParams);
                    writer.flush();
                }
            }
            conn.connect();
            code = conn.getResponseCode();
            if (code >= 500)
                throw new KMUnavailableException("Server unavailable: " + code + " " + conn.getResponseMessage());

            stream = conn.getInputStream();
            cookie = this.getCookie("clientsession", conn);
            location = this.getHeader("Location", conn);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Invalid URL: '" + urlStr + "'", e);
            throw new KMexception("Invalid URL '" + urlStr + "'", e);
        } catch (IOException e) {
            Log.e(TAG, "Network error fetching '" + urlStr + "'", e);
            throw new KMexception("IO Exception", e);
        }

        if (stream != null) {
            try {
                body = stream2Str(stream);
                res = new KMpage(urlStr, code, cookie, location, body);
            } catch (IOException e) {
                throw new KMexception("IO Exception", e);
            }
        }

        return res;
    }

    /*
     *  Extract the content of an InputStream as a string
     */
    private String stream2Str(InputStream stream) throws IOException {
        StringBuilder str = new StringBuilder();
        byte[] bytes = new byte[1000];
        int numRead = 0;

        while ((numRead = stream.read(bytes)) >= 0) {
            str.append(new String(bytes, 0, numRead));
        }
        return str.toString();
    }

    private String getCookie(String name, HttpURLConnection conn) {
        String res = null;

        Map<String, List<String>> headerFields = conn.getHeaderFields();
        Set<String> headerFieldsSet = headerFields.keySet();

        for (String headerFieldKey : headerFieldsSet)
            if ("Set-Cookie".equalsIgnoreCase(headerFieldKey)) {
                List<String> headerFieldValue = headerFields.get(headerFieldKey);
                res = headerFieldValue.get(0);
            }

        return res;
    }

    private String getHeader(String name, HttpURLConnection conn) {
        String res = null;

        Map<String, List<String>> headerFields = conn.getHeaderFields();
        Set<String> headerFieldsSet = headerFields.keySet();

        for (String headerFieldKey : headerFieldsSet)
            if (name.equalsIgnoreCase(headerFieldKey)) {
                List<String> headerFieldValue = headerFields.get(headerFieldKey);
                res = headerFieldValue.get(0);
            }

        return res;
    }

    @Override
    public boolean isValid() {
        return m_Valid;
    }

    @Override
    public String getUser() {
        return m_Userid;
    }

    @Override
    public Date getTS() {
        return m_Summary.getTs();
    }

    @Override
    public String getName() throws KMexception {
        return m_Summary.getName();
    }

    @Override
    public String getNumber() throws KMexception {
        return m_Summary.getNumber();
    }

    @Override
    public String getPlan() throws KMexception {
        return m_Summary.getPlan();
    }

    @Override
    public int getData_days() throws KMexception {
        final Date now = new Date();
        final long days = (m_Summary.getDataRenews().getTime() - (new Date()).getTime()) / 86400000 + 1;
        return (int) days;
    }

    @Override
    public Date getData_renews() throws KMexception {
        return m_Summary.getDataRenews();
    }

    @Override
    public int getDataDaysRemaining() {
        final Date today = new Date();
        final Date expires = m_Summary.getDataRenews();
        final long days = (expires.getTime() - today.getTime()) / 86400000 + 1;
        return (int) days;
    }

    @Override
    public int getPlanDaysRemaining() throws KMexception {
        final Date today = new Date();
        final Date expires = m_Summary.getPlanRenews();
        final long days = (expires.getTime() - today.getTime()) / 86400000 + 1;
        return (int) days;
    }

    @Override
    public int getNational_calls() throws KMexception {
        return m_Summary.getCalls();
    }

    @Override
    public int getNational_texts() throws KMexception {
        return m_Summary.getSms();
    }

    @Override
    public long getNational_data() throws KMexception {
        return m_Summary.getData();
    }

    @Override
    public int getQuota() throws KMexception {
        return m_Summary.getQuota();
    }

    @Override
    public int getData_percent() throws KMexception {
        return m_Summary.getDataPC();
    }

    @Override
    public boolean isAuto_recharge() throws KMexception {
        return m_Summary.isAutoRecharge();
    }

    @Override
    public Date getPlan_expires() throws KMexception {
        return m_Summary.getPlanRenews();
    }

    @Override
    public List<History> getHistoricalDataUsage() {
        return historicalUsageList;
    }

    @Override
    public List<Usage> getUsage() {
        return allUsageList;
    }

    @Override
    public List<Usage> getDataUsage() {
        return dataUsageList;
    }

    @Override
    public List<Usage> getCallUsage() {
        return callUsageList;
    }

    @Override
    public List<Usage> getTextUsage() {
        return textUsageList;
    }

    @Override
    public Date getMostRecentDataUsage() {
        return mMostRecentDataUsage;
    }

    @Override
    public String toString() {
        String buff = "Name: '" + m_Summary.getName() + "'\n" +
                "Number: " + m_Summary.getNumber() + "\n" +
                "Plan: " + m_Summary.getPlan() + "\n" +
                "Data used: " + scaledNumber(national_data) + " of " + m_Summary.getQuota() + "MB (" + m_Summary.getDataPC() + "%)\n" +
                "Day    s used: " + (30 - getDataDaysRemaining()) + " of 30 days (" + percent(30 - getDataDaysRemaining(), 30) + "%)\n" +
                "Data renews: " + m_Summary.getDataRenews() + "\n" +
                "Last data used: " + getMostRecentDataUsage() + "\n" +
                "Expires: " + m_Summary.getPlanRenews() + "\n" +
                "\n" +
                "Records: Calls " + m_Summary.getCalls() +
                " SMS's " + m_Summary.getSms() +
                " Data " + dataUsageList.size() + "\n";

        return buff;
    }

    private String scaledNumber(long num) {
        double number = (double) num;
        double scale = 1.0;
        String scaleStr = "";

        if (num > 1024) {
            scale = 1024.0;
            scaleStr = "KB";
        }
        if (num > 1024 * 1024) {
            scale = 1024.0 * 1024.0;
            scaleStr = "MB";
        }
        if (num > 1024 * 1024 * 1024.0) {
            scale = 1024.0 * 1024.0 * 1024.0;
            scaleStr = "GB";
        }
        DecimalFormat fmt = new DecimalFormat("#.##");
        double round = Double.valueOf(fmt.format(number / scale));
        String result = round + scaleStr;
        return result;
    }

    private int percent(int x, int y) {
        double pc = (double) x / (double) y * 100.0d;
        return (int) pc;
    }
}
