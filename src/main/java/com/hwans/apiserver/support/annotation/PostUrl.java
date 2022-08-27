package com.hwans.apiserver.support.annotation;

import com.hwans.apiserver.support.validator.PostUrlValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@Constraint(validatedBy = PostUrlValidator.class)
@Target({TYPE, FIELD, PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PostUrl {
    String message() default "영문과 숫자 그리고 하이픈과 언더바만 사용 가능합니다.";
    Class<?>[] groups() default { };
    Class<? extends Payload>[] payload() default { };
}
