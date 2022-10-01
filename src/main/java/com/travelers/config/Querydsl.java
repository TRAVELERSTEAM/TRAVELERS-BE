package com.travelers.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManager;

@Configuration
public class Querydsl {

    @Bean
    JPAQueryFactory jpaQueryFactory(final EntityManager em){
        return new JPAQueryFactory(em);
    }
}
