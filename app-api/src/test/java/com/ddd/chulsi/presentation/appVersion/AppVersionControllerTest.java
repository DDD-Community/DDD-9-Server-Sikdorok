package com.ddd.chulsi.presentation.appVersion;

import com.ddd.chulsi.application.appVersion.AppVersionFacade;
import com.ddd.chulsi.domainCore.model.appVersion.AppVersionCommand;
import com.ddd.chulsi.domainCore.model.shared.DefinedCode;
import com.ddd.chulsi.infrastructure.jwt.JwtTokenUtil;
import com.ddd.chulsi.presentation.appVersion.dto.AppVersionDTO;
import com.ddd.chulsi.presentation.shared.ControllerTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static com.ddd.chulsi.infrastructure.format.DocumentOptionalGenerator.deviceTypeFormat;
import static com.ddd.chulsi.infrastructure.inMemory.appVersion.AppVersionFactory.givenAppVersionInfoResponse;
import static com.ddd.chulsi.infrastructure.inMemory.appVersion.AppVersionFactory.givenAppVersionRegisterRequest;
import static com.ddd.chulsi.infrastructure.util.ApiDocumentUtils.getDocumentRequest;
import static com.ddd.chulsi.infrastructure.util.ApiDocumentUtils.getDocumentResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AppVersionController.class)
class AppVersionControllerTest extends ControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppVersionFacade appVersionFacade;

    @Test
    void 등록() throws Exception {

        doNothing().when(appVersionFacade).register(anyString(), any(AppVersionCommand.AppVersionRegister.class));

        AppVersionDTO.AppVersionRegisterRequest request = givenAppVersionRegisterRequest();

        mockMvc.perform(
                post("/app-version")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, JwtTokenUtil.PREFIX + "AccessToken")
                    .with(user("user").authorities((GrantedAuthority) () -> String.valueOf(DefinedCode.C000100001)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("code").value(200))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andDo(document("app-version/register",
                getDocumentRequest(),
                getDocumentResponse(),
                requestFields(
                    fieldWithPath("type").type(JsonFieldType.STRING).attributes(deviceTypeFormat()).description("디바이스 정보"),
                    fieldWithPath("major").type(JsonFieldType.NUMBER).description("메이저 번호"),
                    fieldWithPath("minor").type(JsonFieldType.NUMBER).description("마이너 번호"),
                    fieldWithPath("patch").type(JsonFieldType.NUMBER).description("패치 번호"),
                    fieldWithPath("forceUpdateStatus").type(JsonFieldType.BOOLEAN).description("강제 업데이트 여부")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.NUMBER).description("결과 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메세지"),
                    fieldWithPath("data").type(JsonFieldType.NULL).description("결과 데이터")
                )
            ));

    }

    @Test
    void 최신버전_조회() throws Exception {

        given(appVersionFacade.latest(any(DefinedCode.class))).willReturn(givenAppVersionInfoResponse());

        mockMvc.perform(
                RestDocumentationRequestBuilders.get("/app-version/{type}", DefinedCode.C000700001)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("code").value(200))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andDo(document("app-version/latest",
                getDocumentRequest(),
                getDocumentResponse(),
                pathParameters(
                    parameterWithName("type").attributes(deviceTypeFormat()).description("디바이스 정보")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.NUMBER).description("결과 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메세지"),
                    fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 데이터").optional(),
                    fieldWithPath("data.appVersion").type(JsonFieldType.OBJECT).description("결과 데이터").optional(),
                    fieldWithPath("data.appVersion.appInfoAppVersion").type(JsonFieldType.STRING).description("버전 정보"),
                    fieldWithPath("data.appVersion.major").type(JsonFieldType.NUMBER).description("메이저 번호"),
                    fieldWithPath("data.appVersion.minor").type(JsonFieldType.NUMBER).description("마이너 번호"),
                    fieldWithPath("data.appVersion.patch").type(JsonFieldType.NUMBER).description("패치 번호"),
                    fieldWithPath("data.appVersion.forceUpdateStatus").type(JsonFieldType.BOOLEAN).description("강제 업데이트 여부")
                )
            ));

    }

}