package com.hwans.apiserver.support.validator;

import com.hwans.apiserver.support.annotation.PostUrl;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class PostUrlValidator implements ConstraintValidator<PostUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(value == null) {
            return false;
        }

        return value.matches("^[-a-zA-Z\\d_]+$");
    }
}
