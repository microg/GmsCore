# RCS Support for microG

## Overview
This patch adds a stub RCS service implementation to microG's GmsCore.
It intercepts RCS provisioning requests from Google Messages and returns
success with a minimal dummy configuration, allowing RCS setup to complete
without device attestation.

## Requirements
- microG GmsCore (latest version)
- Google Messages (any recent version)
- No root or bootloader unlock required

## Installation
1. Apply the patch to GmsCore source code.
2. Build and install the modified GmsCore.
3. Grant all telephony permissions to GmsCore and Google Messages.
4. Restart device and open Google Messages -> RCS setup should proceed.

## Note
This is a minimal implementation. For full functionality, additional
technology-specific configuration (e.g., MNO-specific IMSSettings) may be needed.
It provides the basic handshake that Google Messages expects.