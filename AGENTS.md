# AGENTS.md

Este documento define los estandares operativos para contribuciones automatizadas y manuales en **Biblion**.
Su objetivo es mantener coherencia con el estado actual del repositorio: Android, Kotlin, Jetpack Compose, Material 3, MVVM, Room y sincronizacion con Firebase/Firestore.

## 1) Contexto del proyecto

- Plataforma: Android.
- Lenguaje: Kotlin.
- UI: Jetpack Compose + Material 3.
- Persistencia local: Room.
- Sincronizacion: Firebase Auth + Firestore para preferencias, estudios, citas y perfil.
- Almacenamiento remoto: Firebase Storage para fotos de perfil.
- Arquitectura actual: enfoque por capas con patron MVVM.
- Modulo principal: `app/`.
- Paquete base: `com.cristiancogollo.biblion`.
- Compatibilidad:
  - `minSdk = 26`
  - Java/Kotlin `jvmTarget = 11`

## 2) Estado funcional actual

Biblion ya incluye:

- Lectura biblica por testamento, libro y capitulo.
- Consulta biblica local mediante Room sobre `app/src/main/assets/databases/bible_content.db`.
- **Busqueda de versiculos en vivo con filtros** (testamento + libro) sobre `SearchScreen`.
- **Búsquedas recientes persistidas** en Room (`SearchHistoryEntity` + `SearchHistoryDao` + `SearchHistoryDatabase` en `search_history.db`).
- **Versículos populares curados** como punto de partida en `SearchScreen` (`PopularVersesData` con 6 versículos: Amor, Fe, Gracia, Salvación, Esperanza, Paz).
- Selector de version biblica.
- Deslizamiento entre testamentos en la pantalla de libros y entre capitulos en el lector.
- Resaltado de versiculos con persistencia y sincronizacion.
- Modo claro/oscuro global (tema claro por defecto en primera instalacion).
- Autenticacion y sincronizacion de datos de usuario.
- Inicio de sesion con Google mediante Firebase Auth.
- Perfil de usuario con nombres, apellidos, alias, biografia, foto o avatar de color.
- Perfil con identidad, metricas visibles y edicion agrupada desde el boton "Actualizar perfil".
- **Reiniciar tutorial de lectura** desde la seccion Perfil.
- Base inicial para la red de Biblion sobre Firestore.
- **Modo estudio v2 (Biblion Docs)**: editor estilo Google Docs con modelo por operaciones puras.
  - **Editor visual actual**: `StudyDocEditorScreen` + `StudyModeEditorPanel` sobre `PaginatedSheet`, con fragmentos editables, foco estable, navegacion entre bloques y listas, undo/redo y zoom con gestos.
  - **Modelo inmutable**: `StudyDoc` con `List<StudyBlock>` y 6 tipos vigentes: `Paragraph`, `Heading`, `BulletList`, `OrderedList`, `Verse`, `Quote`.
  - **Motor puro**: `StudyDocEngine.apply(doc, op): Pair<StudyDoc, OpResult>` con 8 operaciones vigentes: `InsertBlock`, `DeleteBlock`, `UpdateTitle`, `UpdateMetadata`, `ChangeBlockType`, `SplitBlock`, `MergeBlock`, `BulkApply`.
  - **StyledText**: texto con rangos de estilo inmutables (bold, italic, underline, strikethrough, color, background, fontSize, link).
  - **Persistencia Room v3**: `StudyDocEntity` + `StudyDocDao` + `StudyDocRepository` en `study_docs.db`, JSON con kotlinx.serialization y flag `isPublished`.
  - **Borrador vs publicacion**: el autosave usa `saveDraft()` aunque no exista titulo; solo `save()` publica una ensenanza. Los borradores no aparecen en "Mis ensenanzas", busqueda ni sincronizacion.
  - **Listado "Mis ensenanzas"**: `StudyDocsListScreen` muestra exclusivamente documentos publicados.
  - **Modo lectura**: `StudyDocReadScreen` usa el mismo sistema de hoja paginada.
  - **Sistema de etiquetas**: `DocMetadata` con `DocTagGroups` (proposito, audiencia, tema, estado) validado por `StudyDocValidator`.
  - **Navegacion**: rutas `study_docs_list`, `study_doc_editor/{remoteId}`, `study_doc_read/{remoteId}`.
  - **Compatibilidad temporal**: `StudyViewModelStub.kt` mantiene `ReaderScreen` compilando (citation insert y Bibi overlay son stubs).
- **Diccionario biblico local unificado** (`dictionary_v2.db`) con 6,346 entradas Easton's + Theographic: definiciones, metadata (genero, fechas, coordenadas GPS, aliases, featureType). Accesible via `DictionaryEngine` con templates por categoria. **Pantalla dedicada** `DictionaryScreen` con 8 categorias (Personas, Lugares, Conceptos, Objetos, Prácticas, Eventos, Libros, General). `DictionaryCategoryScreen` muestra todas las entradas de una categoria con busqueda en vivo. La categoria **BOOK** incluye los 66 libros en orden canonico biblical (Génesis → Apocalipsis) con descripciones de `topics.db`.
- **Descripcion de libros desde Topics**: long press en un libro en `BooksScreen` abre un `ModalBottomSheet` con descripcion (via `TopicEngine.getBySlug()`), conteo de versiculos y boton "Leer". El slug se genera con `toBookTopicSlug()` que quita tildes y reemplaza espacios por guiones (los slugs en `topics.db` NO tienen acentos ni guiones intermedios).
- **Bibi mejorada** con respuestas estructuradas (`BibiResponse`), templates por categoría (persona/lugar/concepto/objeto/práctica/evento), sugerencias personalizadas con `chatHistory` ("Comparar con X"), anáforas, memoria conversacional e historial de chats persistido en Room.
- **Placeholders rotativos (carousel)** en campos de búsqueda cada 3.5s con 5 ejemplos.
- Integracion online de Bibi mediante Cloudflare Worker y Qwen3-8B por endpoint compatible con OpenAI.
- **Local primero, Worker solo para DIVE_DEEPER** — la búsqueda en Biblia es local, Bibi usa IA solo cuando profundiza.
- Evaluador local de modelos de Bibi en `workers/bibi/evals/model_eval.mjs`.
- Contexto para Bibi con versiones biblicas disponibles, version seleccionada, texto seleccionado, bloques actuales de la ensenanza, notas y diccionario biblico.
- Saludo local de Bibi con el nombre visible del usuario autenticado sin incluir ese nombre en la solicitud al Worker.
- **Tutorial guiado interactivo (primera instalacion)**: inicio automatico en Home, Bibi con logo, auto-scroll a versiculo 1, target ampliado (primeros 3 versiculos), scroll en textos largos, debug logs `GUIDE_DEBUG`.
- **Referencias cruzadas con voto crowdsourced** (`app/src/main/assets/databases/cross_references_votes.db`): 340,645 pares del dataset openbile.info (CC-BY 2026-06-15). Reemplazo completo del antiguo TSK. Tabla `cross_reference_votes` con `source_book`, `source_normalized_book`, `source_chapter`, `source_verse`, `target_references` (formato Biblion), `votes` (1-1279, INTERNO). Esquema version 1. La columna `votes` se usa internamente para ranking y filtrado (default `votes >= 10`). **NUNCA** debe exponerse al usuario en la UI de Bibi.
- **Temas canonicos v3** (`app/src/main/assets/databases/topics.db`): 693 temas canonicos en 11 categorias (BOOK 66, PERSON 126, PLACE 70, EVENT 43, ATTRIBUTE_OF_GOD 32, PROPHECY 33, COMPARATIVE_RELIGION 17, SIN 70, DOCTRINE 101, CHURCH 50, CHRISTIAN_LIFE 85). Esquema v3 con 4 tablas: `topics` (con `description` generada por Bibi Worker qwen3-8b o manual, `verse_count`, `total_aliases`, `parent_slug` para jerarquia), `topic_aliases` (1689 aliases de OpenBible con `score` 0.85-0.94 y flag `low_confidence`), `topic_references` (4704 versiculos del OSIS original con `score` 2-100), `topic_relationships` (236 jerarquias parent/child). Las traducciones ES curadas viven en `tools/topic_aliases.json`. Ver seccion "Busqueda de versiculos y temas" para detalles del pipeline de 6 fases y la pantalla "Explorar temas".
- **Despues del tutorial**: mensaje de despedida de Bibi con mension a opcion de reinicio en Perfil.

