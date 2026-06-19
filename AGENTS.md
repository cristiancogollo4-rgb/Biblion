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
- Modo estudio con editor de ensenanzas.
- Listado de "Mis ensenanzas" con abrir, editar, eliminar, filtrar por titulo o etiqueta.
- Lectura de ensenanzas con controles de tamano de letra, modo claro/oscuro y pantalla dividida en pantallas grandes.
- Sistema de etiquetas sugeridas por seccion: proposito, audiencia, tema y estado.
- Validacion de guardado de ensenanzas: titulo obligatorio y etiquetas requeridas por seccion.
- **Diccionario biblico local unificado** (`dictionary.db`) con 6,346 entradas Easton's + Theographic: definiciones, metadata (género, fechas, coordenadas GPS, aliases, featureType). Accesible vía `DictionaryEngine` con templates por categoría.
- **Bibi mejorada** con respuestas estructuradas (`BibiResponse`), templates por categoría (persona/lugar/concepto/objeto/práctica/evento), sugerencias personalizadas con `chatHistory` ("Comparar con X"), anáforas, memoria conversacional e historial de chats persistido en Room.
- **Placeholders rotativos (carousel)** en campos de búsqueda cada 3.5s con 5 ejemplos.
- Integracion online de Bibi mediante Cloudflare Worker y Qwen3-8B por endpoint compatible con OpenAI.
- **Local primero, Worker solo para DIVE_DEEPER** — la búsqueda en Biblia es local, Bibi usa IA solo cuando profundiza.
- Evaluador local de modelos de Bibi en `workers/bibi/evals/model_eval.mjs`.
- Contexto para Bibi con versiones biblicas disponibles, version seleccionada, texto seleccionado, bloques actuales de la ensenanza, notas y diccionario biblico.
- Saludo local de Bibi con el nombre visible del usuario autenticado sin incluir ese nombre en la solicitud al Worker.
- **Tutorial guiado interactivo (primera instalacion)**: inicio automatico en Home, Bibi con logo, auto-scroll a versiculo 1, target ampliado (primeros 3 versiculos), scroll en textos largos, debug logs `GUIDE_DEBUG`.
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
2. No romper navegacion ni flujos existentes: `Home`, `Books`, `Reader`, `Search`, `Study`, `Mis ensenanzas`.
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

El modo estudio se compone principalmente de `StudyEditorScreen`, `StudyViewModel`, `StudyDocumentEngine`, `StudyData`, `ReaderScreen`, `StudyReadScreen` y `EnsenanzaScreen`.

### Arquitectura del documento de estudio

- `StudyBlockNode` es el modelo estructurado de la ensenanza.
- `StudyDocumentEngine` es la fuente de verdad para transformaciones puras del documento.
- `StudyViewModel` orquesta estado, autosave, persistencia, citas y sincronizacion; no debe duplicar reglas internas de mutacion de bloques.
- `StudyEditorScreen` debe enfocarse en renderizar UI, seleccion, herramientas flotantes y eventos de usuario.
- Nuevas herramientas que modifiquen texto, estilos, columnas, notas, reflexiones, citas o bloques interactivos deben agregarse primero como operaciones testeables en `StudyDocumentEngine`.
- El editor visual usa `LazyColumn` con claves estables por bloque para mejorar rendimiento en ensenanzas largas; no volver a un `Column` con `verticalScroll` para el lienzo completo salvo que exista una razon validada.
- Las pruebas de transformaciones del documento deben vivir en `StudyDocumentEngineTest` o un test equivalente de dominio.

### Herramientas del editor

- **Texto libre / parrafos**: bloque principal para escribir la ensenanza. Enter separa parrafos; en encabezados crea un nuevo parrafo; en listas crea un nuevo item del mismo tipo.
- **Encabezado**: cambia el rol visual del parrafo a titulo/seccion. Enter en encabezado crea un nuevo parrafo debajo.
- **Lista con vinetas**: transforma parrafos en items de lista. Enter crea nuevo item con vineta.
- **Lista numerada**: transforma parrafos en items numerados. Enter crea nuevo item numerado.
- **Columnas**: no es un bloque independiente; usa `StudyBlockNode.Paragraph` con `role = "columns"` y `parallelText`.
- **Citar**: inserta citas biblicas como `StudyBlockNode.QuotedVerse` con soporte de comparacion de versiones.
- **Nota**: inserta un bloque de nota para observaciones, aclaraciones o recordatorios.
- **Reflexion**: inserta un bloque de reflexion vinculado a una idea o texto seleccionado.
- **Estilos de texto**: color, fondo (resaltado), negrita, cursiva, subrayado y tamano para rangos seleccionados. Colores en burbuja flotante sobre barra de herramientas.
- **Aumentar/disminuir fuente de seleccion**: aplica tamano al texto seleccionado.
- **Modo enfoque**: oculta el panel del lector para concentrarse en el editor.
- **Guardar con metadata**: exige titulo y etiquetas validas.
- **Bibi**: asistente flotante para hacer preguntas biblicas, pedir ideas, pasajes relacionados, bosquejos, aplicaciones, notas o reflexiones. En modo estudio puede insertar respuestas como Nota o Reflexion.

Herramientas eliminadas (no aportaban valor al estudio biblico):
- Alineacion de texto (izquierda, centro, derecha).
- Transformacion de mayusculas/minusculas.

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

### Busqueda de versiculos (SearchScreen)

