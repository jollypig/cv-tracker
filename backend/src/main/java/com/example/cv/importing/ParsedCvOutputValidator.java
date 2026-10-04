package com.example.cv.importing;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ParsedCvOutputValidator {

    private final Validator validator;

    public ParsedCvOutputValidator(Validator validator) {
        this.validator = validator;
    }

    public ParsedCv validate(ParsedCv parsedCv) {
        List<String> warnings = new ArrayList<>(parsedCv.warnings());
        validator.validate(parsedCv).stream()
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .map(this::warning)
                .forEach(warnings::add);
        return new ParsedCv(parsedCv.personalData(), parsedCv.professionalSummary(), parsedCv.employment(),
                parsedCv.projects(), parsedCv.education(), parsedCv.languages(), parsedCv.skills(),
                warnings.stream().distinct().toList());
    }

    private String warning(ConstraintViolation<ParsedCv> violation) {
        return "AI output requires review at '" + violation.getPropertyPath() + "': " + violation.getMessage();
    }
}