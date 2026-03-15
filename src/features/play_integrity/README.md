# Play Integrity Over Remote DroidGuard

## Overview
This module provides a solution for supporting Play Integrity using a remote DroidGuard service. The integration involves sending integrity check requests to a remote server and forwarding the responses.

## Components
- **RemoteDroidGuard**: Handles communication with a remote server to send integrity check requests and retrieve results.
- **PlayIntegrityServer**: Acts as the server that receives integrity check requests, forwards them to the remote DroidGuard, and returns the results.

## Usage
1. Instantiate the `RemoteDroidGuard` with the server URL.
2. Call `verify_integrity(data)` to send integrity data for verification.
3. The `PlayIntegrityServer` listens for incoming requests and forwards them to the `RemoteDroidGuard`.

## Example
```python
# Example usage
server_url = 'https://example.com'
remote_droidguard = RemoteDroidGuard(server_url)
data = {'device_info': 'example_device_info'}
result = remote_droidguard.verify_integrity(data)
print(result)
```