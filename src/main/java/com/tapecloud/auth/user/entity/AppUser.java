package com.tapecloud.auth.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(unique = true, length = 60)
    private String username;

    @Column(nullable = false, length = 120)
    private String displayName;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(length = 6)
    private String verificationCode;

    private Instant verificationCodeExpiresAt;

    @Column(nullable = false)
    private boolean totpEnabled = false;

    @Column(length = 64)
    private String totpSecret;

    @Column(columnDefinition = "TEXT")
    private String avatarDataUri;

    // Se incrementa al cambiar la contraseña o los roles; los JWT emitidos antes
    // de eso quedan invalidados porque su claim "tv" ya no coincide (ver JwtAuthenticationFilter).
    // "default 0" es necesario para que ddl-auto=update pueda agregar la columna NOT NULL
    // sobre una tabla "users" que ya tiene filas (si no, la migración falla en el arranque).
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int tokenVersion = 0;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    protected AppUser() {
    }

    public AppUser(String email, String password) {
        this.email = email;
        this.password = password;
        this.displayName = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
    }

    public AppUser(String email, String password, String username) {
        this(email, password);
        if (username != null && !username.isBlank()) {
            this.username = username;
            this.displayName = username;
        }
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
    public Instant getVerificationCodeExpiresAt() { return verificationCodeExpiresAt; }
    public void setVerificationCodeExpiresAt(Instant verificationCodeExpiresAt) { this.verificationCodeExpiresAt = verificationCodeExpiresAt; }
    public boolean isTotpEnabled() { return totpEnabled; }
    public void setTotpEnabled(boolean totpEnabled) { this.totpEnabled = totpEnabled; }
    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }
    public String getAvatarDataUri() { return avatarDataUri; }
    public void setAvatarDataUri(String avatarDataUri) { this.avatarDataUri = avatarDataUri; }
    public int getTokenVersion() { return tokenVersion; }
    public void bumpTokenVersion() { this.tokenVersion++; }
    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
    public void addRole(Role role) { this.roles.add(role); }
}
