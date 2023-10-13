package com.ddd.chulsi.presentation.users.dto;

import com.ddd.chulsi.domainCore.model.shared.DefinedCode;
import com.ddd.chulsi.domainCore.model.users.UsersCommand;
import com.ddd.chulsi.domainCore.model.users.UsersInfo;
import com.ddd.chulsi.infrastructure.exception.BadRequestException;
import com.ddd.chulsi.infrastructure.exception.message.ErrorMessage;
import com.ddd.chulsi.infrastructure.util.BCryptUtils;
import com.ddd.chulsi.infrastructure.util.StringUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;
import java.util.UUID;

public class UsersDTO {

    public record OauthLoginRequest (
        @NotBlank
        String accessToken
    ) {
        public UsersCommand.LoginCommand toCommand() {
            return UsersCommand.LoginCommand.toCommand(accessToken);
        }
    }

    public record LoginResponse (
        boolean isRegistered,
        UsersInfo.UsersInfoLogin usersInfo
    ) {

    }

    public record KakaoLoginResponse<T> (
        boolean isRegistered,
        T usersInfo
    ) {

    }

    public record Register (
        DefinedCode oauthType,

        Long oauthId,

        @NotBlank
        String nickname,

        @Email(message = ErrorMessage.EMAIL_VALIDATION_FAILED)
        @NotBlank
        String email,

        @NotBlank
        String password,

        @NotBlank
        String passwordCheck
    ) {
        public Register {
            if (oauthType != null && oauthId == null || oauthId != null && oauthType == null)
                throw new BadRequestException("oauth 정보가 올바르지 않습니다", "oauth");
            if (nickname.length() < 2 || nickname.length() > 10)
                throw new BadRequestException("이름은 2자 이상, 10자 이하로 입력해주세요", "nickname");
            if (!StringUtil.isEnglishOrNumberOrSpecial(password, 8, 20))
                throw new BadRequestException(ErrorMessage.PASSWORD_VALIDATION_FAILED, "password");
            if (!StringUtil.isEnglishOrNumberOrSpecial(passwordCheck, 8, 20))
                throw new BadRequestException(ErrorMessage.PASSWORD_VALIDATION_FAILED, "passwordCheck");
            if (!Objects.deepEquals(password, passwordCheck))
                throw new BadRequestException("입력하신 비밀번호가 일치하지 않습니다", "password, passwordCheck");
        }
        public UsersCommand.RegisterCommand toCommand() {
            return UsersCommand.RegisterCommand.toCommand(oauthType, oauthId, nickname, email, BCryptUtils.hash(password));
        }
    }

    public record LoginRequest (
        @Email(message = ErrorMessage.EMAIL_VALIDATION_FAILED)
        @NotBlank
        String email,

        @NotBlank
        String password
    ) {
        public LoginRequest {
            if (!StringUtil.isEnglishOrNumberOrSpecial(password, 8, 20))
                throw new BadRequestException(ErrorMessage.PASSWORD_VALIDATION_FAILED, "password");
        }
        public UsersCommand.UsersLogin toCommand() {
            return UsersCommand.UsersLogin.toCommand(email, BCryptUtils.hash(password));
        }
    }

    public record PasswordFindRequest (
        @Email(message = ErrorMessage.EMAIL_VALIDATION_FAILED)
        @NotBlank
        String email
    ) {
        public UsersCommand.PasswordFind toCommand() {
            return UsersCommand.PasswordFind.toCommand(email);
        }
    }

    public record PasswordResetRequest (
        @NotNull
        UUID usersId,

        @NotBlank
        String password,

        @NotBlank
        String passwordCheck
    ) {
        public PasswordResetRequest {
            if (!StringUtil.isEnglishOrNumberOrSpecial(password, 8, 20))
                throw new BadRequestException(ErrorMessage.PASSWORD_VALIDATION_FAILED, "password");
            if (!StringUtil.isEnglishOrNumberOrSpecial(passwordCheck, 8, 20))
                throw new BadRequestException(ErrorMessage.PASSWORD_VALIDATION_FAILED, "passwordCheck");
            if (!Objects.deepEquals(password, passwordCheck))
                throw new BadRequestException("입력하신 비밀번호가 일치하지 않습니다", "password, passwordCheck");
        }

        public UsersCommand.PasswordReset toCommand() {
            return UsersCommand.PasswordReset.toCommand(usersId, BCryptUtils.hash(password));
        }
    }

    public record PasswordLinkAliveRequest (
        @NotNull
        UUID usersId,

        @NotBlank
        String code
    ) {

        public UsersCommand.PasswordLinkAlive toCommand() {
            return UsersCommand.PasswordLinkAlive.toCommand(usersId, code);
        }
    }

    public record AccessTokenRequest (
        @NotBlank
        String refreshToken
    ) {}

    public record SettingsResponse(
        DefinedCode oauthType,
        String nickname,
        String email,
        boolean isLatest
    ) {
    }
}
