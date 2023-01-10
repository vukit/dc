package ru.vukit.dc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.CheckBoxPreference;

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
        CheckBoxPreference mCheckBoxPref;
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        if (sharedPreferences != null) {
            mCheckBoxPref = findPreference(KEY_BACKGROUND_SERVICE);
            if (mCheckBoxPref != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    mCheckBoxPref.setChecked(false);
                    mCheckBoxPref.setEnabled(false);
                }
            }
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_BACKGROUND_SERVICE) ||
                key.equals(KEY_SERVER_TIMEOUT) ||
                key.equals(KEY_UUID) ||
                key.equals(KEY_DATA_FORMAT)) {
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