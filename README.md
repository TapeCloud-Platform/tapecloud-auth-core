# TapeCloud SSO Backend

Este repositorio corresponde al backend del sistema de identidad y acceso centralizado de TapeCloud. La intención actual es crear la base del proveedor de identidad que luego servirá para autenticar a las aplicaciones cliente del ecosistema.

## Indice del repositorio

Dentro de `tc-backend`, la estructura actual es:

```
tc-backend/
├── docker/
│   ├── .dockerignore
│   ├── .env.example
│   ├── DOCKER.md
│   ├── Dockerfile
│   └── docker-compose.yml
├── src/
│   └── main/
│       ├── java/com/tapecloud/sso/
│       │   ├── api/
│       │   │   └── HealthController.java
│       │   ├── config/
│       │   │   └── SecurityConfig.java
│       │   └── user/
│       │       ├── entity/
│       │       │   ├── AppUser.java
│       │       │   ├── Permission.java
│       │       │   └── Role.java
│       │       └── repository/
│       │           ├── AppUserRepository.java
│       │           ├── PermissionRepository.java
│       │           └── RoleRepository.java
│       └── resources/
│           └── application.properties
├── HELP.md
├── pom.xml
├── mvnw
├── mvnw.cmd
├── README.md
├── target/               # generado por Maven
└── .gitignore
```

## Estado actual del backend

El proyecto ya tiene una base funcional inicial:

- Spring Boot configurado como aplicación backend.
- Seguridad web inicial con Spring Security.
- Endpoint de salud disponible en `/api/health` y `/api/version`.
- Entidades de usuarios, roles y permisos definidas.
- Repositorios JPA para usuarios, roles y permisos.
- Configuración del datasource lista para PostgreSQL.
- Configuración de contenedores Docker lista para compilar y correr la app.

La parte de autenticación real todavía no está implementada en profundidad. Actualmente hay una base estructural, pero falta completar la lógica de login, registro, autorización y JWT.

## Que hace esta aplicacion en este momento

El backend actual sirve como base del sistema de identidad. Su objetivo inmediato es proporcionar:

- almacenamiento y gestión de usuarios
- modelado de roles y permisos
- endpoints base de salud y validación de servicio
- base para autenticación centralizada
- preparación para integración con frontend y otras aplicaciones del ecosistema

## Orden de prioridad actual

La prioridad recomendada para continuar es la siguiente:

1. Definir el flujo de autenticación completo
  - Login
  - Registro
  - Logout
  - Recuperación o reset de contraseña
  - Manejo de errores de autenticación

2. Implementar seguridad real con JWT o session-based
  - Generación de token
  - Validación por filtro
  - Roles y permisos por request
  - Protección de endpoints sensibles

3. Completar la capa de servicio y DTOs
  - `AuthService`
  - `UserService`
  - `RoleService`
  - `PermissionService`
  - modelos de entrada/salida

4. Definir endpoints REST de usuarios y administración
  - crear usuario
  - listar usuarios
  - activar/desactivar usuario
  - asignar roles
  - consultar permisos

5. Consolidar la persistencia y migración de datos
  - validar esquema de base de datos
  - definir datos iniciales de roles
  - revisar `ddl-auto` y estrategia de migraciones

6. Preparar la capa de pruebas
  - pruebas unitarias de servicio
  - pruebas de controladores
  - pruebas de seguridad

7. Revisar y dejar operativo Docker y entorno local
  - `.env` real a partir de `.env.example`
  - validación de compose
  - levantar cliente y base de datos
  - verificar integración completa

8. Documentar la API y la arquitectura
  - endpoints disponibles
  - ejemplos de requests/responses
  - variables de entorno
  - flujo de integración frontend

## Estructura funcional por carpetas

### `src/main/java/com/tapecloud/sso/api`
Contiene los controladores HTTP. Actualmente tiene el controlador de salud, que es la base para validar que la aplicación responde correctamente.

### `src/main/java/com/tapecloud/sso/config`
Contiene la configuración de la aplicación. Aquí está la configuración de seguridad principal y la definición del encoder de contraseñas.

### `src/main/java/com/tapecloud/sso/user/entity`
Contiene las entidades JPA del dominio de usuarios. En esta etapa están definidos:

- `AppUser`
- `Role`
- `Permission`

### `src/main/java/com/tapecloud/sso/user/repository`
Contiene los repositorios de acceso a datos para usuarios, roles y permisos.

### `src/main/resources/application.properties`
Configura la conexión a PostgreSQL y basic settings del backend.

### `docker/`
Contiene la configuración del entorno de ejecución en contenedores:

- `Dockerfile` para compilar la app Java
- `docker-compose.yml` para levantar PostgreSQL y backend
- `.env.example` para variables de entorno
- `DOCKER.md` con documentación específica del entorno Docker

## Recomendaciones de desarrollo

- Mantener la lógica de autenticación centralizada en el backend del SSO.
- No duplicar usuarios ni credenciales entre aplicaciones cliente.
- Usar roles y permisos como base para la autorización.
- Separar claramente entidades, repositorios, servicios y controladores.
- Documentar cada endpoint antes de integrarlo con frontend.

## Estado de Docker

La configuración Docker ya fue verificada sintácticamente, sin levantar el stack. Esto significa que la estructura y el archivo Compose están bien armados para continuar con la ejecución local, pero todavía falta la preparación del entorno real (`.env`) y la validación con una ejecución real del contenedor.

## Siguiente paso recomendado

El paso lógico siguiente no es seguir agregando features sin base, sino cerrar la autenticación real y dejar la aplicación operando sobre PostgreSQL con endpoints seguros. Si se hace en orden, el proyecto pasa de una base de arquitectura a un backend de identidad funcional.

## Resumen corto

Este repositorio está en una etapa inicial de infraestructura y modelo de dominio, con una base Spring Boot y Spring Security armada. El siguiente objetivo principal es completar la autenticación, la autorización y la integración con PostgreSQL para convertirlo en un SSO funcional.

# Licencia

Proyecto desarrollado con fines educativos y como portfolio personal.
