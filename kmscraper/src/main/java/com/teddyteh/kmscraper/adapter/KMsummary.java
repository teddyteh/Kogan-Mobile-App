package com.teddyteh.kmscraper.adapter;

import android.support.annotation.NonNull;
import android.util.Log;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/*
 * Parse through the KM summary page extracting the key info about the account.  This includes
 * 	Account Name
 * 	Mobile Number
 * 	National Call limit (or unlimited = -1)
 * 	National Text limit (or unlimited = -1)
 * 	National Data limit (in MB)
 *  Plan expiry data
 *  Data renewal date
 *  Whether auto recharge is enabled
 *
 * All of the fields are extracted when the object is constructed.  Any errors will result in a KMDocumentException
 */
class KMsummary {

    public static final String TAG = "KMsummary";
    private String name;
    private String number;
    private String plan;
    private Integer data_days;
    private Integer national_calls = null;
    private Integer national_texts = null;
    private Double quota = null;
    private Integer data_percent;
    private boolean auto_recharge;
    private Date plan_expires;
    private Date data_renews;

    KMsummary(Document summary) throws KMexception {
        extractSmallItems(summary);
        data_days = 30;
        name = extractName(summary);
        number = extractMobile(summary);
        plan = extractPlan(summary);
        plan_expires = extractPlanExpires(summary);
        data_renews = extractDataRenews(summary);
        data_percent = extractDataPercent(summary);
    }

    private String extractName(Document summary) throws KMDocumentException {
        Elements custHeader = summary.select("h4.portlet__customer").select("span.portlet__name");
        if (custHeader.size() == 0)
            throw new KMDocumentException("Name not found", summary);
        String text = custHeader.text();
        if (text.endsWith(" •"))
            text = text.substring(0, text.length() - 2);
        return text;
    }

    private String extractMobile(Document summary) throws KMDocumentException {
        Elements custHeader = summary.select("h4.portlet__customer").select("span.portlet__mobile");
        if (custHeader.size() == 0)
            throw new KMDocumentException("Mobile not found", summary);
        return custHeader.text();
    }

    private String extractPlan(Document summary) throws KMDocumentException {
        Element plan = summary.select("div.col-md-6").select("div.portlet-title").select("h3").first();
        if (plan == null)
            throw new KMDocumentException("Plan not found", summary);
        String text = plan.text();
        return text;
    }

    /*
     * Extract a number fields all of which are contained within <small></small> tags.  These are:
     * 	National Calls
     * 	National Text
     * 	National Data
     *  Auto Recharge:
     */
    private void extractSmallItems(Document summary) throws KMDocumentException {
        Element smallList = summary.select("div.col-sm-6").first();
        if (smallList == null)
            throw new KMDocumentException("Small list not found", summary);
        Elements items = smallList.select("p");

        for (Element e : items) {
            String name = e.select("small").text().toUpperCase();
            String text = e.html();
            int pos = text.indexOf("<br>");
            if (pos < 0)
                throw new KMDocumentException("Small list <br> parse error", summary);
            String val = text.substring(pos + 4).toUpperCase();

            // National Calls
            if (name.equals("NATIONAL CALLS")) {
                this.national_calls = numOrUnlimited(val);
                continue;
            }

            // National Text
            if (name.equals("NATIONAL TEXT")) {
                this.national_texts = numOrUnlimited(val);
                continue;
            }

            // National Data
            if (name.equals("NATIONAL DATA")) {
                if (val.endsWith("MB")) {
                    val = val.substring(0, val.length() - 2);
                    this.quota = Double.parseDouble(val);
                } else
                    throw new KMDocumentException("National data should end with MB: '" + val + "'", summary);
                continue;
            }

            // Auto Recharge:
            if (name.equals("AUTO RECHARGE:")) {
                boolean flag = val.equals("ENABLED");
                if (val.equals("DISABLED"))
                    flag = false;
                if (!val.equals("ENABLED") && !val.equals("DISABLED"))
                    throw new KMDocumentException("Invalid auto recharge flag '" + val + "'", summary);
                this.auto_recharge = flag;
            }
        }
    }

    @NonNull
    private Integer numOrUnlimited(String val) throws KMDocumentException {
        if (val.equals("UNLIMITED")) return -1;
        String msg = "Unknown small value '" + val + "'";
        Log.e(TAG, msg);
        throw new KMDocumentException(msg);
    }

    private Date extractPlanExpires(Document summary) throws KMDocumentException {
        final String HEADER = "Plan Expires on ";
        Elements smallList = summary.select("small");
        if (smallList.size() == 0)
            throw new KMDocumentException("Plan expires not found", summary);
        for (Element e : smallList) {
            String text = e.text();
            if (text.startsWith(HEADER)) {
                String dateStr = text.substring(HEADER.length());
                DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
                try {
                    return df.parse(dateStr);
                } catch (ParseException ex) {
                    Log.e(TAG, "Plan expires date parse failure: '" + dateStr + "'");
                    throw new KMDocumentException("Plan expires date - invalid format", summary);
                }
            }
        }
        Log.e(TAG, "Plan expires not found");
        throw new KMDocumentException("Plan expires not found", summary);
    }

    private Date extractDataRenews(Document summary) throws KMDocumentException {
        final String[] HEADERS = {"Data renews on the midnight of ", "Data expires on "};
        Elements smallList = summary.select("small");
        if (smallList.size() == 0)
            throw new KMDocumentException("Data renews not found", summary);
        for (Element e : smallList) {
            String text = e.text();
            for (String HEADER : HEADERS) {
                if (text.startsWith(HEADER)) {
                    String dateStr = text.substring(HEADER.length());
                    DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
                    try {
                        return df.parse(dateStr);
                    } catch (ParseException ex) {
                        Log.e(TAG, "Data renews date parse failure: '" + dateStr + "'");
                        throw new KMDocumentException("Data renews date - invalid format", summary);
                    }
                }
            }
        }
        Log.e(TAG, "Data renews not found");
        throw new KMDocumentException("Data renews not found", summary);
    }

    private Integer extractDataPercent(Document summary) throws KMDocumentException {
        Element percent = summary.select("div.bounce").first();
        if (percent == null)
            throw new KMDocumentException("Data percent not found", summary);
        String val = percent.attr("data-percent");
        if (!val.endsWith("%"))
            throw new KMDocumentException("Data percent does not end with % - '" + val + "'", summary);
        String str = val.substring(0, val.length() - 1);
        return Integer.parseInt(str);
    }

    String getName() {
        return name;
    }

    String getNumber() {
        return number;
    }

    String getPlan() {
        return plan;
    }

    double getData_days() {
        return (double) this.data_days;
    }

    Integer getNational_calls() {
        return national_calls;
    }

    Integer getNational_texts() {
        return national_texts;
    }

    double getQuota() {
        return quota;
    }

    int getData_percent() {
        return data_percent;
    }

    boolean isAuto_recharge() {
        return auto_recharge;
    }

    Date getPlan_expires() {
        return plan_expires;
    }

    Date getData_renews() {
        return data_renews;
    }

    public String toString() {
        String buff = ("Name: '" + this.name + "'\n") +
                "Number: " + this.number + "\n" +
                "Plan: " + this.plan + "\n" +
                "Data allowance: " + this.quota + "MB\n" +
                "Data used: " + this.data_percent + "%\n" +
                "Expires: " + this.plan_expires + "\n" +
                "Data renews: " + this.data_renews + "\n";

        return buff;
    }
}
