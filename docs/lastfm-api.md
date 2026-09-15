# Last.fm API — Guía de endpoints para TapeBeat

Documentación práctica de qué se puede pedir a la API de Last.fm, qué devuelve cada endpoint y qué tan completa es la información. Todos los ejemplos fueron probados contra la API real.

## Configuración base

| Item | Valor |
|---|---|
| Base URL | `https://ws.audioscrobbler.com/2.0/` |
| Autenticación | `api_key` como query param (solo lectura) |
| Formato | `format=json` (por defecto devuelve XML) |
| Variables en el proyecto | `LASTFM_API_KEY`, `LASTFM_BASE_URL` en `.env` |

Estructura de una petición:

```
https://ws.audioscrobbler.com/2.0/?method=<METODO>&api_key=<KEY>&format=json&<params>
```

El `Shared secret` (`LASTFM_SHARED_SECRET`) **no se usa** para nada de lo documentado acá. Solo hace falta para escritura (scrobbling, amar temas, sesiones de usuario), que requiere firmar cada request con MD5.

---

## Endpoints implementados en TapeBeat

### `chart.getTopTracks` — canciones más escuchadas (global)

```
?method=chart.gettoptracks&api_key=KEY&format=json&page=1&limit=20
```

Devuelve por track: `name`, `artist.name`, `artist.mbid`, `mbid`, `url`, `duration`, `playcount`, `listeners`, `image[]`.

```
Ain't In LA        ADÉLA           197.494 oyentes    2.586.414 reproducciones
the cure           Olivia Rodrigo  860.526 oyentes   14.473.745 reproducciones
Earrings           Malcolm Todd  1.173.757 oyentes   16.153.640 reproducciones
```

> ⚠️ **El `image[]` de este endpoint es inútil.** Siempre devuelve el hash `2a96cbd8b46e442fc41c2b86b821562f`, que es el placeholder gris de Last.fm. Para la portada real hay que llamar a `track.getInfo` por cada track (ver abajo).

Paginación: `@attr` indica `totalPages: 2000` y `total: 10000` tracks disponibles.

### `track.getInfo` — detalle de una canción

```
?method=track.getInfo&api_key=KEY&format=json&artist=Queen&track=Bohemian+Rhapsody
```

Devuelve: `name`, `duration`, `listeners`, `playcount`, `album` (con `title` + `image[]` **real**), `toptags`, `wiki` (`summary` y `content`).

Es la fuente de las portadas en TapeBeat. El campo `album.image[]` sí trae carátulas distintas por canción.

---

## Endpoints disponibles no implementados

### Búsqueda

| Método | Devuelve |
|---|---|
| `track.search` | Tracks por título, ordenados por relevancia, con `listeners` y `mbid` |
| `artist.search` | Artistas por nombre |
| `album.search` | Álbumes por título |

Ejemplo de `track.search&track=bohemian+rhapsody`:

```
Bohemian Rhapsody   Queen                 2.310.917 oyentes
Bohemian Rhapsody   Pentatonix               30.315 oyentes
Bohemian Rhapsody   Panic! at the Disco     137.665 oyentes
```

Incluye covers de otros artistas, útil para desambiguar.

### Charts y rankings

| Método | Descripción |
|---|---|
| `chart.getTopTracks` | Top global de canciones |
| `chart.getTopArtists` | Top global de artistas |
| `chart.getTopTags` | Tags más populares del momento |
| `geo.getTopTracks` | Top por país (`country=argentina`) |
| `geo.getTopArtists` | Top de artistas por país |
| `tag.getTopTracks` | Top de canciones de un género |
| `tag.getTopArtists` | Top de artistas de un género |
| `artist.getTopTracks` | Canciones más escuchadas de un artista |
| `artist.getTopAlbums` | Álbumes más escuchados de un artista |

> ⚠️ **`geo.getTopTracks` no es un chart local real.** Consultando `country=argentina` devuelve prácticamente el mismo listado que el chart global (ADÉLA, Olivia Rodrigo), no música argentina. Para contenido regional, `tag.getTopTracks&tag=rock+argentino` funciona mucho mejor.

### Información y contexto

| Método | Devuelve |
|---|---|
| `artist.getInfo` | Bio, stats (`listeners`, `playcount`), tags, artistas similares |
| `album.getInfo` | Tracklist completo, stats, tags, wiki, portada |
| `tag.getInfo` | Descripción del género y total de taggings |
| `artist.getSimilar` | Artistas similares con score de afinidad (0-1) |
| `track.getSimilar` | Canciones similares |

---

## Alcance real de la información

### Cobertura de géneros: muy amplia, incluidos nichos

Los tags son puestos por la comunidad, así que existen géneros muy específicos. Volumen de taggings medido:

| Género | Taggings |
|---|---|
| shoegaze | 229.091 |
| bossa nova | 71.266 |
| math rock | 65.285 |
| reggaeton | 29.657 |
| vaporwave | 18.553 |
| rock argentino | 12.683 |
| cumbia | 9.123 |
| phonk | 7.471 |
| sertanejo | 7.021 |
| city pop | 3.143 |