`SearchScreen` permite buscar versiculos en la Biblia local con filtros y busqueda en vivo.

#### Arquitectura

- **Biblia local**: `BibleRepository.searchVerses()` consulta `bible_verses` con SQL `LIKE '%query%' COLLATE NOCASE`.
- **Filtros**: `BibleSearchFilter(testament, bookName)` se pasa a `searchVerses()` para restringir por testamento y/o libro.
- **Persistencia de busquedas**: `SearchHistoryRepository` guarda cada query en Room (`search_history.db`) con normalizacion (sin acentos) y conteo de uso.

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

**`collectLatest` cancela la corutina anterior** cuando llega un nuevo valor, eliminando race conditions.

#### Componentes

- **`SearchInputCard`**: campo de busqueda con placeholder rotativo (carousel cada 3.5s: `Juan 3:16`, `amor`, `Salmo 23`, `perdon`, `fe`).
- **`TestamentTabsRow`**: tabs `Toda la Biblia` / `Antiguo` / `Nuevo` que filtran la busqueda.
- **`BookFilterRow`**: dropdown con los 66 libros (filtrados por testamento). Boton X para limpiar filtros.
- **`RecentSearchesSection`**: ultimas 5 busquedas (top 10 con "Ver todo").
- **`PopularVersesSection`**: 6 versiculos populares curados (Amor, Fe, Gracia, Salvacion, Esperanza, Paz). Se ocultan cuando el usuario empieza a escribir.
- **`ResultsHeader`**: muestra "Resultados para X · testamento · libro" cuando hay filtros activos.
- **`NoResultsState`**: estado contextual con icono y boton "Limpiar".

#### Re-busqueda automatica

- **Cambio de filtro** (testamento/libro) → re-busca inmediatamente con `runSearchImmediate()`.
- **Enter** → busqueda inmediata sin esperar debounce.
- **Typing** → espera 400ms (debounce) antes de buscar.
- **Boton X (clear)** → `uiState = Idle`, query vacio.

#### Versiculos populares

6 versiculos curados en `PopularVersesData`:
- Amor: 1 Corintios 13:4-7
- Fe: Hebreos 11:1
- Gracia: Efesios 2:8-9
- Salvacion: Romanos 10:9
- Esperanza: Romanos 15:13
- Paz: Juan 14:27

Tap en cualquier versiculo → abre el lector en la referencia exacta.

### Lectura de ensenanzas

`StudyReadScreen` debe mostrar el documento estructurado, no solo texto plano.

Funciones actuales:

- Render de parrafos, encabezados, listas, columnas, notas, reflexiones y citas.
- Cambio de version y comparacion en bloques de cita.
- Modo claro/oscuro desde la lectura.
- Aumentar/disminuir tamano de letra de lectura.
- Lectura en pantalla dividida solo en pantallas grandes (>=840dp).
- Lectura vertical en moviles.
- Filtro y administracion desde "Mis ensenanzas".
- Bibi en lector normal para preguntas biblicas basicas sobre el pasaje actual.
- Deduplicacion de bloques de cita: si un mismo pasaje existe como `Citation` y `QuotedVerse`, se muestra solo el `QuotedVerse` (con soporte de comparacion).

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

- Nuevas acciones de usuario deben agregarse de forma consistente en `StudyIntent`.
- El estado visible debe modelarse en `StudyUiState`.
- Evitar efectos secundarios ocultos; preferir flujos explicitos con corrutinas/Flow.
- Si se agrega logica testeable, preferir funciones puras o helpers internos con pruebas unitarias.
- Para tests, se permite inyectar dispatchers o desactivar semillas demo cuando mejore determinismo.

## 9) Datos, repositorios y Room

- Cambios de esquema Room deben ser compatibles y justificados.
- No romper datos existentes sin migracion.
- Mantener separacion entre entidades de persistencia y estado UI.
- Si se agrega cache o acceso a assets biblicos, seguir el patron de `BibleRepository` y caches dedicados.
- La Biblia se consulta desde la base SQLite preempaquetada `app/src/main/assets/databases/bible_content.db`.
- La base se genera desde los JSON fuente con `tools/build_bible_sqlite.py`; si se regeneran versiones, conservar la deduplicacion de libros por nombre normalizado para evitar duplicados como los de NVI.
- Las citas vinculadas deben conservar `book`, `chapter`, `verseStart`, `verseEnd` y `version`.
- **Diccionario biblico local unificado**: `app/src/main/assets/databases/dictionary.db` contiene 6,346 entradas (Easton's + Theographic). Generado por `tools/build_knowledge_sqlite.py`. Esquema version 2 (con metadata Theographic).
- **Historial de busquedas**: Room database `search_history.db` con `SearchHistoryEntity` (query, normalized_query, use_count, last_used_at). Se usa para mostrar busquedas recientes en `SearchScreen`. Migracion destructiva aceptable (datos regenerables).
- **Chats de Bibi**: Room database `bibi_chat.db` con `ChatSessionEntity` y `ChatMessageEntity`. Permite multiples sesiones independientes de conversacion.

## 10) Navegacion

- Registrar rutas nuevas en `NavigationRoutes`, `AppNavigation` o `NavGraphShared` segun corresponda.
- Evitar duplicidad de rutas.
- Respetar `launchSingleTop` y `popUpTo` usados en la app.
- Las pantallas compartidas deben recibir dependencias como tema global mediante parametros, no accediendo a estado global oculto.

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
