package com.indagon.kimppakyyti.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.indagon.kimppakyyti.Constants;
import com.indagon.kimppakyyti.R;
import com.indagon.kimppakyyti.tools.Callback;
import com.indagon.kimppakyyti.tools.CommonRequests;
import com.indagon.kimppakyyti.tools.DataManager;
import com.indagon.kimppakyyti.tools.SetTimeInterface;
import com.indagon.kimppakyyti.tools.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CreateWatchdogFragment.OnCreateWatchdogFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class CreateWatchdogFragment extends Fragment  {

    private OnCreateWatchdogFragmentInteractionListener mListener;

    private String startPlaceAddress = null;
    private LatLng startPlaceLatLon = null;
    private String endPlaceAddress = null;
    private LatLng endPlaceLatLon = null;
    private Constants.LocationType locationTargetType;
    private JSONObject watchdog = null;

    @BindView(R.id.create_watchdog_button) Button createWatchdogButton;
    @BindView(R.id.delete_watchdog_button) Button deleteWatchdogButton;
    @BindView(R.id.start_location_search_watchdog) TextView startLocationSearch;
    @BindView(R.id.end_location_search) TextView endLocationSearch;
    @BindView(R.id.hours_text) TextView hoursText;
    @BindView(R.id.distance_spinner) Spinner distanceSpinner;
    @BindView(R.id.start_hour) NumberPicker startHour;
    @BindView(R.id.end_hour) NumberPicker endHour;
    @BindView(R.id.day_button_container) LinearLayout dayButtonContainer;
    @BindViews({ R.id.mondayButton, R.id.tuesdayButton, R.id.wednesdayButton, R.id.thursdayButton,
            R.id.fridayButton, R.id.saturdayButton, R.id.sundayButton }) List<ToggleButton> dayButtons;

    public CreateWatchdogFragment() {
        // Required empty public constructor
    }

    public void setWatchdog(JSONObject watchdog) {
        this.watchdog = watchdog;
    }

    private void searchLocation(Constants.LocationType type) {
        this.locationTargetType = type;
        int PLACE_PICKER_REQUEST = 1;
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

        try {
            startActivityForResult(builder.build(this.getActivity()), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Place p = PlacePicker.getPlace(this.getActivity(), data);
                if (this.locationTargetType == Constants.LocationType.START) {
                    this.startLocationSearch.setText(p.getName());
                    this.startPlaceAddress = p.getAddress().toString();
                    this.startPlaceLatLon = p.getLatLng();
                } else if (this.locationTargetType == Constants.LocationType.END) {
                    this.endLocationSearch.setText(p.getName());
                    this.endPlaceAddress = p.getAddress().toString();
                    this.endPlaceLatLon = p.getLatLng();
                }
            } else if (resultCode == PlacePicker.RESULT_ERROR) {
                mListener.showText(getString(R.string.error_in_place_pick));
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                mListener.showText(getString(R.string.error_in_autocomplete));
            }
        }
    }

    @OnClick(R.id.create_watchdog_button)
    public void createWatchdog() {
        // Allow only users who have filled user info to do this
        if (!this.mListener.getDataManager().userInfoOk()) {
            this.mListener.showText(getString(R.string.user_info_not_filled));
            return;
        }
        // Validate start and end
        if (this.startPlaceAddress == null) {
            mListener.showText(getString(R.string.pick_start_place));
            return;
        }
        if (this.endPlaceAddress == null) {
            mListener.showText(getString(R.string.pick_end_place));
            return;
        }
        // Validate repeat
        String repeatDays = "";
        boolean atLeastOneChecked = false;
        for (ToggleButton day : this.dayButtons) {
            if (day.isChecked())
            {
                atLeastOneChecked = true;
                repeatDays = repeatDays.concat("1");
            } else {
                repeatDays = repeatDays.concat("0");
            }
        }
        if (!atLeastOneChecked) {
            mListener.showText(getString(R.string.select_weekday));
            return;
        }
        // Validate start and end hour
        int start = this.startHour.getValue();
        int end = this.endHour.getValue();
        if (start > end) {
            mListener.showText(getString(R.string.start_hour_greater_than_end_hour));
            return;
        }

        int walking_distance = Integer.parseInt((this.distanceSpinner.getSelectedItem().toString()));

        // If we are editing a watchdog or creating a new one
        if (this.watchdog != null) {
            try {
                // Check that some user has changed some field. Otherwise no need to update
                if (!this.someFieldHasChanged(repeatDays)) {
                    mListener.showText(getString(R.string.no_changes_detected));
                    return;
                }

                int index = this.watchdog.getInt(Constants.JSON_FIELD_INDEX);
                int id = this.watchdog.getInt(Constants.JSON_FIELD_ID);
                CommonRequests.updateWatchdog(this.getContext(), id,
                        this.startPlaceAddress, this.startPlaceLatLon,
                        this.endPlaceAddress, this.endPlaceLatLon, start, end,
                        repeatDays, walking_distance, index, new Callback<JSONObject>() {
                            @Override
                            public void callback(JSONObject result) {
                                if (result != null) {
                                    mListener.showText(getString(R.string.watchdog_updated));
                                    mListener.getDataManager().addOrUpdateWatchdog(result);
                                    CreateWatchdogFragment.this.watchdog = null;
                                    mListener.switchTab(Constants.Tab.HOME, true);
                                } else {
                                    mListener.showText(getString(R.string.updating_watchdog_failed));
                                }
                            }
                        });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            CommonRequests.createWatchdog(this.getContext(),
                    this.startPlaceAddress, this.startPlaceLatLon,
                    this.endPlaceAddress, this.endPlaceLatLon, start, end,
                    repeatDays, walking_distance, 0, new Callback<JSONObject>() {
                        @Override
                        public void callback(JSONObject result) {
                            if (result != null) {
                                mListener.showText(getString(R.string.watchdog_created));
                                mListener.getDataManager().addOrUpdateWatchdog(result);
                                CreateWatchdogFragment.this.watchdog = null;
                                mListener.switchTab(Constants.Tab.HOME, true);
                            } else {
                                mListener.showText(getString(R.string.creating_watchdog_failed));
                            }
                        }
                    });
        }
    }

    @OnClick(R.id.delete_watchdog_button)
    public void deleteWatchdog() {
        String cancelConfirmationMessage = getString(
            R.string.remove_watchdog_confirmation_message);

        // Ask for confirmation for delete
        Util.createConfirmationDialog(this.getContext(),
            cancelConfirmationMessage,
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    if (CreateWatchdogFragment.this.watchdog != null) {
                        try {
                            final int id = CreateWatchdogFragment.this.watchdog
                                    .getInt(Constants.JSON_FIELD_ID);
                            // Send delete request
                            CommonRequests.removeWatchdog(CreateWatchdogFragment.this.getContext(),
                                    id, new Callback<JSONObject>() {
                                @Override
                                public void callback(JSONObject result) {
                                    if (result != null) {
                                        mListener.getDataManager().deleteWatchdog(id);
                                        CreateWatchdogFragment.this.watchdog = null;
                                        mListener.showText(getString(R.string.watchdog_removed));
                                        mListener.switchTab(Constants.Tab.HOME, true);
                                    }
                                }
                            });
                        } catch(JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
    }

    private boolean someFieldHasChanged(String repeatDays) {
        boolean someFieldChanged = false;
        try {
            int start = this.startHour.getValue();
            int end = this.endHour.getValue();
            int walking_distance = Integer.parseInt((this.distanceSpinner.getSelectedItem().toString()));

            boolean startPlaceChanged = !this.startPlaceAddress.equals(this.watchdog
                    .getJSONObject(Constants.JSON_FIELD_START_LOCATION)
                    .getString(Constants.JSON_FIELD_ADDRESS));
            boolean endPlaceChanged = !this.endPlaceAddress.equals(this.watchdog
                    .getJSONObject(Constants.JSON_FIELD_END_LOCATION)
                    .getString(Constants.JSON_FIELD_ADDRESS));
            boolean startTimeChanged = start !=
                    this.watchdog.getInt(Constants.JSON_FIELD_START_TIME);
            boolean endTimeChanged = end !=
                    this.watchdog.getInt(Constants.JSON_FIELD_END_TIME);
            boolean walkingDistanceChanged = walking_distance !=
                    this.watchdog.getInt(Constants.JSON_FIELD_DISTANCE);
            boolean repeatChanged = !repeatDays.equals(this.watchdog
                    .getString(Constants.JSON_FIELD_WEEKDAYS));

            if (startPlaceChanged|| endPlaceChanged || startTimeChanged || endTimeChanged ||
                    walkingDistanceChanged || repeatChanged) {
                someFieldChanged = true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return someFieldChanged;
    }

    private void update() {

    }
    @OnClick(R.id.start_location_search_watchdog)
    public void searchStartLocation() {
        this.searchLocation(Constants.LocationType.START);
    }

    @OnClick(R.id.end_location_search)
    public void searchEndLocation() {
        this.searchLocation(Constants.LocationType.END);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_create_watchdog, container, false);

        ButterKnife.bind(this, v);

        // Init hour pickers
        this.startHour.setMinValue(0);
        this.startHour.setMaxValue(24);
        this.endHour.setMinValue(0);
        this.endHour.setMaxValue(24);

        // Init initial passengers spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getActivity(),
                R.array.walking_distances, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.distanceSpinner.setAdapter(adapter);

        // Init values that depend on what we are viewing, a new watchdog or existing
        try {
            if (this.watchdog != null) {
                this.deleteWatchdogButton.setVisibility(View.VISIBLE);

                JSONObject startLocation = this.watchdog.getJSONObject(
                            Constants.JSON_FIELD_START_LOCATION);
                this.startPlaceAddress = startLocation.getString(Constants.JSON_FIELD_ADDRESS);
                this.startLocationSearch.setText(this.startPlaceAddress);
                this.startPlaceLatLon = new LatLng(
                        startLocation.getDouble(Constants.JSON_FIELD_LAT),
                        startLocation.getDouble(Constants.JSON_FIELD_LON));

                JSONObject endLocation = this.watchdog.getJSONObject(
                        Constants.JSON_FIELD_END_LOCATION);
                this.endPlaceAddress = endLocation.getString(Constants.JSON_FIELD_ADDRESS);
                this.endLocationSearch.setText(this.endPlaceAddress);
                this.endPlaceLatLon = new LatLng(
                        endLocation.getDouble(Constants.JSON_FIELD_LAT),
                        endLocation.getDouble(Constants.JSON_FIELD_LON));

                this.startHour.setValue(this.watchdog.getInt(Constants.JSON_FIELD_START_TIME));
                this.endHour.setValue(this.watchdog.getInt(Constants.JSON_FIELD_END_TIME));

                int maxDistance = this.watchdog.getInt(Constants.JSON_FIELD_DISTANCE);
                int elementIndex = adapter.getPosition(Integer.toString(maxDistance));
                this.distanceSpinner.setSelection(elementIndex);

                String weekdays = this.watchdog.getString(Constants.JSON_FIELD_WEEKDAYS);
                for (int i = 0; i < 7; i++) {
                    if (weekdays.charAt(i) == '1' ) {
                        this.dayButtons.get(i).setChecked(true);
                    }
                }
                this.createWatchdogButton.setText(R.string.edit_watchdog);
            } else {
                this.createWatchdogButton.setText(R.string.add_watchdog);
                this.deleteWatchdogButton.setVisibility(View.GONE);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnCreateWatchdogFragmentInteractionListener) {
            mListener = (OnCreateWatchdogFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.update();
        getActivity().setTitle(getString(R.string.menu_option_add_watchdog));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnCreateWatchdogFragmentInteractionListener {
        void showText(String text);
        void switchTab(Constants.Tab tab, boolean addToBackStack);
        DataManager getDataManager();
    }
}
