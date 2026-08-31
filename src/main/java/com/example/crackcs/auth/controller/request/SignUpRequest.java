package com.example.crackcs.auth.controller.request;

import com.example.crackcs.auth.controller.request.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        String email,

        @ValidPassword
        String password,

        @NotBlank(message = "닉네임을 입력해 주세요.")
        @Size(max = 100, message = "닉네임은 100자 이하여야 합니다.")
        String nickname
) {
}
