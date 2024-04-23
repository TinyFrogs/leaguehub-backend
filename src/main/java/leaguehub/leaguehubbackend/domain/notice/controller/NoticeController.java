package leaguehub.leaguehubbackend.domain.notice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import leaguehub.leaguehubbackend.domain.notice.dto.NoticeDto;
import leaguehub.leaguehubbackend.domain.notice.entity.GameType;
import leaguehub.leaguehubbackend.domain.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.OK;

@Tag(name = "Notice-Controller", description = "공지사항 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "DB에 저장된 공지사항 추출", description = "DB에 저장된 공지사항들을 가져온다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "요청하는 게임의 공지사항 가져오기", content = @Content(mediaType = "application/json", schema = @Schema(implementation = NoticeDto.class))),
    })
    @GetMapping("/notice/{target}")
    public ResponseEntity findTargetNotice(@PathVariable("target") String target) {
        return new ResponseEntity<>(noticeService.getNotices(target), OK);
    }


    @Operation(summary = "게임사 공지사항 가져오기 - 수동", description = "원하는 게임사의 공지사항을 업데이트 -수동")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
    })
    @PostMapping("/notice/new/{target}")
    public ResponseEntity updateNewNotice(@PathVariable("target") String target) {

        noticeService.updateNotices(target);

        return new ResponseEntity<>(target + "update Success", OK);
    }


}