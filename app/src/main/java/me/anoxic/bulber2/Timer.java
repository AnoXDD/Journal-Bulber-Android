package me.anoxic.bulber2;


import android.text.format.DateFormat;

/**
 * This is a class to store some time-related methods
 * Created by Anoxic on 061516.
 */
public class Timer {

    public static String getCurrentBulbFilename() {
        DateFormat dateFormat = new DateFormat();
        return (String) dateFormat.format("MMddyy'_'HHmmss", new java.util.Date());
    }
}
