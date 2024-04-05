package leaguehub.leaguehubbackend.domain.match.service;

import leaguehub.leaguehubbackend.domain.channel.entity.Channel;
import leaguehub.leaguehubbackend.domain.channel.exception.exception.ChannelRequestException;
import leaguehub.leaguehubbackend.domain.channel.exception.exception.ChannelStatusAlreadyException;
import leaguehub.leaguehubbackend.domain.match.dto.MatchCallAdminDto;
import leaguehub.leaguehubbackend.domain.match.dto.MatchInfoDto;
import leaguehub.leaguehubbackend.domain.match.dto.MatchPlayerInfo;
import leaguehub.leaguehubbackend.domain.match.entity.Match;
import leaguehub.leaguehubbackend.domain.match.entity.MatchPlayer;
import leaguehub.leaguehubbackend.domain.match.entity.MatchSet;
import leaguehub.leaguehubbackend.domain.match.entity.MatchStatus;
import leaguehub.leaguehubbackend.domain.match.exception.exception.MatchNotEnoughPlayerException;
import leaguehub.leaguehubbackend.domain.match.exception.exception.MatchNotFoundException;
import leaguehub.leaguehubbackend.domain.match.repository.MatchPlayerRepository;
import leaguehub.leaguehubbackend.domain.match.repository.MatchRepository;
import leaguehub.leaguehubbackend.domain.match.repository.MatchSetRepository;
import leaguehub.leaguehubbackend.domain.member.entity.Member;
import leaguehub.leaguehubbackend.domain.member.service.MemberService;
import leaguehub.leaguehubbackend.domain.participant.entity.Participant;
import leaguehub.leaguehubbackend.domain.participant.entity.Role;
import leaguehub.leaguehubbackend.domain.participant.exception.exception.InvalidParticipantAuthException;
import leaguehub.leaguehubbackend.domain.participant.exception.exception.ParticipantNotFoundException;
import leaguehub.leaguehubbackend.domain.participant.exception.exception.ParticipantRejectedRequestedException;
import leaguehub.leaguehubbackend.domain.participant.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static leaguehub.leaguehubbackend.domain.channel.entity.ChannelStatus.PROCEEDING;
import static leaguehub.leaguehubbackend.domain.match.entity.MatchStatus.END;
import static leaguehub.leaguehubbackend.domain.participant.entity.ParticipantStatus.DISQUALIFICATION;
import static leaguehub.leaguehubbackend.domain.participant.entity.ParticipantStatus.PROGRESS;
import static leaguehub.leaguehubbackend.domain.participant.entity.Role.PLAYER;

@Service
@Transactional
@RequiredArgsConstructor
public class MatchService {

    private static final int MIN_PLAYERS_FOR_SUB_MATCH = 8;
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final ParticipantRepository participantRepository;
    private final MatchSetRepository matchSetRepository;
    private final MemberService memberService;
    private static final int INITIAL_RANK = 1;


    /**
     * 채널을 만들 때 빈 값인 매치를 만듦
     *
     * @param channel
     * @param maxPlayers
     */
    public void createSubMatches(Channel channel, int maxPlayers) {
        int currentPlayers = maxPlayers;
        int matchRoundIndex = 1;

        while (currentPlayers >= MIN_PLAYERS_FOR_SUB_MATCH) {
            currentPlayers = createSubMatchesForRound(channel, currentPlayers, matchRoundIndex);
            matchRoundIndex++;
        }
    }


    /**
     * 경기 배정
     *
     * @param channelLink
     * @param matchRound
     */
    public void matchAssignment(String channelLink, Integer matchRound) {
        Participant participant = checkHost(channelLink);

        if (!participant.getChannel().getChannelStatus().equals(PROCEEDING)) {
            throw new ChannelRequestException();
        }

        List<Match> matchList = findMatchList(channelLink, matchRound);


        if (matchRound != 1)
            checkUpdateScore(matchList);

        checkPreviousMatchEnd(channelLink, matchRound);

        List<Participant> playerList = getParticipantList(channelLink, matchRound);

        assignSubMatches(matchList, playerList);
        participant.getChannel().updateChannelLiveRound(matchRound);
    }


