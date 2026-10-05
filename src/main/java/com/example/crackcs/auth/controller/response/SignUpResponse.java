package com.example.crackcs.auth.controller.response;

public record SignUpResponse(String message) {
    public static SignUpResponse accepted() {
        return new SignUpResponse("회원가입 요청을 처리했습니다. 가입한 이메일과 비밀번호로 로그인해 주세요.");
    }
}
