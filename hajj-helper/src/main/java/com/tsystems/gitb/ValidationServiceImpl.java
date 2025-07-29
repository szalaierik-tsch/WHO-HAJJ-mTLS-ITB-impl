package com.tsystems.gitb;

import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.TAR;
import com.gitb.tr.TestAssertionGroupReportsType;
import com.gitb.tr.TestResultType;
import com.gitb.tr.ValidationCounters;
import com.gitb.vs.*;
import com.gitb.vs.Void;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

/**
 * Spring component that realises the validation service.
 */
@Component
public class ValidationServiceImpl implements ValidationService {

    /** Logger. **/
    private static final Logger LOG = LoggerFactory.getLogger(ValidationServiceImpl.class);

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
     * The validate operation is called to validate the input and produce a validation report.
     *
     * The expected input is described for the service's client through the getModuleDefinition call.
     *
     * @param parameters The input parameters and configuration for the validation.
     * @return The response containing the validation report.
     */
    @Override
    public ValidationResponse validate(ValidateRequest parameters) {
        LOG.info("Received 'validate' command from test bed for session [{}]", parameters.getSessionId());
        ValidationResponse result = new ValidationResponse();
        TAR report = utils.createReport(TestResultType.SUCCESS);
        // First extract the parameters and check to see if they are as expected.
        String providedText = utils.getRequiredString(parameters.getInput(), "text");
        String expectedText = utils.getRequiredString(parameters.getInput(), "expected");
        boolean mismatchIsError = Boolean.parseBoolean(utils.getOptionalString(parameters.getInput(), "mismatchIsError").orElse("true"));
        // Now do the validation.
        report.getContext().getItem().add(utils.createAnyContentSimple("text", providedText, ValueEmbeddingEnumeration.STRING));
        report.getContext().getItem().add(utils.createAnyContentSimple("expected", expectedText, ValueEmbeddingEnumeration.STRING));
        report.setReports(new TestAssertionGroupReportsType());
        int infos = 0;
        int warnings = 0;
        int errors = 0;
        if (!providedText.equals(expectedText)) {
            if (mismatchIsError) {
                errors += 1;
                utils.addReportItemError("The texts do not match.", report.getReports().getInfoOrWarningOrError());
            } else {
                warnings += 1;
                utils.addReportItemWarning("The texts do not match.", report.getReports().getInfoOrWarningOrError());
            }
            if (providedText.equalsIgnoreCase(expectedText)) {
                infos += 1;
                utils.addReportItemInfo("The texts match but only when ignoring case.", report.getReports().getInfoOrWarningOrError());
            }
        }
        report.setCounters(new ValidationCounters());
        report.getCounters().setNrOfAssertions(BigInteger.valueOf(infos));
        report.getCounters().setNrOfWarnings(BigInteger.valueOf(warnings));
        report.getCounters().setNrOfErrors(BigInteger.valueOf(errors));
        if (errors > 0) {
            report.setResult(TestResultType.FAILURE);
        } else if (warnings > 0) {
            report.setResult(TestResultType.WARNING);
        }
        // Return the report.
        result.setReport(report);
        return result;
    }

}
