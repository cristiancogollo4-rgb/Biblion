# Biblion Docs v2 — Arquitectura del Modo Estudio

Documento generado el 23 Jun 2026. Reemplaza al antiguo sistema `feature/study/`.

## Indice

1. [Vision y principios](#1-vision-y-principios)
2. [Estructura de archivos](#2-estructura-de-archivos)
3. [Modelo de datos](#3-modelo-de-datos)
4. [Engine — Operaciones puras](#4-engine--operaciones-puras)
5. [Persistencia Room](#5-persistencia-room)
6. [UI — Editor](#6-ui--editor)
7. [UI — Lista](#7-ui--lista)
8. [UI — Lectura](#8-ui--lectura)
9. [Integracion Bibi](#9-integracion-bibi)
10. [Sistema de etiquetas](#10-sistema-de-etiquetas)
11. [Navegacion](#11-navegacion)
12. [Tests](#12-tests)
13. [Cambios realizados](#13-cambios-realizados)
14. [Trabajo pendiente](#14-trabajo-pendiente)

---

## 1. Vision y principios

Biblion Docs v2 es un editor de estudio biblico con experiencia **estilo Google Docs**, adaptado al ecosistema de Biblion (Android, Kotlin, Compose, Material 3, Room).

### Principios tecnicos

1. **Modelo por operaciones puras**: `applyOperation(doc, op): Doc` es la unica forma de mutar. Todo deriva de ahi (undo, redo, version history, sync).
2. **Engine sin estado mutable**: `StudyDocEngine` es 100% funciones puras. ViewModel/Repository/UI trabajan con snapshots inmutables.
3. **Tests del engine desde dia 1**: cobertura obligatoria de cada operacion antes de cualquier UI.
4. **Single-user**: una ensenanza es personal. No se contempla edicion multi-usuario en este alcance.
5. **Biblion-themed**: azul, dorado, blanco, sin `dynamicColor`. Misma identidad visual.

---

## 2. Estructura de archivos

```
feature/studydocs/
├── model/
│   ├── BlockId.kt           (value class BlockId + DocId + generador CUID)
│   ├── VerseRef.kt          (referencia biblica: book, chapter, verseStart, verseEnd, version)
│   ├── DocMetadata.kt       (DocMetadata + DocTagGroups con 4 secciones)
│   ├── StyleRange.kt        (rango de estilo: start, endExclusive, bold, italic, etc.)
│   ├── TextStylePatch.kt    (patch parcial de estilo + TextStyleKind enum)
│   ├── StyledText.kt        (texto inmutable con List<StyleRange> + withText/withStyle/clearStyle)
│   ├── StudyBlock.kt        (sealed interface con 12 tipos de bloque)
│   └── StudyDoc.kt          (documento inmutable: id, title, blocks, metadata, version)
│
├── engine/
│   ├── StudyOp.kt           (10 operaciones selladas + OpResult)
│   ├── StudyDocEngine.kt    (apply puro: Pair<StudyDoc, OpResult>)
│   ├── StudyDocValidator.kt (8 tipos de ValidationIssue)
│   └── StudyDocNormalizer.kt(merge ranges, dedup dividers, strip trailing, intro auto)
│
├── data/
│   ├── StudyDocJson.kt      (serializacion JSON via kotlinx.serialization)
│   ├── StudyDocEntity.kt    (entidad Room con docJson blob)
│   ├── StudyDocDao.kt       (13 queries)
│   ├── StudyDocDatabase.kt  (Room v1, fallbackToDestructiveMigration)
│   └── StudyDocRepository.kt(fachada sobre DAO)
│
├── domain/
│   └── StudyDocViewModel.kt (MVVM con StudyEditorUiState)
│
├── ui/
│   ├── StudyDocRoutes.kt    (composables de ruteo: StudyDocsListRoute, StudyDocEditorRoute, StudyDocReadRoute)
│   ├── editor/
│   │   ├── StudyDocEditorScreen.kt  (lienzo principal + toolbar + slash + outline + drag handles)
│   │   ├── StyledTextRenderer.kt    (convierte StyledText → AnnotatedString)
│   │   ├── CanvasZoom.kt            (Modifier.canvasZoom con pinch-to-zoom + pan)
│   │   └── blocks/
│   │       ├── StyledTextEditor.kt   (BasicTextField con StyledText + AnnotatedString)
│   │       ├── TextBlockEditors.kt   (Paragraph, Heading, Quote)
│   │       └── MoreBlockEditors.kt   (List, Note, Reflection, Verse, Callout, Divider, PageBreak)
│   ├── list/
│   │   ├── StudyDocsListScreen.kt    ("Mis Documentos" con tarjetas)
│   │   └── StudyDocsListViewModel.kt
│   ├── read/
│   │   └── StudyDocReadScreen.kt     (render de los 12 tipos de bloque)
│   └── outline/
│       └── OutlinePanel.kt           (tabla de contenidos por headings)
│
└── tests/ (en app/src/test/.../feature/studydocs/)
    ├── engine/
    │   ├── StudyDocEngineInsertTest.kt   (5 tests)
    │   ├── StudyDocEngineEditTest.kt     (6 tests)
    │   ├── StudyDocEngineStyleTest.kt    (4 tests)
    │   ├── StudyDocEngineMoveTest.kt     (5 tests)
    │   └── StudyDocValidatorTest.kt      (6 tests)
    └── data/
        ├── StudyDocJsonTest.kt           (6 tests)
        └── StudyDocDaoTest.kt            (9 tests con Robolectric)
```

---

## 3. Modelo de datos

### 3.1 `StudyDoc` — Documento inmutable

```kotlin
data class StudyDoc(
    val id: DocId = DocId.generate(),
    val title: String = "",
    val blocks: List<StudyBlock> = emptyList(),
    val metadata: DocMetadata = DocMetadata.Empty,
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
)
```

Cada mutation via `StudyDocEngine.apply` genera una nueva instancia con `version` y `updatedAt` incrementados.

### 3.2 `StudyBlock` — 12 tipos

| Tipo | Descripcion | Campos clave |
|------|-------------|--------------|
| `Paragraph` | Texto libre | `text: StyledText` |
| `Heading` | Encabezado 1-6 | `level: Int`, `text`, `anchor: String?` |
| `BulletList` | Lista con vinetas | `items: List<StyledText>` |
| `NumberedList` | Lista numerada | `items: List<StyledText>` |
| `Quote` | Cita textual | `text`, `attribution: String?` |
| `Table` | Tabla | `rows: List<TableRow>`, `hasHeaderRow` |
| `Verse` | Versiculo biblico | `reference: VerseRef`, `primaryText`, `compareVersion`, `compareText`, `note` |
| `Note` | Nota | `text`, `collapsed` |
| `Reflection` | Reflexion personal | `prompt: String?`, `text`, `collapsed` |
| `Callout` | Destacado con color | `icon`, `color: Int`, `text` |
| `Divider` | Separador horizontal | — |
| `PageBreak` | Salto de pagina | — |

### 3.3 `StyledText` — Texto con estilos inline

```kotlin
data class StyledText(
    val raw: String = "",
    val ranges: List<StyleRange> = emptyList(),
)

data class StyleRange(
    val start: Int,         // inclusive
    val endExclusive: Int,  // exclusive
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val color: Int? = null,      // packed ARGB
    val background: Int? = null, // packed ARGB
    val fontSizeSp: Float? = null,
    val link: String? = null,
)
```

Operaciones:
- `withText(range, replacement)` → nuevo StyledText con texto de reemplazo y estilos reubicados
- `withStyle(range, patch)` → aplica TextStylePatch, cleared overlaps
- `clearStyle(range, kind)` → elimina un tipo de estilo en el rango
- `mergeRanges(ranges)` → merge estatico de rangos contiguos identicos

### 3.4 `DocMetadata` — Etiquetas

```kotlin
data class DocMetadata(
    val tags: List<String> = emptyList(),
    val notebook: String? = null,
    val authorUid: String? = null,
    val globalVersion: String = "rv1960",
)
```

`DocTagGroups` define 4 secciones:
- **Proposito**: predicacion, devocional, estudio-biblico, clase, discipulado, formacion
- **Audiencia**: jovenes, iglesia, lideres, universitarios, familias, simpatizantes, ninos, mujeres, hombres, ancianos, grupos-especiales, pastores
- **Tema**: identidad, fe, gracia, proposito, oracion, evangelismo, servicio, esperanza, doctrina, amor, misiones, adoracion
- **Estado**: borrador, en-preparacion, finalizado (single-selection)

---

## 4. Engine — Operaciones puras

### 4.1 `StudyOp` — 10 operaciones

```kotlin
sealed interface StudyOp {
    data class InsertBlock(val atIndex: Int, val block: StudyBlock) : StudyOp
    data class DeleteBlock(val blockId: BlockId) : StudyOp
    data class MoveBlock(val blockId: BlockId, val toIndex: Int) : StudyOp
    data class ReplaceBlock(val blockId: BlockId, val newBlock: StudyBlock) : StudyOp
    data class EditText(val blockId: BlockId, val range: IntRange, val newText: String) : StudyOp
    data class ApplyStyle(val blockId: BlockId, val range: IntRange, val patch: TextStylePatch) : StudyOp
    data class ClearStyle(val blockId: BlockId, val range: IntRange, val kind: TextStyleKind) : StudyOp
    data class UpdateTitle(val newTitle: String) : StudyOp
    data class UpdateMetadata(val newMetadata: DocMetadata) : StudyOp
    data class BulkApply(val ops: List<StudyOp>) : StudyOp
}

sealed interface OpResult {
    data class Ok(val affectedBlockIds: List<BlockId> = emptyList()) : OpResult
    data class Failed(val reason: String) : OpResult
}
```

### 4.2 `StudyDocEngine`

```kotlin
object StudyDocEngine {
    fun apply(doc: StudyDoc, op: StudyOp): Pair<StudyDoc, OpResult>
    fun applyAll(doc: StudyDoc, ops: List<StudyOp>): Pair<StudyDoc, List<OpResult>>
    fun findText(doc: StudyDoc, query: String): List<TextHit>
    fun plainTextOf(doc: StudyDoc): String
    fun wordCountOf(doc: StudyDoc): Int
    fun charCountOf(doc: StudyDoc): Int
    fun headingsOf(doc: StudyDoc): List<StudyBlock.Heading>
    fun findBlock(doc: StudyDoc, id: BlockId): StudyBlock?
}
```

### 4.3 `StudyDocValidator`

8 tipos de `ValidationIssue`:
- `DuplicateBlockId` — dos bloques con el mismo id
- `EmptyTitle` — documento con bloques sin titulo
- `InvalidHeadingLevel` — heading fuera de 1..6
- `InvalidVerseCompare` — compareText sin compareVersion
- `StyleOutOfBounds` — rango de estilo excede longitud de texto
- `EmptyTableRow` — fila sin celdas
- `InconsistentTableWidth` — filas con distinto ancho
- `MissingRequiredTag` — falta tag obligatorio por seccion

---

## 5. Persistencia Room

### 5.1 `StudyDocEntity`

```kotlin
@Entity(tableName = "study_docs", indices = [
    Index(value = ["remoteId"], unique = true),
    Index("notebookRemoteId"),
    Index("updatedAt"),
    Index("ownerUid"),
    Index("isDirty"),
])
data class StudyDocEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String,
    val title: String,
    val notebookRemoteId: String? = null,
    val ownerUid: String? = null,
    val tagsCsv: String = "",
    val blockCount: Int = 0,
    val version: Int = 1,
    val docJson: String,        // serializacion completa del documento
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val lastSyncedAt: Long? = null,
    val syncVersion: Long = 0,
    val isDirty: Boolean = true,
)
```

### 5.2 `StudyDocJson`

Configuracion de serializacion:
- `classDiscriminator = "type"` — cada StudyBlock se serializa con su tipo
- `ignoreUnknownKeys = true` — compatibilidad forward
- `encodeDefaults = true` — preserva defaults explicitos

### 5.3 `StudyDocDatabase`

- Room v1, `study_docs.db`
- `fallbackToDestructiveMigration(dropAllTables = true)`
- Thread-safe con double-checked locking

---

## 6. UI — Editor

### 6.1 `StudyDocEditorScreen`

Lienzo principal del editor con:
- **Top bar**: titulo editable, boton Back, boton Save, toggle outline
- **Toolbar**: bold (Ctrl+B), italic (Ctrl+I), underline (Ctrl+U), boton "+" para slash commands
- **LazyColumn**: render de bloques con drag handles y seleccion
- **Outline panel**: panel lateral con tabla de contenidos (toggle)
- **Slash command menu**: AlertDialog con 14 tipos de bloque

### 6.2 `BlockWithHandle`

Wrapper por bloque con:
- Drag handle izquierdo (icono MenuBook)
- Highlight de seleccion (primary alpha 8%)
- Long-press para multi-bloque
- Render especifico segun tipo

### 6.3 `StyledTextEditor`

`BasicTextField` que usa `AnnotatedString` derivado de `StyledText.ranges`.
- Soporta placeholder
- Soporta seleccion y onChange
- Integra `pointerInput` para detectar taps

### 6.4 `StyledTextRenderer`

Convierte `StyledText` a `AnnotatedString`:
- bold → `FontWeight.Bold`
- italic → `FontStyle.Italic`
- underline → `TextDecoration.Underline`
- strikethrough → `TextDecoration.LineThrough`
- color → `Color(packedInt)`
- background → `Color(packedInt)`
- fontSizeSp → `TextUnit`

### 6.5 Todos los tipos de bloque renderizados

| Tipo | Renderizador | Caracteristicas |
|------|-------------|----------------|
| Paragraph | `ParagraphBlockEditor` | Texto libre con StyledTextEditor |
| Heading | `HeadingBlockEditor` | Tamano segun nivel (1-6) |
| BulletList | `ListBlockEditor` | Items con • + boton eliminar |
| NumberedList | `NumberedListBlockEditor` | Items con N. |
| Quote | `QuoteBlockEditor` | Icono " + atribucion |
| Table | (inline Text) | Muestra dimensiones |
| Verse | `VerseBlockEditor` | Referencia + primaryText + compare |
| Note | `NoteBlockEditor` | Card tertiaryContainer |
| Reflection | `ReflectionBlockEditor` | Card secondaryContainer + prompt |
| Callout | `CalloutBlockEditor` | Card con color personalizable |
| Divider | `DividerBlockView` | HorizontalDivider |
| PageBreak | `PageBreakBlockView` | Indicador de salto |

### 6.6 Zoom con gestos

`Modifier.canvasZoom(minScale=0.5f, maxScale=3.0f)`:
- `detectTransformGestures` para pinch + pan
- Scale: `(scale * zoom).coerceIn(0.5f, 3.0f)`
- Offset: `offsetX += pan.x`, `offsetY += pan.y`
- Layout: `width * scale`, `height * scale`, `place(offsetX, offsetY)`

---

## 7. UI — Lista

### 7.1 `StudyDocsListScreen`

Pantalla "Mis Documentos":
- Fab: boton "Nuevo" (ExtendedFloatingActionButton)
- LazyColumn con StudyDocCard
- Estado vacio con mensaje
- Observe de todos los documentos activos (deletedAt IS NULL)

### 7.2 `StudyDocCard`

Tarjeta con:
- Titulo (o "Sin titulo")
- Conteo: "N bloques - M palabras"
- Card clickable que navega a StudyDocRead

---

## 8. UI — Lectura

### 8.1 `StudyDocReadScreen`

Render completo de los 12 tipos de bloque:
- Top bar con titulo + Back + boton Edit
- LazyColumn con separadores entre bloques
- Estilos inline via `StyledTextRenderer.toAnnotatedString`

### 8.2 Tipos renderizados en lectura

Todos los tipos se renderizan con `StyledTextRenderer.toAnnotatedString` para preservar estilos inline. Formato especial:
- Listas: bullet/number con indentacion
- Quotes: icono " + atribucion
- Verse: referencia + texto + comparacion
- Note/Reflection: cards coloreadas
- Divider: HorizontalDivider
- PageBreak: indicador visual

---

## 9. Integracion Bibi

### 9.1 Estado actual

Bibi sigue integrada via los motores locales en `feature/bibi/`:
- `KnowledgeEngine` — orquestador con deteccion de intencion
- `DictionaryEngine` — definiciones por categoria
- `VerseResolver` — resolucion de versiculo activo
- `CrossReferenceVoteEngine` — referencias cruzadas

La UI de Bibi en el nuevo editor esta pendiente (PR futuro). Actualmente:
- El `StudyAssistantPanel` (viejo) fue eliminado con `feature/study/`.
- `ReaderAssistantOverlay` es un stub vacio en `StudyViewModelStub.kt`.
- `StudyAssistantRepository` sigue funcionando pero esta en el `feature/study/data/` que fue eliminado. (Los calls sites en `StudyAssistantPanel` fueron eliminados.)

### 9.2 Plan de re-integracion (futuro)

- Crear nuevo `BibiOverlay` en `feature/studydocs/ui/bibi/`
- Integrar con `StudyDocEditorScreen` como panel lateral
- Insertar respuestas como bloques `Note` o `Reflection` via `StudyOp.InsertBlock`

---

## 10. Sistema de etiquetas

Definido en `DocTagGroups` (dentro de `DocMetadata.kt`):

```kotlin
object DocTagGroups {
    val purposeTags = listOf("predicacion", "devocional", "estudio-biblico", "clase", "discipulado", "formacion")
    val audienceTags = listOf("jovenes", "iglesia", "lideres", "universitarios", "familias", "simpatizantes", "ninos", "mujeres", "hombres", "ancianos", "grupos-especiales", "pastores")
    val topicTags = listOf("identidad", "fe", "gracia", "proposito", "oracion", "evangelismo", "servicio", "esperanza", "doctrina", "amor", "misiones", "adoracion")
    val stateTags = listOf("borrador", "en-preparacion", "finalizado")
}
```

Validacion via `StudyDocValidator.validateMetadata()`:
- ≥1 tag de proposito
- ≥1 tag de audiencia
- ≥1 tag de tema
- exactamente 1 tag de estado

---

## 11. Navegacion

### 11.1 Rutas nuevas

| Ruta | Screen | Parametros |
|------|--------|-----------|
| `study_docs_list` | `StudyDocsListScreen` | — |
| `study_doc_editor/{remoteId}` | `StudyDocEditorScreen` | `remoteId` (o "new") |
| `study_doc_read/{remoteId}` | `StudyDocReadScreen` | `remoteId` |

### 11.2 Rutas legacy (compatibilidad)

| Ruta | Redirige a |
|------|-----------|
| `ensenanzas` | `StudyDocsListRoute` |
| `study-read/{studyId}` | `StudyDocsListRoute` |

### 11.3 Flujo de navegacion

```
Home → "Mis Documentos" (study_docs_list)
       ├──→ Crear nuevo (study_doc_editor/new)
       ├──→ Abrir existente → lectura (study_doc_read/{id})
       └──→ Editar desde lectura → editor (study_doc_editor/{id})
```

---

## 12. Tests

### 12.1 Total: 41 tests en `feature/studydocs/`

| Archivo | Tests | Tipo |
|---------|-------|------|
| `StudyDocEngineInsertTest` | 5 | Unit (insert al inicio, medio, final, duplicado, version bump) |
| `StudyDocEngineEditTest` | 6 | Unit (replace, insert, no-existe, heading level, delete, move) |
| `StudyDocEngineStyleTest` | 4 | Unit (bold, merge contiguo, clear con split, shift con edit) |
| `StudyDocEngineMoveTest` | 5 | Unit (noop, swap vecinos, no-existe, replace same id, replace diff id) |
| `StudyDocValidatorTest` | 6 | Unit (vacio, duplicados, heading level, verse compare, style bounds, metadata) |
| `StudyDocJsonTest` | 6 | Unit (round-trip doc vacio, doc con 11 tipos, tabla, verse con compare, JSON malformado, discriminator) |
| `StudyDocDaoTest` | 9 | Robolectric (insert, update, soft delete, hard delete, observe by owner, search title, search tag, mark synced, count) |

Ejecutar: `.\gradlew.bat :app:testDebugUnitTest --tests "com.cristiancogollo.biblion.feature.studydocs.*"`

---

## 13. Cambios realizados

### 13.1 Codigo nuevo (29 archivos de produccion)

`feature/studydocs/`:
- `model/` — 8 archivos (BlockId, VerseRef, DocMetadata, StyleRange, TextStylePatch, StyledText, StudyBlock, StudyDoc)
- `engine/` — 4 archivos (StudyOp, StudyDocEngine, StudyDocValidator, StudyDocNormalizer)
- `data/` — 5 archivos (StudyDocJson, StudyDocEntity, StudyDocDao, StudyDocDatabase, StudyDocRepository)
- `domain/` — 1 archivo (StudyDocViewModel)
- `ui/editor/` — 4 archivos (StudyDocEditorScreen, StyledTextRenderer, CanvasZoom)
- `ui/editor/blocks/` — 3 archivos (StyledTextEditor, TextBlockEditors, MoreBlockEditors)
- `ui/list/` — 2 archivos (StudyDocsListScreen, StudyDocsListViewModel)
- `ui/read/` — 1 archivo (StudyDocReadScreen)
- `ui/outline/` — 1 archivo (OutlinePanel)
- `ui/StudyDocRoutes.kt` — 1 archivo (ruteo)

### 13.2 Tests nuevos (7 archivos, 41 tests)

`app/src/test/.../feature/studydocs/`:
- `engine/` — 5 archivos (Insert, Edit, Style, Move, Validator)
- `data/` — 2 archivos (Json, DaoTest)

### 13.3 Codigo eliminado

- `feature/study/` — todo (data, domain, presentation: ~7,000 lineas)
- `app/src/test/.../StudyDocumentEngineTest.kt`
- `app/src/test/.../StudyViewModelTest.kt`
- `app/src/test/.../StudyTagSelectorTest.kt`
- `app/src/test/.../TeachingFiltersTest.kt`
- `app/src/test/.../ChatSessionTest.kt` (dependia de `StudyAssistantRequest`)

### 13.4 Codigo modificado

- `NavigationRoutes.kt` — +3 rutas nuevas (StudyDocsList, StudyDocEditor, StudyDocRead)
- `NavGraphShared.kt` — +3 composable entries + redirecciones legacy
- `HomeScreen.kt` — "Mis Ensenanzas" → `Screen.StudyDocsList`
- `ReaderScreen.kt` — eliminado del bloque estudio (stub StudyViewModel)
- `FirestoreSyncManager.kt` — sync de estudios deshabilitado (pushStudies, applyRemote* eliminados)
- `MainActivity.kt` — importar .biblion usa nuevo `StudyDocJson.decode`
- `ProfileViewModel.kt` — conteo de estudios usa `StudyDocDatabase`
- `DatabaseSchemaValidationTest.kt` — arreglado sintaxis de nombre de funcion (`>= 80` → `ge80`)
- `DictionaryRepository.kt` — arreglado `toDomain()` faltante en `DictionarySearchResult`

### 13.5 Archivos nuevos de compatibilidad

- `StudyViewModelStub.kt` — `StudyViewModel` + `StudyIntent` + `StudyUiState` + `ReaderAssistantOverlay` stubs para que `ReaderScreen` compile
- `MainDispatcherRule.kt` — regla de test compartida (antes en StudyViewModelTest)

### 13.6 Paquetes creados

- `com.cristiancogollo.biblion.feature.studydocs.model`
- `com.cristiancogollo.biblion.feature.studydocs.engine`
- `com.cristiancogollo.biblion.feature.studydocs.data`
- `com.cristiancogollo.biblion.feature.studydocs.domain`
- `com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks`
- `com.cristiancogollo.biblion.feature.studydocs.ui.list`
- `com.cristiancogollo.biblion.feature.studydocs.ui.read`
- `com.cristiancogollo.biblion.feature.studydocs.ui.outline`

---

## 14. Trabajo pendiente

| Prioridad | Tarea | Estimacion |
|-----------|-------|-----------|
| Alta | Eliminar envio de `userName` al Worker en `StudyAssistantRepository` | 1 dia |
| Alta | Crear UI de tags + metadata (dialog de guardar con plantillas) | 1 semana |
| Media | Find & replace en el editor | 3-4 dias |
| Media | Word count + char count visible en status bar | 1-2 dias |
| Media | Re-integrar Bibi overlay en el nuevo editor | 2-4 dias |
| Media | Re-habilitar sync a Firestore para `StudyDocEntity` | 1 semana |
| Baja | Version history con snapshots y diff visual | 1-2 semanas |
| Baja | Import/Export DOCX y Markdown | 2 semanas |
| Baja | Internacionalizacion (strings.xml) | 2-3 dias |
| Baja | Mejorar render de tablas en el editor | 1-2 dias |
| Baja | Eliminar stubs (`StudyViewModelStub.kt`, `ReaderAssistantOverlay`) | 1 dia |
| Baja | Limpiar deprecation warnings de `fallbackToDestructiveMigration` | 1 dia |
| Baja | Eliminar 3 tests pre-existentes fallando (dictionary_v2 count, DictionaryEngine) | 1 dia |
| Baja | Auditoria `BibleBookMapper` | 1 dia |
