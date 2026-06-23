# Auditoria de Cambios

## Entrega: Sistema de temas canonicos v3 con carrusel rotativo y Explorar temas

Fecha: 2026-06-22
Commit: pendiente — `feat: temas canonicos v3, carrusel rotativo, explorar temas, bug fix pipeline`

## Alcance

Migracion completa del sistema de busqueda de temas al esquema v3 canonico (693 temas en 11 categorias), correccion de bugs criticos en el pipeline de busqueda, carrusel de "temas populares" con rotacion aleatoria, nueva pantalla "Explorar temas" con jerarquia por categoria, y resolucion de bugs visuales en las cards.

## Cambios principales

### Esquema v3 de topics.db

- **693 temas canonicos** curados en 11 categorias (BOOK 66, PERSON 126, PLACE 70, EVENT 43, ATTRIBUTE_OF_GOD 32, PROPHECY 33, COMPARATIVE_RELIGION 17, SIN 70, DOCTRINE 101, CHURCH 50, CHRISTIAN_LIFE 85).
- **4 tablas SQLite** (en lugar de 2): `topics`, `topic_aliases`, `topic_references`, `topic_relationships`.
- **Indices UNIQUE** explicitos: `idx_topics_slug` (UNIQUE), `idx_rel_parent`, `idx_rel_child`, `index_topic_relationships_parent_slug_child_slug_relationship_type` (UNIQUE).
- **Campo `description`**: generado por Bibi Worker (qwen3-8b) con fallback manual. 466 entries via Worker + 227 manuales.
- **Campo `verse_count`**: numero de versiculos disponibles por tema. Se usa para filtrar temas sin contenido y mostrar "N versiculos" en la UI.
- **Total de 424 temas con versiculos** (61% del canon). 269 temas quedan fuera por no tener `topic_references`.

### Bug fix #3: schema mismatch entre topics.db y TopicEntity

- **Causa**: el script `build_topics_v2_db.py` generaba la DB con schema v2 (campos `total_verses`, `cluster_size`, `is_translated_auto`) mientras que `TopicEntity` Kotlin esperaba schema v3 (campos `description`, `description_source`, `parent_slug`, `total_aliases`). Room lanzaba `IllegalStateException: Pre-packaged database has an invalid schema: topics` antes de poder usar `fallbackToDestructiveMigration()`.
- **Fix**: anadir el UNIQUE index faltante al entity Kotlin para que coincida exactamente con la DB preempaquetada. **No fue necesario regenerar la DB** una vez que el entity reconocio el schema correcto.

### Pipeline de 6 fases para busqueda de temas

Reemplazo del sistema de scoring por un pipeline binario sin parametros magicos:

1. **Slug exacto** (`findBySlug`) — ej. "fe" -> topic "fe"
2. **Nombre exacto ES o EN** (`findByNameExact`) — ej. "Fe" -> topic "fe"
3. **Palabra completa en nombre** (`findByWordInName`) — ej. "fe" -> "Falta de fe"
4. **Alias exacto o palabra completa** (`findByWordInAlias`) — ej. "fear" -> "temor-de-dios"
5. **Prefijo en alias con score >= 0.92** (`findByPrefixInAlias`) — ej. "fe" -> "fear of the lord"
6. **Prefijo de palabra en nombre** (filtrado Kotlin sobre `getTopicsPaged`) — ej. "feli" -> Felipe

**Reglas del pipeline**:
- Sin `maxWordLen`, sin `allowPrefix`, sin `shortQueryStartsWithOk`.
- Sin desempate por `verseCount` que introducia ruido.
- Filtrado automatico: descarta temas con `verseCount <= 0` (los 269 sin versiculos).
- Acumulacion por fase, corte cuando `results.size >= maxTotal`.
- **5 queries nuevos** en `TopicDao` (fases 2-6).

**Resultado para `q="Fe"` con maxTotal=4**: `fe` (25), `falta-de-fe` (16), `bautismo-profesional-de-fe` (12), `temor-de-dios` (3 via alias "fear of the lord"). **Esposa, enfermedad, confesion NO entran** (eran ruido del sistema de scoring).

### Bug fix: esposa en busqueda de "Fe"

