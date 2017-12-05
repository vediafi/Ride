package com.indagon.kimppakyyti.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.indagon.kimppakyyti.Constants;
import com.indagon.kimppakyyti.R;
import com.indagon.kimppakyyti.tools.Callback;
import com.indagon.kimppakyyti.tools.CommonRequests;
import com.indagon.kimppakyyti.tools.DataManager;
import com.indagon.kimppakyyti.tools.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static java.lang.Math.min;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link RideFragment.OnRideFragmentInteractionListener} interface
 * to handle interaction events.
 */
// This Fragment shows a ride that you are part of
public class RideFragment extends Fragment implements OnMapReadyCallback{
    private OnRideFragmentInteractionListener mListener;

    private JSONObject ride;
    private boolean joined;

    private GoogleMap map;
    private PolylineOptions mapRoute;
    private LatLngBounds mapBounds;
    private LatLng mapRouteStart;
    private LatLng mapRouteEnd;
    private String mapRouteStartName;
    private String mapRouteEndName;
    private JSONArray mapRouteStops;

    @BindView(R.id.ride_status) TextView rideStatus;
    @BindView(R.id.ride_id) TextView rideIdField;
    @BindView(R.id.my_status) TextView myStatus;
    @BindView(R.id.total_length) TextView totalLength;
    @BindView(R.id.total_duration) TextView totalDuration;
    @BindView(R.id.my_cost) TextView myCost;
    @BindView(R.id.my_length) TextView myLength;
    @BindView(R.id.my_start) TextView myStart;
    @BindView(R.id.my_stop) TextView myStop;
    @BindView(R.id.my_duration) TextView myDuration;
    @BindView(R.id.departure_time) TextView departureTime;
    @BindView(R.id.arrival_time) TextView arrivalTime;
    @BindView(R.id.leave_ride_button) Button leaveRideButton;
    @BindView(R.id.cancel_ride_button) Button cancelRideButton;
    @BindView(R.id.finalize_ride_button) Button finalizeRideButton;
    @BindView(R.id.done_ride_button) Button doneRideButton;
    @BindView(R.id.join_ride_button) Button joinRideButton;
    @BindView(R.id.role_image_driver) ImageView roleImageDriver;
    @BindView(R.id.role_image_rider) ImageView roleImageRider;
    @BindView(R.id.passengers) LinearLayout passengerContainer;
    @BindView(R.id.map) MapView mapView;
    @BindView(R.id.repeater_days) TextView repeaterDays;
    @BindView(R.id.repeater_layout) ConstraintLayout repeaterLayout;
    @BindView(R.id.waypointContainer) LinearLayout waypointContainer;
    @BindView(R.id.Waypoints) TextView waypointText;

    public RideFragment() {
        // Required empty public constructor
    }

    public void setRide(JSONObject ride, boolean joined) {
        this.joined = joined;
        this.ride = ride;
    }

