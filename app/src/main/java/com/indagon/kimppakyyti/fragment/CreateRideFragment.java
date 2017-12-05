package com.indagon.kimppakyyti.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.indagon.kimppakyyti.Constants;
import com.indagon.kimppakyyti.R;
import com.indagon.kimppakyyti.activity.MainActivity;
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
 * {@link CreateRideFragment.OnCreateRideFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class CreateRideFragment extends Fragment
        implements SetTimeInterface {

    private TimePickerFragment timePickerFragment;
    private DatePickerFragment datePickerFragment;
    private Calendar time;

    private Place startPlace = null;
    private Place endPlace = null;
    private Constants.LocationType locationTargetType;

    @BindView(R.id.create_ride) Button createRide;
    @BindView(R.id.dateText) TextView dateText;
    @BindView(R.id.timeText) TextView timeText;
    @BindView(R.id.total_passengers) Spinner totalPassengers;
    @BindView(R.id.initial_passengers) Spinner initialPassengers;
    @BindView(R.id.start_location_favourite) ImageView startLocationFavourite;
    @BindView(R.id.end_location_favourite) ImageView endLocationFavourite;
    @BindView(R.id.start_location_search) TextView startLocationSearch;
    @BindView(R.id.end_location_search) TextView endLocationSearch;
    @BindView(R.id.repeatButton) Switch repeatButton;
    @BindView(R.id.day_button_container) LinearLayout dayButtonContainer;
    @BindViews({ R.id.mondayButton, R.id.tuesdayButton, R.id.wednesdayButton, R.id.thursdayButton,
            R.id.fridayButton, R.id.saturdayButton, R.id.sundayButton }) List<ToggleButton> dayButtons;

    @Override
    public void setTime(int hour, int minute) {
        this.time.set(Calendar.HOUR_OF_DAY, hour);
        this.time.set(Calendar.MINUTE, minute);
        this.timeText.setText(Util.getTimeText(this.time));
        this.time.set(Calendar.SECOND, 0);
        this.time.set(Calendar.MILLISECOND, 0);
    }

    @Override
    public void setDate(int year, int month, int day) {
        this.time.set(Calendar.YEAR, year);
        this.time.set(Calendar.MONTH, month);
        this.time.set(Calendar.DAY_OF_MONTH, day);
        this.dateText.setText(Util.getDateText(this.time));
    }

    private OnCreateRideFragmentInteractionListener mListener;

    public CreateRideFragment() {
        // Required empty public constructor
    }

    // Update data contained in this fragment if necessary. Called when user navigates to this
    // fragment.
    private void update() {

    }

    @OnClick(R.id.dateText)
    public void pickDate() {
        this.datePickerFragment.show(this.getActivity().getFragmentManager(), "datePicker");
    }

    @OnClick(R.id.timeText)
    public void pickTime() {
        this.timePickerFragment.show(this.getActivity().getFragmentManager(), "timePicker");
    }

    @OnCheckedChanged(R.id.repeatButton)
    public void toggleRepeat(CompoundButton button, boolean checked) {
        if (!checked) {
            this.dayButtonContainer.setVisibility(View.GONE);
        } else {
            this.dayButtonContainer.setVisibility(View.VISIBLE);
        }
    }

    @OnClick(R.id.create_ride)
    public void createRide() {
        // Allow only users who have filled user info to do this
        if (!this.mListener.getDataManager().userInfoOk()) {
            this.mListener.showText(getString(R.string.user_info_not_filled));
            return;
        }

        final int initial_passengers = Integer.parseInt(initialPassengers.getSelectedItem().toString());
        final int total_passengers = Integer.parseInt(totalPassengers.getSelectedItem().toString());

        // Validate fields
        if (startPlace == null) {
            mListener.showText(getString(R.string.pick_start_place));
            return;
        }
        if (endPlace == null) {
            mListener.showText(getString(R.string.pick_end_place));
            return;
        }
        if (initial_passengers >= total_passengers) {
            mListener.showText(getString(R.string.too_many_initial_passengers));
            return;
        }
        String repeatDays = "";
        boolean atLeastOneChecked = false;
        if (repeatButton.isChecked())
        {
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
        }

        // Set repeat
        final String repeat;
        if (atLeastOneChecked) {
            repeat = repeatDays;
        } else {
            repeat = null;
        }
        // Calculate route
        CommonRequests.getRoute(this.getActivity(), this.time.getTimeInMillis(), startPlace.getLatLng().latitude,
                startPlace.getLatLng().longitude, endPlace.getLatLng().latitude,
                endPlace.getLatLng().longitude, new Callback<JSONObject>() {
                    @Override
                    public void callback(JSONObject result) {
                        if (result != null && result.has(Constants.JSON_FIELD_POINTS)) {
                            // Create ride with the calculated route
                            int rating = 2;
                            CommonRequests.createRide(CreateRideFragment.this.getContext(), result,
                                    startPlace, endPlace, time.getTimeInMillis(),
                                    initial_passengers, total_passengers, repeat, new Callback<JSONObject>() {
                                        @Override
                                        public void callback(JSONObject result) {
                                            handleRideResult(result);
                                        }
                                    });
                        } else {
                            mListener.showText(getString(R.string.no_route_found));
                        }
                    }
                });
    }

    public void handleRideResult(JSONObject result) {
        if (result != null){
            try {
                if (result.getBoolean(Constants.JSON_FIELD_SUCCESS)) {
                    JSONObject rideJson = result.getJSONObject(Constants.JSON_FIELD_RIDE);
                    mListener.getDataManager().addOrUpdateRide(rideJson);
                    String message = getString(R.string.ride_created);
                    // If ride contains a repeater store it to a different list
                    JSONObject repeater = rideJson.optJSONObject(Constants.JSON_FIELD_REPEATER);
                    if (repeater != null) {
                        mListener.getDataManager().addOrUpdateRepeater(repeater);
                        message = getString(R.string.ride_and_repeater_created);
                    }
                    mListener.showText(message);
                    mListener.viewRide(rideJson, true);
                } else {
                    Log.d("Error in creating ride", Integer.toString(result.getInt("error")));
                    mListener.showText(getString(R.string.unknown_error));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @OnClick(R.id.start_location_favourite)
    public void selectStartLocationFromFavourites() {
        mListener.showText("Select start location from favourites");
    }

    @OnClick(R.id.end_location_favourite)
    public void selectEndLocationFromFavourites() {
        mListener.showText("Select end location from favourites");
    }

    @OnClick(R.id.start_location_search)
    public void searchStartLocation() {
        this.searchLocation(Constants.LocationType.START);
    }

    private void searchLocation(Constants.LocationType type) {
        this.locationTargetType = type;
        int PLACE_PICKER_REQUEST = 1;
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

        try {
            startActivityForResult(builder.build(this.getActivity()), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Place p = PlacePicker.getPlace(this.getActivity(), data);
                if (this.locationTargetType == Constants.LocationType.START) {
                    this.startLocationSearch.setText(p.getName());
                    this.startPlace = p;
                } else if (this.locationTargetType == Constants.LocationType.END) {
                    this.endLocationSearch.setText(p.getName());
                    this.endPlace = p;
                }
            } else if (resultCode == PlacePicker.RESULT_ERROR) {
                mListener.showText(getString(R.string.error_in_place_pick));
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                mListener.showText(getString(R.string.error_in_autocomplete));
            }
        }
    }

    @OnClick(R.id.end_location_search)
    public void searchEndLocation() {
        this.searchLocation(Constants.LocationType.END);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_create_ride, container, false);

        ButterKnife.bind(this, v);

        this.timePickerFragment = new TimePickerFragment();
        this.datePickerFragment = new DatePickerFragment();
        this.timePickerFragment.setParameters(this);
        this.datePickerFragment.setParameters(this);

        // Init initial passengers spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getActivity(),
                R.array.initial_passenger_count, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        initialPassengers.setAdapter(adapter);

        // Init total passengers spinner
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this.getActivity(),
                R.array.total_passenger_count, android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        totalPassengers.setAdapter(adapter2);

        // Init values
        this.time = Calendar.getInstance();
        this.timeText.setText(Util.getTimeText(this.time));
        this.dateText.setText(Util.getDateText(this.time));
        this.time.set(Calendar.SECOND, 0);
        this.time.set(Calendar.MILLISECOND, 0);
        this.startPlace = null;
        this.endPlace = null;
        this.startLocationSearch.setText("");
        this.endLocationSearch.setText("");

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        this.update();
        getActivity().setTitle(R.string.menu_option_create_a_ride);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnCreateRideFragmentInteractionListener) {
            mListener = (OnCreateRideFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnCreateRideFragmentInteractionListener {
        void showText(String text);
        void switchTab(Constants.Tab tab, boolean addToBackStack);
        DataManager getDataManager();
        void viewRide(JSONObject ride, boolean joined);
    }
}
