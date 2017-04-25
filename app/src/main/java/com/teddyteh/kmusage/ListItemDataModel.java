package com.teddyteh.kmusage;

import android.graphics.drawable.Drawable;

import java.util.ArrayList;

/**
 * Created by teddy on 22/4/2017.
 */

public class ListItemDataModel {
    public String key;
    public ArrayList<String> values;
    public Drawable icon;
    public int color;

    public ListItemDataModel(String key, ArrayList<String> values, Drawable icon, int color) {
        this.key = key;
        this.values = values;
        this.icon = icon;
        this.color = color;
    }
}
