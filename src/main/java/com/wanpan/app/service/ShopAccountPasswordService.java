package com.wanpan.app.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.xml.bind.DatatypeConverter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

@Slf4j
@Service
public class ShopAccountPasswordService {
    @Value("${security.shop-account.encrypt.password}")
    private String password;
    @Value("${security.shop-account.encrypt.key-path}")
    private String keyPath;
    @Value("${security.shop-account.encrypt.alias}")
    private String alias;
    @Value("${security.shop-account.encrypt.keystore-type}")
    private String keystoreType;
    @Value("${security.shop-account.encrypt.cipher-transformation}")
    private String cipherTransformation;


    public String decryptPassword(String password)
            throws GeneralSecurityException, IOException {
        PublicKey publicKey = getPublicKey();
        Cipher cipher = Cipher.getInstance(cipherTransformation);
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        byte[] passwordByteArray = DatatypeConverter.parseBase64Binary(password);
        byte[] decryptedMessageHash = cipher.doFinal(passwordByteArray);

        return new String(decryptedMessageHash);
    }

    private PublicKey getPublicKey()
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance(keystoreType);

        InputStream inputStream;
        if (keyPath.startsWith("/")) {
            inputStream = new FileInputStream(Paths.get(keyPath).toFile());
        } else {
            ClassPathResource classPathResource = new ClassPathResource(keyPath);
            inputStream = classPathResource.getInputStream();
        }

        keyStore.load(inputStream, password.toCharArray());
        Certificate certificate = keyStore.getCertificate(alias);

        return certificate.getPublicKey();
    }
}
