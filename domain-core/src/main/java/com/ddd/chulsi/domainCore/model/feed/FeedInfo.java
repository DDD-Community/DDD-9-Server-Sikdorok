package com.ddd.chulsi.domainCore.model.feed;

import com.ddd.chulsi.domainCore.model.photos.PhotosInfo;
import com.ddd.chulsi.domainCore.model.shared.DefinedCode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class FeedInfo {

    public record FeedInfoDTO (
        UUID feedId,
        boolean isMine,
        DefinedCode tag,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        LocalDateTime time,
        String memo,
        DefinedCode icon,
        boolean isMain,
        List<PhotosInfo.Info> photosInfoList,
        int photosLimit
    ) {
        public static FeedInfoDTO toDTO(Feed feed, UUID usersId, List<PhotosInfo.Info> photosInfoList, int photosLimit) {
            return new FeedInfoDTO(
                feed.getFeedId(),
                usersId.equals(feed.getUsersId()),
                DefinedCode.valueOf(feed.getTag()),
                feed.getTime(),
                feed.getMemo(),
                feed.getIcon(),
                feed.isMain(),
                photosInfoList,
                photosLimit
            );
        }
    }

    public record HomeFeedItemDTO(
        List<PhotosInfo.Info> photosInfoList,
        DefinedCode icon,
        boolean isMain,
        String tag,
        LocalDateTime time,
        String memo
    ) {
    }

    public record HomeFeedItem(
        List<PhotosInfo.Info> photosInfoList,
        DefinedCode icon,
        boolean isMain,

        @JsonIgnore
        DefinedCode tag,
        String time,
        String memo
    ) {
    }

    public record HomeInfo(
        List<List<WeeklyFeed>> weeklyFeeds,
        List<DailyFeed> dailyFeeds
    ) {

        public record WeeklyFeed(
            @JsonIgnore
            int week,

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
            LocalDate time,
            DefinedCode icon
        ) {
            public static WeeklyFeed toDTO(FeedInfo.HomeInfo.Weekly weekly) {
                return new WeeklyFeed(weekly.getWeek(), weekly.getTime(), weekly.getIcon());
            }
        }

        public interface Weekly {
            int getWeek();
            LocalDate getTime();
            DefinedCode getIcon();
        }

        public record DailyFeed(
            DefinedCode tags,
            List<FeedInfo.HomeFeedItem> homeFeedItems
        ) {

        }

    }
}
