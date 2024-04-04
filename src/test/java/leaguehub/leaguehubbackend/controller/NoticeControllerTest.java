package leaguehub.leaguehubbackend.controller;

import jakarta.transaction.Transactional;
import leaguehub.leaguehubbackend.domain.notice.entity.GameType;
import leaguehub.leaguehubbackend.domain.notice.repository.NoticeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static leaguehub.leaguehubbackend.domain.notice.entity.GameType.MAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
public class NoticeControllerTest {


    @Autowired
    MockMvc mockMvc;

    @Autowired
    NoticeRepository noticeRepository;

    @Test
    @DisplayName("저장된 공지사항 추출 테스트")
    void findTargetNoticeControllerTest() throws Exception {

        mockMvc.perform(get("/api/notice/" + MAIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("[0].noticeTitle").value("리그허브 서비스 오픈"))
                .andExpect(jsonPath("[1].noticeTitle").value("리그허브 서비스 안정화"))
                .andExpect(jsonPath("[2].noticeTitle").value("리그허브 이벤트 안내"));
    }

    @Test
    @DisplayName("공지사항 업데이트 테스트")
    void noticeUpdateControllerTest() throws Exception {
        //given
        GameType selectType = GameType.LOL;

        mockMvc.perform(post("/api/notice/new/" + selectType))
                .andExpect(status().isOk());

    }
}
