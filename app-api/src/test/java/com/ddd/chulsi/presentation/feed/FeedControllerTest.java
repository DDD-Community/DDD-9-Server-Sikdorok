package com.ddd.chulsi.presentation.feed;

import com.ddd.chulsi.application.feed.FeedFacade;
import com.ddd.chulsi.domainCore.model.feed.FeedCommand;
import com.ddd.chulsi.domainCore.model.shared.DefinedCode;
import com.ddd.chulsi.infrastructure.jwt.JwtTokenUtil;
import com.ddd.chulsi.presentation.feed.dto.FeedDTO;
import com.ddd.chulsi.presentation.shared.ControllerTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.ddd.chulsi.infrastructure.format.DocumentOptionalGenerator.*;
import static com.ddd.chulsi.infrastructure.inMemory.feed.FeedFactory.*;
import static com.ddd.chulsi.infrastructure.util.ApiDocumentUtils.getDocumentRequest;
import static com.ddd.chulsi.infrastructure.util.ApiDocumentUtils.getDocumentResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeedController.class)
class FeedControllerTest extends ControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FeedFacade feedFacade;

    @Test
    void 단일_조회() throws Exception {

        given(feedFacade.info(anyString(), any(UUID.class))).willReturn(givenFeedInfoResponse());

        mockMvc.perform(
                get(
                    "/feed/{feedId}",
                    UUID.randomUUID()
                )
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, JwtTokenUtil.PREFIX + "AccessToken")
                    .with(user("user").authorities((GrantedAuthority) () -> String.valueOf(DefinedCode.C000100001)))
                    .with(user("user").authorities((GrantedAuthority) () -> String.valueOf(DefinedCode.C000100002)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("code").value(200))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andDo(document("feed/info",
                getDocumentRequest(),
                getDocumentResponse(),
                pathParameters(
                    parameterWithName("feedId").description("피드 고유번호")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.NUMBER).description("결과 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메세지"),
                    fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 데이터"),
                    fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("소유자 닉네임"),
                    fieldWithPath("data.feedInfo").type(JsonFieldType.OBJECT).description("피드 정보"),
                    fieldWithPath("data.feedInfo.feedId").type(JsonFieldType.STRING).description("피드 고유번호"),
                    fieldWithPath("data.feedInfo.isMine").type(JsonFieldType.BOOLEAN).description("소유권 여부"),
                    fieldWithPath("data.feedInfo.tag").type(JsonFieldType.STRING).description("태그"),
                    fieldWithPath("data.feedInfo.time").type(JsonFieldType.STRING).attributes(dateFormatFull()).description("날짜 및 시간"),
                    fieldWithPath("data.feedInfo.memo").type(JsonFieldType.STRING).description("내용").optional(),
                    fieldWithPath("data.feedInfo.icon").type(JsonFieldType.STRING).description("대표 아이콘"),
                    fieldWithPath("data.feedInfo.isMain").type(JsonFieldType.BOOLEAN).description("대표 아이콘 설정 여부"),
                    fieldWithPath("data.feedInfo.photosInfoList[]").type(JsonFieldType.ARRAY).description("사진 정보 목록").optional(),
                    fieldWithPath("data.feedInfo.photosInfoList[].token").type(JsonFieldType.STRING).description("사진 토큰"),
                    fieldWithPath("data.feedInfo.photosInfoList[].uploadFullPath").type(JsonFieldType.STRING).description("첨부파일 주소"),
                    fieldWithPath("data.feedInfo.photosLimit").type(JsonFieldType.NUMBER).description("현재 업로드한 사진 개수")
                )
            ));

    }

    @Test
    void 등록() throws Exception {

        given(feedFacade.register(anyString(), any(FeedCommand.RegisterCommand.class), any(MultipartFile.class))).willReturn(UUID.randomUUID());

        FeedDTO.FeedRegisterRequest feedRegisterRequest = givenRegisterRequest();

        MockMultipartFile file = new MockMultipartFile("file", "profile.png", "multipart/form-data", "uploadFile".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile request = new MockMultipartFile("request", null, "application/json", objectMapper.writeValueAsString(feedRegisterRequest).getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(
                multipart("/feed")
                    .file(file)
                    .file(request)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, JwtTokenUtil.PREFIX + "AccessToken")
                    .with(user("user").authorities((GrantedAuthority) () -> String.valueOf(DefinedCode.C000100001)))
                    .with(user("user").authorities((GrantedAuthority) () -> String.valueOf(DefinedCode.C000100002)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("code").value(200))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andDo(document("feed/register",
                getDocumentRequest(),
                getDocumentResponse(),requestParts(
                    partWithName("request").description("요청값"),
                    partWithName("file").description("업로드 파일").optional()
                ),
                requestPartFields(
                    "request",
                    fieldWithPath("tag").type(JsonFieldType.STRING).attributes(tagFormat()).description("태그"),
                    fieldWithPath("time").type(JsonFieldType.STRING).attributes(dateFormatFull()).description("등록시간"),
                    fieldWithPath("memo").type(JsonFieldType.STRING).description("메모").optional(),
                    fieldWithPath("icon").type(JsonFieldType.STRING).attributes(iconFormat()).description("대표 아이콘"),
                    fieldWithPath("isMain").type(JsonFieldType.BOOLEAN).description("대표 아이콘 설정 여부")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.NUMBER).description("결과 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메세지"),
                    fieldWithPath("data").type(JsonFieldType.STRING).description("피드 고유번호")
                )
            ));

    }

    @Test
    void 수정() throws Exception {

        given(feedFacade.infoUpdate(anyString(), any(FeedCommand.InfoUpdateCommand.class), any(MultipartFile.class))).willReturn(UUID.randomUUID());

        FeedDTO.FeedInfoUpdateRequest feedInfoUpdateRequest = givenInfoUpdateRequest();

        MockMultipartFile file = new MockMultipartFile("file", "profile.png", "multipart/form-data", "uploadFile".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile request = new MockMultipartFile("request", null, "application/json", objectMapper.writeValueAsString(feedInfoUpdateRequest).getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(
                multipart(HttpMethod.PUT,"/feed")
                    .file(file)
                    .file(request)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, JwtTokenUtil.PREFIX + "AccessToken")
                    .with(user("user").authorities((GrantedAuthority) () -> String.valueOf(DefinedCode.C000100001)))
                    .with(user("user").authorities((GrantedAuthority) () -> String.valueOf(DefinedCode.C000100002)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("code").value(200))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andDo(document("feed/info-update",
                getDocumentRequest(),
                getDocumentResponse(),requestParts(
                    partWithName("request").description("요청값"),
                    partWithName("file").description("업로드 파일").optional()
                ),
                requestPartFields(
                    "request",
                    fieldWithPath("feedId").type(JsonFieldType.STRING).description("피드 고유번호"),
                    fieldWithPath("tag").type(JsonFieldType.STRING).attributes(tagFormat()).description("태그"),
                    fieldWithPath("time").type(JsonFieldType.STRING).attributes(dateFormatFull()).description("등록시간"),
                    fieldWithPath("memo").type(JsonFieldType.STRING).description("메모").optional(),
                    fieldWithPath("icon").type(JsonFieldType.STRING).attributes(iconFormat()).description("대표 아이콘"),
                    fieldWithPath("isMain").type(JsonFieldType.BOOLEAN).description("대표 아이콘 설정 여부"),
                    fieldWithPath("deletePhotoTokens").type(JsonFieldType.ARRAY).description("삭제할 사진 토큰").optional()
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.NUMBER).description("결과 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메세지"),
                    fieldWithPath("data").type(JsonFieldType.STRING).description("피드 고유번호")
                )
            ));

    }

    @Test
    void 삭제() throws Exception {

        doNothing().when(feedFacade).delete(anyString(), any(UUID.class));

        mockMvc.perform(
                delete(
                    "/feed/{feedId}",
                    UUID.randomUUID()
                )
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, JwtTokenUtil.PREFIX + "AccessToken")
                    .with(user("user").authorities((GrantedAuthority) () -> String.valueOf(DefinedCode.C000100001)))
                    .with(user("user").authorities((GrantedAuthority) () -> String.valueOf(DefinedCode.C000100002)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("code").value(200))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andDo(document("feed/delete",
                getDocumentRequest(),
                getDocumentResponse(),
                pathParameters(
                    parameterWithName("feedId").description("피드 고유번호")
                ),
                responseFields(
                    fieldWithPath("code").type(JsonFieldType.NUMBER).description("결과 코드"),
                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메세지"),
                    fieldWithPath("data").type(JsonFieldType.NULL).description("결과 데이터")
                )
            ));

    }

}