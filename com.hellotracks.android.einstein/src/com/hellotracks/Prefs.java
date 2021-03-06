package com.hellotracks;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.preference.PreferenceManager;

import com.hellotracks.base.C;
import com.hellotracks.places.SimpleGeofenceStore;
import com.hellotracks.util.CompatibilityUtils;
import com.hellotracks.util.Utils;

public class Prefs {

    public static final String CONNECTOR_BASE_URL = Utils.hasHoneycomb() ? "http://hellotracks.com/json/" : "http://78.46.68.183/json/"; // some reason of 2.3 or volley
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
    public static final String IGNORE_GPS_SETTINGS = "ignore_gps_settings";

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
    public static final String SHOW_PARKING = "show_parking";
    public static final String IS_PREMIUM = "is_premium";
    public static final String IS_EMPLOYEE = "is_employee";
    public static final String RATEUSCOUNT = "rateus_count";
    public static final String SEND_LOCATION_TO = "send_location_to";
    public static final String INFO_READ = "info_read";

    public static final String PARKING_LAT = "parking_lat";
    public static final String PARKING_LNG = "parking_lng";
    public static final String PARKING_ACC = "parking_acc";
    public static final String PARKING_TS = "parking_ts";
    
    public static final String CREATE_PLACE_NETWORK_ACTIVATED = "create_place_network_activated";

    public static SharedPreferences get(Context context) {
        return new MySharedPreferences(PreferenceManager.getDefaultSharedPreferences(context));
    }
    
    public static class MyEditor implements Editor {
        
        private Editor ed;
        
        private MyEditor(Editor e) {
            this.ed = e;
        }

        @Override
        public void apply() {
            ed.apply();
        }

        @Override
        public Editor clear() {
            ed.clear();
            return ed;
        }

        @Override
        public boolean commit() {
            CompatibilityUtils.applyPrefsCompat(ed);
            return true;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            ed.putBoolean(key, value);
            return ed;
        }

        @Override
        public Editor putFloat(String key, float value) {
            ed.putFloat(key, value);
            return ed;
        }

        @Override
        public Editor putInt(String key, int value) {
            ed.putInt(key, value);
            return ed;
        }

        @Override
        public Editor putLong(String key, long value) {
            ed.putLong(key, value);
            return ed;
        }

        @Override
        public Editor putString(String key, String value) {
            ed.putString(key, value);
            return ed;
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB) 
        @Override
        public Editor putStringSet(String arg0, Set<String> arg1) {
            ed.putStringSet(arg0, arg1);
            return ed;
        }

        @Override
        public Editor remove(String key) {
            ed.remove(key);
            return ed;
        }
        
    }
    
    public static class MySharedPreferences implements SharedPreferences {

        private SharedPreferences prefs;
        
        public MySharedPreferences(SharedPreferences prefs) {
            this.prefs = prefs;
        }
        
        @Override
        public boolean contains(String key) {
            return prefs.contains(key);
        }

        @Override
        public Editor edit() {
            return prefs.edit();
        }

        @Override
        public Map<String, ?> getAll() {
            return prefs.getAll();
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            return prefs.getBoolean(key, defValue);
        }

        @Override
        public float getFloat(String key, float defValue) {
            return prefs.getFloat(key, defValue);
        }

        @Override
        public int getInt(String key, int defValue) {
           return prefs.getInt(key, defValue);
        }

        @Override
        public long getLong(String key, long defValue) {
            return prefs.getLong(key, defValue);
        }

        @Override
        public String getString(String key, String defValue) {
            return prefs.getString(key, defValue);
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB) 
        @Override
        public Set<String> getStringSet(String arg0, Set<String> arg1) {
            return prefs.getStringSet(arg0, arg1);
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
            prefs.registerOnSharedPreferenceChangeListener(listener);
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
            prefs.unregisterOnSharedPreferenceChangeListener(listener);
        }
        
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

    public static void removeAllLogout(Context context) {
        Map<String, ?> map = Prefs.get(context).getAll();
        Editor edit = Prefs.get(context).edit();
        for (String key : map.keySet().toArray(new String[0])) {           
            if (key.startsWith("profile") || key.startsWith("cache_")) {
                edit.remove(key);
            }
        }
        edit.remove(C.account).remove(Prefs.STATUS_ONOFF).remove(Prefs.PASSWORD)
                .remove(Prefs.USERNAME).remove(Prefs.SEND_LOCATION_TO).remove(Prefs.IS_PREMIUM)
                .remove(Prefs.IS_EMPLOYEE).remove(Prefs.NAME).remove(Prefs.MODE).remove(Prefs.PROFILE_MARKER)
                .remove(Prefs.INFO_READ).putLong(Prefs.LAST_LOGOUT, System.currentTimeMillis()).commit();
        
        new SimpleGeofenceStore(context).clearAll();
    }
    
    
    public static String createMarkerCacheId(Context context) {
        String cacheId = "cache_markers_" + Prefs.get(context).getString(Prefs.USERNAME, "");
        return cacheId;
    }
}
