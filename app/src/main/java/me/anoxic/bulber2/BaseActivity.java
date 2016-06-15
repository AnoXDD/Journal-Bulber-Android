package me.anoxic.bulber2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.LruCache;
import android.widget.Toast;

import com.onedrive.sdk.authentication.MSAAuthenticator;
import com.onedrive.sdk.concurrency.ICallback;
import com.onedrive.sdk.core.ClientException;
import com.onedrive.sdk.core.DefaultClientConfig;
import com.onedrive.sdk.core.IClientConfig;
import com.onedrive.sdk.extensions.IItemRequestBuilder;
import com.onedrive.sdk.extensions.IOneDriveClient;
import com.onedrive.sdk.extensions.Item;
import com.onedrive.sdk.extensions.OneDriveClient;
import com.onedrive.sdk.logger.LoggerLevel;

import java.util.concurrent.atomic.AtomicReference;

public class BaseActivity extends Activity {

    /**
     * The service instance
     */
    private final AtomicReference<IOneDriveClient> mClient = new AtomicReference<>();

    /**
     * The system connectivity manager
     */
    private ConnectivityManager mConnectivityManager;

    private StorageManager storageManager;

    /**
     * What to do when the application starts
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * Create the client configuration
     *
     * @return the newly created configuration
     */
    private IClientConfig createConfig() {
        final MSAAuthenticator msaAuthenticator = new MSAAuthenticator() {
            @Override
            public String getClientId() {
                return "000000004418E939";
            }

            @Override
            public String[] getScopes() {
                return new String[]{"onedrive.readwrite", "onedrive.appfolder", "wl.offline_access"};
            }
        };

        final IClientConfig config = DefaultClientConfig.createWithAuthenticator(msaAuthenticator);
        config.getLogger()
                .setLoggingLevel(LoggerLevel.Debug);
        return config;
    }

    /**
     * Get an instance of the service
     *
     * @return The Service
     */
    synchronized IOneDriveClient getOneDriveClient() {
        if (mClient.get() == null) {
            throw new UnsupportedOperationException("Unable to generate a new service object");
        }
        return mClient.get();
    }

    /**
     * Used to setup the Services
     *
     * @param activity       the current activity
     * @param serviceCreated the callback
     */
    synchronized void createOneDriveClient(final Activity activity,
                                           final ICallback<Void> serviceCreated) {
        final DefaultCallback<IOneDriveClient> callback = new DefaultCallback<IOneDriveClient>(
                activity) {
            @Override
            public void success(final IOneDriveClient result) {
                mClient.set(result);
                serviceCreated.success(null);
            }

            @Override
            public void failure(final ClientException error) {
                serviceCreated.failure(error);
            }
        };
        new OneDriveClient.Builder().fromConfig(createConfig())
                .loginAndBuildClient(activity, callback);
    }

    /**
     * Attempts to publish a bulb
     *
     * @param bulb The bulb to be published
     */
    public void attemptPublishBulb(final String bulb) {
        // Try to find the bulb folder
        if (storageManager.getBulbFolderID() != null) {
            publishBulbOnFolder(bulb);
        } else {
            getOneDriveClient().getDrive()
                    .getRoot()
                    .getItemWithPath("/Apps/Journal/bulb")
                    .buildRequest()
                    .get(new ICallback<Item>() {
                        @Override
                        public void success(Item item) {
                            Toast.makeText(getApplicationContext(),
                                    "Get bulb folder: " + item.toString(),
                                    Toast.LENGTH_LONG)
                                    .show();

                            // Store the data
                            storageManager.setBulbFolderID(item.toString());

                            publishBulbOnFolder(bulb);
                        }

                        @Override
                        public void failure(ClientException ex) {
                            ex.printStackTrace();
                            Toast.makeText(getApplicationContext(),
                                    "Unable to get the bulb folder",
                                    Toast.LENGTH_LONG)
                                    .show();
                        }
                    });
        }
    }

    /**
     * Publish the bulb to the bulb folder
     *
     * @param bulb
     * @require bulb folder ID is valid (in `StorageManager`)
     */
    private void publishBulbOnFolder(final String bulb) {
        String id = StorageManager.getBulbFolderID();

    }
}