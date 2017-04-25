package com.teddyteh.kmscraper.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * Created by matthew on 25/03/2017.
 */

@DatabaseTable(tableName = "summary")
public class Summary {
    @DatabaseField(id = true)
    private String user;

    @DatabaseField(dataType = DataType.DATE_LONG, columnDefinition = "DATETIME NOT NULL DEFAULT (datetime('now','localtime'))")
    private Date ts;

    @DatabaseField(canBeNull = false)
    private String name;

    @DatabaseField(canBeNull = false)
    private String number;

    @DatabaseField(canBeNull = false)
    private String plan;

    @DatabaseField(canBeNull = false)
    private Date dataRenews;

    @DatabaseField(canBeNull = false)
    private Date planRenews;

    @DatabaseField(canBeNull = false)
    private int quota;

    @DatabaseField(canBeNull = false)
    private int calls;

    @DatabaseField(canBeNull = false)
    private int sms;

    @DatabaseField(canBeNull = false)
    private long data;

    @DatabaseField(canBeNull = false)
    private int dataPC;

    @DatabaseField
    private boolean autoRecharge;

    public Summary() {
    }

    public Summary(String user) {
        this.user = user;
        ts = new Date();
        name = null;
        number = null;
        plan = null;
        dataRenews = null;
        planRenews = null;
        quota = 0;
        calls = 0;
        sms = 0;
        data = 0;
        dataPC = 0;
        autoRecharge = false;
    }

    public String getUser() {
        return user;
    }

    public Date getTs() {
        return ts;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public Date getDataRenews() {
        return dataRenews;
    }

    public void setDataRenews(Date dataRenews) {
        this.dataRenews = dataRenews;
    }

    public Date getPlanRenews() {
        return planRenews;
    }

    public void setPlanRenews(Date planRenews) {
        this.planRenews = planRenews;
    }

    public int getQuota() {
        return quota;
    }

    public void setQuota(int quota) {
        this.quota = quota;
    }

    public int getCalls() {
        return calls;
    }

    public void setCalls(int calls) {
        this.calls = calls;
    }

    public int getSms() {
        return sms;
    }

    public void setSms(int sms) {
        this.sms = sms;
    }

    public long getData() {
        return data;
    }

    public void setData(long data) {
        this.data = data;
    }

    public int getDataPC() {
        return dataPC;
    }

    public void setDataPC(int dataPC) {
        this.dataPC = dataPC;
    }

    public boolean isAutoRecharge() {
        return autoRecharge;
    }

    public void setAutoRecharge(boolean autoRecharge) {
        this.autoRecharge = autoRecharge;
    }
}
