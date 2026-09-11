# TapeCloud Auth Core - Backend SSO 🔐

Backend centralizado de autenticación y gestión de contenido para TapeCloud. Desarrollado con **Spring Boot 3.3.4** y **Java 21**.

## 📋 Responsabilidades

- ✅ Autenticación y registro de usuarios
- ✅ Generación y validación de JWT tokens
- ✅ Gestión de roles y permisos (RBAC)
- ✅ APIs REST de películas y contenido
- ✅ Integración con TMDB (descarga de películas)
- ✅ Reviews, likes y comentarios
- ✅ Base de datos H2 centralizada

---

## 🏗️ Estructura

```
src/main/java/com/tapecloud/sso/
├── api/
│   ├── AuthController.java          # POST /auth/register, /auth/login
│   ├── GlobalExceptionHandler.java  # Error handling
│   └── HealthController.java        # GET /health
│
├── config/
│   ├── SecurityConfig.java          # Spring Security + JWT
│   ├── JwtService.java              # Tokens JWT
│   ├── JwtAuthenticationFilter.java  # Filtro JWT
│   ├── CustomUserDetailsService.java # UserDetails provider
│   ├── AutoSyncConfig.java          # TMDB sync on startup
│   └── DataInitializer.java         # Init de datos
│
├── service/
│   ├── AuthService.java             # Lógica de autenticación
│   └── TmdbSyncService.java         # Sync TMDB
│
├── user/
│   ├── entity/
│   │   ├── AppUser.java
│   │   ├── Role.java
│   │   └── Permission.java
│   │
│   ├── dto/
│   │   ├── AuthRequest.java
│   │   └── AuthResponse.java
│   │
│   └── repository/
│       ├── AppUserRepository.java
│       ├── RoleRepository.java
│       └── PermissionRepository.java
│
├── integration/
│   └── tmdb/
│       ├── TmdbClient.java          # Cliente HTTP TMDB
│       └── TmdbProperties.java      # Config API Key
│
└── TapecloudSsoBackendApplication.java  # Main
```

---

## 🚀 Cómo Ejecutar

### Docker (Recomendado)

```bash
cd TapeCloud
docker compose up --build tapecloud-auth-core
# Puerto 8080
```

### Local (Sin Docker)

**Requisitos:**
- Java 21+
- Maven 3.9+

```bash
cd tapecloud-auth-core

# Build
./mvnw clean package

# Run
./mvnw spring-boot:run

# Servidor en http://localhost:8080
```

### Test

```bash
./mvnw test
```

---

## 🔌 APIs Principales

### Autenticación

#### POST `/auth/register`
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "usuario@example.com",
    "password": "secure123",
    "displayName": "Juan Pérez"
  }'
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "usuario@example.com",
  "displayName": "Juan Pérez"
}
```

---

#### POST `/auth/login`
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "usuario@example.com",
    "password": "secure123"
  }'
```

**Response:** `200 OK` (mismo formato que register)

---

#### GET `/health`
```bash
curl http://localhost:8080/health
```

**Response:** `200 OK`
```json
{
  "status": "UP"
}
```

---

### Películas (Content)

#### GET `/api/content/movies?page=0&size=20`
Obtiene todas las películas paginadas.

```bash
curl "http://localhost:8080/api/content/movies?page=0&size=20"
```

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "title": "Toy Story",
      "genre": "Animation, Comedy",
      "voteAverage": 8.3,
      "voteCount": 11000,
      "overview": "A cowboy doll..."
    }
  ],
  "totalElements": 243,
  "totalPages": 13,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

---

#### GET `/api/content/movies/genre?genre=Animation&page=0&size=20`
Obtiene películas filtradas por género.

```bash
curl "http://localhost:8080/api/content/movies/genre?genre=Animation&page=0&size=20"
```

---

#### POST `/api/content/sync/tmdb/movies/bulk?startPage=1&pageCount=5`
Sincroniza películas desde TMDB. ⚠️ **Máximo 10 páginas**.

