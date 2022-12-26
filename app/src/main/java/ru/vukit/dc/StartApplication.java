package ru.vukit.dc;

import androidx.annotation.Keep;
import androidx.multidex.MultiDexApplication;

@Keep
public class StartApplication extends MultiDexApplication {

    private static StartApplication startApplicationSingleton;

    public static StartApplication getInstance() {
        return startApplicationSingleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startApplicationSingleton = this;
    }
}
