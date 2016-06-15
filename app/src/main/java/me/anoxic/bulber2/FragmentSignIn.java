package me.anoxic.bulber2;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

        final Button button = (Button) view.findViewById(R.id.signin);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                button.setEnabled(false);
                final BaseActivity app = (BaseActivity) getActivity();
                final ICallback<Void> serviceCreated = new DefaultCallback<Void>(getActivity()) {
                    @Override
                    public void success(final Void result) {
                        navigateToRoot();
                        button.setEnabled(true);
                    }
                };

                try {
                    app.getOneDriveClient();
                    navigateToRoot();
                    button.setEnabled(true);
                } catch (final UnsupportedOperationException ignored) {
                    app.createOneDriveClient(getActivity(), serviceCreated);
                }
            }
        });

        return view;
    }

    private void navigateToRoot() {
        Toast.makeText(getContext(), "Signed in", Toast.LENGTH_LONG).show();
    }


}