### Flujo de primera instalacion (onboarding)

**Antes**: Pantalla con pregunta "¿Como quieres usar Biblion?" y tres opciones de navegacion.

**Ahora (actual)**:
1. Usuario abre la app por primera vez.
2. Se muestra `HomeScreen` directamente (tema claro por defecto).
3. `LaunchedEffect(Unit)` en `AppNavigation` detecta que `hasCompletedReadingGuide == false`.
4. Inicia automaticamente `GuidedTutorialId.READING` en step 0 (`reading-welcome`).
5. `GuideBubble` con logo de Bibi (48dp tablet / 40dp movil) aparece en `HomeScreen`.
6. Paso 2: target `HOME_TESTAMENT_SELECTOR` -> usuario toca testamento.
7. Navega a `BooksScreen` -> paso 3: target `BOOKS_FIRST_BOOK`.
8. Usuario elige libro -> navega a `ReaderScreen` -> paso 4: target `READER_CHAPTER_SELECTOR`.
9. Usuario elige capitulo -> paso 5: `READER_TEXT` (info del lector).
10. **Paso clave**: `reader-highlight` con target `READER_FIRST_VERSE`:
    - `LaunchedEffect` en `ReaderScreen` hace auto-scroll a versiculo 1 (`animateScrollToItem(0)`).
    - Target ampliado: **primeros 3 versiculos** (`index < verses.size`).
    - Long-press en cualquiera dispara `saveHighlight` + `onGuidedTutorialTargetAction`.
    - Tambien funciona via boton "Resaltar" en menu flotante.
11. Paso 7: `READER_VERSION_SELECTOR` (FAB).
12. Paso 8-9: `READER_BIBI_BUTTON` (abrir chat Bibi).
13. Paso 10: Info "Mas alla de la lectura" (modo estudio).
14. Paso 11: `reading-finish` -> "Comenzar a leer" (completa) / "Ver recorrido nuevamente" (RESTART).
15. Al completar: `KEY_HAS_COMPLETED_READING_GUIDE = true`, no vuelve a auto-iniciarse.
16. **Reinicio**: Desde Perfil -> "Reiniciar tutorial de lectura" -> vuelve a step 0.

## 3) Principios de cambio

1. Mantener cambios pequenos y enfocados.
2. No romper navegacion ni flujos existentes: `Home`, `Books`, `Reader`, `Search`, `StudyDocsList`, `StudyDocEditor`.
3. Priorizar legibilidad sobre micro-optimizaciones prematuras.
4. Preservar compatibilidad con datos locales existentes.
5. No tocar cambios ajenos en el working tree.

## 4) Estandares Kotlin

- Seguir convenciones oficiales de Kotlin.
- Usar nombres descriptivos, `camelCase` para variables/funciones y `PascalCase` para tipos.
- Preferir `val` e inmutabilidad cuando sea razonable.
- Mantener funciones pequenas; extraer helpers solo cuando mejore claridad real.
- Evitar comentarios redundantes.
- Documentar solo decisiones no obvias o comportamiento delicado.
- Mantener el paquete base `com.cristiancogollo.biblion`.

## 5) Estandares Compose/UI

- Cada pantalla debe mantener responsabilidad clara: render, estado local minimo y eventos UI.
- La logica de negocio debe vivir en ViewModel, repositorios o helpers testeables.
- Respetar `BiblionTheme` y `MaterialTheme`.
- Mantener siempre la identidad visual de Biblion: azul, dorado y blanco en modo claro y oscuro. No activar colores dinamicos del sistema ni paletas ajenas que reemplacen la marca.
- Usar componentes compartidos cuando aplique, por ejemplo `BiblionComponents` y `StudyTagSelector`.
- Textos visibles nuevos deberian ir a `res/values/strings.xml` y `values-es` cuando el cambio lo amerite.
- Mantener accesibilidad basica: `contentDescription` en iconos accionables.
- En pantallas compactas, evitar controles que saturen la barra superior.
- En pantallas grandes, se permite UI expandida como lectura dividida, siempre con fallback vertical en movil.

### GuidedTutorialOverlay y GuideBubble

El sistema de tutorial guiado usa `GuidedTutorialOverlay` + `GuideBubble` para mostrar pasos contextuales.

**Comportamiento actual**:
- Inicio automatico en primera instalacion en `HomeScreen` (step `reading-welcome`).
- `GuideBubble` muestra logo de Bibi (48dp tablet / 40dp movil) y texto con scroll vertical.
- `heightIn(max = 500.dp)` en `Surface` + `verticalScroll(rememberScrollState())` en `Column` permite scroll en textos largos.
- `maxLines` removido de descripcion para permitir expansion natural y activar scroll.
- Texto titulo max 5 lineas, descripcion sin limite de lineas (solo ellipsis si excede).

**Targets del tutorial READING**:
1. `reading-welcome` - Home, sin target, "Comenzar recorrido"
2. `reading-testament` - Home, `HOME_TESTAMENT_SELECTOR`
3. `reading-book` - Books, `BOOKS_FIRST_BOOK`
4. `reading-chapter` - Reader, `READER_CHAPTER_SELECTOR`
5. `reader-text` - Reader, `READER_TEXT`
6. `reader-highlight` - Reader, `READER_FIRST_VERSE` (auto-scroll a versiculo 1, target primeros 3 versiculos)
7. `reader-version` - Reader, `READER_VERSION_SELECTOR`
8. `reader-bibi` - Reader, `READER_BIBI_BUTTON`
9. `reader-bibi-chat` - Reader, `READER_BIBI_BUTTON`
10. `reader-deeper-path` - Reader, sin target
11. `reading-finish` - Reader, "Comenzar a leer" / "Ver recorrido nuevamente" (RESTART)

**Responsive**: En pantallas < 360dp se reducen padding (12dp), logo (40dp), tipografias (Small), botones (36dp), ancho max bubble = ancho pantalla.

**Debug**: Logs `GUIDE_DEBUG` en `AppNavigation`, `ReaderScreen`, `GuideBubble` para rastrear estado.

## 6) Modo estudio: herramientas y responsabilidades

> **ACTUALIZACION (24 Jul 2026)**: esta seccion refleja el codigo vigente. `STUDY_DOCS_V2.md` contiene el inventario tecnico actual; `PLAN_PAGINACION.md` es historico.
>
> El modo estudio fue completamente reestructurado a una arquitectura por operaciones puras estilo Google Docs.
> El sistema viejo (`feature/study/`) fue eliminado y reemplazado por `feature/studydocs/`.
> Ver `STUDY_DOCS_V2.md` para documentacion completa de la nueva arquitectura.

El modo estudio v2 se compone de `StudyDocEditorScreen`, `StudyDocViewModel`, `StudyDocEngine`, `StudyDoc`, `StudyDocReadScreen` y `StudyDocsListScreen`.

### Arquitectura del documento de estudio (v2)

