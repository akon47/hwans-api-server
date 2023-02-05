package com.hwans.apiserver.support.validator;

import com.hwans.apiserver.support.annotation.BlogId;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * 블로그 Id에 대한 유효성 검사 기능을 제공한다.
 */
public class BlogIdValidator implements ConstraintValidator<BlogId, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(value == null) {
            return false;
        }

        return value.matches("^@[-a-zA-Z\\d_]+$");
    }
}
