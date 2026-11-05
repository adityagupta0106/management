//package com.serviceplus.filemanagement.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.ClassPathResource;
//
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.security.KeyFactory;
//import java.security.PrivateKey;
//import java.security.PublicKey;
//import java.security.spec.PKCS8EncodedKeySpec;
//import java.security.spec.X509EncodedKeySpec;
//import java.util.Base64;
//
//@Configuration
//public class JwtConfig {
//
//    @Bean
//    public PrivateKey privateKey() throws Exception {
//
//        ClassPathResource resource = new ClassPathResource("Keys/RSAPrivateUpload.pem");
//        byte[] bytes = resource.getInputStream().readAllBytes();
//
//        String key = new String(bytes);
//
//        key = key
//                .replace("-----BEGIN PRIVATE KEY-----", "")
//                .replace("-----END PRIVATE KEY-----", "")
//                .replace("\n", "")
//                .replace("\r", "")
//                .trim();
//
//        byte[] keyBytes = Base64.getDecoder().decode(key);
//
//        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
//
//        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
//
//        return keyFactory.generatePrivate(spec);
//    }
//}
