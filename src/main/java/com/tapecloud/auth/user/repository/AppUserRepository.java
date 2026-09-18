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

    boolean existsByUsernameIgnoreCase(String username);

    // El login acepta email o username: se busca por cualquiera de los dos.
    @EntityGraph(attributePaths = "roles")
    Optional<AppUser> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);

    @Query("select count(u) > 0 from AppUser u join u.roles r where r.name = ?1")
    boolean existsByRoleName(String roleName);
}
