# Remote DroidGuard for Play Integrity

This guide explains how to run Play Integrity through a remote DroidGuard server.

## Requirements

- A microG device (client).
- A stock Android device with Google Play Services (DroidGuard server).
- Both devices must be able to reach each other over HTTP.

## Server setup

1. Install a DroidGuard server app on the stock Android device.
2. The server must expose `POST /droidguard`.
3. The server must accept and return JSON as described below.
4. Make sure the server keeps state per `sessionId`. Play Integrity uses a multi-step DroidGuard process; a new session must be reused across requests.

## Client setup

1. Open the microG settings on the client device.
2. Set the DroidGuard server URL to `http://<server-ip>:8080/droidguard`.
3. Use a Play Integrity checker to verify the token.

## Protocol

### Request

{
  "request": "<base64-encoded DroidGuard request>",
  "hashedClientPackage": "<base64-encoded package hash, optional>",
  "flow": "playintegrity",
  "sessionId": "<opaque session id, optional on first request>"
}
### Response

{
  "response": "<base64-encoded DroidGuard response>",
  "sessionId": "<opaque session id, required for multi-step requests>"
}
The client sends `sessionId` on every request after the first one. The server must keep the DroidGuard session alive for that `sessionId` until Play Integrity completes its multi-step flow.