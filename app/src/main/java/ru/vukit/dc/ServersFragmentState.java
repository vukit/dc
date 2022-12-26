package ru.vukit.dc;

import androidx.annotation.Keep;

import java.util.HashMap;

@Keep
class ServersFragmentState {

    final HashMap<String, String> serverStatus = new HashMap<>();

    private static ServersFragmentState INSTANCE = null;

    private ServersFragmentState() {
    }

    public static synchronized ServersFragmentState getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ServersFragmentState();
        }
        return (INSTANCE);
    }

    void setServerStatus(String serverId, String status) {
        serverStatus.put(serverId, status);
    }

}
