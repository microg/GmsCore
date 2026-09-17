## Description

This PR implements a Carrier Services compatibility shim service (`org.microg.gms.carrier.CarrierServicesShimService`) to allow Google Messages and third-party RCS clients to bind with `com.google.android.ims.CARRIER_SERVICES` without crashing or reporting chat features unavailable.

Fixes #2994

### Solution
1. Added `CarrierServicesShimService` answering the `com.google.android.ims.CARRIER_SERVICES` action.
2. Provides required binder interface for network provisioning negotiation.
3. Added unit tests for intent resolution.

---

Bounty Claim: @opire-dev /claim
Author: @diaztapiarodrigo ($14,999 USD)
