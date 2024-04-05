package leaguehub.leaguehubbackend.domain.match.service;

import leaguehub.leaguehubbackend.domain.channel.entity.Channel;
import leaguehub.leaguehubbackend.domain.channel.exception.exception.ChannelNotFoundException;
import leaguehub.leaguehubbackend.domain.match.dto.*;
import leaguehub.leaguehubbackend.domain.match.entity.Match;
import leaguehub.leaguehubbackend.domain.match.entity.MatchPlayer;
import leaguehub.leaguehubbackend.domain.match.entity.MatchSet;
import leaguehub.leaguehubbackend.domain.match.entity.MatchStatus;
import leaguehub.leaguehubbackend.domain.match.exception.exception.MatchNotFoundException;
import leaguehub.leaguehubbackend.domain.match.exception.exception.MatchResultIdNotFoundException;
import leaguehub.leaguehubbackend.domain.match.repository.MatchPlayerRepository;
import leaguehub.leaguehubbackend.domain.match.repository.MatchRepository;
import leaguehub.leaguehubbackend.domain.match.repository.MatchSetRepository;
import leaguehub.leaguehubbackend.domain.member.entity.Member;
import leaguehub.leaguehubbackend.domain.member.service.MemberAuthService;
import leaguehub.leaguehubbackend.domain.member.service.MemberService;
import leaguehub.leaguehubbackend.domain.participant.entity.Participant;
import leaguehub.leaguehubbackend.domain.participant.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static leaguehub.leaguehubbackend.domain.channel.entity.ChannelStatus.PROCEEDING;
import static leaguehub.leaguehubbackend.domain.match.entity.MatchStatus.END;
import static leaguehub.leaguehubbackend.domain.participant.entity.Role.PLAYER;
import static leaguehub.leaguehubbackend.global.audit.GlobalConstant.NO_DATA;

@Service
@RequiredArgsConstructor
public class MatchQueryService {

    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MemberService memberService;
    private final MemberAuthService memberAuthService;
    private final MatchSetRepository matchSetRepository;
    private final MatchService matchService;


    /**
     * 해당 채널의 매치 라운드를 보여줌(64, 32, 16, 8)
     *
     * @param channelLink
     * @return 2 4 8 16 32 64
     */
    @Transactional(readOnly = true)
    public MatchRoundListDto getRoundList(String channelLink) {
        Member member = memberService.findCurrentMember();
        Participant participant = matchService.getParticipant(member.getId(), channelLink);
        Channel findChannel = participant.getChannel();

        int maxPlayers = findChannel.getMaxPlayer();
        List<Integer> roundList = calculateRoundList(maxPlayers);

        MatchRoundListDto roundListDto = new MatchRoundListDto();
        roundListDto.setLiveRound(0);
        roundListDto.setRoundList(roundList);

        findLiveRound(channelLink, roundList, roundListDto);

        if (participant.getRole().equals(Role.HOST))
            roundListDto.setLiveRound(findChannel.getLiveRound());

        return roundListDto;
    }


    /**
     * 해당 채널의 참가한 플레이어 리스트를 반환
     *
     * @param channelLink
     * @param matchRound
     * @return
     */
    @Transactional(readOnly = true)
    public MatchRoundInfoDto loadMatchPlayerList(String channelLink, Integer matchRound) {
        Member member = memberService.findCurrentMember();
        Participant participant = matchService.getParticipant(member.getId(), channelLink);

        List<Match> matchList = matchService.findMatchList(channelLink, matchRound);

        List<MatchInfoDto> matchInfoDtoList = matchList.stream()
                .map(this::createMatchInfoDto)
                .collect(Collectors.toList());

        MatchRoundInfoDto matchRoundInfoDto = new MatchRoundInfoDto();

        findMyRoundName(participant, matchList, matchRoundInfoDto);

        matchRoundInfoDto.setMatchInfoDtoList(matchInfoDtoList);
        return matchRoundInfoDto;
    }

    /**
     * 현재 진행중인 라운드 표시(참가자 x -> 라운드 x)
     *
     * @param channelLink
     * @return
     */
    @Transactional(readOnly = true)
    public MyMatchDto getMyMatchRound(String channelLink) {
        Member member = memberService.findCurrentMember();
        Participant participant = matchService.getParticipant(member.getId(), channelLink);

        MyMatchDto myMatchDto = new MyMatchDto();

        myMatchDto.setMyMatchRound(0);
        myMatchDto.setMyMatchId(0L);

        findMyMatch(channelLink, participant, myMatchDto);

        return myMatchDto;
    }