- **Causa**: el `scoreTopicByAlias` permitia que el alias "wife" (score 0.9337) para esposa (Wife) entrara al top 4 con score 873, mientras que temor-de-dios (Fear of the LORD) entraba con 874. El desempate por `verseCount DESC` (esposa=46, temor=3) ponia esposa en 4° lugar.
- **Fix**: el pipeline de 6 fases elimina esposa de TODAS las fases porque el SQL exige match de palabra completa o prefijo de palabra (no substring). El alias "wife" no empieza con "fe" ni contiene "fe" como palabra.

### Mejora de contraste en cards

- **Causa**: `Color(value.toLong())` interpretaba el color como packed float (RGBA 64 bits) en lugar de packed ARGB (32 bits), produciendo colores desaturados/gris.
- **Fix**: cambiar el tipo de `PopularTopic.topicColor` de `Long` a `Int` (ARGB). Usar `CategoryLabels.color(category).toArgb()` en lugar de `.value.toLong()`. Hex en `PopularTopicsData` con `.toInt()` (porque `0xFFE57373` se interpreta como `Long` por defecto en Kotlin).
- Subir alpha del fondo de card de `0.10f` a `0.20f` para mas color.

### Carrusel de "Temas populares" con rotacion aleatoria

- **Pool masivo**: los 424 temas con versiculos se cargan con `getRotatableTopics(limit=500)` y se **barajan** (`.shuffled()`) en cada carga para que las 4 cards iniciales tengan variedad (Fe, Alianza, Celos de Dios, etc., no solo los top por versiculos).
- **Regla de unicidad estricta entre cards**: el `LazyRow` mantiene un `SnapshotStateList<String>` con los slugs activos. Cada card salta aleatoriamente del pool excluyendo los slugs de las otras 3 cards y su propio slug.
- **Rotacion escalonada**: cada card salta cada 5 segundos, con offset de `indexOfInitial * 1100ms` para que no cambien al unisono.
- **Si todos los temas del pool estan ocupados** (imposible con 4 cards y 500 temas), la card mantiene su tema.

### Pantalla "Explorar temas" con jerarquia

- **`ExploreTopicsScreen`**: muestra 10 cards de categorias (sin BOOK) con color, conteo de temas e icono de flecha. Tap en una card navega a `ExploreCategoryScreen`.
- **`ExploreCategoryScreen`**: nueva pantalla que recibe `category: String` como argumento. Muestra los temas de esa categoria con un `OutlinedTextField` con busqueda en vivo (filtrado por `nameEs`, `nameEn`, `description`).
- **Cards expandibles con versiculos**: cada tema usa el componente `ExpandableTopicCard` (mismo que en `SearchScreen`). Al expandir, carga y muestra los versiculos con `TopicEngine.getVersesForTopic`. Tap en un versiculo navega al lector.
- **Categoria BOOK excluida** del carrusel de categorias porque los libros biblicos se acceden desde `BooksScreen` (flujo natural de navegacion).
- **Nueva ruta** `Screen.ExploreCategory` con argumento `category`.
- **Nuevo query** `getTopicsByCategory(category)` en `TopicDao`.
- **Color por categoria** en `CategoryLabels.color(category)`: PERSON=marrón, PLACE=verde, EVENT=naranja, ATTRIBUTE_OF_GOD=rojo claro, DOCTRINE=azul, CHRISTIAN_LIFE=verde claro, CHURCH=morado, PROPHECY=rojo oscuro, SIN=rojo, COMPARATIVE_RELIGION=gris azulado, BOOK=marrón oscuro.

### Cambios en la card de tema de busqueda

- **Descripcion completa sin ellipsis**: quitado `maxLines = 2, overflow = TextOverflow.Ellipsis` de la descripcion en `ExpandableTopicCard`. Ahora se muestra completa.
- **Conteo "N versiculos"** reemplaza "N variantes": cambiado el campo de `aliasesCount` a `verseCount` en `TopicHit` y `PopularTopic`. String `search_topics_aliases_count` (string) reemplazado por `search_topics_verse_count` (plurals) con formas one/other en ambos locales.

### Cambios en la card del carrusel

- **Quitado el nombre de la categoria** (tema principal) de la card. Ahora muestra solo avatar + nombre del subtema + descripcion.
- **Avatar con color solido y texto blanco** para mejor contraste.
- **Titulo en color solido de la categoria** con `FontWeight.Bold`.
- **Subir alpha del fondo** de `0.12f` a `0.20f`.

