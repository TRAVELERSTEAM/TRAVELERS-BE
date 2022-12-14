package com.travelers.biz.service;

import com.travelers.biz.domain.AnonymousMember;
import com.travelers.biz.domain.Member;
import com.travelers.biz.domain.departure.Departure;
import com.travelers.biz.domain.reservation.embeddable.HeadCount;
import com.travelers.biz.repository.AnonymousMemberRepository;
import com.travelers.biz.repository.AnonymousReservationRepository;
import com.travelers.biz.repository.MemberRepository;
import com.travelers.biz.repository.departure.DepartureRepository;
import com.travelers.biz.repository.reservation.ReservationRepository;
import com.travelers.dto.AnonymousReservationResInfo;
import com.travelers.dto.ReservationRequest;
import com.travelers.dto.ReservationResInfo;
import com.travelers.dto.paging.PagingCorrespondence;
import com.travelers.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.travelers.exception.OptionalHandler.findOrResourceNotFound;
import static com.travelers.exception.OptionalHandler.findOrThrow;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final MemberRepository memberRepository;
    private final DepartureRepository departureRepository;
    private final ReservationRepository reservationRepository;
    private final AnonymousMemberRepository anonymousMemberRepository;
    private final AnonymousReservationRepository anonymousReservationRepository;

    @Transactional
    public void memberCreate(
            final Long memberId,
            final Long departureId,
            final HeadCount headCount
    ) {
        final Member member = findOrThrow(memberId, memberRepository::findById, ErrorCode.MEMBER_NOT_FOUND);
        final Departure departure = findOrThrow(departureId, departureRepository::findById, ErrorCode.RESOURCE_NOT_FOUND);

        reservationRepository.save(
                departure.reserve(member, headCount)
        );
    }

    @Transactional
    public void nonMemberCreate(
            final Long departureId,
            final ReservationRequest.NonMember reserveRequest
    ) {
        final AnonymousMember anonymousMember = anonymousMemberRepository.save(reserveRequest.toAnonymousMember());
        final Departure departure = findOrThrow(departureId, departureRepository::findById, ErrorCode.RESERVATION_NOT_FOUND);

        anonymousReservationRepository.save(
                departure.reserve(anonymousMember, reserveRequest.getHeadCount())
        );
    }

    @Transactional(readOnly = true)
    public PagingCorrespondence.Response<ReservationResInfo> reservationList(
            final Long memberId,
            final PagingCorrespondence.Request paging
    ) {
        return reservationRepository.findListByMemberId(memberId, paging.toPageable());
    }

    @Transactional
    public void cancel(
            final Long memberId,
            final Long reservationId
    ) {
        findOrThrow(memberId, reservationId, reservationRepository::findByIdAndMemberId, ErrorCode.RESERVATION_NOT_FOUND)
                .cancel();
    }

    @Transactional(readOnly = true)
    public AnonymousReservationResInfo findAnonymousReservation(
            final String reservationCode
    ) {
        return findOrResourceNotFound(reservationCode, anonymousReservationRepository::findDtoByCode);
    }

    @Transactional
    public void cancel(
            final String reservationCode
    ) {
        findOrThrow(reservationCode, anonymousReservationRepository::findByCode, ErrorCode.RESOURCE_NOT_FOUND)
                .cancel();
    }
}
