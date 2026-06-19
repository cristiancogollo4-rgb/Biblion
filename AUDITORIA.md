# Auditoria de Cambios

## Entrega: Rediseño de SearchScreen y Bibi mejorada

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
