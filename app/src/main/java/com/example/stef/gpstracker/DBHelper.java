package com.example.stef.gpstracker;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.gms.maps.model.LatLng;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "MyDBName.db";
    public static final String TRACKPOINTS_TABLE_NAME = "trackpoints";
    public static final String TRACKPOINTS_COLUMN_ID = "id";
    public static final String TRACKPOINTS_COLUMN_LAT = "lat";
    public static final String TRACKPOINTS_COLUMN_LON = "lon";
    public static final String TRACKPOINTS_COLUMN_TIME = "time";
    public static final String TRACKPOINTS_COLUMN_ACTIVITY = "activity";
    private HashMap hp;
    // variable to hold context
    private Context context;

    private SQLiteDatabase db;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS TRACKPOINTS " +
                        "(id integer primary key, lat double, lon double, time text, activity text)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //db.execSQL("DROP TABLE IF EXISTS TRACKPOINTS");
        //onCreate(db);
    }

    public boolean insertTrackpoint(double lat, double lon, String time, String activity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("lat", lat);
        contentValues.put("lon", lon);
        contentValues.put("time", time);
        contentValues.put("activity", activity);
        db.insert("TRACKPOINTS", null, contentValues);
        return true;
    }

    public Cursor getData(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from TRACKPOINTS where id=" + id + "", null);
        return res;
    }

    public int numberOfRows() {
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, TRACKPOINTS_TABLE_NAME);
        return numRows;
    }


    public ArrayList<String> getColumn(String column) {
        ArrayList<String> array_list = new ArrayList<String>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from TRACKPOINTS", null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            String activity = res.getString(res.getColumnIndex(column));
            array_list.add(activity);
            res.moveToNext();
        }
        res.close();
        return array_list;
    }

    public ArrayList<String> getAllPointsActivity() {
        ArrayList<String> array_list;
        array_list = getColumn(TRACKPOINTS_COLUMN_ACTIVITY);
        return array_list;
    }

    public ArrayList<String> getAllPointsTime() {
        ArrayList<String> array_list;
        array_list = getColumn(TRACKPOINTS_COLUMN_TIME);
        return array_list;
    }

    public ArrayList<LatLng> getAllPoints(String mode) {
        ArrayList<LatLng> array_list = new ArrayList<LatLng>();

        SQLiteDatabase db = this.getReadableDatabase();
        String query = "";
        if (mode.equals("All")) {
            query = "select * from TRACKPOINTS";
        } else if (mode.equals("Vehicle")) {
            query = "select * from TRACKPOINTS where activity=" + "'In a vehicle'";
        } else if (mode.equals("Foot")) {
            query = "select * from TRACKPOINTS where activity=" + "'On foot'";
        } else if (mode.equals("Bike")) {
            query = "select * from TRACKPOINTS where activity=" + "'On a bicycle'";
        }

        // send DB query
        Cursor res = db.rawQuery(query, null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            double lati = res.getDouble(res.getColumnIndex(TRACKPOINTS_COLUMN_LAT));
            double lngi = res.getDouble(res.getColumnIndex(TRACKPOINTS_COLUMN_LON));
            LatLng pair = new LatLng(lati, lngi);
            array_list.add(pair);
            res.moveToNext();
        }
        res.close();
        return array_list;
    }

    public ArrayList<LatLng> getAllPointsToday(String mode) {
        ArrayList<LatLng> array_list = new ArrayList<LatLng>();

        SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date date = new Date();
        String nowAsString = originalFormat.format(date);

        String pattern = "(^\\d{4}-\\d{2}-\\d{2})";
        // Create a Pattern object
        Pattern r = Pattern.compile(pattern);
        // Now create matcher object.
        Matcher m = r.matcher(nowAsString);
        String today_date = "";
        if (m.find()) {
            today_date = m.group(0);
        }

        SQLiteDatabase db = this.getReadableDatabase();
        String query = "";
        if (mode.equals("All")) {
            query = "select * from TRACKPOINTS where time LIKE '%" + today_date + "%'";
        } else if (mode.equals("Vehicle")) {
            query = "select * from TRACKPOINTS where time LIKE '%" + today_date + "%' AND activity=" + "'In a vehicle'";
        } else if (mode.equals("Foot")) {
            query = "select * from TRACKPOINTS where time LIKE '%" + today_date + "%' AND activity=" + "'On foot'";
        } else if (mode.equals("Bike")) {
            query = "select * from TRACKPOINTS where time LIKE '%" + today_date + "%' AND activity=" + "'On a bicycle'";
        }

        // send DB query
        Cursor res = db.rawQuery(query, null);
        res.moveToFirst();

        while (!res.isAfterLast()) {

            double lati = res.getDouble(res.getColumnIndex(TRACKPOINTS_COLUMN_LAT));
            double lngi = res.getDouble(res.getColumnIndex(TRACKPOINTS_COLUMN_LON));
            LatLng pair = new LatLng(lati, lngi);
            array_list.add(pair);
            res.moveToNext();
        }
        res.close();
        return array_list;
    }

    public ArrayList<LatLng> getAllPointsDatePeriod(String mode, String start, String end) {
        ArrayList<LatLng> array_list = new ArrayList<LatLng>();

        try {
            DateFormat originalFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            DateFormat targetFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = originalFormat.parse(start);
            String formattedDate_start = targetFormat.format(date);

            Date date2 = originalFormat.parse(end);
            String formattedDate_end = targetFormat.format(date2);

            SQLiteDatabase db = this.getReadableDatabase();
            String query = "";
            if (mode.equals("All")) {
                query = "select * from TRACKPOINTS where time between '"
                        + formattedDate_start + "' and '" + formattedDate_end + "'";
            } else if (mode.equals("Vehicle")) {
                query = "select * from TRACKPOINTS where time between '"
                        + formattedDate_start + "' and '" + formattedDate_end + "' AND activity=" + "'In a vehicle'";
            } else if (mode.equals("Foot")) {
                query = "select * from TRACKPOINTS where time between '"
                        + formattedDate_start + "' and '" + formattedDate_end + "' AND activity=" + "'On foot'";
            } else if (mode.equals("Bike")) {
                query = "select * from TRACKPOINTS where time between '"
                        + formattedDate_start + "' and '" + formattedDate_end + "' AND activity=" + "'On a bicycle'";
            }

            // send DB query
            Cursor res = db.rawQuery(query, null);
            res.moveToFirst();

            while (!res.isAfterLast()) {

                double lati = res.getDouble(res.getColumnIndex(TRACKPOINTS_COLUMN_LAT));
                double lngi = res.getDouble(res.getColumnIndex(TRACKPOINTS_COLUMN_LON));
                LatLng pair = new LatLng(lati, lngi);
                array_list.add(pair);
                res.moveToNext();

            }
            res.close();
            return array_list;

        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }

    }
}
