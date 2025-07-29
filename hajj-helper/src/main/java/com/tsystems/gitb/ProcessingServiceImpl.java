package com.tsystems.gitb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.ps.Void;
import com.gitb.ps.*;
import com.gitb.tr.TestResultType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spring component that realises the processing service.
 */
@Component
public class ProcessingServiceImpl implements ProcessingService {

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(ProcessingServiceImpl.class);

    static final Pattern VALIDATION_METHOD_ID_PATTERN = Pattern.compile(
            "did:web:tng-cdn-dev\\.who\\.int:v2:trustlist:([\\w-]+):(\\w+):(\\w+)#([/\\w+=]+)"
    );

    @Autowired
    private Utils utils = null;

    /**
     * The purpose of the getModuleDefinition call is to inform its caller on how the service is supposed to be called.
     * <p/>
     * Note that defining the implementation of this service is optional, and can be empty unless you plan to publish
     * the service for use by third parties (in which case it serves as documentation on its expected inputs and outputs).
     *
     * @param parameters No parameters are expected.
     * @return The response.
     */
    @Override
    public GetModuleDefinitionResponse getModuleDefinition(Void parameters) {
        return new GetModuleDefinitionResponse();
    }

    /**
     * The purpose of the process operation is to execute one of the service's supported operations.
     * <p/>
     * What would typically take place here is as follows:
     * <ol>
     *    <li>Check that the requested operation is indeed supported by the service.</li>
     *    <li>For the requested operation collect and check the provided input parameters.</li>
     *    <li>Perform the requested operation and return the result to the test bed.</li>
     * </ol>
     *
     * @param processRequest The requested operation and input parameters.
     * @return The result.
     */
    @Override
    public ProcessResponse process(ProcessRequest processRequest) {
        LOG.info("Received 'process' command from test bed for session [{}]", processRequest.getSessionId());
        String operation = processRequest.getOperation();
        if (operation == null) {
            throw new IllegalArgumentException("No processing operation provided");
        }
        switch (operation) {
            case "connectToTrustlist": return getHttpResponse(processRequest);
            case "processDIDjson": return processDIDJSON(processRequest);
            default: throw new IllegalArgumentException(String.format("Unexpected operation [%s].", operation));
        }
    }

    private ProcessResponse processDIDJSON(ProcessRequest processRequest) {
        ProcessResponse processingResponse = new ProcessResponse();
        String didJSON = utils.getRequiredString(processRequest.getInput(), "DIDjson");
        String queriedDomain = utils.getRequiredString(processRequest.getInput(), "queriedDomain");
        String queriedCountry = utils.getRequiredString(processRequest.getInput(), "queriedCountry");
        LOG.info("Got DID JSON");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = null;
        try {
            root = mapper.readTree(didJSON);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        JsonNode verificationMethods = root.path("verificationMethod");

        for (JsonNode method : verificationMethods) {
            String id = method.path("id").asText();
            Matcher matcher = VALIDATION_METHOD_ID_PATTERN.matcher(id);

            if (matcher.matches()) {
                if(queriedCountry.equals(matcher.group(2)) &&
                    queriedDomain.equals(matcher.group(1))) {
                    String issuerType = matcher.group(1);
                    String countryCode = matcher.group(2);
                    String keyType = matcher.group(3);
                    String keyId = matcher.group(4);

                    LOG.debug("Found issuer type [{}], country code [{}], key type [{}] and key ID [{}].",
                            issuerType, countryCode, keyType, keyId);
                    processingResponse.setReport(utils.createReport(TestResultType.SUCCESS));
                    processingResponse.getOutput().add(utils.createAnyContentSimple("keyType", keyType, ValueEmbeddingEnumeration.STRING));
                    processingResponse.getOutput().add(utils.createAnyContentSimple("output", "success", ValueEmbeddingEnumeration.STRING));
                }
            } else {
                LOG.error("ID format did not match.");
                processingResponse.setReport(utils.createReport(TestResultType.FAILURE));
                processingResponse.getOutput().add(utils.createAnyContentSimple("output", "failure", ValueEmbeddingEnumeration.STRING));
                processingResponse.getOutput().add(utils.createAnyContentSimple("errorMessage", "ID format did not match.", ValueEmbeddingEnumeration.STRING));
            }
        }

        LOG.info("Completed operation [{}].", "processDIDJSON");
        return processingResponse;
    }

    private ProcessResponse getHttpResponse(ProcessRequest processRequest) {
        String privateKey = utils.getRequiredString(processRequest.getInput(), "privateKey");
        String privateKeyType = utils.getRequiredString(processRequest.getInput(), "privateKeytype");
        String publicKey = utils.getRequiredString(processRequest.getInput(), "publicKey");
        String serverAddress = utils.getRequiredString(processRequest.getInput(), "serverAddress");

        ProcessResponse processingResponse = new ProcessResponse();
        try {
            var httpResponse = this.makeHandshake(privateKey, publicKey, privateKeyType, serverAddress);
            processingResponse.setReport(utils.createReport(TestResultType.SUCCESS));
            processingResponse.getOutput().add(utils.createAnyContentSimple("output", "success", ValueEmbeddingEnumeration.STRING));
            processingResponse.getOutput().add(utils.createAnyContentSimple("status", String.valueOf(httpResponse.statusCode()), ValueEmbeddingEnumeration.STRING));
            LOG.info("Completed operation [{}].", "getHttpResponse");
            return processingResponse;
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException |
                 UnrecoverableKeyException | InvalidKeySpecException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpResponse<String> makeHandshake(String privateKey, String publicKey,
                                               String privateKeyType, String sutAddress)
            throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException,
            UnrecoverableKeyException, InvalidKeySpecException, KeyManagementException {

        String privateString = new String(privateKey.getBytes(), StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s","");

        byte[] encoded = Base64.getDecoder().decode(privateString.strip());

        final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        final Collection<? extends Certificate> chain = certificateFactory.generateCertificates(
                new ByteArrayInputStream(publicKey.getBytes()));

        Key key = KeyFactory.getInstance(privateKeyType).generatePrivate(new PKCS8EncodedKeySpec(encoded));

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

        HttpClient client = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();

        HttpRequest exactRequest = HttpRequest.newBuilder()
                .uri(URI.create(sutAddress))
                .GET()
                .build();

        var exactResponse = client.sendAsync(exactRequest, HttpResponse.BodyHandlers.ofString())
                .join();
        System.out.println(exactResponse.statusCode());
        return exactResponse;
    }

    /**
     * The purpose of the beginTransaction operation is to begin a unique processing session.
     * <p/>
     * Transactions are used when processing services need to maintain state across several calls. If this is needed
     * then this implementation would generate a session identifier and record the session for subsequent 'process' calls.
     * <p/>
     * In the typical case where no state needs to be maintained, you can provide an empty implementation for this method.
     *
     * @param beginTransactionRequest Optional configuration parameters to consider when starting a processing transaction.
     * @return The response with the generated session ID for the processing transaction.
     */
    @Override
    public BeginTransactionResponse beginTransaction(BeginTransactionRequest beginTransactionRequest) {
        return new BeginTransactionResponse();
    }

    /**
     * The purpose of the endTransaction operation is to complete an ongoing processing session.
     * <p/>
     * The main actions to be taken as part of this operation are to remove the provided session identifier (if this
     * was being recorded to begin with), and to perform any custom cleanup tasks.
     *
     * @param parameters The identifier of the session to terminate.
     * @return A void response.
     */
    @Override
    public Void endTransaction(BasicRequest parameters) {
        return new Void();
    }

}
