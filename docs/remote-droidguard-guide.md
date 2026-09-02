# Remote DroidGuard Server & Play Integrity Guide

This document describes how to configure and deploy a remote DroidGuard attestation server to fulfill Play Integrity and SafetyNet requests without local root requirements.

## Architecture

[ App (e.g. Dott) ] 
       │ Play Integrity Request
       ▼
[ microG GmsCore ]
       │ Multi-step HTTP session
       ▼
[ Remote DroidGuard Server ] (Device with valid GMS hardware integrity)
       │
       ▼ Returns passing attestation token
[ Google Play Integrity Backend ]

## Protocol Specification

The remote DroidGuard server must expose a POST endpoint accepting JSON:

### Request Headers
* Content-Type: application/json
* X-DroidGuard-Session: <uuid> (for multi-step Play Integrity)
* X-DroidGuard-Step: <1|2>

### Request Body Example
{
  "type": "play_integrity",
  "package": "com.ridedott.rider",
  "step": 1,
  "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "payload": "<base64_encoded_request>"
}

### Response Body Example
{
  "status": "OK",
  "token": "<attestation_jwt_token>"
}

## Multi-Step Session Flow

1. Step 1 (Init): GmsCore sends the initial device attestation request with step: 1 and a generated sessionId.
2. Intermediate Challenge (Optional): If Google's Play Integrity pipeline returns an attestation challenge, the server responds with:
   {
     "status": "CONTINUE",
     "intermediateChallenge": "<challenge_data>"
   }
3. Step 2 (Response): GmsCore computes the response and posts it with step: 2 using the identical sessionId.
4. Final Token: The server delivers the verified integrity verdict token back to GmsCore, satisfying Firebase App Check and SMS phone verification.

## Configuring microG

In microG Settings -> Self-Check -> Remote DroidGuard:
1. Enable Remote DroidGuard.
2. Enter your server URL: https://your-droidguard-server.example.com/attest.
