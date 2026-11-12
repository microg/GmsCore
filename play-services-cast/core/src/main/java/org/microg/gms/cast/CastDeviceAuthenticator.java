/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast;

import android.annotation.SuppressLint;

import org.microg.gms.cast.proto.AuthChallenge;
import org.microg.gms.cast.proto.AuthResponse;
import org.microg.gms.cast.proto.CastMessage;
import org.microg.gms.cast.proto.DeviceAuthMessage;
import org.microg.gms.cast.proto.HashAlgorithm;
import org.microg.gms.cast.proto.SignatureAlgorithm;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okio.ByteString;

/**
 * Checks that the other end of a connection is a genuine Cast device.
 * <p>
 * Cast devices use a self-signed TLS certificate that changes every few days, so TLS alone says nothing about who we
 * are talking to. Instead the device proves its identity on the {@code deviceauth} namespace: we send a challenge with
 * a nonce, the device answers with a signature over the nonce and its TLS certificate, made with a key whose
 * certificate chains up to one of the Cast certificate authorities. As the signature covers the TLS certificate, a
 * device in the middle can neither forward the answer of a genuine device nor make up its own.
 * <p>
 * The rules follow the sender side of Open Screen ({@code cast/sender/channel/cast_auth_util.cc}). Revocation lists
 * are not checked, as there (they are optional).
 */
final class CastDeviceAuthenticator {
    private static final String NAMESPACE_DEVICE_AUTH = "urn:x-cast:com.google.cast.tp.deviceauth";
    private static final String SENDER_ID = "sender-0";
    private static final String RECEIVER_ID = "receiver-0";

    /**
     * Largest message the Cast protocol allows
     */
    static final int MAX_MESSAGE_SIZE = 65535;
    private static final int NONCE_SIZE = 16;
    private static final int TIMEOUT_MILLIS = 10000;
    // Devices replace their TLS certificate every other day, a certificate valid for longer is not from a Cast device
    private static final long MAX_TLS_CERTIFICATE_LIFETIME_MILLIS = 4L * 24 * 60 * 60 * 1000;
    // Other messages the device might send before it answers the challenge
    private static final int MAX_MESSAGES_BEFORE_RESPONSE = 8;

    private final Set<TrustAnchor> trustAnchors = new HashSet<>();
    private final SecureRandom random = new SecureRandom();

    CastDeviceAuthenticator(Collection<X509Certificate> certificateAuthorities) {
        for (X509Certificate certificate : certificateAuthorities) {
            this.trustAnchors.add(new TrustAnchor(certificate, null));
        }
    }

    static CastDeviceAuthenticator forCastDevices() throws CertificateException {
        return new CastDeviceAuthenticator(CastRootCertificates.get());
    }

