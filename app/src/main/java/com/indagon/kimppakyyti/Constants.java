package com.indagon.kimppakyyti;

public class Constants {

    /* URLS */
    //private static final String PROTOCOL = "http";
    private static final String PROTOCOL = "https";
    public static final String DOMAIN = "ride.vedia.fi";
    //public static final String DOMAIN = "kimppa.home";
    //public static final String DOMAIN = "joni.fi.indagon.com";

    private static final String BASE_URL = PROTOCOL + "://" + DOMAIN + "/api";
    public static final String IMAGE_BASE_URL = PROTOCOL + "://" + DOMAIN + "/";
    public static final String REFERER = PROTOCOL + "://" + DOMAIN + "/";

    public static final String LOGIN_URL = BASE_URL + "/user/login";
    public static final String UPDATE_USER_URL = BASE_URL + "/user/update_info";
    public static final String UPDATE_PROFILE_PICTURE_URL = BASE_URL + "/user/update_picture";
    public static final String FIND_RIDES_URL = BASE_URL + "/ride/find_nearby/%d/%d";
    public static final String CREATE_RIDE_URL = BASE_URL + "/ride/create";
    public static final String GET_ROUTE_URL = BASE_URL + "/route/get/%d/%f,%f/%f,%f";
    public static final String CANCEL_RIDE_URL = BASE_URL + "/ride/cancel";
    public static final String LEAVE_RIDE_URL = BASE_URL + "/ride/leave";
    public static final String FINALIZE_RIDE_URL = BASE_URL + "/ride/finalize";
    public static final String DONE_RIDE_URL = BASE_URL + "/ride/done";
    public static final String UPDATE_RIDE_INFO = BASE_URL + "/ride/get_new_version";
    public static final String KICK_USER_FROM_RIDE_URL = BASE_URL + "/ride/kick_user";
    public static final String GET_JOINABLE_RIDE_INFO_URL = BASE_URL + "/ride/get_joinable_info";
    public static final String JOIN_RIDE_URL = BASE_URL + "/ride/join";
    public static final String GET_ALL_RIDES_URL = BASE_URL + "/ride/all";
    public static final String CREATE_WATCHDOG_URL = BASE_URL + "/watchdog/create";
    public static final String REMOVE_WATCHDOG_URL = BASE_URL + "/watchdog/remove";
    public static final String UPDATE_WATCHDOG_URL = BASE_URL + "/watchdog/update";
    public static final String GET_ALL_WATCHDOGS_URL = BASE_URL + "/watchdog/all";
    public static final String REGISTER_PUSH_TOKEN_URL = BASE_URL + "/user/register_push_token";
    public static final String GET_ALL_PAYMENTS_URL = BASE_URL + "/payment/all";
    public static final String RECEIVE_PAYMENT_URL = BASE_URL + "/payment/receive";
    public static final String GET_ALL_REPEATERS_URL = BASE_URL + "/repeater/all";
    public static final String REMOVE_REPEATER_URL = BASE_URL + "/repeater/remove";



    public static final String CSRF_TOKEN = "csrftoken";
    public static final String CSRF_HEADER = "X-csrftoken";
    public static final String SESSION_ID = "sessionid";
    public static final String ACCESS_TOKEN = "accessToken";
    public static final long SESSION_AGE = 31449603000l; // milliseconds
    public static final String REGISTRATION_TOKEN_SENT_TO_SERVER = "tokenSentToServer";
    public static final String REGISTRATION_TOKEN = "registrationToken";


    public static final int LOG_INFO = 0;
    public static final int LOG_DEBUG = 1;
    public static final int LOG_ERROR = 2;

    public static final int UNKNOWN_ERROR = 0;
    public static final int NEWER_VERSION_IN_DATABASE = 1;
    public static final int NO_SPACE_LEFT = 2;
    public static final int RIDE_UPDATED_AND_NEW_ROUTE_INELIGIBLE = 3;
    public static final int NOT_OWNER_OF_THE_RIDE = 4;
    public static final int USER_NOT_IN_RIDE = 5;
    public static final int RIDE_IS_NOT_MODIFIABLE = 6;
    public static final int RIDE_IS_MODIFIABLE = 7;
    public static final int ALREADY_NEWEST_VERSION = 10;
    public static final int RIDE_NOT_FOUND = 11;
    public static final int NOT_OWNER_OF_THE_REPEATER = 13;

