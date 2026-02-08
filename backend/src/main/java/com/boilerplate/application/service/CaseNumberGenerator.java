package com.boilerplate.application.service;

import com.boilerplate.presentation.exception.InvalidCaseNumberFormatException;
import org.springframework.stereotype.Service;

@Service
public class CaseNumberGenerator {

    public String generate(
        String template,
        int year,
        String tribunalCode,
        String caseTypeCode,
        int sequence
    ) {
        String result = template;

        result = result.replace("{YEAR}", String.valueOf(year));
        result = result.replace("{TRIBUNAL_CODE}", tribunalCode);
        result = result.replace("{CASETYPE}", caseTypeCode);
        result = result.replace("{SEQ5}", String.format("%05d", sequence));

        // Validate no unreplaced placeholders
        if (result.contains("{") || result.contains("}")) {
            throw new InvalidCaseNumberFormatException("Invalid template: " + template);
        }

        return result;
    }

    public String preview(String template, String tribunalCode, String caseTypeCode) {
        return generate(template, java.time.Year.now().getValue(), tribunalCode, caseTypeCode, 1);
    }
}
