package leaguehub.leaguehubbackend.global.config;


import leaguehub.leaguehubbackend.domain.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalTime;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class SchedulingConfig {

    private final NoticeService noticeService;


    @Scheduled(cron = "${my.custom.cron}")
    @Bean
    public void noticeUpdateRun(){

        noticeService.updateNoticeSchedule();
        log.info("업데이트 한 시간 = " + LocalTime.now());
    }
}
