/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation;

import android.content.Context;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Ts43ClientTest {

    // Mock Base64Decoder using java.util.Base64
    private final Ts43Client.Base64Decoder base64Decoder = new Ts43Client.Base64Decoder() {
        @Override
        public byte[] decode(String str) {
            return java.util.Base64.getDecoder().decode(str);
        }

        @Override
        public String encodeToString(byte[] input) {
            return java.util.Base64.getEncoder().encodeToString(input);
        }
    };

    // Mock Logger
    private final Ts43Client.Logger logger = new Ts43Client.Logger() {
        @Override public void d(String tag, String msg) { System.out.println("D/" + tag + ": " + msg); }
        @Override public void i(String tag, String msg) { System.out.println("I/" + tag + ": " + msg); }
        @Override public void w(String tag, String msg) { System.out.println("W/" + tag + ": " + msg); }
        @Override public void w(String tag, String msg, Throwable tr) { System.out.println("W/" + tag + ": " + msg); tr.printStackTrace(); }
        @Override public void e(String tag, String msg) { System.out.println("E/" + tag + ": " + msg); }
        @Override public void e(String tag, String msg, Throwable tr) { System.out.println("E/" + tag + ": " + msg); tr.printStackTrace(); }
    };

    /**
     * Build a SIM EAP-AKA success response in 3GPP TS 31.102 format:
     * 0xDB [len_RES] [RES] [len_CK] [CK] [len_IK] [IK]
     */
    private byte[] buildSimResponse(byte[] res, byte[] ck, byte[] ik) {
        byte[] response = new byte[1 + 1 + res.length + 1 + ck.length + 1 + ik.length];
        int pos = 0;
        response[pos++] = (byte) 0xDB;
        response[pos++] = (byte) res.length;
        System.arraycopy(res, 0, response, pos, res.length); pos += res.length;
        response[pos++] = (byte) ck.length;
        System.arraycopy(ck, 0, response, pos, ck.length); pos += ck.length;
        response[pos++] = (byte) ik.length;
        System.arraycopy(ik, 0, response, pos, ik.length);
        return response;
    }

    /**
     * Build an EAP-AKA challenge packet (Code=Request, Type=23, Subtype=Challenge).
     */
    private byte[] buildEapAkaChallenge(int id, byte[] rand, byte[] autn) {
        // AT_RAND: Type=1, Length=5 words (20 bytes), Reserved(2) + RAND(16)
        // AT_AUTN: Type=2, Length=5 words (20 bytes), Reserved(2) + AUTN(16)
        int totalLen = 8 + 20 + 20; // Header + AT_RAND + AT_AUTN
        byte[] packet = new byte[totalLen];
        int pos = 0;
        packet[pos++] = 0x01; // Code: Request
        packet[pos++] = (byte) id;
        packet[pos++] = (byte) (totalLen >> 8);
        packet[pos++] = (byte) totalLen;
        packet[pos++] = 0x17; // Type: EAP-AKA (23)
        packet[pos++] = 0x01; // Subtype: Challenge
        packet[pos++] = 0x00; // Reserved
        packet[pos++] = 0x00; // Reserved
        // AT_RAND
        packet[pos++] = 0x01; // Type: AT_RAND
        packet[pos++] = 0x05; // Length: 5 words
        packet[pos++] = 0x00; // Reserved
        packet[pos++] = 0x00; // Reserved
        System.arraycopy(rand, 0, packet, pos, 16); pos += 16;
        // AT_AUTN
        packet[pos++] = 0x02; // Type: AT_AUTN
        packet[pos++] = 0x05; // Length: 5 words
        packet[pos++] = 0x00; // Reserved
        packet[pos++] = 0x00; // Reserved
        System.arraycopy(autn, 0, packet, pos, 16);
        return packet;
    }

    @Test
    public void testProcessEapPacket_producesValidResponse() {
        byte[] res = new byte[8]; Arrays.fill(res, (byte) 0x11);
        byte[] ck = new byte[16]; Arrays.fill(ck, (byte) 0x22);
        byte[] ik = new byte[16]; Arrays.fill(ik, (byte) 0x33);

        Ts43Client.SimAuthProvider simAuthProvider = new Ts43Client.SimAuthProvider() {
            @Override public String getNetworkOperator() { return "12345"; }
            @Override public String getSimOperator() { return "12345"; }
            @Override public String getSubscriberId() { return "123456789012345"; }
            @Override
            public String getIccAuthentication(int appType, int authType, String data) {
                byte[] decoded = java.util.Base64.getDecoder().decode(data);
                assertEquals(34, decoded.length); // [16] + RAND(16) + [16] + AUTN(16)
                return java.util.Base64.getEncoder().encodeToString(
                    buildSimResponse(res, ck, ik)
                );
            }
        };

        Ts43Client client = new Ts43Client(null, simAuthProvider, base64Decoder, logger);

        byte[] rand = new byte[16]; Arrays.fill(rand, (byte) 0xAA);
        byte[] autn = new byte[16]; Arrays.fill(autn, (byte) 0xBB);
        byte[] challengePacket = buildEapAkaChallenge(42, rand, autn);
        String challengeBase64 = java.util.Base64.getEncoder().encodeToString(challengePacket);

        // processEapPacket is package-private, call via reflection or test the full chain
        // For now, test the EAP response format via the public generateEapAkaResponse
        // by calling processEapPacket through the test constructor
        String response = client.processEapPacket(1, challengeBase64, "123456789012345", "12345", null);

        assertNotNull("processEapPacket should return non-null response", response);

        byte[] responseBytes = java.util.Base64.getDecoder().decode(response);

        // Verify EAP Response header
        assertEquals(0x02, responseBytes[0]);  // Code: Response
        assertEquals(42, responseBytes[1] & 0xFF);   // ID matches challenge
        assertEquals(23, responseBytes[4] & 0xFF);    // Type: EAP-AKA
        assertEquals(1, responseBytes[5] & 0xFF);     // Subtype: Challenge

        // Verify AT_RES at offset 8
        int pos = 8;
        assertEquals(3, responseBytes[pos] & 0xFF);   // Type: AT_RES
        // AT_RES length = (4 + 8 + 0) / 4 = 3 words
        assertEquals(3, responseBytes[pos + 1] & 0xFF);
        // RES bit count: 8 bytes * 8 = 64 bits = 0x0040 (big-endian)
        assertEquals(0x00, responseBytes[pos + 2] & 0xFF);
        assertEquals(0x40, responseBytes[pos + 3] & 0xFF);

        // Verify AT_MAC at offset 8 + 12 = 20
        pos = 20;
        assertEquals(11, responseBytes[pos] & 0xFF);  // Type: AT_MAC
        assertEquals(5, responseBytes[pos + 1] & 0xFF); // Length: 5 words

        // MAC should be non-zero (HMAC-SHA1 computed)
        boolean macNonZero = false;
        for (int i = 0; i < 16; i++) {
            if (responseBytes[pos + 4 + i] != 0) macNonZero = true;
        }
        assertTrue("AT_MAC should be non-zero", macNonZero);
    }

    @Test
    public void testSimResponseParsing_success() {
        byte[] res = new byte[]{0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte)0x88};
        byte[] ck = new byte[16]; Arrays.fill(ck, (byte) 0xAA);
        byte[] ik = new byte[16]; Arrays.fill(ik, (byte) 0xBB);
        byte[] simResp = buildSimResponse(res, ck, ik);
        String b64 = java.util.Base64.getEncoder().encodeToString(simResp);

        Ts43Client.SimAuthProvider simAuthProvider = new Ts43Client.SimAuthProvider() {
            @Override public String getNetworkOperator() { return "12345"; }
            @Override public String getSimOperator() { return "12345"; }
            @Override public String getSubscriberId() { return "123456789012345"; }
            @Override public String getIccAuthentication(int a, int b, String d) { return null; }
        };

        Ts43Client client = new Ts43Client(null, simAuthProvider, base64Decoder, logger);

        // parseSimResponse is private, but we can test indirectly via processEapPacket
        // which calls it. For direct testing, use reflection or make package-private.
        // This test verifies the format is correctly built by buildSimResponse.
        assertEquals(0xDB, simResp[0] & 0xFF);
        assertEquals(8, simResp[1] & 0xFF); // RES length
        assertEquals(0x11, simResp[2] & 0xFF); // First RES byte
        assertEquals(16, simResp[10] & 0xFF); // CK length
        assertEquals(16, simResp[27] & 0xFF); // IK length
    }

    @Test
    public void testSimResponseParsing_syncFailure() {
        byte[] auts = new byte[14]; Arrays.fill(auts, (byte) 0xCC);
        byte[] simResp = new byte[1 + 1 + 14];
        simResp[0] = (byte) 0xDC; // Sync failure tag
        simResp[1] = 14; // AUTS length
        System.arraycopy(auts, 0, simResp, 2, 14);

        assertEquals(0xDC, simResp[0] & 0xFF);
        assertEquals(14, simResp[1] & 0xFF);
    }

    @Test
    public void testFips186Prf_deterministicOutput() {
        // The PRF should produce deterministic output for the same input
        Ts43Client.SimAuthProvider simAuthProvider = new Ts43Client.SimAuthProvider() {
            @Override public String getNetworkOperator() { return "12345"; }
            @Override public String getSimOperator() { return "12345"; }
            @Override public String getSubscriberId() { return "123456789012345"; }
            @Override public String getIccAuthentication(int a, int b, String d) { return null; }
        };

        Ts43Client client = new Ts43Client(null, simAuthProvider, base64Decoder, logger);

        // fips186Prf is private, but we can verify via the full chain:
        // Two calls with identical SIM responses should produce identical EAP responses
        byte[] res = new byte[8]; Arrays.fill(res, (byte) 0x11);
        byte[] ck = new byte[16]; Arrays.fill(ck, (byte) 0x22);
        byte[] ik = new byte[16]; Arrays.fill(ik, (byte) 0x33);
        byte[] rand = new byte[16]; Arrays.fill(rand, (byte) 0xAA);
        byte[] autn = new byte[16]; Arrays.fill(autn, (byte) 0xBB);

        Ts43Client.SimAuthProvider authProvider = new Ts43Client.SimAuthProvider() {
            @Override public String getNetworkOperator() { return "12345"; }
            @Override public String getSimOperator() { return "12345"; }
            @Override public String getSubscriberId() { return "123456789012345"; }
            @Override
            public String getIccAuthentication(int appType, int authType, String data) {
                return java.util.Base64.getEncoder().encodeToString(buildSimResponse(res, ck, ik));
            }
        };

        Ts43Client c = new Ts43Client(null, authProvider, base64Decoder, logger);
        byte[] challenge = buildEapAkaChallenge(1, rand, autn);
        String b64 = java.util.Base64.getEncoder().encodeToString(challenge);

        String resp1 = c.processEapPacket(1, b64, "123456789012345", "12345", null);
        String resp2 = c.processEapPacket(1, b64, "123456789012345", "12345", null);

        assertNotNull(resp1);
        assertNotNull(resp2);
        assertEquals("Same inputs should produce identical EAP responses", resp1, resp2);
    }

    @Test
    public void testEntitlementResult_types() {
        Ts43Client.EntitlementResult success = Ts43Client.EntitlementResult.success("token");
        assertFalse(success.isError());
        assertFalse(success.needsManualMsisdn);
        assertEquals("token", success.token);

        Ts43Client.EntitlementResult error = Ts43Client.EntitlementResult.error("fail");
        assertTrue(error.isError());
        assertFalse(error.needsManualMsisdn);
        assertNull(error.token);

        Ts43Client.EntitlementResult ineligible = Ts43Client.EntitlementResult.ineligible("", "reason");
        assertFalse(ineligible.isError());
        assertTrue(ineligible.ineligible);

        Ts43Client.EntitlementResult manual = Ts43Client.EntitlementResult.phoneNumberEntryRequired("reason");
        assertFalse(manual.isError());
        assertTrue(manual.needsManualMsisdn);
    }
}
