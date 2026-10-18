package com.pulsepass.repository;

import com.pulsepass.domain.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    @Query("select c.passwordHash from UserCredential c where c.id = :userId")
    Optional<String> findPasswordHashByUserId(@Param("userId") Long userId);
}
