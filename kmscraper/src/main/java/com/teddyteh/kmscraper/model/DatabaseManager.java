package com.teddyteh.kmscraper.model;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;


public class DatabaseManager {

    public final static String TAG = "DatabaseManager";

    static private DatabaseManager instance;

    static public void init(Context ctx) {
        if (null == instance) {
            instance = new DatabaseManager(ctx);
        }
    }

    static public DatabaseManager getInstance() {
        return instance;
    }

    private DatabaseHelper helper;

    private DatabaseManager(Context ctx) {
        helper = new DatabaseHelper(ctx);
    }

    private DatabaseHelper getHelper() {
        return helper;
    }

    public List<Summary> getAllSummary() {
        List<Summary> summaryList = null;
        try {
            summaryList = getHelper().getSummaryDao().queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return summaryList;
    }

    public Summary getSummary(String user) {
        Summary summary = null;
        try {
            summary = getHelper().getSummaryDao().queryForId(user);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return summary;
    }

    public List<Usage> getAllUsage() {
        List<Usage> usageList = null;
        try {
            usageList = getHelper().getUsageDao().queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return usageList;
    }

    public List<Usage> getUsage(String user) {
        List<Usage> usageList = null;
        try {
            usageList = getHelper().getUsageDao().queryForEq("user", user);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return usageList;
    }

    public void addSummary(Summary summary) {
        try {
            getHelper().getSummaryDao().create(summary);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateSummary(Summary summary) {
        try {
            getHelper().getSummaryDao().update(summary);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delSummary(Summary summary) {
        try {
            getHelper().getSummaryDao().delete(summary);
        } catch (SQLException e) {
            ;   // Ignore failed deletes
        }
    }

    public void addUsage(Usage usage) {
        try {
            getHelper().getUsageDao().create(usage);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateUsage(Usage usage) {
        try {
            getHelper().getUsageDao().update(usage);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delUsage(Usage usage) {
        try {
            getHelper().getUsageDao().delete(usage);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delUsage(String user) {
        try {
            Dao dao = getHelper().getUsageDao();
            DeleteBuilder<Usage, String> deleteBuilder = dao.deleteBuilder();
            deleteBuilder.where().eq("USER", user);
            dao.delete(deleteBuilder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void doTransaction(Callable<Void> func) {
        try {
            getHelper().doTransaction(func);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}