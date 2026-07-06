# RCS Support for microG

This module provides a minimal implementation of the RCS service required by Google Messages.
It stubs the necessary APIs to allow the RCS setup to complete without real carrier backend.

## Installation
1. Add the `rcs` package to your microG GmsCore source.
2. Apply the AndroidManifest.xml patch.
3. Build and install.

## Limitations
- This is a proof-of-concept; real RCS messaging will not work (sending/receiving).
- It only allows the setup UI to pass the verification step.
- For full functionality, further integration with carrier services is required.

## Credits
This work is part of the microG RCS bounty effort.