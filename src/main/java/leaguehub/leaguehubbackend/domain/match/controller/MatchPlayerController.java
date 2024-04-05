package leaguehub.leaguehubbackend.domain.match.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import leaguehub.leaguehubbackend.domain.match.dto.MatchInfoDto;
import leaguehub.leaguehubbackend.domain.match.dto.MatchSetReadyMessage;
import leaguehub.leaguehubbackend.domain.match.service.MatchPlayerService;
import leaguehub.leaguehubbackend.domain.participant.dto.ParticipantIdResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Match-Player-Controller", description = "대회 경기자 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MatchPlayerController {


    private final MatchPlayerService matchPlayerService;
    private final SimpMessagingTemplate simpMessagingTemplate;


    /**
     * 참가자 매치 체크인
     * @param matchIdStr
     * @param message
     */
    @MessageMapping("/match/{matchId}/checkIn")
    public void checkIn(@DestinationVariable("matchId") String matchIdStr, @Payload MatchSetReadyMessage message) {

        ParticipantIdResponseDto participantIdResponseDto = matchPlayerService.markPlayerAsReady(message, matchIdStr);

        simpMessagingTemplate.convertAndSend("/match/" + matchIdStr, participantIdResponseDto);
    }

    /**
     * 참가자 매치 점수 업데이트
     * @param matchIdStr
     * @param matchSetStr
     */
    @MessageMapping("/match/{matchId}/{matchSet}/score-update")
    public void updateMatchPlayerScore(@DestinationVariable("matchId") String matchIdStr, @DestinationVariable("matchSet") String matchSetStr) {
        Long matchId = Long.valueOf(matchIdStr);
        Integer matchSet = Integer.valueOf(matchSetStr);
        long endTime = System.currentTimeMillis() / 1000;
        MatchInfoDto matchInfoDto = matchPlayerService.updateMatchPlayerScore(matchId, matchSet, endTime);

        simpMessagingTemplate.convertAndSend("/match/" + matchId + "/" + matchSet, matchInfoDto);
    }
}
