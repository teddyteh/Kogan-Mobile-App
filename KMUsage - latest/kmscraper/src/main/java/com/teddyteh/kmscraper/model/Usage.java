package com.teddyteh.kmscraper.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;


/**
 * Created by matthew on 25/03/2017.
 */

@DatabaseTable(tableName = "usage")
public class Usage {
    @DatabaseField(index = true)
    private String user;

    @DatabaseField
    private String type;

    @DatabaseField
    private Date ts;

    @DatabaseField
    private String number;

    @DatabaseField
    private String callType;

    @DatabaseField
    private Double cost;

    @DatabaseField
    private Long volume;

    public Usage() {
    }     //  Required by OrmLite, otherwise unused

    public Usage(String user, Date ts) {
        this.user = user;
        this.ts = ts;
        type = "?";
        number = null;
        callType = null;
        cost = 0.0;
        volume = 0L;
    }

    public String getUser() {
        return user;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getTs() {
        return ts;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }

    public String toString() {
        String str = "Unknown type '" + type + "'";
        if (type.equals("V")) str = fmtVoice();
        if (type.equals("T")) str = fmtText();
        if (type.equals("D")) str = fmtData();
        return user + "\t" + str;
    }

    private String fmtVoice() {
        String str = "Voice:\t" + ts + "\t" + number + "\t" + callType + "\t$" + cost;
        return str;
    }

    private String fmtText() {
        String str = "SMS:\t" + ts + "\t" + number + "\t" + callType + "\t$" + cost;
        return str;
    }

    private String fmtData() {
        String str = "Data:\t" + ts + "\t" + volume;
        return str;
    }
}
