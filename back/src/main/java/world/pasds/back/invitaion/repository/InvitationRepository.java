package world.pasds.back.invitaion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import world.pasds.back.invitaion.entity.Invitation;
import world.pasds.back.organization.entity.Organization;
import world.pasds.back.team.entity.Team;

import java.util.List;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Invitation findOrganizationInvitationByInvitedMemberEmailAndOrganizationOrderByCreatedAt(String invitedMemberEmail, Organization organization);
    Invitation findTeamInvitationByInvitedMemberEmailAndOrganizationAndTeamOrderByCreatedAt(String invitedMemberEmail, Organization organization, Team team);

    List<Invitation> findAllByInvitedMemberEmail(String email);
}