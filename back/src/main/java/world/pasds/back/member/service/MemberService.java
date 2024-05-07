package world.pasds.back.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.pasds.back.common.exception.BusinessException;
import world.pasds.back.common.exception.ExceptionCode;
import world.pasds.back.invitaion.service.InvitationService;
import world.pasds.back.member.dto.request.SignupRequestDto;
import world.pasds.back.member.entity.Member;
import world.pasds.back.member.repository.MemberRepository;
import world.pasds.back.organization.entity.dto.request.CreateOrganizationRequestDto;
import world.pasds.back.organization.service.OrganizationService;
import world.pasds.back.totp.service.TotpService;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final MemberRepository memberRepository;
    private final InvitationService invitationService;
    private final TotpService totpService;
    private final OrganizationService organizationService;

    @Value("${security.pepper}")
    private String pepper;

    @Transactional
    public byte[] signup(SignupRequestDto signupRequestDto) {

        // 이메일 형식
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        Pattern pattern = Pattern.compile(emailRegex);
        if (!pattern.matcher(signupRequestDto.getEmail()).matches()) {
            throw new BusinessException(ExceptionCode.EMAIL_INVALID_FORMAT);
        }

        // DB에 없는 이메일
        if (memberRepository.existsByEmail(signupRequestDto.getEmail())) {
            throw new BusinessException(ExceptionCode.EMAIL_EXISTS);
        }

        // TODO: 이메일 인증을 거쳐 온 것(여기서는 어떻게 알지?)

        // 정규 표현식: 소문자, 대문자, 숫자, 특수문자를 포함하며 길이가 10자 이상
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+{}\\[\\]:;<>,.?/~`\\-|\\\\=])[A-Za-z\\d!@#$%^&*()_+{}\\[\\]:;<>,.?/~`\\-|\\\\=]{10,}$";
        if (!signupRequestDto.getPassword().matches(passwordRegex)) {
            throw new BusinessException(ExceptionCode.PASSWORD_INVALID_FORMAT);
        }

        // 비밀번호와 비밀번호 확인이 일치
        if (!signupRequestDto.getPassword().equals(signupRequestDto.getConfirmPassword())) {
            throw new BusinessException(ExceptionCode.PASSWORD_CONFIRM_INVALID);
        }

        // 닉네임이 2자리 이상 20자리 이하
        if (signupRequestDto.getNickname().length() < 2 || signupRequestDto.getNickname().length() > 20) {
            throw new BusinessException(ExceptionCode.NICKNAME_INVALID_FORMAT);
        }

        // 비밀번호 암호화하여 저장
        String encryptedPassword = bCryptPasswordEncoder.encode(signupRequestDto.getPassword() + pepper);
        Member newMember = Member.builder()
                .email(signupRequestDto.getEmail())
                .password(encryptedPassword)
                .nickname(signupRequestDto.getNickname())
                .build();
        memberRepository.save(newMember);

		/**
         * 회원가입시 받은 초대 모두 가입시키기
         */
//        invitationService.checkInvitation(newMember, signupRequestDto.getEmail());

        Member member = memberRepository.findByEmail(newMember.getEmail());
        organizationService.createOrganization(new CreateOrganizationRequestDto("MY ORGANIZATION"), newMember.getId());
        // totp key 발급
        return totpService.generateSecretKeyQR(member.getId());
    }
}