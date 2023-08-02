package com.ddd.chulsi.presentation.feed.dto;

import com.ddd.chulsi.domainCore.model.feed.FeedCommand;
import com.ddd.chulsi.domainCore.model.feed.FeedInfo;
import com.ddd.chulsi.domainCore.model.shared.DefinedCode;
import com.ddd.chulsi.infrastructure.exception.BadRequestException;
import com.ddd.chulsi.infrastructure.exception.message.ErrorMessage;
import com.ddd.chulsi.presentation.shared.request.Validator;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public class FeedDTO {


    public record FeedRegisterRequest (
        @NotNull
        DefinedCode tag,

        @NotNull
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        LocalDateTime time,
        String memo,

        @NotNull
        DefinedCode icon,
        boolean isMain
    ) implements Validator {
        public FeedCommand.RegisterCommand toCommand() {
            return FeedCommand.RegisterCommand.nonState(tag, time, memo, icon, isMain);
        }

        @Override
        public void verify() {
            if (!tag.getSectionCode().equals(DefinedCode.C0003.getSectionCode()))
                throw new BadRequestException(ErrorMessage.EXPECTATION_FAILED_MSG_DEFAULT, "tag");
            if (!icon.getSectionCode().equals(DefinedCode.C0004.getSectionCode()))
                throw new BadRequestException(ErrorMessage.EXPECTATION_FAILED_MSG_DEFAULT, "icon");
            if (time.toLocalDate().isAfter(LocalDateTime.now().toLocalDate()))
                throw new BadRequestException("당일 날짜만 등록 가능합니다.", "time");
        }
    }

    public record FeedInfoResponse (
        String nickname,
        FeedInfo.FeedInfoDTO feedInfo
    ) {

    }

    public record FeedInfoUpdateRequest(
        @NotNull
        UUID feedId,

        @NotNull
        DefinedCode tag,

        @NotNull
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        LocalDateTime time,
        String memo,

        @NotNull
        DefinedCode icon,
        boolean isMain,
        Set<UUID> deletePhotoTokens
    ) implements Validator {
        public FeedCommand.InfoUpdateCommand toCommand() {
            return FeedCommand.InfoUpdateCommand.nonState(feedId, tag, time, memo, icon, isMain, deletePhotoTokens);
        }

        @Override
        public void verify() {
            if (!tag.getSectionCode().equals(DefinedCode.C0003.getSectionCode()))
                throw new BadRequestException(ErrorMessage.EXPECTATION_FAILED_MSG_DEFAULT, "tag");
            if (!icon.getSectionCode().equals(DefinedCode.C0004.getSectionCode()))
                throw new BadRequestException(ErrorMessage.EXPECTATION_FAILED_MSG_DEFAULT, "icon");
            if (time.toLocalDate().isAfter(LocalDateTime.now().toLocalDate()))
                throw new BadRequestException("당일 날짜만 등록 가능합니다.", "time");
        }
    }
}