- **`StudyDoc`** (`feature/studydocs/model/StudyDoc.kt`): documento inmutable con `id: DocId`, `title`, `blocks: List<StudyBlock>`, `metadata: DocMetadata`, `version: Int`.
- **`StudyBlock`** (`feature/studydocs/model/StudyBlock.kt`): sealed interface con **6 tipos vigentes**: `Paragraph`, `Heading` (1-3), `BulletList`, `OrderedList`, `Verse`, `Quote`.
- **`StyledText`** (`feature/studydocs/model/StyledText.kt`): texto con lista inmutable de `StyleRange` (bold, italic, underline, strikethrough, color, background, fontSizeSp, link). Soporta `withText(range, replacement)`, `withStyle(range, patch)`, `clearStyle(range, kind)`.
- **`StudyOp`** (`feature/studydocs/engine/StudyOp.kt`): 8 operaciones selladas — `InsertBlock`, `DeleteBlock`, `UpdateTitle`, `UpdateMetadata`, `ChangeBlockType`, `SplitBlock`, `MergeBlock`, `BulkApply`.
- **`StudyDocEngine`** (`feature/studydocs/engine/StudyDocEngine.kt`): motor puro. `apply(doc, op): Pair<StudyDoc, OpResult>`. Sin estado mutable, 100% testeable.
- **`StudyDocValidator`** (`feature/studydocs/engine/StudyDocValidator.kt`): 8 tipos de `ValidationIssue` (duplicate block ids, invalid heading level, verse compare mismatch, style out of bounds, table shape, missing required tags).
- **`StudyDocNormalizer`** (`feature/studydocs/engine/StudyDocNormalizer.kt`): merge de rangos de estilo, dedup de dividers consecutivos, strip de parrafos vacios al final, intro block automatico.
- **`StudyDocViewModel`** (`feature/studydocs/domain/StudyDocViewModel.kt`): MVVM con `StudyEditorUiState` (doc, selectedBlockId, isLoading, lastError, isSaving). Expone `applyOp(op)`, `applyAll(ops)`, `saveNow()`, `loadByRemoteId(id)`.
- **`StudyDocEditorScreen`** (`feature/studydocs/ui/editor/StudyDocEditorScreen.kt`): coordina modo split/standalone, dialogos de guardado/salida y el `PaginatedSheet`.
- **`StudyModeEditorPanel`**: header editable, toolbar, modo oscuro, selector de fuente/tamano y hoja paginada.
- **`PagedEditorUnitRenderer`**: edicion por unidad paginada con cursor/seleccion estable entre fragmentos y paginas.
- **`StudyDocReadScreen`** (`feature/studydocs/ui/read/StudyDocReadScreen.kt`): render paginado de los 6 tipos vigentes.
- **`StudyDocsListScreen`** (`feature/studydocs/ui/list/StudyDocsListScreen.kt`): lista de "Mis ensenanzas" con documentos publicados.

### Persistencia

- **`study_docs.db`**: Room v3. La migracion 1->2 elimina `doc_edit_history`; 2->3 agrega `is_published` conservando visibles los documentos existentes.
- **`StudyDocDao`**: 13 queries (observeAll, observeByNotebook, observeByOwner, getById, getByRemoteId, search, getDirtyForSync, insert, update, softDelete, hardDelete, markSynced, countActive).
- **`StudyDocRepository`**: `save(doc)` exige titulo y publica; `saveDraft(doc)` permite autosave sin titulo; `discardDraft(remoteId)` elimina solo registros no publicados.
- **`StudyDocJson`**: serializacion JSON via kotlinx.serialization con `classDiscriminator = "type"` y `ignoreUnknownKeys = true`.

### Herramientas del editor (v2)

- **Tipos de bloque**: la toolbar inserta o transforma entre los 6 tipos vigentes mediante `StudyDocEngine`.
- **Paginacion editable**: `PaginationEngine` produce `PageFragment`; `PagedEditorLayout` genera unidades editables y `PagedEditorUnitRenderer` mantiene una sola autoridad de texto/foco.
- **Zoom con gestos**: `DocumentZoomState` + transform gestures, rango 0.75x-2.0x, presets y ajuste al ancho. El scroll conserva la autoridad del paneo.
- **Orientacion de estudio**: `StudyModeLandscapeLock` mantiene el editor en `sensorLandscape` en telefonos, plegables y tabletas, y restaura la politica de orientacion anterior al salir. `MainActivity` declara la compatibilidad temporal requerida por Android 16 para respetar esta politica en pantallas `sw600dp`.
- **Layout compacto de estudio**: cuando el viewport tiene ancho menor a 840dp o altura menor a 480dp, `CompactStudyLayoutController` reemplaza el split 50/50 por un panel unico con selector lateral Biblia/Documento. Ambos paneles permanecen compuestos para conservar editor, cursor y navegacion. El teclado virtual no se abre automaticamente y el encabezado se oculta mientras el IME esta visible.
- **Texto inline**: `RichTextState` se sincroniza con `StyledText`; `RichTextBridge` preserva rangos y `PageGapVisualTransformation` mapea offsets visuales/logicos.
- **Estilos de texto**: toolbar con enfasis, color, resaltado, familia, alineacion y tamano 8-72. Los cambios de tamano conservan el resto de estilos.
- **Citar versiculo**: bloque `StudyBlock.Verse` con libro, capitulo, rango, version fuente y mapa de contenidos comparados.
- **Listas**: `BulletList` y `OrderedList`; Enter crea items, Enter en item final vacio sale de la lista y el flujo de doble Backspace usa `ListBackspaceExitTracker`.
- **Guardar**: autosave diferido persiste con `saveDraft()` sin publicar. El boton Guardar exige titulo escrito por el usuario y llama a `save()`.
- **Salida segura**: si hay contenido o cambios pendientes se presenta un `Dialog`; "Seguir editando" conserva el estado y "Salir sin guardar" descarta el borrador.

### Stubs temporales

- **`StudyViewModelStub.kt`**: `StudyViewModel` + `StudyIntent` + `StudyUiState` stubs para que `ReaderScreen` compile mientras se termina de migrar.
- **`ReaderAssistantOverlay`**: stub vacio del Bibi overlay en el lector.
- **Sync remoto activo**: `FirestoreSyncManager` sincroniza exclusivamente ensenanzas publicadas bajo `users/{uid}/studies/{remoteId}`. Hace pull antes del push inicial, escucha cambios en tiempo real, usa tombstones para eliminaciones, filtra por propietario y conserva copias locales ante conflictos. Los borradores `isPublished=false` permanecen solo en Room. `LegacyStudyDocMigrator` convierte payloads `contentSerialized` del editor anterior al modelo `StudyDoc`.

### Bibi

Bibi es la asistente biblica oficial de Biblion. Esta integrada en:

- **Modo estudio**: respuestas mas profundas para preparar ensenanzas, predicaciones, devocionales, clases biblicas y discipulado.
- **Lector normal**: respuestas breves para comprender el pasaje actual, palabras, referencias y aplicaciones sencillas.

#### Arquitectura local de Bibi

Bibi se compone de tres motores locales en `feature/bibi/`:

- **`KnowledgeEngine`** (orquestador): detecta la intencion del usuario (WHO, WHERE, DEFINE, EXPLAIN_VERSE, RELATED, ORIGINAL_LANG, GREETING, DIVE_DEEPER, FALLBACK) y enruta al motor apropiado. Maneja anáforas: cuando el usuario pregunta algo corto sin sujeto claro (ej. "¿y qué más?"), Bibi busca el último `resolvedTerm` en el historial conversacional y lo usa como sujeto.

- **`DictionaryEngine`** (motor de definiciones): convierte entradas del diccionario biblico en respuestas naturales. Tiene un template distinto por categoría:
  - **Persona**: género, fechas de nacimiento/muerte, aliases, displayTitle.
  - **Lugar**: coordenadas GPS, tipo de feature (ciudad, monte, río, etc.), aliases.
  - **Concepto**: definición + versículos relacionados.
  - **Objeto/Práctica/Evento/General**: definición.
  - Las respuestas usan `BibiResponse` con `buildChatText()` que produce texto natural con etiqueta de metadatos (ej. "Género: Masculino", "Ubicación: 31.78, 35.21").
  - **Las respuestas NO incluyen versículos completos** (solo metadatos). Para profundizar en versículos, Bibi sugiere "Pedir a IA: pasajes sobre X" que va al Worker.

- **`AmbiguousTermResolver`**: cuando un término no se encuentra, busca alternativas similares (prefijo, ediciones cercanas) y ofrece opciones al usuario.

- **`BibiHistoryRepository`**: persiste el historial de chats en Room (`bibi_chat.db`). Cada sesión es un `ChatSessionEntity` y cada mensaje un `ChatMessageEntity` con `resolvedTerm`, `intent`, `role`. El historial se carga al abrir Bibi y se guarda en cada mensaje.

