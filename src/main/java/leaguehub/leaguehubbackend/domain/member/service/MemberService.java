package leaguehub.leaguehubbackend.domain.member.service;

import jakarta.transaction.Transactional;
import leaguehub.leaguehubbackend.domain.member.dto.kakao.KakaoUserDto;
import leaguehub.leaguehubbackend.domain.member.entity.Member;
import leaguehub.leaguehubbackend.domain.member.exception.member.exception.MemberNotFoundException;
import leaguehub.leaguehubbackend.domain.member.repository.MemberRepository;
import leaguehub.leaguehubbackend.global.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;


    //PersonalId를 이용하여 Member 추출
    public Optional<Member> findMemberByPersonalId(String personalId) {
        return memberRepository.findMemberByPersonalId(personalId);
    }

    //Member Repository에 엔티티 저장(회원가입)
    @Transactional
    public Optional<Member> saveMember(KakaoUserDto kakaoUserDto) {
        Member newUser = Member.kakaoUserToMember(kakaoUserDto);
        memberRepository.save(newUser);
        return Optional.of(newUser);
    }

    //존재하는 Member인지 확인
    public Member validateMember(String personalId) {
        Member member = memberRepository.findMemberByPersonalId(personalId)
                .orElseThrow(MemberNotFoundException::new);
        return member;
    }

    //Email이 인증되었는지 확인
    public String getVerifiedEmail(Member member) {
        if (member.getEmailAuth() != null && member.isEmailUserVerified()) {
            return member.getEmailAuth().getEmail();
        }
        return "N/A";
    }


    //자신의 Member 추출
    public Member findCurrentMember() {
        UserDetails userDetails = SecurityUtils.getAuthenticatedUser();

        return validateMember(userDetails.getUsername());
    }

}

