# Índice del repositorio - TapeCloud SSO Backend

Este repositorio es el backend de autenticación y autorización para TapeCloud. El objetivo es servir como base para una arquitectura SSO (Single Sign-On) con una API REST segura, JWT y conexión con un frontend en React.

## Stack principal

- Java 21
- Maven
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA
- Spring Security
- PostgreSQL
- H2 (para tests)
- JWT (jjwt)
- Docker + Docker Compose

## Índice rápido

- [Resumen del repositorio](#resumen-del-repositorio)
- [Estructura de carpetas](#estructura-de-carpetas)
- [Qué hace cada módulo](#qué-hace-cada-módulo)
- [Flujo actual de autenticación](#flujo-actual-de-autenticación)
- [Configuración importante](#configuración-importante)
- [Cómo levantar la app](#cómo-levantar-la-app)
- [Cómo probarlo](#cómo-probarlo)
- [Cómo se integra con React](#cómo-se-integra-con-react)
- [Roadmap recomendado](#roadmap-recomendado)

## Resumen del repositorio

El backend está pensado como una API REST stateless:

1. el frontend envía usuario y password
2. el backend valida credenciales
3. el backend devuelve un JWT
4. el frontend guarda el token
5. cada request protegida lleva `Authorization: Bearer <token>`
6. Spring Security valida el token antes de permitir el acceso

Esto es la arquitectura correcta para integrar con React, porque el frontend ya no depende de sesiones del lado del servidor.

## Estructura de carpetas

```text
tc-backend/
├── docker/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── .env.example
├── src/
│   ├── main/
│   │   ├── java/com/tapecloud/sso/
│   │   │   ├── api/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── HealthController.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── JwtService.java
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   └── DataInitializer.java
│   │   │   ├── service/
│   │   │   │   └── AuthService.java
│   │   │   └── user/
│   │   │       ├── dto/
│   │   │       │   ├── AuthRequest.java
│   │   │       │   └── AuthResponse.java
│   │   │       ├── entity/
│   │   │       │   ├── AppUser.java
│   │   │       │   ├── Role.java
│   │   │       │   └── Permission.java
│   │   │       └── repository/
│   │   │           ├── AppUserRepository.java
│   │   │           ├── RoleRepository.java
│   │   │           └── PermissionRepository.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── resources/
│           └── application.properties
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .gitignore
├── HELP.md
└── README.md
```

## Qué hace cada módulo

### api
Contiene los endpoints HTTP del backend.

Ejemplos:
- `/api/health`
- `/api/version`
- `/api/auth/login`
- `/api/auth/register`
- `/api/auth/me`

### config
Contiene la configuración central de Spring y seguridad.

Ejemplos:
- `SecurityConfig`: reglas de acceso, CORS, JWT filter
- `JwtAuthenticationFilter`: valida token por request
- `JwtService`: firma y valida tokens
- `CustomUserDetailsService`: carga usuarios para Spring Security
- `DataInitializer`: crea roles por defecto

### service
Aquí va la lógica de negocio.

Ejemplo:
- `AuthService` valida credenciales, crea usuarios, genera JWT

### user
Es el módulo de dominio del usuario.

Incluye:
- entidades JPA
- repositorios
- DTOs de entrada/salida
- lógica estructural del usuario y roles

## Flujo actual de autenticación

El flujo actual es:

1. el cliente hace `POST /api/auth/login`
2. backend recibe email y password
3. Spring Security autentica con `AuthenticationManager`
4. si las credenciales son válidas, genera un JWT
5. responde `{ token, email, roles }`
6. el frontend guarda el token
7. en cada petición protegida envía `Authorization: Bearer <token>`
8. el filtro JWT valida la firma y la expiración

## Configuración importante

El archivo principal es:

- `src/main/resources/application.properties`

Variables clave:

```properties
spring.datasource.url=${DB_URL:jdbc:h2:mem:tapecloud;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE}
spring.datasource.username=${DB_USERNAME:sa}
spring.datasource.password=${DB_PASSWORD:}
spring.datasource.driver-class-name=${DB_DRIVER_CLASS_NAME:org.h2.Driver}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=${HIBERNATE_DIALECT:org.hibernate.dialect.H2Dialect}

jwt.secret=${JWT_SECRET:test-secret-key-for-local-development}
jwt.expiration-ms=${JWT_EXPIRATION_MS:86400000}
```

Puntos importantes:
- en local puede usar H2 para testing rápido
- en Docker debe apuntar a PostgreSQL
- JWT secret no debe quedar hardcodeado en producción
- Fijarse siempre la URL del datasource y el driver correcto

## Cómo levantar la app

### Opción 1: local con Maven

```bash
cd tc-backend
./mvnw spring-boot:run
```

### Opción 2: con Docker + PostgreSQL

Desde la carpeta `docker`:

```bash
docker compose up --build -d
```

Esto levanta:
- PostgreSQL
- backend Java
- servicio API en `http://localhost:8080`

## Cómo probarlo

### Health

```bash
curl http://localhost:8080/api/health
```

Respuesta esperada:

```text
Backend TapeCloud SSO funcionando
```

### Version

```bash
curl http://localhost:8080/api/version
```

### Registro

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@tapecloud.com",
    "password": "123456",
    "name": "Admin",
    "lastName": "TapeCloud"
  }'
```

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@tapecloud.com",
    "password": "123456"
  }'
```

## Cómo se integra con React

La integración recomendada es:

- React hace login en el backend
- backend devuelve JWT
- frontend guarda el token en `localStorage` o memoria
- cada request autenticada envía `Authorization: Bearer ...`
- rutas protegidas se bloquean si no hay token válido

Flujo típico:

```text
React login form
   ↓
POST /api/auth/login
   ↓
JWT generado por backend
   ↓
React guarda token
   ↓
Siguiente request usa el token en headers
```

Esto exige:
- CORS habilitado en backend
- frontend en puerto distinto al backend
- headers correctos en requests
- manejo de errores de autenticación

## Roadmap recomendado

1. validar registro y login con JWT end-to-end
2. agregar manejo robusto de errores
3. definir roles y permisos más claros
4. crear endpoints para usuarios/admin
5. agregar refresh token
6. preparar integración con React
7. pruebas de integración reales
8. seguridad y despliegue en entorno productivo

## Punto clave del proyecto

Este backend ya está orientado a una API REST moderna, no a una app web tradicional con sesiones del servidor. Eso lo hace compatible con React, apps móviles y sistemas distribuidos.

Es un buen punto de partida para continuar desarrollando un SSO real y escalable.
