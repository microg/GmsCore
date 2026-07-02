#!/usr/bin/env python3
"""Quick test for the DroidGuard server."""
import json, time, urllib.request, urllib.error, threading, sys

def run_server(port=18080):
    import subprocess
    p = subprocess.Popen([sys.executable, "play-services-droidguard/server/droidguard_server.py", "--port", str(port)],
                         stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    time.sleep(1)
    return p

def test_server(port=18080):
    base = f"http://localhost:{port}"
    
    # Test health
    req = urllib.request.Request(f"{base}/health")
    resp = urllib.request.urlopen(req)
    data = json.loads(resp.read())
    assert data["status"] == "ok", f"Health check failed: {data}"
    print(f"[PASS] Health check: {data}")
    
    # Test root
    req = urllib.request.Request(f"{base}/")
    resp = urllib.request.urlopen(req)
    data = json.loads(resp.read())
    assert data["service"] == "DroidGuard Remote Server"
    print(f"[PASS] Root endpoint: {data[\"service\"]}")
    
    # Test session creation
    req = urllib.request.Request(f"{base}/droidguard/session?flow=play_integrity&source=com.test.app&steps=2",
                                 data=b"{}", method="POST")
    resp = urllib.request.urlopen(req)
    data = json.loads(resp.read())
    session_id = data["sessionId"]
    assert data["totalSteps"] == 2
    print(f"[PASS] Session created: {session_id} (flow={data[\"flow\"]}, steps={data[\"totalSteps\"]})")
    
    # Test step execution
    req = urllib.request.Request(f"{base}/droidguard/session/{session_id}/step?step=1",
                                 data=json.dumps({"data": {"test": "value"}}).encode(), method="POST")
    resp = urllib.request.urlopen(req)
    data = json.loads(resp.read())
    assert "result" in data
    assert data["step"] == 1
    print(f"[PASS] Step 1 completed: completed={data[\"completed\"]}, nextStep={data.get(\"nextStep\")}")
    
    # Test step 2
    req = urllib.request.Request(f"{base}/droidguard/session/{session_id}/step?step=2",
                                 data=json.dumps({"data": {"test": "value2"}}).encode(), method="POST")
    resp = urllib.request.urlopen(req)
    data = json.loads(resp.read())
    assert data["completed"] == True
    print(f"[PASS] Step 2 completed: completed={data[\"completed\"]}")
    
    # Test close
    req = urllib.request.Request(f"{base}/droidguard/session/{session_id}/close",
                                 data=b"{}", method="POST")
    resp = urllib.request.urlopen(req)
    data = json.loads(resp.read())
    assert data["state"] == "closed"
    print(f"[PASS] Session closed")
    
    print("\nAll tests passed!")

if __name__ == "__main__":
    port = 18080
    proc = run_server(port)
    try:
        test_server(port)
    finally:
        proc.terminate()
        proc.wait()

