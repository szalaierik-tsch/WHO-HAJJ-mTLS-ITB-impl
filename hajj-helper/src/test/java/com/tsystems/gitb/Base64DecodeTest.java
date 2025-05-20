package com.tsystems.gitb;

import org.junit.jupiter.api.Test;

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

public class Base64DecodeTest {

    @Test
    public void replaceNewLineInKey() {
        String key = "-----BEGIN PRIVATE KEY-----\n" +
                "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg+UN8Hw6cNEPCFc3u\n" +
                "RXQkBNii+s4hyVacRjFlevN9wXihRANCAATkMJ2ZoBlY+hY1lzaoJx8XsL9DffAU\n" +
                "qXyJmng57jumBopVtyGPEVcCjfTQ36MNsXvPg6xB6K9uFxoE9EQqXMdl\n" +
                "-----END PRIVATE KEY-----";
        String privateString = new String(key.getBytes(), StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s","");
        System.out.println(privateString);

        privateString = key
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s","");
        System.out.println(privateString);

    }

    @Test
    public void determineCharValueOfInteger() {
        int i = 10;
        char c = (char) i;
        System.out.println("char value of 10: " + c + "::");
    }

    @Test
    public void loadFilesAndDecode() throws IOException, CertificateException, NoSuchAlgorithmException, InvalidKeySpecException, KeyStoreException, UnrecoverableKeyException, KeyManagementException, InterruptedException {
        String privateKeyPath = "C:\\Development\\WHOProjects\\ExampleKeys\\generated\\TLS-XA-2025-2.key";
        String publicKeyPath = "C:\\Development\\WHOProjects\\ExampleKeys\\generated\\TLS-XA-2025.pem";
//        String privateKeyPath = "C:\\Development\\WHOProjects\\ExampleKeys\\generated\\client.key";
//        String publicKeyPath = "C:\\Development\\WHOProjects\\ExampleKeys\\generated\\client.pem";

        final byte[] publicData = Files.readAllBytes(Path.of(publicKeyPath));
        final byte[] privateData = Files.readAllBytes(Path.of(privateKeyPath));

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
        Key key = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(encoded));
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
