package com.tapecloud.sso.user.repository;

import com.tapecloud.sso.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    @Query("select distinct u from AppUser u left join fetch u.roles where lower(u.email) = lower(?1)")
    Optional<AppUser> findByEmailWithRolesIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
