package com.tapecloud.auth.user.repository;

import com.tapecloud.auth.user.entity.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    @EntityGraph(attributePaths = "roles")
    Optional<AppUser> findByEmailIgnoreCase(String email);

    @Query("select distinct u from AppUser u left join fetch u.roles where lower(u.email) = lower(?1)")
    Optional<AppUser> findByEmailWithRolesIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
