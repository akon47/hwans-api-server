package com.hwans.apiserver.dto.account;

import com.hwans.apiserver.support.annotation.BlogId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import java.io.Serializable;

/**
 * 사용자 생성 Dto
 */
@Getter
@Builder
@Schema(description = "사용자 생성 Dto")
public class CreateAccountDto implements Serializable {
    /**
     * 이메일 인증 코드를 사용하여 계정 생성 시 사용하는 Validation 그룹
     */
    public interface ByEmailVerifyCode {
    }

    /**
     * 계정 생성 토큰을 사용하여 계정 생성 시 사용하는 Validation 그룹
     */
    public interface ByRegisterToken {
    }

    @Schema(description = "사용자 이메일", requiredMode = Schema.RequiredMode.REQUIRED, example = "akon47@naver.com")
    @NotBlank
    @Length(max = 320)
    @Email
    String email;
    @Schema(description = "사용자 로그인 비밀번호", example = "12345")
    @NotBlank(groups = ByEmailVerifyCode.class)
    @Null(groups = ByRegisterToken.class)
    String password;
    @Schema(description = "사용자 이름", requiredMode = Schema.RequiredMode.REQUIRED, example = "김환")
    @NotBlank
    @Length(max = 32)
    String name;
    @Schema(description = "블로그 Id", requiredMode = Schema.RequiredMode.REQUIRED, example = "@kim-hwan")
    @NotBlank
    @Length(min = 2, max = 64)
    @BlogId
    String blogId;
    @Schema(description = "사용자 이메일 인증 코드", example = "123456")
    @NotBlank(groups = ByEmailVerifyCode.class)
    @Null(groups = ByRegisterToken.class)
    String emailVerifyCode;
}