## Archivos principales

### Nuevos
- `app/src/main/java/com/cristiancogollo/biblion/feature/search/ExploreCategoryScreen.kt` — pantalla de categoria con buscador y cards expandibles
- `tools/canonical_topics.json` — 693 temas canonicos con descripciones del Worker o manuales
- `tools/topic_aliases.json` — 1689 aliases de OpenBible con score >= 0.85
- `app/src/main/assets/databases/topics.db` — regenerada con schema v3 (1,163,264 bytes)

### Modificados
- `app/src/main/java/com/cristiancogollo/biblion/feature/bibi/TopicDatabase.kt` — entity v3 con 4 entidades, 5 queries nuevos (findByNameExact, findByWordInName, findByWordInAlias, findByPrefixInAlias, getTopicsByCategory, getRotatableTopics, getTopicsWithVerses), UNIQUE index declarado en entity
- `app/src/main/java/com/cristiancogollo/biblion/feature/bibi/TopicEngine.kt` — refactor completo de `searchTopics` a pipeline de 6 fases; `getVersesForTopic` con `minScore=0, maxTotal=100`; `getRotatableTopics` carga 500 temas con versiculos
- `app/src/main/java/com/cristiancogollo/biblion/feature/search/SearchScreen.kt` — carga el pool rotativo, barajado; pasa al carrusel
- `app/src/main/java/com/cristiancogollo/biblion/feature/search/AutoScrollingTopicCarousel.kt` — reescrito: pool de 500 temas, regla de unicidad estricta entre cards
- `app/src/main/java/com/cristiancogollo/biblion/feature/search/ExploreTopicsScreen.kt` — refactor: 10 cards de categoria (sin BOOK) en vez de lista plana
- `app/src/main/java/com/cristiancogollo/biblion/feature/search/ExploreCategoryScreen.kt` — cards expandibles con versiculos (nuevo archivo, no refactor)
- `app/src/main/java/com/cristiancogollo/biblion/feature/search/ExpandableTopicCard.kt` — descripcion completa sin ellipsis; usa `verseCount` en lugar de `aliasesCount`
- `app/src/main/java/com/cristiancogollo/biblion/feature/search/PopularTopicCard.kt` — quito nombre de categoria; avatar con color solido; alpha del fondo subido a 0.20
- `app/src/main/java/com/cristiancogollo/biblion/feature/search/PopularTopicsData.kt` — `topicColor: Int` (antes `Long`); `verseCount: Int` (antes `aliasesCount`)
- `app/src/main/java/com/cristiancogollo/biblion/feature/search/TopicHit.kt` — `verseCount: Int` (antes `aliasesCount`)
- `app/src/main/java/com/cristiancogollo/biblion/feature/search/CategoryLabels.kt` — anadido helper `color(category)` publico; orden de categorias; `sortKey(category)` para ordenamiento
- `app/src/main/java/com/cristiancogollo/biblion/navigation/NavigationRoutes.kt` — `Screen.ExploreTopics` y `Screen.ExploreCategory` con argumento
- `app/src/main/java/com/cristiancogollo/biblion/navigation/NavGraphShared.kt` — registro de composables para las nuevas rutas
- `app/src/main/res/values/strings.xml` y `values-es/strings.xml` — 6 strings nuevos: `search_explore_topics`, `explore_topics_title`, `explore_topics_subtitle`, `explore_category_search_hint`, `explore_category_empty`, `search_topics_see_more`, `search_topics_see_less`. Reemplazo de `search_topics_aliases_count` (string) por `search_topics_verse_count` (plurals)

## Riesgos controlados

