package com.teddyteh.kmusage.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.gigamole.library.ArcProgressStackView;
import com.teddyteh.kmscraper.KMadapter;
import com.teddyteh.kmscraper.adapter.KMexception;
import com.teddyteh.kmusage.ListItemDataModel;
import com.teddyteh.kmusage.MainFragmentListView;
import com.teddyteh.kmusage.R;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class MainFragment extends Fragment implements MainView {
    public static final String TAG = "MainActivity";
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    ListView mListView;
    ArrayList<ListItemDataModel> list;
    private KMadapter adapter;
    private ArcProgressStackView mArcProgressStackView;

    public MainFragment() {

    }

    /**
     * Returns a new instance of this fragment
     */
    public static MainFragment newInstance(KMadapter adapter) {
        MainFragment fragment = new MainFragment();
        fragment.adapter = adapter;
//        presenter = new MainFragmentPresenter((MainView)this, adapter);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mArcProgressStackView = (ArcProgressStackView) rootView.findViewById(R.id.apsv);
        mListView = (ListView) rootView.findViewById(R.id.listView);
        list = new ArrayList<>();
        drawView();

        return rootView;
    }

    /*
     *  Render the view
     */
    public void drawView() {
        String dataStartDate = null, dataRenewalDate = null, dataUsed = null, quota = null;
        int dataDaysPercentage = 0, dataPercentage = 0;

        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(adapter.getData_renews());
            cal.add(Calendar.DATE, -30);
            Date dataStart = cal.getTime();
            dataStartDate = new SimpleDateFormat("d MMM yy").format(dataStart);
            dataRenewalDate = new SimpleDateFormat("d MMM yy").format(adapter.getData_renews());
            dataDaysPercentage = percent(30 - adapter.getDataDaysRemaining(), 30);
            dataUsed = scaledNumber(adapter.getNational_data());
            quota = Integer.toString(adapter.getQuota()) + "MB";
            dataPercentage = adapter.getData_percent();
        } catch (KMexception ex) {

        }

        MainFragmentListView adapter2 = new MainFragmentListView(getActivity(), dataStartDate, dataRenewalDate, dataUsed, quota, dataDaysPercentage, dataPercentage, list);
        mListView.setAdapter(adapter2);
        drawProgressBar(dataDaysPercentage, dataPercentage);
    }

    /*
     *  Draw circular progress bar
     */
    private void drawProgressBar(int dataDaysPercentage, int dataPercentage) {
        final ArrayList<ArcProgressStackView.Model> models = new ArrayList<>();
        models.add(new ArcProgressStackView.Model("Data used", dataPercentage, getColorFromTheme(R.attr.colorDarkGrey), getColorFromTheme(R.attr.colorDataUsed)));
        models.add(new ArcProgressStackView.Model("Days left", dataDaysPercentage, getColorFromTheme(R.attr.colorLightGrey), getColorFromTheme(R.attr.colorDaysLeft)));
        mArcProgressStackView.setModels(models);
    }

    /*
     *  Get a colour from the current theme
     */
    public int getColorFromTheme(int attr) {
        TypedValue a = new TypedValue();
        getActivity().getTheme().resolveAttribute(attr, a, true);

        return a.data;
    }

    /*
     *  Scale a raw number into KB/MB/GB
     */
    private String scaledNumber(long num) {
        double number = (double) num;
        double scale = 1.0;
        String scaleStr = "";

        if (num > 1024) {
            scale = 1024.0;
            scaleStr = "KB";
        }
        if (num > 1024 * 1024) {
            scale = 1024.0 * 1024.0;
            scaleStr = "MB";
        }
        if (num > 1024 * 1024 * 1024.0) {
            scale = 1024.0 * 1024.0 * 1024.0;
            scaleStr = "GB";
        }
        DecimalFormat fmt = new DecimalFormat("#.##");
        //int round = (int)Math.floor((number / scale) + 0.5d);
        double round = Double.valueOf(fmt.format(num / scale));
        return round + scaleStr;
    }

    /*
     *  Take two integers and return an int giving the percent
     */
    private int percent(int x, int y) {
        double pc = (double) x / (double) y * 100.0d;
        return (int) pc;
    }
}