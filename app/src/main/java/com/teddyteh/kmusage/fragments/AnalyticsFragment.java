package com.teddyteh.kmusage.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.teddyteh.kmscraper.KMadapter;
import com.teddyteh.kmscraper.adapter.KMexception;
import com.teddyteh.kmscraper.model.Usage;
import com.teddyteh.kmusage.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class AnalyticsFragment extends Fragment {
    private final static String TAG = AnalyticsFragment.class.getSimpleName();
    private KMadapter adapter;

    public AnalyticsFragment() {
    }

    /**
     * Returns a new instance of this fragment
     */
    public static AnalyticsFragment newInstance(KMadapter adapter) {
        AnalyticsFragment fragment = new AnalyticsFragment();
        fragment.adapter = adapter;

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_analytics, container, false);

        List<Usage> dataUsageList = adapter.getDataUsage();
        GraphView graph = (GraphView) rootView.findViewById(R.id.graph);
        BarGraphSeries<DataPoint> series = new BarGraphSeries<>(makeDataPoints());
        graph.addSeries(series);
        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(getActivity(), new SimpleDateFormat("d-MMM", Locale.getDefault())));

        Date firstDate = getFirstDate(dataUsageList);
        Calendar cal = Calendar.getInstance();
        cal.setTime(firstDate);
        int startDay = cal.get(Calendar.DAY_OF_MONTH);

        Date lastDate = getLastDate(dataUsageList);
        cal.setTime(lastDate);
        int lastDay = cal.get(Calendar.DAY_OF_MONTH);

        graph.getViewport().setMinX(firstDate.getTime());
//        graph.getViewport().setMaxX(lastDate.getTime());
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setXAxisBoundsManual(true);

//        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
//            SimpleDateFormat df = new SimpleDateFormat("d-MMM", Locale.getDefault());
//            @Override
//            public String formatLabel(double value, boolean isValueX) {
//                if (isValueX) {
//                    Date date = new Date((int)value * 1000);
//                    return df.format(date);
//
//                    //return super.formatLabel(value, isValueX);
//                }
//                return "test";
//            }
//        });

        series.setSpacing(0);
        graph.getGridLabelRenderer().setHumanRounding(false);

        return rootView;
    }

    public DataPoint[] makeDataPoints() {
        final int DURATION = 30;

        // Extract the usage info from the adapter
        List<Usage> dataUsageList = null;
        Date renews = null;
        try {
            dataUsageList = adapter.getDataUsage();
            renews = adapter.getData_renews();
        } catch (KMexception ex) {
            Log.e(TAG, "Error accessing adapter", ex);
            throw new RuntimeException("Error accessing adapter", ex);
        }

        // Compute start of period date (renews - 30 days)
        Calendar cal = Calendar.getInstance();
        cal.setTime(renews);
        cal.add(Calendar.DATE, -DURATION);
        Date startOfPeriod = new Date(cal.getTimeInMillis());


        Map<Date, Long> map = getUsage(dataUsageList, startOfPeriod);
        DataPoint[] points = new DataPoint[DURATION];

        int i = 0;
        for (Map.Entry<Date, Long> entry : map.entrySet()) {
            Date date = entry.getKey();

            Long usage = entry.getValue();

            points[i++] = new DataPoint(date, bytesToMegabytes(usage));
        }

        return points;
    }

    public Map<Date, Long> getUsage(List<Usage> dataUsageList, Date startOfPeriod) {
        HashMap<Date, Long> map = new HashMap<>();
        Date firstDate = getFirstDate(dataUsageList);
        Date lastDate = getLastDate(dataUsageList);

        Calendar cal = Calendar.getInstance();
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

            if (!map.containsKey(day)) {
                throw new RuntimeException("Usage date outside billing period. TS=" + u.getTs() + " period start=" + startOfPeriod);
            }
            Long total = map.get(day);
            total += usage;
            map.put(day, total);
        }

        if (map.size() != 30)
            throw new RuntimeException("Usage map contains other than 30 days (size=" + map.size() + ")");

        SortedMap<Date, Long> sortedMap = new TreeMap<>(map);

        return sortedMap;
    }

    private Date getLastDate(List<Usage> dataUsageList) {
        Date date = null;
        for (Usage u : dataUsageList) {
            if (date == null || u.getTs().after(date))
                date = u.getTs();
        }
        return makeDay(date);
    }

    private Date getFirstDate(List<Usage> dataUsageList) {
        Date date = null;
        for (Usage u : dataUsageList) {
            if (date == null || u.getTs().before(date))
                date = u.getTs();
        }
        return makeDay(date);
    }

    private Date makeDay(Date ts) {
        if (ts == null) return null;
        ts.setHours(0);
        ts.setMinutes(0);
        ts.setSeconds(0);

        return ts;
    }

    public long bytesToMegabytes(long bytes) {
        long MEGABYTE = 1024L * 1024L;
        long b = bytes / MEGABYTE;

        return b;
    }
}
