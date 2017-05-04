package com.teddyteh.kmusage.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
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

        drawGraph1(rootView);
        drawGraph2(rootView);

        return rootView;
    }

    public void drawGraph1(View rootView) {
        List<Usage> dataUsageList = adapter.getDataUsage();

        GraphView graph1 = (GraphView) rootView.findViewById(R.id.graph);
        BarGraphSeries<DataPoint> series = new BarGraphSeries<>(makeGraph1DataPoints());
        graph1.addSeries(series);
        graph1.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(getActivity(), new SimpleDateFormat("d-MMM", Locale.getDefault())));

        Date firstDate = getFirstDate(dataUsageList);
        Date lastDate = getLastDate(dataUsageList);

        graph1.getViewport().setMinX(firstDate.getTime());
//        graph1.getViewport().setMaxX(lastDate.getTime());
        graph1.getViewport().setYAxisBoundsManual(true);
        graph1.getViewport().setXAxisBoundsManual(true);

        series.setSpacing(0);
        graph1.getGridLabelRenderer().setHumanRounding(false);
    }

    public void drawGraph2(View rootView) {
        List<Usage> dataUsageList = adapter.getDataUsage();

        GraphView graph2 = (GraphView) rootView.findViewById(R.id.graph2);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(makeGraph2DataPoints());
        LineGraphSeries<DataPoint> series2 = new LineGraphSeries<>(makeGraph2ProjectedDataPoints());
        graph2.addSeries(series);
        graph2.addSeries(series2);
        graph2.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(getActivity(), new SimpleDateFormat("d-MMM", Locale.getDefault())));

        Date firstDate = getFirstDate(dataUsageList);
        Date lastDate = getLastDate(dataUsageList);

        graph2.getViewport().setMinX(firstDate.getTime());
        graph2.getViewport().setMinY(0);
//        graph1.getViewport().setMaxX(lastDate.getTime());
        graph2.getViewport().setYAxisBoundsManual(true);
        graph2.getViewport().setXAxisBoundsManual(true);

        graph2.getGridLabelRenderer().setHumanRounding(false);
    }

    public DataPoint[] makeGraph1DataPoints() {
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

    public DataPoint[] makeGraph2DataPoints() {
        final int DURATION = 30;
        long quota;

        // Extract the usage info from the adapter
        List<Usage> dataUsageList = null;
        Date renews = null;
        try {
            dataUsageList = adapter.getDataUsage();
            quota = ((long) adapter.getQuota()) * 1024L * 1024L;
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

        Date today = new Date();
        long diff = today.getTime() - startOfPeriod.getTime();
        int days = (int) (diff / (1000 * 60 * 60 * 24));

        Map<Date, Long> map = getDailyUsage(dataUsageList, startOfPeriod, days, quota);

        DataPoint[] points = new DataPoint[days];

        int i = 0;
        for (Map.Entry<Date, Long> entry : map.entrySet()) {
            Date date = entry.getKey();

            Long usage = entry.getValue();

            points[i++] = new DataPoint(date, bytesToMegabytes(usage));
        }

        return points;
    }

    private DataPoint[] makeGraph2ProjectedDataPoints() {
        // Extract the usage info from the adapter
        List<Usage> dataUsageList = null;
        Date renews = null;
        long dataLeft;

        try {
            dataUsageList = adapter.getDataUsage();
            renews = adapter.getData_renews();
            dataLeft = (((long) adapter.getQuota()) * 1000000) - adapter.getNational_data();
        } catch (KMexception ex) {
            Log.e(TAG, "Error accessing adapter", ex);
            throw new RuntimeException("Error accessing adapter", ex);
        }

        Date lastDate = renews;
        Date today = new Date();
        long diff = lastDate.getTime() - today.getTime();
        int days = (int) (diff / (1000 * 60 * 60 * 24));

        DataPoint[] points = new DataPoint[days];

        HashMap<Date, Long> map = new HashMap<>();
        Calendar cal = Calendar.getInstance();

        // Create 30 empty entries
        for (int i = 0; i < days; i++) {
            Date day = makeDay(new Date(cal.getTimeInMillis()));
            map.put(day, 0L);
            cal.add(Calendar.DATE, 1);
        }

        SortedMap<Date, Long> sortedMap = new TreeMap<>(map);

        long averageDailyData = dataLeft / days;
        int i = 0;
        for (Map.Entry<Date, Long> entry : sortedMap.entrySet()) {
            Date date = entry.getKey();

            Long usage = dataLeft - (averageDailyData * i);

            points[i++] = new DataPoint(date, bytesToMegabytes(usage));
        }


        return points;
    }


    public Map<Date, Long> getUsage(List<Usage> dataUsageList, Date startOfPeriod) {
        HashMap<Date, Long> map = new HashMap<>();
        Date firstDate = getFirstDate(dataUsageList);
        Date lastDate = getLastDate(dataUsageList);

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

    public Map<Date, Long> getDailyUsage(List<Usage> dataUsageList, Date startOfPeriod, int days, long quota) {
        HashMap<Date, Long> map = new HashMap<>();
        Calendar cal = Calendar.getInstance();
        cal.setTime(startOfPeriod);

        // Create 30 empty entries
        for (int i = 0; i < days; i++) {
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

        if (map.size() != days)
            throw new RuntimeException("Usage map contains other than 30 days (size=" + map.size() + ")");

        SortedMap<Date, Long> sortedMap = new TreeMap<>(map);

        List<Long> list = new ArrayList<Long>(sortedMap.values());
        getAccumulatedDailyUsage(list);

        int index = 0;
        for (Map.Entry<Date, Long> entry : sortedMap.entrySet()) {
            sortedMap.put(entry.getKey(), quota - list.get(index));

            index++;
        }

        return sortedMap;
    }

    public void getAccumulatedDailyUsage(List<Long> list) {
        List<Long> newList = list;

        for (int i = 0; i < list.size(); i++) {
            newList.set(i, calculateAccumulativeUsage(list, i));
        }
    }

    public Long calculateAccumulativeUsage(List<Long> list, int index) {
        Long totalUsage = list.get(index);

        if (index > 0) {
            Long u = list.get(index - 1);

            totalUsage = totalUsage + u;
        }

        return totalUsage;
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
