package com.teddyteh.kmusage;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.percent.PercentLayoutHelper;
import android.support.percent.PercentRelativeLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements MainView {

    public static final String TAG = "MainActivity";
    private static Context m_Context;

    // GUI references
    MainPresenter presenter;
    private Spinner mSpinner;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ProgressBar mProgress;
    private GraphView mGraph;
    private ListView mListView;

    public static Context getContext() {
        return m_Context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // hide app title from toolbar

        mSpinner = (Spinner) findViewById(R.id.accounts_spinner);
        mProgress = (ProgressBar) findViewById(R.id.progress);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                presenter.forceRefresh();
            }
        });
        mGraph = (GraphView) findViewById(R.id.graph);
        mListView = (ListView) findViewById(R.id.listview);

        // Setup static context which can be referenced in other tasks
        m_Context = getApplicationContext();

        presenter = new MainPresenterImpl(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        presenter.populateSpinner();
        presenter.refresh();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        presenter.onDestroy();
    }

    @Override
    public void fillSpinner(ArrayAdapter<String> accounts, AdapterView.OnItemSelectedListener listener) {
        mSpinner.setAdapter(accounts);
        mSpinner.setOnItemSelectedListener(listener);
    }

    @Override
    public void graphDraw(LineGraphSeries<DataPoint> historySeries, LineGraphSeries<DataPoint> projectedSeries, Date firstDay, Date lastDay, long quota) {
        PercentRelativeLayout container = (PercentRelativeLayout) findViewById(R.id.container);
        GraphView oldGraph = (GraphView) findViewById(R.id.graph);
        container.removeView(oldGraph);

        // Create a new graph
        GraphView graph = new GraphView(this);
        graph.setId(R.id.graph);
        PercentRelativeLayout.LayoutParams params = new PercentRelativeLayout.LayoutParams(PercentRelativeLayout.LayoutParams.MATCH_PARENT, 0);
        params.addRule(RelativeLayout.BELOW, R.id.progress);
        graph.setLayoutParams(params);
        PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();
        info.heightPercent = 0.65f;

        // Add history graph
        historySeries.setColor(Color.LTGRAY);
        historySeries.setBackgroundColor(Color.LTGRAY);
        historySeries.setDrawBackground(true);
        historySeries.setAnimated(true);
        graph.addSeries(historySeries);

        // Add projected graph
        // custom paint to make a dotted line
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        projectedSeries.setCustomPaint(paint);
        projectedSeries.setAnimated(true);
        graph.addSeries(projectedSeries);

        // set date label formatter
        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this, new SimpleDateFormat("d-MMM", Locale.getDefault())));
        graph.getGridLabelRenderer().setNumHorizontalLabels(4); // only 4 because of the space
        graph.getGridLabelRenderer().setNumVerticalLabels(5); // only 4 because of the space

        // Set manual X bounds
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        Date fiveDaysBeforeToday = cal.getTime();
        graph.getViewport().setMinX(fiveDaysBeforeToday.getTime());
//        graph.getViewport().setMaxX(projectedSeries.getHighestValueX());
        graph.getViewport().setXAxisBoundsManual(true);

        // Set manual Y bounds
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(historySeries.getHighestValueY());
        graph.getViewport().setYAxisBoundsManual(true);

        // as we use dates as labels, the human rounding to nice readable numbers is not necessary
        graph.getGridLabelRenderer().setHumanRounding(false);

        // Enable horizontal scrolling
        graph.getViewport().setScrollable(true);

        container.addView(graph);
    }

    @Override
    public void fillList(ArrayList<MainPresenterImpl.ListItem> listItems) {
        ListAdapter adapter = new ListAdapter(this, 0, listItems);
        mListView.setAdapter(adapter);
    }

    @Override
    public void setLoading(boolean loading) {
        if (loading == true)
            mProgress.setVisibility(View.VISIBLE);
        else if (loading == false)
            mProgress.setVisibility(View.GONE);
    }

    @Override
    public void setRefreshing(boolean refreshing) {
        if (refreshing == false) {
            mSwipeRefreshLayout.setRefreshing(false);
        }
        else {
            mSwipeRefreshLayout.setRefreshing(true);
        }
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    private class ListAdapter  extends ArrayAdapter<MainPresenterImpl.ListItem> {

        // declaring our ArrayList of items
        private ArrayList<MainPresenterImpl.ListItem> objects;

        /* here we must override the constructor for ArrayAdapter
        * the only variable we care about now is ArrayList<Item> objects,
        * because it is the list of objects we want to display.
        */
        public ListAdapter (Context context, int textViewResourceId, ArrayList<MainPresenterImpl.ListItem> objects) {
            super(context, textViewResourceId, objects);
            this.objects = objects;
        }

        /*
         * we are overriding the getView method here - this is what defines how each
         * list item will look.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent){

            // assign the view we are converting to a local variable
            View v = convertView;

            // first check to see if the view is null. if so, we have to inflate it.
            // to inflate it basically means to render, or show, the view.
            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.activity_main_listview_layout, null);
            }

        /*
         * Recall that the variable position is sent in as an argument to this method.
         * The variable simply refers to the position of the current object in the list. (The ArrayAdapter
         * iterates through the list we sent it)
         *
         * Therefore, i refers to the current Item object.
         */
            MainPresenterImpl.ListItem i = objects.get(position);

            if (i != null) {

                // This is how you obtain a reference to the TextViews.
                // These TextViews are created in the XML files we defined.

                TextView name = (TextView) v.findViewById(R.id.row_name);
                TextView desc = (TextView) v.findViewById(R.id.row_desc);


                // check to see if each individual textview is null.
                // if not, assign some text!
                name.setText( objects.get(position).getName());
                desc.setText( objects.get(position).getDesc());

            }

            // the view must be returned to our activity
            return v;

        }

    }
}
