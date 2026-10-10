package com.google.android.gms.droidguard;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Handles the multi-step challenge-response protocol required by Play Integrity
 * when communicating with a remote DroidGuard server.
 */
public class MultiStepDroidGuardClient {
    private static final String TAG = "MultiStepDG";
    private static final int TIMEOUT_MS = 30000;

    private final Context context;
    private final String serverUrl;

    public MultiStepDroidGuardClient(Context context, String serverUrl) {
        this.context = context;
        this.serverUrl = serverUrl;
    }

    /**
     * Executes the multi-step handshake with the remote server.
     *
     * @param initialPayload The initial request payload from the client device.
     * @return The final token string.
     * @throws Exception If the protocol fails or network errors occur.
     */
    public String executeHandshake(byte[] initialPayload) throws Exception {
        Log.d(TAG, "Starting multi-step handshake");

        // Step 1: Initiate Session
        String sessionId = initiateSession(initialPayload);
        Log.d(TAG, "Session initiated: " + sessionId);

        // Step 2: Get Challenge
        byte[] challengeData = getChallenge(sessionId);
        Log.d(TAG, "Received challenge, size: " + challengeData.length);

        // Step 3: Process Challenge locally (or send back if server expects raw response)
        // In many implementations, the server provides the challenge, and the client
        // must sign it using hardware-backed keys (Keystore).
        // For Remote DroidGuard, the "Server" is the remote device.
        // Does the remote device do the signing?
        // If the remote device is rooted and has TrickyStore/etc., it can sign.
        // So we send the challenge to the server, and the server signs it.
        
        // Send Response (Signed Challenge)
        String responsePayload = sendResponse(sessionId, challengeData);
        
        // Step 4: Retrieve Final Token
        return retrieveToken(sessionId, responsePayload);
    }

    private String initiateSession(byte[] payload) throws IOException, JSONException {
        URL url = new URL(serverUrl + "/init");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/octet-stream");

        conn.getOutputStream().write(payload);
        conn.getOutputStream().flush();
        conn.getOutputStream().close();

        InputStream inputStream = conn.getInputStream();
        String response = readStream(inputStream);
        
        JSONObject json = new JSONObject(response);
        return json.getString("sessionId");
    }

    private byte[] getChallenge(String sessionId) throws IOException, JSONException {
        URL url = new URL(serverUrl + "/challenge?sessionId=" + sessionId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);

        InputStream inputStream = conn.getInputStream();
        String response = readStream(inputStream);
        
        JSONObject json = new JSONObject(response);
        // Assuming the challenge is base64 encoded in the response
        String challengeBase64 = json.getString("challenge");
        return android.util.Base64.decode(challengeBase64, android.util.Base64.DEFAULT);
    }

    private String sendResponse(String sessionId, byte[] challengeData) throws IOException, JSONException {
        // Note: In a true remote scenario, the server might need the *signed* challenge.
        // If the server is just a proxy, it might forward to Google.
        // But here the server IS the integrity provider (e.g., running TrickyStore).
        // So we need to send the challenge so the server can sign it.
        
        // However, if the server expects the client to sign, we'd need Keystore access.
        // Given the bounty context ("run strange bypassing software... on your phone"),
        // the goal is to move the "bypassing" to the SERVER.
        // So the CLIENT sends the challenge, the SERVER signs it.
        
        String challengeBase64 = android.util.Base64.encodeToString(challengeData, android.util.Base64.NO_WRAP);
        
        JSONObject json = new JSONObject();
        json.put("sessionId", sessionId);
        json.put("challenge", challengeBase64);

        URL url = new URL(serverUrl + "/response");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        conn.getOutputStream().write(json.toString().getBytes(StandardCharsets.UTF_8));
        conn.getOutputStream().flush();
        conn.getOutputStream().close();

        InputStream inputStream = conn.getInputStream();
        String response = readStream(inputStream);
        JSONObject jsonResponse = new JSONObject(response);
        return jsonResponse.getString("status"); // Or further payload
    }

    private String retrieveToken(String sessionId, String responseStatus) throws IOException, JSONException {
        URL url = new URL(serverUrl + "/token?sessionId=" + sessionId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);

        InputStream inputStream = conn.getInputStream();
        String response = readStream(inputStream);
        
        JSONObject json = new JSONObject(response);
        return json.getString("token");
    }

    private String readStream(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) > 0) {
            sb.append(new String(buffer, 0, len, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
