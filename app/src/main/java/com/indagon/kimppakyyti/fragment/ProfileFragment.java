package com.indagon.kimppakyyti.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.indagon.kimppakyyti.Constants;
import com.indagon.kimppakyyti.R;
import com.indagon.kimppakyyti.tools.DataManager;
import com.indagon.kimppakyyti.tools.DownloadImageTask;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ProfileFragment.OnProfileFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class ProfileFragment extends Fragment {
    private OnProfileFragmentInteractionListener mListener;

    private JSONObject viewedUser = null;

    @BindView(R.id.name) TextView name;
    @BindView(R.id.phone) TextView phone;
    @BindView(R.id.email) TextView email;
    @BindView(R.id.description) TextView description;
    @BindView(R.id.image) ImageView image;
    @BindView(R.id.edit_profile_button) Button editProfileButton;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @OnClick(R.id.edit_profile_button)
    public void editProfile() {
        this.mListener.switchTab(Constants.Tab.SETTINGS, true);
    }

    // This method is called from main activity when we are selecting a user to view
    public void setViewedUser(JSONObject user) {
        this.viewedUser = user;
    }

    public boolean viewingOtherUser() {
        if (this.viewedUser != null) {
            return true;
        }
        return false;
    }

    // Update data contained in this fragment if necessary. Called when user navigates to this
    // fragment.
    public void update() {
        String nameString = "";
        String emailString = "";
        String phoneString = "";
        String descriptionString = "";
        String imageUrl = "";

        // If viewedUser = null we are viewing our own profile and not other user from the service
        if (viewedUser != null) {
            this.editProfileButton.setVisibility(View.GONE);
            try {
                nameString = String.format("%s %s",
                        this.viewedUser.getString(Constants.JSON_FIELD_FIRST_NAME),
                        this.viewedUser.getString(Constants.JSON_FIELD_LAST_NAME));
                emailString = viewedUser.getString(Constants.JSON_FIELD_EMAIL);
                phoneString = viewedUser.getString(Constants.JSON_FIELD_PHONE_NUMBER);
                descriptionString = viewedUser.getString(Constants.JSON_FIELD_DESCRIPTION);
                imageUrl = Constants.IMAGE_BASE_URL +
                        this.viewedUser.getString(Constants.JSON_FIELD_PICTURE_URL);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            this.editProfileButton.setVisibility(View.VISIBLE);
            JSONObject me = mListener.getDataManager().getUser();
            try {
                nameString = String.format("%s %s",
                        me.getString(Constants.JSON_FIELD_FIRST_NAME),
                        me.getString(Constants.JSON_FIELD_LAST_NAME));
                emailString = me.getString(Constants.JSON_FIELD_EMAIL);
                phoneString = me.getString(Constants.JSON_FIELD_PHONE_NUMBER);
                descriptionString = me.getString(Constants.JSON_FIELD_DESCRIPTION);
                imageUrl = Constants.IMAGE_BASE_URL +
                        me.getString(Constants.JSON_FIELD_PICTURE_URL);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        name.setText(nameString);
        description.setText(descriptionString);

        // Make email click to send email
        email.setText(emailString);
        final String emailUrl = "mailto:" + emailString;
        email.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                Uri uri = Uri.parse(emailUrl);
                intent.setData(uri);
                startActivity(intent);
            }
        });

        // Make phone number click to make a call. Only if feature is available
        phone.setText(phoneString);
        final String phoneNumber = "tel:" + phoneString;
        phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int permissionCheck = ContextCompat.checkSelfPermission(
                        ProfileFragment.this.getContext(),
                        Manifest.permission.CALL_PHONE);

                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            ProfileFragment.this.getActivity(),
                            new String[]{Manifest.permission.CALL_PHONE},
                            123);
                } else {
                    Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse(phoneNumber));
                    startActivity(intent);
                }
            }
        });

        // Download and set image to profilePicture ImageView
        new DownloadImageTask(this.image)
                .execute(imageUrl);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        ButterKnife.bind(this, v);

        update();

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnProfileFragmentInteractionListener) {
            mListener = (OnProfileFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.setTitle();
    }

    // Set title of the activity to match this frament. Called from onResume()
    private void setTitle() {
        String title = "";
        if (this.viewingOtherUser()) {
            try {
                title = String.format("%s %s",
                        this.viewedUser.getString(Constants.JSON_FIELD_FIRST_NAME),
                        this.viewedUser.getString(Constants.JSON_FIELD_LAST_NAME));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            title = getString(R.string.profile);
        }
        getActivity().setTitle(title);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnProfileFragmentInteractionListener {
        void showText(String text);
        DataManager getDataManager();
        void switchTab(Constants.Tab tab, boolean addToBackStack);
    }
}