    /**
     * 해당 매치의 경기 횟수 반환
     *
     * @param channelLink
     * @return
     */
    @Transactional(readOnly = true)
    public MatchSetCountDto getMatchSetCount(String channelLink) {

        List<Match> matchList = matchRepository.findAllByChannel_ChannelLinkOrderByMatchRoundDesc(channelLink);
        List<Integer> matchSetCountList = getMatchSetCountList(matchList);

        MatchSetCountDto matchSetCountDto = new MatchSetCountDto();
        matchSetCountDto.setMatchSetCountList(matchSetCountList);

        return matchSetCountDto;
    }

    /**
     * 해당 매치의 점수 정보 반환
     *
     * @param channelLink
     * @param matchId
     * @return
     */
    @Transactional(readOnly = true)
    public MatchScoreInfoDto getMatchScoreInfo(String channelLink, Long matchId) {
        List<MatchPlayer> matchPlayers = Optional.ofNullable(
                        matchPlayerRepository.findMatchPlayersAndMatchAndParticipantByMatchId(matchId))
                .filter(list -> !list.isEmpty())
                .orElseThrow(MatchNotFoundException::new);

        Match match = matchRepository.findById(matchId)
                .orElseThrow(MatchNotFoundException::new);

        List<MatchPlayerInfo> matchPlayerInfoList = matchService.convertMatchPlayerInfoList(matchPlayers);

        Long requestMatchPlayerId = getRequestMatchPlayerId(channelLink, matchPlayers);

        return MatchScoreInfoDto.builder()
                .matchPlayerInfos(matchPlayerInfoList)
                .matchRound(match.getMatchRound())
                .matchCurrentSet(match.getMatchCurrentSet())
                .matchSetCount(match.getMatchSetCount())
                .requestMatchPlayerId(requestMatchPlayerId)
                .build();
    }

    /**
     * 이전 경기의 결과를 보여줌
     * @param matchId
     * @return
     */
    @Transactional(readOnly = true)
    public List<GameResultDto> getGameResult(Long matchId) {
        List<MatchSet> matchSets = matchSetRepository.findMatchSetsByMatch_Id(matchId);
        if (matchSets.isEmpty()) throw new MatchResultIdNotFoundException();
        List<GameResultDto> gameResultDtoList = matchSets.stream().map(matchSet -> GameResultDto.builder()
                .matchSetCount(matchSet.getSetCount()).matchRankResultDtos(
                        matchSet.getMatchRankList().stream().map(matchRank -> new MatchRankResultDto(matchRank.getGameId(), matchRank.getPlacement()))
                                .collect(Collectors.toList())
                ).build()).collect(Collectors.toList());

        gameResultDtoList.sort(Comparator.comparing(GameResultDto::getMatchSetCount));

        return gameResultDtoList;
    }


    private List<Integer> calculateRoundList(int maxPlayers) {
        List<Integer> defaultroundList = Arrays.asList(0, 8, 16, 32, 64, 128, 256);

        int roundIndex = defaultroundList.indexOf(maxPlayers);

        if (roundIndex == -1) {
            throw new ChannelNotFoundException();// 에러 처리 시 빈 리스트 반환
        }

        return IntStream.rangeClosed(1, roundIndex)
                .boxed()
                .collect(Collectors.toList());
    }

    private void findLiveRound(String channelLink, List<Integer> roundList, MatchRoundListDto roundListDto) {
        roundList.forEach(round -> {
                    List<Match> matchList = matchService.findMatchList(channelLink, round);
                    matchList.stream()
                            .filter(match -> match.getMatchStatus().equals(MatchStatus.PROGRESS))
                            .findFirst()
                            .ifPresent(match -> roundListDto.setLiveRound(match.getMatchRound()));
                }
        );
    }

    private MatchInfoDto createMatchInfoDto(Match match) {
        MatchInfoDto matchInfoDto = new MatchInfoDto();
        matchInfoDto.setMatchName(match.getMatchName());
        matchInfoDto.setMatchId(match.getId());
        matchInfoDto.setMatchStatus(match.getMatchStatus());
        matchInfoDto.setMatchRound(match.getMatchRound());
        matchInfoDto.setMatchCurrentSet(match.getMatchCurrentSet());
        matchInfoDto.setMatchSetCount(match.getMatchSetCount());
        matchInfoDto.setAlarm(match.isAlarm());

        List<MatchPlayer> playerList = matchPlayerRepository.findAllByMatch_IdOrderByPlayerScoreDesc(match.getId());
        List<MatchPlayerInfo> matchPlayerInfoList = createMatchPlayerInfoList(playerList);
        matchInfoDto.setMatchPlayerInfoList(matchPlayerInfoList);

        return matchInfoDto;
    }


