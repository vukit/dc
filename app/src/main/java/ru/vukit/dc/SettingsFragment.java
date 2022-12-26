package ru.vukit.dc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.ListPreference;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Keep
public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String KEY_FIRST_RUN = "FIRST_RUN";
    public static final String KEY_BACKGROUND_SERVICE = "BACKGROUND_SERVICE";
    public static final String KEY_STARTUP_SCREEN = "STARTUP_SCREEN";
    public static final String KEY_SERVER_TIMEOUT = "SERVER_TIMEOUT";
    public static final String KEY_UUID = "UUID";
    public static final String KEY_DATA_FORMAT = "DATA_FORMAT";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ListPreference mListPref;
        CheckBoxPreference mCheckBoxPref;
        EditTextPreference mEditTextPref;
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        if (sharedPreferences != null) {
            mListPref = findPreference(KEY_SERVER_TIMEOUT);
            if (mListPref != null) {
                mListPref.setSummary(sharedPreferences.getString(KEY_SERVER_TIMEOUT, getString(R.string.msec2000)));
            }
            mCheckBoxPref = findPreference(KEY_BACKGROUND_SERVICE);
            if (mCheckBoxPref != null) {
                mCheckBoxPref.setChecked(sharedPreferences.getBoolean(KEY_BACKGROUND_SERVICE, false));
            }
            mListPref = findPreference(KEY_STARTUP_SCREEN);
            if (mListPref != null) {
                mListPref.setSummary(sharedPreferences.getString(KEY_STARTUP_SCREEN, getString(R.string.action_about)));
            }
            mEditTextPref = findPreference(KEY_UUID);
            if (mEditTextPref != null) {
                mEditTextPref.setSummary(sharedPreferences.getString(KEY_UUID, ""));
            }
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        boolean isRestartService = false;
        if (key.equals(KEY_STARTUP_SCREEN)) {
            Preference preference = findPreference(key);
            if (preference != null) {
                preference.setSummary(sharedPreferences.getString(key, ""));
            }
        }
        if (key.equals(KEY_BACKGROUND_SERVICE)) {
            isRestartService = true;
        }
        if (key.equals(KEY_SERVER_TIMEOUT)) {
            Preference preference = findPreference(key);
            if (preference != null) {
                preference.setSummary(sharedPreferences.getString(key, ""));
                isRestartService = true;
            }
        }
        if (key.equals(KEY_UUID)) {
            Preference preference = findPreference(key);
            if (preference != null) {
                preference.setSummary(sharedPreferences.getString(key, ""));
                isRestartService = true;
            }
        }
        if (key.equals(KEY_DATA_FORMAT)) {
            isRestartService = true;
        }

        if (isRestartService) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                Context context = StartApplication.getInstance().getApplicationContext();
                Intent mainServiceIntent = new Intent(context, MainService.class);
                mainServiceIntent.putExtra("launchInitiator", MainService.applicationInitiator);
                context.stopService(mainServiceIntent);
                context.startService(mainServiceIntent);
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setTitle(getResources().getString(R.string.action_settings));
        }
        Objects.requireNonNull(getPreferenceScreen().getSharedPreferences()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Objects.requireNonNull(getPreferenceScreen().getSharedPreferences()).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
    }

}