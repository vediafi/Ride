package com.indagon.kimppakyyti.tools;

import android.content.Context;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.location.places.Place;
import com.indagon.kimppakyyti.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.indagon.kimppakyyti.Constants;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;


public class CommonRequests {

    public static void login(final Context context, final String accessToken, final String userId,
                             final int loginMethod, final Callback<JSONObject> callback) {
        final ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage(context.getString(R.string.logging_in));
        dialog.setCancelable(false);
        dialog.setInverseBackgroundForced(false);
        dialog.show();

        final JSONObject obj = new JSONObject();
        try {
            obj.put(Constants.JSON_FIELD_USER_ID, userId);
            obj.put(Constants.JSON_FIELD_TOKEN, accessToken);
            obj.put(Constants.JSON_FIELD_LOGIN_METHOD, loginMethod);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /* First do a GET request to acquire CSRF-token and then do the actual login with POST */
        JsonObjectRequest request = new JsonObjectRequest(context, Request.Method.GET,
                Constants.LOGIN_URL, obj, new Response.Listener<JSONObject>() {

            public void onResponse(final JSONObject response) {
                NetworkSingleton.persistCSFRToken(context);

                JsonObjectRequest request = new JsonObjectRequest(context, Request.Method.POST,
                        Constants.LOGIN_URL, obj, new Response.Listener<JSONObject>() {

                    public void onResponse(JSONObject response) {
                        NetworkSingleton.persistSessionId(context);
                        NetworkSingleton.persistCSFRToken(context);

                        dialog.dismiss();
                        if (callback != null) {
                            callback.callback(response);
                        }
                    }
                }, new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        Util.printResponseDataToLogcat(error.networkResponse.data);
                        if (error instanceof TimeoutError) {
                            Log.e("Volley", "TimeoutError");
                        }else if(error instanceof NoConnectionError){
                            Log.e("Volley", "NoConnectionError");
                        } else if (error instanceof AuthFailureError) {
                            Log.e("Volley", "AuthFailureError");
                        } else if (error instanceof ServerError) {
                            Log.e("Volley", "ServerError");
                        } else if (error instanceof NetworkError) {
                            Log.e("Volley", "NetworkError");
                        } else if (error instanceof ParseError) {
                            Log.e("Volley", "ParseError");
                        }
                        dialog.dismiss();
                        Toast.makeText(context, R.string.server_login_failed,
                                Toast.LENGTH_LONG).show();

                        if (callback != null) {
                            callback.callback(null);
                        }
                    }
                });
                NetworkSingleton.getInstance(context).addToRequestQueue(request);
            }
        }, new Response.ErrorListener() {
            public void onErrorResponse(VolleyError error) {
                if (error.networkResponse != null){
                    Util.printResponseDataToLogcat(error.networkResponse.data);
                }
                Toast.makeText(context, R.string.server_login_failed,
                        Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });

        NetworkSingleton.getInstance(context).addToRequestQueue(request);
    }

    public static void updateUserInfo(final Context context, String email, String phoneNumber,
                                      String description, final Callback<JSONObject> callback) {
        ProgressDialog dialog = Util.makeProgressDialog(context, context.getString(R.string.updating_user_info));

        JSONObject obj = new JSONObject();
        try {
            obj.put(Constants.JSON_FIELD_PHONE_NUMBER, phoneNumber);
            obj.put(Constants.JSON_FIELD_EMAIL, email);
            obj.put(Constants.JSON_FIELD_DESCRIPTION, description);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Util.makeJsonObjectRequest(context, Request.Method.POST, obj, Constants.UPDATE_USER_URL,
                dialog, context.getString(R.string.updating_user_info_failed), callback);
    }

    public static void updateProfilePicture(final Context context, ImageView profilePicture,
                                            final Callback<JSONObject> callback) {
        ProgressDialog dialog = Util.makeProgressDialog(context, context.getString(R.string.updating_profile_picture));

        Bitmap profileBitmap = ((BitmapDrawable)profilePicture.getDrawable()).getBitmap();
        JSONObject obj = new JSONObject();
        try {
            obj.put(Constants.JSON_FIELD_PROFILE_PICTURE,
                    Util.convertImageToBase64String(profileBitmap));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Util.makeJsonObjectRequest(context, Request.Method.POST, obj,
                Constants.UPDATE_PROFILE_PICTURE_URL,
                dialog, context.getString(R.string.updating_profile_picture_failed), callback);
    }

    public static void getRoute(final Context context, long time, double startLat, double startLon,
                                double endLat, double endLon, final Callback<JSONObject> callback) {
        ProgressDialog dialog = Util.makeProgressDialog(context, "Finding route");


        String url = String.format(Locale.ENGLISH, Constants.GET_ROUTE_URL, time,
                    startLat,startLon,endLat,endLon);

        Util.makeJsonObjectRequest(context, Request.Method.GET, null, url,
                dialog, context.getString(R.string.finding_route_failed), callback);
    }


    public static void createRide(final Context context, JSONObject route, Place startLocation,
                                  Place endLocation, long startTime, int initialPersonCount,
                                  int totalSeats, String repeat,
                                  final Callback<JSONObject> callback) {
        ProgressDialog dialog = Util.makeProgressDialog(context, context.getString(R.string.creating_ride));

        JSONObject obj = new JSONObject();
        try {
            JSONObject startLocationJSON = new JSONObject();
            startLocationJSON.put(Constants.JSON_FIELD_ADDRESS, startLocation.getAddress());
            startLocationJSON.put(Constants.JSON_FIELD_LON, startLocation.getLatLng().longitude);
            startLocationJSON.put(Constants.JSON_FIELD_LAT, startLocation.getLatLng().latitude);

            JSONObject endStopJSON = new JSONObject();
            endStopJSON.put(Constants.JSON_FIELD_DISTANCE_FROM_LAST_STOP, 0);
            endStopJSON.put(Constants.JSON_FIELD_TIME_FROM_LAST_STOP, 0);
            endStopJSON.put(Constants.JSON_FIELD_INDEX, 0);
            endStopJSON.put(Constants.JSON_FIELD_PERSON_COUNT, initialPersonCount);
            endStopJSON.put(Constants.JSON_FIELD_ADDRESS, endLocation.getAddress());
            endStopJSON.put(Constants.JSON_FIELD_LON, endLocation.getLatLng().longitude);
            endStopJSON.put(Constants.JSON_FIELD_LAT, endLocation.getLatLng().latitude);

            route.put(Constants.JSON_FIELD_START_LOCATION, startLocationJSON);
            route.put(Constants.JSON_FIELD_END_STOP, endStopJSON);

            obj.put(Constants.JSON_FIELD_ROUTE, route);
            obj.put(Constants.JSON_FIELD_START_TIME, startTime);
            obj.put(Constants.JSON_FIELD_INITIAL_PERSON_COUNT, initialPersonCount);
            obj.put(Constants.JSON_FIELD_TOTAL_SEATS, totalSeats);
            if (repeat != null)
            {
                obj.put(Constants.JSON_FIELD_DAYS, repeat);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Util.makeJsonObjectRequest(context, Request.Method.POST, obj, Constants.CREATE_RIDE_URL,
                dialog, context.getString(R.string.create_ride_failed), callback);
    }

    public static void cancelRide(final Context context, int rideId, final Callback<JSONObject> callback) {
        ProgressDialog dialog = Util.makeProgressDialog(context, context.getString(R.string.canceling_ride));

        JSONObject obj = new JSONObject();
        try {
            obj.put(Constants.JSON_FIELD_ID, rideId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Util.makeJsonObjectRequest(context, Request.Method.POST, obj, Constants.CANCEL_RIDE_URL,
                dialog, context.getString(R.string.canceling_ride_failed), callback);
    }

    public static void leaveRide(final Context context, int rideId, final Callback<JSONObject> callback) {
        ProgressDialog dialog = Util.makeProgressDialog(context, context.getString(R.string.leaving_ride));

        JSONObject obj = new JSONObject();
        try {
            obj.put(Constants.JSON_FIELD_ID, rideId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Util.makeJsonObjectRequest(context, Request.Method.POST, obj, Constants.LEAVE_RIDE_URL,
                dialog, context.getString(R.string.leaving_ride_failed), callback);
    }

    public static void finalizeRide(final Context context, int rideId, final Callback<JSONObject> callback) {
        ProgressDialog dialog = Util.makeProgressDialog(context, context.getString(R.string.finalizing_ride));

        JSONObject obj = new JSONObject();
        try {
            obj.put(Constants.JSON_FIELD_ID, rideId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Util.makeJsonObjectRequest(context, Request.Method.POST, obj, Constants.FINALIZE_RIDE_URL,
                dialog, context.getString(R.string.finalizing_ride_failed), callback);
    }

    public static void markRideDone(final Context context, int rideId, final Callback<JSONObject> callback) {
        ProgressDialog dialog = Util.makeProgressDialog(context, context.getString(R.string.marking_ride_done));

        JSONObject obj = new JSONObject();
        try {
            obj.put(Constants.JSON_FIELD_ID, rideId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Util.makeJsonObjectRequest(context, Request.Method.POST, obj, Constants.DONE_RIDE_URL,
                dialog, context.getString(R.string.marking_ride_done_failed), callback);
    }

    public static void kickUserFromRide(final Context context, int rideId,
                                        String kickedUserid, final Callback<JSONObject> callback) {
        ProgressDialog dialog = Util.makeProgressDialog(context, context.getString(R.string.kicking_user_from_ride));

        JSONObject obj = new JSONObject();
        try {
            obj.put(Constants.JSON_FIELD_ID, rideId);
            obj.put(Constants.JSON_FIELD_KICKED_USER_ID, kickedUserid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Util.makeJsonObjectRequest(context, Request.Method.POST, obj, Constants.KICK_USER_FROM_RIDE_URL,
                dialog, context.getString(R.string.kicking_user_from_ride_failed), callback);
    }

    public static void getJoinableRideInfo(final Context context, long rideId,
                                           JSONObject startLocation, JSONObject endLocation,
                                           final Callback<JSONObject> callback) {
        ProgressDialog dialog = Util.makeProgressDialog(context,
                context.getString(R.string.getting_ride_info));

        JSONObject obj = new JSONObject();
        try {
            obj.put(Constants.JSON_FIELD_ID, rideId);
            obj.put(Constants.JSON_FIELD_START_LOCATION, startLocation);
            obj.put(Constants.JSON_FIELD_END_LOCATION, endLocation);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Util.makeJsonObjectRequest(context, Request.Method.POST, obj,
                Constants.GET_JOINABLE_RIDE_INFO_URL, dialog,
                context.getString(R.string.getting_ride_info_failed), callback);
    }

    public static void joinRide(final Context context, JSONObject ride, final Callback<JSONObject>
            callback) {
        ProgressDialog dialog = Util.makeProgressDialog(context, context.getString(R.string.joining_ride));

        JSONObject route = null;
        int userEndStopIndex, userStartStopIndex;
        JSONObject userEndStop;
        JSONObject userStartStop;
        JSONObject obj = new JSONObject();

        try {
            route = ride.getJSONObject(Constants.JSON_FIELD_ROUTE);
            userEndStopIndex = route.getInt(Constants.JSON_FIELD_USER_END_INDEX);
            userStartStopIndex = route.getInt(Constants.JSON_FIELD_USER_START_INDEX);
            userEndStop = route.getJSONArray(Constants.JSON_FIELD_STOPS)
                    .getJSONObject(userEndStopIndex);
            userStartStop = route.getJSONArray(Constants.JSON_FIELD_STOPS)
                    .getJSONObject(userStartStopIndex);

            obj.put(Constants.JSON_FIELD_ROUTE, route);
            obj.put(Constants.JSON_FIELD_USER_START_STOP, userStartStop);
            obj.put(Constants.JSON_FIELD_USER_END_STOP, userEndStop);
            obj.put(Constants.JSON_FIELD_VERSION, ride.getInt(Constants.JSON_FIELD_VERSION));
            obj.put(Constants.JSON_FIELD_ID, ride.getInt(Constants.JSON_FIELD_ID));
            obj.put(Constants.JSON_FIELD_USER_START_TIME,
                    ride.getLong(Constants.JSON_FIELD_START_TIME));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Util.makeJsonObjectRequest(context, Request.Method.POST, obj, Constants.JOIN_RIDE_URL,
                dialog, context.getString(R.string.joining_ride_failed), callback);
    }

    public static void getNewRideVersion(final Context context, long id, long version,
                                         final Callback<JSONObject> callback) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(Constants.JSON_FIELD_ID, id);
            obj.put(Constants.JSON_FIELD_VERSION, version);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest req = new JsonObjectRequest(context, Request.Method.POST,
                Constants.UPDATE_RIDE_INFO,
                obj, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                callback.callback(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Util.printResponseDataToLogcat(error.networkResponse.data);
                callback.callback(null);
            }
        });
        NetworkSingleton.getInstance(context).addToRequestQueue(req);
    }

    public static void getAllRides(final Context context, final Callback<JSONArray> callback) {
        final ProgressDialog dialog = Util.makeProgressDialog(context,
                context.getString(R.string.getting_all_rides));

        JsonArrayRequest request = new JsonArrayRequest(context, Request.Method.GET,
                Constants.GET_ALL_RIDES_URL, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                dialog.dismiss();
                callback.callback(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dialog.dismiss();
                if (error.networkResponse == null) {
                    Toast.makeText(context, context.getString(R.string.getting_all_payments_failed),
                            Toast.LENGTH_SHORT).show();
                } else if (error.networkResponse.statusCode == 403) {
                    Util.reLogin(context);
                } else {
                    Util.printResponseDataToLogcat(error.networkResponse.data);
                    Toast.makeText(context, context.getString(R.string.getting_all_rides_failed),
                            Toast.LENGTH_SHORT).show();
                    callback.callback(null);
                }
            }
        });
        NetworkSingleton.getInstance(context).addToRequestQueue(request);
    }

    public static void getNearbyRides(final Context context, final int personsJoining,
                                      final long startTime, final Place startLocation,
                                      final Place endLocation, final Callback<JSONArray> callback) {
        try {
            JSONObject endLocationJson = new JSONObject();
            JSONObject startLocationJson = new JSONObject();
            try {
                endLocationJson.put(Constants.JSON_FIELD_ADDRESS,
                        URLEncoder.encode(endLocation.getAddress().toString(), "UTF-8"));
                endLocationJson.put(Constants.JSON_FIELD_LON, endLocation.getLatLng().longitude);
                endLocationJson.put(Constants.JSON_FIELD_LAT, endLocation.getLatLng().latitude);
                startLocationJson.put(Constants.JSON_FIELD_ADDRESS,
                        URLEncoder.encode(startLocation.getAddress().toString(), "UTF-8"));
                startLocationJson.put(Constants.JSON_FIELD_LON,
                        startLocation.getLatLng().longitude);
                startLocationJson.put(Constants.JSON_FIELD_LAT,
                        startLocation.getLatLng().latitude);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Show dialog
            final ProgressDialog dialog = Util.makeProgressDialog(context, context.getString(R.string.searching_for_rides));

            // Create request url for search
            String url = String.format(Locale.ENGLISH, Constants.FIND_RIDES_URL,
                    personsJoining, startTime);
            url += "/" + URLEncoder.encode(startLocationJson.toString(), "UTF-8");
            url += "/" + URLEncoder.encode(endLocationJson.toString(), "UTF-8");

            JsonArrayRequest request = new JsonArrayRequest(context, Request.Method.GET,
                    url, null, new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    dialog.dismiss();
                    Util.log(context, "Response: " + response, Constants.LOG_DEBUG);
                    callback.callback(response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    dialog.dismiss();
                    if (error.networkResponse == null) {
                        Toast.makeText(context, context.getString(R.string.searching_for_rides_failed), Toast.LENGTH_SHORT).show();
                    } else if (error.networkResponse.statusCode == 403) {
                        Util.reLogin(context);
                    } else {
                        Util.printResponseDataToLogcat(error.networkResponse.data);
                        Toast.makeText(context, context.getString(R.string.searching_for_rides_failed), Toast.LENGTH_SHORT).show();
                        callback.callback(null);
                    }
                }
            });
            NetworkSingleton.getInstance(context).addToRequestQueue(request);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void createWatchdog(final Context context,
                                      String startAddress, LatLng startLatLon,
                                      String endAddress, LatLng endLatLon,
                                      long startTime, long endTime,
                                      String repeat, int distance, int index,
                                      final Callback<JSONObject> callback) {
        JSONObject obj = new JSONObject();
        try {
            JSONObject startLocationJSON = new JSONObject();
            startLocationJSON.put(Constants.JSON_FIELD_ADDRESS, startAddress);
            startLocationJSON.put(Constants.JSON_FIELD_LON, startLatLon.longitude);
            startLocationJSON.put(Constants.JSON_FIELD_LAT, startLatLon.latitude);
            obj.put(Constants.JSON_FIELD_START_LOCATION, startLocationJSON);

            JSONObject endLocationJSON = new JSONObject();
            endLocationJSON.put(Constants.JSON_FIELD_ADDRESS, endAddress);
            endLocationJSON.put(Constants.JSON_FIELD_LON, endLatLon.longitude);
            endLocationJSON.put(Constants.JSON_FIELD_LAT, endLatLon.latitude);
            obj.put(Constants.JSON_FIELD_END_LOCATION, endLocationJSON);

            obj.put(Constants.JSON_FIELD_START_TIME, startTime);
            obj.put(Constants.JSON_FIELD_END_TIME, endTime);
            obj.put(Constants.JSON_FIELD_DISTANCE, distance);
            obj.put(Constants.JSON_FIELD_WEEKDAYS, repeat);
            obj.put(Constants.JSON_FIELD_INDEX, index);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ProgressDialog dialog = Util.makeProgressDialog(context, context.getString(R.string.creating_watchdog));
        Util.makeJsonObjectRequest(context, Request.Method.POST, obj, Constants.CREATE_WATCHDOG_URL,
                dialog, context.getString(R.string.creating_watchdog_failed), callback);
    }

    public static void updateWatchdog(final Context context, int id,
                                      String startAddress, LatLng startLatLon,
                                      String endAddress, LatLng endLatLon,
                                      long startTime, long endTime,
                                      String repeat, int distance, int index,
                                      final Callback<JSONObject> callback) {
        JSONObject obj = new JSONObject();

        try {
            JSONObject startLocationJSON = new JSONObject();
            startLocationJSON.put(Constants.JSON_FIELD_ADDRESS, startAddress);
            startLocationJSON.put(Constants.JSON_FIELD_LON, startLatLon.longitude);
            startLocationJSON.put(Constants.JSON_FIELD_LAT, startLatLon.latitude);
            obj.put(Constants.JSON_FIELD_START_LOCATION, startLocationJSON);

            JSONObject endLocationJSON = new JSONObject();
            endLocationJSON.put(Constants.JSON_FIELD_ADDRESS, endAddress);
            endLocationJSON.put(Constants.JSON_FIELD_LON, endLatLon.longitude);
            endLocationJSON.put(Constants.JSON_FIELD_LAT, endLatLon.latitude);
            obj.put(Constants.JSON_FIELD_END_LOCATION, endLocationJSON);

            obj.put(Constants.JSON_FIELD_START_TIME, startTime);
            obj.put(Constants.JSON_FIELD_END_TIME, endTime);
            obj.put(Constants.JSON_FIELD_DISTANCE, distance);
            obj.put(Constants.JSON_FIELD_WEEKDAYS, repeat);
            obj.put(Constants.JSON_FIELD_INDEX, index);
            obj.put(Constants.JSON_FIELD_ID, id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ProgressDialog dialog = Util.makeProgressDialog(context, context.getString(R.string.updating_watchdog));
        Util.makeJsonObjectRequest(context, Request.Method.POST, obj, Constants.UPDATE_WATCHDOG_URL,
                dialog, context.getString(R.string.updating_watchdog_failed), callback);
    }

    public static void removeWatchdog(final Context context, int id,
                                      final Callback<JSONObject> callback) {
        ProgressDialog dialog = Util.makeProgressDialog(context, context.getString(R.string.removing_watchdog));

        JSONObject obj = new JSONObject();
        try {
            obj.put(Constants.JSON_FIELD_ID, id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Util.makeJsonObjectRequest(context, Request.Method.POST, obj, Constants.REMOVE_WATCHDOG_URL,
                dialog, context.getString(R.string.removing_watchdog_failed), callback);
    }

    public static void getAllWatchdogs(final Context context, final Callback<JSONArray> callback) {
        final ProgressDialog dialog = Util.makeProgressDialog(context,
                context.getString(R.string.getting_all_watchdogs));

        JsonArrayRequest request = new JsonArrayRequest(context, Request.Method.GET,
                Constants.GET_ALL_WATCHDOGS_URL, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                dialog.dismiss();
                callback.callback(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dialog.dismiss();
                if (error.networkResponse == null) {
                    Toast.makeText(context, context.getString(R.string.getting_all_payments_failed),
                            Toast.LENGTH_SHORT).show();
                } else if (error.networkResponse.statusCode == 403) {
                    Util.reLogin(context);
                } else {
                    Util.printResponseDataToLogcat(error.networkResponse.data);
                    Toast.makeText(context, context.getString(R.string.getting_all_watchdogs_failed),
                            Toast.LENGTH_SHORT).show();
                    callback.callback(null);
                }
            }
        });
        NetworkSingleton.getInstance(context).addToRequestQueue(request);
    }

    public static void registerPushToken(final Context context, String token, final Callback<JSONObject> callback) {
        JSONObject obj = new JSONObject();

        try {
            obj.put(Constants.JSON_FIELD_TOKEN, token);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest req = new JsonObjectRequest(context, Request.Method.POST,
                Constants.REGISTER_PUSH_TOKEN_URL,
                obj, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                callback.callback(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Util.printResponseDataToLogcat(error.networkResponse.data);
                callback.callback(null);
            }
        });
        NetworkSingleton.getInstance(context).addToRequestQueue(req);
    }

    public static void getAllPayments(final Context context, final Callback<JSONArray> callback) {
        // TODO how to fix this
        //final ProgressDialog dialog = Util.makeProgressDialog(context,
        //        context.getString(R.string.getting_all_payments));

        JsonArrayRequest request = new JsonArrayRequest(context, Request.Method.GET,
                Constants.GET_ALL_PAYMENTS_URL, null, new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    //dialog.dismiss();
                    callback.callback(response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    //dialog.dismiss();
                    if (error.networkResponse == null) {
                        Toast.makeText(context, context.getString(R.string.getting_all_payments_failed),
                                Toast.LENGTH_SHORT).show();
                    } else if (error.networkResponse.statusCode == 403) {
                        Util.reLogin(context);
                    } else {
                        Util.printResponseDataToLogcat(error.networkResponse.data);
                        Toast.makeText(context, context.getString(R.string.getting_all_payments_failed),
                                Toast.LENGTH_SHORT).show();
                        callback.callback(null);
                    }
                }
            });
        NetworkSingleton.getInstance(context).addToRequestQueue(request);
    }

    public static void receivePayment(final Context context, String payer, int amount,
                                      final Callback<JSONObject> callback) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(Constants.JSON_FIELD_PAYER, payer);
            obj.put(Constants.JSON_FIELD_AMOUNT, amount);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest req = new JsonObjectRequest(context, Request.Method.POST,
                Constants.RECEIVE_PAYMENT_URL,
                obj, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                callback.callback(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                callback.callback(null);
            }
        });
        NetworkSingleton.getInstance(context).addToRequestQueue(req);
    }

    public static void getAllRepeaters(final Context context, final Callback<JSONArray> callback) {
        final ProgressDialog dialog = Util.makeProgressDialog(context,
                context.getString(R.string.getting_all_repeaters));

        JsonArrayRequest request = new JsonArrayRequest(context, Request.Method.GET,
                Constants.GET_ALL_REPEATERS_URL, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                dialog.dismiss();
                callback.callback(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dialog.dismiss();
                if (error.networkResponse == null) {
                    Toast.makeText(context, context.getString(R.string.getting_all_repeaters_failed),
                            Toast.LENGTH_SHORT).show();
                } else if (error.networkResponse.statusCode == 403) {
                    Util.reLogin(context);
                } else {
                    Util.printResponseDataToLogcat(error.networkResponse.data);
                    Toast.makeText(context, context.getString(R.string.getting_all_repeaters_failed),
                            Toast.LENGTH_SHORT).show();
                    callback.callback(null);
                }
            }
        });
        NetworkSingleton.getInstance(context).addToRequestQueue(request);
    }

    public static void removeRepeater(final Context context, int id,
                                      final Callback<JSONObject> callback) {
        ProgressDialog dialog = Util.makeProgressDialog(context,
                context.getString(R.string.removing_repeater));

        JSONObject obj = new JSONObject();
        try {
            obj.put(Constants.JSON_FIELD_ID, id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Util.makeJsonObjectRequest(context, Request.Method.POST, obj, Constants.REMOVE_REPEATER_URL,
                dialog, context.getString(R.string.removing_repeater_failed), callback);
    }
}