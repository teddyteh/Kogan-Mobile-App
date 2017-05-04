package com.teddyteh.kmscraper.model;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.util.concurrent.Callable;

/**
 * Database interface.  Handle database creation and upgrade.
 */

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    private final String TAG = "DatabaseHelper";
    //  Database version - change this to force a database rebuild
    public static final int DATABASE_VERSION = 3;
    //  The name of the database
    private static final String DATABASE_NAME = "NotTheOfficialKoganMobileApp.db";

    //  The DAO object we use to access the NotTheOfficialKoganMobileApp tables
    private Dao<Summary, String> summaryDao = null;
    private Dao<Usage, String> usageDao = null;
    private Dao<History, String> historyDao = null;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    /*
     *  Create the tables in the NotTheOfficialKoganMobileApp.db sqlite database
     */
    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, Summary.class);
            TableUtils.createTable(connectionSource, Usage.class);
            TableUtils.createTable(connectionSource, History.class);
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't create NotTheOfficialKoganMobileApp database", e);
            throw new RuntimeException(e);
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    /*
     *  Upgrade the schema of the NotTheOfficialKoganMobileApp.db
     *  Since this database only contains ephemeral data, just discard it on upgrade
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            TableUtils.dropTable(connectionSource, Summary.class, true);
            TableUtils.dropTable(connectionSource, Usage.class, true);
            TableUtils.dropTable(connectionSource, History.class, true);
            onCreate(db, connectionSource);
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Exception during onUpgrade of NotTheOfficialKoganMobileApp database", e);
            throw new RuntimeException(e);
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    /*
     *
     */
    public Dao<Summary, String> getSummaryDao() {
        if (null == summaryDao) {
            try {
                summaryDao = getDao(Summary.class);
            } catch (java.sql.SQLException e) {
                e.printStackTrace();
            }
        }
        return summaryDao;
    }

    /*
     *
     */
    public Dao<Usage, String> getUsageDao() {
        if (null == usageDao) {
            try {
                usageDao = getDao(Usage.class);
            } catch (java.sql.SQLException e) {
                e.printStackTrace();
            }
        }
        return usageDao;
    }

    /*
     *
     */
    public Dao<History, String> getHistoryDao() {
        if (null == historyDao) {
            try {
                historyDao = getDao(History.class);
            } catch (java.sql.SQLException e) {
                e.printStackTrace();
            }
        }
        return historyDao;
    }

    public void doTransaction(Callable<Void> func) throws java.sql.SQLException {
        TransactionManager.callInTransaction(getConnectionSource(), func);
    }
}

