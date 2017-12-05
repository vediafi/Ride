package com.indagon.kimppakyyti.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.indagon.kimppakyyti.Constants;
import com.indagon.kimppakyyti.R;
import com.indagon.kimppakyyti.tools.Callback;
import com.indagon.kimppakyyti.tools.CommonRequests;
import com.indagon.kimppakyyti.tools.DataManager;
import com.indagon.kimppakyyti.tools.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HomeFragment.OnHomeInteractionListener} interface
 * to handle interaction events.
 */
public class HomeFragment extends Fragment {

    @BindView(R.id.fragment_home) ScrollView rootView;
    @BindView(R.id.ride_container) LinearLayout rideContainer;
    @BindView(R.id.repeater_container) LinearLayout repeaterContainer;
    @BindView(R.id.watchdog_container) LinearLayout watchdogContainer;
    @BindView(R.id.rides_text) TextView ridesText;
    @BindView(R.id.watchdogs_text) TextView watchdogsText;


    private OnHomeInteractionListener mListener;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        // Set title
        getActivity().setTitle(R.string.app_name);
    }

    // Update data contained in this fragment if necessary. Called when user navigates to this
    // fragment.
    public void update() {
        try {
            // Add rides to ui
            JSONArray rides = mListener.getDataManager().getActiveRides();
            rideContainer.removeAllViews();

            for (int i = 0; i < rides.length(); i++)  {
                final JSONObject ride = rides.getJSONObject(i);
                final int rideId = ride.getInt(Constants.JSON_FIELD_ID);
                String myId = mListener.getDataManager().getUser().getString(
                        Constants.JSON_FIELD_USERNAME);
                ConstraintLayout item = Util.createAndFillRideElement(
                        ride, myId, this.getActivity().getLayoutInflater());

                item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mListener.viewRide(rideId, true);
                    }
                });
                rideContainer.addView(item);
            }
            // Add watchdogs
            JSONArray watchdogs = mListener.getDataManager().getWatchdogs();
            watchdogContainer.removeAllViews();

            for (int i = 0; i < watchdogs.length(); i++) {
                final JSONObject watchdog = watchdogs.getJSONObject(i);
                final int watchdogId = watchdog.getInt(Constants.JSON_FIELD_ID);
                // Fill the UI item of this watchdog
                ConstraintLayout item =
                        (ConstraintLayout) this.getActivity().getLayoutInflater()
                                .inflate(R.layout.element_watchdog, null, false);

                item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mListener.editWatchdog(watchdogId);
                    }
                });

                TextView daysField = item.findViewById(R.id.days);
                String daysBitString = watchdog.getString(Constants.JSON_FIELD_WEEKDAYS);
                String daysString = Util.getWeekdaysDescriptive(this.getContext(), daysBitString);
                daysField.setText(daysString);

                TextView timeField = item.findViewById(R.id.time);
                String start = new DecimalFormat("00").format(
                        watchdog.getInt(Constants.JSON_FIELD_START_TIME));
                String end = new DecimalFormat("00").format(
                        watchdog.getInt(Constants.JSON_FIELD_END_TIME));
                String timeText = start + "-" + end;
                timeField.setText(timeText);

                TextView startField = item.findViewById(R.id.start_location);
                String startAddress = watchdog.getJSONObject(Constants.JSON_FIELD_START_LOCATION)
                        .getString(Constants.JSON_FIELD_ADDRESS);
                startField.setText(startAddress);

                TextView endField = item.findViewById(R.id.end_location);
                String endAddress = watchdog.getJSONObject(Constants.JSON_FIELD_END_LOCATION)
                        .getString(Constants.JSON_FIELD_ADDRESS);
                endField.setText(endAddress);

                this.watchdogContainer.addView(item);
            }

            // Add repeaters
            JSONArray repeaters = mListener.getDataManager().getRepeaters();
            repeaterContainer.removeAllViews();

            for (int i = 0; i < repeaters.length(); i++) {
                JSONObject repeater = repeaters.getJSONObject(i);

                // Fill the UI item of this repeater
                final ConstraintLayout item = (ConstraintLayout) this.getActivity()
                        .getLayoutInflater().inflate(R.layout.element_repeater, null, false);

                TextView startField = item.findViewById(R.id.start);
                TextView endField = item.findViewById(R.id.end);
                TextView timeField = item.findViewById(R.id.time);
                TextView daysField = item.findViewById(R.id.days);
                TextView seatsField = item.findViewById(R.id.seats);
                Button deleteRepeaterButton = item.findViewById(R.id.delete_repeater_button);

                // Take values from repeater
                String startAddress = repeater.getJSONObject(Constants.JSON_FIELD_START_LOCATION)
                        .getString(Constants.JSON_FIELD_ADDRESS);
                String endAddress = repeater.getJSONObject(Constants.JSON_FIELD_END_LOCATION)
                        .getString(Constants.JSON_FIELD_ADDRESS);
                int initialPassengerCount = repeater.getInt(
                        Constants.JSON_FIELD_INITIAL_PASSENGER_COUNT);
                int totalSeats = repeater.getInt(Constants.JSON_FIELD_TOTAL_SEATS);
                int startHour = repeater.getInt(Constants.JSON_FIELD_START_HOUR);
                int startMinute = repeater.getInt(Constants.JSON_FIELD_START_MINUTE);
                String days = repeater.getString(Constants.JSON_FIELD_DAYS);
                final int repeaterId = repeater.getInt(Constants.JSON_FIELD_ID);

                // Calculate values for UI
                int freeSeats = totalSeats - initialPassengerCount;
                String freeSeatsString = Integer.toString(freeSeats) + "/" +
                        Integer.toString(totalSeats);
                String weekdays = Util.getWeekdaysDescriptive(this.getContext(), days);
                String startTime = Util.getTimeText(startHour, startMinute);
                String startAddressShortened = Util.shortenString(startAddress, 25);
                String endAddressShortened = Util.shortenString(endAddress, 25);

                // Set UI values
                startField.setText(startAddressShortened);
                endField.setText(endAddressShortened);
                timeField.setText(startTime);
                daysField.setText(weekdays);
                seatsField.setText(freeSeatsString);

                // Set button to delete this repeater
                deleteRepeaterButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String cancelConfirmationMessage = getString(
                                R.string.remove_repeater_confirmation_message);
                        Util.createConfirmationDialog(HomeFragment.this.getContext(),
                                cancelConfirmationMessage,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        HomeFragment.this.deleteRepeater(repeaterId, item);
                                    }
                                }
                        );
                    }
                });
                this.repeaterContainer.addView(item);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void deleteRepeater(final int repeaterId, final View item) {
        CommonRequests.removeRepeater(HomeFragment.this.getContext(),
            repeaterId, new Callback<JSONObject>() {
                @Override
                public void callback(JSONObject result) {
                    if (result != null) {
                        try {
                            if (result.getBoolean(Constants.JSON_FIELD_SUCCESS)){
                                mListener.getDataManager().deleteRepeater(repeaterId);
                                mListener.showText(getString(R.string.repeater_removed));
                                repeaterContainer.removeView(item);
                            } else {
                                int error = result.getInt("error");
                                if (error == Constants.NOT_OWNER_OF_THE_REPEATER) {
                                    mListener.getDataManager().deleteRepeater(repeaterId);
                                    mListener.showText(getString(
                                            R.string.not_owner_of_repeater));
                                    repeaterContainer.removeView(item);
                                } else {
                                    mListener.showText(getString(R.string.unknown_error));
                                    Log.e("Delete repeater", "error " +
                                            Integer.toString(error));
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        ButterKnife.bind(this, v);

        this.update();

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnHomeInteractionListener) {
            mListener = (OnHomeInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnHomeInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnHomeInteractionListener {
        void showText(String text);
        void viewRide(JSONObject ride, boolean joined);
        void viewRide(int rideId, boolean joined);
        void editWatchdog(int watchdogId);
        DataManager getDataManager();
        void switchTab(Constants.Tab tab, boolean addToBackStack);
    }
}
