# TapeCloud Platform

> **Plataforma de identidad y acceso centralizado para el ecosistema Tape.**

TapeCloud Platform es un ecosistema de aplicaciones compuesto por un proveedor de identidad centralizado (**Single Sign-On**) y múltiples aplicaciones cliente independientes.

En lugar de que cada aplicación gestione sus propios usuarios, credenciales y sesiones, TapeCloud actúa como el punto central de autenticación y autorización, permitiendo que un usuario inicie sesión una única vez y acceda únicamente a las aplicaciones para las que posee permisos.

Este proyecto fue desarrollado como trabajo final integrador con el objetivo de simular una arquitectura inspirada en sistemas empresariales reales y servir como proyecto de portfolio.

---

# Visión General

El ecosistema está compuesto por tres aplicaciones principales:

```
                  TapeCloud Platform
        ┌────────────────────────────────┐
        │  Identidad y Gestión de Acceso │
        │  Autenticación Centralizada    │
        │  Portal de Aplicaciones        │
        └──────────────┬─────────────────┘
                       │
          ┌────────────┴────────────┐
          │                         │
     TapeFlix                  TapeBeat
 Películas y Series              Música
```

Cada aplicación se especializa en un dominio específico y delega completamente la autenticación en TapeCloud.

---

# ¿Qué es TapeCloud?

TapeCloud no es únicamente un sistema de login.

Es el centro del ecosistema y el responsable de administrar la identidad de todos los usuarios.

Entre sus responsabilidades se encuentran:

* Autenticación centralizada (SSO)
* Administración de usuarios
* Administración de roles y permisos
* Registro de aplicaciones cliente
* Gestión de sesiones
* Cambio obligatorio de contraseña
* Portal de aplicaciones
* Control de acceso entre sistemas

Después de iniciar sesión, el usuario accede a un portal desde el cual puede visualizar únicamente las aplicaciones habilitadas para su cuenta.

---

# Aplicaciones del Ecosistema

## TapeCloud

Centro de identidad y acceso del ecosistema.

Responsabilidades:

* Login único
* Gestión de usuarios
* Gestión de roles
* Gestión de permisos
* Registro de aplicaciones
* Administración de sesiones
* Portal de aplicaciones

---

## TapeFlix

Aplicación dedicada a la gestión y exploración de películas y series.

Incluye funcionalidades como:

* Catálogo
* Búsqueda
* Filtros
* Página de detalle
* Favoritos
* Estado de visualización

Toda la autenticación es realizada por TapeCloud.

---

## TapeBeat

Aplicación dedicada a la gestión y exploración de música.

Incluye funcionalidades como:

* Catálogo de álbumes y artistas
* Búsqueda
* Filtros
* Favoritos
* Estado de escucha
* Página de detalle

Al igual que TapeFlix, no administra usuarios propios.

---

# Flujo de Autenticación

```text
Usuario
   │
   ▼
TapeFlix / TapeBeat
   │
No existe una sesión válida
   │
   ▼
TapeCloud
(Login)
   │
Credenciales válidas
   │
   ▼
Se genera la sesión
   │
   ▼
Redirección automática
   │
   ▼
Aplicación cliente
```

El usuario inicia sesión una sola vez y puede acceder a todas las aplicaciones habilitadas sin volver a autenticarse.

---

# Principios de la Arquitectura

La plataforma sigue un modelo **Hub & Spoke**, donde TapeCloud actúa como núcleo del ecosistema.

```
                   TapeCloud
              Identity Provider
                     │
      ┌──────────────┼──────────────┐
      │                             │
 TapeFlix                      TapeBeat
 Aplicación Cliente        Aplicación Cliente
```

Esta arquitectura permite:

* Centralizar la autenticación.
* Mantener desacopladas las aplicaciones.
* Escalar el ecosistema incorporando nuevos sistemas.
* Evitar duplicación de usuarios y credenciales.

---

# Tecnologías

## Backend

* Spring Boot
* Spring Security
* Spring Data JPA
* JWT
* PostgreSQL

## Frontend

* React
* TypeScript
* Tailwind CSS

## Infraestructura

* Docker
* Docker Compose

---

# Objetivos del Proyecto

* Implementar un sistema de autenticación centralizado.
* Simular una arquitectura empresarial desacoplada.
* Gestionar usuarios, roles y permisos desde un único punto.
* Integrar múltiples aplicaciones cliente bajo un mismo ecosistema.
* Desarrollar un proyecto orientado a portfolio.

---

# Estructura del Proyecto

```
tapecloud-platform

├── tapecloud-sso-backend
├── tapecloud-sso-frontend
├── tapeflix
└── tapebeat
```

---

# Escalabilidad

La arquitectura fue diseñada para permitir la incorporación de nuevas aplicaciones sin modificar las existentes.

En el futuro podrían agregarse nuevos dominios como:

* TapeBooks
* TapeGames
* TapePhotos

Todas compartirían el mismo sistema de autenticación y gestión de identidad proporcionado por TapeCloud.

---

# Licencia

Proyecto desarrollado con fines educativos y como portfolio personal.
