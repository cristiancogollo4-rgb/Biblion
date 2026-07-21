# PLAN: Hoja paginada estilo Word/Google Docs (modo estudio)

> **Documento autocontenido**: cualquier agente puede ejecutar este plan sin contexto adicional. Toda la información técnica (firmas, file:line, decisiones) está fijada aquí.

---

## 0. Objetivo y requisito del usuario

Reemplazar la "hoja que crece con cada Enter" por una **hoja de tamaño fijo (carta 850×1100dp) con paginación real estilo Word/Google Docs**:
- El contenido se parte entre páginas (párrafos, listas, versículos, citas — TODO es partible).
- Scroll vertical continuo sobre páginas apiladas (NO swipe).
- Zoom visual 0.75x–2.0x con reescalado lineal (SIN reflow).
- **El contenido es idéntico en cualquier dispositivo** (el tamaño de hoja no depende de la pantalla).
- Compartido por editor (modo split + standalone) y modo lectura.

**Requisito explícito del usuario**: "la idea es que el contenido no se redimensione según la pantalla, sino que en cualquier dispositivo sea el mismo". Eso define el modelo Word/Pages, no el modelo Notion (continuous reflow).

---

## 1. Estado REAL actual del codebase (verificado el 2026-07-20)

### ✅ Ya implementado y compilando

`./gradlew.bat :app:compileDebugKotlin` → BUILD SUCCESSFUL.

| Archivo | Estado | Notas |
|---------|--------|-------|
| `feature/studydocs/model/StyledText.kt` | ✅ `slice(range)` implementado | Líneas 30-42. Recorta texto + ajusta `StyleRange`. |
| `feature/studydocs/model/DocConfig.kt` | ✅ `PageDimensions` existe | `LETTER_WIDTH=850.dp`, `LETTER_HEIGHT=1100.dp`, `PAGE_MARGIN=48.dp` (nota: no se usa actualmente, los Card no aplican margen interno). |
| `feature/studydocs/ui/editor/DocumentZoomState.kt` | ✅ Simplificado | Solo `zoom`, `step`, `set`, `reset`. Rango 0.75-2.0. `PRESETS=[0.75,1.0,1.25,1.5,2.0]`. |
| `feature/studydocs/ui/editor/ZoomMenu.kt` | ✅ Implementado | Botón "NN%" + DropdownMenu con presets + opcional A-/A+. |
| `feature/studydocs/ui/pagination/PageFragment.kt` | ⚠️ Parcial | Ver §2.1. |
| `feature/studydocs/ui/pagination/PaginationEngine.kt` | ⚠️ Con bugs | Ver §2.2. |
| `feature/studydocs/ui/pagination/PaginatedSheet.kt` | ⚠️ Funcional pero con issues | Ver §2.3. |
| `feature/studydocs/ui/editor/UnifiedBlockRenderer.kt` | ✅ Ambas sobrecargas | `UnifiedBlockRenderer(block=...)` y `UnifiedBlockRenderer(fragment=...)`. `renderFragmentReadOnly` + `originBlock` + `buildTextStyle`. |
| `feature/studydocs/ui/editor/StudyDocEditorScreen.kt` | ✅ Usa `PaginatedSheet` | Modo split (50/50 con `BibleReaderPane`) + standalone (Scaffold + `EditorToolbar` + `PaginatedSheet`). |
| `feature/studydocs/ui/editor/StudyModeEditorPanel.kt` | ✅ Usa `PaginatedSheet` | Header + Toolbar (con `ZoomMenu`) + `PaginatedSheet`. |
| `feature/studydocs/ui/read/StudyDocReadScreen.kt` | ✅ Usa `PaginatedSheet` | Con `ZoomMenu` en actions. |
| `feature/studydocs/ui/StudyDocRoutes.kt` | ✅ | `StudyDocEditorRoute` crea `StudyDocSplitViewModel` y pasa `isSplitMode=true`. |
| `feature/studydocs/ui/editor/VirtualSheetContainer.kt` | ✅ ELIMINADO | No existe. |
| `strings.xml` | ✅ | `zoom_in`, `zoom_out` definidos (líneas 291-292). |
| Tests `DocumentZoomStateTest.kt` | ✅ 6 tests en `feature/studydocs/editor/` | JUnit4 puro. |
| Tests `StyledTextSliceTest.kt` | ✅ 7 tests en `feature/studydocs/model/` | JUnit4 puro. |

### ❌ Faltante / roto

