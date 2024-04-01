package leaguehub.leaguehubbackend.domain.notice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NoticeDto {

    private String noticeLink;

    private String noticeTitle;

    private String noticeInfo;
}