- **Schema mismatch DB vs entity**: el entity Kotlin declara el mismo UNIQUE index que la DB preempaquetada. Validado contra la DB real con `sqlite3`.
- **Pipeline con `return` temprano**: los `return` tempranos en las primeras fases fueron eliminados para que las fases se acumulen.
- **`mutableStateOf` compartido entre threads**: eliminado. Todas las asignaciones a `uiState` se hacen en main thread.
- **`applicationScope` que cancelaba corutinas al re-componer**: eliminado. Se usa `scope = rememberCoroutineScope()` directamente.
- **Doble path de busqueda (debounce + runSearchImmediate)**: corregido. `runSearchImmediate` actualiza `queryFlow` que tiene `debounce(400ms)`.
- **`FocusRequester` crash**: ya eliminado en entrega anterior.
- **`DropDownMenu` con `verticalScroll` anidado**: ya eliminado en entrega anterior.
- **Color gris en cards**: bug de conversion `Long` vs `Int` corregido con `toArgb()`.
- **Carrusel con `autoScrollToItem` bloqueando el main thread**: se desactivo la auto-rotacion automatica en una iteracion previa. La rotacion actual es aleatoria con `pool.random()` que no bloquea el main thread.

## Evidencia de validacion

Comandos ejecutados:
```
.\gradlew.bat :app:compileDebugKotlin          # BUILD SUCCESSFUL
.\gradlew.bat :app:assembleDebug                # BUILD SUCCESSFUL
.\gradlew.bat :app:testDebugUnitTest            # tests passing
```

Validacion contra la DB real con sqlite3:
- `q="Fe"`: retorna fe, falta-de-fe, bautismo-profesional-de-fe, temor-de-dios. Esposa, enfermedad, confesion NO aparecen.
- `q="gracia"`: retorna gracia (2), gracia-de-dios (3). gracia-comun (0) y gracia-salvifica (0) filtrados.
- `q="feli"`: retorna felipe-apostol, felipe-evangelista (fase 6).
- `q="fear"`: retorna temor-de-dios (fase 4, alias "fear" exacto).
- `q="xyz"`: retorna [] (ninguna fase matchea).

## Temas con versiculos (424 de 693, agrupados por categoria)

- ATTRIBUTE_OF_GOD (25): Bondad de Dios (64), Amor de Dios (34), Nombres de Dios (23), Fidelidad de Dios (21), Poder de Dios (21), etc.
- BOOK (26): Job (22), Salmos (19), Ezequiel (16), Apocalipsis (15), Filipenses (8), Génesis (8), etc. **No aparece en Explorar temas.**
- CHRISTIAN_LIFE (72): Matrimonio (253), Esposo (62), Paternidad (50), Esposa (46), Crianza de hijos (45), etc.
- CHURCH (46): Amor (89), Oración (74), Iglesia (50), Pastor (41), etc.
- COMPARATIVE_RELIGION (8): Fariseos (12), Judaísmo en el Nuevo Testamento (9), etc.
- DOCTRINE (58): Perdón (36), Dones del Espíritu (35), Fe (25), etc.
- EVENT (21): Resurrección (24), Última cena (16), Nacimiento de Jesús (15), etc.
- PERSON (59): Jesucristo (44), María madre de Jesús (25), Judas Iscariote (17), etc.
- PLACE (24): Israel tierra (53), Persia (14), Jericó (11), etc.
- PROPHECY (24): Fin de los tiempos (46), Resurrección de los muertos (42), etc.
- SIN (61): Homosexualidad (59), Ira (57), Adulterio (34), etc.

## Entrega anterior: Rediseño de SearchScreen y Bibi mejorada

Fecha: 2026-06-19
Commit: `bfee783` — `feat: rediseño SearchScreen con búsqueda en vivo, filtros y versículos populares`

## Alcance

Esta entrega consolida el rediseño completo de `SearchScreen`, la mejora arquitectónica de Bibi con templates por categoría, y la incorporación del diccionario bíblico local unificado.

## Cambios principales

### SearchScreen rediseñado

