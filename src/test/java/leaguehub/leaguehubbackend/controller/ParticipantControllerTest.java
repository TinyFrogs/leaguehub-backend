package leaguehub.leaguehubbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import leaguehub.leaguehubbackend.dto.channel.CreateChannelDto;
import leaguehub.leaguehubbackend.dto.participant.ParticipantResponseDto;
import leaguehub.leaguehubbackend.entity.channel.Channel;
import leaguehub.leaguehubbackend.entity.channel.ChannelBoard;
import leaguehub.leaguehubbackend.entity.member.Member;
import leaguehub.leaguehubbackend.entity.participant.Participant;
import leaguehub.leaguehubbackend.entity.participant.RequestStatus;
import leaguehub.leaguehubbackend.entity.participant.Role;
import leaguehub.leaguehubbackend.fixture.ChannelFixture;
import leaguehub.leaguehubbackend.fixture.ParticipantFixture;
import leaguehub.leaguehubbackend.fixture.UserFixture;
import leaguehub.leaguehubbackend.repository.channel.ChannelBoardRepository;
import leaguehub.leaguehubbackend.repository.channel.ChannelRepository;
import leaguehub.leaguehubbackend.repository.member.MemberRepository;
import leaguehub.leaguehubbackend.repository.particiapnt.ParticipantRepository;
import leaguehub.leaguehubbackend.service.participant.ParticipantService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
class ParticipantControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ParticipantController participantController;

    @Autowired
    ParticipantService participantService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    ChannelRepository channelRepository;

    @Autowired
    ChannelBoardRepository channelBoardRepository;

    @Autowired
    ParticipantRepository participantRepository;

    @Autowired
    ObjectMapper mapper;

    Channel createCustomChannel(Boolean tier, Boolean playCount, String tierMax, String gradeMax, int playCountMin) throws Exception{
        Member member = memberRepository.save(UserFixture.createMember());
        Member ironMember = memberRepository.save(UserFixture.createCustomeMember("썹맹구"));
        Member unrankedMember = memberRepository.save(UserFixture.createCustomeMember("서초임"));
        Member platinumMember = memberRepository.save(UserFixture.createCustomeMember("손성한"));
        Member masterMember = memberRepository.save(UserFixture.createCustomeMember("채수채수밭"));
        Member alreadyMember = memberRepository.save(UserFixture.createCustomeMember("요청한사람"));
        Member rejectedMember = memberRepository.save(UserFixture.createCustomeMember("거절된사람"));
        Member doneMember1 = memberRepository.save(UserFixture.createCustomeMember("참가된사람1"));
        Member doneMember2 = memberRepository.save(UserFixture.createCustomeMember("참가된사람2"));

        CreateChannelDto channelDto = ChannelFixture.createAllPropertiesCustomChannelDto(tier, playCount, tierMax, gradeMax, playCountMin);
        Channel channel = Channel.createChannel(channelDto.getTitle(),
                channelDto.getGame(), channelDto.getParticipationNum(),
                channelDto.getTournament(), channelDto.getChannelImageUrl(),
                channelDto.getTier(), channelDto.getTierMax(), channelDto.getGradeMax(),
                channelDto.getPlayCount(),
                channelDto.getPlayCountMin());
        channelRepository.save(channel);
        channelBoardRepository.saveAll(ChannelBoard.createDefaultBoard(channel));
        participantRepository.save(Participant.createHostChannel(member, channel));
        participantRepository.save(Participant.participateChannel(unrankedMember, channel));;
        participantRepository.save(Participant.participateChannel(ironMember, channel));
        participantRepository.save(Participant.participateChannel(platinumMember, channel));
        participantRepository.save(Participant.participateChannel(masterMember, channel));

        Participant alreadyParticipant = participantRepository.save(Participant.participateChannel(alreadyMember, channel));
        Participant rejectedParticipant = participantRepository.save(Participant.participateChannel(rejectedMember, channel));
        Participant doneParticipant1 = participantRepository.save(Participant.participateChannel(doneMember1, channel));
        Participant doneParticipant2 = participantRepository.save(Participant.participateChannel(doneMember2, channel));

        alreadyParticipant.updateParticipantStatus("bronze", "bronze");
        rejectedParticipant.rejectParticipantRequest();
        doneParticipant1.updateParticipantStatus("참가된사람1", "platinum");
        doneParticipant2.updateParticipantStatus("참가된사람2", "iron");
        doneParticipant1.approveParticipantMatch();
        doneParticipant2.approveParticipantMatch();

        return channel;
    }

    @Test
    @DisplayName("티어 조회 테스트 (참여 x) - 성공")
    void searchTierSuccessTest() throws Exception {

        mockMvc.perform(get("/api/stat?gameid=서초임&gamecategory=0"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.tier").value("UNRANKED"))
                .andExpect(jsonPath("$.grade").value("NONE"))
                .andExpect(jsonPath("$.playCount").value(0));

    }

    @Test
    @DisplayName("티어 조회 테스트 (참여 x) - 실패")
    void searchTierFailTest() throws Exception {

        mockMvc.perform(get("/api/stat?gameid=saovkovsk&gamecategory=0"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());

    }

    @Test
    @DisplayName("참여 여부 테스트 - 성공")
    void participateMatchSuccessTest() throws Exception {
        Channel channel = createCustomChannel(false, false, "Silver", "iv",100);
        UserFixture.setUpCustomAuth("서초임");
        mockMvc.perform(get(("/api/participant/") + channel.getChannelLink()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(print());

    }

    @Test
    @DisplayName("참여 여부 테스트 (관리자) - 실패")
    void participateMatchFailTest() throws Exception {
        Channel channel = createCustomChannel(false, false, "Silver", "iv",100);
        UserFixture.setUpCustomAuth("id");

        mockMvc.perform(get(("/api/participant/") + channel.getChannelLink()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 참가하였거나 경기 관리자입니다."));

    }

    @Test
    @DisplayName("해당 채널의 경기 참가 테스트 (티어, 판수 제한 x) - 성공")
    void participateDefaultMatchSuccessTest() throws Exception {

        Channel channel = createCustomChannel(false, false, "Silver", "iv",100);
        UserFixture.setUpCustomAuth("썹맹구");

        ParticipantResponseDto participantResponseDto = ParticipantFixture.createParticipantResponseDto(channel.getChannelLink(), "썹맹구");
        String dtoToJson = mapper.writeValueAsString(participantResponseDto);

        mockMvc.perform(post("/api/participant/match")
                .contentType(MediaType.APPLICATION_JSON)
                .content(dtoToJson))
                .andExpect(status().isOk());

    }

    @Test
    @DisplayName("해당 채널의 경기 참가 테스트 (판수 제한 o) - 성공")
    void participateLimitedPlayCountMatchSuccessTest() throws Exception {

        Channel channel = createCustomChannel(false, true, "Silver", "iv",20);
        UserFixture.setUpCustomAuth("손성한");

        ParticipantResponseDto participantResponseDto = ParticipantFixture.createParticipantResponseDto(channel.getChannelLink(), "손성한");
        String dtoToJson = mapper.writeValueAsString(participantResponseDto);

        mockMvc.perform(post("/api/participant/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dtoToJson))
                .andExpect(status().isOk());

    }

    @Test
    @DisplayName("해당 채널의 경기 참가 테스트 (마스터 10000점 이하, 판수 제한 o) - 성공")
    void participateLimitedMasterMatchSuccessTest() throws Exception {

        Channel channel = createCustomChannel(true, true, "master", "10000",20);
        UserFixture.setUpCustomAuth("채수채수밭");

        ParticipantResponseDto participantResponseDto = ParticipantFixture.createParticipantResponseDto(channel.getChannelLink(), "채수채수밭");
        String dtoToJson = mapper.writeValueAsString(participantResponseDto);

        mockMvc.perform(post("/api/participant/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dtoToJson))
                .andExpect(status().isOk());

    }
    
    
    @Test
    @DisplayName("해당 채널의 경기 참가 테스트 (중복) - 실패")
    public void participateDuplicatedMatchFailTest() throws Exception{
        //given
        Channel channel = createCustomChannel(false, false, "Silver", "iv",100);
        UserFixture.setUpCustomAuth("서초임");
        
        ParticipantResponseDto participantResponseDto = ParticipantFixture.createParticipantResponseDto(channel.getChannelLink(), "참가된사람1");
        String dtoToJson = mapper.writeValueAsString(participantResponseDto);

        mockMvc.perform(post("/api/participant/match")
                .contentType(MediaType.APPLICATION_JSON)
                .content(dtoToJson))
                .andExpect(status().isBadRequest());

    }


    @Test
    @DisplayName("해당 채널의 경기 참가 테스트 (참가된사람) - 실패")
    public void participantDoneMatchFailTest() throws Exception{
        //given
        Channel channel = createCustomChannel(true, false, "Silver", "iv",100);
        UserFixture.setUpCustomAuth("참가된사람1");

        ParticipantResponseDto participantResponseDto = ParticipantFixture.createParticipantResponseDto(channel.getChannelLink(), "참가된사람1");
        String dtoToJson = mapper.writeValueAsString(participantResponseDto);

        mockMvc.perform(post("/api/participant/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dtoToJson))
                .andExpect(status().isBadRequest());

    }

    @Test
    @DisplayName("해당 채널의 경기 참가 테스트 (이미참가요청한사람) - 실패")
    public void participantAlreadyMatchFailTest() throws Exception{
        //given
        Channel channel = createCustomChannel(true, false, "Silver", "iv",100);
        UserFixture.setUpCustomAuth("요청한사람");

        ParticipantResponseDto participantResponseDto = ParticipantFixture.createParticipantResponseDto(channel.getChannelLink(), "요청한사람");
        String dtoToJson = mapper.writeValueAsString(participantResponseDto);

        mockMvc.perform(post("/api/participant/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dtoToJson))
                .andExpect(status().isBadRequest());

    }

    @Test
    @DisplayName("해당 채널의 경기 참가 테스트 (거절된사람) - 실패")
    public void participantRejectedMatchFailTest() throws Exception{
        //given
        Channel channel = createCustomChannel(true, false, "Silver", "iv",100);
        UserFixture.setUpCustomAuth("거절된사람");

        ParticipantResponseDto participantResponseDto = ParticipantFixture.createParticipantResponseDto(channel.getChannelLink(), "거절된사람");
        String dtoToJson = mapper.writeValueAsString(participantResponseDto);

        mockMvc.perform(post("/api/participant/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dtoToJson))
                .andExpect(status().isBadRequest());

    }

    @Test
    @DisplayName("해당 채널의 경기 참가 테스트 (티어 제한) - 실패")
    void participateLimitedTierMatchFailTest() throws Exception {

        Channel channel = createCustomChannel(true, false, "Silver", "iv",20);
        UserFixture.setUpCustomAuth("손성한");

        ParticipantResponseDto participantResponseDto = ParticipantFixture.createParticipantResponseDto(channel.getChannelLink(), "손성한");
        String dtoToJson = mapper.writeValueAsString(participantResponseDto);

        mockMvc.perform(post("/api/participant/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dtoToJson))
                .andExpect(status().isBadRequest());

    }

    @Test
    @DisplayName("해당 채널의 경기 참가 테스트 (횟수 제한) - 실패")
    void participateLimitedPlayCountMatchFailTest() throws Exception {

        Channel channel = createCustomChannel(true, true, "Silver", "iv",100);
        UserFixture.setUpCustomAuth("서초임");

        ParticipantResponseDto participantResponseDto = ParticipantFixture.createParticipantResponseDto(channel.getChannelLink(), "서초임");
        String dtoToJson = mapper.writeValueAsString(participantResponseDto);

        mockMvc.perform(post("/api/participant/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dtoToJson))
                .andExpect(status().isBadRequest());

    }

    @Test
    @DisplayName("해당 채널의 경기 참가 테스트 (마스터 100점 이하, 횟수 제한) - 실패")
    void participateLimitedMasterMatchFailTest() throws Exception {

        Channel channel = createCustomChannel(true, true, "master", "100",20);
        UserFixture.setUpCustomAuth("채수채수밭");

        ParticipantResponseDto participantResponseDto = ParticipantFixture.createParticipantResponseDto(channel.getChannelLink(), "채수채수밭");
        String dtoToJson = mapper.writeValueAsString(participantResponseDto);

        mockMvc.perform(post("/api/participant/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dtoToJson))
                .andExpect(status().isBadRequest());

    }

    @Test
    @DisplayName("채널 경기 참여자 조회 테스트")
    void loadPlayerTest() throws Exception {

        Channel channel = createCustomChannel(false, false, "master", "100",20);
        UserFixture.setUpCustomAuth("id");

        mockMvc.perform(get("/api/profile/player?channelLink=" + channel.getChannelLink()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("[0].nickname").value("참가된사람1"))
                .andExpect(jsonPath("[0].gameId").value("참가된사람1"))
                .andExpect(jsonPath("[1].nickname").value("참가된사람2"))
                .andExpect(jsonPath("[1].gameId").value("참가된사람2"))
                .andDo(print());

    }


    @Test
    @DisplayName("요청된사람 조회 테스트 (관리자 x) - 실패")
    public void loadRequestStatusPlayerListFailTest() throws Exception{
        //given
        Channel channel = createCustomChannel(false, false, "master", "100",20);
        UserFixture.setUpCustomAuth("참가된사람1");

        mockMvc.perform(get("/api/profile/request?channelLink=" + channel.getChannelLink()))
                .andExpect(status().isBadRequest());

    }

    @Test
    @DisplayName("요청된사람 조회 테스트 (관리자 o) - 성공")
    public void loadRequestStatusPlayerListSuccessTest() throws Exception{
        //given
        Channel channel = createCustomChannel(false, false, "master", "100",20);
        UserFixture.setUpCustomAuth("id");

        mockMvc.perform(get("/api/profile/request?channelLink=" + channel.getChannelLink()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("[0].nickname").value("요청한사람"))
                .andExpect(jsonPath("[0].gameId").value("bronze"))
                .andExpect(jsonPath("[0].tier").value("bronze"));

    }


    @Test
    @DisplayName("요청한사람 승인 테스트 (관리자 o) - 성공")
    public void approveParticipantSuccessTest() throws Exception{
        //given
        Channel channel = createCustomChannel(false, false, "master", "100",20);
        UserFixture.setUpCustomAuth("id");

        Member dummyMember1 = memberRepository.save(UserFixture.createCustomeMember("더미1"));
        Participant dummy1 = participantRepository.save(Participant.participateChannel(dummyMember1, channel));
        dummy1.updateParticipantStatus("더미1", "platinum");
        //when

        mockMvc.perform(post("/api/player/approve/" + channel.getChannelLink() + "/" + dummy1.getId()))
                .andExpect(status().isOk());

    }

    @Test
    @DisplayName("요청한사람 승인 테스트 (관리자 o) - 실패")
    public void approveParticipantFailTest() throws Exception{
        //given
        Channel channel = createCustomChannel(false, false, "master", "100",20);
        UserFixture.setUpCustomAuth("참가된사람1");

        Member dummyMember1 = memberRepository.save(UserFixture.createCustomeMember("더미1"));
        Participant dummy1 = participantRepository.save(Participant.participateChannel(dummyMember1, channel));
        dummy1.updateParticipantStatus("더미1", "platinum");

        mockMvc.perform(post("/api/player/approve/" + channel.getChannelLink() + "/" + dummy1.getId()))
                .andExpect(status().isBadRequest());

    }

    @Test
    @DisplayName("요청한사람 거절 테스트 (관리자 o) - 성공")
    public void rejectedParticipantSuccessTest() throws Exception{
        //given
        Channel channel = createCustomChannel(false, false, "master", "100",20);
        UserFixture.setUpCustomAuth("id");

        Member dummyMember1 = memberRepository.save(UserFixture.createCustomeMember("더미1"));
        Participant dummy1 = participantRepository.save(Participant.participateChannel(dummyMember1, channel));
        dummy1.updateParticipantStatus("더미1", "platinum");

        mockMvc.perform(post("/api/player/reject/" + channel.getChannelLink() + "/" + dummy1.getId()))
                .andExpect(status().isOk());
        
    }

    @Test
    @DisplayName("요청한사람 거절 테스트 (관리자 x) - 실패")
    public void rejectedParticipantFailTest() throws Exception{
        //given
        Channel channel = createCustomChannel(false, false, "master", "100",20);
        UserFixture.setUpCustomAuth("참가된사람1");

        Member dummyMember1 = memberRepository.save(UserFixture.createCustomeMember("더미1"));
        Participant dummy1 = participantRepository.save(Participant.participateChannel(dummyMember1, channel));
        dummy1.updateParticipantStatus("더미1", "platinum");

        mockMvc.perform(post("/api/player/reject/" + channel.getChannelLink() + "/" + dummy1.getId()))
                .andExpect(status().isBadRequest());

    }



}