| Problema | Severidad |
|----------|-----------|
| **NO HAY tests de `PaginationEngine`** (carpeta `feature/studydocs/pagination/` vacía). El archivo `PaginationEngineTest.kt` que existió se perdió. | 🔴 Alta |
| **Bug lógico en `PaginationEngine.paginatePartible`**: `pageHeight = remainingHeight` (línea ~229) mezcla conceptos. Después del primer corte de página, los siguientes cortes usan un `pageHeight` ya consumido → páginas con 1 sola línea. | 🔴 Alta |
| **Bug `cursorY` acumulado**: `paginatePartible` devuelve `cursorY` local (se reinicia en cada corte), pero el caller lo asigna al `cursorY` global, perdiendo el acumulado entre bloques. | 🔴 Alta |
| **`Quote` mal tratado**: se marca como `Whole` + fuerza `cursorY = pageHeightPx` + `openPage()` (líneas ~173-180) → siempre va sola dejando hueco enorme. El plan declara `Quote` partible. | 🔴 Alta |
| **`QuoteSlice` no existe en `PageFragment.kt`** (el plan original lo definía). El `when` de `PaginationEngine` no tiene rama `Quote` que cree `QuoteSlice`. | 🔴 Alta |
| **`TextStyle.Default` en medición**: `PaginationEngine` mide con `defaultTextStyle = TextStyle.Default` (línea 29, 218), ignorando fontSize/fontFamily/fontWeight reales del bloque → alturas de línea incorrectas → cortes en lugares equivocados. | 🟡 Media |
| **4 `Log.d("BIBLION_STUDY", ...)` en producción** en `PaginationEngine.kt` (líneas 62, 188, 208, 210) y `PaginatedSheet.kt` (línea 72). | 🟡 Baja |
| **Línea muerta** `PaginationEngine.kt:187`: `if (pages.isEmpty()) pages.add(mutableListOf<PageFragment>().also { it -> })` — inalcanzable + `also { it -> }` no hace nada. | 🟡 Baja |
| **`PaginatedSheet` no aplica `PAGE_MARGIN`** interno en los Card → contenido pegado a los bordes de la hoja. | 🟡 Media |
| **`PaginatedSheet` no tiene `imePadding()`** → el teclado tapa el fragmento activo al editar. | 🟡 Media |
| **`PaginatedSheet` no hace scroll-to-focused** cuando cambia `activeBlockId`. | 🟡 Media |
| **`PaginatedSheet` mide en `Density(1f)` fijo** pero recibe `pageWidthPx/toPx()` del `LocalDensity` actual → si el dispositivo tiene densidad ≠ 1, el ancho/alto de página medido no coincide con `LETTER_WIDTH`/`LETTER_HEIGHT` en dp. Inconsistencia. | 🟡 Media |
| **Bug preexistente (NO de esta tarea)**: 3 tests de `StyledTextRangeTest.withText` fallan en baseline (verificado con `git stash`). Reportar, no arreglar aquí. | 🟡 Info |

---

## 2. Bugs a corregir — detalle exacto

### 2.1 `PageFragment.kt` — falta `QuoteSlice`

**Archivo**: `app/src/main/java/com/cristiancogollo/biblion/feature/studydocs/ui/pagination/PageFragment.kt`

**Cambios**:
1. Añadir entre `VerseSlice` (línea 50) y `Whole` (línea 61):
```kotlin
data class QuoteSlice(
    override val originBlockId: BlockId,
    val block: StudyBlock.Quote,
    val charStart: Int,
    val charEndExclusive: Int,
) : PageFragment
```
2. En `UnifiedBlockRenderer.kt`, función `originBlock(...)` (~línea 292) y `renderFragmentReadOnly(...)` (~línea 220): añadir rama `is PageFragment.QuoteSlice`. En `originBlock`:
```kotlin
is PageFragment.QuoteSlice -> block
```
En `renderFragmentReadOnly`, rama nueva (entre `VerseSlice` y el cierre del `when`):
```kotlin
is PageFragment.QuoteSlice -> {
    val sliced = fragment.block.text.slice(fragment.charStart until fragment.charEndExclusive)
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text(
            text = sliced.toAnnotatedString(),
            style = textStyle.copy(fontStyle = FontStyle.Italic),
            modifier = Modifier.padding(start = 16.dp),
        )
        // La atribución solo se muestra en el ÚLTIMO slice del Quote.
        // Como no sabemos aquí si es el último, se delega al slice cuyo
        // charEndExclusive == block.text.length.
        if (fragment.charEndExclusive >= fragment.block.text.length &&
            !fragment.block.attribution.isNullOrBlank()) {
            Text(
                "\u2014 ${fragment.block.attribution}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    }
}
```
Imports necesarios en `UnifiedBlockRenderer.kt`: ya están `HorizontalDivider`, `MaterialTheme`, `FontStyle`, `Column`, `Modifier.padding`.