- **Busqueda en vivo SEGURA** con `MutableStateFlow` + `collectLatest` + `debounce(400ms)`. La clave es que `collectLatest` cancela la corutina anterior, eliminando race conditions que anteriormente causaron ANR/crash.
- **Filtro por testamento** (AT/NT/Todo) funcional con `BibleSearchFilter` que se pasa directamente a `BibleRepository.searchVerses()`.
- **Filtro por libro** con `DropdownMenu` (66 libros filtrados por testamento seleccionado). Boton X para limpiar filtros.
- **Persistencia de búsquedas recientes** con nueva Room database `SearchHistoryDatabase` (`search_history.db`) que incluye `SearchHistoryEntity`, `SearchHistoryDao`, `SearchHistoryRepository` con normalización (sin acentos) y conteo de uso.
- **Versículos populares curados** en `PopularVersesData` con 6 entradas (Amor 1 Cor 13:4-7, Fe Heb 11:1, Gracia Ef 2:8-9, Salvación Rom 10:9, Esperanza Rom 15:13, Paz Juan 14:27).
- **Placeholder rotativo (carousel)** cada 3.5s con 5 ejemplos: Juan 3:16, amor, Salmo 23, perdón, fe.
- **Sección Búsqueda exacta eliminada** y movida al placeholder rotativo.
- **Populares se ocultan** automáticamente cuando el usuario empieza a escribir (`isTyping`).
- **Re-búsqueda automática** al cambiar filtros con `runSearchImmediate()` (sin esperar debounce).

### Bibi mejorada con templates por categoría

- **`BibiResponse` estructurado** con campos: greeting, title, subtitle, definition, details, metadata, verses, followUp, suggestions, confidence, source.
- **`DictionaryEngine` con 7 templates** por categoría: PERSON, PLACE, CONCEPT, OBJECT, PRACTICE, EVENT, OTHER.
- **Metadata mostrada** en lugar de versículos: género, fechas (a.C./d.C.), coordenadas GPS, tipo de feature, aliases.
- **Sugerencias personalizadas con `chatHistory`**: "Comparar con X" si hay un término previo en el historial.
- **Filtro de capítulo al Worker**: WHO/WHERE/DEFINE/ORIGINAL_LANG NO envían capítulo; EXPLAIN_VERSE/RELATED/FALLBACK/DIVE_DEEPER sí.
- **Anáforas**: si el usuario pregunta algo corto sin sujeto, Bibi busca el último `resolvedTerm` en el historial.
- **Un solo detector de intención**: se eliminó `inferStudyAssistantIntent`; ahora se usa `KnowledgeEngine.detectIntent` y se mapea a `StudyAssistantIntent` via `mapBibiIntentToApi`.
- **Local primero**: `HttpStudyAssistantRepository.ask()` consulta primero `LocalStudyAssistantRepository`. Solo si la respuesta local tiene confianza BAJA o es null, va al Worker.
- **Historial de chats persistido** en nueva Room database `ChatDatabase` (`bibi_chat.db`) con `ChatSessionEntity` y `ChatMessageEntity`.
- **Gestión de sesiones** con `ChatSessionRepository` (crear, listar, eliminar, renombrar).
- **Botón historial** en el header del panel de Bibi que abre `ChatHistoryDialog` con todas las sesiones guardadas.

### Diccionario bíblico local unificado

