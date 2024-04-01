package leaguehub.leaguehubbackend.domain.notice.entity;

import lombok.Getter;

@Getter
public enum GameType {


    TFT("https://www.leagueoflegends.com/ko-kr/news/game-updates/",
            "#gatsby-focus-wrapper > div > div.style__Wrapper-sc-1ynvx8h-0.style__ResponsiveWrapper-sc-1ynvx8h-6.bNRNtU.dzWqHp > div > div.style__Wrapper-sc-106zuld-0.style__ResponsiveWrapper-sc-106zuld-4.enQqER.jYHLfd.style__List-sc-1ynvx8h-3.qfKFn > div > ol > li",
            "a > article > div.style__Info-sc-1h41bzo-6.eBtwVi > div > h2"),
    LOL("https://www.leagueoflegends.com/ko-kr/news/notices/",
            "#gatsby-focus-wrapper > div > div.style__Wrapper-sc-1ynvx8h-0.style__ResponsiveWrapper-sc-1ynvx8h-6.bNRNtU.dzWqHp > div > div.style__Wrapper-sc-106zuld-0.style__ResponsiveWrapper-sc-106zuld-4.enQqER.jYHLfd.style__List-sc-1ynvx8h-3.qfKFn > div > ol > li",
            "a > article > div.style__Info-sc-1h41bzo-6.eBtwVi > div > h2"),
    FC("https://fconline.nexon.com/news/notice/list",
            "#divListPart > div.board_list > div.content > div.list_wrap > div.tbody > div:nth-child(%d) > a",
            "a > span.td.subject"),
    HOS("https://news.blizzard.com/ko-kr/hearthstone",
            "#recent-articles > li:nth-child(%d) > article > a",
            null),
    MAIN(null, null, null);


    private final String url;

    private final String selector;

    private final String titleSelector;

    GameType(String url, String selector, String titleSelector) {
        this.url = url;
        this.selector = selector;
        this.titleSelector = titleSelector;
    }
}
