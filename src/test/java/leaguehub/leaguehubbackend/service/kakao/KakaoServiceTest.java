package leaguehub.leaguehubbackend.service.kakao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import leaguehub.leaguehubbackend.domain.member.dto.kakao.KakaoTokenResponseDto;
import leaguehub.leaguehubbackend.domain.member.dto.kakao.KakaoUserDto;
import leaguehub.leaguehubbackend.domain.member.repository.MemberRepository;
import leaguehub.leaguehubbackend.domain.member.service.JwtService;
import leaguehub.leaguehubbackend.domain.member.service.MemberAuthService;
import leaguehub.leaguehubbackend.domain.member.service.MemberService;
import leaguehub.leaguehubbackend.global.exception.global.exception.GlobalServerErrorException;
import leaguehub.leaguehubbackend.domain.member.exception.kakao.exception.KakaoInvalidCodeException;
import leaguehub.leaguehubbackend.fixture.KakaoTokenResponseDtoFixture;
import leaguehub.leaguehubbackend.fixture.KakaoUserDtoFixture;
import leaguehub.leaguehubbackend.global.redis.service.RedisService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional

@TestPropertySource(locations = "classpath:application-test.properties")
class KakaoServiceTest {

    MockWebServer mockWebServer;
    WebClient webClient;
    MemberAuthService memberAuthService;
    ObjectMapper objectMapper;

    @Mock
    MemberRepository memberRepository;
    @Mock
    MemberService memberService;
    @Mock
    RedisService redisService;
    @Mock
    JwtService jwtService;


    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        objectMapper = new ObjectMapper();
        webClient = WebClient.create(mockWebServer.url("/").toString());
        memberAuthService = new MemberAuthService(webClient, memberRepository, memberService, redisService, jwtService);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("유효한 카카오 코드일시 카카오 토큰을 받는다")
    void whenValidKakaoToken_thenGetKakaoToken() throws JsonProcessingException {
        KakaoTokenResponseDto mockResponse = KakaoTokenResponseDtoFixture.createKakaoTokenResponseDto();
        String mockResponseJson = objectMapper.writeValueAsString(mockResponse);
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseJson)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        KakaoTokenResponseDto actualResponse = memberAuthService.getKakaoToken("valid_code");

        assertEquals(mockResponse, actualResponse);
    }

    @Test
    @DisplayName("유효한 카카오 토큰일시 카카오 유저를 받는다")
    void whenValidAccessToken_thenGetUserInfo() throws JsonProcessingException {
        KakaoTokenResponseDto tokenResponseDto = KakaoTokenResponseDtoFixture.createKakaoTokenResponseDto();
        KakaoUserDto mockResponse = KakaoUserDtoFixture.createKakaoUserDto();
        String mockResponseJson = objectMapper.writeValueAsString(mockResponse);

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseJson)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        KakaoUserDto actualResponse = memberAuthService.getKakaoUser(tokenResponseDto);

        assertEquals(mockResponse, actualResponse);
    }

    @Test
    @DisplayName("잘못된 카카오 코드일시 KakaoInvalidCodeException Throw")
    void whenInvalidKakaoCode_thenThrowKakaoInvalidCodeException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        assertThrows(KakaoInvalidCodeException.class, () -> memberAuthService.getKakaoToken("invalid_code"));
    }

    @Test
    @DisplayName("카카오 서버에 문제가 있을 때 GlobalServerErrorException Throw")
    void whenKakaoServerProblem_thenThrowGlobalServerErrorException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        assertThrows(GlobalServerErrorException.class, () -> memberAuthService.getKakaoToken("valid_code"));
    }


}