/********************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *******************************************************************************/

package org.eclipse.winery.security;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.eclipse.winery.security.algorithm.encryption.EncryptionAlgorithm;
import org.eclipse.winery.security.datatypes.DistinguishedName;
import org.eclipse.winery.security.exceptions.GenericSecurityProcessorException;
import org.eclipse.winery.security.support.KeyManagementHelper;
import org.eclipse.winery.security.support.SignatureAlgorithmEnum;

import org.apache.commons.configuration2.Configuration;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.apache.commons.codec.binary.Base64;
//import org.apache.commons.configuration.Configuration;

public class BCSecurityProcessor implements SecurityProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BCSecurityProcessor.class);
    private Configuration configuration;
    private EncryptionAlgorithm symmetricEncryption;

    public BCSecurityProcessor(Configuration c) {
        this.configuration = c;
        Security.addProvider(new BouncyCastleProvider());
        // Available since Java8u151, allows 256bit key usage
        Security.setProperty("crypto.policy", "unlimited");
    }

    public BCSecurityProcessor() {
        this(null);
    }

    @Override
    public SecretKey generateSecretKey(String algorithm, int keySize) throws GenericSecurityProcessorException {
        return KeyManagementHelper.generateSecretKey(algorithm, keySize);
    }

    @Override
    public KeyPair generateKeyPair(String algorithm, int keySize) throws GenericSecurityProcessorException {
        return KeyManagementHelper.generateKeyPair(algorithm, keySize);
    }

    @Override
    public SecretKey getSecretKeyFromInputStream(String algorithm, InputStream secretKeyInputStream) throws GenericSecurityProcessorException {
        return  KeyManagementHelper.getSecretKeyFromInputStream(algorithm, secretKeyInputStream);
    }

    @Override
    public PrivateKey getPKCS8PrivateKeyFromInputStream(String algorithm, InputStream privateKeyInputStream) throws GenericSecurityProcessorException {
        return KeyManagementHelper.getPKCS8PrivateKeyFromInputStream(algorithm, privateKeyInputStream);
    }

    @Override
    public PublicKey getX509EncodedPublicKeyFromInputStream(String algorithm, InputStream publicKeyInputStream) throws GenericSecurityProcessorException {
        return  KeyManagementHelper.getX509EncodedPublicKeyFromInputStream(algorithm, publicKeyInputStream);
    }
    
    @Override
    public Certificate generateSelfSignedX509Certificate(KeyPair keypair, DistinguishedName distinguishedName) throws GenericSecurityProcessorException {
        String signatureAlgorithm;
        try {
            signatureAlgorithm = SignatureAlgorithmEnum.getDefaultOptionForAlgorithm(keypair.getPrivate().getAlgorithm());
        } catch (IllegalArgumentException e) {
            LOGGER.error("Signature algorithm for keypair algorithm is not found", e);
            throw new GenericSecurityProcessorException("Signature algorithm for keypair algorithm is not found", e);
        }
        try {
            X500Name dn = buildX500Name(distinguishedName);

            long now = System.currentTimeMillis();
            Date startDate = new Date(now);
            BigInteger certSerialNumber = new BigInteger(Long.toString(now));
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);
            calendar.add(Calendar.YEAR, 1); // <-- 1 Yr validity

            Date endDate = calendar.getTime();

            ContentSigner sigGen = new JcaContentSignerBuilder(signatureAlgorithm)
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(keypair.getPrivate());

            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                dn,
                certSerialNumber,
                startDate,
                endDate,
                dn,
                keypair.getPublic()
            ).addExtension(new ASN1ObjectIdentifier("2.5.29.35"), false, new AuthorityKeyIdentifier(keypair.getPublic().getEncoded())
            ).addExtension(new ASN1ObjectIdentifier("2.5.29.19"), false, new BasicConstraints(false) // true if it is allowed to sign other certs
            ).addExtension(new ASN1ObjectIdentifier("2.5.29.15"), true, new X509KeyUsage(
                X509KeyUsage.digitalSignature |
                    X509KeyUsage.nonRepudiation |
                    X509KeyUsage.keyEncipherment |
                    X509KeyUsage.dataEncipherment)
            );

            X509CertificateHolder certHolder = certBuilder.build(sigGen);

            return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(certHolder);
        } catch (OperatorCreationException | CertIOException | CertificateException e) {
            LOGGER.error("Error generating a self-signed certificate", e);
            throw new GenericSecurityProcessorException("Error generating a self-signed certificate", e);
        }
    }

    @Override
    public Certificate[] getX509Certificates(InputStream certInputStream) throws GenericSecurityProcessorException {
        try {
            return createCertificates(certInputStream);
        } catch (Exception e) {
            LOGGER.error("Error processing the provided X509 certificate", e);
            throw new GenericSecurityProcessorException("Error processing the provided X509 certificate chain", e);
        }
    }

    private Certificate[] createCertificates(InputStream certInputStream) throws Exception {
        final CertificateFactory factory = CertificateFactory.getInstance("X.509");

        final List<X509Certificate> result = new ArrayList<>();
        final BufferedReader r = new BufferedReader(new InputStreamReader(certInputStream));

        String s = r.readLine();
        if (s == null || !s.contains("BEGIN CERTIFICATE")) {
            r.close();
            throw new GenericSecurityProcessorException("Error processing the provided X509 certificate chain");
        }

        StringBuilder b = new StringBuilder();
        while (s != null) {
            if (s.contains("END CERTIFICATE")) {
                String hexString = b.toString();
                final byte[] bytes = Base64.getDecoder().decode(hexString);
                X509Certificate cert = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(bytes));
                result.add(cert);
                b = new StringBuilder();
            } else {
                if (!s.startsWith("----")) {
                    b.append(s);
                }
            }
            s = r.readLine();
        }
        r.close();

        return result.toArray(new X509Certificate[result.size()]);
    }

    @Override
    public byte[] encryptBytes(Key k, byte[] sequence) throws GenericSecurityProcessorException {
        try {
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            final int blockSize = cipher.getBlockSize();

            // generate random IV using block size
            final byte[] ivData = new byte[blockSize];
            final SecureRandom rnd = SecureRandom.getInstance("SHA1PRNG");
            rnd.nextBytes(ivData);
            final IvParameterSpec iv = new IvParameterSpec(ivData);

            cipher.init(Cipher.ENCRYPT_MODE, k, iv);

            final byte[] encryptedMessage = cipher.doFinal(sequence);

            // concatenate IV and encrypted message
            final byte[] ivAndEncryptedMessage = new byte[ivData.length + encryptedMessage.length];
            System.arraycopy(ivData, 0, ivAndEncryptedMessage, 0, blockSize);
            System.arraycopy(encryptedMessage, 0, ivAndEncryptedMessage, blockSize, encryptedMessage.length);

            return ivAndEncryptedMessage;
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException e) {
            LOGGER.error("Error processing the encryption request", e);
            throw new GenericSecurityProcessorException("Error processing the encryption request", e);
        }
    }

    @Override
    public byte[] decryptBytes(Key k, byte[] sequence) throws GenericSecurityProcessorException {
        try {
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            final int blockSize = cipher.getBlockSize();

            // retrieve random IV from start of the received message
            final byte[] ivData = new byte[blockSize];
            System.arraycopy(sequence, 0, ivData, 0, blockSize);
            final IvParameterSpec iv = new IvParameterSpec(ivData);

            // retrieve the encrypted message itself
            final byte[] encryptedMessage = new byte[sequence.length - blockSize];
            System.arraycopy(sequence, blockSize, encryptedMessage, 0, encryptedMessage.length);

            cipher.init(Cipher.DECRYPT_MODE, k, iv);

            return cipher.doFinal(encryptedMessage);
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException | NoSuchPaddingException e) {
            LOGGER.error("Error processing the decryption request", e);
            throw new GenericSecurityProcessorException("Error processing the decryption request", e);
        }
    }

    @Override
    public String calculateDigest(String str, String digestAlgorithm) throws GenericSecurityProcessorException {
        try {
            return calculateDigest(str.getBytes(StandardCharsets.UTF_8), digestAlgorithm);
        } catch (GenericSecurityProcessorException e) {
            LOGGER.error("Error calculating hash", e);
            throw e;
        }
    }

    @Override
    public String calculateDigest(byte[] bytes, String digestAlgorithm) throws GenericSecurityProcessorException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(digestAlgorithm);
            md.update(bytes);
            byte[] digest = md.digest();
            return String.format("%064x", new BigInteger(1, digest));
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Error calculating hash", e);
            throw new GenericSecurityProcessorException("Error calculating hash", e);
        }
    }

    @Override
    public byte[] signText(Key privateKey, String text) throws GenericSecurityProcessorException {
        return signBytes(privateKey, text.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public byte[] signBytes(Key privateKey, byte[] text) throws GenericSecurityProcessorException {
        try {
            String algo = SignatureAlgorithmEnum.getDefaultOptionForAlgorithm(privateKey.getAlgorithm());
            Signature privateSignature = Signature.getInstance(algo);
            privateSignature.initSign((PrivateKey) privateKey);
            privateSignature.update(text);

            return privateSignature.sign();
        } catch (SignatureException | InvalidKeyException | NoSuchAlgorithmException e) {
            LOGGER.error("Error calculating hash", e);
            throw new GenericSecurityProcessorException("Error signing the provided string", e);
        }
    }

    @Override
    public boolean verifyText(Certificate cert, String text, byte[] signature) throws GenericSecurityProcessorException {
        return verifyBytes(cert, text.getBytes(StandardCharsets.UTF_8), signature);
    }

    @Override
    public boolean verifyBytes(Certificate cert, byte[] text, byte[] signature) throws GenericSecurityProcessorException {
        try {
            PublicKey publicKey = cert.getPublicKey();
            String algo = SignatureAlgorithmEnum.getDefaultOptionForAlgorithm(publicKey.getAlgorithm());
            Signature publicSignature = Signature.getInstance(algo);
            publicSignature.initVerify(publicKey);
            publicSignature.update(text);

            return publicSignature.verify(signature);
        } catch (SignatureException | InvalidKeyException | NoSuchAlgorithmException e) {
            LOGGER.error("Error verifying provided data", e);
            throw new GenericSecurityProcessorException("Error verifying provided data", e);
        }
    }

    private X500Name buildX500Name(DistinguishedName distinguishedName) throws GenericSecurityProcessorException {
        if (distinguishedName.isValid()) {
            Map<String, String> rdns = distinguishedName.getIdentityData();

            X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
            builder.addRDN(BCStyle.CN, rdns.get("CN"));
            builder.addRDN(BCStyle.O, rdns.get("O"));
            builder.addRDN(BCStyle.C, rdns.get("C"));
            if (rdns.containsKey("OU")) {
                builder.addRDN(BCStyle.OU, rdns.get("OU"));
            }
            if (rdns.containsKey("L")) {
                builder.addRDN(BCStyle.L, rdns.get("L"));
            }
            if (rdns.containsKey("ST")) {
                builder.addRDN(BCStyle.ST, rdns.get("ST"));
            }

            return builder.build();
        }

        throw new GenericSecurityProcessorException("The provided distinguished name either is not valid or incomplete");
    }
}