package leaguehub.leaguehubbackend.domain.member.service;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import leaguehub.leaguehubbackend.domain.member.dto.kakao.KakaoTokenRequestDto;
import leaguehub.leaguehubbackend.domain.member.dto.kakao.KakaoTokenResponseDto;
import leaguehub.leaguehubbackend.domain.member.dto.kakao.KakaoUserDto;
import leaguehub.leaguehubbackend.domain.member.dto.member.LoginMemberResponse;
import leaguehub.leaguehubbackend.domain.member.entity.BaseRole;
import leaguehub.leaguehubbackend.domain.member.entity.Member;
import leaguehub.leaguehubbackend.domain.member.exception.kakao.exception.KakaoInvalidCodeException;
import leaguehub.leaguehubbackend.domain.member.repository.MemberRepository;
import leaguehub.leaguehubbackend.global.exception.global.exception.GlobalServerErrorException;
import leaguehub.leaguehubbackend.global.redis.service.RedisService;
import leaguehub.leaguehubbackend.global.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class MemberAuthService {

    @Value("${KAKAO_CLIENT_ID}")
    private String kakaoClientId;

    @Value("${KAKAO_REDIRECT_URI}")
    private String kakaoRedirectUri;

    @Value("${KAKAO_TOKEN_REQUEST_URI}")
    private String kakaoToeknRequestUri;

    @Value("${KAKAO_USERINFO_REQUEST_URI}")
    private String kakaoUserInfoRequestUri;

    private final WebClient webClient;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final RedisService redisService;
    private final JwtService jwtService;

    /**
     * 카카오로 부터 토큰을 받는 함수
     */
    public KakaoTokenResponseDto getKakaoToken(String kakaoCode) {

        KakaoTokenRequestDto kakaoTokenRequestDto = new KakaoTokenRequestDto("authorization_code", kakaoClientId, kakaoRedirectUri, kakaoCode);
        MultiValueMap<String, String> params = kakaoTokenRequestDto.toMultiValueMap();

        return webClient.post()
                .uri(kakaoToeknRequestUri)
                .body(BodyInserters.fromFormData(params))
                .header("Content-type", "application/x-www-form-urlencoded;charset=utf-8")
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> Mono.error(new KakaoInvalidCodeException()))
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(new GlobalServerErrorException()))
                .bodyToMono(KakaoTokenResponseDto.class)
                .block();

    }

    /**
     * 카카오로 부터 유저 정보를 받는 함수
     */
    public KakaoUserDto getKakaoUser(KakaoTokenResponseDto token) {

        return webClient.get()
                .uri(kakaoUserInfoRequestUri)
                .header("Content-type", "application/x-www-form-urlencoded;charset=utf-8")
                .header("Authorization", "Bearer " + token.getAccessToken())
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(new GlobalServerErrorException()))
                .bodyToMono(KakaoUserDto.class)
                .block();

    }


    //Member Logout 메서드
    public void logoutMember(HttpServletRequest request, HttpServletResponse response) {

        Member member = memberService.findCurrentMember();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
            redisService.deleteRefreshToken(member.getPersonalId());
            SecurityContextHolder.clearContext();
            memberRepository.save(member);
        }
    }

    //멤버를 찾거나 저장
    @Transactional
    public LoginMemberResponse findOrSaveMember(KakaoUserDto kakaoUserDto) {
        Member member = memberRepository.findMemberByPersonalId(String.valueOf(kakaoUserDto.getId()))
                .map(existingMember -> updateProfileUrl(existingMember, kakaoUserDto))
                .orElseGet(() -> memberService.saveMember(kakaoUserDto).orElseThrow(GlobalServerErrorException::new));
        return createLoginResponse(member);
    }


    //로그인 반응 생성
    public LoginMemberResponse createLoginResponse(Member member) {
        LoginMemberResponse loginMemberResponse = jwtService.createTokens(String.valueOf(member.getPersonalId()));

        jwtService.updateRefreshToken(member.getPersonalId(), loginMemberResponse.getRefreshToken());

        loginMemberResponse.setVerifiedUser(member.getBaseRole() != BaseRole.GUEST);
        return loginMemberResponse;
    }

    //익명의 로그인 반응 생성
    public Boolean checkIfMemberIsAnonymous() {
        UserDetails userDetails = SecurityUtils.getAuthenticatedUser();
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        for (GrantedAuthority authority : authorities) {
            if ("ROLE_ANONYMOUS".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    //프로필 이미지 변경
    private Member updateProfileUrl(Member member, KakaoUserDto kakaoUserDto) {
        member.updateProfileImageUrl(kakaoUserDto.getKakaoAccount().getProfile().getThumbnailImageUrl());
        return member;
    }
}
