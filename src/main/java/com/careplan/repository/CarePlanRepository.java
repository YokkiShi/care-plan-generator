package com.careplan.repository;

import com.careplan.model.CarePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CarePlanRepository extends JpaRepository<CarePlan, Long> {

    @Query("SELECT cp FROM CarePlan cp WHERE cp.order.id = :orderId")
    Optional<CarePlan> findByOrderId(@Param("orderId") Long orderId);

    // JOIN FETCH 一次性把 order、patient、provider 都加载进来，避免懒加载报错
    @Query("SELECT cp FROM CarePlan cp JOIN FETCH cp.order o JOIN FETCH o.patient JOIN FETCH o.provider WHERE cp.id = :id")
    Optional<CarePlan> findByIdWithDetails(@Param("id") Long id);
}
