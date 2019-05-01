package ru.nsu.mmedia.petrogismobile;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by vkazakov on 26.07.2017.
 */

public class LocalDBHelper {
    private SQLiteDatabase mdb;
    public LocalDBHelper(Context context) {
        PetroglyphOpenHelper dbHelper = new PetroglyphOpenHelper(context);
        mdb = dbHelper.getWritableDatabase();
    }
    protected long createDBRecord(String name, double lat, double lng, String photoPath, double orientation_x, double orientation_y, double orientation_z) {
        ContentValues cv = new ContentValues();
        cv.put("uuid", UUID.randomUUID().toString());
        cv.put("name", name);
        cv.put("lat", lat);
        cv.put("lng", lng);
        cv.put("image", photoPath);
        cv.put("orientation_x", orientation_x);
        cv.put("orientation_y", orientation_y);
        cv.put("orientation_z", orientation_z);
        long rowID = mdb.insert("petroglyph", null, cv);
        return rowID;
    }
    protected long createDBRecordWithUuid(String name, double lat, double lng, String photoPath, String uuid, double orientation_x, double orientation_y, double orientation_z) {
        ContentValues cv = new ContentValues();
        cv.put("uuid", uuid);
        cv.put("name", name);
        cv.put("lat", lat);
        cv.put("lng", lng);
        cv.put("image", photoPath);
        cv.put("orientation_x", orientation_x);
        cv.put("orientation_y", orientation_y);
        cv.put("orientation_z", orientation_z);
        long rowID = mdb.insert("petroglyph", null, cv);
        return rowID;
    }

    public ArrayList<Petroglyph> getRecords()
    {
        Cursor c = mdb.query("petroglyph", null, null, null, null, null, null);
        // ставим позицию курсора на первую строку выборки
        // если в выборке нет строк, вернется false
        ArrayList<Petroglyph> petroglyphs= new ArrayList<Petroglyph>();
        if (c.moveToFirst()) {
            do {
                Petroglyph petroglyph = new Petroglyph(
                        c.getInt(c.getColumnIndex("id")),
                        c.getString(c.getColumnIndex("uuid")),
                        c.getString(c.getColumnIndex("name")),
                        c.getDouble(c.getColumnIndex("lat")),
                        c.getDouble(c.getColumnIndex("lng")),
                        c.getString(c.getColumnIndex("image")),
                        c.isNull(c.getColumnIndex("orientation_x"))?0:c.getDouble(c.getColumnIndex("orientation_x")),
                        c.isNull(c.getColumnIndex("orientation_y"))?0:c.getDouble(c.getColumnIndex("orientation_y")),
                        c.isNull(c.getColumnIndex("orientation_z"))?0:c.getDouble(c.getColumnIndex("orientation_z"))
                );
                petroglyphs.add(petroglyph);
                // определяем номера столбцов по имени в выборке
            } while (c.moveToNext());
        }
        return petroglyphs;
    }
}