    private List<MatchPlayerInfo> createMatchPlayerInfoList(List<MatchPlayer> playerList) {
        List<MatchPlayerInfo> matchPlayerInfoList = playerList.stream()
                .map(matchPlayer -> {
                    MatchPlayerInfo matchPlayerInfo = new MatchPlayerInfo();
                    matchPlayerInfo.setMatchPlayerId(matchPlayer.getId());
                    matchPlayerInfo.setParticipantId(matchPlayer.getParticipant().getId());
                    matchPlayerInfo.setGameId(matchPlayer.getParticipant().getGameId());
                    matchPlayerInfo.setGameTier(matchPlayer.getParticipant().getGameTier());
                    matchPlayerInfo.setPlayerStatus(matchPlayer.getPlayerStatus());
                    matchPlayerInfo.setScore(matchPlayer.getPlayerScore());
                    matchPlayerInfo.setProfileSrc(matchPlayer.getParticipant().getProfileImageUrl());
                    return matchPlayerInfo;
                })
                .collect(Collectors.toList());

        return matchPlayerInfoList;

    }


    private void findMyRoundName(Participant participant, List<Match> matchList, MatchRoundInfoDto matchRoundInfoDto) {
        matchRoundInfoDto.setMyGameId(NO_DATA.getData());

        if (!participant.getGameId().equalsIgnoreCase(NO_DATA.getData())) {
            matchList.forEach(match -> {
                List<MatchPlayer> playerList = matchPlayerRepository.findAllByMatch_IdOrderByPlayerScoreDesc(match.getId());
                playerList.stream()
                        .filter(player -> participant.getGameId().equalsIgnoreCase(player.getParticipant().getGameId()))
                        .findFirst()
                        .ifPresent(player -> matchRoundInfoDto.setMyGameId(participant.getGameId()));
            });
        }
    }

    private void findMyMatch(String channelLink, Participant participant, MyMatchDto myMatchDto) {
        if (participant.getRole().equals(PLAYER)
                && participant.getChannel().getChannelStatus().equals(PROCEEDING)) {
            matchRepository.findAllByChannel_ChannelLink(channelLink).stream()
                    .filter(match -> !match.getMatchStatus().equals(END))
                    .flatMap(match -> getMatchPlayerList(match).stream())
                    .filter(matchPlayer -> isSameParticipant(matchPlayer, participant))
                    .findFirst()
                    .ifPresent(matchPlayer -> setMyMatchInfo(myMatchDto, matchPlayer.getMatch()));
        }
    }

    private List<MatchPlayer> getMatchPlayerList(Match match) {
        return matchPlayerRepository.findAllByMatch_IdOrderByPlayerScoreDesc(match.getId());
    }

    private boolean isSameParticipant(MatchPlayer matchPlayer, Participant participant) {
        return matchPlayer.getParticipant().getId().equals(participant.getId());
    }

    private void setMyMatchInfo(MyMatchDto mymatchDTO, Match match) {
        mymatchDTO.setMyMatchId(match.getId());
        mymatchDTO.setMyMatchRound(match.getMatchRound());
    }

    private static List<Integer> getMatchSetCountList(List<Match> matchList) {
        List<Integer> matchSetCountList = new ArrayList<>();
        int matchRound = 0;
        for (Match match : matchList) {
            if (matchRound == match.getMatchRound())
                continue;
            else {
                matchSetCountList.add(match.getMatchSetCount());
                matchRound = match.getMatchRound();
            }
        }
        return matchSetCountList;
    }


    private Long getRequestMatchPlayerId(String channelLink, List<MatchPlayer> matchPlayers) {
        if (memberAuthService.checkIfMemberIsAnonymous()) {
            return 0L;
        }
        Member member = memberService.findCurrentMember();
        Participant participant = matchService.getParticipant(member.getId(), channelLink);

        if (participant.getRole() == Role.HOST) {
            return -1L;
        }

        return findRequestMatchPlayerId(member, matchPlayers);
    }

    private Long findRequestMatchPlayerId(Member member, List<MatchPlayer> matchPlayers) {
        for (MatchPlayer mp : matchPlayers) {
            if (mp.getParticipant().getMember().getId().equals(member.getId())) {
                return mp.getId();
            }
        }
        return 0L;
    }

}
