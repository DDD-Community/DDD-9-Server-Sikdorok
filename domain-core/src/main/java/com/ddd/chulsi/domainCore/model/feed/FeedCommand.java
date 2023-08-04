package com.ddd.chulsi.domainCore.model.feed;

import com.ddd.chulsi.domainCore.model.shared.DefinedCode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public class FeedCommand {

    public record RegisterCommand(
        DefinedCode tag,
        LocalDateTime time,
        String memo,
        DefinedCode icon,
        boolean isMain
    ) {
        public static RegisterCommand nonState(DefinedCode tag, LocalDateTime time, String memo, DefinedCode icon, boolean isMain) {
            return new RegisterCommand(tag, time, memo, icon, isMain);
        }

        public Feed toEntity(UUID usersId) {
            return Feed.builder()
                .usersId(usersId)
                .tag(tag.toString())
                .time(time)
                .memo(memo)
                .icon(icon)
                .isMain(isMain)
                .build();
        }
    }

    public record InfoUpdateCommand(
        UUID feedId,
        DefinedCode tag,
        LocalDateTime time,
        String memo,
        DefinedCode icon,
        boolean isMain,
        Set<UUID> deletePhotoTokens
    ) {
        public static InfoUpdateCommand nonState(UUID feedId, DefinedCode tag, LocalDateTime time, String memo, DefinedCode icon, boolean isMain, Set<UUID> deletePhotoTokens) {
            return new InfoUpdateCommand(feedId, tag, time, memo, icon, isMain, deletePhotoTokens);
        }
    }

    @Getter
    @Builder
    public static class ListCommand {
        private final int page;
        private final int size;
        private final LocalDate date;
        private final DefinedCode tag;
    }
}
