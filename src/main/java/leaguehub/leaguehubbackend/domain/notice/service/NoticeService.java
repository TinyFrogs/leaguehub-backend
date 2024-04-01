package leaguehub.leaguehubbackend.domain.notice.service;

import leaguehub.leaguehubbackend.domain.notice.dto.NoticeDto;
import leaguehub.leaguehubbackend.domain.notice.entity.GameType;
import leaguehub.leaguehubbackend.domain.notice.entity.Notice;
import leaguehub.leaguehubbackend.domain.notice.exception.exception.NoticeUnsupportedException;
import leaguehub.leaguehubbackend.domain.notice.exception.exception.WebScrapingException;
import leaguehub.leaguehubbackend.domain.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static leaguehub.leaguehubbackend.domain.notice.entity.GameType.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeService {


    private final NoticeRepository noticeRepository;

    private final int MAX_NOTICE_COUNT = 6;

    /**
     * 원하는 게임의 공지사항 반환
     *
     * @param gameType
     * @return
     */
    public List<NoticeDto> getNotices(GameType gameType) {
        List<Notice> noticeList = noticeRepository.findAllByGameTypeOrderByIdAsc(gameType);

        List<NoticeDto> noticeDtos = new ArrayList<>();

        for (Notice notice : noticeList) {
            NoticeDto noticedto = NoticeDto.builder()
                    .noticeLink(notice.getGameLink())
                    .noticeTitle(notice.getGameTitle())
                    .noticeInfo(notice.getGameInfo())
                    .build();
            noticeDtos.add(noticedto);
        }

        return noticeDtos;
    }


    /**
     * 게임 공지사항 업데이트(수동)
     *
     * @param gameType
     * @return
     */
    @Transactional
    public List<Notice> updateNotices(GameType gameType) {
        return switch (gameType) {
            case LOL, TFT ->
                    scrapeRiotNotice(gameType, gameType.getUrl(), gameType.getSelector(), gameType.getTitleSelector());
            case FC ->
                    scrapeAnotherNotice(gameType, gameType.getUrl(), gameType.getSelector(), gameType.getTitleSelector(), 4, 9);
            case HOS ->
                    scrapeAnotherNotice(gameType, gameType.getUrl(), gameType.getSelector(), gameType.getTitleSelector(), 1, 6);
            case MAIN -> throw new NoticeUnsupportedException();
            default -> throw new NoticeUnsupportedException();
        };
    }


    /**
     * 일정 주기마다 공지사항 업데이트(자동)
     */
    @Transactional
    public void updateNoticeSchedule() {
        scrapeRiotNotice(LOL, LOL.getUrl(), LOL.getSelector(), LOL.getTitleSelector());
        log.info("LOL 공지사항 업데이트 완료");

        scrapeRiotNotice(TFT, TFT.getUrl(), TFT.getSelector(), TFT.getTitleSelector());
        log.info("TFT 공지사항 업데이트 완료");

        scrapeAnotherNotice(FC, FC.getUrl(), FC.getSelector(), FC.getTitleSelector(), 4, 9);
        log.info("FC 온라인 공지사항 업데이트 완료");

        scrapeAnotherNotice(HOS, HOS.getUrl(), HOS.getSelector(), HOS.getTitleSelector(), 1, 6);
        log.info("하스스톤 공지사항 업데이트 완료");

    }


    private List<Notice> scrapeRiotNotice(GameType gameType, String url, String itemSelector, String titleSelector) {
        try {
            List<Notice> recentNotices = noticeRepository.findAllByGameTypeOrderByIdAsc(gameType);
            List<Notice> updateNotices = findUpdateRiotNotice(gameType, url, itemSelector, titleSelector);

            if (recentNotices.size() < 6)
                createSaveNotice(recentNotices, updateNotices);

            if (recentNotices.size() == 6)
                updateNotices(recentNotices, updateNotices);

        } catch (Exception e) {
            log.error(gameType + "스크래핑 오류");
            throw new WebScrapingException();
        }

        return noticeRepository.findAllByGameTypeOrderByIdAsc(gameType);
    }

    private List<Notice> findUpdateRiotNotice(GameType gameType, String url, String itemSelector, String titleSelector) throws IOException {
        List<Notice> notices = new ArrayList<>();
        Document doc = Jsoup.connect(url).get();

        Elements newsItems = doc.select(
                "#gatsby-focus-wrapper > div > div.style__Wrapper-sc-1ynvx8h-0.style__ResponsiveWrapper-sc-1ynvx8h-6.bNRNtU.dzWqHp > div > div.style__Wrapper-sc-106zuld-0.style__ResponsiveWrapper-sc-106zuld-4.enQqER.jYHLfd.style__List-sc-1ynvx8h-3.qfKFn > div > ol > li");

        for (Element item : newsItems) {
            String newsLink = item.select("a").attr("abs:href");
            String title = item.select(itemSelector).text();
            String metaData = item.select(
                            titleSelector)
                    .text();

            Notice notice = Notice.createNotice(gameType, newsLink, title, metaData);

            notices.add(notice);
        }

        return notices;
    }

    private List<Notice> scrapeAnotherNotice(GameType gameType, String url, String itemSelector, String titleSelector, Integer start, Integer end) {
        try {
            List<Notice> updateNotices = findUpdateAnotherNotice(gameType, url, itemSelector, titleSelector, start, end);
            List<Notice> recentNotices = noticeRepository.findAllByGameTypeOrderByIdAsc(gameType);
            if (recentNotices.size() < 6)
                createSaveNotice(recentNotices, updateNotices);

            if (recentNotices.size() == 6)
                updateNotices(recentNotices, updateNotices);

        } catch (Exception e) {
            log.error(gameType + "스크래핑 오류");
            throw new WebScrapingException();
        }

        return noticeRepository.findAllByGameTypeOrderByIdAsc(gameType);
    }

    private List<Notice> findUpdateAnotherNotice(GameType gameType, String url, String itemSelector, String titleSelector, Integer start, Integer end) throws IOException {
        Document doc = Jsoup.connect(url).get();

        int startIndex = start != null ? start : 1;
        int endIndex = end != null ? end : doc.select(itemSelector).size();

        return IntStream.rangeClosed(startIndex, endIndex)
                .mapToObj(i -> doc.select(String.format(itemSelector, i)))
                .filter(elements -> !elements.isEmpty())
                .map(Elements::first).filter(Objects::nonNull)
                .map(newsItem ->
                        createNoticeFromElement(gameType, newsItem, titleSelector))
                .toList();
    }


    private void createSaveNotice(List<Notice> recentNotices, List<Notice> updateNotices) {
        int createNoticeCount = MAX_NOTICE_COUNT - recentNotices.size();
        for (int i = 0; i < createNoticeCount; i++) {
            noticeRepository.save(updateNotices.get(i));
        }
    }

    private void updateNotices(List<Notice> recentNotices, List<Notice> updateNotices) {
        for (int i = 0; i < recentNotices.size(); i++) {
            recentNotices.get(i).updateNotice(updateNotices.get(i));
        }
    }

    private Notice createNoticeFromElement(GameType gameType, Element element, String titleSelector) {
        String newsLink = element.select("a").attr("abs:href");
        String title = titleSelector != null ? element.select(titleSelector).text() : element.text();

        return Notice.createNotice(gameType, newsLink, title, " ");
    }
}