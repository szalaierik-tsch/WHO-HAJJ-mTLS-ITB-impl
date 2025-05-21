package com.tsystems.gitb;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.Assert;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Base64DecodeTest.ConfigHolder.class)
@TestPropertySource(locations =
        {"classpath:application-test.properties",
        "classpath:application-test-local.properties"})
public class Base64DecodeTest {
    public static class ConfigHolder{} //This class is needed only to make the @Value injections work without booting the app.
    @Value("${hajjhelper.mtls.privatekey.path}")
    private String privateKeyFileLocation;
    @Value("${hajjhelper.mtls.privatekey.type}")
    private String privateKeyType;
    @Value("${hajjhelper.mtls.publiccrt.path}")
    private String publicKeyCertFileLocation;

    @Test
    public void loadFilesAndDecode() throws IOException, CertificateException, NoSuchAlgorithmException, InvalidKeySpecException, KeyStoreException, UnrecoverableKeyException, KeyManagementException, InterruptedException {
        final byte[] publicData = Files.readAllBytes(Path.of(this.publicKeyCertFileLocation));
        final byte[] privateData = Files.readAllBytes(Path.of(this.privateKeyFileLocation));

        String privateString = new String(privateData, StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s","");

        System.out.println("private string:\n" + privateString);
        byte[] encoded = Base64.getDecoder().decode(privateString.strip());

        final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        final Collection<? extends Certificate> chain = certificateFactory.generateCertificates(
                new ByteArrayInputStream(publicData));

        //EC is for the
        Key key = KeyFactory.getInstance(this.privateKeyType).generatePrivate(new PKCS8EncodedKeySpec(encoded));
//        Key key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encoded));

        KeyStore clientKeyStore = KeyStore.getInstance("jks");
        final char[] pwdChars = "test".toCharArray();
        clientKeyStore.load(null, null);
        clientKeyStore.setKeyEntry("test", key, pwdChars, chain.toArray(new Certificate[0]));

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(clientKeyStore, pwdChars);

        TrustManager[] acceptAllTrustManager = {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(
                            X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), acceptAllTrustManager, new java.security.SecureRandom());

        //https://localhost:8443/

        HttpClient client = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();

        HttpRequest exactRequest = HttpRequest.newBuilder()
//                .uri(URI.create("https://127.0.0.1"))
//                .uri(URI.create("https://127.0.0.1:8443"))
//                .uri(URI.create("https://127.0.0.1:8443/"))
//                .uri(URI.create("https://localhost"))
//                .uri(URI.create("https://localhost:8443"))
//                .uri(URI.create("https://localhost:8443/"))
                .uri(URI.create("https://tng-dev.who.int/trustList"))
                .GET()
                .build();

        var exactResponse = client.send(exactRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println(exactResponse.statusCode());
//        System.out.println(exactResponse.body());
        Files.writeString(Path.of(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+"response-body.txt"),
                exactResponse.body());
    }
}
