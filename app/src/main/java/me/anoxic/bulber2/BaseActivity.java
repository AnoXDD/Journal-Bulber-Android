package me.anoxic.bulber2;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.onedrive.sdk.authentication.MSAAuthenticator;
import com.onedrive.sdk.concurrency.ICallback;
import com.onedrive.sdk.concurrency.IProgressCallback;
import com.onedrive.sdk.core.ClientException;
import com.onedrive.sdk.core.DefaultClientConfig;
import com.onedrive.sdk.core.IClientConfig;
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

    public static StorageManager storageManager = new StorageManager();

    /**
     * What to do when the application starts
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPreferences = this.getPreferences(Context.MODE_PRIVATE);
        storageManager.setSharedPreferences(sharedPreferences)
                .setContext(getApplicationContext());

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
                            // Store the data
                            storageManager.setBulbFolderID(item.id);

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
     * @param bulb the bulb content
     * @require bulb folder ID is valid (in `StorageManager`)
     */
    private void publishBulbOnFolder(final String bulb) {
        if (bulb == null)
            return;

        String id = storageManager.getBulbFolderID();

        final String filename = Timer.getCurrentBulbFilename();
        final byte[] fileContents = bulb.getBytes();
        final IProgressCallback<Item> callback = new IProgressCallback<Item>() {
            @Override
            public void progress(long current, long max) {

            }

            public void success(final Item item) {
                Snackbar.make(getCurrentFocus(),
                        getString(R.string.bulb_pushed),
                        Snackbar.LENGTH_LONG)
                        .show();

                // Clear the field
                final EditText editText = (EditText) findViewById(R.id.bulbContent);
                editText.setText("");

                // Store the last pushed data
                storageManager.setLastPushedBulbID(item.id);

                // Enable the undo button
                final Button button = (Button) findViewById(R.id.undo);
                button.setEnabled(true);
            }

            @Override
            public void failure(ClientException ex) {
                Snackbar.make(getCurrentFocus(),
                        getString(R.string.bulb_push_failed),
                        Snackbar.LENGTH_LONG)
                        .show();
            }
        };

        this.getOneDriveClient()
                .getDrive()
                .getItems(id)
                .getChildren()
                .byId(filename)
                .getContent()
                .buildRequest()
                .put(fileContents, callback);
    }

    public void attemptRemoveLastPushedBulb() {
        // Try to get the id of last pushed bulb
        String id = storageManager.getLastPushedBulbID();
        if (id != null) {

            final ICallback<Void> callback = new ICallback<Void>() {
                @Override
                public void success(Void aVoid) {
                    Snackbar.make(getCurrentFocus(),
                            getString(R.string.bulb_remove_last_pushed_success),
                            Snackbar.LENGTH_LONG)
                            .show();

                    // Remove the id
                    storageManager.clearLastPushedBulbID();

                    // Disable the button
                    final Button undo = (Button) findViewById(R.id.undo);
                    undo.setEnabled(false);
                }

                @Override
                public void failure(ClientException ex) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.bulb_remove_last_pushed_fail),
                            Toast.LENGTH_SHORT).show();

                    ex.printStackTrace();
                }
            };

            this.getOneDriveClient()
                    .getDrive()
                    .getItems(id)
                    .buildRequest()
                    .delete(callback);

        } else {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.bulb_remove_last_pushed_fail),
                    Toast.LENGTH_SHORT).show();

            final Button button = (Button) findViewById(R.id.undo);
            button.setEnabled(false);
        }
    }

    public void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }
}