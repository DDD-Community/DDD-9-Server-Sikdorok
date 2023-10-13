package com.ddd.chulsi.application.users;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.ddd.chulsi.domainCore.model.appVersion.AppVersionInfo;
import com.ddd.chulsi.domainCore.model.appVersion.AppVersionService;
import com.ddd.chulsi.domainCore.model.feed.FeedCommand;
import com.ddd.chulsi.domainCore.model.oauthToken.OauthToken;
import com.ddd.chulsi.domainCore.model.oauthToken.OauthTokenService;
import com.ddd.chulsi.domainCore.model.shared.DefinedCode;
import com.ddd.chulsi.domainCore.model.users.Users;
import com.ddd.chulsi.domainCore.model.users.UsersCommand;
import com.ddd.chulsi.domainCore.model.users.UsersInfo;
import com.ddd.chulsi.domainCore.model.users.UsersService;
import com.ddd.chulsi.infrastructure.exception.*;
import com.ddd.chulsi.infrastructure.exception.message.ErrorMessage;
import com.ddd.chulsi.infrastructure.jwt.JWTClaim;
import com.ddd.chulsi.infrastructure.jwt.JWTProperties;
import com.ddd.chulsi.infrastructure.jwt.JwtTokenUtil;
import com.ddd.chulsi.infrastructure.mail.MailService;
import com.ddd.chulsi.infrastructure.oauth.OauthInfo;
import com.ddd.chulsi.infrastructure.oauth.OauthKakaoService;
import com.ddd.chulsi.infrastructure.specification.users.UsersSpecification;
import com.ddd.chulsi.infrastructure.util.RedisUtil;
import com.ddd.chulsi.infrastructure.util.StringUtil;
import com.ddd.chulsi.presentation.users.dto.UsersDTO;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsersFacade {

    private final JwtTokenUtil jwtTokenUtil;
    private final JWTProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    private final RedisUtil redisUtil;

    private final UsersService usersService;
    private final OauthTokenService oauthTokenService;
    private final OauthKakaoService oauthKakaoService;
    private final MailService mailService;
    private final UsersSpecification usersSpecification;
    private final AppVersionService appVersionService;

    @Transactional(rollbackFor = Exception.class)
    public UsersDTO.LoginResponse login(UsersCommand.UsersLogin usersLogin, HttpServletResponse response) {
        Users users = usersService.findByEmailAndPassword(usersLogin.email(), usersLogin.password());

        if (users == null) throw new UserNotFoundException();

        return usersLogin(new UsersInfo.UsersInfoLogin(users, null), response);
    }

    @Transactional(rollbackFor = Exception.class)
    public UsersDTO.LoginResponse autoLogin(String token, HttpServletResponse response) {
        JWTClaim jwtClaim = jwtTokenUtil.getClaims(token, properties, true);

        UUID usersId = jwtClaim.getUsersId();
        Users users = usersService.findByUsersId(usersId);
        if (
            users == null ||
            StringUtils.isBlank(users.getRefreshToken()) ||
            !users.getRefreshToken().equals(token)
        )
            throw new UserNotFoundException();

        return usersLogin(new UsersInfo.UsersInfoLogin(users, null), response);
    }

    public UsersDTO.LoginResponse usersLogin(UsersInfo.UsersInfoLogin usersInfoLogin, HttpServletResponse response) {
        JWTClaim jwtClaim = JWTClaim.builder()
            .usersId(usersInfoLogin.getUsersId())
            .auth(usersInfoLogin.getAuth())
            .build();

        // Token 발행
        String accessToken = jwtTokenUtil.createToken(jwtClaim, properties, true);
        String refreshToken = usersInfoLogin.getRefreshToken() == null ? jwtTokenUtil.createToken(jwtClaim, properties, false) : usersInfoLogin.getRefreshToken();

        try {
            jwtTokenUtil.getClaimsForRefreshToken(refreshToken, properties, true);
        } catch (TokenExpiredException e) {
            refreshToken = jwtTokenUtil.createToken(jwtClaim, properties, false);
        }

        // Token 정보 추가
        usersInfoLogin.setAccessToken(accessToken);
        usersInfoLogin.setRefreshToken(refreshToken);

        // Refresh Token 유효시간을 가져온 후 Redis에 Refresh Token을 저장합니다.
        redisUtil.set("REFRESH_TOKEN:" + usersInfoLogin.getUsersId(), refreshToken, properties.getRefreshExpiresTime(), TimeUnit.MILLISECONDS);

        eventPublisher.publishEvent(new UsersCommand.UsersRefreshTokenUpdateEvent(usersInfoLogin.getUsersId(), refreshToken));

        return new UsersDTO.LoginResponse(true, usersInfoLogin);
    }

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void updateRefreshToken(UsersCommand.UsersRefreshTokenUpdateEvent event) {
        Users users = usersService.findByUsersId(event.usersId());
        users.updateRefreshToken(event.refreshToken());
        users.updateLastLoginAt();
    }

    public String kakaoAccessToken(String code) {
        OauthInfo.KakaoInfoResponse kakaoInfoResponse = oauthKakaoService.getAccessToken(code);
        if (kakaoInfoResponse == null) throw new OauthException(700, ErrorMessage.OAUTH_REQUEST_FAILED);
        return kakaoInfoResponse.accessToken();
    }


    @Transactional(rollbackFor = Exception.class)
    public UsersDTO.KakaoLoginResponse kakaoLogin(UsersCommand.LoginCommand loginCommand, HttpServletResponse response) {
        OauthInfo.KakaoUserMe kakaoUserMe = oauthKakaoService.getUserName(loginCommand.accessToken());

        String nickname = kakaoUserMe.kakaoAccount().kakaoProfile().nickname();
        String email = kakaoUserMe.kakaoAccount().email();
        boolean isValidEmail = kakaoUserMe.kakaoAccount().isEmailValid() && kakaoUserMe.kakaoAccount().isEmailVerified();
        OauthInfo.KakaoUserMeDTO kakaoUserMeDTO = new OauthInfo.KakaoUserMeDTO(DefinedCode.C000200001, kakaoUserMe.id(), nickname, email, isValidEmail);

        Users users = usersService.findByOauthTypeAndOauthId(DefinedCode.C000200001, kakaoUserMe.id());
        if (users == null) {
            // 존재하지 않는 회원이면 회원가입 처리를 위해 데이터 Return
            boolean isRegistered = false;

            // 이메일 중복검사
            if (email != null) {
                Users duplicationCheckByEmail = usersService.findByEmail(email);
                if (duplicationCheckByEmail != null) {
                    isValidEmail = false;
                    isRegistered = true;
                }
            }

            return new UsersDTO.KakaoLoginResponse<>(isRegistered, kakaoUserMeDTO);
        }

        OauthToken oauthToken = oauthTokenService.findByOauthTypeAndOauthId(DefinedCode.C000200001, kakaoUserMe.id());
        if (oauthToken == null) {
            oauthTokenService.save(OauthToken.builder().oauthId(kakaoUserMe.id()).oauthType(DefinedCode.C000200001).accessToken(loginCommand.accessToken()).build());
        } else {
            oauthToken.updateAccessToken(loginCommand.accessToken());
        }

        UsersDTO.LoginResponse loginResponse = usersLogin(new UsersInfo.UsersInfoLogin(users, loginCommand.accessToken()), response);

        return new UsersDTO.KakaoLoginResponse<>(
            true,
            loginResponse.usersInfo()
        );
    }

    @Transactional(rollbackFor = Exception.class, noRollbackFor = SlackNotificationHandler.class)
    public void logout(String token) {
        JWTClaim jwtClaim = jwtTokenUtil.checkAuth(token, properties);

        Users users = usersService.findByUsersId(jwtClaim.getUsersId());
        if (users == null) throw new UserNotFoundException();

        if (users.getOauthType() == DefinedCode.C000200001) {
            OauthToken oauthToken = oauthTokenService.findByOauthTypeAndOauthId(users.getOauthType(), users.getOauthId());
            if (oauthToken != null) {
                oauthKakaoService.logout(oauthToken.getAccessToken());
                oauthTokenService.delete(oauthToken);
            }
        }

        users.logout();

        // Redis 에서 해당 User id 로 저장된 Refresh Token 이 있는지 여부를 확인 후 있을 경우 삭제합니다.
        try {
            if (redisUtil.hasKey("REFRESH_TOKEN:" + users.getUsersId()).get()) {
                redisUtil.delete("REFRESH_TOKEN:" + users.getUsersId());

                Long expiration = jwtTokenUtil.getExpiration(token);
                redisUtil.set(token, "logout", expiration, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new SlackNotificationHandler("Redis Server Error");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public UsersDTO.LoginResponse register(UsersCommand.RegisterCommand registerCommand, HttpServletResponse response) {
        // 이메일 중복검사
        if (usersService.duplicationCheckEmail(registerCommand.email()))
            throw new UserExistsException();

        // 회원가입 처리
        Users insertUsers = registerCommand.toEntity();
        Users users = usersService.store(insertUsers);

        // 로그인 처리
        return usersLogin(new UsersInfo.UsersInfoLogin(users, null), response);
    }

    @Transactional(readOnly = true)
    public boolean passwordFind(UsersCommand.PasswordFind passwordFind) {
        Users users = usersService.findByEmail(passwordFind.email());
        if (users == null) throw new NotFoundException();

        String code = StringUtil.getRandomNumberString();

        try {
            if (redisUtil.hasKey("PASSWORD_LINK_ALIVE:" + users.getUsersId()).get()) {
                CompletableFuture<Object> future = redisUtil.get("PASSWORD_LINK_ALIVE:" + users.getUsersId());
                String codeCheck = future.get().toString();

                while (code.equals(codeCheck))
                    code = StringUtil.getRandomNumberString();

                redisUtil.delete("PASSWORD_LINK_ALIVE:" + users.getUsersId());
            }

            redisUtil.set("PASSWORD_LINK_ALIVE:" + users.getUsersId(), code, 3600, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException e) {
            throw new SlackNotificationHandler("Redis Server Error");
        }

        return mailService.sendMail(Collections.singletonList(users), code);
    }

    @Transactional(rollbackFor = Exception.class)
    public void passwordReset(UsersCommand.PasswordReset passwordReset) {
        Users users = usersService.findByUsersId(passwordReset.usersId());
        if (users == null) throw new NotFoundException();

        users.passwordReset(passwordReset.password());

        try {
            if (redisUtil.hasKey("PASSWORD_LINK_ALIVE:" + users.getUsersId()).get())
                redisUtil.delete("PASSWORD_LINK_ALIVE:" + users.getUsersId());
        } catch (InterruptedException | ExecutionException e) {
            throw new SlackNotificationHandler("Redis Server Error");
        }
    }

    @Transactional(readOnly = true)
    public boolean passwordLinkAlive(UsersCommand.PasswordLinkAlive passwordLinkAlive) {
        Users users = usersService.findByUsersId(passwordLinkAlive.usersId());
        if (users == null) return false;

        try {
            if (redisUtil.hasKey("PASSWORD_LINK_ALIVE:" + users.getUsersId()).get()) {
                CompletableFuture<Object> future = redisUtil.get("PASSWORD_LINK_ALIVE:" + users.getUsersId());
                String code = future.get().toString();
                return code.equals(passwordLinkAlive.code());
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new SlackNotificationHandler("Redis Server Error");
        }

        return false;
    }

    @Transactional(readOnly = true)
    public Boolean emailCheck(String email) {
        return usersService.duplicationCheckEmail(email);
    }

    @Transactional(readOnly = true)
    public String accessToken(String refreshToken) {
        JWTClaim jwtClaim = jwtTokenUtil.getClaimsForRefreshToken(refreshToken, properties, true);

        UUID userId = jwtClaim.getUsersId();

        // 존재하는 유저인지 확인
        Users users = usersService.findByUsersId(userId);
        if (users == null) return null;

        return jwtTokenUtil.createToken(jwtClaim, properties, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public void revoke(String token) {
        JWTClaim jwtClaim = jwtTokenUtil.checkAuth(token, properties);

        UUID usersId = jwtClaim.getUsersId();
        Users users = usersSpecification.findByUsersId(usersId);

        // 피드 전체 삭제
        eventPublisher.publishEvent(new FeedCommand.RevokeUsers(usersId));

        usersService.revoke(users);
    }

    @Transactional(readOnly = true)
    public UsersDTO.SettingsResponse settings(String token, String version, DefinedCode deviceType) {
        JWTClaim jwtClaim = jwtTokenUtil.checkAuth(token, properties);

        UUID usersId = jwtClaim.getUsersId();
        Users users = usersSpecification.findByUsersId(usersId);

        AppVersionInfo.AppVersionDTO latest = appVersionService.latest(deviceType);

        return new UsersDTO.SettingsResponse(
            users.getOauthType(),
            users.getName(),
            users.getEmail(),
            latest != null && StringUtils.isNotBlank(latest.appInfoAppVersion()) && latest.appInfoAppVersion().equals(version)
        );
    }

}
