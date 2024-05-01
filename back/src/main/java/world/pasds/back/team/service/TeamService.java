package world.pasds.back.team.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.pasds.back.common.dto.KmsKeyDto;
import world.pasds.back.common.dto.KmsReGenerationKeysResponseDto;
import world.pasds.back.common.exception.BusinessException;
import world.pasds.back.common.exception.ExceptionCode;
import world.pasds.back.common.service.KeyService;
import world.pasds.back.invitaion.service.InvitationService;
import world.pasds.back.member.entity.Member;
import world.pasds.back.member.entity.MemberOrganization;
import world.pasds.back.member.entity.MemberTeam;
import world.pasds.back.member.repository.MemberOrganizationRepository;
import world.pasds.back.member.repository.MemberRepository;
import world.pasds.back.member.repository.MemberTeamRepository;
import world.pasds.back.organization.entity.Organization;
import world.pasds.back.organization.repository.OrganizationRepository;
import world.pasds.back.privateData.entity.PrivateData;
import world.pasds.back.team.entity.Team;
import world.pasds.back.team.entity.dto.request.*;
import world.pasds.back.team.entity.dto.response.GetTeamsResponseDto;
import world.pasds.back.team.repository.PrivateDataRepository;
import world.pasds.back.team.repository.TeamRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final MemberRepository memberRepository;
    private final MemberTeamRepository memberTeamRepository;
    private final MemberOrganizationRepository memberOrganizationRepository;
    private final TeamRepository teamRepository;
    private final OrganizationRepository organizationRepository;
    private final InvitationService invitationService;
    private final KeyService keyService;
    private final PrivateDataRepository privateDataRepository;
    @Transactional
    public List<GetTeamsResponseDto> getTeams(GetTeamsRequestDto requestDto, Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ExceptionCode.MEMBER_NOT_FOUND));
        Organization organization = organizationRepository.findById(requestDto.getOrganizationId()).orElseThrow(() -> new BusinessException(ExceptionCode.ORGANIZATION_NOT_FOUND));
        List<Team> findTeamList = teamRepository.findAllByOrganization(organization);

        List<GetTeamsResponseDto> response = new ArrayList<>();
        for (Team team : findTeamList) {
            MemberTeam findMemberAndTeam = memberTeamRepository.findByMemberAndTeam(member, team);

            // 내가 속해 있는 팀
            if (findMemberAndTeam != null) {
                response.add(new GetTeamsResponseDto(team.getId(), organization.getId(), team.getName()));
            }
        }

        return response;
    }

    @Transactional
    public void createTeam(CreateTeamRequestDto requestDto, Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ExceptionCode.MEMBER_NOT_FOUND));
        Organization organization = organizationRepository.findById(requestDto.getOrganizationId()).orElseThrow(() -> new BusinessException(ExceptionCode.ORGANIZATION_NOT_FOUND));

        if (teamRepository.existsByName(requestDto.getTeamName())) {
            throw new BusinessException(ExceptionCode.TEAM_NAME_CONFLICT);
        }

        /**
         * Todo 팀 비밀키 발급
         */
        byte[] encryptedDataKey = null;
        byte[] encryptedIv = null;
        LocalDateTime expiredAt = null;

        Team newTeam = Team.builder()
                .header(member)
                .organization(organization)
                .name(requestDto.getTeamName())
                .roleCount(0)
                .secretCount(0)
                .encryptedDataKey(encryptedDataKey)
                .encryptedIv(encryptedIv)
                .expiredAt(expiredAt)
                .build();

        teamRepository.save(newTeam);
    }

    @Transactional
    public void deleteTeam(DeleteTeamRequestDto requestDto, Long memberId) {
        Team team = teamRepository.findById(requestDto.getTeamId()).orElseThrow(() -> new BusinessException(ExceptionCode.TEAM_NOT_FOUND));
        if (team.getHeader().getId() != memberId) {
            throw new BusinessException(ExceptionCode.TEAM_UNAUTHORIZED);
        }
        teamRepository.delete(team);
    }

    @Transactional
    public void inviteMemberToTeam(InviteMemberToTeamRequestDto requestDto, Long memberId) {
        /**
         * Todo 팀 초대권한 확인
         */
        Member sender = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ExceptionCode.MEMBER_NOT_FOUND));
        Member receiver = memberRepository.findByEmail(requestDto.getInviteMemberEmail());
        Organization organization = organizationRepository.findById(requestDto.getOrganizationId()).orElseThrow(() -> new BusinessException(ExceptionCode.ORGANIZATION_NOT_FOUND));
        Team team = teamRepository.findById(requestDto.getTeamId()).orElseThrow(() -> new BusinessException(ExceptionCode.TEAM_NOT_FOUND));

        invitationService.inviteMemberToTeam(organization, team, sender, requestDto.getInviteMemberEmail());

        // 우리 회원인 경우
        if (receiver != null) {
            MemberOrganization findMemberAndOrganization = memberOrganizationRepository.findByMemberAndOrganization(receiver, organization);
            if (findMemberAndOrganization != null) {    // 이미 우리 회원인 경우
                throw new BusinessException(ExceptionCode.BAD_REQUEST);
            } else {
                /**
                 * Todo 알림 보내기
                 */
            }
        }
    }

    @Transactional
    public void removeMemberFromTeam(RemoveMemberFromTeamRequestDto requestDto, Long memberId) {
        /**
         * Todo 팀원 추방권한 확인
         */
        Member findMember = memberRepository.findByEmail(requestDto.getRemoveMemberEmail());
        Team team = teamRepository.findById(requestDto.getTeamId()).orElseThrow(() -> new BusinessException(ExceptionCode.TEAM_NOT_FOUND));
        if (findMember == null) {
            throw new BusinessException(ExceptionCode.MEMBER_NOT_FOUND);
        }
        MemberTeam findMemberAndTeam = memberTeamRepository.findByMemberAndTeam(findMember, team);
        memberTeamRepository.delete(findMemberAndTeam);
    }


    @Async
    @Transactional
    public void refreshByMasterKey(){

        //team 목록 가져오기..
        Long startId = 0L;
        Long endId = 1000L;
        while(true) {
            List<Team> teams = teamRepository.findByIdBetween(startId, endId);
            if (!teams.isEmpty()) {
                for(Team team : teams){

                    //team에서 encryptedDataKey, encryptedIvKey 가져오기.
                    KmsKeyDto requestDto = KmsKeyDto.builder()
                            .encryptedDataKey(Base64.getEncoder().encodeToString(team.getEncryptedDataKey()))
                            .encryptedIv(Base64.getEncoder().encodeToString(team.getEncryptedIv()))
                            .build();

                    //data key 재암호화 요청.
                    KmsKeyDto responseDto = keyService.reEncrypt(requestDto);

                    //재암호화된 data key들 갱신
                    team.setEncryptedDataKey(Base64.getDecoder().decode(responseDto.getEncryptedDataKey()));
                    team.setEncryptedIv(Base64.getDecoder().decode(responseDto.getEncryptedIv()));
                    teamRepository.save(team);

                    //로그 찍기
                    log.info("member {}'s TeamDataKey re-encrypted", team.getId());
                }
                startId = endId;
                endId += 1000L;
            } else {
                break;
            }
        }
    }

    @Async
    @Transactional
    public void rotateDataKey(){
        //team 목록 가져오기..
        Long startId = 0L;
        Long endId = 1000L;

        //팀 풀스캔. 1000개씩 검색.
        while(true){
            List<Team> teams = teamRepository.findByIdBetween(startId, endId);
            if (!teams.isEmpty()) {
                for(Team team : teams){

                    //만료여부확인
					LocalDateTime expiredAt = team.getExpiredAt();

                    if(expiredAt.isAfter(LocalDateTime.now())) continue;

                    //만료시간이 지났으면 갱신로직 시작

                    //team에서 encryptedDataKey, encryptedDataIvKey 가져오기.
                    KmsKeyDto requestDto = KmsKeyDto.builder()
                            .encryptedDataKey(Base64.getEncoder().encodeToString(team.getEncryptedDataKey()))
                            .encryptedIv(Base64.getEncoder().encodeToString(team.getEncryptedIv()))
                            .build();

                    //기존 데이터 키 복호화 및 재발급 요청
                    KmsReGenerationKeysResponseDto responseDto = keyService.reGenerateKey(requestDto);


                    //현재 팀에 해당하는 privateData 모두 가져오기
                    List<PrivateData> privateDatas = privateDataRepository.findAllByTeam(team);
                    for(PrivateData privateData : privateDatas){
                        //privatData 복호화
                        byte[] plainContent = keyService.decryptSecret(privateData.getContent(),
                                Base64.getDecoder().decode(responseDto.getOldDataKey()),
                                Base64.getDecoder().decode(responseDto.getOldIv()));

                        //재암호화
                        byte[] encrpytedContent = keyService.encryptSecret(plainContent,
                                Base64.getDecoder().decode(responseDto.getNewDataKey()),
                                Base64.getDecoder().decode(responseDto.getNewIv()));

                        //재암호화된 privateData 저장.
                        privateData.setContent(encrpytedContent);
                        privateDataRepository.save(privateData);
                    }

                    //재암호화된 data key들 갱신
                    team.setEncryptedDataKey(Base64.getDecoder().decode(responseDto.getEncryptedNewDataKey()));
                    team.setEncryptedIv(Base64.getDecoder().decode(responseDto.getEncryptedNewIv()));
                    teamRepository.save(team);

                    //로그 찍기
                    log.info("team {}'s DataKey re-generated and all PrivateDate re-encrypted", team.getId());
                }
                startId = endId;
                endId += 1000L;
            } else {
                break;
            }
        }

    }
}
