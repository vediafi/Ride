package com.indagon.kimppakyyti.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.indagon.kimppakyyti.Constants;
import com.indagon.kimppakyyti.R;
import com.indagon.kimppakyyti.tools.Callback;
import com.indagon.kimppakyyti.tools.CommonRequests;
import com.indagon.kimppakyyti.tools.DataManager;
import com.indagon.kimppakyyti.tools.SetTimeInterface;
import com.indagon.kimppakyyti.tools.StartEndLocationPicker;
import com.indagon.kimppakyyti.tools.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FindRideFragment.OnFindRideFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class FindRideFragment extends Fragment
                                implements StartEndLocationPicker, SetTimeInterface {

    private OnFindRideFragmentInteractionListener mListener;
    private Place startPlace = null;
    private Place endPlace = null;
    private Constants.LocationType locationTargetType;
    private Calendar time;
    private TimePickerFragment timePickerFragment;
    private DatePickerFragment datePickerFragment;

    @BindView(R.id.find_ride_button) Button findRide;
    @BindView(R.id.dateText) TextView dateText;
    @BindView(R.id.timeText) TextView timeText;
    @BindView(R.id.start_location_search) TextView startLocationSearch;
    @BindView(R.id.start_location_favourite) ImageView startLocationFavourite;
    @BindView(R.id.end_location_search) TextView endLocationSearch;
    @BindView(R.id.end_location_favourite) ImageView endLocationFavourite;
    @BindView(R.id.passenger_count) Spinner passengerCount;

    public FindRideFragment() {
        // Required empty public constructor
    }

    // Update data contained in this fragment if necessary. Called when user navigates to this
    // fragment.
    private void update() {

    }

    @Override
    public void setTime(int hour, int minute) {
        this.time.set(Calendar.HOUR_OF_DAY, hour);
        this.time.set(Calendar.MINUTE, minute);
        this.timeText.setText(Util.getTimeText(this.time));
    }

    @Override
    public void setDate(int year, int month, int day) {
        this.time.set(Calendar.YEAR, year);
        this.time.set(Calendar.MONTH, month);
        this.time.set(Calendar.DAY_OF_MONTH, day);
        this.dateText.setText(Util.getDateText(this.time));
    }

    @OnClick(R.id.dateText)
    public void pickDate() {
        this.datePickerFragment.show(this.getActivity().getFragmentManager(), "datePicker");
    }

    @OnClick(R.id.timeText)
    public void pickTime() {
        this.timePickerFragment.show(this.getActivity().getFragmentManager(), "timePicker");
    }

    @OnClick(R.id.find_ride_button)
    public void findRide() {
        if (this.validateFields()) {
            int passengersJoining = Integer.parseInt(this.passengerCount.getSelectedItem().toString());
            CommonRequests.getNearbyRides(this.getContext(), passengersJoining,
                    this.time.getTimeInMillis(), this.startPlace, this.endPlace, new Callback<JSONArray>() {
                        @Override
                        public void callback(JSONArray result) {
                            handleSearchResults(result);
                        }
                    });
        }
    }

    public void handleSearchResults(JSONArray results) {
        if (results != null) {
            if (results.length() > 0) {
                mListener.showSearchResults(results);
            } else {
                mListener.showText(getString(R.string.no_search_results));
            }
        }
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

    @OnClick(R.id.start_location_search)
    public void selectStartLocation() {
        this.searchLocation(Constants.LocationType.START);
    }

    @OnClick(R.id.end_location_search)
    public void selectEndLocation() {
        this.searchLocation(Constants.LocationType.END);
    }

    @OnClick(R.id.start_location_favourite)
    public void selectStartLocationFromFavourites() {
        mListener.showText("Select start location from favourites");
    }

    @OnClick(R.id.end_location_favourite)
    public void selectEndLocationFromFavourites() {
        mListener.showText("Select end location from favourites");
    }

    public boolean validateFields()
    {
        if (this.startPlace == null) {
            mListener.showText(getString(R.string.start_location_missing));
            return false;
        }
        if (this.endPlace == null) {
            mListener.showText(getString(R.string.end_location_missing));
            return false;
        }
        Calendar currentTime = Calendar.getInstance();
        currentTime.set(Calendar.SECOND, 0);
        currentTime.set(Calendar.MILLISECOND, 0);
        if (this.time.getTimeInMillis() < currentTime.getTimeInMillis()) {
            mListener.showText(getString(R.string.time_too_soon));
            return false;
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_find_ride, container, false);

        ButterKnife.bind(this, v);

        this.timePickerFragment = new TimePickerFragment();
        this.datePickerFragment = new DatePickerFragment();
        this.timePickerFragment.setParameters(this);
        this.datePickerFragment.setParameters(this);

        this.time = Calendar.getInstance();
        this.timeText.setText(Util.getTimeText(this.time));
        this.dateText.setText(Util.getDateText(this.time));
        this.time.set(Calendar.SECOND, 0);
        this.time.set(Calendar.MILLISECOND, 0);

        // Init initial passengers spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getActivity(),
                R.array.initial_passenger_count, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        passengerCount.setAdapter(adapter);

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFindRideFragmentInteractionListener) {
            mListener = (OnFindRideFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.menu_option_find_ride);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFindRideFragmentInteractionListener {
        void showText(String text);
        void showSearchResults(JSONArray results);
    }
}
