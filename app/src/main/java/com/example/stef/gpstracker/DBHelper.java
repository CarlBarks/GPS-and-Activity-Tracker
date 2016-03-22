package com.example.stef.gpstracker;

/**
 * Created by Stef on 9/3/2016.
 */
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

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
    public static final String TRACKPOINTS_COLUMN_ACTIVITY= "activity";
    private HashMap hp;
    // variable to hold context
    private Context context;

    private SQLiteDatabase db;

    public DBHelper(Context context)
    {
        super(context, DATABASE_NAME , null, 1);
        this.context=context;
        //db = getWritableDatabase();
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

    public boolean insertTrackpoint  (double lat, double lon, String time, String activity)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("lat", lat);
        contentValues.put("lon", lon);
        contentValues.put("time", time);
        contentValues.put("activity", activity);
        db.insert("TRACKPOINTS", null, contentValues);
        return true;
    }

    public Cursor getData(int id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from TRACKPOINTS where id="+id+"", null );
        return res;
    }

    public int numberOfRows(){
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, TRACKPOINTS_TABLE_NAME);
        return numRows;
    }

    /*public boolean updateContact (Integer id, String name, String phone, String email, String street,String place)
    {
        /*SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", name);
        contentValues.put("phone", phone);
        contentValues.put("email", email);
        contentValues.put("street", street);
        contentValues.put("place", place);
        db.update("TRACKPOINTS", contentValues, "id = ? ", new String[] { Integer.toString(id) } );
        return true;
    }*/

    /*public Integer deleteContact (Integer id)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("TRACKPOINTS",
                "id = ? ",
                new String[] { Integer.toString(id) });
    }*/

    public ArrayList<LatLng> getAllPoints()
    {
        ArrayList<LatLng> array_list = new ArrayList<LatLng>();

        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from TRACKPOINTS", null );
        res.moveToFirst();

        while(res.isAfterLast() == false){

            double lati = res.getDouble(res.getColumnIndex(TRACKPOINTS_COLUMN_LAT));
            double lngi =  res.getDouble(res.getColumnIndex(TRACKPOINTS_COLUMN_LON));
            LatLng pair = new LatLng(lati, lngi);
            array_list.add(pair);
            res.moveToNext();
        }
        res.close();
        return array_list;
    }

    public ArrayList<String> getAllPointsActivity()
    {
        ArrayList<String> array_list = new ArrayList<String>();

        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from TRACKPOINTS", null );
        res.moveToFirst();

        while(res.isAfterLast() == false){
            String activity = res.getString(res.getColumnIndex(TRACKPOINTS_COLUMN_ACTIVITY));
            array_list.add(activity);
            res.moveToNext();
        }
        res.close();
        return array_list;
    }

    public ArrayList<String> getAllPointsTime()
    {
        ArrayList<String> array_list = new ArrayList<String>();

        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from TRACKPOINTS", null );
        res.moveToFirst();

        while(res.isAfterLast() == false){
            String timestamp = res.getString(res.getColumnIndex(TRACKPOINTS_COLUMN_TIME));
            array_list.add(timestamp);
            res.moveToNext();
        }
        res.close();
        return array_list;
    }

    public ArrayList<LatLng> getAllVehicles(){

        ArrayList<LatLng> array_list = new ArrayList<LatLng>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from TRACKPOINTS where activity="+"'In a vehicle'", null );
        res.moveToFirst();

        while(res.isAfterLast() == false){

            double lati = res.getDouble(res.getColumnIndex(TRACKPOINTS_COLUMN_LAT));
            double lngi =  res.getDouble(res.getColumnIndex(TRACKPOINTS_COLUMN_LON));
            LatLng pair = new LatLng(lati, lngi);
            array_list.add(pair);
            res.moveToNext();
        }
        res.close();
        return array_list;
    }

    public ArrayList<LatLng> getAllOnFoot(){

        ArrayList<LatLng> array_list = new ArrayList<LatLng>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from TRACKPOINTS where activity="+"'On foot'", null );
        res.moveToFirst();

        while(res.isAfterLast() == false){

            double lati = res.getDouble(res.getColumnIndex(TRACKPOINTS_COLUMN_LAT));
            double lngi =  res.getDouble(res.getColumnIndex(TRACKPOINTS_COLUMN_LON));
            LatLng pair = new LatLng(lati, lngi);
            array_list.add(pair);
            res.moveToNext();
        }
        res.close();
        return array_list;
    }

    public ArrayList<LatLng> getAllOnBike(){

        ArrayList<LatLng> array_list = new ArrayList<LatLng>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from TRACKPOINTS where activity="+"'On a bike'", null );
        res.moveToFirst();

        while(res.isAfterLast() == false){

            double lati = res.getDouble(res.getColumnIndex(TRACKPOINTS_COLUMN_LAT));
            double lngi =  res.getDouble(res.getColumnIndex(TRACKPOINTS_COLUMN_LON));
            LatLng pair = new LatLng(lati, lngi);
            array_list.add(pair);
            res.moveToNext();
        }
        res.close();
        return array_list;
    }

}