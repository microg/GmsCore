/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.libraries.entitlement.eapaka;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.libraries.entitlement.ServiceEntitlementException;
import com.android.libraries.entitlement.utils.BytesConverter;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The class for Master Key.
 *
 * <p>Reference : RFC 4187, Section 7. Key Generation MK = SHA1(Identity|IK|CK)
 */
class MasterKey {
    private static final String TAG = "ServiceEntitlement";
    /* K_encr (128 bits) */
    private static final int LENGTH_K_ENCR = 16;
    /* K_aut (128 bits) */
    private static final int LENGTH_K_AUT = 16;
    /* Master Session Key (64 bytes) */
    private static final int LENGTH_MSK = 64;
    /* Extended Master Session Key (64 bytes) */
    private static final int LENGTH_EMSK = 64;
    /* Transient EAP Keys : K_enrc + K_aut + MSK + EMSK */
    private static final int LENGTH_TEKS = 160;

    /* Master Key */
    private byte[] mMasterKey;

    /* Transient EAP Keys */
    private byte[] mEncr;
    private byte[] mAut;
    private byte[] mMsk;
    private byte[] mEmsk;

    private MasterKey() {
    }

    /** Create the {@code masterKey}. */
    public static MasterKey create(String identity, @Nullable byte[] ik, @Nullable byte[] ck)
            throws ServiceEntitlementException {
        if (TextUtils.isEmpty(identity)
                || ik == null
                || ik.length == 0
                || ck == null
                || ck.length == 0) {
            Log.d(TAG, "Can't create master key due to invalid input!");
            return null;
        }
        MasterKey mk = new MasterKey();
        mk.from(identity, ik, ck);
        return mk;
    }