    // Push codes
    public static final int USER_JOINED_RIDE = 0;
    public static final int USER_LEFT_RIDE = 1;
    public static final int USER_KICKED_FROM_THE_RIDE = 2;
    public static final int RIDE_FINALIZED = 3;
    public static final int RIDE_CANCELED = 4;
    public static final int ROUTE_CHANGED = 5;
    public static final int RIDE_DONE = 6;
    public static final int WATCHDOG_ACTIVATED = 7;
    public static final int PAYMENT_RECEIVED = 14;

    // Task codes
    public static final int TASK_UPDATE_RIDE = 0;
    public static final int TASK_UPDATE_PAYMENTS = 1;
    public static final int TASK_WATCHDOG_ACTIVATED = 2;
    public static final int TASK_UPDATE_GCM_TOKEN = 3;
    public static final int TASK_DELETE_RIDE = 4;

    public static final String LAST_LOGIN = "lastLogin";

    public static final int GOOGLE_LOGIN_METHOD = 2;

    // JSON field names
    public static final String JSON_FIELD_ID = "id";
    public static final String JSON_FIELD_USERNAME = "username";
    public static final String JSON_FIELD_EMAIL = "email";
    public static final String JSON_FIELD_PHONE_NUMBER = "phone_number";
    public static final String JSON_FIELD_TIME_FROM_LAST_STOP = "time_from_last_stop";
    public static final String JSON_FIELD_DISTANCE_FROM_LAST_STOP = "distance_from_last_stop";
    public static final String JSON_FIELD_USER = "user";
    public static final String JSON_FIELD_LEAVING = "leaving";
    public static final String JSON_FIELD_ROUTE = "route";
    public static final String JSON_FIELD_STOPS = "stops";
    public static final String JSON_FIELD_MODIFIABLE = "modifiable";
    public static final String JSON_FIELD_ACTIVE = "active";
    public static final String JSON_FIELD_OWNER = "owner";
    public static final String JSON_FIELD_REPEATER = "repeater";
    public static final String JSON_FIELD_START_TIME = "start_time";
    public static final String JSON_FIELD_START_HOUR = "start_hour";
    public static final String JSON_FIELD_START_MINUTE = "start_minute";
    public static final String JSON_FIELD_END_TIME = "end_time";
    public static final String JSON_FIELD_START_LOCATION = "start_location";
    public static final String JSON_FIELD_END_LOCATION = "end_location";
    public static final String JSON_FIELD_ADDRESS = "address";
    public static final String JSON_FIELD_TIME = "time";
    public static final String JSON_FIELD_DISTANCE = "distance";
    public static final String JSON_FIELD_PRICE = "price";
    public static final String JSON_FIELD_PASSENGERS = "passengers";
    public static final String JSON_FIELD_FIRST_NAME = "first_name";
    public static final String JSON_FIELD_LAST_NAME = "last_name";
    public static final String JSON_FIELD_SUCCESS = "success";
    public static final String JSON_FIELD_ERROR = "error";
    public static final String JSON_FIELD_DAYS = "days";
    public static final String JSON_FIELD_LAT = "lat";
    public static final String JSON_FIELD_LON = "lon";
    public static final String JSON_FIELD_BB_SOUTHWEST_LAT = "bb_southwest_lat";
    public static final String JSON_FIELD_BB_SOUTHWEST_LON = "bb_southwest_lon";
    public static final String JSON_FIELD_BB_NORTHEAST_LAT = "bb_northeast_lat";
    public static final String JSON_FIELD_BB_NORTHEAST_LON = "bb_northeast_lon";
    public static final String JSON_FIELD_RIDE = "ride";
    public static final String JSON_FIELD_POINTS = "points";
    public static final String JSON_FIELD_INDEX = "index";
    public static final String JSON_FIELD_WEEKDAYS = "weekdays";
    public static final String JSON_FIELD_INITIAL_PASSENGER_COUNT = "initial_passenger_count";
    public static final String JSON_FIELD_TOTAL_SEATS = "total_seats";
    public static final String JSON_FIELD_AMOUNT = "amount";
    public static final String JSON_FIELD_TO_USER = "to_user";
    public static final String JSON_FIELD_TO_USER_NAME = "to_user_name";
    public static final String JSON_FIELD_FROM_USER = "from_user";
    public static final String JSON_FIELD_FROM_USER_NAME = "from_user_name";
    public static final String JSON_FIELD_PROFILE_PICTURE = "profile_picture";
    public static final String JSON_FIELD_USER_ID = "user_id";
    public static final String JSON_FIELD_TOKEN = "token";
    public static final String JSON_FIELD_PERSON_COUNT = "person_count";
    public static final String JSON_FIELD_INITIAL_PERSON_COUNT = "initial_person_count";
    public static final String JSON_FIELD_END_STOP = "end_stop";
    public static final String JSON_FIELD_KICKED_USER_ID = "kicked_user_id";
    public static final String JSON_FIELD_USER_START_INDEX = "user_start_index";
    public static final String JSON_FIELD_USER_END_INDEX = "user_end_index";
    public static final String JSON_FIELD_USER_START_STOP = "user_start_stop";
    public static final String JSON_FIELD_USER_END_STOP = "user_end_stop";
    public static final String JSON_FIELD_VERSION = "version";
    public static final String JSON_FIELD_USER_START_TIME = "user_start_time";
    public static final String JSON_FIELD_PAYER = "payer";
    public static final String JSON_FIELD_LOGIN_METHOD = "login_method";
    public static final String JSON_FIELD_SHARES = "shares";
    public static final String JSON_FIELD_TASK = "task";
    public static final String JSON_FIELD_RIDE_ID = "ride_id";
    public static final String JSON_FIELD_WATCHDOG_ID = "watchdog_id";
    public static final String JSON_FIELD_PICTURE_URL = "picture_url";
    public static final String JSON_FIELD_DEBT = "debt";
    public static final String JSON_FIELD_DESCRIPTION = "description";

