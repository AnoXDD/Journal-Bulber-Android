package me.anoxic.bulber2;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * This class manages the storage of everything needed
 * Created by Anoxic on 061516.
 */
public class StorageManager {
    private SharedPreferences sharedPreferences = null;
    private Context context = null;
    private boolean isDebugging = false;
    private boolean isAttachingLocation = false;

    public boolean isAttachingLocation() {
        return isAttachingLocation;
    }

    public void setAttachingLocation(boolean attachingLocation) {
        isAttachingLocation = attachingLocation;
    }

    public boolean isDebugging() {
        return isDebugging;
    }

    public void setDebugging(boolean debugging) {
        isDebugging = debugging;
    }

    public String getBulbFolderID() {
        return sharedPreferences.getString(context.getString(R.string.bulb_folder_id), null);
    }

    public void setBulbFolderID(String bulbFolderID) {
        sharedPreferences.edit()
                .putString(context.getString(R.string.bulb_folder_id), bulbFolderID)
                .apply();
    }

    public String getLastPushedBulbID() {
        return sharedPreferences.getString(context.getString(R.string.bulb_last_pushed_id), null);
    }

    public void setLastPushedBulbID(String lastPushedBulbID) {
        sharedPreferences.edit()
                .putString(context.getString(R.string.bulb_last_pushed_id), lastPushedBulbID)
                .apply();
    }

    public void clearLastPushedBulbID() {
        sharedPreferences.edit()
                .remove(context.getString(R.string.bulb_last_pushed_id))
                .apply();
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public StorageManager setSharedPreferences(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;

        return this;
    }

    public Context getContext() {
        return context;
    }

    public StorageManager setContext(Context context) {
        this.context = context;

        return this;
    }

}