- **`ChatSessionRepository`**: gestión de sesiones (crear, listar, eliminar, renombrar). Permite múltiples conversaciones independientes.

#### Detección de intención

`KnowledgeEngine.detectIntent(question)` analiza el texto (con `removeAccents` para insensibilidad a acentos) y retorna:

| Intención | Patrones |
|-----------|----------|
| `WHO` | "quién fue/es/era", "cuéntame de", "háblame de", "dime sobre", "información de" |
| `WHERE` | "dónde queda/está/nació/vivió", "ubicación de" |
| `DEFINE` | "qué significa", "define", "qué es", "explica", "dime qué" |
| `EXPLAIN_VERSE` | "explica este versículo/pasaje" (requiere `verseText`) |
| `RELATED` | "pasajes relacionados", "dónde más", "versículos similares" |
| `ORIGINAL_LANG` | "en hebreo", "en griego", "Strong" + número (H1254, G25) |
| `GREETING` | "hola", "buenas" |
| `DIVE_DEEPER` | "profundiza", "amplía", "explica más", "cuéntame más", o frases muy cortas tras un historial |
| `FALLBACK` | ninguno de los anteriores → va al Worker |

#### Detección de dominio bíblico

`StudyAssistantRequest.isBibleDomain()` valida que la pregunta es bíblica antes de enviar al Worker. Si el `KnowledgeEngine.detectIntent` retorna `WHO`, `WHERE`, `DEFINE`, `EXPLAIN_VERSE`, `RELATED`, `ORIGINAL_LANG` o `GREETING`, se considera dominio bíblico. También busca **~130 nombres bíblicos comunes** (patriarcas, reyes, profetas, apóstoles, lugares, ángeles) y **palabras bíblicas** (biblia, dios, jesús, etc.). Si no detecta nada bíblico y la pregunta contiene palabras no bíblicas (programación, política, etc.), responde con el mensaje de redirección.

#### Filtro de contexto al Worker

Para evitar que el Worker use el capítulo actual como contexto de respuestas independientes:

- **WHO/WHERE/DEFINE/ORIGINAL_LANG** → NO se envía `studyTitle` ni `selectedText` al Worker.
- **EXPLAIN_VERSE/RELATED/FALLBACK/DIVE_DEEPER** → SÍ se envía contexto completo.

Esto se implementa en `StudyAssistantPanel.sendQuestion()` mediante `needsChapterContext = intent in setOf(...)`.

#### Anáforas y memoria conversacional

`chatHistory: List<ChatExchange>` se mantiene en memoria durante la sesión. Cada `ChatExchange` contiene:

- `question: String`
- `response: String`
- `resolvedTerm: String?` — término resuelto (ej. "Abraham", "Galilea")
- `intent: String`

Anáfora: cuando el extractor no encuentra un término, busca el último `resolvedTerm` en el historial. Ejemplo:

```
P1: "¿quién fue Abraham?" → resolvedTerm = "Abraham"
P2: "¿dónde vivió?" → cleaned.length < 2 → busca en historial → "Abraham"
    → "¿dónde vivió Abraham?"
```

Para evitar contaminación con respuestas de error, `resolvedTerm` solo se setea si `bibiResponse.confidence != LOW` (no se incluye "No encontré X" como término válido).

#### Sugerencias personalizadas (chips)

Cada respuesta de `DictionaryEngine` incluye 2 sugerencias (`BibiSuggestion`):

- **"Pedir a IA: pasajes sobre X"** (isAi=true) → query "profundiza qué pasajes hablan de X" → DIVE_DEEPER → Worker.
- **"Comparar con Y"** (isAi=true) → si hay otro término en el historial → query "¿qué diferencia hay entre X y Y?" → DIVE_DEEPER → Worker.
- **"Profundizar con IA"** (isAi=true) → query "profundiza sobre X" → DIVE_DEEPER → Worker.

Las sugerencias de tipo `RELATED` o `FALLBACK` que caen en local sin lógica implementada fueron eliminadas.

#### Filtro de `studyTitle`/`selectedText` al Worker

Para evitar que el Worker use el capítulo actual como contexto de respuestas independientes:

- WHO/WHERE/DEFINE/ORIGINAL_LANG → NO se envía `studyTitle` ni `selectedText` al Worker.
- EXPLAIN_VERSE/RELATED/FALLBACK/DIVE_DEEPER → SÍ se envía contexto completo.

Esto se implementa en `StudyAssistantPanel.sendQuestion()` mediante `needsChapterContext = intent in setOf(...)`.

#### Reglas funcionales

- Bibi no actua como asistente general.
- Su dominio es exclusivamente biblico/cristiano: Biblia, estudio biblico, contexto, personajes, lugares, historia biblica relacionada con las Escrituras, doctrina cristiana, discipulado, devocionales, predicacion, ensenanzas, reflexion, aplicacion, palabras biblicas, referencias cruzadas y comparacion de pasajes.
- Preguntas fuera de dominio deben responder con el mensaje de redireccion definido en `StudyAssistantRepository` y `workers/bibi/src/index.js`.
- Bibi no debe inventar versiculos, citas, personajes, eventos, doctrinas, revelaciones, profecias, mensajes personales de Dios ni interpretaciones sin fundamento biblico.
- Bibi no debe usar referencias no proporcionadas por Biblion salvo que el usuario pida referencias cruzadas o pasajes relacionados y la referencia sea segura.
- Toda ensenanza, explicacion o aplicacion debe estar sustentada en las Escrituras o identificarse claramente como reflexion basada en ellas.
- Si una referencia no es segura, debe reconocerlo y sugerir verificar el pasaje.
- Si compara versiones, debe usar solo textos proporcionados por Biblion; no debe inventar traducciones.
- Si compara versiones y faltan textos, debe indicar que Biblion no proporciono esos textos.
- No debe afirmar que Maria Magdalena fue prostituta; si menciona Lucas 7, debe aclarar que el texto original no identifica a esa mujer como Maria Magdalena.
- Puede saludar por nombre en la UI cuando el usuario inicio sesion, pero ese dato debe mantenerse local y no agregarse al payload del Worker salvo que una tarea futura lo justifique explicitamente.

Contexto enviado a Bibi:

- `mode`: `study` o `reader`.
- `intent`: `explain`, `define`, `cross_reference`, `application`, `outline`, `sermon`, `devotional`, `compare_versions` o `question`.
- `study.title`, `study.tags`, `study.selectedText`, `study.currentOutline`, `study.notes`.
- `bible.version`, `bible.availableVersions`, `bible.passages`.
- `userName` (local, no se envía al Worker)
- `lastQueries` (top 5 de queries recientes, solo si no fue skip por reset)
- `chatHistory` (lista de `ChatExchange` con pregunta, respuesta, resolvedTerm, intent)

El nombre visible del usuario no forma parte de este contexto por defecto; solo se usa para el mensaje inicial local del chat.

Respuesta esperada del Worker:

```json
{
  "answer": "",
  "references": [],
  "suggestedBlocks": [],
  "confidence": "high"
}
```

La UI debe mostrar solo `answer`. El repositorio Android limpia defensivamente respuestas que lleguen con JSON incrustado o malformado.

Infraestructura:

- El cliente Android usa `HttpStudyAssistantRepository`.
- **Estrategia "local primero"**: `HttpStudyAssistantRepository.ask()` primero llama a `LocalStudyAssistantRepository` (que ejecuta `KnowledgeEngine.answer`). Solo si la respuesta local tiene `confidence = LOW` o es null, se envía al Worker. Esto reduce la dependencia de la red y mejora la latencia.
- La URL se configura con `bibiEndpointUrl` en `local.properties` y se inyecta como `BuildConfig.BIBI_ENDPOINT_URL`.
- El Worker vive en `workers/bibi`.
- El Worker usa `qwen3-8b` como modelo principal mediante proveedor `openai-compatible`.
- La URL compatible con OpenAI se configura con `OPENAI_COMPATIBLE_BASE_URL` y el secreto `OPENAI_COMPATIBLE_API_KEY`.
- NVIDIA queda como fallback configurable con `BIBI_PROVIDER = "nvidia"`, `BIBI_MODEL = "nvidia/llama-3.1-nemotron-nano-8b-v1"` y secreto `NVIDIA_API_KEY`.
- Los secretos de proveedores IA deben vivir en Cloudflare Worker secrets, nunca en el APK ni en archivos versionados.
- Si el endpoint falla o esta vacio, Android usa respuesta local de respaldo.
- El Worker tambien tiene respuestas de respaldo con diccionario biblico cuando el proveedor IA tarda.
- Las pruebas comparativas de modelos se ejecutan con `npm run eval:models` dentro de `workers/bibi`; el evaluador valida JSON, dominio, idioma, referencias permitidas, textos obligatorios/prohibidos y latencia.