### 2.2 `PaginationEngine.kt` — bugs lógicos (REESCRITURA COMPLETA recomendada)

**Archivo**: `app/src/main/java/com/cristiancogollo/biblion/feature/studydocs/ui/pagination/PaginationEngine.kt`

Reescribir el `object PaginationEngine` completo con esta especificación:

**Firma pública** (mantener compatibilidad con `PaginatedSheet.kt:65`):
```kotlin
object PaginationEngine {
    fun paginate(
        blocks: List<StudyBlock>,
        pageWidthPx: Float,
        pageHeightPx: Float,
        textMeasurer: TextMeasurer,
        density: Density = Density(1f),
    ): List<Page>
}
```

**Contrato del algoritmo**:
1. `blocks.isEmpty() || pageWidthPx <= 0 || pageHeightPx <= 0` → devuelve `listOf(Page(0, emptyList()))`.
2. Recorre bloques en orden manteniendo estado mutable: `pages: MutableList<MutableList<PageFragment>>`, `currentFragments`, `cursorY: Float`.
3. `openPage()`: si `currentFragments.isNotEmpty()` lo añade a `pages`, reinicia `currentFragments` y `cursorY = 0f`.
4. Para cada bloque, según su tipo:
   - `Paragraph`, `Heading`, `Quote`, `Verse` → partible por línea (ver §2.2.1).
   - `BulletList` → cada `item` se parte por línea independientemente (slice con `itemIndex`).
   - `OrderedList` → igual que BulletList pero con `OrderedListItemSlice`.
   - (No hay otros tipos hoy: `StudyBlock` solo tiene 6 subclases: Paragraph, Heading, BulletList, OrderedList, Verse, Quote. Verificado en `StudyBlock.kt:7-89`.)
5. Al final: si `currentFragments.isNotEmpty()` → `openPage()`. Si `pages.isEmpty()` → añade una página vacía. Devuelve `pages.mapIndexed { idx, frags -> Page(idx, frags) }`.

**2.2.1 `placePartible(...)` — la pieza central** (reemplaza al `paginatePartible` bugueado):

```kotlin
private fun placePartible(
    text: String,
    sliceFactory: (start: Int, endExclusive: Int) -> PageFragment,
    pageWidthPx: Float,
    pageHeightPx: Float,         // ← ALTURA TOTAL de página (NO remaining)
    cursorY: Float,              // ← cursor actual (entrada/salida vía retorno)
    density: Density,
    textMeasurer: TextMeasurer,
    style: TextStyle,            // ← estilo REAL del bloque (NO TextStyle.Default)
    onSliceEmitted: (PageFragment) -> Unit,
    onPageFilled: () -> Unit,
): Float  // devuelve el NUEVO cursorY
```

Algoritmo:
```
if text.isEmpty():
    onSliceEmitted(sliceFactory(0, 0))
    return cursorY   // sin consumir espacio

layout = textMeasurer.measure(text, style, constraints=Constraints(maxWidth=pageWidthPx.toInt()), density=density)
totalLength = text.length
sliceStart = 0
localY = cursorY   // cursor acumulado dentro de la página actual

while sliceStart < totalLength:
    lineIndex = layout.getLineForOffset(sliceStart)
    lineEnd = layout.getLineEnd(lineIndex, false)   // exclusivo
    if lineEnd <= sliceStart: break   // safety
    lineHeight = (layout.getLineBottom(lineIndex) - layout.getLineTop(lineIndex)).coerceAtLeast(1f)

    if localY + lineHeight > pageHeightPx:
        # La línea no cabe en la página actual.
        # Cerrar el slice pendiente (si hay contenido acumulado) y abrir nueva página.
        if sliceStart > <sliceStart al entrar al while>:
            # Ya emitimos un slice parcial del mismo bloque en esta página;
            # pero como sliceStart se actualiza solo tras emitir, aquí no hay nada que cerrar.
            pass
        # CASO ESPECIAL: si la página está vacía (localY == 0) y ni una línea cabe,
        # forzar emitir la línea para garantizar progreso (bloque más alto que la página).
        if localY == 0f:
            onSliceEmitted(sliceFactory(sliceStart, lineEnd))
            sliceStart = lineEnd
            # No abrimos página: ya estamos en una vacía. Continuamos.
            continue
        else:
            onPageFilled()
            localY = 0f
            continue   # reintentar sin avanzar sliceStart

    # La línea cabe.
    localY += lineHeight
    sliceStart = lineEnd

# Tras procesar todas las líneas: emitir el slice final del bloque con el rango pendiente.
# Implementación más simple: acumular sliceStartStart al inicio del bloque y emitir UN slice
# por "visita a la página". Ver variante abajo.
```

