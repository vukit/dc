package ru.vukit.dc.servres;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

class HTTPSServer extends DCServer {

    private final String url;
    private final String username;
    private final String password;
    private final Context context;
    private RequestQueue queue;
    private boolean isError;

    HTTPSServer(Context context, String id, String url, String username, String password) {
        this.id = id;
        this.url = url;
        this.username = username;
        this.password = password;
        this.context = context;
        setup();
    }

    @SuppressLint("CustomX509TrustManager")
    public void setup() {
        isError = false;
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    @SuppressLint("TrustAllX509TrustManager")
                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    @SuppressLint("TrustAllX509TrustManager")
                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((arg0, arg1) -> true);
            queue = Volley.newRequestQueue(context);
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            isError = true;
            setStatus(Objects.requireNonNull(e.getMessage()).split(":")[1].trim().toLowerCase());
        }
    }

    @Override
    public void connect() {
        if (isError) {
            disconnect();
            setup();
        }
    }

    @Override
    public void disconnect() {
        queue.stop();
    }

    @Override
    public void send(final String data) {
        if (!isError) {
            final StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<>() {
                        String responseMessage = "";

                        @Override
                        public void onResponse(String response) {
                            if (Integer.parseInt(response) == HttpURLConnection.HTTP_OK) {
                                responseMessage = STATUS_OK;
                            } else {
                                responseMessage = String.valueOf(Integer.parseInt(response));
                            }
                            setStatus(responseMessage);
                        }
                    },
                    new Response.ErrorListener() {
                        String errorMessage;

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            isError = true;
                            if (error.toString().contains("Timeout")) errorMessage = "timeout";
                            else if (error.toString().contains("NoConnection")) {
                                if (error.toString().contains("SSLHandshakeException")) {
                                    errorMessage = "ssl handshake exception";
                                } else {
                                    errorMessage = "no connection";
                                }
                            } else if (error.toString().contains("AuthFailure")) {
                                errorMessage = "unauthorized";
                            } else if (error.toString().contains("failed to connect")) {
                                errorMessage = "no connection";
                            } else errorMessage = STATUS_UNKNOWN;
                            setStatus(errorMessage);
                        }
                    }
            ) {
                int statusCode;

                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() {
                    return data.isEmpty() ? null : data.getBytes(StandardCharsets.UTF_8);
                }

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    if (!username.isEmpty() && !password.isEmpty()) {
                        HashMap<String, String> headers = new HashMap<>();
                        String credentials = username + ":" + password;
                        String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                        headers.put("Authorization", auth);
                        return headers;
                    } else return super.getHeaders();
                }

                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    statusCode = response.statusCode;
                    return super.parseNetworkResponse(response);
                }

                @Override
                protected void deliverResponse(String response) {
                    super.deliverResponse(String.valueOf(statusCode));
                }

            };
            stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                    TIMEOUT,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(stringRequest);
        }
    }

}