    public void setMatchSetCount(String channelLink, List<Integer> roundCount) {
        Participant participant = checkHost(channelLink);

        checkChannelProceeding(participant);

        List<Match> findMatchList = matchRepository.findAllByChannel_ChannelLink(channelLink);

        if (findMatchList.isEmpty())
            throw new MatchNotFoundException();

        updateMatchSetCount(roundCount, findMatchList);
    }

    public void processMatchSet(String channelLink) {
        List<Match> matchList = matchRepository.findAllByChannel_ChannelLink(channelLink);

        createMatchSet(matchList);
    }

    public MatchCallAdminDto callAdmin(String channelLink, Long matchId, Long participantId) {
        Participant participant = participantRepository.findParticipantByIdAndChannel_ChannelLink(participantId, channelLink)
                .orElseThrow(() -> new ParticipantNotFoundException());

        if (!participant.getParticipantStatus().equals(PROGRESS)) {
            throw new ParticipantRejectedRequestedException();
        }

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException());

        match.updateCallAlarm();

        MatchCallAdminDto matchCallAdminDto = new MatchCallAdminDto();
        matchCallAdminDto.setCallName(participant.getNickname());
        matchCallAdminDto.setMatchRound(match.getMatchRound());
        matchCallAdminDto.setMatchName(match.getMatchName());

