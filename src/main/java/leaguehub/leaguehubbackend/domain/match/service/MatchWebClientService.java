package leaguehub.leaguehubbackend.domain.match.service;

import leaguehub.leaguehubbackend.domain.match.exception.exception.MatchResultIdNotFoundException;
import leaguehub.leaguehubbackend.domain.participant.exception.exception.ParticipantGameIdNotFoundException;
import leaguehub.leaguehubbackend.global.exception.global.exception.GlobalServerErrorException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@Service
@RequiredArgsConstructor
@Transactional
public class MatchWebClientService {


    private final WebClient webClient;
    private final JSONParser jsonParser;
    @Value("${riot-api-key-1}")
    private String riot_api_key_1;
    @Value("${riot-api-key-2}")
    private String riot_api_key_2;


    /**
     * 소환사의 라이엇 puuid를 얻는 메서드
     *
     * @param name 게임 닉네임
     * @return puuid
     */
    public String getSummonerPuuid(String name) {
        String gameId = name.split("#")[0];
        String gameTag = name.split("#")[1];

        String summonerPuuidUrl = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/";

        JSONObject userAccount = webClient.get()
                .uri(summonerPuuidUrl + gameId + "/" + gameTag + riot_api_key_1)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.error(new ParticipantGameIdNotFoundException()))
                .onStatus(HttpStatusCode::is5xxServerError, response -> Mono.error(new GlobalServerErrorException()))
                .bodyToMono(JSONObject.class)
                .block();


        String puuid =  userAccount.get("puuid").toString();

        return puuid;

    }


    /**
     * 게임 Id로 얻은 puuid로 라이엇 서버에 고유 매치 Id 검색
     *
     * @param puuid
     * @return
     */
    public String getMatch(String puuid, long endTime) {
//        long endTime = System.currentTimeMillis() / 1000;
        long statTime = 0;

        String matchUrl = "https://asia.api.riotgames.com/tft/match/v1/matches/by-puuid/";
        String Option = "/ids?start=0&endTime=" + endTime + "&startTime=" + statTime + "&count=1";


        JSONArray matchArray = webClient.get()
                .uri(matchUrl + puuid + Option + riot_api_key_2)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.error(new MatchResultIdNotFoundException()))
                .onStatus(HttpStatusCode::is5xxServerError, response -> Mono.error(new GlobalServerErrorException()))
                .bodyToMono(JSONArray.class)
                .block();


        return matchArray.get(0).toString();
    }



    @SneakyThrows
    public JSONObject responseMatchDetail(String matchId) {
        String matchDetailUrl = "https://asia.api.riotgames.com/tft/match/v1/matches/";

        return (JSONObject) jsonParser.parse
                (webClient
                        .get()
                        .uri(matchDetailUrl + matchId + riot_api_key_1)
                        .retrieve()
                        .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(new GlobalServerErrorException()))
                        .bodyToMono(JSONObject.class)
                        .block().toJSONString());
    }


}