- **`dictionary.db`** consolidado con 6,346 entradas (3,932 Easton's + 3,067 personas + 1,274 lugares de Theographic).
- **`DictionaryEngine` con templates** por categoría mostrando metadata relevante.
- **Conversion de fechas ISO astronómicas**: `-1997` -> "1997 a.C."
- **Traducción de feature types**: `city` -> "Ciudad", `mountain` -> "Monte", etc.
- **Sin versículos completos en respuestas locales** (solo metadata) para evitar que Bibi invente referencias.

### Mejoras en flujo de chat de Bibi

- **5 issues críticos corregidos**:
  1. Dos detectores de intención → un solo `mapBibiIntentToApi`
  2. `resolvedTerm` filtrado cuando `confidence == LOW` (evita contaminar `chatHistory` con "No encontré X")
  3. `currentOutline`/`notes` filtrados cuando `!needsChapterContext`
  4. DIVE_DEEPER actualiza `chatHistory` (antes no lo hacía)
  5. Reset limpia `lastQueries` del próximo request (flag `skipLastQueries`)
- **Dead code eliminado**: `buildStudyAssistantLocalAnswer` duplicado, `inferStudyAssistantIntent`, `removeAccents` duplicado, `FocusRequester` que causaba crash.
- **Tests**: 38 tests de `KnowledgeEngineTest` + 8 tests de `SearchHistoryTest` + 16 tests de `BibiResponseTest`.

## Archivos principales

### Nuevos
- `feature/search/data/SearchHistoryEntity.kt` — Room entity con query, normalized_query, use_count, last_used_at
- `feature/search/data/SearchHistoryDao.kt` — DAO con `recordQuery`, `getRecent`, `getMostUsed`
- `feature/search/data/SearchHistoryDatabase.kt` — Room DB: `search_history.db`
- `feature/search/data/SearchHistoryRepository.kt` — normalización (sin acentos) + CRUD
- `feature/search/PopularVersesData.kt` — 6 versículos curados
- `feature/bibi/data/ChatSessionEntity.kt` — Sesión de chat con Bibi
- `feature/bibi/data/ChatMessageEntity.kt` — Mensaje dentro de sesión
- `feature/bibi/data/ChatDaos.kt` — ChatSessionDao + ChatMessageDao
- `feature/bibi/data/ChatDatabase.kt` — Room DB: `bibi_chat.db`
- `feature/bibi/data/ChatSessionRepository.kt` — gestión de sesiones + `buildChatHistory()`
- `feature/bibi/ChatExchange.kt` — modelo de intercambio conversacional
- `feature/bibi/BibiResponse.kt` — respuesta estructurada con `buildChatText()`

### Modificados
- `feature/search/SearchScreen.kt` — rediseño completo
- `feature/bibi/DictionaryEngine.kt` — templates por categoría, sin versículos, sugerencias con chatHistory
- `feature/bibi/KnowledgeEngine.kt` — anáforas en extractors
- `feature/study/data/StudyAssistantRepository.kt` — `local first`, `chatHistory`, `mapBibiIntentToApi`, filtro de contexto
- `feature/study/presentation/StudyAssistantPanel.kt` — chips de sugerencias, historial, gestión de sesiones

## Riesgos controlados

- **Crash por `FocusRequester`**: eliminado completamente
- **Crash por `DropdownMenu` con `Column { verticalScroll }` anidado**: refactorizado a `forEach` simple
- **Race conditions en búsquedas paralelas**: `collectLatest` cancela automáticamente
- **`resolvedTerm` contaminado con respuestas de error**: filtrado por `confidence != LOW`
- **Live search filtra capítulo irrelevante**: solo EXPLAIN_VERSE/RELATED/FALLBACK/DIVE_DEEPER envían contexto
- **Migración de BibiHistoryEntity**: se usa `fallbackToDestructiveMigration()` (datos regenerables)
- **Migración de SearchHistoryEntity**: igual enfoque

## Evidencia de validación

Comandos ejecutados:
```
.\gradlew.bat :app:compileDebugKotlin   # BUILD SUCCESSFUL
.\gradlew.bat :app:testDebugUnitTest    # 100+ tests passing
.\gradlew.bat clean :app:assembleDebug  # BUILD SUCCESSFUL
```

---

## Entrega anterior: Corrección de bugs del modo estudio y optimización de barra de herramientas

Fecha: 2026-06-17
Commit: `4c728dc` — `fix: corrige bugs del modo estudio y optimiza barra de herramientas`

### Cambios principales

- **Enter en encabezados**: `normalizeStudyFlow` eliminaba los parrafos vacios creados al presionar Enter en bloques con rol "heading". Corregido agregando "heading" al conjunto de roles preservados en `StudyDocumentEngine.kt:435`.
- **Race condition en LaunchedEffect**: `LaunchedEffect(block.text)` sobrescribia fieldValue con texto desactualizado del ViewModel cuando el usuario escribia rapido. Corregido con `delay(300)` antes de sincronizar en `StudyEditorScreen.kt:830`.
- **Duplicacion de citas en lectura**: `addCitation()` agregaba un bloque Citation inmediatamente y guardaba una solicitud pendiente que luego se convertia en QuotedVerse. Corregido eliminando la escritura inmediata del bloque Citation en `StudyViewModel.kt:643`.
- **Filtro de citas duplicadas en lectura**: `toReadItems()` en `StudyReadScreen.kt:209` ahora filtra bloques Citation cuando existe un QuotedVerse con la misma referencia.
- Eliminadas herramientas de alineacion de texto (izquierda, centrar, derecha).
- Eliminadas herramientas de transformacion de mayusculas/minusculas.
- Barra reducida de 12 a 9 botones en la fila superior.
- Burbuja de colores en Popup con offset de (-200).dp, sin sombra, tonalElevation = 4.dp.

### Evidencia de validación

```
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:testDebugUnitTest
```
