package com.hwans.apiserver.dto.account;

import com.hwans.apiserver.support.annotation.BlogId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Null;
import java.io.Serializable;

@Getter
@Builder
@ApiModel(description = "사용자 생성 Dto")
public class CreateAccountDto implements Serializable {
    public interface ByEmailVerifyCode {
    }

    public interface ByRegisterToken {
    }

    @ApiModelProperty(value = "사용자 이메일", required = true, example = "akon47@naver.com")
    @NotBlank
    @Length(max = 320)
    @Email
    String email;
    @ApiModelProperty(value = "사용자 로그인 비밀번호", required = false, example = "12345")
    @NotBlank(groups = ByEmailVerifyCode.class)
    @Null(groups = ByRegisterToken.class)
    String password;
    @ApiModelProperty(value = "사용자 이름", required = true, example = "김환")
    @NotBlank
    @Length(max = 32)
    String name;
    @ApiModelProperty(value = "블로그 Id", required = true, example = "kim-hwan")
    @NotBlank
    @Length(min = 2, max = 64)
    @BlogId
    String blogId;
    @ApiModelProperty(value = "사용자 이메일 인증 코드", required = false, example = "123456")
    @NotBlank(groups = ByEmailVerifyCode.class)
    @Null(groups = ByRegisterToken.class)
    String emailVerifyCode;
}