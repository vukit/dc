package ru.vukit.dc.servres;

import android.content.Context;

import androidx.annotation.Nullable;

public class ServerFactory {

    @Nullable
    public static DCServer makeServer(Context context, String id, String url, String username, String password, String protocols) {
        try {
            switch (url.split(":")[0]) {
                case "ws":
                    return new WSServer(id, url, protocols, username, password);
                case "wss":
                    return new WSSServer(id, url, protocols, username, password);
                case "http":
                    return new HTTPServer(context, id, url, username, password);
                case "https":
                    return new HTTPSServer(context, id, url, username, password);
            }
        } catch (IllegalArgumentException ignored) {

        }
        return null;
    }

}
