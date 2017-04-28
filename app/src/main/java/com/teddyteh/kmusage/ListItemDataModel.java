package com.teddyteh.kmusage;

import android.graphics.drawable.Drawable;

import java.util.ArrayList;

/**
 * Created by teddy on 22/4/2017.
 */

public class ListItemDataModel {
    public String key;
    public String startDate;
    public String endDate;
    public int percentage;
    public Drawable icon;
    public int color;

    public ListItemDataModel(String key, String startDate, String endDate, int percentage, Drawable icon, int color) {
        this.key = key;
        this.startDate = startDate;
        this.endDate = endDate;
        this.percentage = percentage;
        this.icon = icon;
        this.color = color;
    }
}
