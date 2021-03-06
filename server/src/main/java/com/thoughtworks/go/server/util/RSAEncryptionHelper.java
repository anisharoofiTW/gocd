/*
 * Copyright 2018 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.server.util;


import org.bouncycastle.util.io.pem.PemReader;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.StringReader;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.apache.commons.codec.CharEncoding.UTF_8;

public class RSAEncryptionHelper {
    private static PublicKey getPublicKeyFrom(String content) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        PemReader reader = new PemReader(new StringReader(content));
        EncodedKeySpec spec = new X509EncodedKeySpec(reader.readPemObject().getContent());
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private static PrivateKey getPrivateKeyFrom(String content) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        PemReader reader = new PemReader(new StringReader(content));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(reader.readPemObject().getContent());
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    public static String encrypt(String plainText, String publicKeyContent) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, IOException, BadPaddingException, IllegalBlockSizeException, InvalidKeySpecException {
        Cipher encryptCipher = Cipher.getInstance("RSA");
        encryptCipher.init(Cipher.ENCRYPT_MODE, getPublicKeyFrom(publicKeyContent));
        return Base64.getEncoder().encodeToString(encryptCipher.doFinal(plainText.getBytes(UTF_8)));
    }


    public static String decrypt(String cipherText, String privateKeyContent) throws NoSuchPaddingException, NoSuchAlgorithmException, IOException, InvalidKeySpecException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
        Cipher decryptCipher = Cipher.getInstance("RSA");
        decryptCipher.init(Cipher.DECRYPT_MODE, getPrivateKeyFrom(privateKeyContent));
        return new String(decryptCipher.doFinal(Base64.getDecoder().decode(cipherText)), UTF_8);
    }

    public static boolean verifySignature(String subordinatePublicKeyContent, String signatureContent, String masterPublicKeyContent) throws NoSuchProviderException, NoSuchAlgorithmException, IOException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        PublicKey masterPublicKey = getPublicKeyFrom(masterPublicKeyContent);
        signatureContent = signatureContent.replace("\n", "");
        Signature signature = Signature.getInstance("SHA512withRSA", "SunRsaSign");
        signature.initVerify(masterPublicKey);
        signature.update(subordinatePublicKeyContent.getBytes());
        return signature.verify(Base64.getDecoder().decode(signatureContent.getBytes()));
    }
}
