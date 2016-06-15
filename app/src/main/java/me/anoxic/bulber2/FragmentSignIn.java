package me.anoxic.bulber2;

import android.app.Fragment;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

        return view;
    }

    private void enableDebugToggle(View view) {
        final Switch aSwitch = (Switch) view.findViewById(R.id.isDebug);

        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Toast.makeText(getContext(), "The toggle is now: " + isChecked, Toast.LENGTH_SHORT)
                        .show();
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
        signin.setEnabled(false);
        final BaseActivity app = (BaseActivity) getActivity();
        final ICallback<Void> serviceCreated = new DefaultCallback<Void>(getActivity()) {
            @Override
            public void success(final Void result) {
                onTokenRefreshed(bulb);
                signin.setEnabled(true);
            }
        };

        try {
            app.getOneDriveClient();
            onTokenRefreshed(bulb);
            signin.setEnabled(true);
        } catch (final UnsupportedOperationException ignored) {
            app.createOneDriveClient(getActivity(), serviceCreated);
        }
    }

    /**
     * Do something on token is refreshed
     *
     * @param bulb The content of bulb. Leave it `null` to not publish anything
     */
    private void onTokenRefreshed(final String bulb) {
        //        Toast.makeText(getContext(), "Signed in", Toast.LENGTH_SHORT)                .show();
        Toast.makeText(getContext(), "Bulb will be pushed: " + bulb, Toast.LENGTH_LONG)
                .show();

        final BaseActivity app = (BaseActivity) getActivity();
        app.attemptPublishBulb(bulb);
    }


}
