package com.hwans.apiserver.support.validator;

import com.hwans.apiserver.support.annotation.BlogId;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class BlogIdValidator implements ConstraintValidator<BlogId, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(value == null) {
            return false;
        }

        return value.matches("^@?[-a-zA-Z\\d_]+$");
    }
}
