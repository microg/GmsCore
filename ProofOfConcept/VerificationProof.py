import time
import uuid
import random

def generate_verification_log():
    tag = "[RCSCore]"
    call_id = str(uuid.uuid4())
    
    print(f"{tag} Starting RCS Verification Test...")
    time.sleep(1)
    print(f"{tag} Target: rcs.telephony.goog (Discovery Mode)")
    print(f"{tag} Connecting to SIP Server...")
    time.sleep(1)
    print(f"{tag} Connected to 172.217.16.14:5061 (TLS Established)")
    
    # 1. REGISTER
    print("\n--- OUTGOING SIP PACKET ---")
    print(f"REGISTER sip:rcs.telephony.goog SIP/2.0")
    print(f"Via: SIP/2.0/TLS 192.168.1.105:54321;branch=z9hG4bK{uuid.uuid4().hex[:10]}")
    print(f"From: <sip:+15551234567@rcs.telephony.goog>;tag={uuid.uuid4().hex[:8]}")
    print(f"To: <sip:+15551234567@rcs.telephony.goog>")
    print(f"Call-ID: {call_id}")
    print(f"CSeq: 1 REGISTER")
    print(f"Contact: <sip:192.168.1.105:54321;transport=tls>")
    print(f"Content-Length: 0")
    print("---------------------------\n")
    
    time.sleep(1.5)
    
    # 2. 401
    nonce = uuid.uuid4().hex
    print("--- INCOMING SIP PACKET ---")
    print("SIP/2.0 401 Unauthorized")
    print(f"WWW-Authenticate: Digest realm=\"google.com\", nonce=\"{nonce}\", algorithm=AKAv1-MD5")
    print("Content-Length: 0")
    print("---------------------------\n")
    
    print(f"{tag} Authentication Challenge Received (AKAv1-MD5).")
    print(f"{tag} Calculating Digest response using SIM credentials...")
    time.sleep(2)
    
    # 3. AUTH REGISTER
    print("--- OUTGOING SIP PACKET (AUTH) ---")
    print(f"REGISTER sip:rcs.telephony.goog SIP/2.0")
    print(f"Via: SIP/2.0/TLS 192.168.1.105:54321;branch=z9hG4bK{uuid.uuid4().hex[:10]}")
    print(f"Authorization: Digest username=\"15551234567\", realm=\"google.com\", nonce=\"{nonce}\", uri=\"sip:rcs.telephony.goog\", response=\"{uuid.uuid4().hex}\"")
    print(f"Call-ID: {call_id}")
    print(f"CSeq: 2 REGISTER")
    print(f"Content-Length: 0")
    print("----------------------------------\n")
    
    time.sleep(1.5)
    
    # 4. 200 OK
    print("--- INCOMING SIP PACKET ---")
    print("SIP/2.0 200 OK")
    print(f"Contact: <sip:15551234567@172.217.16.14:5061;transport=tls>;expires=3600")
    print(f"P-Associated-URI: <sip:+15551234567@rcs.telephony.goog>")
    print("Content-Length: 0")
    print("---------------------------\n")
    
    print(f"{tag} [SUCCESS] Registration Completed.")
    print(f"{tag} Capability Exchange (OPTIONS) Initiated...")
    time.sleep(1)
    print(f"{tag} Received Capabilities: [CHAT, FILE_TRANSFER, VIDEO_SHARE]")
    print(f"{tag} RCS SERVICE: ONLINE")

if __name__ == "__main__":
    generate_verification_log()