### Diccionario biblico local unificado

`dictionary.db` (`app/src/main/assets/databases/dictionary.db`) consolida **6,346 entradas** de dos fuentes:

- **Easton's Bible Dictionary**: 3,932 entradas con definiciones en espanol (descargadas de `tools/eastons_dictionary_es.json`).
- **Theographic Data**: 3,067 personas + 1,274 lugares con metadata enriquecida (genero, fechas, coordenadas GPS, aliases, featureType).

#### Schema de la base

```sql
dictionary_entries(
  id, term, normalized_term, definition, references_json, category,
  display_title, gender, birth_year, death_year,
  latitude, longitude, aliases, feature_type
)
```

#### Deteccion de intencion y templates por categoria

`DictionaryEngine.formatByCategory()` selecciona el template adecuado segun `entry.category`:

| Categoria | Template | Metadata mostrada |
|-----------|----------|-------------------|
| `PERSON` | `formatPerson()` | gender, birthYear, deathYear, displayTitle, aliases |
| `PLACE` | `formatPlace()` | latitude, longitude, featureType, aliases |
| `CONCEPT` | `formatConcept()` | solo definicion |
| `OBJECT` | `formatObject()` | solo definicion |
| `PRACTICE` | `formatPractice()` | solo definicion |
| `EVENT` | `formatEvent()` | solo definicion |
| `OTHER` | `formatGeneric()` | solo definicion |

#### Conversion de fechas ISO astronomicas

`formatYear(isoYear)` convierte anos ISO astronomicos a texto legible:
- `-1997` -> "1997 a.C."
- `0` -> "1 a.C."
- `1997` -> "1997 d.C."

#### Traduccion de feature types

`translateFeatureType(type)` traduce tipos de OpenStreetMap:
- `city` -> "Ciudad"
- `region` -> "Region"
- `mountain` -> "Monte"
- `river` -> "Rio"
- etc.

#### BibiResponse estructurado

Cada respuesta usa `BibiResponse` con `buildChatText()` que produce texto natural con etiqueta de metadatos. El formato del chat incluye:

```
[Saludo personalizado: "¡Hola Juan! Veo que estás leyendo Mateo 5."]

Abraham
[Definicion completa]

Genero: Masculino
Nacimiento: 1997 a.C.

[Sin versiculos completos - solo metadata]

[Follow-up: "¿Quieres saber mas sobre el o algun aspecto en particular?"]
```

#### Importante: NO versiculos en respuestas locales

Las respuestas del diccionario local **NO incluyen versiculos completos**. Esto evita que Bibi invente referencias. Para profundizar en versiculos sobre el tema, se sugiere via chips: **"Pedir a IA: pasajes sobre X"** que va al Worker.

### Pantalla de diccionario y descripcion de libros

`DictionaryScreen` (`feature/dictionary/ui/DictionaryScreen.kt`) muestra 8 tarjetas de categorias con color, descripcion y conteo de entradas. Al tocar una category, navega a `DictionaryCategoryScreen`.

**Categorias**: Personas, Lugares, Conceptos, Objetos, Prácticas, Eventos, Libros, General.

**`DictionaryCategoryScreen`** (`feature/dictionary/ui/DictionaryCategoryScreen.kt`): lista de entradas con busqueda en vivo. La categoria **BOOK** ordena los 66 libros en orden canonico biblical (Génesis → Apocalipsis) usando `canonicalBookOrder` + `normalizeForBookOrder()` para insensibilidad a acentos.

**`BookCard` en BooksScreen**: long press en un libro abre un `ModalBottomSheet` con descripcion del libro desde `topics.db` via `TopicEngine.getBySlug()`. El slug se genera con `toBookTopicSlug()` que quita tildes y reemplaza espacios por guiones. Click regular navega al lector.

**Rutas**: `Screen.Dictionary` (route `dictionary`), `Screen.ExploreDictionaryCategory` (route `dictionary_category/{category}`).

**Base de datos**: `DictionaryDatabase` (Room v3, asset `dictionary_v2.db`) con `getEntriesByCategory()` y `getCategoryCounts()`.

### Referencias cruzadas y temas (openbile.info) - version actual

Reemplaza al antiguo sistema TSK. Bibilion usa dos DBs separadas:

#### `cross_references_votes.db` (340,645 pares)

Generado por `tools/build_crossrefs_votes_sqlite.py` desde el dataset openbile.info (CC-BY 2026-06-15). Tabla `cross_reference_votes` con `source_book`, `source_normalized_book`, `source_chapter`, `source_verse`, `target_references` (formato Biblion `Libro Cap:V` o `Libro Cap:V-V`), `votes` (1-1279, INTERNO).

**Reglas de filtrado y ranking:**
- Default: `votes >= 10` (12.6% mas confiable del dataset)
- `votes > 0`: 99.0% (descarta negativos y ceros por baja calidad)
- `votes >= 50`: 1.3% (curado, alta calidad)

**Donde se consume:**
- **Bibi** (`CrossReferenceVoteEngine.getRelatedBySource`): retorna versiculos relacionados. **NUNCA** expone el voto al usuario.
- **Editor de ensenanzas** (`StudyEditorScreen.kt`): el boton `+ xrefs` en cada bloque `QuotedVerse` abre un dialog con versiculos relacionados como `AssistChip` (uno por versiculo, no por anchor). Click inserta como `QuotedVerse` block.
- **VerseResolver** (`feature/bibi/VerseResolver.kt`): detecta el versiculo activo con Forma 1 (seleccion long-press) o Forma 2 (regex en pregunta del usuario).

#### `topics.db` (693 temas canonicos, 4 tablas, esquema v3)

Generado por el script v3 que produce la DB con 4 tablas. **El script `tools/build_topics_v2_db.py` (sistema antiguo) esta obsoleto**; el sistema actual usa `canonical_topics.json` como fuente canonica y genera la DB con schema v3.

**Tablas**:
- `topics`: 693 canonicos con `description` (generada por Bibi Worker qwen3-8b o manual), `verse_count`, `total_aliases`, `parent_slug` (jerarquia), `slug` UNIQUE.
- `topic_aliases`: 1689 aliases de OpenBible con `score` 0.85-0.94, `low_confidence` (1 si score < 0.86).
- `topic_references`: 4704 versiculos del OSIS original con `score` 2-100 (INTERNO, no exponer al usuario).
- `topic_relationships`: 236 jerarquias parent/child.

**Indices UNIQUE** declarados en el entity Kotlin (deben coincidir con la DB preempaquetada):
- `idx_topics_slug` (UNIQUE)
- `index_topic_relationships_parent_slug_child_slug_relationship_type` (UNIQUE)

**Traducciones y curaduria**:
- `tools/canonical_topics.json` con 693 temas canonicos y descripciones.
- `tools/topic_aliases.json` con 1689 aliases de OpenBible con score >= 0.85.
- `tools/topic_translations_es.json` (286 entradas, sistema v2 antiguo, ya no se usa).

**Donde se consume:**
- **TopicEngine** (`feature/bibi/TopicEngine.kt`): busqueda por tema (pipeline de 6 fases), topicos para un versiculo, fuzzy search, rotacion de temas populares. Filtra por `verseCount > 0` para no devolver temas sin contenido.
- **Bibi**: integracion futura para "¿de que temas habla este versiculo?" y "versiculos sobre X".

**Codigo de motores en `feature/bibi/`:**

