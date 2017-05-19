package com.teddyteh.kmusage;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.teddyteh.kmscraper.KMadapter;
import com.teddyteh.kmscraper.adapter.KMexception;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by teddy on 1/04/2017.
 */

public class MainFragmentListView extends BaseAdapter {
    Activity activity;
    String dataStartDate, dataRenewalDate, dataUsed, quota;
    int dataDaysPercentage, dataPercentage;
    ImageView icon;
    TextView tvStartDate, tvEndDate;
    NumberProgressBar progress;
    ArrayList<ListItemDataModel> list;

    public MainFragmentListView(Activity activity, String dataStartDate, String dataRenewalDate, String dataUsed, String quota, int dataDaysPercentage, int dataPercentage, ArrayList<ListItemDataModel> list) {
        super();
        this.activity = activity;
        this.dataStartDate = dataStartDate;
        this.dataRenewalDate = dataRenewalDate;
        this.dataUsed = dataUsed;
        this.quota = quota;
        this.dataDaysPercentage = dataDaysPercentage;
        this.dataPercentage = dataPercentage;
        this.list = list;
        populateListView();
    }

    /*
     *  Extract info from the result record and populate the ListView
     */
    private void populateListView() {
        list.clear();
        list.add(addItemToList("Days left", dataStartDate, dataRenewalDate, dataDaysPercentage, ContextCompat.getDrawable(activity.getApplicationContext(), R.drawable.calendar_48px), getColorFromTheme(R.attr.colorDaysLeft)));
        list.add(addItemToList("Data used", dataUsed, quota, dataPercentage, ContextCompat.getDrawable(activity.getApplicationContext(), R.drawable.data_xfer_48px), getColorFromTheme(R.attr.colorDataUsed)));
    }

    /*
     *  Creates a ListItemDataModel object which holds information about a list item
     */
    private ListItemDataModel addItemToList(String key, String startDate, String endDate, int percentage, Drawable icon, int color) {
        return new ListItemDataModel(key, startDate, endDate, percentage, icon, color);
    }

    /*
     *  Get a colour from the current theme
     */
    public int getColorFromTheme(int attr) {
        TypedValue a = new TypedValue();
        activity.getTheme().resolveAttribute(attr, a, true);

        return a.data;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = activity.getLayoutInflater();

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.fragment_main_listview_layout, null);

            ListItemDataModel item = list.get(position);

            icon = (ImageView) convertView.findViewById(R.id.icon);
            icon.setColorFilter(ContextCompat.getColor(activity.getApplicationContext(), R.color.secondary_text));
            tvStartDate = (TextView) convertView.findViewById(R.id.item_start);
            tvStartDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            tvEndDate = (TextView) convertView.findViewById(R.id.item_end);
            tvEndDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            progress = (NumberProgressBar) convertView.findViewById(R.id.number_progress_bar);

            icon.setImageDrawable(item.icon);
            tvStartDate.setText(item.startDate);
            tvEndDate.setText(item.endDate);
            progress.setProgress(item.percentage);
            progress.setReachedBarColor(item.color);
        }

        return convertView;
    }
}
