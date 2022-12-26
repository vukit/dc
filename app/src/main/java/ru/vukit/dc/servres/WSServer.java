package ru.vukit.dc.servres;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;

class WSServer extends DCServer {

    private final String url;
    private final String username;
    private final String password;
    private final String protocols;
    private WebSocket socket;

    WSServer(String id, String url, String protocols, String username, String password) {
        this.id = id;
        this.url = url;
        this.username = username;
        this.password = password;
        this.protocols = protocols;
        setup();
    }

    public void setup() {
        try {
            WebSocketFactory factory = new WebSocketFactory();
            socket = factory.createSocket(url, TIMEOUT);
            if (socket != null) {
                if (!protocols.isEmpty()) {
                    for (String protocol : protocols.split(",")) {
                        socket.addProtocol(protocol.trim());
                    }
                }
                if (!username.isEmpty() && !password.isEmpty()) {
                    socket.setUserInfo(username, password);
                }
                socket.connectAsynchronously();
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public void connect() {
        if (!socket.isOpen()) {
            disconnect();
            setup();
        }
    }

    @Override
    public void send(String data) {
        setStatus(socket.getState().toString().toLowerCase());
        if (!data.isEmpty()) socket.sendText(data);
    }

    @Override
    public void disconnect() {
        socket.disconnect();
    }

}
