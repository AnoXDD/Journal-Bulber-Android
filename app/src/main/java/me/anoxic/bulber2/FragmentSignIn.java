package me.anoxic.bulber2;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

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

    private void enablePushButton(View view) {
        final Button push = (Button) view.findViewById(R.id.push);
        final EditText bulbContent = (EditText) view.findViewById(R.id.bulbContent);

        push.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Get the content
                String bulb = bulbContent.getText()
                        .toString();
                Toast.makeText(getContext(), "Bulb will be pushed: " + bulb, Toast.LENGTH_LONG)
                        .show();

                final BaseActivity app = (BaseActivity) getActivity();

            }
        });
    }

    private void enableSignInButton(View view) {
        final Button signin = (Button) view.findViewById(R.id.signin);

        signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                signin.setEnabled(false);
                final BaseActivity app = (BaseActivity) getActivity();
                final ICallback<Void> serviceCreated = new DefaultCallback<Void>(getActivity()) {
                    @Override
                    public void success(final Void result) {
                        navigateToRoot();
                        signin.setEnabled(true);
                    }
                };

                try {
                    app.getOneDriveClient();
                    navigateToRoot();
                    signin.setEnabled(true);
                } catch (final UnsupportedOperationException ignored) {
                    app.createOneDriveClient(getActivity(), serviceCreated);
                }
            }
        });
    }

    private void navigateToRoot() {
        Toast.makeText(getContext(), "Signed in", Toast.LENGTH_LONG)
                .show();
    }


}
