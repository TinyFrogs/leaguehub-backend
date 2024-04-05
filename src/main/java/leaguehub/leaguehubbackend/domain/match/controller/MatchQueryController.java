package leaguehub.leaguehubbackend.domain.match.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import leaguehub.leaguehubbackend.domain.match.dto.*;
import leaguehub.leaguehubbackend.domain.match.service.MatchQueryService;
import leaguehub.leaguehubbackend.domain.match.service.chat.MatchChatService;
import leaguehub.leaguehubbackend.global.exception.global.ExceptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.HttpStatus.OK;

@Tag(name = "Match-Query-Controller", description = "대회 조회 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MatchQueryController {


    private final MatchChatService matchChatService;
    private final MatchQueryService matchQueryService;


    @Operation(summary = "라운드 수(몇 강) 리스트 반환 - 사용자")
    @Parameter(name = "channelLink", description = "해당 채널의 링크", example = "42aa1b11ab88")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "라운드(몇 강) 리스트 반환", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MatchRoundListDto.class))),
            @ApiResponse(responseCode = "403", description = "매치 결과를 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/match/{channelLink}")
    public ResponseEntity loadMatchRoundList(@PathVariable("channelLink") String channelLink) {

        MatchRoundListDto roundList = matchQueryService.getRoundList(channelLink);

        return new ResponseEntity<>(roundList, OK);
    }


    @Operation(summary = "해당 채널의 (1, 2, 3)라운드에 대한 매치 조회")
    @Parameters(value = {
            @Parameter(name = "channelLink", description = "해당 채널의 링크", example = "42aa1b11ab88"),
            @Parameter(name = "matchRound", description = "조회하고 싶은 매치의 라운드(1, 2, 3)", example = "1, 2, 3, 4")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "매치가 조회되었습니다. - 배열로 반환", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MatchRoundInfoDto.class))),
            @ApiResponse(responseCode = "403", description = "권한이 관리자가 아님,채널을 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/match/{channelLink}/{matchRound}")
    public ResponseEntity loadMatchInfo(@PathVariable("channelLink") String channelLink, @PathVariable("matchRound") Integer matchRound) {

        MatchRoundInfoDto matchInfoDtoList = matchQueryService.loadMatchPlayerList(channelLink, matchRound);

        return new ResponseEntity<>(matchInfoDtoList, OK);

    }

    @Operation(summary = "현재 진행중인 매치의 정보 조회.")
    @Parameters(value = {
            @Parameter(name = "channelLink", description = "해당 채널의 링크", example = "42aa1b11ab88"),
            @Parameter(name = "matchId", description = "조회 대상 matchId", example = "1")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "매치가 조회됨", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MatchScoreInfoDto.class))),
            @ApiResponse(responseCode = "404", description = "매치를 찾지 못함", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/channel/{channelLink}/match/{matchId}/player/info")
    public ResponseEntity loadMatchScore(@PathVariable("channelLink") String channelLink, @PathVariable("matchId") Long matchId) {

        MatchScoreInfoDto matchScoreInfoDto = matchQueryService.getMatchScoreInfo(channelLink, matchId);

        List<MatchMessage> matchMessages = matchChatService.findMatchChatHistory(channelLink, matchId);

        matchScoreInfoDto.setMatchMessages(matchMessages);

        return new ResponseEntity<>(matchScoreInfoDto, OK);
    }


    @Operation(summary = "해당 채널의 (1, 2, 3)라운드에 대한 설정된 경기 횟수를 반환")
    @Parameter(name = "roundCountList", description = "설정할려는 횟수 배열 결승전부터", example = "[3, 4, 2, 1]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "경기 횟수 반환"),
            @ApiResponse(responseCode = "403", description = "매치 또는 채널을 찾을 수 없습니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("match/{channelLink}/count")
    public ResponseEntity getMatchRoundCount(@PathVariable("channelLink") String channelLink) {

        MatchSetCountDto matchSetCountDto = matchQueryService.getMatchSetCount(channelLink);

        return new ResponseEntity(matchSetCountDto, OK);
    }

    @Operation(summary = "해당 채널 매치의 결과 - 이전 경기 결과를 가져옴 매치 세트 결과를 다 가져온다.")
    @Parameters(value = {
            @Parameter(name = "matchId", description = "불러오고 싶은 매치의 PK", example = "3"),
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "매치 결과를 리스트로 가져온다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameResultDto.class))),
            @ApiResponse(responseCode = "404", description = "매치 세트를 찾을 수 없습니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/match/{matchId}/result")
    public ResponseEntity getGameResult(@PathVariable Long matchId) {
        List<GameResultDto> gameResultList = matchQueryService.getGameResult(matchId);

        return new ResponseEntity(gameResultList, OK);
    }


}