**⚠️ IMPORTANTE — decisión de diseño para evitar complejidad**:

La lógica anterior tiene un problema: si un bloque ocupa varias páginas, hay que emitir **múltiples slices** (uno por página), cada uno con su `charStart..charEndExclusive` contiguo.

**Implementación recomendada (más simple y correcta)**:

```kotlin
private fun placePartible(
    text: String,
    sliceFactory: (start: Int, endExclusive: Int) -> PageFragment,
    pageWidthPx: Float,
    pageHeightPx: Float,
    cursorY: Float,
    density: Density,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    onSliceEmitted: (PageFragment) -> Unit,
    onPageFilled: () -> Unit,
): Float {
    if (text.isEmpty()) {
        onSliceEmitted(sliceFactory(0, 0))
        return cursorY
    }
    val layout = textMeasurer.measure(
        text = text,
        style = style,
        constraints = Constraints(maxWidth = pageWidthPx.toInt()),
        density = density,
    )
    val totalLength = text.length
    var localY = cursorY
    var sliceStart = 0
    var i = 0
    while (sliceStart < totalLength) {
        val lineIndex = layout.getLineForOffset(sliceStart)
        val lineEnd = layout.getLineEnd(lineIndex, false).coerceAtMost(totalLength)
        if (lineEnd <= sliceStart) break  // safety anti-bucle infinito
        val lineHeight = (layout.getLineBottom(lineIndex) - layout.getLineTop(lineIndex)).coerceAtLeast(1f)

        val fits = localY + lineHeight <= pageHeightPx
        if (!fits && localY > 0f) {
            // La línea no cabe y la página NO está vacía: cerrar página y reintentar.
            onPageFilled()
            localY = 0f
            continue
        }
        // fits == true  O  (fits == false Y localY == 0): consumir la línea.
        localY += lineHeight
        sliceStart = lineEnd
        i++
        if (i > 100_000) break  // safety hard anti-bucle
    }
    // Emitir el slice final del bloque (lo que cupo en la página actual desde el último corte).
    // PERO: si el bloque cruzó páginas, cada cruce debió emitir su propio slice.
    // Simplificación: llevar un "pageSliceStart" que se actualiza al cerrar página.
    // → Ver implementación final abajo (versión con pageSliceStart).
    return localY
}
```

**✅ Implementación FINAL correcta y completa** (usar esta):

```kotlin
private fun placePartible(
    text: String,
    sliceFactory: (start: Int, endExclusive: Int) -> PageFragment,
    pageWidthPx: Float,
    pageHeightPx: Float,
    cursorYStart: Float,
    density: Density,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    onSliceEmitted: (PageFragment) -> Unit,
    onPageFilled: () -> Unit,
): Float {
    if (text.isEmpty()) {
        onSliceEmitted(sliceFactory(0, 0))
        return cursorYStart
    }
    val layout = textMeasurer.measure(
        text = text,
        style = style,
        constraints = Constraints(maxWidth = pageWidthPx.toInt()),
        density = density,
    )
    val totalLength = text.length
    var cursorY = cursorYStart
    var sliceStart = 0
    var guard = 0
    while (sliceStart < totalLength) {
        val lineIndex = layout.getLineForOffset(sliceStart)
        val lineEnd = layout.getLineEnd(lineIndex, false).coerceAtMost(totalLength)
        if (lineEnd <= sliceStart) break
        val lineHeight = (layout.getLineBottom(lineIndex) - layout.getLineTop(lineIndex)).coerceAtLeast(1f)

        val fits = cursorY + lineHeight <= pageHeightPx
        if (!fits && cursorY > 0f) {
            // Cerrar página con el slice acumulado [sliceStart_original..sliceStart] pendiente.
            // Ojo: el slice pendiente se emite ANTES de cerrar la página si hubo líneas
            // desde el último corte. Como aquí solo cerramos, el slice se emite al
            // reintentar abajo si corresponde. Simplificamos: cerrar y resetear.
            onPageFilled()
            cursorY = 0f
            continue
        }
        cursorY += lineHeight
        sliceStart = lineEnd
        if (++guard > 100_000) break
    }
    // Slice final del bloque con todo el texto que falta desde sliceStart.
    // Como NO se emiten slices intermedios arriba (la versión anterior los emitía),
    // este enfoque solo es correcto si el bloque cabe en UNA página.
    // → Por eso la versión CORRECTA emite slices intermedios. Usar la versión de abajo.
    return cursorY
}
```