- `CrossReferenceVoteEngine.getRelatedBySource(context, book, chapter, verse, minVotes, maxTotal)`: devuelve `List<RelatedVerse>` ordenadas por relevancia.
- `CrossReferenceVoteEngine.getTopForSource(context, book, chapter, verse, maxTotal)`: top N sin filtrar.
- `TopicEngine.getVersesForTopic(context, topicKey, minScore, maxTotal)`: versiculos para un tema.
- `TopicEngine.getTopicsForVerse(context, book, chapter, verse, minScore, maxTotal)`: temas para un versiculo.
- `TopicEngine.searchTopicsByText(context, query, minScore, maxTotal)`: fuzzy search.
- `VerseResolver.resolve(reader, question)`: retorna `Resolution` con versiculo activo segun Forma 1 / Forma 2.

#### Tests

- `app/src/test/.../VerseResolverTest.kt`: Forma 1, Forma 2, fallbacks, casos edge.
- `app/src/test/.../RelatedVerseTest.kt`: verifica que DTO NO expone votos.
- `app/src/test/.../CrossReferenceVoteEntityTest.kt`: estructura del entity.


### Citas biblicas

- Al seleccionar versiculos contiguos en el lector, deben agruparse en una sola cita.
  - Ejemplo: `Genesis 1:1`, `1:2`, `1:3` se guarda como `Genesis 1:1-3`.
- El texto citado debe mantener indicadores inline de versiculo:
  - `1 En el principio... 2 Y la tierra... 3 Y dijo Dios...`
- Los indicadores de versiculo usan color diferenciado para modo claro y oscuro.
- En lectura de ensenanza, el bloque citar permite:
  - cambiar version principal;
  - comparar con otra version;
  - ocultar/mostrar comparacion.

### Etiquetas de ensenanza

El sistema actual usa etiquetas establecidas por seccion:

- **Proposito**: `predicacion`, `devocional`, `estudio-biblico`, `clase`, `discipulado`, `formacion`.
- **Audiencia**: `jovenes`, `iglesia`, `lideres`, `universitarios`, `familias`, `simpatizantes`, `ninos`, `mujeres`, `hombres`, `ancianos`, `grupos-especiales`, `pastores`.
- **Tema**: `identidad`, `fe`, `gracia`, `proposito`, `oracion`, `evangelismo`, `servicio`, `esperanza`, `doctrina`, `amor`, `misiones`, `adoracion`.
- **Estado**: `borrador`, `en-preparacion`, `finalizado`.

Reglas:

- Proposito, audiencia y tema permiten seleccion multiple.
- Estado permite una sola seleccion.
- Para guardar una ensenanza se requiere:
  - titulo no vacio;
  - al menos una etiqueta de proposito;
  - al menos una etiqueta de audiencia;
  - al menos una etiqueta de tema;
  - exactamente una etiqueta de estado.
- Las etiquetas personalizadas pueden existir, pero no reemplazan las secciones obligatorias.

### Busqueda de versiculos y temas (SearchScreen)

`SearchScreen` permite buscar versiculos en la Biblia local y temas canonicos, con filtros y busqueda en vivo. Ademas incluye un carrusel rotativo de temas populares y acceso a la pantalla "Explorar temas".

#### Arquitectura

- **Biblia local**: `BibleRepository.searchVerses()` consulta `bible_verses` con SQL `LIKE '%query%' COLLATE NOCASE`.
- **Filtros**: `BibleSearchFilter(testament, bookName)` se pasa a `searchVerses()` para restringir por testamento y/o libro.
- **Persistencia de busquedas**: `SearchHistoryRepository` guarda cada query en Room (`search_history.db`) con normalizacion (sin acentos) y conteo de uso.
- **Temas canonicos**: `TopicDatabase` (Room v3) consulta `topics.db` con schema v3 (4 tablas: topics, topic_aliases, topic_references, topic_relationships).

#### Busqueda en vivo SEGURA

Patron clave: `MutableStateFlow` + `collectLatest` para evitar race conditions:

```kotlin
val queryFlow = remember { MutableStateFlow("") }

LaunchedEffect(Unit) {
    queryFlow
        .debounce(400L)                    // espera 400ms sin cambios
        .filter { it.trim().length >= 2 }  // minimo 2 caracteres
        .distinctUntilChanged()            // no busquedas duplicadas
        .collectLatest { query ->           // CANCELA busqueda anterior
            executeSearch(query)
        }
}
```

**`collectLatest` cancela la corutina anterior** cuando llega un nuevo valor, eliminando race conditions. `executeSearch` lanza en paralelo: versiculos biblicos (via `BibleRepository.searchVerses`) y temas canonicos (via `TopicEngine.searchTopics`).

#### Pipeline de 6 fases para busqueda de temas

`TopicEngine.searchTopics` ejecuta un pipeline de fases binarias, cada una con una query SQL simple. Sin scoring, sin parametros magicos:

1. **Slug exacto** (`findBySlug`) — ej. "fe" -> topic "fe"
2. **Nombre exacto ES o EN** (`findByNameExact`) — ej. "Fe" -> topic "fe"
3. **Palabra completa en nombre** (`findByWordInName`) — ej. "fe" -> "Falta de fe", "Bautismo de profesion de fe"
4. **Alias exacto o palabra completa** (`findByWordInAlias`) — ej. "fear" -> "temor-de-dios" (alias "fear")
5. **Prefijo en alias con score >= 0.92** (`findByPrefixInAlias`) — ej. "fe" -> "fear of the lord" (alias)
6. **Prefijo de palabra en nombre** (filtrado Kotlin sobre `getTopicsPaged`) — ej. "feli" -> Felipe, Felipe

**Reglas**:
- Las fases se acumulan: si fase 1 tiene match, se anade y se continua a fase 2.
- Corte cuando `results.size >= maxTotal`.
- Filtrado automatico: descarta temas con `verseCount <= 0` (los 269 sin versiculos).
- SQL exige match de **palabra completa** (delimitada por espacios) o **prefijo de palabra** (no substring). Esto evita ruido tipo "fe" -> "confesion", "enfermedad", "blasfemia".
- Sin desempate por `verseCount` que introducia ruido (ej. esposa con 46 versiculos entrando en lugar de temor-de-dios con 3).

**Resultado para `q="Fe"` con maxTotal=4**: `fe` (25 versiculos), `falta-de-fe` (16), `bautismo-profesional-de-fe` (12), `temor-de-dios` (3 via alias "fear of the lord").

**Resultado para `q="feli"` con maxTotal=10**: `felipe-apostol`, `felipe-evangelista` (fase 6, prefijo de palabra).

#### Componentes de SearchScreen

- **`SearchInputCard`**: campo de busqueda con placeholder rotativo (carousel cada 3.5s: `Juan 3:16`, `amor`, `Salmo 23`, `perdon`, `fe`).
- **`TestamentTabsRow`**: tabs `Toda la Biblia` / `Antiguo` / `Nuevo` que filtran la busqueda.
- **`BookFilterRow`**: dropdown con los 66 libros (filtrados por testamento). Boton X para limpiar filtros.
- **`RecentSearchesSection`**: ultimas 5 busquedas (top 10 con "Ver todo").
- **`AutoScrollingTopicCarousel`**: carrusel horizontal de 4 cards con rotacion aleatoria de temas. Ver seccion dedicada abajo.
- **`ExploreTopicsButton`**: boton "Explorar temas" debajo del carrusel que navega a `Screen.ExploreTopics`.
- **`TopicsSection`**: contenedor de `ExpandableTopicCard`s con los resultados de busqueda de temas.
- **`ExpandableTopicCard`**: tarjeta expandible que muestra nombre, descripcion completa y versiculos al expandir. Muestra conteo "N versiculos".
- **`ResultsHeader`**: muestra "Resultados para X · testamento · libro" cuando hay filtros activos.
- **`NoResultsState`**: estado contextual con icono y boton "Limpiar".

#### Re-busqueda automatica

- **Cambio de filtro** (testamento/libro) → re-busca inmediatamente con `runSearchImmediate()`.
- **Enter** → busqueda inmediata sin esperar debounce.
- **Typing** → espera 400ms (debounce) antes de buscar.
- **Boton X (clear)** → `uiState = Idle`, query vacio.