    public void update() {
        try {
            JSONObject route = ride.getJSONObject(Constants.JSON_FIELD_ROUTE);
            JSONArray stops = route.getJSONArray(Constants.JSON_FIELD_STOPS);
            Boolean modifiable = ride.getBoolean(Constants.JSON_FIELD_MODIFIABLE);
            Boolean active = this.ride.getBoolean(Constants.JSON_FIELD_ACTIVE);
            String myId = mListener.getDataManager().getUser().getString(
                    Constants.JSON_FIELD_USERNAME);
            String owner = ride.getString(Constants.JSON_FIELD_OWNER);
            JSONObject repeater = ride.optJSONObject(Constants.JSON_FIELD_REPEATER);
            // Find out if I'm the driver or passenger and
            // calculate start, stop, time and distance differently whether im a driver or passenger
            if (myId.equals(owner)) {
                if (modifiable) {
                    this.cancelRideButton.setVisibility(View.VISIBLE);
                    this.finalizeRideButton.setVisibility(View.VISIBLE);
                    this.doneRideButton.setVisibility(View.INVISIBLE);
                } else {
                    this.cancelRideButton.setVisibility(View.INVISIBLE);
                    this.finalizeRideButton.setVisibility(View.INVISIBLE);
                    if (active) {
                        this.doneRideButton.setVisibility(View.VISIBLE);
                    } else {
                        this.doneRideButton.setVisibility(View.INVISIBLE);
                    }
                }
                this.leaveRideButton.setVisibility(View.INVISIBLE);
                this.joinRideButton.setVisibility(View.INVISIBLE);
                roleImageDriver.setVisibility(View.VISIBLE);
                roleImageRider.setVisibility(View.INVISIBLE);

                // Start time
                long startTimeMillis = ride.getLong(Constants.JSON_FIELD_START_TIME);
                this.departureTime.setText(Util.timeMillisToString(startTimeMillis));

                // Start address
                String startAddress = route.getJSONObject(Constants.JSON_FIELD_START_LOCATION)
                        .getString(Constants.JSON_FIELD_ADDRESS);
                this.myStart.setText(startAddress);

                // End time
                long endTimeMillis = startTimeMillis + 1000 *
                        route.getLong(Constants.JSON_FIELD_TIME);
                this.arrivalTime.setText(Util.timeMillisToString(endTimeMillis));

                // End address
                JSONObject lastStop = stops.getJSONObject(stops.length() - 1 );
                this.myStop.setText(lastStop.getString(Constants.JSON_FIELD_ADDRESS));

                // Length of drivers route is the whole ride
                this.myLength.setText(Util.distanceToString(route.getDouble(
                        Constants.JSON_FIELD_DISTANCE)));

                // Duration of drivers route is the whole ride
                this.myDuration.setText(DateUtils.formatElapsedTime(route.getLong(
                        Constants.JSON_FIELD_TIME)));

            } else {
                roleImageDriver.setVisibility(View.INVISIBLE);
                roleImageRider.setVisibility(View.VISIBLE);
                this.cancelRideButton.setVisibility(View.INVISIBLE);
                this.doneRideButton.setVisibility(View.INVISIBLE);
                this.finalizeRideButton.setVisibility(View.INVISIBLE);

                if (this.joined) {
                    this.joinRideButton.setVisibility(View.INVISIBLE);
                    if (modifiable) {
                        this.leaveRideButton.setVisibility(View.VISIBLE);
                    } else {
                        this.leaveRideButton.setVisibility(View.INVISIBLE);
                    }
                } else {
                    if (modifiable) {
                        this.joinRideButton.setVisibility(View.VISIBLE);
                    } else {
                        this.joinRideButton.setVisibility(View.INVISIBLE);;
                    }
                    this.leaveRideButton.setVisibility(View.INVISIBLE);
                }

                // Find riders join stop and last stop from stops
                JSONObject myEndStop = null;
                JSONObject myStartStop = null;
                int myEndStopIndex = 0;
                for (int i = 0; i < stops.length() ; i ++)
                {
                    JSONObject stop = stops.getJSONObject(i);
                    if (stop.getString(Constants.JSON_FIELD_USER).equals(myId) &&
                            !stop.getBoolean(Constants.JSON_FIELD_LEAVING)) {
                        myStartStop = stop;
                    } else if (stop.getString(Constants.JSON_FIELD_USER).equals(myId) &&
                            stop.getBoolean(Constants.JSON_FIELD_LEAVING)) {
                        myEndStop = stop;
                        myEndStopIndex = i;
                        break;
                    }
                }

                // Start address
                this.myStart.setText(myStartStop.getString(Constants.JSON_FIELD_ADDRESS));

                // End address
                this.myStop.setText(myEndStop.getString(Constants.JSON_FIELD_ADDRESS));

                // Find time when user joins and leaves and how long he travels
                // Start time is time from start to my join stop
                // End time is start time + time from start to my end stop
                int timeFromStartToStartStopSeconds = 0;
                int timeFromStartToEndStopSeconds = 0;
                int rideDurationSeconds = 0;
                double rideDistance = 0.0d;
                boolean startStopFound = false;
                for (int i = 0; i <= myEndStopIndex ; i++)
                {
                    JSONObject stop = stops.getJSONObject(i);
                    timeFromStartToEndStopSeconds += stop.getInt(
                            Constants.JSON_FIELD_TIME_FROM_LAST_STOP);
                    if (!startStopFound) {
                        timeFromStartToStartStopSeconds += stop.getInt(
                                Constants.JSON_FIELD_TIME_FROM_LAST_STOP);
                    } else {
                        rideDurationSeconds += stop.getInt(
                                Constants.JSON_FIELD_TIME_FROM_LAST_STOP);
                        rideDistance += stop.getInt(Constants.JSON_FIELD_DISTANCE_FROM_LAST_STOP);
                    }
                    if (stop.getString(Constants.JSON_FIELD_USER).equals(myId) &&
                            !stop.getBoolean(Constants.JSON_FIELD_LEAVING)) {
                        startStopFound = true;
                    } else if (stop.getString(Constants.JSON_FIELD_USER).equals(myId) &&
                            stop.getBoolean(Constants.JSON_FIELD_LEAVING)) {
                        break;
                    }
                }

                // Set start time
                long startTimeMillis = ride.getLong(Constants.JSON_FIELD_START_TIME);
                long rideStartMillis = startTimeMillis + 1000 * timeFromStartToStartStopSeconds;
                this.departureTime.setText(Util.timeMillisToString(rideStartMillis));

                // Set end time
                long rideEndMillis = startTimeMillis + 1000 * timeFromStartToEndStopSeconds;
                this.arrivalTime.setText(Util.timeMillisToString(rideEndMillis));

                // Set duration of my ride
                this.myDuration.setText(DateUtils.formatElapsedTime(rideDurationSeconds));

                // Set my route length
                this.myLength.setText(Util.distanceToString(rideDistance));
            }

            rideIdField.setText(Long.toString(this.ride.getLong(Constants.JSON_FIELD_ID)));

            // If I'm the owner the ride is free. For everyone else theres a share
            if (myId.equals(owner)) {
                this.myCost.setText(Util.priceToString(0));
            } else {
                //Find the price of my share
                int my_route_cost = 0;
                JSONArray shares = ride.optJSONArray(Constants.JSON_FIELD_SHARES);
                if (shares != null) {
                    for (int j = 0; j < shares.length(); j++) {
                        JSONObject share = shares.getJSONObject(j);
                        String shareUser = share.getString(Constants.JSON_FIELD_USER);
                        if (shareUser.equals(myId)) {
                            my_route_cost = share.getInt(Constants.JSON_FIELD_PRICE);
                            break;
                        }
                    }
                } else {
                    my_route_cost = ride.getInt(Constants.JSON_FIELD_PRICE);
                }
                this.myCost.setText(Util.priceToString(my_route_cost));
            }

            // Set total length
            this.totalLength.setText(Util.distanceToString(route.getDouble(
                    Constants.JSON_FIELD_DISTANCE)));

            // Set duration of whole ride
            this.totalDuration.setText(DateUtils.formatElapsedTime(route.getInt(
                    Constants.JSON_FIELD_TIME)));

            // Add passengers
            passengerContainer.setVisibility(View.VISIBLE);
            passengerContainer.removeAllViews(); // Clear all passengers from list
            JSONArray passengers = ride.getJSONArray(Constants.JSON_FIELD_PASSENGERS);
            for (int j = 0; j < passengers.length(); j++) {
                final JSONObject passenger = passengers.getJSONObject(j);
                final String passengerUsername = passenger.getString(Constants.JSON_FIELD_USERNAME);
                final String passengerName = String.format("%s %s",
                        passenger.getString(Constants.JSON_FIELD_FIRST_NAME),
                        passenger.getString(Constants.JSON_FIELD_LAST_NAME));

                // If I have not yet joined this ride don't show me in the passengers list
                if (!joined && myId.equals(passengerUsername))
                    continue;

                ConstraintLayout item = (ConstraintLayout) this.getActivity().getLayoutInflater()
                        .inflate(R.layout.element_user, null, false);
                TextView usernameField = item.findViewById(R.id.username);
                Button kickButton = item.findViewById(R.id.kick_button);
                ImageView ownerImage = item.findViewById(R.id.owner_image);
                ImageView passengerImage = item.findViewById(R.id.passenger_image);

                // Show leader symbol at ride owner
                String ownerId = ride.getString(Constants.JSON_FIELD_OWNER);
                if (passengerUsername.equals(ownerId)) {
                    ownerImage.setVisibility(View.VISIBLE);
                    passengerImage.setVisibility(View.INVISIBLE);
                } else {
                    ownerImage.setVisibility(View.INVISIBLE);
                    passengerImage.setVisibility(View.VISIBLE);
                }

                // When user clicks a users name, view his profile
                usernameField.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mListener.viewUser(passenger);
                    }
                });

