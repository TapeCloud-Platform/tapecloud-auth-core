# 🐳 Docker Setup - TapeCloud SSO Backend

## Requisitos
- Docker
- Docker Compose

## Inicio Rápido

### Opción 1: Usar Docker Compose (Recomendado)

```bash
# Clonar o entrar al directorio del proyecto
cd tc-backend

# Crear archivo .env (opcional, si quieres usar valores personalizados)
cp docker/.env.example .env

# Construir y levantar los servicios
docker-compose -f docker/docker-compose.yml up --build

# El backend estará disponible en: http://localhost:8080
# PostgreSQL en: localhost:5432
```

### Opción 2: Solo la Base de Datos (Desarrollo Local)

Si prefieres ejecutar el backend localmente:

```bash
# Levantar solo PostgreSQL
docker-compose -f docker/docker-compose.yml up postgres

# El backend se conectará a: jdbc:postgresql://localhost:5432/tapecloud
# Usuario: tapecloud / Contraseña: secret
```

## Variables de Entorno

Crea un archivo `.env` en la raíz del proyecto:

```env
POSTGRES_DB=tapecloud
POSTGRES_USER=tapecloud
POSTGRES_PASSWORD=secret
SERVER_PORT=8080
```

## Comandos Útiles

```bash
# Ver logs de los servicios
docker-compose -f docker/docker-compose.yml logs -f

# Ver logs del backend
docker-compose -f docker/docker-compose.yml logs -f backend

# Ver logs de PostgreSQL
docker-compose -f docker/docker-compose.yml logs -f postgres

# Detener los servicios
docker-compose -f docker/docker-compose.yml down

# Detener y eliminar volúmenes (borra base de datos)
docker-compose -f docker/docker-compose.yml down -v

# Reiniciar servicios
docker-compose -f docker/docker-compose.yml restart

# Construir imagen sin ejecutar
docker-compose -f docker/docker-compose.yml build
```

## Acceso a la Base de Datos

### Desde la máquina host:
```bash
psql -h localhost -U tapecloud -d tapecloud
# Contraseña: secret
```

### Desde dentro del contenedor:
```bash
docker exec -it tapecloud-postgres psql -U tapecloud -d tapecloud
```

## Puertos

- **Backend**: 8080
- **PostgreSQL**: 5432

## Solución de Problemas

### Puerto ya está en uso
```bash
# Cambiar el puerto en docker/docker-compose.yml
# Línea: "8080:8080" → "8081:8080"
```

### Base de datos corrupta
```bash
# Eliminar volúmenes y reiniciar
docker-compose -f docker/docker-compose.yml down -v
docker-compose -f docker/docker-compose.yml up --build
```

### Problemas de permisos en Linux
```bash
sudo usermod -aG docker $USER
newgrp docker
```

## Estructura del Dockerfile

- **Stage 1**: Builder - Compila la aplicación con Maven
- **Stage 2**: Runtime - Imagen ligera con JRE basada en Alpine

Este enfoque optimiza el tamaño de la imagen final.
