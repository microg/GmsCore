package org.microg.gms.droidguard;

import android.content.Context;
import android.util.Base64;

import com.google.android.gms.droidguard.internal.DroidGuardRequest;
import com.google.android.gms.droidguard.internal.DroidGuardResponse;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RemoteDroidGuard {
    private final Context context;

    public RemoteDroidGuard(Context context) {
        this.context = context;
    }

    public DroidGuardResponse process(DroidGuardRequest request) throws IOException {
        try {
            String serverUrl = context.getSharedPreferences("droidguard", Context.MODE_PRIVATE)
                    .getString("server_url", "http://10.0.2.2:8080/droidguard");

            JSONObject payload = new JSONObject();
            payload.put("request", Base64.encodeToString(request.request, Base64.NO_WRAP));
            if (request.hashedClientPackage != null) {
                payload.put("hashedClientPackage", Base64.encodeToString(request.hashedClientPackage, Base64.NO_WRAP));
            }
            if (request.flow != null) {
                payload.put("flow", request.flow);
            }
            if (request.sessionId != null && !request.sessionId.isEmpty()) {
                payload.put("sessionId", request.sessionId);
            }

            HttpURLConnection connection = (HttpURLConnection) new URL(serverUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.toString().getBytes("UTF-8"));
            }

            int code = connection.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? connection.getInputStream() : connection.getErrorStream();
            String body = readAll(is);
            is.close();

            if (code < 200 || code >= 300) {
                throw new IOException("Remote DroidGuard error: " + body);
            }

            JSONObject responseJson = new JSONObject(body);
            if (!responseJson.has("response")) {
                throw new IOException("Remote DroidGuard response missing 'response'");
            }

            DroidGuardResponse response = new DroidGuardResponse();
            response.response = Base64.decode(responseJson.getString("response"), Base64.NO_WRAP);

            String sessionId = responseJson.optString("sessionId", null);
            if (sessionId != null && !sessionId.isEmpty()) {
                response.sessionId = sessionId;
            }

            return response;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Remote DroidGuard request failed", e);
        }
    }

    private static String readAll(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toString("UTF-8");
    }
}