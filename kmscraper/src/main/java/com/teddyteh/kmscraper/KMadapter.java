package com.teddyteh.kmscraper;

import com.teddyteh.kmscraper.adapter.KMexception;
import com.teddyteh.kmscraper.model.Usage;

import java.util.Date;
import java.util.List;


public interface KMadapter {
    /* Login succeeded                      */
    public abstract boolean isValid();

    /* Account user name                    */
    public abstract String getUser();

    /* Retrieve account data via cache      */
    public abstract void getCachedData() throws KMexception;

    /* Force refresh account data           */
    public abstract void getData() throws KMexception;

    /* Check whether userid/password are valid  */
    public abstract int testLogin() throws KMexception;

    /*  Time of last retrieval              */
    public abstract Date getTS();

    /* Account name							*/
    public abstract String getName() throws KMexception;

    /* Phone number							*/
    public abstract String getNumber() throws KMexception;

    /* Plan name 							*/
    public abstract String getPlan() throws KMexception;

    /* Duration (days) of data				*/
    public abstract int getData_days() throws KMexception;

    /* Date data renews						*/
    public abstract Date getData_renews() throws KMexception;

    /* No of days before data expires		*/
    public abstract int getDataDaysRemaining() throws KMexception;

    /* No of days before plan expires		*/
    public abstract int getPlanDaysRemaining() throws KMexception;

    /*	Count of domestic calls				*/
    public abstract int getNational_calls() throws KMexception;

    /* Count of domestic SMS				*/
    public abstract int getNational_texts() throws KMexception;

    /* Total data (bytes) used in this billing cycle    */
    public abstract long getNational_data() throws KMexception;

    /* Quota (bytes) of data in this period	*/
    public abstract int getQuota() throws KMexception;

    /* Percentage (from KM) of data used	*/
    public abstract int getData_percent() throws KMexception;

    /* Is auto recharge enabled				*/
    public abstract boolean isAuto_recharge() throws KMexception;

    /* Expiry date for plan					*/
    public abstract Date getPlan_expires() throws KMexception;

    /*	List of all detail records			*/
    public abstract List<Usage> getUsage();

    /*	List of data records				*/
    public abstract List<Usage> getDataUsage();

    /*  List of data records since dawn of time */
    public abstract List<Usage> getHistoricalDataUsage();

    /*	List of call records				*/
    public abstract List<Usage> getCallUsage();

    /*	List of SMS/MMS records				*/
    public abstract List<Usage> getTextUsage();

    /*  Timestamp of last recorded data usage   */
    public abstract Date getMostRecentDataUsage();

    public abstract String toString();

}
