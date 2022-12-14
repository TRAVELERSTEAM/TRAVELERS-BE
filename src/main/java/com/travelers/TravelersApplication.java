package com.travelers;

import com.travelers.biz.domain.Authority;
import com.travelers.biz.domain.Gender;
import com.travelers.biz.domain.Member;
import com.travelers.biz.domain.image.MemberImage;
import com.travelers.biz.domain.notify.ManualIncrement;
import com.travelers.biz.repository.MemberRepository;
import com.travelers.biz.repository.notify.NotifyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootApplication
@EnableJpaAuditing
public class TravelersApplication {

    @Value("${email}") String email;
    @Value("${password}") String password;
    @Value("${profileImage}") String url;

    public static void main(String[] args) {
        SpringApplication.run(TravelersApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            NotifyRepository notifyRepository
    ){
        return (arg) -> {
            if(!memberRepository.existsByEmail(email)) {
                Member member = Member.builder()
                        .email(email)
                        .username("관리자")
                        .password(passwordEncoder.encode(password))
                        .birth("19000906")
                        .gender(Gender.MALE)
                        .tel("01077777777")
                        .recommend("")
                        .groupTrip("5070끼리,2040끼리,남자끼리,여자끼리,자녀동반,누구든지")
                        .area("동남아/태평양,인도/중앙아시아,아프리카/중동,유럽/코카서스,중남미/북미,대만/중국/일본")
                        .theme("문화탐방,골프여행,휴양지,트레킹,성지순례,볼론투어")
                        .build();
                member.setId(1L);
                member.changeAuthority(Authority.ROLE_ADMIN);
                new MemberImage(url, member);
                memberRepository.save(member);
            }

            notifyRepository.initRefLibrary()
                    .ifPresent(e -> ManualIncrement.REF_LIBRARY.set(e.getSequence()));
            notifyRepository.initNotice()
                    .ifPresent(e -> ManualIncrement.NOTICE.set(e.getSequence()));
        };
    }

}