**⚠️ Lo anterior sigue siendo incompleto**. Para no alargar este plan con 5 versiones, la especificación **definitiva** es:

#### Especificación definitiva de `placePartible`

El motor debe garantizar estos invariantes (verificables por tests):

> **I1**: Para cada bloque partible, la unión de `charStart until charEndExclusive` sobre todos sus `*Slice` emitidos (en cualquier página) == `0 until text.length`, sin solapamientos ni huecos.
>
> **I2**: Cada `*Slice` vive en exactamente una página.
>
> **I3**: Si una línea L del bloque no cabe en el espacio restante de la página P, TODAS las líneas a partir de L van a la página P+1 (no se parte una línea por la mitad).
>
> **I4**: Si el bloque entero cabe en `pageHeightPx - cursorYStart`, se emite UN único slice `0 until text.length` y el cursor avanza `cursorYStart + alturaDelBloque`.
>
> **I5**: Si ni una sola línea del bloque cabe en una página vacía (cursorY=0), se emite esa línea forzadamente (progreso garantizado), aunque desborde visualmente.

**Pseudocódigo canónico** (este es el que hay que implementar):

```
fun placePartible(text, sliceFactory, pageWidthPx, pageHeightPx, cursorYStart, density, textMeasurer, style, onSliceEmitted, onPageFilled): Float {
    if text.isEmpty():
        onSliceEmitted(sliceFactory(0, 0))
        return cursorYStart

    layout = textMeasurer.measure(text, style, Constraints(maxWidth=pageWidthPx.toInt()), density)
    total = text.length
    cursorY = cursorYStart
    pageSliceStart = 0   # inicio del slice pendiente para la página actual
    blockOffset = 0      # posición actual dentro del bloque

    while blockOffset < total:
        line = layout.getLineForOffset(blockOffset)
        lineEnd = min(layout.getLineEnd(line, false), total)
        if lineEnd <= blockOffset: break   # safety
        lineH = max(layout.getLineBottom(line) - layout.getLineTop(line), 1f)

        if cursorY + lineH > pageHeightPx AND cursorY > 0f:
            # No cabe y la página tiene contenido: emitir slice pendiente y cerrar.
            if blockOffset > pageSliceStart:
                onSliceEmitted(sliceFactory(pageSliceStart, blockOffset))
            onPageFilled()
            cursorY = 0f
            pageSliceStart = blockOffset
            continue

        # Cabe (o página vacía con línea que no cabe → forzar).
        cursorY += lineH
        blockOffset = lineEnd

    # Slice final pendiente.
    if total > pageSliceStart:
        onSliceEmitted(sliceFactory(pageSliceStart, total))

    return cursorY
}
```

**Notas**:
- `getLineEnd(lineIndex, visibleEnd=false)` devuelve el offset exclusivo del final de la línea. Para preservar el `\n` entre slices consecutivos, usar `visibleEnd=true` NO es lo correcto (include el whitespace). Usar `false` y aceptar que el `\n` pertenece al inicio del siguiente slice. Esto puede causar que un slice empiece con `\n` — aceptable (se renderiza como espacio).
- El `guard` anti-bucle infinito (`if (++guard > 100_000) break`) es OBLIGATORIO por safety.

#### Estilo real del bloque

Añadir función helper privada:

```kotlin
private fun textStyleFor(block: StudyBlock, density: Density): TextStyle {
    val fontSizeSp = when (block) {
        is StudyBlock.Heading -> when (block.level) {
            1 -> DocConfig.HEADING1_SIZE
            2 -> DocConfig.HEADING2_SIZE
            3 -> DocConfig.HEADING3_SIZE
            else -> block.fontSize
        }
        else -> block.fontSize
    }.coerceIn(DocConfig.MIN_FONT_SIZE, DocConfig.MAX_FONT_SIZE)
    val fontFamily = if (block is StudyBlock.Verse || block.fontFamily == "serif") FontFamily.Serif else FontFamily.Default
    val fontWeight = if (block is StudyBlock.Heading) FontWeight.Bold else FontWeight.Normal
    val fontStyle = if (block is StudyBlock.Quote) FontStyle.Italic else FontStyle.Normal
    val lineHeightSp = fontSizeSp * DocConfig.LINE_HEIGHT
    return TextStyle(
        fontSize = with(density) { fontSizeSp.sp },
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        textAlign = block.alignment.toTextAlign(),
        lineHeight = with(density) { lineHeightSp.sp },
    )
}
```

