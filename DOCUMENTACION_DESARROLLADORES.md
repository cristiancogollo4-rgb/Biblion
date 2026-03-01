# Documentación técnica de Biblion

## 1) Objetivo de la aplicación
Biblion es una app de lectura bíblica con modo de estudio integrado.

- **Panel izquierdo**: lectura de libros/capítulos/versículos.
- **Panel derecho (modo estudio)**: editor enriquecido para preparar enseñanzas.
- **Navegación principal**: Home → Books → Reader/Search.

---

## 2) Arquitectura general (alto nivel)

### Entrada
- `MainActivity.kt`
  - Arranca Compose.
  - Aplica `BiblionTheme`.
  - Carga `AppNavigation` como raíz.

### Navegación
- `AppNavigation.kt`
  - Define rutas Compose:
    - `home`
    - `books/{testament}`
    - `reader/{bookName}?studyMode={studyMode}`
    - `study/{bookName}`
    - `search`
  - Decodifica parámetros URL para evitar errores con caracteres especiales.

### Pantallas
- `HomeScreen.kt`
  - Menú lateral.
  - Selector de testamento.
  - Versículo del día.
- `BooksScreen.kt`
  - Lista de libros por testamento.
  - Drawer reutilizable con accesos rápidos.
- `ReaderScreen.kt`
  - Lectura de capítulo/versículos.
  - Selección múltiple de versículos.
  - Menú contextual flotante (copiar/citar/subrayar/limpiar).
  - Integración con modo estudio en split.
- `SearchScreen.kt`
  - Búsqueda textual en todo `rvr1960.json`.
- `StudyEditorScreen.kt`
  - Editor enriquecido para notas.
  - Inserción de citas bíblicas como nodos estructurados.
  - Menú contextual flotante al seleccionar texto.
  - Popup contextual anclado al tocar una cita.

### Componentes reutilizables
- `BiblionComponents.kt`
  - AppBars, cards, selector de testamento, diálogos.
  - `VerseActionsFloatingMenu` (burbuja contextual de acciones).

### Estado / lógica de estudio
- `StudyViewModel.kt`
  - Estado del cuaderno (título, referencia base, contenido).
  - Persistencia estructurada de documento rico en JSON.
  - Cola de inserciones de citas desde el lector.

### Tema
- `ui/theme/Color.kt`, `Theme.kt`, `Type.kt`
  - Paleta, tipografías y tema Material.

---

## 3) Flujo funcional principal

## 3.1 Home
1. Carga versículo diario (`getDailyVerse`).
2. Usuario selecciona testamento.
3. Navega a Books con parámetro codificado.

## 3.2 Books
1. Recibe testamento.
2. Renderiza lista de libros.
3. Al tocar libro, navega a Reader.

## 3.3 Reader
1. Lee JSON de Biblia local por libro/capítulo.
2. Renderiza versículos.
3. Long press inicia selección.
4. Tap agrega/quita versículos mientras hay selección activa.
5. Burbuja contextual flotante permite:
   - Copiar versículos
   - Citar en cuaderno (si aplica)
   - Subrayar con colores
   - Limpiar selección

## 3.4 Study Editor
1. Recibe inserciones de citas pendientes del ViewModel.
2. Inserta cita inline como `CitationSpan`.
3. Al seleccionar texto aparece menú flotante con formato:
   - Pintar fondo
   - Negrita
   - Cursiva
   - A+/A-
   - Título
   - Limpiar formato
4. Al tocar una cita aparece popup anclado con:
   - Referencia
   - Texto
   - Icono cambiar versión (placeholder)
   - Eliminar cita

---

## 4) Documento enriquecido (persistencia)

`StudyViewModel` serializa el documento como JSON:

- `type: study_document`
- `version`
- `text`
- `spans[]`

Cada span se guarda con:
- `type` (bold, italic, highlight, size_relative, size_abs, bible_reference)
- `start`, `end`
- metadatos opcionales (`citationId`, `citationReference`, etc.)

Esto permite:
- Restaurar formato real.
- Mantener citas como nodos estructurados.
- Escalar hacia exportación futura (PDF/Johnson/publicación).

---

## 5) Parámetros clave y por qué existen

## Reader
- `bookName`:
  - identifica qué libro cargar desde JSON.
- `isStudyModeActive`:
  - habilita acciones de “citar en cuaderno”.
- `viewModel`:
  - canal para enviar citas al panel de estudio.

## StudyEditor
- `viewModel`:
  - fuente de verdad del documento rico y cola de citas.
- `onClose`:
  - cierra panel derecho sin romper navegación global.

## VerseActionsFloatingMenu
- `selectedCount`: feedback de selección actual.
- `anchorOffset`: posicionamiento contextual (no fijo).
- `onDismiss`: cerrar al tocar fuera.
- `onHighlight` / `onCopy` / `onAddCitation` / `onClearSelection`: acciones explícitas desacopladas de UI.

---

## 6) Archivo de datos bíblicos

- `app/src/main/assets/rvr1960.json`
  - Biblia base usada por Home (versículo diario), Reader y Search.

---

## 7) Convenciones de mantenimiento

1. Mantener estilos dentro del sistema visual actual (tema/components).
2. Preferir componentes reutilizables en `BiblionComponents.kt`.
3. En modo estudio, no degradar citas a texto plano.
4. Cualquier formato nuevo debe entrar al esquema de `RichSpanRecord`.
5. Mantener compatibilidad con serialización/deserialización.

---

## 8) Resumen por archivo (rápido)

- `MainActivity.kt`: bootstrap Android + Compose.
- `AppNavigation.kt`: rutas y argumentos.
- `HomeScreen.kt`: inicio, menú, versículo diario.
- `BooksScreen.kt`: catálogo de libros + drawer.
- `ReaderScreen.kt`: lectura y selección contextual de versículos.
- `StudyEditorScreen.kt`: editor enriquecido y citas inline.
- `StudyViewModel.kt`: estado y persistencia estructurada.
- `BiblionComponents.kt`: UI reutilizable compartida.
- `ui/theme/*`: tema visual.

---

## 9) Próximos pasos recomendados

1. Persistir `noteDocumentJson` en almacenamiento local permanente (DataStore/Room).
2. Añadir pruebas de serialización/deserialización de spans.
3. Añadir pruebas de interacción de selección contextual (reader/editor).
4. Implementar selector real de versiones bíblicas para citas.

