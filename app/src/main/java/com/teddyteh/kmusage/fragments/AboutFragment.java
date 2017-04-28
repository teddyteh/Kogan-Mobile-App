package com.teddyteh.kmusage.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.teddyteh.kmusage.MainActivity;
import com.teddyteh.kmusage.R;

public class AboutFragment extends Fragment {
    public AboutFragment() {
    }

    /**
     * Returns a new instance of this fragment
     */
    public static AboutFragment newInstance(int sectionNumber) {
        AboutFragment fragment = new AboutFragment();

        return fragment;
    }

    public static AboutFragment newInstance() {
        AboutFragment fragment = new AboutFragment();

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_about, container, false);

        return rootView;
    }
}