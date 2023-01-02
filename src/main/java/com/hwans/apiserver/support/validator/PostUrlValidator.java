package com.hwans.apiserver.support.validator;

import com.hwans.apiserver.support.annotation.PostUrl;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * PostUrl에 대한 유효성 검사 기능을 제공한다.
 */
public class PostUrlValidator implements ConstraintValidator<PostUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(value == null) {
            return true;
        }

        return value.matches("^[-a-zA-Z\\d_]+$");
    }
}
