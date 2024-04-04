package leaguehub.leaguehubbackend.service.notice;

import leaguehub.leaguehubbackend.domain.member.service.MemberService;
import leaguehub.leaguehubbackend.domain.notice.dto.NoticeDto;
import leaguehub.leaguehubbackend.domain.notice.entity.GameType;
import leaguehub.leaguehubbackend.domain.notice.entity.Notice;
import leaguehub.leaguehubbackend.domain.notice.exception.exception.NoticeUnsupportedException;
import leaguehub.leaguehubbackend.domain.notice.service.NoticeService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;


@SpringBootTest
@Transactional
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(locations = "classpath:application-test.properties")
public class NoticeServiceTest {

    @Autowired
    MemberService memberService;

    @Autowired
    NoticeService noticeService;


    @Test
    @DisplayName("공지사항 추출 테스트")
    void noticeTest() {

        List<NoticeDto> notices = noticeService.getNotices(GameType.MAIN);

        assertThat(notices.get(0).getNoticeTitle()).isEqualTo("리그허브 서비스 오픈");
        assertThat(notices.get(1).getNoticeTitle()).isEqualTo("리그허브 서비스 안정화");
        assertThat(notices.get(2).getNoticeTitle()).isEqualTo("리그허브 이벤트 안내");

    }

    @Test
    @DisplayName("공지사항 업데이트 테스트")
    void noticeUpdateTest() throws Exception {
        //given
        List<Notice> noticeList = noticeService.updateNotices(GameType.TFT);

        assertThat(noticeList.size()).isEqualTo(6);
        assertThatThrownBy(()->noticeService.updateNotices(GameType.MAIN)).isInstanceOf(NoticeUnsupportedException.class);
    }
}
