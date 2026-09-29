package com.mirror.salon.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StylistRepository extends JpaRepository<Stylist, Integer> {

    @Query("SELECT s FROM Stylist s JOIN FETCH s.staff")
    List<Stylist> findAllWithStaff();

    @Query("SELECT s FROM Stylist s JOIN FETCH s.staff WHERE s.staffId = :staffId")
    Optional<Stylist> findByIdWithStaff(@Param("staffId") Integer staffId);
}
