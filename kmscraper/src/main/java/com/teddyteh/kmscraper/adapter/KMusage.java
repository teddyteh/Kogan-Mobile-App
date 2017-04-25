package com.teddyteh.kmscraper.adapter;

import android.util.Log;

import com.teddyteh.kmscraper.model.Usage;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by matthew on 21/03/2017.
 */

class KMusage {

    public static final String TAG = "KMusage";
    private List<Usage> entries = new ArrayList<Usage>();
    private int mVoiceCnt = 0;
    private int mSmsCnt = 0;
    private int mDataCnt = 0;
    private long mDataTotal = 0;
    private double mCostTotal = 0.0;
    private Date mPeriodStart;
    private String mUser;

    KMusage(Document usageDoc, Date dataRenew, String user) throws KMexception {
        mPeriodStart = periodStart(dataRenew);
        mUser = user;
        extractUsageTable(usageDoc);
        for (Usage usage : entries) {
            if (usage.getType().equals("V")) {
                mVoiceCnt++;
                mCostTotal += usage.getCost();
            }
            if (usage.getType().equals("T")) {
                mSmsCnt++;
                mCostTotal += usage.getCost();
            }
            if (usage.getType().equals("D")) {
                mDataCnt++;
                mDataTotal += usage.getVolume();
            }
        }
    }

