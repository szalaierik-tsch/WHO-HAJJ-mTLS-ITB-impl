package com.tsystems.gitb;

import com.gitb.core.*;
import com.gitb.tr.*;
import com.gitb.tr.ObjectFactory;
import jakarta.xml.ws.WebServiceContext;
import org.w3c.dom.Element;
import org.apache.cxf.headers.Header;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.*;
import java.util.*;
import java.util.function.Function;

/**
 * Class containing utility methods.
 */
@Component
public class Utils {

    /** SOAP header name for the ReplyTo address. */
    public static final QName REPLY_TO_QNAME = new QName("http://www.w3.org/2005/08/addressing", "ReplyTo");
    /** SOAP header name for the test session ID. */
    public static final QName TEST_SESSION_ID_QNAME = new QName("http://www.gitb.com", "TestSessionIdentifier", "gitb");

    @Autowired
    private ObjectFactory objectFactory;

    /**
     * Create a report for the given result.
     *
     * This method creates the report, sets its time and constructs an empty context map to return values with.
     *
     * @param result The overall result of the report.
     * @return The report.
     */
    public TAR createReport(TestResultType result) {
        TAR report = new TAR();
        report.setContext(new AnyContent());
        report.getContext().setType("map");
        report.setResult(result);
        try {
            report.setDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException(e);
        }
        return report;
    }

    /**
     * Create a parameter definition.
     *
     * @param name The name of the parameter.
     * @param type The type of the parameter. This needs to match one of the GITB types.
     * @param use The use (required or optional).
     * @param kind The kind og parameter it is (whether it should be provided as the specific value, as BASE64 content or as a URL that needs to be looked up to obtain the value).
     * @param description The description of the parameter.
     * @return The created parameter.
     */
    public TypedParameter createParameter(String name, String type, UsageEnumeration use, ConfigurationType kind, String description) {
        TypedParameter parameter =  new TypedParameter();
        parameter.setName(name);
        parameter.setType(type);
        parameter.setUse(use);
        parameter.setKind(kind);
        parameter.setDesc(description);
        return parameter;
    }

    /**
     * Collect the inputs that match the provided name.
     *
     * @param parameterItems The items to look through.
     * @param inputName The name of the input to look for.
     * @return The collected inputs (not null).
     */
    public List<AnyContent> getInputsForName(List<AnyContent> parameterItems, String inputName) {
        List<AnyContent> inputs = new ArrayList<>();
        if (parameterItems != null) {
            for (AnyContent anInput: parameterItems) {
                if (inputName.equals(anInput.getName())) {
                    inputs.add(anInput);
                }
            }
        }
        return inputs;
    }

