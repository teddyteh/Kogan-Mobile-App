package com.teddyteh.kmusage.fragments;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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


public class MainFragment extends Fragment {
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
    private void drawView() {
        try {
            drawProgressBar();
            populateListView();
            MainFragmentListView adapter = new MainFragmentListView(getActivity(), list);
            mListView.setAdapter(adapter);
//            buildGraph(mImage);
        } catch (KMexception ex) {
            Log.e(TAG, "Adapter error", ex);
            // TODO error message on UI
        }
    }

    /*
     *  Get a colour from the current theme
     */
    private void drawProgressBar() throws KMexception {
        int dataDaysPercentage = percent(30 - adapter.getDataDaysRemaining(), 30);
        int dataPercentage = adapter.getData_percent();

        final ArrayList<ArcProgressStackView.Model> models = new ArrayList<>();
        models.add(new ArcProgressStackView.Model("Data used", dataPercentage, getColorFromTheme(R.attr.colorDarkGrey), getColorFromTheme(R.attr.colorDataUsed)));
        models.add(new ArcProgressStackView.Model("Days left", dataDaysPercentage, getColorFromTheme(R.attr.colorLightGrey), getColorFromTheme(R.attr.colorDaysLeft)));
        mArcProgressStackView.setModels(models);
//        mArcProgressStackView.setStartAngle(180);
//        mArcProgressStackView.setSweepAngle(270);
////        mArcProgressStackView.setIsShadowed(false);
//        mArcProgressStackView.setTypeface("fonts/agency.ttf");
//        mArcProgressStackView.setAnimationDuration(1000);
//        mArcProgressStackView.animateProgress();
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
     *  Extract info from the result record and populate the ListView
     */
    private void populateListView() throws KMexception {
//        String dataUsed = scaledNumber(adapter.getNational_data()) + " of " + adapter.getQuota() + "MB (" + adapter.getData_percent() + "%)";
//        String daysUsed = (30 - adapter.getDataDaysRemaining()) + " of 30 days (" + percent(30 - adapter.getDataDaysRemaining(), 30) + "%)";
//        String timeUpd = fmtTime(adapter.getTS());
//        String dataUpd = fmtTime(adapter.getMostRecentDataUsage());

        Calendar cal = Calendar.getInstance();
        cal.setTime(adapter.getData_renews());
        cal.add(Calendar.DATE, -30);
        Date dataStart = cal.getTime();
        String dataStartDate = new SimpleDateFormat("d MMM yy").format(dataStart);
        String dataRenewalDate = new SimpleDateFormat("d MMM yy").format(adapter.getData_renews());
        int dataDaysPercentage = percent(30 - adapter.getDataDaysRemaining(), 30);
        String dataUsed = scaledNumber(adapter.getNational_data());
        String quota = Integer.toString(adapter.getQuota()) + "MB";
        int dataPercentage = adapter.getData_percent();

        list.clear();
        list.add(addItemToList("Days left", dataStartDate, dataRenewalDate, dataDaysPercentage, ContextCompat.getDrawable(getContext(), R.drawable.calendar_48px), getColorFromTheme(R.attr.colorDaysLeft)));
        list.add(addItemToList("Data used", dataUsed, quota, dataPercentage, ContextCompat.getDrawable(getContext(), R.drawable.data_xfer_48px), getColorFromTheme(R.attr.colorDataUsed)));

        //list.add(addItem("Account name", result.getName()));
        //list.add(addItem("Mobile number", result.getNumber()));
//        list.add(addItem("Days used", daysUsed));
//        list.add(addItem("Data renews", dateFmt(adapter.getData_renews())));
//        list.add(addItem("Plan", adapter.getPlan()));
//        list.add(addItem("Expires", dateFmt(adapter.getPlan_expires())));
//        list.add(addItem("Last data", dataUpd));
//        list.add(addItem("Last updated", timeUpd));
//        this.setTitle(adapter.getName() + ": " + adapter.getNumber());
    }

    /*
     *  Creates a ListItemDataModel object which holds information about a list item
     */
    private ListItemDataModel addItemToList(String key, String startDate, String endDate, int percentage, Drawable icon, int color) {
        return new ListItemDataModel(key, startDate, endDate, percentage, icon, color);
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

    /*
     *  Draw the image, circles, arcs and all
     */

    private void buildGraph(ImageView image) throws KMexception {
        //  TODO - Restructure graph drawing
        // --------------------------------------------------------------------------------
        //  This is an ugly chunk of code.  It should be split off to a custom ImageView
        //  object, with most of the graphics occuring in the onDraw() method.  The problem
        //  how to pass the params (data, days and quota) to the ImageView object.  Android
        //  does not allow a custom constructor, so the best approach would be to make a
        //  nested class which has access to data in this object (but I couldn't get that
        //  to work either - see the unused UsageGraphView class below)
        // --------------------------------------------------------------------------------

        final float arcLength = 300.0f;
        Resources r = getResources();

        // Initialize a new Bitmap object
        Bitmap bitmap = Bitmap.createBitmap(
                500, // Width
                500, // Height
                Bitmap.Config.ARGB_8888 // Config
        );

        // Get the data required for the graph
        long data = adapter.getNational_data();
        int days = (30 - adapter.getDataDaysRemaining());
        int quota = adapter.getQuota();

        // Initialize a new Canvas instance
        Canvas canvas = new Canvas(bitmap);

        // Initialize a new Paint instance to draw the Circle
        Paint linePaint = new Paint();
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.DKGRAY);
        linePaint.setStrokeWidth(1.0f);
        linePaint.setAntiAlias(true);

        // Initialize a new Paint for text
        TextPaint textPaint = new TextPaint();
        textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        textPaint.setTextSize(16.0f * getResources().getDisplayMetrics().density);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);

        // Calculate the available radius of canvas
        int outerRadius = Math.min(canvas.getWidth(), canvas.getHeight() / 2);
        int innerRadius = outerRadius - 100;
        int midRadius = outerRadius - 24;

        // Set a pixels value to padding around the circle
        int padding = 5;

        int backgroundArcColor = ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorArc);
        int daysArcColor = ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorDaysLeft);
        int dataArcColor = ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorDataUsed);

        // draw the outer circle on the canvas
        drawArc(canvas, outerRadius, backgroundArcColor, arcLength);

        // Draw the inner circle
        drawArc(canvas, midRadius, backgroundArcColor, arcLength);

        // Draw the central line
        int lineWidth = 2 * innerRadius - 100;
        int lineX = (canvas.getWidth() - lineWidth) / 2;
        int lineY = canvas.getHeight() / 2;    // Place the line in the center
        linePaint.setColor(Color.BLACK);
        linePaint.setStrokeWidth(1.0f);
        canvas.drawLine(
                lineX,                  // start line x
                lineY,                  // start line y
                lineX + lineWidth,      // end line x
                lineY,                  // end line y
                linePaint);

        // Draw the data usage text
        String usageText = scaledNumber(data);
        textPaint.setTextSize(16.0f * getResources().getDisplayMetrics().density);
        Rect textRect = new Rect();
        textPaint.getTextBounds(usageText, 0, usageText.length(), textRect);
        int usageOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, r.getDisplayMetrics());
        drawText(canvas, usageText, textPaint, lineX + lineWidth / 2, lineY - textRect.height() - usageOffset);

        // Draw the quota text
        // TODO - Check that Quota's other than 1024MB work
        String quotaText = (quota > 1024) ? scaledNumber((long) quota * 1024L * 1024L) : String.valueOf(quota) + "MB";
        textPaint.setTextSize(10.0f * getResources().getDisplayMetrics().density);
        Rect quotaRect = new Rect();
        textPaint.getTextBounds(quotaText, 0, quotaText.length(), quotaRect);
        int quotaOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, r.getDisplayMetrics());
        drawText(canvas, quotaText, textPaint, lineX + lineWidth / 2, lineY + quotaRect.height() - quotaOffset);

        // Draw the outer days used arc
        drawArc(canvas, outerRadius, daysArcColor, ((float) days / 30.0f * arcLength));

        // Draw the inner data used arc
        drawArc(canvas, midRadius, dataArcColor, ((float) data / ((float) quota * 1024f * 1024f) * arcLength));

        // Display the newly created bitmap on app interface
        image.setImageBitmap(bitmap);
        image.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
    }
    private void drawArc(Canvas canvas, int radius, int color, float degrees) {
        float width = 44.0f;
        Paint arcPaint = new Paint();
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setColor(color);
        arcPaint.setStrokeWidth(width);
        arcPaint.setAntiAlias(true);

        // Convert the radius to a rectangle (used to
        // set the oval shape for the arc)
        final RectF oval = new RectF();
        float size = radius - width / 4.0f - 3f;
        float top = 500f - 2f * size;
        float left = top;
        float bottom = 2f * size;
        float right = bottom;
        oval.set(left, top, right, bottom);

        // Convert the degrees from starting at 12 to starting at 3
        canvas.drawArc(oval, 90.0f, degrees, false, arcPaint);
    }

    /*
     *  Draw the text using a StaticLayout control (as recommended in the design guidelines)
     */
    private void drawText(Canvas canvas, String str, TextPaint paint, int x, int y) {
        int width = (int) paint.measureText(str);
        StaticLayout layout = new StaticLayout(
                str,                    // text string to draw
                paint,                  // text font etc
                width,                  // string width
                Layout.Alignment.ALIGN_NORMAL,
                1.0f,
                0f,
                false);
        canvas.save();
        canvas.translate(x, y);
        layout.draw(canvas);
        canvas.restore();
    }

    /*
     *  Format a date into dayname monname day
     */
    private String dateFmt(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("EEE MMM d");
        return df.format(date);
    }

    private String fmtTime(Date ts) {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm MMM d");
        return df.format(ts);
    }
}