Imports necesarios en `PaginationEngine.kt`:
```kotlin
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.feature.studydocs.model.DocConfig
import com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment
import androidx.compose.ui.text.style.TextAlign
```

Y la función de extensión `BlockAlignment.toTextAlign()` ya existe como `internal` en `UnifiedBlockRenderer.kt:328`. Como está en otro paquete y es `internal`, **duplicarla como `private` en `PaginationEngine.kt`** (es 4 líneas):

```kotlin
private fun BlockAlignment.toTextAlign(): TextAlign = when (this) {
    BlockAlignment.Start -> TextAlign.Start
    BlockAlignment.Center -> TextAlign.Center
    BlockAlignment.End -> TextAlign.End
    BlockAlignment.Justify -> TextAlign.Justify
}
```

####rama `Quote` en el `when` principal

```kotlin
is StudyBlock.Quote -> {
    val style = textStyleFor(block, density)
    cursorY = placePartible(
        text = block.text.raw,
        sliceFactory = { start, end ->
            PageFragment.QuoteSlice(originBlockId = block.id, block = block, charStart = start, charEndExclusive = end)
        },
        pageWidthPx = pageWidthPx,
        pageHeightPx = pageHeightPx,
        cursorYStart = cursorY,
        density = density,
        textMeasurer = textMeasurer,
        style = style,
        onSliceEmitted = { f -> currentFragments.add(f) },
        onPageFilled = { openPage() },
    )
}
```

#### Eliminar
- `defaultTextStyle` (línea 29).
- `paginatePartible` (la versión bugueada, líneas 197-258) → reemplazar por `placePartible`.
- Todos los `Log.d(...)` (líneas 62, 188, 208, 210).
- Línea muerta 187.

### 2.3 `PaginatedSheet.kt` — issues de UX

**Archivo**: `app/src/main/java/com/cristiancogollo/biblion/feature/studydocs/ui/pagination/PaginatedSheet.kt`

Cambios:
1. **Medición consistente con la hoja**: el `PaginationEngine` debe medir en la MISMA densidad en la que se renderiza la hoja. Como la hoja es `LETTER_WIDTH.dp` medido con `LocalDensity`, y el zoom se aplica vía `virtualDensity`, la medición debe hacerse con `density = LocalDensity.current` (sin zoom) — NO con `Density(1f)` fijo. Cambiar `PaginatedSheet.kt:70`:
   ```kotlin
   // ANTES:
   density = Density(1f),
   // DESPUÉS:
   density = density,   // LocalDensity.current sin zoom
   ```
   Y `pageWidthPx`/`pageHeightPx` ya se calculan con `with(density) { LETTER_WIDTH.toPx() }` (líneas 61-62), que es consistente.

2. **Margen interno del Card**: envolver el contenido del `PageCard` en un `Column(Modifier.padding(PageDimensions.PAGE_MARGIN))`. Cambiar `PageCard`:
   ```kotlin
   Card(
       modifier = Modifier
           .width(PageDimensions.LETTER_WIDTH)
           .height(PageDimensions.LETTER_HEIGHT)
           .padding(horizontal = PageDimensions.PAGE_MARGIN, vertical = PageDimensions.PAGE_MARGIN),
       // ... resto igual
   )
   ```
   ⚠️ CUIDADO: el `padding` en el modifier del `Card` aplica al Card mismo (lo empequeñece), NO a su contenido. Para margen INTERNO usar el parámetro `contentWindowInsets` o envolver los fragments en un `Column(Modifier.padding(...))`. Implementación correcta:
   ```kotlin
   Card(...) {
       Column(modifier = Modifier.fillMaxSize().padding(PageDimensions.PAGE_MARGIN)) {
           page.fragments.forEachIndexed { idx, fragment -> contentFragmentRenderer(fragment, idx) }
       }
   }
   ```
   Import `fillMaxSize` ya está. Añadir `padding` ya está vía `androidx.compose.foundation.layout.*`? NO — actualmente solo se importa `padding` explícitamente (línea 10). Confirmar.

3. **`imePadding()`**: aplicar al `Box` externo:
   ```kotlin
   Box(
       modifier = modifier
           .fillMaxSize()
           .imePadding()   // ← NUEVO
           .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
       // ...
   )
   ```
   Import: `androidx.compose.foundation.layout.imePadding`.

