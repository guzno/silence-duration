package se.magnulund.dev.android.silencetimer.preferences;// Created by Gustav on 06/02/2014.

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;

import java.util.Calendar;

import se.magnulund.dev.android.silencetimer.models.RingerTimer;
import se.magnulund.dev.android.silencetimer.utils.DateTimeUtil;

public class BasePrefs {
    public static final String KEY_PREF_ENABLED = "pref_key_enabled";
    public static final String KEY_PREF_AUTO_TIMER_ENABLED = "pref_key_auto_timer_enabled";
    public static final String KEY_PREF_DEFAULT_TIMER = "key_default_timer";
    public static final String KEY_PREF_ALWAYS_REVERT_TO_NORMAL = "pref_key_always_revert_to_normal";

    static final String TAG = "Prefs";
    static final String KEY_PREF_INITIALIZED = "pref_key_initialized";
    static final String KEY_VERSION = "pref_version";
    static final String KEY_PREVIOUS_RINGER_MODE = "key_previous_ringer_mode";
    static final String KEY_CURRENT_RINGER_MODE = "key_current_ringer_mode";
    static final String KEY_RINGER_TIMER_ACTIVE = "key_ringer_timer_active";
    static final String KEY_RINGER_TIMER = "key_ringer_timer";
    static final String KEY_RINGER_CHANGED = "key_ringer_changed";

    final SharedPreferences prefs;
    final SharedPreferences.Editor editor;

    protected BasePrefs(Context context) {
        PackageManager packageManager = context.getPackageManager();
        assert packageManager != null;
        int versionCode;
        try {
            versionCode = packageManager.getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            versionCode = 1;
        }
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        editor = prefs.edit();
        boolean initializedNow = intializePrefsIfNeeded(context, versionCode);
        if (!initializedNow) {
            boolean updateComplete = checkPrefsVersion(versionCode);
            if (!updateComplete) {
                reInitializePrefs(context, versionCode);
            }
        }
    }

    public boolean alwaysRevertToNormal() {
        return prefs.getBoolean(KEY_PREF_ALWAYS_REVERT_TO_NORMAL, false);
    }

    public boolean isAppEnabled() {
        return prefs.getBoolean(KEY_PREF_ENABLED, false);
    }

    public boolean isAutoModeEnabled() {
        return prefs.getBoolean(KEY_PREF_AUTO_TIMER_ENABLED, false);
    }

    public RingerTimer getDefaultTimer(int targetMode, int currentMode) {
        long duration = prefs.getLong(KEY_PREF_DEFAULT_TIMER, 60) * DateTimeUtil.MILLIS_PER_MINUTE;
        return new RingerTimer(targetMode, currentMode, duration, Calendar.getInstance().getTimeInMillis());
    }

    public int getPreviousRingerMode() {
        return prefs.getInt(KEY_PREVIOUS_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL);
    }

    public void setPreviousRingerMode(int ringerMode) {
        editor.putInt(KEY_PREVIOUS_RINGER_MODE, ringerMode).commit();
    }

    public void updateRingerModes(int newRingerMode) {

        final int oldRingerMode = getCurrentRingerMode();

        setPreviousRingerMode(oldRingerMode);
        setCurrentRingerMode(newRingerMode);
    }

    public RingerTimer getRingerTimer() {
        String json = prefs.getString(KEY_RINGER_TIMER, null);
        if (json != null) {
            try {
                return RingerTimer.fromJSON(json);
            } catch (JSONException e) {
                Log.d(TAG, "Invalid json string: " + json);
                return null;
            }
        } else {
            return null;
        }
    }

    public void setRingerTimer(RingerTimer ringerTimer) {
        try {
            editor.putString(KEY_RINGER_TIMER, ringerTimer.toJSON().toString()).commit();
            setRingerTimerActive(true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void clearRingerTimer() {
        setRingerTimerActive(false);
        editor.putString(KEY_RINGER_TIMER, null).commit();
    }

    public int getCurrentRingerMode() {
        return prefs.getInt(KEY_CURRENT_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL);
    }

    public void setCurrentRingerMode(int ringerMode) {
        editor.putInt(KEY_CURRENT_RINGER_MODE, ringerMode).commit();
    }

    public boolean isRingerTimerActive() {
        return prefs.getBoolean(KEY_RINGER_TIMER_ACTIVE, false);
    }

    public void setRingerTimerActive(boolean isActive) {
        editor.putBoolean(KEY_RINGER_TIMER_ACTIVE, isActive).commit();
    }

    private boolean intializePrefsIfNeeded(Context context, int versionCode) {
        if (!prefs.getBoolean(KEY_PREF_INITIALIZED, false)) {
            initializePrefs(context, versionCode);
            
            return true;
        }
        return false;
    }

    void initializePrefs(Context context, int versionCode) {
        Log.e(TAG, "init prefs");
        editor.putBoolean(KEY_PREF_ENABLED, false)
                .putInt(KEY_VERSION, versionCode)
                .putBoolean(KEY_PREF_AUTO_TIMER_ENABLED, true)
                .putBoolean(KEY_PREF_ALWAYS_REVERT_TO_NORMAL, false)
                .putLong(KEY_PREF_DEFAULT_TIMER, 60)
                .putInt(KEY_PREVIOUS_RINGER_MODE, -1)
                .putInt(KEY_CURRENT_RINGER_MODE, ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).getRingerMode())
                .putBoolean(KEY_RINGER_TIMER_ACTIVE, false)
                .putString(KEY_RINGER_TIMER, null)
                .putBoolean(KEY_RINGER_CHANGED, false)
                .putBoolean(KEY_PREF_INITIALIZED, true)
                .commit();
    }

    private boolean checkPrefsVersion(int versionCode) {
        int storedVersionCode = prefs.getInt(KEY_VERSION, 0);
        if (storedVersionCode != versionCode) {
            return updatePrefs(storedVersionCode);
        }
        return true;
    }

    boolean updatePrefs(int versionCode) {
        switch (versionCode) {
            case 0:
            case 1:
                editor.putBoolean(KEY_PREF_ALWAYS_REVERT_TO_NORMAL, false);
            case 2:
                editor.putBoolean(KEY_RINGER_CHANGED, false);
            case 3:
            case 4:
                break;
            default:
                editor.commit();
                return false;
        }
        editor.putInt(KEY_VERSION, versionCode);
        editor.commit();
        Log.d(TAG, "updated prefs to version" + versionCode);
        return true;
    }

    private void reInitializePrefs(Context context, int versionCode) {
        editor.putBoolean(KEY_PREF_INITIALIZED, false)
                .commit();
        intializePrefsIfNeeded(context, versionCode);
    }

    public boolean getRingerChanged(){
        return prefs.getBoolean(KEY_RINGER_CHANGED, false);
    }

    public void setRingerChanged(boolean ringerChanged) {
        editor.putBoolean(KEY_RINGER_CHANGED, ringerChanged).commit();
    }
}