    /**
     * Connects to the device and makes it prove that it is a Cast device.
     *
     * @return the TLS certificate the device proved to own. A later connection is to the same device if and only if
     * it presents this certificate.
     * @throws GeneralSecurityException if the device could not prove to be a Cast device
     * @throws IOException              if the device could not be reached or did not answer properly
     */
    byte[] authenticate(String host, int port) throws IOException, GeneralSecurityException {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[]{new SelfSignedTrustManager()}, this.random);
        SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket();
        try {
            socket.connect(new InetSocketAddress(host, port), TIMEOUT_MILLIS);
            socket.setSoTimeout(TIMEOUT_MILLIS);
            socket.startHandshake();
            X509Certificate tlsCertificate = (X509Certificate) socket.getSession().getPeerCertificates()[0];

            byte[] nonce = new byte[NONCE_SIZE];
            this.random.nextBytes(nonce);
            OutputStream out = socket.getOutputStream();
            out.write(frame(CastMessage.ADAPTER.encode(challengeMessage(nonce))));
            out.flush();

            AuthResponse response = readResponse(new DataInputStream(socket.getInputStream()));
            verify(response, nonce, tlsCertificate, new Date());
            return tlsCertificate.getEncoded();
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static CastMessage challengeMessage(byte[] nonce) {
        AuthChallenge challenge = new AuthChallenge.Builder()
                .signature_algorithm(SignatureAlgorithm.RSASSA_PKCS1v15)
                .sender_nonce(ByteString.of(nonce))
                .hash_algorithm(HashAlgorithm.SHA256)
                .build();
        return new CastMessage.Builder()
                .protocol_version(CastMessage.ProtocolVersion.CASTV2_1_0)
                .source_id(SENDER_ID)
                .destination_id(RECEIVER_ID)
                .namespace(NAMESPACE_DEVICE_AUTH)
                .payload_type(CastMessage.PayloadType.BINARY)
                .payload_binary(new DeviceAuthMessage.Builder().challenge(challenge).build().encodeByteString())
                .build();
    }

    private static AuthResponse readResponse(DataInputStream in) throws IOException, GeneralSecurityException {
        for (int i = 0; i < MAX_MESSAGES_BEFORE_RESPONSE; i++) {
            int size = in.readInt();
            if (size < 0 || size > MAX_MESSAGE_SIZE) {
                throw new IOException("Cast message of " + (size & 0xffffffffL) + " bytes exceeds the limit");
            }
            byte[] data = new byte[size];
            in.readFully(data);
            CastMessage message = CastMessage.ADAPTER.decode(data);
            if (!NAMESPACE_DEVICE_AUTH.equals(message.namespace)) continue;

            if (message.payload_binary == null) throw new IOException("Device authentication message without payload");
            DeviceAuthMessage deviceAuth = DeviceAuthMessage.ADAPTER.decode(message.payload_binary);
            if (deviceAuth.error != null) {
                throw new GeneralSecurityException("Device refused to authenticate");
            }
            if (deviceAuth.response == null) throw new GeneralSecurityException("Device did not answer the authentication challenge");
            return deviceAuth.response;
        }
        throw new IOException("Device did not answer the authentication challenge");
    }

    private void verify(AuthResponse response, byte[] nonce, X509Certificate tlsCertificate, Date now) throws GeneralSecurityException {
        tlsCertificate.checkValidity(now);
        if (tlsCertificate.getNotAfter().getTime() - now.getTime() > MAX_TLS_CERTIFICATE_LIFETIME_MILLIS) {
            throw new CertificateException("TLS certificate of the device is valid for too long");
        }

        if (response.signature == null || response.signature.size() == 0 || response.client_auth_certificate == null) {
            throw new GeneralSecurityException("Incomplete answer to the authentication challenge");
        }

        // Devices with old firmware don't know about the nonce and leave it out
        byte[] responseNonce = response.sender_nonce == null ? new byte[0] : response.sender_nonce.toByteArray();
        if (responseNonce.length > 0 && !Arrays.equals(responseNonce, nonce)) {
            throw new GeneralSecurityException("Answer is for a different authentication challenge");
        }

        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate deviceCertificate = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(response.client_auth_certificate.toByteArray()));
        List<X509Certificate> chain = new ArrayList<>();
        chain.add(deviceCertificate);
        for (ByteString intermediate : response.intermediate_certificate) {
            chain.add((X509Certificate) factory.generateCertificate(new ByteArrayInputStream(intermediate.toByteArray())));
        }
        X509CertSelector target = new X509CertSelector();
        target.setCertificate(deviceCertificate);
        PKIXBuilderParameters parameters = new PKIXBuilderParameters(this.trustAnchors, target);
        parameters.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(chain)));
        parameters.setRevocationEnabled(false);
        parameters.setDate(now);
        // Throws unless the device certificate chains up to one of the Cast certificate authorities
        CertPathBuilder.getInstance("PKIX").build(parameters);

        if (response.signature_algorithm != null && response.signature_algorithm != SignatureAlgorithm.RSASSA_PKCS1v15) {
            throw new GeneralSecurityException("Unsupported signature algorithm");
        }
        // Devices that leave out the hash algorithm use SHA-1
        String algorithm = response.hash_algorithm == HashAlgorithm.SHA256 ? "SHA256withRSA" : "SHA1withRSA";
        Signature verifier = Signature.getInstance(algorithm);
        verifier.initVerify(deviceCertificate.getPublicKey());
        verifier.update(responseNonce);
        verifier.update(tlsCertificate.getEncoded());
        if (!verifier.verify(response.signature.toByteArray())) {
            throw new SignatureException("Signature of the device does not match its TLS certificate");
        }
    }

    /**
     * Accepts the self-signed certificate of the device. Who the device is gets established by the authentication
     * challenge, which is bound to this certificate.
     */
    @SuppressLint("CustomX509TrustManager")
    private static final class SelfSignedTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            throw new CertificateException("Not a server");
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            if (chain == null || chain.length == 0) throw new CertificateException("Device sent no certificate");
            chain[0].checkValidity();
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    /**
     * Cast messages are preceded by their size as four bytes.
     */
    private static byte[] frame(byte[] message) {
        byte[] framed = new byte[message.length + 4];
        framed[0] = (byte) (message.length >>> 24);
        framed[1] = (byte) (message.length >>> 16);
        framed[2] = (byte) (message.length >>> 8);
        framed[3] = (byte) message.length;
        System.arraycopy(message, 0, framed, 4, message.length);
        return framed;
    }
}