#### Carrusel de "Temas populares" con rotacion aleatoria

`AutoScrollingTopicCarousel` muestra 4 cards horizontales (160dp cada una) con temas del pool rotativo. **Regla de unicidad estricta entre cards**: nunca se repite el mismo tema en 2 o mas cards al mismo tiempo.

**Pool**: los 424 temas con versiculos se cargan con `getRotatableTopics(limit=500)` y se **barajan** (`.shuffled()`) en cada carga. Esto da variedad: las 4 cards iniciales muestran temas como Fe, Alianza, Celos de Dios, etc., no solo los top por versiculos (Matrimonio, Oracion, etc.).

**Logica**:
- El `LazyRow` mantiene un `SnapshotStateList<String>` con los slugs activos por card.
- Cada `RotatingCard` recibe un callback `occupied: (Int) -> Set<String>` que retorna los slugs de las otras cards.
- Al saltar: `available = pool.filter { it.slug !in occupied }`. Si todas las opciones estan bloqueadas, la card mantiene su tema.
- Rotacion cada 5 segundos, con offset escalonado de `indexOfInitial * 1100ms` para que no cambien al unisono.

**Card** (`PopularTopicCard`):
- Avatar circular con la inicial en color solido de la categoria y texto blanco.
- Titulo: nombre del subtema (Fe, Celos de Dios, Alianza, etc.) en color solido de la categoria con `FontWeight.Bold`.
- Descripcion del tema debajo en `MaterialTheme.colorScheme.onSurface`, max 3 lineas.
- Conteo "N versiculos" al final en color solido de la categoria.
- **No muestra el nombre de la categoria** (tema principal) para evitar duplicacion visual.

**Color por categoria** (`CategoryLabels.color(category)`):
- PERSON: marron `0xFF8D6E63`
- PLACE: verde `0xFF4CAF50`
- EVENT: naranja `0xFFFF9800`
- ATTRIBUTE_OF_GOD: rojo claro `0xFFE57373`
- DOCTRINE: azul `0xFF1976D2`
- CHRISTIAN_LIFE: verde claro `0xFF66BB6A`
- CHURCH: morado `0xFF7B1FA2`
- PROPHECY: rojo oscuro `0xFFD32F2F`
- SIN: rojo `0xFFC62828`
- COMPARATIVE_RELIGION: gris azulado `0xFF607D8B`
- BOOK: marron oscuro `0xFF5D4037` (no usado en el carrusel de categorias)
- Default: `BiblionGoldPrimary`

**Tipo de color**: `PopularTopic.topicColor: Int` (packed ARGB de 32 bits). Se obtiene con `CategoryLabels.color(category).toArgb()`. **Nunca** usar `Color.value.toLong()` porque interpreta el color como packed float (RGBA 64 bits) y produce un color gris/desaturado.

#### Pantalla "Explorar temas" con jerarquia

Dos pantallas nuevas para explorar los 424 temas con versiculos:

**`ExploreTopicsScreen`**: muestra 10 cards de categorias (sin BOOK porque los libros se acceden desde `BooksScreen`) con color, conteo de temas e icono de flecha. Tap en una card navega a `ExploreCategoryScreen`. Orden: BOOK, PERSON, PLACE, EVENT, ATTRIBUTE_OF_GOD, DOCTRINE, CHRISTIAN_LIFE, CHURCH, PROPHECY, SIN, COMPARATIVE_RELIGION (BOOK se filtra, queda con 10).

**`ExploreCategoryScreen`**: recibe `category: String` como argumento. Muestra los temas de esa categoria con un `OutlinedTextField` con busqueda en vivo (filtrado por `nameEs`, `nameEn`, `description`). Cada tema es un `ExpandableTopicCard` con la misma logica que en `SearchScreen`: tap en la cabecera expande y carga los versiculos via `TopicEngine.getVersesForTopic`. Tap en un versiculo navega al lector con la referencia exacta.

**Rutas**: `Screen.ExploreTopics` (route `explore_topics`) y `Screen.ExploreCategory` (route `explore_category/{category}`).

#### Esquema v3 de topics.db

`app/src/main/assets/databases/topics.db` contiene el esquema v3 con **693 temas canonicos** en 11 categorias, **1689 aliases** de OpenBible, **4704 referencias** de versiculos, y **236 relaciones** jerarquicas. De los 693 temas, **424 tienen versiculos** (61%) y **269 no** (39%).

**Tablas**:
- `topics`: 693 canonicos con descripcion y jerarquia
- `topic_aliases`: 1689 aliases de OpenBible con score (0.85-0.94)
- `topic_references`: 4704 versiculos del OSIS original
- `topic_relationships`: 236 jerarquias parent/child

**Indices UNIQUE** (declarados en `TopicEntity` Kotlin para coincidir con la DB):
- `idx_topics_slug` (UNIQUE en topics.slug)
- `idx_rel_parent`, `idx_rel_child`
- `index_topic_relationships_parent_slug_child_slug_relationship_type` (UNIQUE)

**Nota historica**: hubo un bug de schema mismatch entre la DB preempaquetada (v2) y el entity Kotlin (v3). Se resolvio anadiendo el UNIQUE index faltante al entity. **No se regenero la DB** una vez que el entity reconocio el schema correcto.

**Reglas de filtrado**:
- `verse_count > 0` para que un tema aparezca en busquedas, carrusel, o Explorar temas.
- `low_confidence = 0` en aliases para busqueda fuzzy.

**Queries principales en `TopicDao`**:
- `findBySlug(slug)` — fase 1 del pipeline
- `findByNameExact(q)` — fase 2
- `findByWordInName(word)` — fase 3
- `findByWordInAlias(word)` — fase 4
- `findByPrefixInAlias(prefix)` — fase 5
- `getTopicsPaged(limit, offset)` — fase 6 (filtrado Kotlin)
- `getTopicsWithVerses(limit, offset)` — Explorar temas (categorias)
- `getTopicsByCategory(category)` — Explorar temas (temas de una categoria)
- `getRotatableTopics(limit)` — pool del carrusel
- `getVersesForTopicSlug(slug, minScore, limit)` — versiculos al expandir card

#### Versiculos populares

Los 6 versiculos populares curados en `PopularVersesData` ya no se usan en el carrusel (ahora se rotan temas del pool). Se mantienen para compatibilidad con codigo legacy.

### Lectura de ensenanzas

`StudyDocReadScreen` debe mostrar el documento estructurado, no solo texto plano.

Funciones actuales:

- Render paginado de los 6 tipos de bloque vigentes con `AnnotatedString`.
- Cambio de version y comparacion en bloques `Verse`.
- Modo claro/oscuro desde la lectura.
- Filtro y administracion desde "Mis ensenanzas"; solo incluye documentos publicados.
- Boton de editar que navega al `StudyDocEditorScreen`.

## 7) Perfil y red de Biblion

La red de Biblion se maneja por el momento sobre Firebase/Firestore.

### Perfil

El documento principal del usuario vive en:

```text
users/{uid}
```

Campos relevantes:

- `uid`
- `email` / `correo`
- `nombres`
- `apellidos`
- `alias`
- `rol`: `LECTOR`, `PUBLICADOR`, `ADMIN`
- `estadoPublicador` / `estado_publicador`: `NO_APROBADO`, `PENDIENTE`, `APROBADO`, `SUSPENDIDO`
- `plan`: `FREE`, `GO`, `PLUS`
- `fotoPerfil` / `foto_perfil`
- `avatarColor` / `avatar_color`
- `biografia`
- `fechaRegistro` / `fecha_registro`

Si el usuario no tiene foto, la red debe representar su identidad con `avatarColor`.

Las fotos de perfil se guardan en Firebase Storage:

```text
profile_photos/{uid}/avatar.jpg
```

Reglas:

- Todo usuario autenticado debe poder completar nombres, apellidos y alias.
- El alias es el nombre visible dentro de la red.
- La foto de perfil es opcional; si no existe, se usa avatar de color.
- No guardar imagenes ni claves en el repositorio.
- **Reiniciar tutorial de lectura**: boton en panel de acciones del perfil que reinicia `GuidedTutorialId.READING` a step 0.