    void from(String identity, byte[] ik, byte[] ck) {
        // concatenate Identity/IK/CK
        byte[] identityBytes = identity.getBytes(UTF_8);
        byte[] data = new byte[identityBytes.length + ik.length + ck.length];
        int index = 0;
        System.arraycopy(identityBytes, 0, data, index, identityBytes.length);
        index += identityBytes.length;
        System.arraycopy(ik, 0, data, index, ik.length);
        index += ik.length;
        System.arraycopy(ck, 0, data, index, ck.length);

        // process SHA1
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(data);
            mMasterKey = messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG, "process SHA-1 failed", e);
        }

        // Generate TEKs
        generateTransientEapKeys();
    }

    /**
     * Generates TEKs base on RFC 4187, Section 7. Key Generation, snippet as below
     *
     * <p>The Master Key is fed into a Pseudo-Random number Function (PRF), which generates
     * separate
     * Transient EAP Keys (TEKs) for protecting EAP-AKA packets, as well as a Master Session Key
     * (MSK)
     * for link layer security and an Extended Master Session Key (EMSK) for other purposes.
     */
    void generateTransientEapKeys() {
        byte[] teks = generatePsudoRandomNumber();

        if (teks == null || teks.length != 160) {
            Log.e(TAG, "Invalid TEKs data!");
            return;
        }

        int index = 0;
        mEncr = new byte[LENGTH_K_ENCR];
        System.arraycopy(teks, index, mEncr, 0, LENGTH_K_ENCR);
        index += LENGTH_K_ENCR;
        mAut = new byte[LENGTH_K_AUT];
        System.arraycopy(teks, index, mAut, 0, LENGTH_K_AUT);
        index += LENGTH_K_AUT;
        mMsk = new byte[LENGTH_MSK];
        System.arraycopy(teks, index, mMsk, 0, LENGTH_MSK);
        index += LENGTH_MSK;
        mEmsk = new byte[LENGTH_EMSK];
        System.arraycopy(teks, index, mEmsk, 0, LENGTH_EMSK);
    }

    /** Returns {@code aut}. */
    public byte[] getAut() {
        return mAut;
    }

    // RFC 4187 Appendix A.  Pseudo-Random Number Generator
    @Nullable
    private byte[] generatePsudoRandomNumber() {
        // Step 1: Choose a new, secret value for the seed-key, XKEY
        byte[] key = mMasterKey;

        // 160-bit XKEY and XVAL values are used, so b = 160.  On each full
        // authentication, the Master Key is used as the initial secret seed-key
        // XKEY.
        if (key == null || key.length != 20) {
            Log.e(TAG, "Not a valid XKey!length=" + (key == null ? "null" : key.length));
            return null;
        }

        // Step 2: In hexadecimal notation let
        //     t = 67452301 EFCDAB89 98BADCFE 10325476 C3D2E1F0
        //     This is the initial value for H0|H1|H2|H3|H4
        //     in the FIPS SHS [SHA-1]
        int[] t = {0x67452301, 0xefcdab89, 0x98badcfe, 0x10325476, 0xc3d2e1f0};

        // Step 3: For j = 0 to m - 1 do
        //       3.1.  XSEED_j = 0 /* no optional user input */
        //       3.2.  For i = 0 to 1 do
        //             a.  XVAL = (XKEY + XSEED_j) mod 2^b
        //             b.  w_i = G(t, XVAL)
        //             c.  XKEY = (1 + XKEY + w_i) mod 2^b
        //       3.3.  x_j = w_0|w_1
        // Step 3: For j = 0 to m - 1 do
        //
        // Base on below snippet from RFC 4187, b is 160, x_j is 40 bytes, w_i is 20 bytes, TEKs
        // length is 160 bytes and m is 160/40=4
        //
        // 160-bit XKEY and XVAL values are used, so b = 160.  On each full
        // authentication, the Master Key is used as the initial secret seed-key
        // XKEY.  The optional user input values (XSEED_j) in step 3.1 are set
        // to zero.
        // On full authentication, the resulting 320-bit random numbers x_0,
        // x_1, ..., x_m-1 are concatenated and partitioned into suitable-sized
        // chunks and used as keys in the following order: K_encr (128 bits),
        // K_aut (128 bits), Master Session Key (64 bytes), Extended Master
        // Session Key (64 bytes).
        byte[] teks = new byte[LENGTH_TEKS];
        int index = 0;
        for (int j = 0; j < 4; j++) {
            // 3.1.  XSEED_j = 0, do nothing
            // 3.2.  For i = 0 to 1 do
            for (int i = 0; i < 2; i++) {
                // a.  XVAL = (XKEY + XSEED_j) mod 2^b
                byte[] val = key;

                // b.  w_i = G(t, XVAL)
                byte[] w = doFunctionG(t, val);
                if (w == null || w.length != 20) {
                    Log.e(TAG, "Get invalid w value from G function!");
                    return null;
                }
                // fill w to teks
                System.arraycopy(w, 0, teks, index, 20);
                index += 20;

                // c.  XKEY = (1 + XKEY + w_i) mod 2^b
                // XKEY is 20 bytes, 160 bits, mod 2^160 is for make sure XKEY just 160 bits
                int carry = 1;
                for (int k = 19; k >= 0; k--) {
                    carry += (key[k] & 0xff) + (w[k] & 0xff);
                    key[k] = (byte) (carry & 0xff);
                    // shift one byte and keep carry for next byte calculate
                    carry >>= 8;
                }
            }
            // 3.3.  x_j = w_0|w_1, already copy w_0/w_1 to output
        }

        return teks;
    }

    // See FIPS 186-2 APPENDIX 3.3. CONSTRUCTING THE FUNCTION G FROM THE SHA-1, snippet as below
    //
    // G(t,c) may be constructed using steps (a) - (e) in section 7 of the Specifications for the
    // Secure Hash Standard. Before executing these steps, {Hj} and M1 must be initialized as
    // follows:
    //
    // i. Initialize the {Hj} by dividing the 160 bit value t into five 32-bit segments as follows:
    // t = t0 || t1 || t2 || t3 || t4
    //  Then Hj = tj for j = 0 through 4.
    //
    // ii. There will be only one message block, M1, which is initialized as follows:
    //  M1 = c || 0^(512-b)
    //  (The first b bits of M1 contain c, and the remaining (512-b) bits are set to zero).
    //
    // Then steps (a) through (e) of section 7 are executed, and G(t,c) is the 160 bit string
    // represented by the five words:
    //  H0 || H1 || H2 || H3 || H4
    // at the end of step (e).
    private byte[] doFunctionG(int[] t, byte[] c) {
        // i. Initialize the {Hj} by dividing the 160 bit value t into five 32-bit segments
        // 5 segments and every segments is 32 bits/4 bytes
        byte[][] bytesH = new byte[5][4];
        for (int i = 0; i < 5; i++) {
            System.arraycopy(BytesConverter.convertIntegerTo4Bytes(t[i]), 0, bytesH[i], 0, 4);
        }

        // ii. init message block, M1
        // The first b bits of M1 contain c, and the remaining (512-b) bits are set to zero
        byte[] bytesM1 = new byte[64];
        System.arraycopy(c, 0, bytesM1, 0, 20);
        for (int i = 20; i < 64; i++) {
            bytesM1[i] = 0x00;
        }

        // See FIPS PUB 180-1, Secure Hash Standard
        //     Section 7. COMPUTING THE MESSAGE DIGEST which defined steps (a) - (e)

        // The words of the 80-word sequence are labeled W0, W1,..., W79.
        byte[][] bytesW = new byte[80][4];

        // a. Divide Mi into 16 words W0, W1, ... , W15, where W0 is the left-most word.
        for (int i = 0; i < 16; i++) {
            System.arraycopy(bytesM1, i * 4, bytesW[i], 0, 4);
        }

        // b. For t = 16 to 79 let Wt = S^1(Wt-3 XOR Wt-8 XOR Wt-14 XOR Wt-16).
        for (int i = 16; i < 80; i++) {
            bytesW[i] =
                    doFunctionS(1,
                            doXor(bytesW[i - 3], bytesW[i - 8], bytesW[i - 14], bytesW[i - 16]));
        }

        // c. Let A = H0, B = H1, C = H2, D = H3, E = H4.
        byte[] bytesA = new byte[4];
        byte[] bytesB = new byte[4];
        byte[] bytesC = new byte[4];
        byte[] bytesD = new byte[4];
        byte[] bytesE = new byte[4];
        System.arraycopy(bytesH[0], 0, bytesA, 0, 4);
        System.arraycopy(bytesH[1], 0, bytesB, 0, 4);
        System.arraycopy(bytesH[2], 0, bytesC, 0, 4);
        System.arraycopy(bytesH[3], 0, bytesD, 0, 4);
        System.arraycopy(bytesH[4], 0, bytesE, 0, 4);

        // d. For t = 0 to 79 do
        //    TEMP = S^5(A) + ft(B,C,D) + E + Wt + Kt;
        //    E = D; D = C; C = S^30(B); B = A; A = TEMP;
        for (int i = 0; i < 80; i++) {
            int tmpA = new BigInteger(doFunctionS(5, bytesA)).intValue();
            int tmpF = doFunctionF(i, bytesB, bytesC, bytesD);
            int tmpE = new BigInteger(bytesE).intValue();
            int tmpW = new BigInteger(bytesW[i]).intValue();
            int tmpK = doFunctionK(i);
            int temp = tmpA + tmpF + tmpE + tmpW + tmpK;
            bytesE = bytesD;
            bytesD = bytesC;
            bytesC = doFunctionS(30, bytesB);
            bytesB = bytesA;
            bytesA = BytesConverter.convertIntegerTo4Bytes(temp);
        }

        // e. Let H0 = H0 + A, H1 = H1 + B, H2 = H2 + C, H3 = H3 + D, H4 = H4 + E.
        bytesH[0] = addTwoBytes(bytesH[0], bytesA);
        bytesH[1] = addTwoBytes(bytesH[1], bytesB);
        bytesH[2] = addTwoBytes(bytesH[2], bytesC);
        bytesH[3] = addTwoBytes(bytesH[3], bytesD);
        bytesH[4] = addTwoBytes(bytesH[4], bytesE);

        // After processing Mn, the message digest is the 160-bit string represented by the 5 words
        // H0 H1 H2 H3 H4.
        byte[] output = new byte[20];
        System.arraycopy(bytesH[0], 0, output, 0, 4);
        System.arraycopy(bytesH[1], 0, output, 4, 4);
        System.arraycopy(bytesH[2], 0, output, 8, 4);
        System.arraycopy(bytesH[3], 0, output, 12, 4);
        System.arraycopy(bytesH[4], 0, output, 16, 4);

        return output;
    }

    private static byte[] addTwoBytes(byte[] a, byte[] b) {
        BigInteger iA = new BigInteger(a);
        BigInteger iB = new BigInteger(b);
        return BytesConverter.convertIntegerTo4Bytes(iA.add(iB).intValue());
    }

    // See FIPS PUB 180-1, Section 3. OPERATIONS ON WORDS
    // Sn(X) = (X << n) OR (X >> 32-n).
    private static byte[] doFunctionS(int n, byte[] dataX) {
        BigInteger leftShiftValue = new BigInteger(dataX).shiftLeft(n);

        // BigInteger.shiftRight would fill 1 if the left-most bit is 1, so use '>>>'
        int value = new BigInteger(dataX).intValue();
        value = value >>> (32 - n); // X should be 32 bits
        BigInteger rightShiftValue = BigInteger.valueOf(value);
        BigInteger result = leftShiftValue.or(rightShiftValue);
        return BytesConverter.convertIntegerTo4Bytes(result.intValue());
    }

    private static byte[] doXor(byte[] a, byte[] b, byte[] c, byte[] d) {
        BigInteger iA = new BigInteger(a);
        BigInteger iB = new BigInteger(b);
        BigInteger iC = new BigInteger(c);
        BigInteger iD = new BigInteger(d);
        BigInteger result = iA.xor(iB).xor(iC).xor(iD);
        return BytesConverter.convertIntegerTo4Bytes(result.intValue());
    }

    // See FIPS PUB 180-1, Section 5. FUNCTIONS USED
    // A sequence of logical functions f0, f1,..., f79 is used in the SHA-1. Each ft, 0 <= t <= 79,
    // operates on three 32-bit words B, C, D and produces a 32-bit word as output. ft(B,C,D) is
    // defined as follows: for words B, C, D,
    //
    // ft(B,C,D) = (B AND C) OR ((NOT B) AND D) (0 <= t <= 19)
    // ft(B,C,D) = B XOR C XOR D (20 <= t <= 39)
    // ft(B,C,D) = (B AND C) OR (B AND D) OR (C AND D) (40 <= t <= 59)
    // ft(B,C,D) = B XOR C XOR D (60 <= t <= 79).
    private static int doFunctionF(int t, byte[] b, byte[] c, byte[] d) {
        BigInteger iB = new BigInteger(b);
        BigInteger iC = new BigInteger(c);
        BigInteger iD = new BigInteger(d);
        BigInteger result = BigInteger.valueOf(-1);
        if (0 <= t && t <= 19) {
            result = iB.and(iC).or(iB.not().and(iD));
        } else if (20 <= t && t <= 39) {
            result = iB.xor(iC).xor(iD);
        } else if (40 <= t && t <= 59) {
            result = iB.and(iC).or(iB.and(iD)).or(iC.and(iD));
        } else if (60 <= t && t <= 79) {
            result = iB.xor(iC).xor(iD);
        }

        return result.intValue();
    }

    // See FIPS PUB 180-1, Section 6. CONSTANTS USED
    //
    // A sequence of constant words K(0), K(1), ... , K(79) is used in the SHA-1. In hex these are
    // given by
    // K = 5A827999 ( 0 <= t <= 19)
    // Kt = 6ED9EBA1 (20 <= t <= 39)
    // Kt = 8F1BBCDC (40 <= t <= 59)
    // Kt = CA62C1D6 (60 <= t <= 79).
    private static int doFunctionK(int t) {
        if (0 <= t && t <= 19) {
            return 0x5A827999;
        } else if (20 <= t && t <= 39) {
            return 0x6ED9EBA1;
        } else if (40 <= t && t <= 59) {
            return 0x8F1BBCDC;
        } else if (60 <= t && t <= 79) {
            return 0xCA62C1D6;
        }

        return -1;
    }
}
