package me.anoxic.bulber2;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.onedrive.sdk.concurrency.ICallback;

public class FragmentSignIn extends Fragment {


    public FragmentSignIn() {

    }

    /**
     * Handle creation of the view
     *
     * @param inflater           the layout inflater
     * @param container          the hosting containing for this fragment
     * @param savedInstanceState saved state information
     * @return The constructed view
     */
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final
    Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment, container, false);

        enableSignInButton(view);
        enablePushButton(view);
        enableDebugToggle(view);
        enableUndoButton(view);
        enableLocationCheckbox(view);
        enableLocationAddressTextMonitor(view);
        enableRefreshLocation(view);
        enableGalleryButton(view);
        enableCameraButton(view);
        enableRemovePhotoOnClickImageView(view);
        enableShiftFocusOnClickContentBox(view);

        return view;
    }

    private void enableRemovePhotoOnClickImageView(View view) {
        final ImageView imageView = (ImageView) view.findViewById(R.id.bulbImage);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((BaseActivity) getActivity()).removeAttachPhoto();
            }
        });
    }

    private void enableShiftFocusOnClickContentBox(final View view) {
        final LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.contentBox);

        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((BaseActivity) getActivity()).setFocusOnBulbContent();
            }
        });
    }

    private void enableRefreshLocation(final View view) {
        final ImageButton button = (ImageButton) view.findViewById(R.id.locationRefresh);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearLocationResult(view);
                retreiveAndSetLocationResult((BaseActivity) getActivity(), view);
            }
        });
    }

    private void enableCameraButton(View view) {
        final ImageButton button = (ImageButton) view.findViewById(R.id.camera);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((BaseActivity) getActivity()).attemptAttachPhoto(BaseActivity.FROM_CAMERA);
            }
        });
    }

    private void enableGalleryButton(View view) {
        final ImageButton button = (ImageButton) view.findViewById(R.id.gallery);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((BaseActivity) getActivity()).attemptAttachPhoto(BaseActivity.FROM_GALLERY);
            }
        });

        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ((BaseActivity) getActivity()).attemptAttachPhoto(BaseActivity
                        .FROM_LATEST_OF_GALLERY);
                return true;
            }
        });
    }


    /**
     * Adds listener to the edit text next to the checkbox of location
     * Any change in this edittext will be saved in storageManager
     *
     * @param view - the view that has the edittext
     */

    private void enableLocationAddressTextMonitor(View view) {
        final EditText editText = (EditText) view.findViewById(R.id.locationAddress);
        final BaseActivity baseActivity = (BaseActivity) getActivity();

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                baseActivity.getStorageManager()
                        .setCurrentLocationAddress(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void enableLocationCheckbox(final View view) {

        final ImageButton imageButton = (ImageButton) view.findViewById(R.id.isAppendLocation);

        // At the beginning, this image button is off
        imageButton.setTag(R.drawable.ic_location_on_black);

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean wasOn = (Integer) imageButton.getTag() == R.drawable.ic_location_off_black;
                final LinearLayout locationBox = (LinearLayout) view.findViewById(R.id.locationBox);

                final BaseActivity baseActivity = (BaseActivity) getActivity();
                baseActivity.getStorageManager()
                        .setAttachingLocation(!wasOn);

                baseActivity.setLocationAppend(!wasOn);
                locationBox.setVisibility(wasOn ? View.GONE : View.VISIBLE);
                if (!wasOn) {
                    retreiveAndSetLocationResult(baseActivity, view);
                } else {
                    clearLocationResult(view);
                }

            }
        });
    }

    private void clearLocationResult(View view) {
        ((EditText) view.findViewById(R.id.locationAddress)).setText("");
    }

    private void retreiveAndSetLocationResult(BaseActivity baseActivity, View view) {
        // Retrieve the current location when checked
        baseActivity.promptCurrentLocation();
        // Try to fetch the data
        baseActivity.fetchAddressButtonHandler(view);
    }

    private void enableUndoButton(View view) {
        final ImageButton undo = (ImageButton) view.findViewById(R.id.undo);

        undo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Try to get the last element id
                final BaseActivity app = (BaseActivity) getActivity();

                app.attemptRemoveLastPushedBulb();
                app.hideKeyboard();
            }
        });

    }

    private void enableDebugToggle(View view) {
        final ImageButton send = (ImageButton) view.findViewById(R.id.push);

        send.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final BaseActivity baseActivity = (BaseActivity) getActivity();
                StorageManager storageManager = baseActivity.getStorageManager();

                boolean isNowDebug = !storageManager.isDebugging();
                storageManager.setDebugging(isNowDebug);

                Toast.makeText(getContext(), isNowDebug ? baseActivity.getString(R.string
                        .debug_turned_on) : baseActivity.getString(R.string.debug_turned_off),
                        Toast.LENGTH_SHORT)
                        .show();

                return true;
            }
        });
    }

    private void enablePushButton(final View view) {
        final ImageButton push = (ImageButton) view.findViewById(R.id.push);
        final EditText bulbContent = (EditText) view.findViewById(R.id.bulbContent);
        final ImageButton signin = (ImageButton) view.findViewById(R.id.signin);

        push.setEnabled(false);

        push.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Get the content
                String bulb = bulbContent.getText()
                        .toString();

                final BaseActivity app = (BaseActivity) getActivity();
                app.hideKeyboard();

                challengeSignIn(signin, bulb);
            }
        });
    }

    private void enableSignInButton(final View view) {
        final ImageButton signin = (ImageButton) view.findViewById(R.id.signin);

        signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                challengeSignIn(signin, null);
            }
        });

        // Enable/disable the button based on the activities of edittext
        final EditText editText = (EditText) view.findViewById(R.id.bulbContent);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final ImageButton button = (ImageButton) view.findViewById(R.id.push);

                button.setEnabled(s.length() != 0);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    /**
     * Challenges the server for signing in, after which publishing a bulb
     *
     * @param signin The button for signing in
     * @param bulb   The content of bulb. Leave it `null` to not publish anything
     */
    private void challengeSignIn(final ImageButton signin, final String bulb) {
        setButtonThrottled(signin);
        final BaseActivity app = (BaseActivity) getActivity();
        final ICallback<Void> serviceCreated = new DefaultCallback<Void>(getActivity()) {
            @Override
            public void success(final Void result) {
                onTokenRefreshed(bulb);
                resetSigninButtonThrottled(signin);
            }
        };

        app.findViewById(R.id.push)
                .setEnabled(false);
        app.showProgressBar();

        try {
            app.getOneDriveClient();
            onTokenRefreshed(bulb);
            resetSigninButtonThrottled(signin);
        } catch (final UnsupportedOperationException ignored) {
            app.createOneDriveClient(getActivity(), serviceCreated);
        }
    }

    /**
     * Re-enable this signin button and tell the user it's done its job
     *
     * @param button the button to be reset throttling
     */
    private void resetSigninButtonThrottled(ImageButton button) {
        button.setEnabled(true);
    }

    /**
     * Disable this button and tell the user it's doing something
     *
     * @param button the button to be throttled
     */
    private void setButtonThrottled(ImageButton button) {
        button.setEnabled(false);
    }

    /**
     * Do something on token is refreshed
     *
     * @param bulb The content of bulb. Leave it `null` to not publish anything
     */
    private void onTokenRefreshed(String bulb) {
        final BaseActivity app = (BaseActivity) getActivity();
        bulb = addTagsToBulb(bulb);

        app.setProgressBarProgressTo(BaseActivity.SIGNED_IN);
        app.attemptPublishBulb(bulb);
    }

    private String addTagsToBulb(String bulb) {
        final BaseActivity baseActivity = (BaseActivity) getActivity();

        // Add location tag
        if (baseActivity.getStorageManager()
                .isAttachingLocation()) {
            // Append the location
            // Fetch the address if applicable
            String locationAddress = baseActivity.getStorageManager()
                    .getCurrentLocationAddress();

            String location;
            if (locationAddress == null) {
                location = baseActivity.getLocationManager()
                        .getFormattedLocation();
            } else {
                location = baseActivity.getLocationManager()
                        .getFormattedLocationWithName(locationAddress);
            }

            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + location);
            bulb = bulb.concat(location);
        }
        return bulb;
    }

}
