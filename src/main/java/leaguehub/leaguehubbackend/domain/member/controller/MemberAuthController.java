package leaguehub.leaguehubbackend.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import leaguehub.leaguehubbackend.domain.member.dto.kakao.KakaoTokenResponseDto;
import leaguehub.leaguehubbackend.domain.member.dto.kakao.KakaoUserDto;
import leaguehub.leaguehubbackend.domain.member.dto.member.LoginMemberResponse;
import leaguehub.leaguehubbackend.domain.member.exception.kakao.exception.KakaoInvalidCodeException;
import leaguehub.leaguehubbackend.domain.member.service.JwtService;
import leaguehub.leaguehubbackend.domain.member.service.MemberAuthService;
import leaguehub.leaguehubbackend.domain.member.service.MemberService;
import leaguehub.leaguehubbackend.global.exception.global.ExceptionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.function.Predicate;

import static org.springframework.http.HttpStatus.OK;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MemberAuthController {

    private final JwtService jwtService;
    private final MemberAuthService kakaoService;
    private final MemberService memberService;
    private final MemberAuthService memberAuthService;

    @Operation(summary = "카카오 로그인/회원가입", description = "카카오 AccessCode를 사용하여 로그인/회원가입을 한다")
    @SecurityRequirements
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인/회원가입 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginMemberResponse.class))),
            @ApiResponse(responseCode = "400", description = "KA-C-001 유효하지 않은 카카오 코드입니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "500", description = "G-S-001 Internal Server Error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionResponse.class))),
    })
    @PostMapping("/member/oauth/kakao")
    public ResponseEntity handleKakaoLogin(@RequestHeader HttpHeaders headers, HttpServletResponse response) {
        String kakaoCode = headers.getFirst("Kakao-Code");

        Optional.ofNullable(kakaoCode)
                .filter(Predicate.not(String::isEmpty))
                .orElseThrow(KakaoInvalidCodeException::new);

        KakaoTokenResponseDto KakaoToken = kakaoService.getKakaoToken(kakaoCode);
        KakaoUserDto userDto = kakaoService.getKakaoUser(KakaoToken);
        LoginMemberResponse loginMemberResponse = memberAuthService.findOrSaveMember(userDto);

        Cookie refreshTokenCookie = new Cookie("refreshToken", loginMemberResponse.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setSecure(true);
        response.addCookie(refreshTokenCookie);
        response.setHeader("Authorization", "Bearer " + loginMemberResponse.getAccessToken());

        return new ResponseEntity("Login Successful", OK);
    }

    @Operation(summary = "앱 로그아웃", description = "앱에서 사용자를 로그아웃")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "MB-C-001 존재하지 않는 회원입니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionResponse.class))),
    })
    @PostMapping("/member/logout")
    public ResponseEntity<String> handleKakaoLogout(HttpServletRequest request, HttpServletResponse response) {

        memberAuthService.logoutMember(request, response);

        return ResponseEntity.ok("Logout Success!");
    }


    @Operation(summary = "토큰 재발급", description = "refreshToken을 사용해서 accessToken 과 refreshToken 재발급")
    @SecurityRequirements
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginMemberResponse.class))),
            @ApiResponse(responseCode = "400_1", description = "AT-C-004 요청에 토큰이 존재하지 않습니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "400_2", description = "AT-C-005 해당 리프레쉬 토큰을 가지는 멤버가 없습니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "AT-C-001 유효하지 않은 토큰입니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionResponse.class))),
    })
    @PostMapping("/member/token")
    public ResponseEntity<LoginMemberResponse> refreshAccessToken(HttpServletRequest request) {

        LoginMemberResponse loginMemberResponse = jwtService.refreshAccessToken(request);

        return ResponseEntity.ok(loginMemberResponse);
    }
}
