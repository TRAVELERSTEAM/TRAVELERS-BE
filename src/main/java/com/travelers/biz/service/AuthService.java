package com.travelers.biz.service;


import com.travelers.biz.domain.Member;
import com.travelers.biz.domain.Token;
import com.travelers.biz.domain.image.Image;
import com.travelers.biz.domain.image.MemberImage;
import com.travelers.biz.repository.ImageRepository;
import com.travelers.biz.repository.MemberRepository;
import com.travelers.biz.repository.TokenRepository;
import com.travelers.biz.service.handler.FileUploader;
import com.travelers.biz.service.handler.S3Uploader;
import com.travelers.dto.MemberRequestDto;
import com.travelers.dto.MemberResponseDto;
import com.travelers.dto.TokenRequestDto;
import com.travelers.dto.TokenResponseDto;
import com.travelers.exception.TravelersException;
import com.travelers.jwt.JwtTokenProvider;
import com.travelers.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.travelers.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    @Value("${spring.file.directory}")
    private String location;

    @Value("${profileImage}")
    private String url;

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final MemberRepository memberRepository;
    private final ImageRepository imageRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRepository tokenRepository;
    private final FileUploader fileUploader;
    private final MemberService memberService;
    private final EmailService emailService;
    private final S3Uploader s3Uploader;

    @Transactional
    public MemberResponseDto register(MemberRequestDto memberRequestDto, List<MultipartFile> files) throws IOException {
        if (memberRepository.existsByEmail(memberRequestDto.getEmail())) {
            throw new TravelersException(DUPLICATE_ACCOUNT);
        }

        if(!checkPasswordIsSame(memberRequestDto.getPassword(), memberRequestDto.getConfirmPassword())){
            throw new TravelersException(PASSWORD_NOT_MATCHING);
        }

        if(!emailService.verifyKey(memberRequestDto.getEmail() ,memberRequestDto.getKey())) {
            throw new TravelersException(KEY_NOT_FOUND);
        }

        Member member = memberRequestDto.toMember(passwordEncoder);
        emailService.deleteKey(memberRequestDto.getKey());

        memberRepository.save(member);

        Member myMember = memberRepository.findByEmail(member.getEmail())
                .orElseThrow(() -> new TravelersException(MEMBER_NOT_FOUND));
        if(files != null && !files.isEmpty() && !files.get(0).isEmpty()) {
            String storedLocation = FileUtils.getStoredLocation(files.get(0).getOriginalFilename(), location);
            File file = new File(storedLocation);
            FileCopyUtils.copy(files.get(0).getBytes(), file);
            String s3url = s3Uploader.upload(file, files.get(0).getOriginalFilename());
            addImage(myMember, s3url);
        }
        else {
            addImage(myMember, url);
        }

        return MemberResponseDto.of(memberRepository.save(myMember));
    }

    @Transactional
    public TokenResponseDto login(MemberRequestDto.Login login) {
        // 1. Login ID/PW ??? ???????????? AuthenticationToken ??????
        UsernamePasswordAuthenticationToken authenticationToken = login.toAuthentication();

        // 2. ????????? ?????? (????????? ???????????? ??????) ??? ??????????????? ??????
        //    authenticate ???????????? ????????? ??? ??? CustomUserDetailsService ?????? ???????????? loadUserByUsername ???????????? ?????????
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. ?????? ????????? ???????????? JWT ?????? ??????
        TokenResponseDto tokenDto = jwtTokenProvider.generateTokenDto(authentication);

        // 4. Token ??????
        Token token = Token.builder()
                .id(authentication.getName())
                .accessToken(tokenDto.getAccessToken())
                .refreshToken(tokenDto.getRefreshToken())
                .build();

        tokenRepository.save(token);

        // 5. ?????? ??????
        return tokenDto;
    }

    @Transactional
    public TokenResponseDto reissue(TokenRequestDto tokenRequestDto) {
        // 1. Refresh Token ??????
        if (!jwtTokenProvider.validateRefreshToken(tokenRequestDto.getRefreshToken())) {
            throw new TravelersException(INVALID_REFRESH_TOKEN);
        }

        // 2. Access Token ?????? Member ID ????????????
        Authentication authentication = jwtTokenProvider.getAuthentication(tokenRequestDto.getAccessToken());

        // 3. ??????????????? Member ID ??? ???????????? Refresh Token ??? ?????????
        Token token = tokenRepository.findById(authentication.getName())
                .orElseThrow(() -> new TravelersException(REFRESH_TOKEN_NOT_FOUND));

        // 4. Refresh Token ??????????????? ??????
        if (!token.getRefreshToken().equals(tokenRequestDto.getRefreshToken())) {
            throw new TravelersException(MISMATCH_REFRESH_TOKEN);
        }

        // 5. ????????? ?????? ??????
        TokenResponseDto tokenDto = jwtTokenProvider.generateTokenDto(authentication);

        // 6. ????????? ?????? ????????????
        Token newToken = token.refreshUpdate(tokenDto.getRefreshToken());
        tokenRepository.save(newToken);

        // ?????? ??????
        return tokenDto;
    }

    @Transactional
    public void findPassword(MemberRequestDto.FindPassword findPassword) {
        Member member = memberRepository.findByUsernameAndBirthAndGenderAndTelAndEmail(
                findPassword.getUsername(),
                findPassword.getBirth(),
                findPassword.getGender(),
                findPassword.getTel(),
                findPassword.getEmail()
        ).orElseThrow(() -> new TravelersException(MEMBER_NOT_FOUND));

        if(!emailService.verifyKey(findPassword.getEmail(), findPassword.getKey())){
            throw new TravelersException(KEY_NOT_FOUND);
        }
        String tempPassword = emailService.joinResetPassword(findPassword.getEmail());
        memberService.changePassword(member, tempPassword);
    }

    // ?????? ???????????? ??????(????????? ????????????)
    public boolean checkPasswordIsSame(String password, String confirmPassword) {
        return password.equals(confirmPassword);
    }

    // ????????? ????????? ??????
    private void addImage(final Member member, String url) {
        new MemberImage(url, member);
    }

    // ????????? ????????? ????????????
    public void update(final Long memberId, String url) {
        final Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new TravelersException(MEMBER_NOT_FOUND));
        deleteImage(memberId);
        addImage(member, url);
    }

    // ????????? ????????? ??????
    private void deleteImage(final Long memberId) {
        final Image image = imageRepository.findByMemberId(memberId)
                .orElseThrow(() -> new TravelersException(IMAGE_NOT_FOUND));

        final String key = image.getKey();
        fileUploader.delete(List.of(key));
        imageRepository.delete(image);
    }
}