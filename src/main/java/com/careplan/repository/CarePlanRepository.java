package com.careplan.repository;

import com.careplan.model.CarePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CarePlanRepository extends JpaRepository<CarePlan, Long> {

    @Query("SELECT cp FROM CarePlan cp WHERE cp.order.id = :orderId")
    Optional<CarePlan> findByOrderId(@Param("orderId") Long orderId);
}