    // All fragments
    public enum Tab {
        SETTINGS, HOME, PAYMENTS, CREATE_RIDE, FIND_RIDE, PROFILE,
        RIDE, CREATE_WATCHDOG, EDIT_WATCHDOG, HISTORY, SEARCH_RESULTS
    }

    // Ride state
    public static final String RIDE_STATUS_OPEN = "OPEN";
    public static final String RIDE_STATUS_CLOSED = "CLOSED";
    public static final String RIDE_STATUS_DONE = "FINISHED";

    // My status on a ride
    public static final String MY_RIDE_STATUS_JOINED = "JOINED";
    public static final String MY_RIDE_STATUS_NOT_JOINED = "NOT JOINED";
    public static final String MY_RIDE_STATUS_OWNER = "OWNER";

    // Ride roles
    public static final String RIDE_ROLE_DRIVER = "DRIVER";
    public static final String RIDE_ROLE_RIDER = "RIDER";

    public enum LocationType {
        START, END
    }

    // Shared pfef keys
    public static final String SHAREDPREFS_KEY_USER = "user";
    public static final String SHAREDPREFS_KEY_RIDES = "rides";
    public static final String SHAREDPREFS_KEY_WATCHDOGS = "watchdogs";
    public static final String SHAREDPREFS_KEY_REPEATERS = "repeaters";
    public static final String SHAREDPREFS_KEY_PAYMENTS = "payments";
    public static final String SHAREDPREFS_KEY_GOOGLE_NAME = "google_name";
    public static final String SHAREDPREFS_KEY_PUSH_TOKEN = "push_token";
    public static final String SHAREDPREFS_KEY_FIRST_START = "first_start";
    public static final String SHAREDPREFS_KEY_TASKS = "tasks";

    // Weekdays
    public static final int MONDAY = 0;
    public static final int TUESDAY = 1;
    public static final int WEDNESDAY = 2;
    public static final int THURSDAY = 3;
    public static final int FRIDAY = 4;
    public static final int SATURDAY = 5;
    public static final int SUNDAY = 6;
}
