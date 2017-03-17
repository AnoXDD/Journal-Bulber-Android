package me.anoxic.bulber2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;


public class BaseActivity extends Activity implements ActivityCompat
        .OnRequestPermissionsResultCallback, ConnectionCallbacks, OnConnectionFailedListener {

    private static final int REQUEST_FINE_LOCATION = 0;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    protected static final String TAG = "main-activity";
    protected static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    protected static final String LOCATION_ADDRESS_KEY = "location-address";

    private static final int TAKE_IMAGE = 0;
    private static final int PICK_IMAGE_REQUEST = 1;

    /**
     * The service instance
     */
    private final AtomicReference<IOneDriveClient> mClient = new AtomicReference<>();

    /**
     * The system connectivity manager
     */
    private ConnectivityManager mConnectivityManager;

    private MyLocationManager locationManager;

    private static StorageManager storageManager = new StorageManager();

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Tracks whether the user has requested an address. Becomes true when the user requests an
     * address and false when the address (or an error message) is delivered.
     * The user requests an address by pressing the Fetch Address button. This may happen
     * before GoogleApiClient connects. This activity uses this boolean to keep track of the
     * user's intent. If the value is true, the activity tries to fetch the address as soon as
     * GoogleApiClient connects.
     */
    protected boolean mAddressRequested;

    /**
     * The formatted location address.
     */
    protected String mAddressOutput;

    protected Uri photoImageUri;

    /**
     * Receiver registered with this activity to get the response from FetchAddressIntentService.
     */
    private AddressResultReceiver mResultReceiver;

    /**
     * What to do when the application starts
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up shared preference
        SharedPreferences sharedPreferences = this.getPreferences(Context.MODE_PRIVATE);
        storageManager.setSharedPreferences(sharedPreferences)
                .setContext(getApplicationContext());

        // Request location
        requestLocationPermission();

        // Request write external storage (for camera)
        requestWriteExternalStoragePermission();

        // Initialize to get the location address
        mResultReceiver = new AddressResultReceiver(new Handler());
        buildGoogleApiClient();

        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        locationManager = new MyLocationManager(getApplicationContext());

        // Set the version number
        setVersionNumber();
    }

    private void setVersionNumber() {
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);

            final String version = pInfo.versionName;

            final ImageButton imageButton = (ImageButton) findViewById(R.id.signin);
            imageButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(getApplicationContext(), version, Toast.LENGTH_SHORT)
                            .show();
                    return true;
                }
            });

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        MultiDex.install(this);
    }

    private void requestWriteExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission
                    .WRITE_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission
                        .WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission
                    .ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission
                        .ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
            }
        }
    }

    StorageManager getStorageManager() {
        return storageManager;
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
                return new String[]{"onedrive.readwrite", "onedrive.appfolder", "wl" + "" +
                        ".offline_access"};
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
    synchronized void createOneDriveClient(final Activity activity, final ICallback<Void>
            serviceCreated) {
        final DefaultCallback<IOneDriveClient> callback = new DefaultCallback<IOneDriveClient>
                (activity) {
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
     * Attempts to publish a bulb.
     *
     * @param bulb The bulb to be published
     */
    public void attemptPublishBulb(final String bulb) {
        // todo clear the image anyways if it's a new photo
        // Try to find the bulb folder
        if (storageManager.getBulbFolderID() != null) {
            publishBulbOnFolder(bulb);
        } else {
            // Find the bulb folder on OneDrive first
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
                            Toast.makeText(getApplicationContext(), "Unable to get the bulb " +
                                    "folder", Toast.LENGTH_LONG)
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
        if (bulb == null) {
            return;
        }

        String id = storageManager.getBulbFolderID();

        final String filename = Timer.getCurrentBulbFilename();
        final byte[] fileContents = bulb.getBytes();
        final IProgressCallback<Item> callback = new IProgressCallback<Item>() {
            @Override
            public void progress(long current, long max) {

            }

            public void success(final Item item) {
                Snackbar.make(getCurrentFocus(), getString(R.string.bulb_pushed), Snackbar
                        .LENGTH_LONG)
                        .show();

                // Clear the field
                final EditText editText = (EditText) findViewById(R.id.bulbContent);
                editText.setText("");

                // Store the last pushed data
                storageManager.setLastPushedBulbID(item.id);

                // Enable the undo button
                final ImageButton button = (ImageButton) findViewById(R.id.undo);
                button.setEnabled(true);
            }

            @Override
            public void failure(ClientException ex) {
                Snackbar.make(getCurrentFocus(), getString(R.string.bulb_push_failed), Snackbar
                        .LENGTH_LONG)
                        .show();
            }
        };

        if (storageManager.isDebugging()) {
            return;
        }

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
                    Snackbar.make(getCurrentFocus(), getString(R.string
                            .bulb_remove_last_pushed_success), Snackbar.LENGTH_LONG)
                            .show();

                    // Remove the id
                    storageManager.clearLastPushedBulbID();

                    // Disable the button
                    final ImageButton undo = (ImageButton) findViewById(R.id.undo);
                    undo.setEnabled(false);
                }

                @Override
                public void failure(ClientException ex) {
                    Toast.makeText(getApplicationContext(), getString(R.string
                            .bulb_remove_last_pushed_fail), Toast.LENGTH_SHORT)
                            .show();

                    ex.printStackTrace();
                }
            };

            this.getOneDriveClient()
                    .getDrive()
                    .getItems(id)
                    .buildRequest()
                    .delete(callback);

        } else {
            Toast.makeText(getApplicationContext(), getString(R.string
                    .bulb_remove_last_pushed_fail), Toast.LENGTH_SHORT)
                    .show();

            final ImageButton button = (ImageButton) findViewById(R.id.undo);
            button.setEnabled(false);
        }
    }

    public MyLocationManager getLocationManager() {
        return locationManager;
    }

    public void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context
                .INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public void promptCurrentLocation() {
        updateCurrentLocation();

        // Prompt location
        String string = locationManager.getFormattedLocation();
        Toast.makeText(getApplicationContext(), String.format(getString(R.string
                .bulb_prompt_current_location), string), Toast.LENGTH_SHORT)
                .show();
    }

    private void updateCurrentLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context
                .LOCATION_SERVICE);

        // Get the last know location from your location manager.
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission
                .ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat
                .checkSelfPermission(getApplicationContext(), Manifest.permission
                        .ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), getString(R.string
                    .bulb_location_request_fail), Toast.LENGTH_SHORT)
                    .show();

            return;
        }
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        this.locationManager.setLocation(location);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOCATION:
                // Check if the only required permission has been granted
                if (grantResults.length == 1 && grantResults[0] == PackageManager
                        .PERMISSION_GRANTED) {
                    // Location permission has been granted, preview can be displayed
                /* Toast.makeText(getApplicationContext(), R.string.bulb_location_request_granted,
                        Toast.LENGTH_SHORT)
                        .show();
                       */

                    Log.d(TAG, getString(R.string.bulb_location_request_granted));
                } else {
                    Toast.makeText(getApplicationContext(), R.string.bulb_location_request_fail,
                            Toast.LENGTH_SHORT)
                            .show();
                }
                break;

            case REQUEST_WRITE_EXTERNAL_STORAGE:

        }
    }

    /**
     * Updates fields based on data stored in the bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Check savedInstanceState to see if the address was previously requested.
            if (savedInstanceState.keySet()
                    .contains(ADDRESS_REQUESTED_KEY)) {
                mAddressRequested = savedInstanceState.getBoolean(ADDRESS_REQUESTED_KEY);
            }
            // Check savedInstanceState to see if the location address string was previously found
            // and stored in the Bundle. If it was found, display the address string in the UI.
            if (savedInstanceState.keySet()
                    .contains(LOCATION_ADDRESS_KEY)) {
                mAddressOutput = savedInstanceState.getString(LOCATION_ADDRESS_KEY);
                displayAddressOutput();
            }
        }
    }

    /**
     * Builds a GoogleApiClient. Uses {@code #addApi} to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * Runs when user clicks the Fetch Address button. Starts the service to fetch the address if
     * GoogleApiClient is connected.
     */
    public void fetchAddressButtonHandler(View view) {
        // We only start the service to fetch the address if GoogleApiClient is connected.
        if (mGoogleApiClient.isConnected() && locationManager.getLocation() != null) {
            startLocationRequestIntentService();
        }
        // If GoogleApiClient isn't connected, we process the user's request by setting
        // mAddressRequested to true. Later, when GoogleApiClient connects, we launch the service to
        // fetch the address. As far as the user is concerned, pressing the Fetch Address button
        // immediately kicks off the process of getting the address.
        mAddressRequested = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = locationManager.getLocation();
        if (location != null) {
            // Determine whether a Geocoder is available.
            if (!Geocoder.isPresent()) {
                Toast.makeText(this, R.string.no_geocoder_available, Toast.LENGTH_LONG)
                        .show();
                return;
            }
            // It is possible that the user presses the button to get the address before the
            // GoogleApiClient object successfully connects. In such a case, mAddressRequested
            // is set to true, but no attempt is made to fetch the address (see
            // fetchAddressButtonHandler()) . Instead, we start the intent service here if the
            // user has requested an address, since we now have a connection to GoogleApiClient.
            if (mAddressRequested) {
                startLocationRequestIntentService();
            }
        }
    }

    /**
     * Creates an intent, adds location data to it as an extra, and starts the intent service for
     * fetching an address.
     */
    protected void startLocationRequestIntentService() {
        // Create an intent for passing to the intent service responsible for fetching the address.
        Intent intent = new Intent(this, FetchAddressIntentService.class);

        // Pass the result receiver as an extra to the service.
        intent.putExtra(FetchAddressIntentService.Constants.RECEIVER, mResultReceiver);

        // Pass the location data as an extra to the service.
        intent.putExtra(FetchAddressIntentService.Constants.LOCATION_DATA_EXTRA, locationManager
                .getLocation());

        // Start the service. If the service isn't already running, it is instantiated and started
        // (creating a process for it if needed); if it is running then it remains running. The
        // service kills itself automatically once all intents are processed.
        startService(intent);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    /**
     * Updates the address in the UI.
     */
    protected void displayAddressOutput() {
        final EditText editText = (EditText) findViewById(R.id.locationAddress);
        final ImageButton imageButton = (ImageButton) findViewById(R.id.isAppendLocation);

        imageButton.setEnabled(true);

        if (mAddressOutput.equals(getString(R.string.service_not_available))) {
            setLocationAppend(false);

        } else {
            setLocationAppend(true);
            editText.setText(mAddressOutput);
        }

    }

    /**
     * Sets (changes) the location append button
     *
     * @param isOn - true if the location is appended
     */
    protected void setLocationAppend(boolean isOn) {
        final ImageButton imageButton = (ImageButton) findViewById(R.id.isAppendLocation);
        System.out.println(isOn);
        // If it is on, we want the user to look at it as if it can be turned off
        imageButton.setImageResource(isOn ? R.drawable.ic_location_off_black : R.drawable
                .ic_location_on_black);
        imageButton.setTag(isOn ? R.drawable.ic_location_off_black : R.drawable
                .ic_location_on_black);
    }

    /**
     * Shows a toast with the given text.
     */
    protected void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save whether the address has been requested.
        savedInstanceState.putBoolean(ADDRESS_REQUESTED_KEY, mAddressRequested);

        // Save the address string.
        savedInstanceState.putString(LOCATION_ADDRESS_KEY, mAddressOutput);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Attempts to attach a photo to this bulb
     *
     * @param isCamera - if the camera should be invoked
     */
    public void attemptAttachPhoto(boolean isCamera) {

        if (isCamera) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Make sure there is something to handle this intent
            if (intent.resolveActivity(getPackageManager()) != null) {
                File photo = null;
                try {
                    photo = createImageFile();
                } catch (IOException e) {
                    Toast.makeText(BaseActivity.this, R.string.create_local_image_fail, Toast
                            .LENGTH_SHORT)
                            .show();
                    e.printStackTrace();
                }

                if (photo != null) {
                    this.photoImageUri = FileProvider.getUriForFile(this, "com.example.android" +
                            ".fileprovider", photo);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, this.photoImageUri);

                    startActivityForResult(intent, TAKE_IMAGE);
                }
            }
        } else {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);

            startActivityForResult(Intent.createChooser(intent, "Select"), PICK_IMAGE_REQUEST);
        }
    }

    /**
     * Create a local image file
     * todo remove all the local files
     *
     * @return a created file
     * @throws IOException - whatever IOException appears
     */
    private File createImageFile() throws IOException {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile("bulbImage", ".jpg", storageDir);

        return image;
    }

    /**
     * Removes any attached photo
     */
    public void removeAttachPhoto() {
        // Clean the UI
        ImageView imageView = (ImageView) findViewById(R.id.bulbImage);
        imageView.setImageBitmap(null);

        storageManager.clearBulbImage();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST || requestCode == TAKE_IMAGE) {
                Uri uri = null;
                if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                    uri = data.getData();
                } else if (requestCode == TAKE_IMAGE) {
                    uri = this.photoImageUri;
                }

                Log.d(TAG, "uri is " + uri.toString());

                if (uri != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),
                                uri);

                        ImageView imageView = (ImageView) findViewById(R.id.bulbImage);
                        imageView.setImageBitmap(bitmap);

                        storageManager.setBulbImage(uri);
                    } catch (IOException e) {
                        Toast.makeText(BaseActivity.this, R.string.select_photo_fail, Toast
                                .LENGTH_SHORT)
                                .show();
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Receiver for data sent from FetchAddressIntentService.
     */
    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        /**
         * Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string or an error message sent from the intent service.
            mAddressOutput = resultData.getString(FetchAddressIntentService.Constants
                    .RESULT_DATA_KEY);
            displayAddressOutput();

            // Show a toast message if an address was found.
            if (resultCode == FetchAddressIntentService.Constants.SUCCESS_RESULT) {
                showToast(getString(R.string.address_found));
            }

            // Reset. Enable the Fetch Address button and stop showing the progress bar.
            mAddressRequested = false;
        }
    }
}