    private Date periodStart(Date dataRenew) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(dataRenew);
        cal.add(Calendar.DATE, -29);
        Date startDate = cal.getTime();
        return startDate;
    }

    private void extractUsageTable(Document usageDoc) throws KMDocumentException {
        Elements table = usageDoc.select("table[id=service-usage-history]");
        Elements body = table.select("tbody");
        Elements rows = body.select("tr");
        for (Element row : rows) {
            Elements entry = row.select("td");
            if (entry.size() != 6)
                throw new KMDocumentException("Malformed usage table entry", usageDoc);

            // Build a usage entry from HTML
            Usage usage = null;
            String type = getType(entry);
            if (type.equals("V")) usage = buildVoice(entry, mUser);
            if (type.equals("T")) usage = buildText(entry, mUser);
            if (type.equals("D")) usage = buildData(entry, mUser);
            if (usage == null) {
                Log.e(TAG, "Usage parse error. Entry = '" + entry + "'");
                throw new KMDocumentException("Usage parse error", usageDoc);
            }

            // Only add to list if entry date >= last renew date
            if (usage.getTs().compareTo(mPeriodStart) >= 0) {
                //Log.d(TAG, usage.toString());
                this.entries.add(usage);
            }
        }
    }

    /*
     *  Extract a voice usage entry from HTML
     */
    private Usage buildVoice(Elements entry, String user) throws KMDocumentException {
        Date ts = extractTS(entry);
        Usage usage = new Usage(user, ts);
        usage.setType("V");
        String costStr = entry.get(5).text();
        usage.setCallType(entry.get(2).text());
        usage.setNumber(entry.get(3).text());
        usage.setCost(Double.parseDouble(costStr.substring(1)));
        return usage;
    }

    private Usage buildText(Elements entry, String user) throws KMDocumentException {
        Date ts = extractTS(entry);
        Usage usage = new Usage(user, ts);
        usage.setType("T");
        String costStr = entry.get(5).text();
        usage.setCallType(entry.get(2).text());
        usage.setNumber(entry.get(3).text());
        usage.setCost(Double.parseDouble(costStr.substring(1)));
        return usage;
    }

    private Usage buildData(Elements entry, String user) throws KMDocumentException {
        Date ts = extractTS(entry);
        Usage usage = new Usage(user, ts);
        usage.setType("D");
        usage.setVolume(extractVolume(entry));
        return usage;
    }

    private Date extractTS(Elements entry) throws KMDocumentException {
        String dateStr = entry.get(0).text() + " " + entry.get(1).text();
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
        try {
            Date ts = df.parse(dateStr);
            return ts;
        } catch (ParseException ex) {
            throw new KMDocumentException("Usage entry invalid timestamp '" + dateStr + "'");
        }
    }

    /*
     *  Extract the data volume from an HTML entry.  These entries can be in multiple formats,
     *  as either bytes or megabytes.  Examples are:
     *      "132096 B"
     *      "2.12 MB"
     *      "590848 B"
     *  Although KB is not currently used, it is supported.
     */
    private long extractVolume(Elements entry) throws KMDocumentException {

        String destStr = entry.get(3).text();
        if (!destStr.toUpperCase().equals("INTERNET"))
            throw new KMDocumentException("Invalid number field for DATA entry: '" + destStr + "'");

        String valueStr = entry.get(4).text();
        int pos = valueStr.indexOf(' ');
        if (pos < 0)
            throw new KMDocumentException("Invalid data volume for DATA entry: '" + valueStr + "'");
        String numberStr = valueStr.substring(0, pos);
        String scaleStr = valueStr.substring(pos + 1);

        Double number = Double.parseDouble(numberStr) * extractScale(scaleStr);
        return number.longValue();
    }

    /*
     * Convert a scale string such as 'KB' or 'MB' to a scale number
     */
    private Double extractScale(String scaleStr) {
        Double res = 1.0;

        scaleStr = scaleStr.toUpperCase();
        if (scaleStr.equals("KB"))
            res = 1024.0;
        if (scaleStr.equals("MB"))
            res = 1024.0 * 1024.0;
        if (scaleStr.equals("GB"))
            res = 1024.0 * 1024.0 * 1024.0;

        return res;
    }

    private String getType(Elements entry) {
        String type;
        String typeStr = entry.get(2).text().toUpperCase();

        // Lots of unknown typeStr, so default to voice
        type = "V";

        if (typeStr.equals("DATA"))
            type = "D";
        if (typeStr.equals("SMS"))
            type = "T";
        if (typeStr.equals("MMS"))
            type = "T";
        if (typeStr.equals("1800"))
            type = "V";
        if (typeStr.equals("1300"))
            type = "V";
        if (typeStr.equals("MVNO2MVNO"))
            type = "V";
        if (typeStr.equals("NATIONAL MOBILE"))
            type = "V";
        if (typeStr.equals("CALLFORWARD TO VOICEMAIL"))
            type = "V";
        if (typeStr.equals("VOICEMAIL RETRIEVAL"))
            type = "V";
        if (typeStr.startsWith("AUSTRALIA"))
            type = "V";

        return type;
    }

    private String getLocaleDate(Date date) {
        DateFormat formatter = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());
        return formatter.format(date);
    }

    public String getDataTotalStr() {
        double val = (double) mDataTotal;
        double scale = 1.0;
        String scaleStr = "";
        if (val > (1024.0 * 1024.0 * 1024.0)) {
            scale = 1024.0 * 1024.0 * 1024.0;
            scaleStr = "GB";
        } else if (val > (1024.0 * 1024.0)) {
            scale = 1024.0 * 1024.0;
            scaleStr = "MB";
        } else if (val > 1024.0) {
            scale = 1024.0;
            scaleStr = "KB";
        }

        val = val / scale;
        return String.format("%.2f %s", val, scaleStr);
    }

    public List<Usage> getEntries() {
        return entries;
    }

    public String toString() {
        String buff = String.format("Billing period starting %s:\n", getLocaleDate(mPeriodStart)) +
                String.format("Voice calls:\t%d\n", getVoiceCnt()) +
                String.format("SMS's sent:\t%d\n", getSmsCnt()) +
                String.format("Data entries:\t%d\n", getDataCnt()) +
                String.format("Total data:\t%s\n", getDataTotalStr()) +
                String.format("Total charges:\t$%.2f\n", getCostTotal());

        return buff;
    }

    public int getVoiceCnt() {
        return mVoiceCnt;
    }

    public int getSmsCnt() {
        return mSmsCnt;
    }

    public int getDataCnt() {
        return mDataCnt;
    }

    public long getDataTotal() {
        return mDataTotal;
    }

    public double getCostTotal() {
        return mCostTotal;
    }

    public Date getPeriodStart() {
        return mPeriodStart;
    }
}