        return matchCallAdminDto;
    }

    public void turnOffAlarm(String channelLink, Long matchId) {
        Participant participant = checkHost(channelLink);

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException());

        match.updateOffAlarm();
    }


    private int createSubMatchesForRound(Channel channel, int maxPlayers, int matchRoundIndex) {
        int currentPlayers = maxPlayers;
        int tableCount = currentPlayers / MIN_PLAYERS_FOR_SUB_MATCH;

        for (int tableIndex = 1; tableIndex <= tableCount; tableIndex++) {
            String groupName = "Group " + (char) (64 + tableIndex);
            Match match = Match.createMatch(matchRoundIndex, channel, groupName);
            matchRepository.save(match);
        }

        return currentPlayers / 2;
    }

    public Participant getParticipant(Long memberId, String channelLink) {
        Participant participant = participantRepository.findParticipantByMemberIdAndChannel_ChannelLink(memberId, channelLink)
                .orElseThrow(() -> new InvalidParticipantAuthException());
        return participant;
    }

    private void checkRoleHost(Role role) {
        if (role != Role.HOST) {
            throw new InvalidParticipantAuthException();
        }
    }

    public List<Match> findMatchList(String channelLink, Integer matchRound) {
        List<Match> matchList = matchRepository.findAllByChannel_ChannelLinkAndMatchRoundOrderByMatchName(channelLink, matchRound);
        return matchList;
    }

    private List<Participant> getParticipantList(String channelLink, Integer matchRound) {
        List<Participant> playerList = participantRepository
                .findAllByChannel_ChannelLinkAndRoleAndParticipantStatus(channelLink, PLAYER, PROGRESS);

        if (playerList.size() < matchRound * 0.75) throw new MatchNotEnoughPlayerException();
        return playerList;
    }

    private void assignSubMatches(List<Match> matchList, List<Participant> playerList) {
        Collections.shuffle(playerList);

        int totalPlayers = playerList.size();
        int matchCount = matchList.size();
        int playersPerMatch = totalPlayers / matchCount;
        int remainingPlayers = totalPlayers % matchCount;
        int playerIndex = 0;

        for (Match match : matchList) {
            int currentPlayerCount = playersPerMatch + (remainingPlayers > 0 ? 1 : 0);

            for (int i = 0; i < currentPlayerCount; i++) {
                Participant player = playerList.get(playerIndex);
                MatchPlayer matchPlayer = MatchPlayer.createMatchPlayer(player, match);
                matchPlayerRepository.save(matchPlayer);

                playerIndex++;
                remainingPlayers--;
            }

            match.updateMatchStatus(MatchStatus.PROGRESS);
        }
    }


    public MatchInfoDto convertMatchInfoDto(Match match, List<MatchPlayer> matchPlayers) {
        return MatchInfoDto.builder().matchId(match.getId())
                .matchName(match.getMatchName())
                .matchStatus(match.getMatchStatus())
                .matchRound(match.getMatchRound())
                .matchSetCount(match.getMatchSetCount())
                .matchCurrentSet(match.getMatchCurrentSet())
                .matchPlayerInfoList(convertMatchPlayerInfoList(matchPlayers))
                .matchAlarm(match.isAlarm())
                .build();
    }


    public List<MatchPlayerInfo> convertMatchPlayerInfoList(List<MatchPlayer> matchPlayers) {
        List<MatchPlayerInfo> matchPlayerInfoList = matchPlayers.stream()
                .map(matchPlayer -> new MatchPlayerInfo(
                        matchPlayer.getId(),
                        matchPlayer.getParticipant().getId(),
                        matchPlayer.getParticipant().getGameId(),
                        matchPlayer.getParticipant().getGameTier(),
                        matchPlayer.getPlayerStatus(),
                        matchPlayer.getPlayerScore(),
                        matchPlayer.getMatchPlayerResultStatus(),
                        matchPlayer.getParticipant().getProfileImageUrl(),
                        matchPlayer.getPlayerScore()
                ))
                .sorted(Comparator.comparingInt(MatchPlayerInfo::getScore).reversed()
                        .thenComparing(MatchPlayerInfo::getGameId))
                .collect(Collectors.toList());

        assignRankToMatchPlayerInfoList(matchPlayerInfoList);

        return matchPlayerInfoList;
    }

    private Participant checkHost(String channelLink) {
        Member member = memberService.findCurrentMember();
        Participant participant = getParticipant(member.getId(), channelLink);
        checkRoleHost(participant.getRole());

        return participant;
    }

    private void checkUpdateScore(List<Match> matchList) {
        for (Match currentMatch : matchList) {
            List<MatchPlayer> matchplayerList = matchPlayerRepository.findAllByMatch_IdOrderByPlayerScoreDesc(currentMatch.getId());

            int progressCount = 0;

            for (MatchPlayer matchPlayer : matchplayerList) {
                if (progressCount >= 5) {
                    if (!matchPlayer.getParticipant().getParticipantStatus().equals(DISQUALIFICATION)) {
                        matchPlayer.getParticipant().dropoutParticipantStatus();
                    }
                    continue;
                }

                if (matchPlayer.getParticipant().getParticipantStatus().equals(PROGRESS)) {
                    progressCount++;
                } else {
                    matchPlayer.getParticipant().dropoutParticipantStatus();
                }
            }
        }
    }

    private void checkPreviousMatchEnd(String channelLink, Integer matchRound) {
        if (matchRound != 1) {
            List<Match> previousMatch = findMatchList(channelLink, matchRound - 1);
            previousMatch.stream()
                    .filter(match -> !match.getMatchStatus().equals(END))
                    .findAny()
                    .ifPresent(match -> {
                        throw new MatchNotFoundException();
                    });

            List<Match> presentMatch = findMatchList(channelLink, matchRound);
            presentMatch.stream()
                    .filter(match -> match.getMatchStatus().equals(MatchStatus.PROGRESS))
                    .findAny()
                    .ifPresent(match -> {
                        throw new MatchNotFoundException();
                    });
        }
    }


    private void assignRankToMatchPlayerInfoList(List<MatchPlayerInfo> matchPlayerInfoList) {
        int rank = INITIAL_RANK;
        for (int i = 0; i < matchPlayerInfoList.size(); i++) {
            MatchPlayerInfo info = matchPlayerInfoList.get(i);
            if (i > 0 && !info.getScore().equals(matchPlayerInfoList.get(i - 1).getScore())) {
                rank = i + 1;
            }
            info.setMatchRank(rank);
        }
    }


    private static void updateMatchSetCount(List<Integer> roundCount, List<Match> findMatchList) {
        int responseIndex = 0;
        for (int i = roundCount.size(); i >= 1; i--) {
            for (Match match : findMatchList) {
                if (match.getMatchRound().equals(i))
                    match.updateMatchSetCount(roundCount.get(responseIndex));
            }
            responseIndex++;
        }

    }

    private static void checkChannelProceeding(Participant participant) {
        if (participant.getChannel().getChannelStatus().equals(PROCEEDING))
            throw new ChannelStatusAlreadyException();
    }

    private void createMatchSet(List<Match> matchList) {
        matchList.stream()
                .flatMap(match -> IntStream.rangeClosed(1, match.getMatchSetCount())
                        .mapToObj(setCount -> MatchSet.createMatchSet(match, setCount)))
                .forEach(matchSetRepository::save);
    }

}