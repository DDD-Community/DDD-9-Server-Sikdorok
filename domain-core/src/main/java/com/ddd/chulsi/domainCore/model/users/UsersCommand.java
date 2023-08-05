package com.ddd.chulsi.domainCore.model.users;

import java.util.UUID;

public class UsersCommand {

    public record LoginCommand (
        String accessToken
    ) {
        public static LoginCommand toCommand(String accessToken) {
            return new LoginCommand(accessToken);
        }
    }

    public record UsersLogin (
        String email,
        String password
    ) {
        public static UsersLogin toCommand(String email, String password) {
            return new UsersLogin(email, password);
        }
    }

    public record UsersRefreshTokenUpdateEvent (
        UUID usersId,
        String refreshToken
    ) { }

    public record RegisterCommand (
        String nickname,
        String email,
        String password
    ) {
        public static RegisterCommand toCommand(String nickname, String email, String password) {
            return new RegisterCommand(nickname, email, password);
        }
        public Users toEntity() {
            return Users.builder()
                .name(nickname)
                .email(email)
                .password(password)
                .build();
        }
    }

    public record PasswordFind (
        String email
    ) {
        public static PasswordFind toCommand(String email) {
            return new PasswordFind(email);
        }
    }

    public record PasswordReset (
        UUID usersId,
        String password
    ) {
        public static PasswordReset toCommand(UUID usersId, String password) {
            return new PasswordReset(usersId, password);
        }
    }

    public record PasswordLinkAlive (
        UUID usersId,
        String code
    ) {
        public static PasswordLinkAlive toCommand(UUID usersId, String code) {
            return new PasswordLinkAlive(usersId, code);
        }
    }
}
