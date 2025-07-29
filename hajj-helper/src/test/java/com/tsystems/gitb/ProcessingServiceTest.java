package com.tsystems.gitb;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProcessingServiceTest {
    String [] exampleIDs = {
            "did:web:tng-cdn-dev.who.int:v2:trustlist:PH4H:XXA:SCA#/Eg0veXxFzY=",
            "did:web:tng-cdn-dev.who.int:v2:trustlist:PH4H:BHS:SCA#/qO4e0TVFjY=",
            "did:web:tng-cdn-dev.who.int:v2:trustlist:DCC:SVN:SCA#/rcyDdJNU/A=",
            "did:web:tng-cdn-dev.who.int:v2:trustlist:PH4H:SUR:SCA#1Uv37VPSMhs="};

    @Test
    public void validateIDs() {
        assertEquals(4, this.exampleIDs.length);
        for (String id : this.exampleIDs) {
            Matcher matcher = ProcessingServiceImpl.VALIDATION_METHOD_ID_PATTERN.matcher(id);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("ID does not match." + id);
            }
        }

    }
}
