# tapecloud-auth-core

Backend central de TapeCloud basado en Java, Spring Boot y Maven.

## Responsabilidad

Este repositorio será la fuente central para:

- autenticación y registro
- usuarios, roles y permisos
- tokens y sesiones
- contenido compartido
- reseñas y comentarios
- perfiles y métricas
- APIs consumidas por TapeCloud Portal, TapeBeat y TapeFlix

La información común del ecosistema debe almacenarse aquí, no en bases de datos separadas para cada aplicación cliente.

## Estado actual

El proyecto contiene el esqueleto inicial de Spring Boot, configuración Maven, propiedades de aplicación y un endpoint de salud. La implementación de autenticación, persistencia y las APIs de contenido se incorporará progresivamente.

## Tecnologías

- Java 21
- Spring Boot
- Maven
- Spring Web
- Persistencia y seguridad, según avance la implementación

## Desarrollo

La rama de desarrollo es `develop`.

```powershell
./mvnw.cmd test
./mvnw.cmd spring-boot:run
```

El entorno necesita un JDK 21 configurado. No se deben subir credenciales ni archivos generados.
