package com.hwans.apiserver.support.validator;

import com.hwans.apiserver.support.annotation.SeriesUrl;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * SeriesUrl에 대한 유효성 검사 기능을 제공한다.
 */
public class SeriesUrlValidator implements ConstraintValidator<SeriesUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(value == null) {
            return true;
        }

        return value.matches("^[-a-zA-Z\\d_]+$");
    }
}
