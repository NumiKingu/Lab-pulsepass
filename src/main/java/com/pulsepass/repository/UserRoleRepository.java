package com.pulsepass.repository;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.UserRole;
import com.pulsepass.domain.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    @Query("select r.id.role from UserRole r where r.id.userId = :userId")
    Set<Role> findRolesByUserId(@Param("userId") Long userId);
}
