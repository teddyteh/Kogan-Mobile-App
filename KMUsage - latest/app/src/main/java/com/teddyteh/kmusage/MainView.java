package com.teddyteh.kmusage;

import android.app.Activity;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by teddy on 13/5/2017.
 */

public interface MainView {
    void fillSpinner(ArrayAdapter<String> accounts, AdapterView.OnItemSelectedListener listener);

    void graphDraw(LineGraphSeries<DataPoint> historySeries, LineGraphSeries<DataPoint> projectedSeries, Date firstDay, Date lastDay, long quota);

    void fillList(ArrayList<MainPresenterImpl.ListItem> listItems);

    void setLoading(boolean loading);

    void setRefreshing(boolean refreshing);

    Activity getActivity();
}