4. **Scroll-to-focused**: añadir `LaunchedEffect` que observa el bloque activo y hace scroll. Como `PaginatedSheet` no conoce el bloque activo, pasar un parámetro opcional `activeBlockId: BlockId? = null` y usar `rememberScrollState()` + calcular el offset Y aproximado de la página que contiene el primer fragmento con ese `originBlockId`. Implementación simple (v1): NO hacer scroll automático en v1 (dejar como TODO); el usuario hace scroll manual. Justificación: calcular el offset Y exacto de un fragmento requiere medir los anteriores, que es caro. Postergar a v2.

5. **Eliminar `Log.d` línea 72**.

### 2.4 Imports de `PaginatedSheet.kt`

Tras los cambios, imports necesarios (verificar/ajustar):
```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.model.PageDimensions
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.DocumentZoomState
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.rememberDocumentZoomState
```
Quita: `android.util.Log`, `mutableStateOf`, `setValue`, `LaunchedEffect` (si no se usa scroll-to-focused).

---

## 3. Tests de `PaginationEngine` (OBLIGATORIO crear)

**Archivo NUEVO**: `app/src/test/java/com/cristiancogollo/biblion/feature/studydocs/pagination/PaginationEngineTest.kt`

**Framework**: Robolectric (necesario para `TextMeasurer`). Patrón sacado de `DatabaseSchemaValidationTest.kt:32-34`.

**Setup del `TextMeasurer`** (VERIFICADO en bytecode de Compose UI 1.10.6):
```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PaginationEngineTest {

    private lateinit var textMeasurer: TextMeasurer
    private val density = Density(1f)

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // createFontFamilyResolver vive en androidx.compose.ui.text.font
        val resolver = androidx.compose.ui.text.font.createFontFamilyResolver(context)
        textMeasurer = TextMeasurer(
            defaultFontFamilyResolver = resolver,
            defaultDensity = density,
            defaultLayoutDirection = LayoutDirection.Ltr,
        )
    }
    // ...
}
```

**Imports**:
```kotlin
import android.content.Context
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
```

**Página de test**: alto pequeño (200px) para forzar splitting rápido. `pageWidthPx=658f` (carta 850dp - 2×48dp margen - padding típico). `pageHeightPx=200f`.

**Tests OBLIGATORIOS** (uno por caso):

```kotlin
@Test fun empty_blocks_returns_one_empty_page()
@Test fun single_short_paragraph_returns_one_page_one_slice()
@Test fun long_paragraph_splits_into_multiple_pages_with_contiguous_char_ranges()
@Test fun bullet_list_items_are_split_independently()
@Test fun ordered_list_items_are_split_independently()
@Test fun verse_block_splits_like_paragraph()
@Test fun quote_block_splits_and_attribution_appears_only_in_last_slice()
@Test fun multiple_block_types_fill_pages_sequentially()
```

**Aserciones clave para `long_paragraph_splits_...`** (invariante I1):
```kotlin
val paragraphSlices = pages.flatMap { it.fragments }
    .filterIsInstance<PageFragment.ParagraphSlice>()
assertTrue(paragraphSlices.size >= 2)
var cursor = 0
for (slice in paragraphSlices) {
    assertEquals("Gap en offset $cursor", cursor, slice.charStart)
    cursor = slice.charEndExclusive
}
assertEquals(longText.length, cursor)
```

**Helper**:
```kotlin
private fun shortParagraph(): StudyBlock.Paragraph = StudyBlock.Paragraph(
    id = BlockId("p1"),
    text = StyledText(raw = "Hola mundo"),
    fontSize = DocConfig.DEFAULT_FONT_SIZE,
)
```

`BlockId("p1")` — constructor directo con String (es `value class`, válido).

---

## 4. Plan de commits (7 commits)

Cada commit debe compilar y pasar `./gradlew.bat :app:testDebugUnitTest --tests "com.cristiancogollo.biblion.feature.studydocs.*"`.

### Commit A: `feat(studydocs): PageFragment.QuoteSlice + renderer de fragmento`
- Añadir `QuoteSlice` en `PageFragment.kt` (§2.1).
- Añadir rama en `originBlock` y `renderFragmentReadOnly` en `UnifiedBlockRenderer.kt`.
- Verificar compile.

### Commit B: `fix(studydocs): PaginationEngine respeta estilo del bloque y corrige paginacion multi-pagina`
- Reescribir `PaginationEngine.kt` completo según §2.2:
  - `placePartible` con pseudocódigo canónico (emite slices intermedios).
  - `textStyleFor(block, density)`.
  - Rama `Quote` en el `when`.
  - Eliminar `defaultTextStyle`, `paginatePartible`, `Log.d`, línea muerta.
