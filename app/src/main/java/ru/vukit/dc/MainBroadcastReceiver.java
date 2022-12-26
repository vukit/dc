package ru.vukit.dc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import static ru.vukit.dc.SettingsFragment.KEY_BACKGROUND_SERVICE;

public class MainBroadcastReceiver extends BroadcastReceiver {
    SharedPreferences sharedPreferences;
    boolean backgroundService;

    @Override
    public void onReceive(Context context, Intent intent) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        backgroundService = sharedPreferences.getBoolean(KEY_BACKGROUND_SERVICE, false);
        if (backgroundService
                && intent.getAction() != null
                && (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")
                || intent.getAction().equals("ru.vukit.dc.RESTART_MAIN_SERVICE"))) {
            Intent mainServiceIntent = new Intent(context, MainService.class);
            mainServiceIntent.putExtra("launchInitiator", MainService.broadcastReceiverInitiator);
            context.startForegroundService(mainServiceIntent);
        }
    }
}