package com.indagon.kimppakyyti.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;
import com.indagon.kimppakyyti.R;
import com.indagon.kimppakyyti.Constants;
import com.indagon.kimppakyyti.tools.Callback;
import com.indagon.kimppakyyti.tools.CommonRequests;
import com.indagon.kimppakyyti.tools.DataManager;
import com.indagon.kimppakyyti.tools.DownloadImageTask;
import com.indagon.kimppakyyti.tools.Util;

import java.util.List;
import java.util.Stack;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SettingsFragment.OnSettingsInteractionListener} interface
 * to handle interaction events.
 */
public class SettingsFragment extends Fragment {
    private boolean profilePictureUpdated = false;
    private OnSettingsInteractionListener mListener;

    private Stack<Runnable> updateQueue = new Stack<>();

    @BindView(R.id.editEmail) EditText editEmailField;
    @BindView(R.id.editPhone) EditText editPhoneField;
    @BindView(R.id.editDescription) EditText editDescriptionField;
    @BindView(R.id.profile_picture) ImageView profilePicture;
    @BindView(R.id.saveSettingsButton) Button saveSettingsButton;

    private static final int REQUEST_CODE_PICKER = 722;

    public SettingsFragment() {
        // Required empty public constructor
    }

    // Update data contained in this fragment if necessary. Called when user navigates to settings
    // fragment.
    public void update() {
        JSONObject user = mListener.getDataManager().getUser();
        try {
            String email = user.getString(Constants.JSON_FIELD_EMAIL);
            String phone = user.getString(Constants.JSON_FIELD_PHONE_NUMBER);
            String description = user.getString(Constants.JSON_FIELD_DESCRIPTION);

            this.editEmailField.setText(email);
            this.editPhoneField.setText(phone);
            this.editDescriptionField.setText(description);

            // Download and set image to profilePicture ImageView
            String imageUrl = Constants.IMAGE_BASE_URL +
                    user.getString(Constants.JSON_FIELD_PICTURE_URL);
            new DownloadImageTask(this.profilePicture)
                    .execute(imageUrl);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.saveSettingsButton)
    public void save() {
        if (validateSettings()) {
            this.updateSettings();
        }
    }

    @OnClick(R.id.profile_picture)
    public void pickImage() {
        ImagePicker.create(this) // Activity or Fragment
                //.returnAfterFirst(true) // set whether pick or camera action should return
                // immediate result or not. For pick image only work on single mode
                //.folderMode(true) // folder mode (false by default)
                .single() // single mode
                .limit(1) // max images can be selected (99 by default)
                .showCamera(true) // show camera or not (true by default)
                //.imageDirectory("Camera") // directory name for captured image
                // ("Camera" folder by default)
                .enableLog(false) // disabling log
                .start(this.REQUEST_CODE_PICKER);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICKER) {
            List<Image> images = ImagePicker.getImages(data);
            if (images != null && !images.isEmpty()) {
                this.profilePicture.setImageBitmap(
                        BitmapFactory.decodeFile(images.get(0).getPath()));
                this.profilePictureUpdated = true;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void updateSettings() {

        String emailCurrent = "";
        String phoneCurrent = "";
        String descriptionCurrent = "";

        try {
            JSONObject user = mListener.getDataManager().getUser();
            emailCurrent = user.getString(Constants.JSON_FIELD_EMAIL);
            phoneCurrent = user.getString(Constants.JSON_FIELD_PHONE_NUMBER);
            descriptionCurrent = user.getString(Constants.JSON_FIELD_DESCRIPTION);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        final String emailFieldValue = this.editEmailField.getText().toString();
        final String phoneFieldValue = this.editPhoneField.getText().toString();
        final String descriptionFieldValue = this.editDescriptionField.getText().toString();

        // If user image has changed update it to server and to local memory
        if (profilePictureUpdated) {
            updateQueue.add(new Runnable() {
                @Override
                public void run() {
                    CommonRequests.updateProfilePicture(SettingsFragment.this.getActivity(),
                            profilePicture, new Callback<JSONObject>() {
                        @Override
                        public void callback(JSONObject result) {
                                if (result != null) {
                                    Log.e("Update image response", result.toString());
                                    //Store new user data to sharedPrefs
                                    mListener.getDataManager()
                                            .setUser(result.toString());
                                    SettingsFragment.this.profilePictureUpdated = false;
                                    mListener.switchTab(Constants.Tab.PROFILE, true);
                                }
                            }
                        });
                }
            });
        }

        // If email or phone has changed update it to server and to local memory
        if (!emailFieldValue.equals(emailCurrent) ||
                !phoneFieldValue.equals(phoneCurrent) ||
                !descriptionFieldValue.equals(descriptionCurrent)) {
            updateQueue.add(new Runnable() {
                public void run() {
                CommonRequests.updateUserInfo(SettingsFragment.this.getActivity(),
                        emailFieldValue, phoneFieldValue, descriptionFieldValue,
                        new Callback<JSONObject>() {
                    @Override
                    public void callback(JSONObject result) {
                        if (result != null) {
                            try {
                                if (result.getBoolean(Constants.JSON_FIELD_SUCCESS)) {
                                    result = result.getJSONObject(
                                            Constants.JSON_FIELD_USER);

                                    //Store new user data to sharedPrefs
                                    mListener.getDataManager()
                                            .setUser(result.toString());
                                    mListener.switchTab(Constants.Tab.PROFILE, true);
                                } else {
                                    mListener.showText(getString(
                                            R.string.updating_user_info_failed));
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            mListener.showText(getString(R.string.unknown_error));
                        }
                    }
                });
                }
            });
        }

        if (updateQueue.isEmpty()) {
            mListener.showText(getString(R.string.no_changes_detected));
        } else {
            // Run requests
            while (!updateQueue.isEmpty()) {
                updateQueue.pop().run();
            }
        }
    }

    private boolean validateSettings()
    {
        if (!Util.isValidEmail(editEmailField.getText().toString())) {
            mListener.showText(getString(R.string.email_not_valid));
            return false;
        } else if (!Util.isValidPhoneNumber(editPhoneField.getText().toString())) {
            mListener.showText(getString(R.string.phone_not_valid));
            return false;
        } else if (editDescriptionField.length() == 0) {
            mListener.showText(getString(R.string.description_empty));
            return false;
        }
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        ButterKnife.bind(this, v);

        update();

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSettingsInteractionListener) {
            mListener = (OnSettingsInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnSettingsInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(getString(R.string.menu_option_settings));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnSettingsInteractionListener {
        void showText(String text);
        void switchTab(Constants.Tab tab, boolean addToBackStack);
        DataManager getDataManager();
    }
}
