package com.teddyteh.kmusage;

import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.daimajia.numberprogressbar.NumberProgressBar;

import java.util.ArrayList;

/**
 * Created by teddy on 1/04/2017.
 */

public class MainFragmentListView extends BaseAdapter {
    Activity activity;
    ImageView icon;
    TextView tvStartDate;
    TextView tvEndDate;
    NumberProgressBar progress;
    ArrayList<ListItemDataModel> list;

    public MainFragmentListView(Activity activity, ArrayList<ListItemDataModel> list) {
        super();
        this.activity = activity;
        this.list = list;
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
