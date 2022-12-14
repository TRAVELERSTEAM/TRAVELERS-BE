package com.travelers.biz.repository.departure;

import com.travelers.biz.domain.departure.Departure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartureRepository extends JpaRepository<Departure, Long>, DepartureRepositoryQuery {
}
