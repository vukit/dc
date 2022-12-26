package ru.vukit.dc;

import androidx.annotation.Keep;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

@Keep
public
class SnackBar {

    private final AppCompatActivity activity;

    public SnackBar() {
        activity = MainActivityState.getInstance().controller;
    }

    public void ShowLong(String message) {
        if (activity != null && !message.isEmpty()) {
            Snackbar.make(activity.findViewById(R.id.cl), message, Snackbar.LENGTH_LONG).show();
        }
    }

}
