package ru.vukit.dc;

import androidx.annotation.Keep;

import java.util.ArrayDeque;
import java.util.Deque;

@Keep
class MainActivityState {

    MainActivity controller;
    final Deque<String> fragmentStack = new ArrayDeque<>();
    boolean isCheckedPermission = false;
    boolean permissionInternet = false;
    boolean permissionAccessFineLocation = false;
    boolean permissionAccessCoarseLocation = false;
    boolean isFirstStart = false;

    private static class SingletonHolder {
        static final MainActivityState HOLDER_INSTANCE = new MainActivityState();
    }

    @SuppressWarnings("SameReturnValue")
    static MainActivityState getInstance() {
        return SingletonHolder.HOLDER_INSTANCE;
    }

    void connectController(MainActivity controller) {
        this.controller = controller;
    }

    void disconnectController() {
        this.controller = null;
    }
}