                // Only ride owner can kick users if ride is modifiable
                // If the user is not me and im the ride owner
                final int rideId = ride.getInt("id");
                if (!passengerUsername.equals(myId) && owner.equals(myId) && modifiable) {
                    kickButton.setVisibility(View.VISIBLE);

                    kickButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                        // Ask for confirmation on kick
                            final String kickUserConfirmationMessage = String.format(getString(
                                R.string.kick_user_confirmation_message), passengerName);
                            Util.createConfirmationDialog(RideFragment.this.getContext(),
                                kickUserConfirmationMessage,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        CommonRequests.kickUserFromRide(RideFragment.this.getContext(),
                                            rideId, passengerUsername, new Callback<JSONObject>() {
                                                @Override
                                                public void callback(JSONObject result) {
                                                if (result == null) {
                                                    Log.e("KICK error", "Kick user returned null");
                                                    return;
                                                }
                                                try {
                                                    if (result.getBoolean(
                                                            Constants.JSON_FIELD_SUCCESS)) {
                                                        JSONObject rideJson = result
                                                                .getJSONObject("ride");
                                                        mListener.getDataManager()
                                                                .addOrUpdateRide(rideJson);
                                                        RideFragment.this.setRide(rideJson, true);
                                                        RideFragment.this.update();
                                                        mListener.showText(String.format(getString(
                                                                R.string.kicked_user_from_ride),
                                                                passengerName));
                                                    } else {
                                                        int error = result.getInt(
                                                                Constants.JSON_FIELD_ERROR);
                                                        if (error == Constants.RIDE_NOT_FOUND) {
                                                            mListener.showText(getString(R.string.ride_not_found));
                                                        } else if (error ==
                                                                Constants.RIDE_IS_NOT_MODIFIABLE) {
                                                            mListener.showText(getString(
                                                                    R.string.ride_cannot_be_modified));
                                                        } else if (error ==
                                                                Constants.USER_NOT_IN_RIDE) {
                                                            mListener.showText(getString(
                                                                    R.string.user_not_on_ride));
                                                        } else if (error ==
                                                                Constants.NOT_OWNER_OF_THE_RIDE) {
                                                            mListener.showText(getString(
                                                                    R.string.not_owner_of_ride));
                                                        }
                                                    }
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                                }
                                            });
                                    }
                                }
                            );
                        }
                    });
                } else {
                    kickButton.setVisibility(View.INVISIBLE);
                }
                usernameField.setText(passengerName);
                passengerContainer.addView(item);
            }

            // Set ride status (whether it is open, finalized, done)
            if (modifiable) {
                this.rideStatus.setText(Constants.RIDE_STATUS_OPEN);
            } else {
                if (active) {
                    this.rideStatus.setText(Constants.RIDE_STATUS_CLOSED);
                } else {
                    this.rideStatus.setText(Constants.RIDE_STATUS_DONE);
                }
            }

            // Set my status on ride: Joined, Owner, Not joined
            if (this.joined) {
                if (owner.equals(myId)) {
                    this.myStatus.setText(Constants.MY_RIDE_STATUS_OWNER);
                } else {
                    this.myStatus.setText(Constants.MY_RIDE_STATUS_JOINED);
                }
            } else {
                this.myStatus.setText(Constants.MY_RIDE_STATUS_NOT_JOINED);
            }

            // Show repeater data if this ride was created with a repeater and I'm the owner
            if (myId.equals(owner) && repeater != null) {
                String days = repeater.getString(Constants.JSON_FIELD_DAYS);
                repeaterDays.setText(Util.getWeekdaysDescriptive(this.getContext(), days));
                // When user clicks the repeater area take him to view his repeaters
                repeaterLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mListener.switchTab(Constants.Tab.HOME, true);
                        // TODO scroll to repeaters section
                    }
                });
            } else {
                repeaterLayout.setVisibility(View.GONE);
            }

            // Set map route and markers
            JSONArray points = route.getJSONArray("points");
            ArrayList<LatLng> routePoints = new ArrayList<>();
            for (int p = 0; p < points.length() - 1; p += 2) {
                double lat = points.getDouble(p);
                double lon = points.getDouble(p+1);
                routePoints.add(new LatLng(lat,lon));
            }
            JSONObject firstStop = route.getJSONObject(Constants.JSON_FIELD_START_LOCATION);
            LatLng start = new LatLng(firstStop.getDouble(Constants.JSON_FIELD_LAT),
                    firstStop.getDouble(Constants.JSON_FIELD_LON));

            JSONObject lastStop = stops.getJSONObject(stops.length() - 1);
            LatLng end = new LatLng(lastStop.getDouble(Constants.JSON_FIELD_LAT),
                    lastStop.getDouble(Constants.JSON_FIELD_LON));

            LatLng southWestCorner = new LatLng(
                    route.getDouble(Constants.JSON_FIELD_BB_SOUTHWEST_LAT),
                    route.getDouble(Constants.JSON_FIELD_BB_SOUTHWEST_LON));
            LatLng northEastCorner = new LatLng(
                    route.getDouble(Constants.JSON_FIELD_BB_NORTHEAST_LAT),
                    route.getDouble(Constants.JSON_FIELD_BB_NORTHEAST_LON));

            // Find pickup/drop points and add users name to them
            JSONArray stopPoints = new JSONArray();
            for (int i = 0; i < stops.length() - 1; i++) {
                JSONObject stop = stops.getJSONObject(i);
                String passengerUsername = stop.getString(Constants.JSON_FIELD_USER);
                String passengerFullName = "";
                for (int j = 0; j < passengers.length(); j++) {
                    JSONObject passenger = passengers.getJSONObject(j);
                    if (passenger.getString(Constants.JSON_FIELD_USERNAME)
                            .equals(passengerUsername)) {
                        passengerFullName = passengers.getJSONObject(j)
                                .getString(Constants.JSON_FIELD_FIRST_NAME);
                        // TODO we are using only first name but variable name is full name
                    }
                }
                stop.put("user_full_name", passengerFullName);
                stopPoints.put(stops.get(i));
            }

            // Add stop/pickup points to list
            this.waypointContainer.removeAllViews();
            if (joined && myId.equals(owner)) {
                this.waypointContainer.setVisibility(View.VISIBLE);
                this.waypointText.setVisibility(View.VISIBLE);
                for (int i = 0; i < stopPoints.length(); i++) {
                    JSONObject stop = stopPoints.getJSONObject(i);
                    String user = stop.getString("user_full_name");
                    String address = stop.getString(Constants.JSON_FIELD_ADDRESS);
                    String actionText = "";
                    if (stop.getBoolean(Constants.JSON_FIELD_LEAVING)) {
                        // Someone is leaving on this stop
                        actionText = "Drop";
                    } else {
                        // Someone is joining on this stop
                        actionText = "Pick";
                    }
                    ConstraintLayout item = (ConstraintLayout) this.getActivity().getLayoutInflater()
                            .inflate(R.layout.element_stop, null, false);
                    TextView nameField = item.findViewById(R.id.name_text);
                    TextView locationField = item.findViewById(R.id.location_text);

                    // Add Google Maps link to address
                    final Uri gmmIntentUri = Uri.parse("geo:" +
                            stop.getDouble(Constants.JSON_FIELD_LAT) + "," +
                            stop.getDouble(Constants.JSON_FIELD_LON) + "?q=" +
                            Uri.encode(address));
                    locationField.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Create a Uri from an intent string. Use the result to create an Intent.

                            // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                            // Make the Intent explicit by setting the Google Maps package
                            mapIntent.setPackage("com.google.android.apps.maps");
                            // Attempt to start an activity that can handle the Intent
                            if (mapIntent.resolveActivity(
                                    RideFragment.this.getActivity().getPackageManager()) != null) {
                                startActivity(mapIntent);
                            }
                        }
                    });

                    TextView actionField = item.findViewById(R.id.action_text);
                    nameField.setText(user);
                    locationField.setText(address);
                    actionField.setText(actionText);
                    this.waypointContainer.addView(item);
                }
            } else {
                this.waypointContainer.setVisibility(View.GONE);
                this.waypointText.setVisibility(View.GONE);
            }

            this.setMap(southWestCorner, northEastCorner,
                    start, end,
                    firstStop.getString(Constants.JSON_FIELD_ADDRESS),
                    lastStop.getString(Constants.JSON_FIELD_ADDRESS),
                    routePoints, stopPoints);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.cancel_ride_button)
    public void cancelRide() {
        // Ask for confirmation
        String cancelConfirmationMessage = getString(
                R.string.cancel_ride_confirmation_message);
        Util.createConfirmationDialog(RideFragment.this.getContext(),
                cancelConfirmationMessage,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        int id = -1;
                        try {
                            id = ride.getInt(Constants.JSON_FIELD_ID);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        final int rideId = id;

                        CommonRequests.cancelRide(RideFragment.this.getContext(),
                                rideId, new Callback<JSONObject>() {
                                    @Override
                                    public void callback(JSONObject result) {
                                        if (result == null) return;
                                        try {
                                            if (result.getBoolean(Constants.JSON_FIELD_SUCCESS)) {
                                                mListener.showText("Ride cancelled");
                                                mListener.getDataManager().deleteRide(rideId);
                                                mListener.switchTab(Constants.Tab.HOME,true);
                                                RideFragment.this.setRide(null, false);
                                            } else {
                                                int error = result.getInt(
                                                        Constants.JSON_FIELD_ERROR);
                                                if (error == Constants.RIDE_NOT_FOUND) {
                                                    mListener.showText(getString(R.string.ride_not_found));
                                                    mListener.getDataManager().deleteRide(
                                                            ride.getInt(Constants.JSON_FIELD_ID));
                                                    setRide(null, false);
                                                    mListener.switchTab(Constants.Tab.HOME, true);
                                                } else if (error ==
                                                        Constants.RIDE_IS_NOT_MODIFIABLE) {
                                                    mListener.showText(getString(R.string.ride_cannot_be_modified));
                                                } else if (error ==
                                                        Constants.NOT_OWNER_OF_THE_RIDE) {
                                                    mListener.showText(getString(
                                                            R.string.not_owner_of_ride));
                                                } else {
                                                    mListener.showText(getString(R.string.unknown_error));
                                                }
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                    }
                });
    }

    @OnClick(R.id.join_ride_button)
    public void joinRide() {
        if (!this.mListener.getDataManager().userInfoOk()) {
            this.mListener.showText(getString(R.string.user_info_not_filled));
            return;
        }

        CommonRequests.joinRide(RideFragment.this.getContext(), this.ride, new Callback<JSONObject>() {
            @Override
            public void callback(JSONObject result) {
                if (result != null)  {
                    try {
                        if (result.getBoolean(Constants.JSON_FIELD_SUCCESS)) {
                            JSONObject rideJson = result.getJSONObject(Constants.JSON_FIELD_RIDE);
                            mListener.getDataManager().addOrUpdateRide(rideJson);
                            RideFragment.this.setRide(rideJson, true);
                            RideFragment.this.update();
                        } else {
                            int error = result.getInt(Constants.JSON_FIELD_ERROR);
                            if (error == Constants.RIDE_IS_NOT_MODIFIABLE) {
                                mListener.showText(getString(R.string.ride_cannot_be_modified));
                            } else if (error == Constants.RIDE_NOT_FOUND) {
                                mListener.showText(getString(R.string.ride_not_found));
                                mListener.getDataManager().deleteRide(ride.getInt(
                                        Constants.JSON_FIELD_ID));
                                setRide(null, false);
                                mListener.switchTab(Constants.Tab.HOME, true);
                            } else if (error == Constants.RIDE_UPDATED_AND_NEW_ROUTE_INELIGIBLE) {
                                mListener.showText(getString(R.string.new_route_enigible));
                                mListener.getDataManager().deleteRide(ride.getInt(
                                        Constants.JSON_FIELD_ID));
                                setRide(null, false);
                                mListener.switchTab(Constants.Tab.HOME, true);
                            } else if (error == Constants.NEWER_VERSION_IN_DATABASE) {
                                JSONObject newVersion = result.getJSONObject(
                                        Constants.JSON_FIELD_RIDE);
                                mListener.getDataManager().addOrUpdateRide(newVersion);
                                mListener.showText(getString(R.string.got_new_version_of_ride));
                                setRide(newVersion, false);
                                update();
                            }  else if (error == Constants.NO_SPACE_LEFT) {

                                mListener.showText(getString(R.string.ride_full));
                            } else {
                                mListener.showText(getString(R.string.unknown_error));
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @OnClick(R.id.finalize_ride_button)
    public void finalizeRide() {
        int id = -1;
        try {
            id = ride.getInt(Constants.JSON_FIELD_ID);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final int rideId = id;
        CommonRequests.finalizeRide(RideFragment.this.getContext(),
                rideId, new Callback<JSONObject>() {
                    @Override
                    public void callback(JSONObject result) {
                        if (result == null) return;
                        try {
                            if (result.getBoolean(Constants.JSON_FIELD_SUCCESS)) {
                                mListener.getDataManager().addOrUpdateRide(
                                        result.getJSONObject(Constants.JSON_FIELD_RIDE));
                                RideFragment.this.setRide(
                                        result.getJSONObject(Constants.JSON_FIELD_RIDE), true);
                                RideFragment.this.update();

                                //new CountDownTimer(4000, 1000) {
                                //    public void onTick(long millisUntilFinished) {}

                                //    public void onFinish() {
                                        // Update payments

                                //    }
                                //}.start();
                            } else {
                                int error = result.getInt(Constants.JSON_FIELD_ERROR);

                                if (error == Constants.RIDE_NOT_FOUND) {
                                    mListener.showText(getString(R.string.ride_not_found));
                                    mListener.getDataManager().deleteRide(rideId);
                                    setRide(null, false);
                                    mListener.switchTab(Constants.Tab.HOME, true);
                                } else if (error == Constants.NOT_OWNER_OF_THE_RIDE) {
                                    mListener.showText(getString(R.string.not_owner_of_ride));
                                } else {
                                    mListener.showText(getString(R.string.unknown_error));
                                    Log.e("Error in finalization ", Integer.toString(error));
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    @OnClick(R.id.done_ride_button)
    public void doneRide()
    {
        int id = -1;
        try {
            id = ride.getInt(Constants.JSON_FIELD_ID);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        CommonRequests.markRideDone(RideFragment.this.getContext(), id, new Callback<JSONObject>() {
            @Override
            public void callback(JSONObject result) {
                if (result == null) return;
                try {
                    if (result.getBoolean(Constants.JSON_FIELD_SUCCESS)) {
                        mListener.showText(getString(R.string.ride_marked_done));
                        mListener.getDataManager().addOrUpdateRide(
                                result.getJSONObject(Constants.JSON_FIELD_RIDE));
                        RideFragment.this.setRide(
                                result.getJSONObject(Constants.JSON_FIELD_RIDE), true);
                        // Update payments
                        CommonRequests.getAllPayments(
                                RideFragment.this.getContext(),
                                new Callback<JSONArray>() {
                                    @Override
                                    public void callback(JSONArray result) {
                                        // If result is a valid json array set payments
                                        if (result != null) {
                                            mListener.getDataManager().setPayments(result);
                                        }
                                    }
                                });

                        mListener.switchTab(Constants.Tab.HOME, true);
                    } else {
                        int error = result.getInt(Constants.JSON_FIELD_ERROR);
                        if (error == Constants.RIDE_IS_MODIFIABLE) {
                            mListener.showText(getString(R.string.ride_has_not_started));
                        } else if (error == Constants.NOT_OWNER_OF_THE_RIDE) {
                            mListener.showText(getString(R.string.not_owner_of_ride));
                        } else {
                            mListener.showText(getString(R.string.unknown_error));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @OnClick(R.id.leave_ride_button)
    public void leaveRide() {
        String leaveConfirmationMessage = getString(
                R.string.leave_ride_confirmation_message);
        Util.createConfirmationDialog(RideFragment.this.getContext(),
                leaveConfirmationMessage,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        int id = -1;
                        try {
                            id = ride.getInt("id");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        final int rideId = id;
                        CommonRequests.leaveRide(RideFragment.this.getContext(), rideId,
                                new Callback<JSONObject>() {
                            @Override
                            public void callback(JSONObject result) {
                                if (result == null) return;
                                try {
                                    if (result.getBoolean(Constants.JSON_FIELD_SUCCESS)) {
                                        mListener.showText(getString(R.string.left_ride));
                                        mListener.getDataManager().deleteRide(rideId);
                                        RideFragment.this.setRide(null, false);
                                        mListener.switchTab(Constants.Tab.HOME, true);
                                    } else {
                                        int error = result.getInt(Constants.JSON_FIELD_ERROR);
                                        if (error == Constants.RIDE_NOT_FOUND ||
                                                error == Constants.USER_NOT_IN_RIDE) {
                                            mListener.getDataManager().deleteRide(rideId);
                                            mListener.showText(getString(R.string.ride_not_found));
                                            mListener.switchTab(Constants.Tab.HOME, true);
                                        } else if (error == Constants.RIDE_IS_NOT_MODIFIABLE) {
                                            mListener.showText(getString(
                                                    R.string.ride_has_started_cannot_leave));
                                        } else {
                                            mListener.showText(getString(R.string.unknown_error));
                                            Log.e("Unknown error", result.toString());
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });
    }

    @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_ride, container, false);

        ButterKnife.bind(this, v);

        this.update();

        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mapView != null) {
            mapView.onCreate(null);
            mapView.onResume();
            mapView.getMapAsync(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Set title
        int rideId = 0;
        try {
            rideId = ride.getInt(Constants.JSON_FIELD_ID);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        getActivity().setTitle(R.string.ride + " " + rideId);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnRideFragmentInteractionListener) {
            mListener = (OnRideFragmentInteractionListener) context;
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

    // This function zooms the map to given bounds and draws route, start and end markers
    private void setMap(LatLng southWestCorner, LatLng northEastCorner,
                        LatLng start, LatLng end, String startName, String endName,
                        ArrayList<LatLng> route, JSONArray stops)
    {
        LatLngBounds bounds = new LatLngBounds(southWestCorner, northEastCorner);

        PolylineOptions plo = new PolylineOptions().addAll(route)
                .geodesic(true).color(Color.RED);

        this.mapRoute = plo;
        this.mapBounds = bounds;
        this.mapRouteStart = start;
        this.mapRouteEnd = end;
        this.mapRouteStartName = startName;
        this.mapRouteEndName = endName;
        this.mapRouteStops = stops;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        final GoogleMap m = this.map;
        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
             @Override
             public void onMapLoaded() {
                 m.moveCamera(CameraUpdateFactory.newLatLngBounds(
                         RideFragment.this.mapBounds, 20));
             }
         });
        MapsInitializer.initialize(getContext());
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        this.map.addPolyline(this.mapRoute);

        if (this.mapRouteStart != null) {
            googleMap.addMarker(new MarkerOptions().position(this.mapRouteStart)
                    .title(this.mapRouteStartName));
        } if (this.mapRouteEnd != null) {
            googleMap.addMarker(new MarkerOptions().position(this.mapRouteEnd)
                    .title(this.mapRouteEndName));
        }
        for (int i = 0; i < this.mapRouteStops.length(); i++) {
            try {
                JSONObject stop = this.mapRouteStops.getJSONObject(i);
                LatLng pos = new LatLng(stop.getDouble(Constants.JSON_FIELD_LAT),
                        stop.getDouble(Constants.JSON_FIELD_LON));
                String user = stop.getString("user_full_name");
                String address = stop.getString(Constants.JSON_FIELD_ADDRESS);
                String marker = "";

                if (stop.getBoolean(Constants.JSON_FIELD_LEAVING)) {
                    // Someone is leaving on this stop
                    marker = "Drop";
                } else {
                    // Someone is joining on this stop
                    marker = "Pick up";
                }
                marker += " user \"" + user + "\" at \"" + address + "\".";
                googleMap.addMarker(new MarkerOptions().position(pos)
                        .title(marker));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(this.mapBounds, 20));
    }

    public interface OnRideFragmentInteractionListener {
        void showText(String text);
        DataManager getDataManager();
        void switchTab(Constants.Tab tab, boolean addToBackStack);
        void viewRide(JSONObject ride, boolean joined);
        void viewUser(JSONObject user);
    }
}
