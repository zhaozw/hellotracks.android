package com.hellotracks;

import java.util.Locale;
import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class Prefs {

    public static final String CONNECTOR_BASE_URL = "http://hellotracks.com/json/";
    public static final String PUSH_INTENT = "com.hellotracks.action.PUSHMSGRECEIVED";
    public static final String TAB_COCKPIT_INTENT = "com.hellotracks.action.TAB_COCKPIT";
    public static final String TAB_NETWORK_INTENT = "com.hellotracks.action.TAB_NETWORK";
    public static final String TAB_TRACKS_INTENT = "com.hellotracks.action.TAB_TRACKS";
    public static final String TAB_ACTIVITIES_INTENT = "com.hellotracks.action.TAB_ACTIVITIES";
    public static final String TAB_MAP_INTENT = "com.hellotracks.action.TAB_MAP";
    public static final String TAB_MESSAGES_INTENT = "com.hellotracks.action.TAB_MESSAGES";
    public static final String TAB_PROFILE_INTENT = "com.hellotracks.action.TAB_PROFILE";

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String MODE = "mode";
    public static final String STATUS_ONOFF = "statusOnOff";
    public static final String LASTLOG = "lastlog";
    public static final String ACTIVATE_ON_LOGIN = "activateOnLogin";
    public static final String LAST_TRANSMISSION = "lastTransmission";
    public static final String LOCATIONS_TOTAL = "locationsTotal";
    public static final String LOGINS = "logins";
    public static final String NOTIFICATION_ID = "notificationID";
    public static final String LOGGING = "logging";
    public static final String LAST_LOGOUT = "lastLogout";

    public static final String PROFILE_THUMB = "profileThumb";
    public static final String PROFILE_MARKER = "profileMarker";
    public static final String PROFILE_TYPE = "profileType";
    public static final String NO_CONTACTS = "noContacts";
    public static final String NO_PLACES = "noPlaces";
    public static final String NO_ACTIVITIES = "noActivites";
    public static final String ACCOUNT = "account";
    public static final String NAME = "name";
    public static final String EMAIL = "email";
    public static final String UNIT_DISTANCE = "unit_distance";

    public static final String CURRENT_LAT = "currentLat";
    public static final String CURRENT_LNG = "currentLng";

    public static final String TRACKLABEL_GREEN = "label_track_green";
    public static final String TRACKLABEL_YELLOW = "label_track_yellow";
    public static final String TRACKLABEL_ORANGE = "label_track_orange";
    public static final String TRACKLABEL_RED = "label_track_red";
    public static final String TRACKLABEL_VIOLETT = "label_track_violett";
    public static final String TRACKLABEL_BLUE = "label_track_blue";

    public static final String MAP_TYPE = "map_type";
    public static final String SEARCH_MAP = "search_map";

    public static final String BUSINESS = "business";
    public static final String TRACKING_AUTOSTOP_AT = "tracking_autostop_at";
    public static final String PLAN_PRODUCT = "plan_product";
    public static final String PLAN_STATUS = "plan_status";
    public static final String PLAN_ORDER = "plan_order";
    public static final String PLAN_FEEDBACK = "plan_feedback";
    public static final String SHOW_TRAFFIC = "show_traffic";
    public static final String IS_PREMIUM = "is_premium";

    public static SharedPreferences get(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean isDistanceUS(Context context) {
        String unitDist = get(context).getString(Prefs.UNIT_DISTANCE, null);
        return "US".equals(unitDist);
    }

    private static String uniqueID = null;
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";
   
    

    public synchronized static int id(Context context) {
        if (uniqueID == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(PREF_UNIQUE_ID, Context.MODE_PRIVATE);
            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString();
                Editor editor = sharedPrefs.edit();
                editor.putString(PREF_UNIQUE_ID, uniqueID);
                editor.commit();
            }
        }
        return Math.abs(uniqueID.hashCode());
    }
}