    /**
     * Get a single required input for the provided name.
     *
     * @param parameterItems The items to look through.
     * @param inputName The name of the input to look for.
     * @return The input.
     */
    public AnyContent getSingleRequiredInputForName(List<AnyContent> parameterItems, String inputName) {
        var inputs = getInputsForName(parameterItems, inputName);
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException(String.format("No input named [%s] was found.", inputName));
        } else if (inputs.size() > 1) {
            throw new IllegalArgumentException(String.format("Multiple inputs named [%s] were found when only one was expected.", inputName));
        }
        return inputs.get(0);
    }

    /**
     * Get a single optional input for the provided name.
     *
     * @param parameterItems The items to look through.
     * @param inputName The name of the input to look for.
     * @return The input.
     */
    public Optional<AnyContent> getSingleOptionalInputForName(List<AnyContent> parameterItems, String inputName) {
        var inputs = getInputsForName(parameterItems, inputName);
        if (inputs.isEmpty()) {
            return Optional.empty();
        } else if (inputs.size() > 1) {
            throw new IllegalArgumentException(String.format("Multiple inputs named [%s] were found when at most one was expected.", inputName));
        } else {
            return Optional.of(inputs.get(0));
        }
    }

    /**
     * Convert the provided content to a string value.
     *
     * @param content The content to convert.
     * @return The string value.
     */
    public String asString(AnyContent content) {
        if (content == null || content.getValue() == null) {
            return null;
        } else if (content.getEmbeddingMethod() == ValueEmbeddingEnumeration.BASE_64) {
            // Value provided as BASE64 string.
            return new String(Base64.getDecoder().decode(content.getValue()));
        } else if (content.getEmbeddingMethod() == ValueEmbeddingEnumeration.URI) {
            // Value provided as URI to look up.
            try {
                var request = HttpRequest.newBuilder()
                        .uri(new URI(content.getValue()))
                        .GET()
                        .build();
                return HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofString())
                        .body();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(String.format("The provided value [%s] was not a valid URI.", content.getValue()), e);
            } catch (IOException | InterruptedException e) {
                throw new IllegalArgumentException(String.format("Error while calling URI [%s]", content.getValue()), e);
            }
        } else {
            // Value provided as String.
            return content.getValue();
        }
    }

    /**
     * Get a single required input for the provided name as a string value.
     *
     * @param parameterItems The items to look through.
     * @param inputName The name of the input to look for.
     * @return The input's string value.
     */
    public String getRequiredString(List<AnyContent> parameterItems, String inputName) {
        return asString(getSingleRequiredInputForName(parameterItems, inputName));
    }

    /**
     * Get a single required input for the provided name as a binary value.
     *
     * @param parameterItems The items to look through.
     * @param inputName The name of the input to look for.
     * @return The input's byte[] value.
     */
    public byte[] getRequiredBinary(List<AnyContent> parameterItems, String inputName) {
        var input = getSingleRequiredInputForName(parameterItems, inputName);
        if (input.getEmbeddingMethod() == null || input.getEmbeddingMethod() == ValueEmbeddingEnumeration.BASE_64) {
            // Base64 encoded string.
            return Base64.getDecoder().decode(input.getValue());
        } else if (input.getEmbeddingMethod() == ValueEmbeddingEnumeration.URI) {
            // Remote URI to read from.
            try {
                var request = HttpRequest.newBuilder()
                        .uri(new URI(input.getValue()))
                        .GET()
                        .build();
                return HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofByteArray())
                        .body();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(String.format("The provided value [%s] was not a valid URI.", input.getValue()), e);
            } catch (IOException | InterruptedException e) {
                throw new IllegalArgumentException(String.format("Error while calling URI [%s]", input.getValue()), e);
            }
        } else {
            throw new IllegalArgumentException(String.format("Input [%s] was expected to be provided as a BASE64 string or a URI.", inputName));
        }
    }

    /**
     * Get a single optional input for the provided name as a string value.
     *
     * @param parameterItems The items to look through.
     * @param inputName The name of the input to look for.
     * @return The input's string value.
     */
    public Optional<String> getOptionalString(List<AnyContent> parameterItems, String inputName) {
        var input = getSingleOptionalInputForName(parameterItems, inputName);
        return input.map(this::asString);
    }

    /**
     * Create a AnyContent object value based on the provided parameters.
     *
     * @param name The name of the value.
     * @param value The value itself.
     * @param embeddingMethod The way in which this value is to be considered.
     * @return The value.
     */
    public AnyContent createAnyContentSimple(String name, String value, ValueEmbeddingEnumeration embeddingMethod) {
        AnyContent input = new AnyContent();
        input.setName(name);
        input.setValue(value);
        input.setType("string");
        input.setEmbeddingMethod(embeddingMethod);
        return input;
    }

    /**
     * Parse the received SOAP headers to retrieve the "reply-to" address.
     *
     * @param context The call's context.
     * @return The header's value.
     */
    public Optional<String> getReplyToAddressFromHeaders(WebServiceContext context) {
        return getHeaderAsString(context, REPLY_TO_QNAME).map(h -> {
            if (h.endsWith("?wsdl")) {
                return h;
            } else {
                return h + "?wsdl";
            }
        });
    }

    /**
     * Parse the received SOAP headers to retrieve the test session identifier.
     *
     * @param context The call's context.
     * @return The header's value.
     */
    public Optional<String> getTestSessionIdFromHeaders(WebServiceContext context) {
        return getHeaderAsString(context, TEST_SESSION_ID_QNAME);
    }

    /**
     * Extract a value from the SOAP headers.
     *
     * @param name The name of the header to locate.
     * @param valueExtractor The function used to extract the data.
     * @return The extracted data.
     * @param <T> The type of data extracted.
     */
    public <T> T getHeaderValue(WebServiceContext context, QName name, Function<Header, T> valueExtractor) {
        return ((List<Header>) context.getMessageContext().get(Header.HEADER_LIST))
                .stream()
                .filter(header -> name.equals(header.getName())).findFirst()
                .map(valueExtractor).orElse(null);
    }

    /**
     * Get the specified header element as a string.
     *
     * @param name The name of the header element to lookup.
     * @return The text value of the element.
     */
    public Optional<String> getHeaderAsString(WebServiceContext context, QName name) {
        return Optional.ofNullable(getHeaderValue(context, name, (header) -> ((Element) header.getObject()).getTextContent().trim()));
    }

    /**
     * Add an information message to the report.
     *
     * @param message The message.
     * @param reportItems The report's items.
     */
    public void addReportItemInfo(String message, List<JAXBElement<TestAssertionReportType>> reportItems) {
        reportItems.add(objectFactory.createTestAssertionGroupReportsTypeInfo(createReportItemContent(message)));
    }

    /**
     * Add a warning message to the report.
     *
     * @param message The message.
     * @param reportItems The report's items.
     */
    public void addReportItemWarning(String message, List<JAXBElement<TestAssertionReportType>> reportItems) {
        reportItems.add(objectFactory.createTestAssertionGroupReportsTypeWarning(createReportItemContent(message)));
    }

    /**
     * Add an error message to the report.
     *
     * @param message The message.
     * @param reportItems The report's items.
     */
    public void addReportItemError(String message, List<JAXBElement<TestAssertionReportType>> reportItems) {
        reportItems.add(objectFactory.createTestAssertionGroupReportsTypeError(createReportItemContent(message)));
    }

    /**
     * Create the internal content of a report's item.
     *
     * @param message The message.
     * @return The content to wrap.
     */
    private BAR createReportItemContent(String message) {
        BAR itemContent = new BAR();
        itemContent.setDescription(message);
        return itemContent;
    }

}