```bash
curl -X POST "http://localhost:8080/api/content/sync/tmdb/movies/bulk?startPage=1&pageCount=5"
```

---

## ⚙️ Configuración

### application.properties

```properties
# Server
server.port=8080

# Database (H2 - File-based)
spring.datasource.url=jdbc:h2:file:./data/tapecloud
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect

# JWT
jwt.secret=tapecloud-secret-key-change-production
jwt.expiration=86400000

# TMDB API
TMDB_API_KEY=a84f35dc44a41efc9abc4c589872c0eb
```

---

## 🎬 TMDB Integration

### Auto-Sync on Startup

Al iniciar el servidor, `AutoSyncConfig.java` automáticamente:

1. Descarga **10 páginas** de películas de TMDB
2. Filtro: Género **Animation** (id: 16)
3. Ordenamiento: **Popularidad descendente**
4. Rate limiting: **250ms** entre requests
5. Total: **~250 películas** en la BD

### Logs esperados

```
INFO - Sincronización automática de películas Animation completada al iniciar el servidor
INFO - Total de películas descargadas: 243
```

### Endpoint para sincronización manual

```bash
# Descargar 5 más páginas de películas
curl -X POST "http://localhost:8080/api/content/sync/tmdb/movies/bulk?startPage=11&pageCount=5"
```

---

## 🔒 Seguridad (JWT)

### Flujo de autenticación

```
1. Cliente envía credenciales POST /auth/login
                ↓
2. AuthService valida password contra BD
                ↓
3. JwtService genera token (24h de validez)
                ↓
4. Token se devuelve al cliente
                ↓
5. Cliente envía: Authorization: Bearer <token>
                ↓
6. JwtAuthenticationFilter valida token
                ↓
7. Si válido: Acceso permitido
```

### Token JWT

**Structure:**
```
Header.Payload.Signature
```

**Ejemplo:**
```json
// Payload
{
  "sub": "usuario@example.com",
  "iat": 1625000000,
  "exp": 1625086400
}
```

---

## 💾 Base de Datos (H2)

### Archivo de datos

- **Ubicación:** `./data/tapecloud`
- **Formato:** H2 File-based SQL
- **Persistencia:** Sobrevive reinicios de contenedores (volumen Docker)

### Tablas principales

```sql
CREATE TABLE APP_USER (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(255),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE MOVIE (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  genre VARCHAR(255),
  vote_average DECIMAL(3,1),
  vote_count INT,
  overview TEXT,
  release_date DATE,
  poster_path VARCHAR(255),
  backdrop_path VARCHAR(255),
  tmdb_id BIGINT UNIQUE
);

CREATE TABLE ROLE (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) UNIQUE
);

CREATE TABLE PERMISSION (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) UNIQUE
);
```

---

## 🐛 Troubleshooting

| Problema | Solución |
|----------|----------|
| "JAVA_HOME not set" | Instalar Java 21 y configurar JAVA_HOME |
| "Cannot find mvnw" | Usar `./mvnw` (Linux/Mac) o `mvnw.cmd` (Windows) |
| Puerto 8080 en uso | `docker compose down` |
| "TMDB API rate limit" | Esperar 10 segundos, o aumentar `Thread.sleep()` |
| Películas no sincronizadas | Ver logs: `docker compose logs tapecloud-auth-core` |

---

## 📚 Dependencias principales

```xml
<!-- Spring Boot -->
<spring-boot-starter-web/>
<spring-boot-starter-security/>
<spring-boot-starter-data-jpa/>

<!-- Database -->
<h2/>
<spring-boot-starter-jdbc/>

<!-- JWT -->
<jjwt>0.11.5</jjwt>

<!-- HTTP Client -->
<httpclient5>5.3.1</httpclient5>
```

---

## 📖 Más información

Ver [README raíz](../README.md) para arquitectura general del proyecto.

---

**Última actualización:** Septiembre 2024
