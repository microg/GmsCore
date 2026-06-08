---
[BOUNTY] Support Play Integrity over remote DroidGuard + Server/Guide [$100]
PLATFORM: issuehunt | VALUE: $100 USD
DESCRIPTION: Play Integrity should be supported over the _remote DroidGuard_ functionality and there should be documentation how to set up a phone as a DroidGuard server.

**Why?**
While existing solutions allow getting a sufficiently passing Play Integrity token with a non-integrity-complia

ACTUAL REPO CODE (use these exact function names, imports, and patterns):
// FILE: firebase-auth/src/main/java/com/google/firebase/auth/PhoneAuthCredential.java
import org.microg.safeparcel.AutoSafeParcelable;
import com.google.firebase.auth.PhoneAuthCredential;

public class PhoneAuthCredential extends AuthCredential {
    @Field(1)
    @PublicApi(exclude = true)
    public String sessionInfo;
    @Field(2)
    @PublicApi(exclude = true)
    public String smsCode;
    @Field(3)
    @PublicApi(exclude = true)
    public boolean hasVerificationCode;
    @Field(4)
    @PublicApi(exclude = true)
    public String phoneNumber;
    @Field(5)
    @PublicApi(exclude = true)
    public boolean autoCreate;
    @Field(6)
    @PublicApi(exclude = true)
    public String temporaryProof;
    @Field(7)
    @PublicApi(exclude = true