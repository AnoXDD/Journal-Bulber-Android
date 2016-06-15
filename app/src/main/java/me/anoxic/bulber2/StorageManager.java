package me.anoxic.bulber2;

/**
 * This class manages the storage of everything needed
 * Created by Anoxic on 061516.
 */
public class StorageManager {
    private static String bulbFolderID = null;


    public static String getBulbFolderID() {
        return bulbFolderID;
    }

    public static void setBulbFolderID(String bulbFolderID) {
        StorageManager.bulbFolderID = bulbFolderID;
    }
}
