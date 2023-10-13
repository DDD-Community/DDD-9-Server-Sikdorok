package com.ddd.chulsi.presentation.users;

import com.ddd.chulsi.application.users.UsersFacade;
import com.ddd.chulsi.domainCore.model.shared.DefinedCode;
import com.ddd.chulsi.domainCore.model.users.UsersCommand;
import com.ddd.chulsi.infrastructure.jwt.JwtTokenUtil;
import com.ddd.chulsi.presentation.shared.ControllerTest;
import com.ddd.chulsi.presentation.users.dto.UsersDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.ddd.chulsi.infrastructure.format.DocumentOptionalGenerator.*;
import static com.ddd.chulsi.infrastructure.inMemory.users.UsersFactory.*;
import static com.ddd.chulsi.infrastructure.util.ApiDocumentUtils.getDocumentRequest;
import static com.ddd.chulsi.infrastructure.util.ApiDocumentUtils.getDocumentResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsersController.class)
class UsersControllerTest extends ControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UsersFacade usersFacade;

    @Test
    void 카카오_로그인() throws Exception {

        given(usersFacade.kakaoLogin(any(UsersCommand.LoginCommand.class), any(HttpServletResponse.class))).willReturn(givenKakaoLoginResponse());

        UsersDTO.OauthLoginRequest request = new UsersDTO.OauthLoginRequest("accessToken");

        mockMvc.perform(
                post("/users/kakao/login")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("code").value(200))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andDo(document("users/kakao/login",
                getDocumentRequest(),
                getDocumentResponse(),
                requestFields(
                    fieldWithPath("accessToken").type(JsonFieldType.STRING).description("Kakao accessToken")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.NUMBER).description("결과 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메세지"),
                    fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 데이터"),
                    fieldWithPath("data.isRegistered").type(JsonFieldType.BOOLEAN).description("회원가입 유무"),
                    fieldWithPath("data.usersInfo").type(JsonFieldType.OBJECT).description("유저 정보"),
                    fieldWithPath("data.usersInfo.usersId").type(JsonFieldType.STRING).description("유저 고유번호"),
                    fieldWithPath("data.usersInfo.accessToken").type(JsonFieldType.STRING).description("access token"),
                    fieldWithPath("data.usersInfo.refreshToken").type(JsonFieldType.STRING).description("JWT Refresh Token"),
                    fieldWithPath("data.usersInfo.lastLoginAt").type(JsonFieldType.STRING).description("마지막 로그인 일시")
                )
            ));

    }

    @Test
    void 카카오_로그인_회원가입_필요() throws Exception {

        given(usersFacade.kakaoLogin(any(UsersCommand.LoginCommand.class), any(HttpServletResponse.class))).willReturn(givenKakaoLoginNeedSignUpResponse());

        UsersDTO.OauthLoginRequest request = new UsersDTO.OauthLoginRequest("accessToken");

        mockMvc.perform(
                post("/users/kakao/login")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("code").value(200))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andDo(document("users/kakao/login/need-sign-up",
                getDocumentRequest(),
                getDocumentResponse(),
                requestFields(
                    fieldWithPath("accessToken").type(JsonFieldType.STRING).description("Kakao accessToken")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.NUMBER).description("결과 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메세지"),
                    fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 데이터"),
                    fieldWithPath("data.isRegistered").type(JsonFieldType.BOOLEAN).description("회원가입 유무"),
                    fieldWithPath("data.usersInfo").type(JsonFieldType.OBJECT).description("유저 정보"),
                    fieldWithPath("data.usersInfo.oauthType").type(JsonFieldType.STRING).attributes(oauthTypeFormat()).description("oauth 유형"),
                    fieldWithPath("data.usersInfo.oauthId").type(JsonFieldType.NUMBER).description("oauth 고유번호"),
                    fieldWithPath("data.usersInfo.nickname").type(JsonFieldType.STRING).description("닉네임"),
                    fieldWithPath("data.usersInfo.email").type(JsonFieldType.STRING).description("이메일"),
                    fieldWithPath("data.usersInfo.isValidEmail").type(JsonFieldType.BOOLEAN).description("이메일 유효성")
                )
            ));

    }

    @Test
    void 로그아웃() throws Exception {

        doNothing().when(usersFacade).logout(anyString());

        mockMvc.perform(
                post("/users/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, JwtTokenUtil.PREFIX + "AccessToken")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("code").value(200))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andDo(document("users/logout",
                getDocumentRequest(),
                getDocumentResponse(),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.NUMBER).description("결과 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메세지"),
                    fieldWithPath("data").type(JsonFieldType.NULL).description("결과 데이터")
                )
            ));

    }

    @Test
    void 회원가입() throws Exception {

        given(usersFacade.register(any(UsersCommand.RegisterCommand.class), any(HttpServletResponse.class))).willReturn(givenLoginResponse());

        UsersDTO.Register request = givenRegisterRequest();

        mockMvc.perform(
                post("/users/register")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("code").value(200))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andDo(document("users/register",
                getDocumentRequest(),
                getDocumentResponse(),
                requestFields(
                    fieldWithPath("oauthType").type(JsonFieldType.STRING).description("닉네임").optional(),
                    fieldWithPath("oauthId").type(JsonFieldType.NUMBER).description("닉네임").optional(),
                    fieldWithPath("nickname").type(JsonFieldType.STRING).description("닉네임"),
                    fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                    fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호"),
                    fieldWithPath("passwordCheck").type(JsonFieldType.STRING).description("비밀번호 확인")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.NUMBER).description("결과 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메세지"),
                    fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 데이터"),
                    fieldWithPath("data.isRegistered").type(JsonFieldType.BOOLEAN).description("회원가입 유무"),
                    fieldWithPath("data.usersInfo").type(JsonFieldType.OBJECT).description("유저 로그인 정보"),
                    fieldWithPath("data.usersInfo.usersId").type(JsonFieldType.STRING).description("유저 고유번호"),
                    fieldWithPath("data.usersInfo.accessToken").type(JsonFieldType.STRING).description("JWT Access Token"),
                    fieldWithPath("data.usersInfo.refreshToken").type(JsonFieldType.STRING).description("JWT Refresh Token"),
                    fieldWithPath("data.usersInfo.lastLoginAt").type(JsonFieldType.STRING).attributes(dateFormatFull()).description("마지막 로그인 일시").optional()
                )
            ));

    }

    @Test
    void 로그인() throws Exception {

        given(usersFacade.login(any(UsersCommand.UsersLogin.class), any(HttpServletResponse.class))).willReturn(givenLoginResponse());

        UsersDTO.LoginRequest request = givenLoginRequest();

        mockMvc.perform(
                post("/users/login")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("code").value(200))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andDo(document("users/login",
                getDocumentRequest(),
                getDocumentResponse(),
                requestFields(
                    fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                    fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.NUMBER).description("결과 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메세지"),
                    fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 데이터"),
                    fieldWithPath("data.isRegistered").type(JsonFieldType.BOOLEAN).description("회원가입 유무"),
                    fieldWithPath("data.usersInfo").type(JsonFieldType.OBJECT).description("유저 로그인 정보"),
                    fieldWithPath("data.usersInfo.usersId").type(JsonFieldType.STRING).description("유저 고유번호"),
                    fieldWithPath("data.usersInfo.accessToken").type(JsonFieldType.STRING).description("JWT Access Token"),
                    fieldWithPath("data.usersInfo.refreshToken").type(JsonFieldType.STRING).description("JWT Refresh Token"),
                    fieldWithPath("data.usersInfo.lastLoginAt").type(JsonFieldType.STRING).attributes(dateFormatFull()).description("마지막 로그인 일시").optional()
                )
            ));

    }

    @Test
    void 자동_로그인() throws Exception {

        given(usersFacade.autoLogin(anyString(), any(HttpServletResponse.class))).willReturn(givenLoginResponse());

        mockMvc.perform(
                post("/users/auto-login")
                    .header(HttpHeaders.AUTHORIZATION, JwtTokenUtil.PREFIX + "AccessToken")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("code").value(200))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andDo(document("users/auto-login",
                getDocumentRequest(),
                getDocumentResponse(),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.NUMBER).description("결과 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메세지"),
                    fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 데이터"),
                    fieldWithPath("data.isRegistered").type(JsonFieldType.BOOLEAN).description("회원가입 유무"),
                    fieldWithPath("data.usersInfo").type(JsonFieldType.OBJECT).description("유저 로그인 정보"),
                    fieldWithPath("data.usersInfo.usersId").type(JsonFieldType.STRING).description("유저 고유번호"),
                    fieldWithPath("data.usersInfo.accessToken").type(JsonFieldType.STRING).description("JWT Access Token"),
                    fieldWithPath("data.usersInfo.refreshToken").type(JsonFieldType.STRING).description("JWT Refresh Token"),
                    fieldWithPath("data.usersInfo.lastLoginAt").type(JsonFieldType.STRING).attributes(dateFormatFull()).description("마지막 로그인 일시").optional()
                )
            ));

        verify(usersFacade).autoLogin(anyString(), any(HttpServletResponse.class));

    }

    @Test
    void 비밀번호_찾기() throws Exception {

        given(usersFacade.passwordFind(any(UsersCommand.PasswordFind.class))).willReturn(true);

        UsersDTO.PasswordFindRequest request = new UsersDTO.PasswordFindRequest("team.sikdorok@gmail.com");

        mockMvc.perform(
            post("/users/password-find")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("code").value(200))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andDo(document("users/password-find",
                getDocumentRequest(),
                getDocumentResponse(),
                requestFields(
                    fieldWithPath("email").type(JsonFieldType.STRING).description("이메일")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.NUMBER).description("결과 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메세지"),
                    fieldWithPath("data").type(JsonFieldType.BOOLEAN).description("전송 결과")
                )
            ));

        verify(usersFacade).passwordFind(any(UsersCommand.PasswordFind.class));

    }

    @Test
    void 비밀번호_링크_유효성_검사() throws Exception {

        given(usersFacade.passwordLinkAlive(any(UsersCommand.PasswordLinkAlive.class))).willReturn(true);

        UsersDTO.PasswordLinkAliveRequest request = new UsersDTO.PasswordLinkAliveRequest(UUID.randomUUID(), "012345");

        mockMvc.perform(
                post("/users/password-link-alive")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("code").value(200))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andDo(document("users/password-link-alive",
                getDocumentRequest(),
                getDocumentResponse(),
                requestFields(
                    fieldWithPath("usersId").type(JsonFieldType.STRING).description("유저 고유번호"),
                    fieldWithPath("code").type(JsonFieldType.STRING).description("인증코드")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.NUMBER).description("결과 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메세지"),
                    fieldWithPath("data").type(JsonFieldType.BOOLEAN).description("결과 데이터")
                )
            ));

        verify(usersFacade).passwordLinkAlive(any(UsersCommand.PasswordLinkAlive.class));

    }

    @Test
    void 비밀번호_재설정() throws Exception {

        doNothing().when(usersFacade).passwordReset(any(UsersCommand.PasswordReset.class));

        UsersDTO.PasswordResetRequest request = new UsersDTO.PasswordResetRequest(UUID.randomUUID(), "qwer1234!", "qwer1234!");

        mockMvc.perform(
                put("/users/password-reset")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("code").value(200))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andDo(document("users/password-reset",
                getDocumentRequest(),
                getDocumentResponse(),
                requestFields(
                    fieldWithPath("usersId").type(JsonFieldType.STRING).description("유저 고유번호"),
                    fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호"),
                    fieldWithPath("passwordCheck").type(JsonFieldType.STRING).description("비밀번호 확인")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.NUMBER).description("결과 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메세지"),
                    fieldWithPath("data").type(JsonFieldType.NULL).description("결과 데이터")
                )
            ));

        verify(usersFacade).passwordReset(any(UsersCommand.PasswordReset.class));

    }

    @Test
    void 이메일_중복검사() throws Exception {

        given(usersFacade.emailCheck(anyString())).willReturn(false);

        mockMvc.perform(
                get("/users/email-check/{email}", "team.sikdorok@gmail.com")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("code").value(200))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andDo(document("users/email-check",
                getDocumentRequest(),
                getDocumentResponse(),
                pathParameters(
                    parameterWithName("email").description("이메일")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.NUMBER).description("결과 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메세지"),
                    fieldWithPath("data").type(JsonFieldType.BOOLEAN).description("결과 데이터")
                )
            ));

        verify(usersFacade).emailCheck(anyString());

    }

    @Test
    void accessToken_재발급() throws Exception {

        given(usersFacade.accessToken(anyString())).willReturn("AccessToken");

        UsersDTO.AccessTokenRequest request = new UsersDTO.AccessTokenRequest("RefreshToken");

        mockMvc.perform(
                post("/users/access-token")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("code").value(200))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andDo(document("users/access-token",
                getDocumentRequest(),
                getDocumentResponse(),
                requestFields(
                    fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("Refresh Token")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.NUMBER).description("결과 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메세지"),
                    fieldWithPath("data").type(JsonFieldType.STRING).description("AccessToken")
                )
            ));

    }

    @Test
    void 회원탈퇴() throws Exception {

        doNothing().when(usersFacade).revoke(anyString());

        mockMvc.perform(
                delete("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, JwtTokenUtil.PREFIX + "AccessToken")

            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("code").value(200))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andDo(document("users/revoke",
                getDocumentRequest(),
                getDocumentResponse(),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.NUMBER).description("결과 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메세지"),
                    fieldWithPath("data").type(JsonFieldType.NULL).description("결과 데이터")
                )
            ));

    }

    @Test
    void 설정() throws Exception {

        given(usersFacade.settings(anyString(), anyString(), any(DefinedCode.class))).willReturn(givenSettingsResponse());

        mockMvc.perform(
                get("/users/settings?version={version}&deviceType={deviceType}", "1.0.0", DefinedCode.C000700001)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, JwtTokenUtil.PREFIX + "AccessToken")

            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("code").value(200))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andDo(document("users/settings",
                getDocumentRequest(),
                getDocumentResponse(),
                queryParameters(
                    parameterWithName("version").description("버전"),
                    parameterWithName("deviceType").attributes(deviceTypeFormat()).description("디바이스 종류")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.NUMBER).description("결과 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메세지"),
                    fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 데이터"),
                    fieldWithPath("data.oauthType").type(JsonFieldType.STRING).attributes(oauthTypeFormat()).description("oauth 종류").optional(),
                    fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("닉네임"),
                    fieldWithPath("data.email").type(JsonFieldType.STRING).description("이메일"),
                    fieldWithPath("data.isLatest").type(JsonFieldType.BOOLEAN).description("최신 버전 유무")
                )
            ));

    }

}