- Verificar compile.

### Commit C: `feat(studydocs): PaginatedSheet con margen interno, imePadding y medicion consistente`
- Aplicar cambios de §2.3 a `PaginatedSheet.kt`:
  - `density = density` (no `Density(1f)`).
  - `Column(Modifier.padding(PAGE_MARGIN))` dentro del Card.
  - `.imePadding()` en el Box externo.
  - Eliminar `Log.d`.

### Commit D: `test(studydocs): PaginationEngineTest con Robolectric`
- Crear `app/src/test/java/com/cristiancogollo/biblion/feature/studydocs/pagination/PaginationEngineTest.kt` según §3.
- Los 8 tests deben pasar.
- Si `createFontFamilyResolver(context)` no resuelve (Robolectric puede no tener fuentes), los tests de altura serán aproximados pero los invariantes I1-I4 deben cumplirse.

### Commit E (opcional): `chore(studydocs): limpieza de Log.d restantes en StudyModeEditorPanel y StudyDocEditorScreen`
- Si quedan `Log.d("BIBLION_STUDY", ...)` en producción tras los commits anteriores, eliminarlos. ( Hoy hay ~8 repartidos en `StudyDocEditorScreen.kt`, `StudyModeEditorPanel.kt`, `StudyDocReadScreen.kt`, `UnifiedBlockRenderer.kt`.)

---

## 5. Verificación final

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio1\br"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd "C:\Users\LENOVO LOQ\Desktop\PROYECTOS\Biblion"
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:testDebugUnitTest --tests "com.cristiancogollo.biblion.feature.studydocs.*"
```

Ambos deben terminar con `BUILD SUCCESSFUL`. Reportar conteo de tests (esperado: 6 de DocumentZoomState + 7 de StyledTextSlice + 8 de PaginationEngine + los de StudyDocsListViewModel + los de StyledTextRange = ~25 tests, de los cuales 3 de StyledTextRange FALLAN por bug preexistente — reportar pero no bloquear).

---

## 6. Fuera de alcance (NO tocar)

- `StudyDoc`, `StudyBlock` (modelo), `StudyOp`, `StudyDocEngine`, `StudyDocValidator`, `StudyDocNormalizer`.
- Persistencia Room (`StudyDocEntity`, `StudyDocDao`, `StudyDocRepository`, `StudyDocJson`).
- `StudyDocViewModel` / `StudyDocSplitViewModel` (solo se LEEN).
- Rutas de navegación.
- Otros features (Bibi, diccionario, search, reader bíblico, tutorial, profile).
- Bug preexistente en `StyledTextRangeTest.withText` (3 tests) — reportar, no arreglar.

---

## 7. Notas operativas para el agente

- **JAVA_HOME**: el Android Studio en `C:\Program Files\Android\Android Studio\jbr` está INCOMPLETO (sin `lib/jvm.cfg`). Usar `C:\Program Files\Android\Android Studio1\jbr` (JDK 21 completo). Verificado.
- **Gradle daemon**: la primera invocación tarda ~1-2 min (arranca daemon). Subsecuentes ~30s.
- **Git**: NO hacer commit/push salvo instrucción explícita del usuario. El usuario pidió "termina los cambios", no "commitea".
- **Plan mode**: si dudas sobre arquitectura, usa `EnterPlanMode`. Para ejecución directa de este plan, procede sin re-entrar a plan mode.
- **Si un test de `PaginationEngine` falla por fuentes**: Robolectric puede no tener la font family default. En ese caso, los tests de "altura exacta" son frágiles; enfocar los tests en los **invariantes I1-I4** (contiguidad de rangos, no solapamientos), que no dependen de la fuente exacta.
- **`createFontFamilyResolver`**: si el import `androidx.compose.ui.text.font.createFontFamilyResolver` no resuelve, alternativa es `androidx.compose.ui.text.font.emptyCacheFontFamilyResolver(context)` (mismo paquete). Ambas verificadas en bytecode 1.10.6.

---

## 8. Cómo reportar progreso

Al terminar, reportar:
1. Commits creados (hash + mensaje) o archivos modificados si no se commiteó.
2. Salida de `compileDebugKotlin` (BUILD SUCCESSFUL/FAILED).
3. Salida de `testDebugUnitTest --tests feature.studydocs.*`: X tests, Y passed, Z failed, Z' ignored.
4. Lista de los 3 tests preexistentes que fallan en `StyledTextRangeTest` (bug ajeno).
5. Cualquier desviación de este plan y por qué.
