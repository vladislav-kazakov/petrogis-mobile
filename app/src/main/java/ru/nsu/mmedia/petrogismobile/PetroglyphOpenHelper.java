package ru.nsu.mmedia.petrogismobile;

/**
 * Created by vkazakov on 14.07.2017.
 */
import android.database.sqlite.*;
import android.content.Context;

public class PetroglyphOpenHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 10;
    private static final String DATABASE_NAME = "petrogis";
    private static final String PETROGLYPH_TABLE_NAME = "petroglyph";

    PetroglyphOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL( "CREATE TABLE " + PETROGLYPH_TABLE_NAME + " (id INTEGER PRIMARY KEY, uuid TEXT, name TEXT, lat REAL, lng REAL, image TEXT, orientation_x REAL, orientation_y REAL, orientation_z REAL)");
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //проверяете какая версия сейчас и делаете апдейт
        db.execSQL("DROP TABLE IF EXISTS " + PETROGLYPH_TABLE_NAME);
        onCreate(db);
    }
}