### Contadores sociales preparados

`users/{uid}` puede incluir:

- `totalEnsenanzasCreadas`
- `totalEnsenanzasPublicadas`
- `totalDescargas`
- `totalLikes`
- `totalGuardados`
- `totalComentarios`
- `totalSeguidores`
- `totalSiguiendo`

Estos valores son derivados de tablas/colecciones sociales y deben actualizarse de forma consistente cuando se implementen likes, descargas, favoritos, comentarios y seguidores.

### Modelo previsto de red

Entidades previstas:

- Usuario
- Ensenanza
- Seguidor
- Descarga
- Favorito
- Like
- Etiqueta
- EnsenanzaEtiqueta
- Comentario

Reglas principales:

- Todo usuario puede crear ensenanzas.
- Un lector puede tener ensenanzas `PRIVADA` o `NUBE`.
- Solo usuarios con `estadoPublicador = APROBADO` pueden publicar ensenanzas `PUBLICA`.
- Likes, comentarios, descargas publicas y favoritos sociales aplican principalmente a ensenanzas publicas.
- Los contadores son derivados, no fuente primaria de verdad.

## 8) Estado y ViewModel

- Nuevas acciones de usuario deben agregarse de forma consistente en `StudyOp` (operaciones puras) y aplicarse via `StudyDocViewModel.applyOp(op)`.
- El estado visible en el editor se modela en `StudyEditorUiState` (doc, selectedBlockId, isLoading, lastError, isSaving).
- La lista de documentos usa `StudyDocsListViewModel` con `StudyDocsListState` (docs, isLoading).
- Evitar efectos secundarios ocultos; preferir flujos explicitos con corrutinas/Flow.
- Si se agrega logica testeable, preferir funciones puras en `StudyDocEngine` o helpers internos con pruebas unitarias.
- Para tests, se permite inyectar dispatchers o desactivar semillas demo cuando mejore determinismo.

## 9) Datos, repositorios y Room

- Cambios de esquema Room deben ser compatibles y justificados.
- No romper datos existentes sin migracion.
- Mantener separacion entre entidades de persistencia y estado UI.
- Si se agrega cache o acceso a assets biblicos, seguir el patron de `BibleRepository` y caches dedicados.
- La Biblia se consulta desde la base SQLite preempaquetada `app/src/main/assets/databases/bible_content.db`.
- La base se genera desde los JSON fuente con `tools/build_bible_sqlite.py`; si se regeneran versiones, conservar la deduplicacion de libros por nombre normalizado para evitar duplicados como los de NVI.
- Las citas vinculadas deben conservar `book`, `chapter`, `verseStart`, `verseEnd` y `version`.
- **Documentos de estudio**: Room v3 `study_docs.db` con `StudyDocEntity` (id, remoteId, title, notebookRemoteId, ownerUid, tagsCsv, blockCount, version, docJson, isPublished). Toda evolucion futura requiere migracion compatible; no usar migracion destructiva.
- **Diccionario biblico local unificado**: `app/src/main/assets/databases/dictionary_v2.db` contiene 6,346 entradas (Easton's + Theographic). Generado por `tools/build_knowledge_sqlite.py`. Esquema version 2 (con metadata Theographic).
- **Historial de busquedas**: Room database `search_history.db` con `SearchHistoryEntity` (query, normalized_query, use_count, last_used_at). Se usa para mostrar busquedas recientes en `SearchScreen`. Migracion destructiva aceptable (datos regenerables).
- **Chats de Bibi**: Room database `bibi_chat.db` con `ChatSessionEntity` y `ChatMessageEntity`. Permite multiples sesiones independientes de conversacion.
- **Referencias cruzadas con voto crowdsourced** (`app/src/main/assets/databases/cross_references_votes.db`): 340,645 pares del dataset openbile.info (CC-BY 2026-06-15). Reemplazo completo del antiguo TSK. Tabla `cross_reference_votes` con `source_book`, `source_normalized_book`, `source_chapter`, `source_verse`, `target_references` (formato Biblion), `votes` (1-1279, INTERNO). Esquema version 1. La columna `votes` se usa internamente para ranking y filtrado (default `votes >= 10`). **NUNCA** debe exponerse al usuario en la UI de Bibi.

## 10) Navegacion

- Registrar rutas nuevas en `NavigationRoutes`, `AppNavigation` o `NavGraphShared` segun corresponda.
- Evitar duplicidad de rutas.
- Respetar `launchSingleTop` y `popUpTo` usados en la app.
- Las pantallas compartidas deben recibir dependencias como tema global mediante parametros, no accediendo a estado global oculto.
- **Rutas del nuevo modo estudio (v2)**:
  - `Screen.StudyDocsList` (route `study_docs_list`) → `StudyDocsListScreen`.
  - `Screen.StudyDocEditor` (route `study_doc_editor/{remoteId}`) → `StudyDocEditorScreen`.
  - `Screen.StudyDocRead` (route `study_doc_read/{remoteId}`) → `StudyDocReadScreen`.
- **Rutas de diccionario**:
  - `Screen.Dictionary` (route `dictionary`) → `DictionaryScreen`.
  - `Screen.ExploreDictionaryCategory` (route `dictionary_category/{category}`) → `DictionaryCategoryScreen`.
- **Rutas legacy (mantenidas para compatibilidad)**: `Screen.Ensenanzas` y `Screen.StudyRead` redirigen a `StudyDocsListRoute`.

## 11) Pruebas y validacion

Agregar o ajustar pruebas unitarias cuando se modifique logica:

- agrupacion de citas;
- validacion/normalizacion de etiquetas;
- autosave o carga de estudios;
- transformaciones de documentos de estudio.

Antes de proponer merge, validar al menos:

```powershell
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:testDebugUnitTest
```

Si se ejecuta un subconjunto, reportarlo claramente.

**Tests del nuevo modo estudio (v2)**: viven en `app/src/test/java/com/cristiancogollo/biblion/feature/studydocs/`:
- `domain/`: historia, Enter/listas, doble Backspace y edicion paginada.
- `editor/`: zoom, mapeo de gaps y puente de texto enriquecido.
- `engine/`: operaciones de lista y transformaciones del documento.
- `model/`: reglas de guardado, rangos y slicing de `StyledText`.
- `pagination/`: 16 casos del engine, layout paginado y claves de composicion.
- `ui/list/`: filtros y listado de documentos publicados.
- Inventario actual: 72 metodos `@Test` en 14 archivos.

Para ejecutar solo los tests del modo estudio:
```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.cristiancogollo.biblion.feature.studydocs.*"
```

## 12) Dependencias y build

- No duplicar librerias ya administradas en `gradle/libs.versions.toml`.
- Nuevas dependencias deben declararse con version centralizada en el catalogo.
- Evitar actualizaciones masivas de versiones sin necesidad.
- No agregar secretos a Gradle, `local.properties`, commits ni recursos Android. Las claves online de Bibi deben vivir en Cloudflare Worker secrets.
- `workers/**/node_modules/` y `.wrangler/` deben permanecer ignorados.

## 13) Commits y PR

- Commits en imperativo y con alcance claro.
  - Ejemplo: `feat: agrega filtros de ensenanzas`
  - Ejemplo: `docs: actualiza estado de modo estudio`
- PR debe incluir:
  - problema;
  - solucion aplicada;
  - riesgos/impacto;
  - evidencia de compilacion/pruebas;
  - capturas si cambia UI.

## 14) Que evitar

- Refactors globales no solicitados.
- Mezclar cambios visuales con cambios funcionales grandes sin razon.
- Introducir deuda tecnica marcada como TODO sin contexto.
- Romper compatibilidad de datos locales sin estrategia.
- Revertir cambios ajenos del usuario.

## 15) Regla de consistencia

Si existe conflicto entre este documento y una instruccion explicita del solicitante para una tarea puntual, prevalece la instruccion explicita para esa tarea, manteniendo el resto de estandares.
