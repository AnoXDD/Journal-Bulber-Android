package me.anoxic.bulber2;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.test.suitebuilder.annotation.MediumTest;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.onedrive.sdk.concurrency.ICallback;

/**
 * Created by Anoxic on 061516.
 */
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
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment, container, false);

        enableSignInButton(view);
        enablePushButton(view);
        enableDebugToggle(view);
        enableUndoButton(view);
        enableLocationCheckbox(view);
        enableLocationAddressTextMonitor(view);

        return view;
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
                        .setCurrentLocationAddress((String) s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void enableLocationCheckbox(View view) {

        final CheckBox checkBox = (CheckBox) view.findViewById(R.id.isAppendLocation);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final BaseActivity baseActivity = (BaseActivity) getActivity();
                baseActivity.getStorageManager()
                        .setAttachingLocation(isChecked);

                if (isChecked) {
                    // Retrieve the current location when checked
                    baseActivity.promptCurrentLocation();
                }
            }
        });
    }

    private void enableUndoButton(View view) {
        final Button undo = (Button) view.findViewById(R.id.undo);

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
        final Switch aSwitch = (Switch) view.findViewById(R.id.isDebug);

        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Toast.makeText(getContext(), "The toggle is now: " + isChecked, Toast.LENGTH_SHORT)
                        .show();

                final BaseActivity baseActivity = (BaseActivity) getActivity();
                baseActivity.getStorageManager()
                        .setDebugging(isChecked);
            }
        });
    }

    private void enablePushButton(final View view) {
        final Button push = (Button) view.findViewById(R.id.push);
        final EditText bulbContent = (EditText) view.findViewById(R.id.bulbContent);
        final Button signin = (Button) view.findViewById(R.id.signin);

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
        final Button signin = (Button) view.findViewById(R.id.signin);

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
                final Button button = (Button) view.findViewById(R.id.push);

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
    private void challengeSignIn(final Button signin, final String bulb) {
        setButtonThrottled(signin);
        final BaseActivity app = (BaseActivity) getActivity();
        final ICallback<Void> serviceCreated = new DefaultCallback<Void>(getActivity()) {
            @Override
            public void success(final Void result) {
                onTokenRefreshed(bulb);
                resetSigninButtonThrottled(signin);
            }
        };

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
    private void resetSigninButtonThrottled(Button button) {
        button.setText(getString(R.string.sign_in));
        button.setEnabled(true);
    }

    /**
     * Disable this button and tell the user it's doing something
     *
     * @param button the button to be throttled
     */
    private void setButtonThrottled(Button button) {
        button.setText(getString(R.string.working));
        button.setEnabled(false);
    }

    /**
     * Do something on token is refreshed
     *
     * @param bulb The content of bulb. Leave it `null` to not publish anything
     */
    private void onTokenRefreshed(String bulb) {
        //        Toast.makeText(getContext(), "Signed in", Toast.LENGTH_SHORT)                .show();
        if (bulb != null) {
            Toast.makeText(getContext(),
                    getContext().getString(R.string.bulb_push_progress),
                    Toast.LENGTH_LONG)
                    .show();
        }

        final BaseActivity app = (BaseActivity) getActivity();
        bulb = addTagsToBulb(bulb);

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