Top de tags globales por alcance: `rock` (403k), `alternative` (267k), `electronic` (262k), `indie` (260k), `pop` (234k), `alternative rock` (170k), `female vocalists` (169k), `metal` (159k).

> Ojo: los tags **no son solo géneros**. Aparecen cosas como `seen live` (82k), `female vocalists` o `design`, porque es folksonomía libre. Si se usan para categorizar, conviene filtrar contra una lista blanca.

`tag.getTopTracks&tag=rock+argentino` sí devuelve resultados coherentes:

```
Mil Horas                      Los Abuelos de la Nada
Seguir Viviendo Sin Tu Amor    Luis Alberto Spinetta
Puente                         Gustavo Cerati
Flaca                          Andrés Calamaro
```

### Cobertura de descripciones: despareja

**A nivel canción, la mayoría no tiene wiki ni tags.** Probado con Gustavo Cerati:

| Canción | Tags | Wiki |
|---|---|---|
| Cosas Imposibles | vacío | vacío |
| Crimen | vacío | vacío |
| Puente | vacío | vacío |
| De Música Ligera | vacío | vacío |

En cambio, `Bohemian Rhapsody` sí trae wiki completa y tags (`classic rock, rock, Queen, 70s, 80s`). Las wikis de canciones existen casi solo para temas anglo muy conocidos.

**A nivel artista y álbum la cobertura es mucho mejor.** `artist.getInfo` de Gustavo Cerati:

- `listeners`: 551.892 · `playcount`: 42.511.203
- `tags`: Rock Argentino, rock, argentina, Rock en Espanol, Rock Latino
- `bio`: biografía completa (nacimiento, rol en Soda Stereo, fallecimiento, etc.)

`album.getInfo` de *Bocanada*:

- `listeners`: 314.661 · `playcount`: 12.072.091 · 15 tracks con tracklist
- `tags`: rock argentino, rock en español, trip hop, design, downtempo
- `wiki`: reseña del álbum

**Recomendación:** para mostrar descripciones, usar cascada `track.wiki` → `album.wiki` → `artist.bio`.

### Idioma

El parámetro `lang=es` existe pero **la mayoría de los textos vuelven en inglés igual**. La bio de Cerati pedida con `lang=es` devolvió texto en inglés. Las traducciones de Last.fm son muy limitadas.

### Datos numéricos

`listeners` y `playcount` están disponibles de forma consistente en todos los niveles (track, álbum, artista, tag) y son datos reales de escuchas acumuladas — el punto más fuerte de Last.fm frente a MusicBrainz.

---

## Comparación con MusicBrainz

| Aspecto | Last.fm | MusicBrainz |
|---|---|---|
| Popularidad (listeners/playcount) | ✅ Datos reales | ❌ No trackea escuchas |
| Portadas | ✅ Vía `track.getInfo` | ⚠️ Vía Cover Art Archive (aparte) |
| Descripciones | ⚠️ Solo artistas/álbumes populares | ❌ No tiene prosa |
| Géneros | ⚠️ Tags libres, con ruido | ✅ Géneros curados y granulares |
| Metadata precisa (ISRC, duración, releases) | ❌ Limitada | ✅ Muy completa |
| Rate limit | Generoso | Estricto (1 req/seg, 503 frecuentes) |

Ejemplo del contraste con *Bohemian Rhapsody*:

- **Last.fm**: 2.3M oyentes, 16.6M reproducciones, 5 tags, wiki descriptiva
- **MusicBrainz**: 14 géneros curados (`alternative rock, arena rock, art rock, baroque pop, classic rock, glam, glam rock, hard rock, heavy metal, pop, pop rock, progressive rock, rock, rock opera`), 7 códigos ISRC, duración exacta, todos los releases donde aparece

Los tracks de Last.fm ya incluyen `mbid`, así que se pueden cruzar con MusicBrainz sin búsqueda adicional.

---

## Límites y buenas prácticas

- **Rate limit**: no está documentado oficialmente pero es holgado. La recomendación de la comunidad es ~5 req/seg máximo. Conviene espaciar las llamadas en syncs masivos.
- **Costo de las portadas**: enriquecer con `track.getInfo` implica **2 requests por canción** (chart + info). Con `limit` alto el sync se vuelve lento.
- **Nombres con acentos/espacios**: hay que URL-encodear (`Gustavo%20Cerati`). Last.fm es tolerante con mayúsculas.
- **Errores**: devuelve HTTP 200 con un body `{"error": N, "message": "..."}`. Hay que validar el body, no solo el status.
- **`mbid` puede venir vacío**: en TapeBeat se usa `artista-titulo` como `externalId` de fallback.

## Endpoints que requieren autenticación de usuario

Necesitan `LASTFM_SHARED_SECRET` + firma MD5 + sesión OAuth. No implementados:

`track.scrobble`, `track.updateNowPlaying`, `track.love`, `track.unlove`, `album.addTags`, `user.getRecentTracks` (de usuarios privados), `library.*`.
