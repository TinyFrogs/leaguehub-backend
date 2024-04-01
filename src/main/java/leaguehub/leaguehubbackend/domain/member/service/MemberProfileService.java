package leaguehub.leaguehubbackend.domain.member.service;

import jakarta.transaction.Transactional;
import leaguehub.leaguehubbackend.domain.member.dto.member.MypageResponseDto;
import leaguehub.leaguehubbackend.domain.member.dto.member.NicknameRequestDto;
import leaguehub.leaguehubbackend.domain.member.dto.member.ProfileDto;
import leaguehub.leaguehubbackend.domain.member.entity.Member;
import leaguehub.leaguehubbackend.domain.participant.entity.Participant;
import leaguehub.leaguehubbackend.domain.participant.exception.exception.ParticipantNotFoundException;
import leaguehub.leaguehubbackend.domain.participant.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberProfileService {


    private final MemberService memberService;
    private final ParticipantRepository participantRepository;


    //Member profile 조회
    public ProfileDto getProfile() {

        Member member = memberService.findCurrentMember();

        return ProfileDto.builder()
                .profileImageUrl(member.getProfileImageUrl())
                .nickName(member.getNickname())
                .build();
    }

    //자기 자신의 profile 조회
    public MypageResponseDto getMypageProfile() {

        Member member = memberService.findCurrentMember();

        return MypageResponseDto.builder()
                .profileImageUrl(member.getProfileImageUrl())
                .nickName(member.getNickname())
                .email(memberService.getVerifiedEmail(member))
                .userEmailVerified(member.isEmailUserVerified())
                .build();
    }

    //Member 닉네임 변경
    @Transactional
    public ProfileDto changeMemberParticipantNickname(NicknameRequestDto nicknameRequestDto) {

        Member member = memberService.findCurrentMember();

        member.updateNickname(nicknameRequestDto.getNickName());

        List<Participant> participants = participantRepository.findAllByMemberId(member.getId());
        if (participants.isEmpty()) {
            throw new ParticipantNotFoundException();
        }

        participants.forEach(participant -> participant.updateNickname(member.getNickname()));

        return getProfile();
    }
}
