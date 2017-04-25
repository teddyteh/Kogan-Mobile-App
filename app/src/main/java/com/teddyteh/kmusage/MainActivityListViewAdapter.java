package com.teddyteh.kmusage;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.daimajia.numberprogressbar.NumberProgressBar;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by matthew on 1/04/2017.
 */

public class MainActivityListViewAdapter extends BaseAdapter {
    Activity activity;
    ImageView icon;
    TextView tvName;
    TextView tvValue;
    NumberProgressBar progress;
    ArrayList<ListItemDataModel> list;

    public MainActivityListViewAdapter(Activity activity, ArrayList<ListItemDataModel> list) {
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
            convertView = inflater.inflate(R.layout.listview_custom_layout, null);

            RelativeLayout layout = (RelativeLayout) convertView.findViewById(R.id.content_layout);

            ListItemDataModel item = list.get(position);
            ArrayList<String> values = item.values;

//            for (String value : values)
//            {
//                TextView mTextView = new TextView(activity.getApplicationContext());
//                mTextView.setText(value);
//                RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
//                        ViewGroup.LayoutParams.WRAP_CONTENT);
//
//                p.addRule(RelativeLayout.BELOW, R.id.item_progress);
//                p.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 1);
//                mTextView.setLayoutParams(p);
//                layout.addView(mTextView);
//            }

            icon = (ImageView) convertView.findViewById(R.id.icon);
            icon.setColorFilter(ContextCompat.getColor(activity.getApplicationContext(), R.color.secondary_text));
            tvName = (TextView) convertView.findViewById(R.id.item_start);
            tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            tvValue = (TextView) convertView.findViewById(R.id.item_end);
            tvValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            progress = (NumberProgressBar) convertView.findViewById(R.id.number_progress_bar);

            icon.setImageDrawable(item.icon);
            tvName.setText(values.get(0));
            tvValue.setText(values.get(1));
            progress.setProgress(Integer.parseInt(values.get(2)));
            progress.setReachedBarColor(item.color);
        }

//        tvName.setText(map.get("NAME"));
//        tvValue.setText(map.get("VALUE"));
        return convertView;
    }
}
