package leaguehub.leaguehubbackend.domain.match.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import leaguehub.leaguehubbackend.domain.match.dto.MatchCallAdminDto;
import leaguehub.leaguehubbackend.domain.match.dto.MatchSetCountDto;
import leaguehub.leaguehubbackend.domain.match.service.MatchService;
import leaguehub.leaguehubbackend.domain.match.service.chat.MatchChatService;
import leaguehub.leaguehubbackend.global.exception.global.ExceptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.OK;

@Tag(name = "Match-Controller", description = "대회 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MatchController {

    private final MatchService matchService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final MatchChatService matchChatService;


    @Operation(summary = "해당 채널의 라운드 경기 배정")
    @Parameters(value = {
            @Parameter(name = "channelLink", description = "해당 채널의 링크", example = "42aa1b11ab88"),
            @Parameter(name = "matchRound", description = "배정 싶은 매치의 라운드(1, 2 라운드)", example = "1, 2, 3, 4")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "참가자들이 첫 매치에 배정되었습니다."),
            @ApiResponse(responseCode = "403", description = "권한이 관리자가 아님,채널을 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/match/{channelLink}/{matchRound}")
    public ResponseEntity assignmentMatches(@PathVariable("channelLink") String channelLink, @PathVariable("matchRound") Integer matchRound) {

        matchService.matchAssignment(channelLink, matchRound);

        return new ResponseEntity<>("참가자들이 첫 매치에 배정되었습니다.", OK);
    }


    @Operation(summary = "해당 채널의 (1, 2, 3)라운드에 대한 경기 횟수 설정")
    @Parameters(value = {
            @Parameter(name = "channelLink", description = "해당 채널의 링크", example = "42aa1b11ab88"),
            @Parameter(name = "roundCountList", description = "설정할려는 횟수 배열", example = "[3, 4, 2, 1]")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "경기 횟수가 배정되었습니다."),
            @ApiResponse(responseCode = "403", description = "매치 또는 채널을 찾을 수 없습니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/match/{channelLink}/count")
    public ResponseEntity setMatchRoundCount(@PathVariable("channelLink") String channelLink,
                                             @RequestBody MatchSetCountDto matchSetCountDto) {

        matchService.setMatchSetCount(channelLink, matchSetCountDto.getMatchSetCountList());

        return new ResponseEntity("경기 횟수가 배정되었습니다.", OK);
    }

    @MessageMapping("/match/{channelLink}/{participantId}/{matchId}/call-admin")
    public void callAdmin(@DestinationVariable("channelLink") String channelLink,
                          @DestinationVariable("participantId") String participantId,
                          @DestinationVariable("matchId") String matchId) {

        MatchCallAdminDto matchCallAdminDto = matchService.callAdmin(channelLink, Long.valueOf(matchId), Long.valueOf(participantId));

        matchChatService.processAdminAlert(channelLink, Long.valueOf(matchId));

        simpMessagingTemplate.convertAndSend("/match/" + channelLink, matchCallAdminDto);
    }


    @Operation(summary = "해당 매치 알람 끄기")
    @Parameters(value = {
            @Parameter(name = "channelLink", description = "해당 채널의 링크", example = "42aa1b11ab88"),
            @Parameter(name = "matchId", description = "알람을 끄는 match의 PK", example = "1, 2, 3, 4")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "403", description = "권한이 관리자가 아님,채널을 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/match/{channelLink}/{matchId}/call-off")
    public ResponseEntity turnOffAlarm(@PathVariable("channelLink") String channelLink,
                                       @PathVariable("matchId") Long matchId) {

        matchService.turnOffAlarm(channelLink, matchId);

        return new ResponseEntity(OK);
    }

}
