# Biblion

**Biblion** es una aplicacion movil Android construida con **Kotlin + Jetpack Compose + Material 3**. Su objetivo es ofrecer una experiencia clara para lectura biblica, busqueda de versiculos y preparacion de ensenanzas desde un solo lugar.

El proyecto esta en desarrollo activo. La experiencia visual, los flujos de estudio y la sincronizacion siguen evolucionando.

---

## Estado actual (v0.9.0 - Jun 2026)

Biblion ya cuenta con:

- Lectura biblica por testamento, libro y capitulo.
- Consulta biblica local desde una base SQLite/Room preempaquetada.
- **Busqueda de versiculos en vivo** con filtros por testamento y libro.
- **Búsquedas recientes** persistidas en Room (top 5 visibles, top 10 con "Ver todo").
- **Versículos populares curados** como punto de partida (Amor, Fe, Gracia, Salvación, Esperanza, Paz).
- **Placeholder rotativo** en campos de busqueda (carousel cada 3.5s).
- **Temas canonicos v3**: 693 temas en 11 categorias con 424 que tienen versiculos. Busqueda por pipeline de 6 fases (slug exacto, nombre exacto, palabra completa, alias, prefijo de alias con score >= 0.92, prefijo de nombre).
- **Carrusel de "Temas populares"** con rotacion aleatoria entre los 424 temas y regla de unicidad estricta entre cards (nunca se repite el mismo tema en 2+ cards).
- **Pantalla "Explorar temas"** con 10 categorias (sin BOOK) y buscador en vivo por categoria. Cada tema es una card expandible con versiculos.
- Selector de version biblica.
- Resaltado de versiculos con persistencia y sincronizacion.
- Preferencias de lectura, incluyendo tamano de fuente.
- Modo claro/oscuro global (tema claro por defecto en primera instalacion).
- Autenticacion con Firebase Auth, incluyendo inicio de sesion con Google.
- Sincronizacion con Firestore para preferencias, resaltados, estudios y citas.
- Perfil de usuario con datos reales, alias, biografia, foto o avatar de color.
- Perfil con panel principal de identidad, metricas y edicion agrupada en un solo boton.
- **Reiniciar tutorial de lectura** desde el perfil.
- Base inicial para la red de Biblion sobre Firebase/Firestore.
- Modo estudio con editor estructurado y barra de herramientas optimizada.
- Gestion de "Mis ensenanzas" con filtros por titulo o etiquetas.
- Lectura enriquecida de ensenanzas con soporte de comparacion de versiones biblicas.
- Sistema de etiquetas sugeridas y validacion de metadata.
- **Diccionario biblico local unificado** (6,346 entradas Easton's + Theographic) con metadata (género, fechas, coordenadas GPS, aliases). Pantalla dedicada `DictionaryScreen` con 8 categorias. `DictionaryCategoryScreen` con busqueda en vivo. Libros en orden canonico biblical.
- **Descripcion de libros en BooksScreen**: long press en un libro muestra bottom sheet con descripcion (desde `topics.db`), conteo de versiculos y boton "Leer".
- **Bibi mejorada** con respuestas estructuradas, templates por categoría, anáforas, memoria conversacional e historial de chats persistido.
- **Bibi local primero**: la búsqueda en Biblia es local, Bibi usa IA solo cuando profundiza.
- **Tutorial guiado interactivo** con Bibi (auto-inicio en primera instalacion, logo de Bibi, scroll en textos largos).

---

## Caracteristicas principales

### Lectura biblica

- Navegacion por Antiguo y Nuevo Testamento.
- Deslizamiento entre Antiguo y Nuevo Testamento desde la pantalla de libros.
- Lectura por libro/capitulo.
- Cambio de version biblica desde el lector.
- Deslizamiento entre capitulos con animacion y actualizacion del indicador superior.
- Busqueda de versiculos y navegacion directa al resultado.
- Seleccion multiple de versiculos.
- Resaltado por color.
- Insercion de citas al modo estudio.
- **Tutorial guiado**: Bibi acompaña en primera instalacion con auto-scroll a versiculo 1, target ampliado (primeros 3 versiculos), y burbuja con logo de Bibi.

### Citas por rango

Cuando se seleccionan versiculos contiguos, Biblion los agrupa como una sola cita:

```text
Genesis 1:1-3
1 En el principio... 2 Y la tierra... 3 Y dijo Dios...
```

Esto evita crear tres componentes separados para una sola referencia.

### Temas canonicos y busqueda por tema

Ademas de buscar versiculos, Biblion incluye un sistema de **temas canonicos** con 11 categorias, alimentado por el dataset de openbile.info (CC-BY).

- **693 temas canonicos** curados: BOOK (66), PERSON (126), PLACE (70), EVENT (43), ATTRIBUTE_OF_GOD (32), PROPHECY (33), COMPARATIVE_RELIGION (17), SIN (70), DOCTRINE (101), CHURCH (50), CHRISTIAN_LIFE (85).
- **424 temas con versiculos disponibles** (61% del canon). Los 269 restantes quedan ocultos en la UI.
- **1689 aliases** de OpenBible mapeados a canonicos con score coseno 0.85-0.94.
- **4704 referencias** a versiculos del OSIS original con quality score 2-100.
- **236 relaciones jerarquicas** parent/child.

**Pipeline de busqueda de 6 fases** (sin scoring, sin parametros magicos):

1. Slug exacto — ej. "fe" -> topic "fe"
2. Nombre exacto ES o EN
3. Palabra completa en nombre — ej. "fe" -> "Falta de fe", "Bautismo de profesion de fe"
4. Alias exacto o palabra completa — ej. "fear" -> "temor-de-dios"
5. Prefijo en alias con score >= 0.92 — ej. "fe" -> "fear of the lord"
6. Prefijo de palabra en nombre — ej. "feli" -> Felipe, Felipe

Las fases se acumulan hasta alcanzar el `maxTotal` de resultados. Solo se devuelven temas con `verseCount > 0`.

**Carrusel de "Temas populares"** con rotacion aleatoria:

- 4 cards horizontales de 160dp.
- Pool de 500 temas con versiculos (los 424 canonicos), barajados en cada carga.
- Regla de unicidad estricta: nunca se repite el mismo tema en 2+ cards al mismo tiempo.
- Rotacion cada 5 segundos, con offset escalonado de 1.1 segundos entre cards.
- Tap en una card -> pre-llena la busqueda del tema en SearchScreen.

**Pantalla "Explorar temas"** con jerarquia:

- `ExploreTopicsScreen`: 10 cards de categorias (sin BOOK) con color, conteo de temas e icono de flecha.
- `ExploreCategoryScreen`: temas de una categoria con buscador en vivo, cada tema como card expandible con versiculos.
- Tap en un versiculo -> abre el lector en la referencia exacta.

**Colores por categoria**: PERSON (marron), PLACE (verde), EVENT (naranja), ATTRIBUTE_OF_GOD (rojo claro), DOCTRINE (azul), CHRISTIAN_LIFE (verde claro), CHURCH (morado), PROPHECY (rojo oscuro), SIN (rojo), COMPARATIVE_RELIGION (gris azulado).

### Mis ensenanzas

La seccion **Mis ensenanzas** permite:

- Ver ensenanzas guardadas.
- Abrir una ensenanza en modo lectura.
- Editar una ensenanza en modo estudio.
- Editar titulo y etiquetas.
- Eliminar ensenanzas.
- Filtrar por titulo o etiquetas.

---

## Perfil y red de Biblion

Biblion ya incluye una seccion **Perfil** para usuarios autenticados. Esta seccion prepara la identidad que se usara en la red de Biblion.

La vista principal prioriza identidad, presencia en la red y metricas. Los datos personales, foto y color de avatar se editan desde **Actualizar perfil**, mientras que **Actualizar plan** queda preparado como accion futura.

### Datos del perfil

El perfil solicita y guarda en Firestore:

- nombres;
- apellidos;
- alias visible;
- correo;
- biografia;
- rol;
- estado de publicador;
- plan;
- fecha de registro.

Si el usuario inicia sesion con Google y faltan nombres, apellidos o alias, Biblion muestra un dialogo para completar el perfil.

### Avatar visible en la red

El usuario puede elegir como verse en la red:

- subir una foto de perfil;
- usar un avatar circular con color personalizable.

La foto se sube a Firebase Storage en:

```text
profile_photos/{uid}/avatar.jpg
```

La URL se guarda en Firestore como `fotoPerfil` y `foto_perfil`.

Si el usuario no usa foto, el avatar se representa con `avatarColor` y `avatar_color`. Esto permite que la red muestre una identidad visual consistente aunque no exista imagen.

Si la subida de foto falla, el caso mas comun es una regla o bucket de Firebase Storage que no permite escribir en `profile_photos/{uid}/avatar.jpg`. La app mantiene el avatar por color como respaldo y muestra el mensaje de error devuelto por Firebase.

### Metricas del perfil

El perfil muestra metricas iniciales segun el rol o estado del usuario.

Para lectores:

- ensenanzas creadas;
- ensenanzas descargadas;
- guardados;
- comentarios.

Para publicadores, administradores o usuarios con `estadoPublicador = APROBADO`:

- ensenanzas creadas;
- ensenanzas publicadas;
- likes recibidos;
- descargas recibidas.

Estas metricas se guardan como contadores en `users/{uid}` y arrancan en `0` hasta que se conecten los flujos sociales de la red.

---

## Bibi, asistente biblica

**Bibi** es la asistente biblica oficial de Biblion. Esta especializada exclusivamente en contenido biblico y cristiano; no actua como asistente general.

### Donde aparece

- **Modo lector**: ayuda con preguntas breves sobre el pasaje actual, contexto inmediato, palabras, referencias y aplicaciones sencillas.
- **Modo estudio**: ayuda a preparar ensenanzas, predicaciones, devocionales, clases y materiales de discipulado. Puede sugerir bosquejos, ideas principales, aplicaciones, notas y reflexiones.

### Arquitectura local de Bibi

Bibi ahora es un sistema de tres motores locales que funcionan **offline**:

- **KnowledgeEngine** (orquestador): detecta la intencion del usuario (WHO, WHERE, DEFINE, EXPLAIN_VERSE, RELATED, ORIGINAL_LANG, GREETING, DIVE_DEEPER, FALLBACK) y enruta al motor apropiado.
- **DictionaryEngine**: convierte entradas del diccionario biblico en respuestas naturales con templates por categoría (persona, lugar, concepto, objeto, práctica, evento, general). Cada categoría tiene su propio template con metadata relevante (género, fechas, coordenadas GPS, aliases).
- **AmbiguousTermResolver**: cuando un término no se encuentra, busca alternativas similares y ofrece opciones al usuario.

### Memoria conversacional

Bibi mantiene un historial de chat (`chatHistory`) que le permite:

- **Anáforas**: cuando el usuario pregunta algo corto sin sujeto claro (ej. "¿y qué más?"), Bibi busca el último término resuelto en el historial y lo usa como sujeto.
- **Sugerencias personalizadas**: si el usuario preguntó por Abraham antes, las sugerencias para una pregunta sobre Sara pueden incluir "Comparar con Abraham".
- **Historial de chats persistido**: las sesiones de chat se guardan en Room (`bibi_chat.db`), permitiendo múltiples conversaciones independientes.

### Dominio de Bibi

Bibi responde sobre:

- Biblia, libros, capitulos y versiculos;
- estudio biblico y contexto;
- personajes, lugares e historia biblica relacionada con las Escrituras;
- doctrina cristiana, discipulado, devocionales y predicacion;
- preparacion de ensenanzas;
- significado de palabras biblicas;
- referencias cruzadas y comparacion de pasajes.

Si el usuario pregunta algo fuera del contexto biblico o cristiano, Bibi redirige amablemente hacia el estudio de las Escrituras.

### Estrategia "local primero"

Bibi sigue un patrón **local-first**:

1. Android llama primero al `LocalStudyAssistantRepository` que ejecuta el `KnowledgeEngine` local
2. Si la respuesta local tiene confianza ALTA o MEDIA, se devuelve sin tocar la red
3. Si la respuesta local tiene confianza BAJA o es null, se envía al Worker
4. Esto reduce latencia y dependencia de red

### Filtro de contexto al Worker

Para evitar que el Worker use el capítulo actual como contexto de respuestas independientes:

- **WHO/WHERE/DEFINE/ORIGINAL_LANG** → NO se envía `studyTitle` ni `selectedText` al Worker
- **EXPLAIN_VERSE/RELATED/FALLBACK/DIVE_DEEPER** → SÍ se envía contexto completo

Esto se implementa en `StudyAssistantPanel.sendQuestion()` mediante `needsChapterContext = intent in setOf(...)`.

### Sugerencias (chips)

Cada respuesta de Bibi puede incluir hasta 2 sugerencias como chips clickables:

- **"Pedir a IA: pasajes sobre X"** (isAi=true) → query "profundiza qué pasajes hablan de X" → DIVE_DEEPER → Worker
- **"Comparar con Y"** (isAi=true) → si hay otro término en el historial → query "¿qué diferencia hay entre X y Y?" → DIVE_DEEPER → Worker
- **"Profundizar con IA"** (isAi=true) → query "profundiza sobre X" → DIVE_DEEPER → Worker

Solo se incluyen sugerencias que Bibi SÍ puede contestar (no se sugieren acciones sin lógica implementada).

### Diccionario biblico local

El diccionario biblico local unificado (`dictionary_v2.db`) consolida 6,346 entradas de dos fuentes:

- **Easton's Bible Dictionary**: 3,932 entradas con definiciones en espanol
- **Theographic Data**: 3,067 personas + 1,274 lugares con metadata enriquecida

El diccionario se accede desde `DictionaryScreen` con 8 categorias (Personas, Lugares, Conceptos, Objetos, Prácticas, Eventos, Libros, General). La categoria **BOOK** incluye los 66 libros en orden canonico biblical con descripciones desde `topics.db`.

El diccionario se usa como base para las respuestas de Bibi. Las respuestas locales **NO incluyen versiculos completos** (solo metadatos) para evitar que Bibi invente referencias. Para profundizar en versículos, se sugiere via el chip "Pedir a IA: pasajes sobre X".

### Descripcion de libros desde Topics

En `BooksScreen`, un **long press** en cualquier libro abre un `ModalBottomSheet` con:

- Nombre del libro con chip de categoria
- Descripcion del libro (desde `topics.db` via `TopicEngine.getBySlug()`)
- Conteo de versiculos disponibles
- Boton "Leer [Libro]" que navega al lector

Los slugs de `topics.db` NO tienen acentos ni guiones intermedios. La funcion `toBookTopicSlug()` convierte el nombre del libro (ej. "1 Corintios") a slug (ej. "1-corintios") quitando tildes y reemplazando espacios por guiones.

### Contexto que recibe el Worker

Bibi online no recibe toda la Biblia completa. Biblion le envia contexto relevante:

- modo actual: `reader` o `study`;
- intencion inferida por el cliente Android (no por el Worker);
- version biblica seleccionada;
- versiones biblicas disponibles en Biblion;
- texto seleccionado por el usuario (solo si la intención lo requiere);
- titulo, etiquetas, bloques actuales y notas de la ensenanza;
- historial conversacional (preguntas y respuestas previas);
- entradas relevantes del diccionario biblico local.

El saludo inicial puede usar el nombre visible del usuario de forma local en la UI. Ese nombre **no** se agrega al payload del Worker.

### Versiones biblicas

Bibi conoce las versiones disponibles de Biblion porque Android se las envia desde `BibleRepository.getAvailableVersions`.

Versiones actuales:

- Reina Valera 1960 (`rv1960`)
- Nueva Version Internacional (`nvi`)
- Dios Habla Hoy (`dhh`)
- Traduccion en Lenguaje Actual (`tla`)
- Nueva Traduccion Viviente (`ntv`)

Si se agregan nuevas versiones como assets, Bibi puede recibirlas automaticamente.

### Respuesta estructurada

Internamente Bibi responde con:

```json
{
  "answer": "",
  "references": [],
  "suggestedBlocks": [],
  "confidence": "high"
}
```

La app muestra solamente `answer` en el chat. Android y el Worker limpian defensivamente respuestas que incluyan JSON visible o malformado.

### Infraestructura online

Bibi online usa:

- Android: `HttpStudyAssistantRepository`.
- Endpoint configurable: `bibiEndpointUrl` en `local.properties`.
- Cloudflare Worker: `workers/bibi`.
- Modelo principal: `qwen3-8b` mediante endpoint compatible con OpenAI.
- Proveedor por defecto del Worker: `openai-compatible`.
- Endpoint por defecto: DashScope/Alibaba compatible-mode.
- Fallback configurable: NVIDIA con `nvidia/llama-3.1-nemotron-nano-8b-v1`.
- Secreto principal: `OPENAI_COMPATIBLE_API_KEY` guardado como Cloudflare secret.
- Secreto opcional de fallback/pruebas: `NVIDIA_API_KEY`.

La API key no debe guardarse en Android, Gradle, recursos, commits ni archivos versionados.

Para configurar localmente el endpoint:

```properties
bibiEndpointUrl=https://biblion-bibi.cristiancogollo4.workers.dev/ask
```

Para desplegar el Worker:

```powershell
cd workers\bibi
npx wrangler secret put OPENAI_COMPATIBLE_API_KEY
npx wrangler deploy
```

Para volver temporalmente a NVIDIA, cambia `BIBI_PROVIDER = "nvidia"` y `BIBI_MODEL = "nvidia/llama-3.1-nemotron-nano-8b-v1"` en `workers/bibi/wrangler.toml`, y configura:

```powershell
npx wrangler secret put NVIDIA_API_KEY
```

El Worker tiene respaldos para respuestas de identidad, versiones, dominio no biblico, diccionario biblico y timeouts del proveedor IA.

---

## Modo estudio

El modo estudio combina el lector biblico con un editor para preparar ensenanzas, bosquejos, devocionales o clases.

### Estado actual (24 Jul 2026)

La implementacion vigente vive en `feature/studydocs/`. El editor usa hojas carta fijas de 850 x 1100 dp, paginacion por fragmentos editables, zoom pinch 0.75x-2.0x, undo/redo, modo oscuro y formato enriquecido. Los bloques vigentes son parrafo, encabezado, lista con vinetas, lista numerada, versiculo y cita.

El autosave siempre persiste una copia local mediante `StudyDocRepository.saveDraft()`, incluso sin titulo. Esa copia queda con `is_published = 0`, por lo que no aparece en "Mis ensenanzas", busqueda, conteos ni sincronizacion. El guardado manual exige un titulo y publica mediante `save()`.

Al salir de un documento nuevo vacio no se muestra advertencia. Si existe contenido, titulo, bloques adicionales o cambios en una ensenanza existente, se muestra un dialogo para continuar editando o salir sin guardar.

La persistencia actual es Room v3 con migraciones 1->2 y 2->3. La sincronizacion Firestore del modo estudio sigue deshabilitada. Tambien quedan pendientes la recuperacion visible de borradores tras cierre forzado y la separacion entre snapshot publicado y copia de trabajo.

> La descripcion basada en `feature/study/`, `StudyEntity`, `StudyDocumentEngine` y `StudyEditorScreen` que aparece debajo se conserva como referencia historica del editor anterior. Para cambios nuevos se debe usar `STUDY_DOCS_V2.md` y el codigo de `feature/studydocs/`.

### Arquitectura del documento

El documento de una ensenanza se representa como una lista de `StudyBlockNode` serializada en JSON dentro de `StudyEntity.contentSerialized`. Ese formato se conserva porque permite:

- trabajo offline con Room;
- sincronizacion con Firestore;
- importacion/exportacion en formato `.biblion`;
- lectura estructurada sin depender de HTML.

La logica pura del documento vive en `StudyDocumentEngine`. Este motor concentra:

- normalizacion del flujo de bloques;
- conversion defensiva de contenido legado;
- aplicacion y limpieza de estilos por rango;
- manejo de columnas y bloques embebidos dentro de columnas;
- insercion, actualizacion, colapso y eliminacion de bloques interactivos;
- snapshot de texto plano para busqueda, autosave y contexto de Bibi;
- deteccion de referencias biblicas.

`StudyViewModel` debe coordinar estado, autosave, persistencia, citas y sincronizacion, pero no debe duplicar reglas internas de mutacion del documento. Nuevas herramientas del editor deben agregarse primero al motor cuando transformen bloques o texto.

`StudyEditorScreen` renderiza el lienzo de bloques con `LazyColumn` y claves estables por bloque. Esto mejora el rendimiento en ensenanzas largas, reduce recomposiciones innecesarias y mantiene mejor el estado visual de parrafos, columnas, notas, citas y herramientas flotantes.

### Herramientas del editor

#### Texto libre

Bloque principal para redactar la ensenanza. Permite escribir parrafos continuos y separar ideas. Enter separa parrafos en encabezados; en listas con vinetas y numeradas crea un nuevo item del mismo tipo.

#### Encabezado

Convierte un parrafo en titulo/seccion. Sirve para estructurar el bosquejo. Enter en un encabezado crea un nuevo parrafo debajo.

#### Lista con vinetas

Convierte parrafos en items de lista para puntos no secuenciales. Enter crea un nuevo item con vineta.

#### Lista numerada

Convierte parrafos en items ordenados para pasos, argumentos o secuencias. Enter crea un nuevo item numerado.

#### Columnas

Permite comparar o presentar dos ideas lado a lado. Internamente no es un bloque independiente: usa un parrafo con `role = "columns"` y `parallelText`.

Usos recomendados:

- contraste de ideas;
- predicar/refugio;
- antes/despues;
- texto/aplicacion.

#### Citar

Inserta una cita biblica como bloque interactivo `QuotedVerse`.

Funciones:

- insertar texto completo de la cita;
- mantener rangos como una sola referencia;
- mostrar indicadores inline de versiculo;
- cambiar version biblica;
- comparar con otra version;
- ocultar/mostrar comparacion.

Las citas se acumulan como pendientes desde el lector y se insertan al tocar "Citar" en el editor.

#### Nota

Bloque para observaciones, ideas auxiliares, datos de contexto o recordatorios del expositor.

#### Reflexion

Bloque para desarrollar una idea espiritual o pastoral vinculada al tema.

#### Estilos de texto

Permite aplicar estilos a rangos seleccionados:

- color de texto;
- color de fondo (resaltado);
- negrita;
- cursiva;
- subrayado;
- tamano de fuente (aumentar/disminuir).

La seleccion de colores se muestra en una burbuja flotante sobre la barra de herramientas al tocar "Color" o "Resaltar".

#### Modo enfoque

Oculta el panel del lector y deja el editor como area principal de trabajo.

#### Bibi

Abre un chat flotante para pedir ayuda biblica durante la preparacion. Mientras genera respuesta, muestra una burbuja de carga. En modo estudio permite insertar respuestas como Nota o Reflexion.

#### Guardar con metadata

Antes de guardar, Biblion solicita titulo y etiquetas organizadas por secciones.

---

## Lectura de ensenanzas

La pantalla de lectura de ensenanzas renderiza el documento estructurado, no solo texto plano.

Funciones actuales:

- visualizacion de parrafos, encabezados, listas, columnas, notas, reflexiones y citas;
- cambio de version en citas biblicas;
- comparacion de versiones en citas biblicas (lado a lado o apilado);
- ocultar/mostrar comparacion por cita;
- numeracion inline de versiculos con color diferenciado para modo claro/oscuro;
- aumentar/disminuir tamano de letra;
- alternar modo claro/oscuro;
- lectura vertical en moviles;
- lectura en pantalla dividida en pantallas grandes (>=840dp);
- deduplicacion automatica de bloques de cita: si una cita existe como `Citation` y `QuotedVerse`, se muestra solo la version con soporte de comparacion.

---

## Sistema de etiquetas

Las ensenanzas usan un sistema rico de etiquetas sugeridas.

### Proposito

- `predicacion`
- `devocional`
- `estudio-biblico`
- `clase`
- `discipulado`
- `formacion`

### Audiencia

- `jovenes`
- `iglesia`
- `lideres`
- `universitarios`
- `familias`
- `simpatizantes`
- `ninos`
- `mujeres`
- `hombres`
- `ancianos`
- `grupos-especiales`
- `pastores`

### Tema

- `identidad`
- `fe`
- `gracia`
- `proposito`
- `oracion`
- `evangelismo`
- `servicio`
- `esperanza`
- `doctrina`
- `amor`
- `misiones`
- `adoracion`

### Estado

- `borrador`
- `en-preparacion`
- `finalizado`

### Reglas de guardado

Para guardar una ensenanza se requiere:

- titulo obligatorio;
- al menos una etiqueta de proposito;
- al menos una etiqueta de audiencia;
- al menos una etiqueta de tema;
- exactamente una etiqueta de estado.

Proposito, audiencia y tema permiten seleccion multiple. Estado permite una sola seleccion.

---

## Arquitectura

La organizacion actual sigue un enfoque por capas con patron **MVVM**.

### UI

Pantallas y componentes Compose:

- `HomeScreen`
- `BooksScreen` (con bottom sheet de descripcion de libros via long press)
- `ReaderScreen`
- `SearchScreen` (con boton "Diccionario biblico" que navega a DictionaryScreen)
- `DictionaryScreen` (8 categorias con busqueda y cards de color)
- `DictionaryCategoryScreen` (entradas de una categoria con busqueda en vivo)
- `StudyEditorScreen`
- `StudyReadScreen`
- `EnsenanzaScreen`
- `StudyTagSelector`
- `StudyAssistantPanel`

### ViewModel

`StudyViewModel` coordina:

- estado del editor;
- autosave;
- carga y guardado de estudios;
- insercion de citas;
- cambio/comparacion de versiones en citas;
- metadata;
- seeds/demo de estudio.

### Data layer

- `BibleRepository`: acceso a textos biblicos desde `assets` y cache. Soporta busqueda con filtros (`BibleSearchFilter` con testament y bookName).
- `DictionaryRepository`: acceso al diccionario biblico unificado (Easton's + Theographic, 6,346 entradas en `dictionary_v2.db`). Usado por `DictionaryEngine` para generar respuestas locales de Bibi. Incluye `getEntriesByCategory()`, `getCategoryCounts()` y `searchEntries()`.
- `StudyDatabase`: Room para cuadernos, estudios y citas vinculadas.
- `SearchHistoryDatabase`: Room para historial de busquedas (`search_history.db`) con normalizacion y conteo de uso.
- `ChatDatabase`: Room para historial de chats de Bibi (`bibi_chat.db`) con sesiones y mensajes persistidos.
- `FirestoreSyncManager`: sincronizacion de preferencias, resaltados y estudios.
- `AppPreferencesSyncStore`: preferencias locales sincronizables.
- `UserProfileRepository`: lectura y escritura del perfil en Firestore, foto en Storage y color de avatar.
- `StudyAssistantRepository`: contrato Android para Bibi con estrategia "local primero". `HttpStudyAssistantRepository` consulta primero `LocalStudyAssistantRepository` antes de ir al Worker. Soporta `chatHistory` para memoria conversacional.
- `ChatSessionRepository`: gestion de sesiones de chat de Bibi (crear, listar, eliminar, renombrar).
- `workers/bibi`: Cloudflare Worker que aplica prompts, dominio biblico, diccionario, versiones y conexion con Qwen/DashScope u otros proveedores compatibles.

### Navegacion

- `NavigationRoutes`
- `AppNavigation`
- `NavGraphShared`

---

## Estructura de datos

Biblion combina persistencia local con sincronizacion en Firebase.

### Room local

`StudyDatabase` mantiene los datos del modo estudio en el dispositivo:

- `study_notebooks`: cuadernos de estudio.
- `studies`: ensenanzas/documentos del usuario.
- `linked_citations`: citas biblicas vinculadas a una ensenanza.

`SearchHistoryDatabase` (`search_history.db`) persiste las busquedas del usuario:

- `search_history`: tabla con `query`, `normalized_query`, `use_count`, `last_used_at`. Indice unico en `normalized_query` para deduplicar. Migracion destructiva aceptable (los datos se regeneran).

`ChatDatabase` (`bibi_chat.db`) persiste el historial de chats de Bibi:

- `chat_sessions`: sesiones de conversacion con Bibi (id, title, first_query, mode, created_at, updated_at).
- `chat_messages`: mensajes dentro de una sesion (id, session_id FK con CASCADE, role "user"/"assistant", content, resolved_term, intent, created_at). Permite multiples conversaciones independientes.

`DictionaryDatabase` (Room v3, basada en asset `dictionary_v2.db`) contiene el diccionario biblico unificado:

- `dictionary_entries`: 6,346 entradas con definiciones y metadata (gender, birth_year, death_year, latitude, longitude, aliases, feature_type).
- `getEntriesByCategory()`: todas las entradas de una categoria (limit 9999).
- `getCategoryCounts()`: conteo por categoria para las cards de `DictionaryScreen`.

Campos relevantes de sincronizacion:

- `remoteId`;
- `ownerUid`;
- `deletedAt`;
- `lastSyncedAt`;
- `syncVersion`.

Las citas vinculadas conservan:

- `book`;
- `chapter`;
- `verseStart`;
- `verseEnd`;
- `version`;
- `positionMetadata`.

### Firestore privado por usuario

La sincronizacion actual usa documentos privados bajo:

```text
users/{uid}
```

Subcolecciones actuales:

```text
users/{uid}/preferences/app
users/{uid}/notebooks/{notebookRemoteId}
users/{uid}/studies/{studyRemoteId}
users/{uid}/chapter_highlights/{book__chapter}
```

`users/{uid}` contiene el perfil principal:

| Campo | Funcion |
| --- | --- |
| `uid` | Identificador de Firebase Auth. |
| `email` / `correo` | Correo de inicio de sesion. |
| `nombres` | Nombres reales. |
| `apellidos` | Apellidos reales. |
| `alias` | Nombre visible en Biblion. |
| `rol` | Tipo general: `LECTOR`, `PUBLICADOR`, `ADMIN`. |
| `estadoPublicador` / `estado_publicador` | Estado para publicar: `NO_APROBADO`, `PENDIENTE`, `APROBADO`, `SUSPENDIDO`. |
| `plan` | Plan del usuario: `FREE`, `GO`, `PLUS`. |
| `fotoPerfil` / `foto_perfil` | URL de foto en Firebase Storage. |
| `avatarColor` / `avatar_color` | Color del avatar cuando no hay foto. |
| `biografia` | Descripcion breve del usuario. |
| `fechaRegistro` / `fecha_registro` | Fecha de creacion del perfil. |
| `createdAt`, `updatedAt`, `lastLoginAt`, `lastSeenAt` | Trazabilidad de sincronizacion. |

Contadores preparados para la red:

| Campo | Funcion |
| --- | --- |
| `totalEnsenanzasCreadas` | Ensenanzas creadas por el usuario. |
| `totalEnsenanzasPublicadas` | Ensenanzas publicadas por publicadores aprobados. |
| `totalDescargas` | Descargas asociadas al usuario o a sus publicaciones segun contexto. |
| `totalLikes` | Likes recibidos o acumulados en publicaciones. |
| `totalGuardados` | Ensenanzas guardadas/favoritas. |
| `totalComentarios` | Comentarios hechos o recibidos segun el flujo social. |
| `totalSeguidores` | Usuarios que siguen a este usuario. |
| `totalSiguiendo` | Usuarios seguidos por este usuario. |

### Firebase Storage

Las fotos de perfil se guardan en:

```text
profile_photos/{uid}/avatar.jpg
```

El cliente guarda la URL publica/descargable en Firestore. Si no existe foto, la UI usa `avatarColor`.

### Modelo previsto para la red de Biblion

Por ahora la red se modelara en Firebase. La estructura prevista sigue estas entidades:

#### Usuario

Representado por `users/{uid}`. Todos los usuarios pueden crear ensenanzas privadas o sincronizadas. Solo usuarios con `estadoPublicador = APROBADO` pueden publicar ensenanzas publicas.

#### Ensenanza

Entidad central para contenido creado por usuarios.

Campos previstos:

- `id`;
- `usuario_id`;
- `titulo`;
- `descripcion`;
- `contenido`;
- `estado`: `BORRADOR`, `PUBLICADA`, `ARCHIVADA`;
- `visibilidad`: `PRIVADA`, `NUBE`, `PUBLICA`;
- `total_descargas`;
- `total_likes`;
- `total_guardados`;
- `fecha_creacion`;
- `fecha_actualizacion`.

Reglas:

- un lector puede tener ensenanzas `PRIVADA` o `NUBE`;
- un publicador aprobado puede publicar con `estado = PUBLICADA` y `visibilidad = PUBLICA`.

#### Seguidor

Relacion entre usuarios:

- `id`;
- `seguidor_id`;
- `seguido_id`;
- `fecha`.

#### Descarga

Registra descargas de ensenanzas:

- `id`;
- `usuario_id`;
- `ensenanza_id`;
- `fecha_descarga`.

#### Favorito

Guarda ensenanzas conservadas por un usuario:

- `id`;
- `usuario_id`;
- `ensenanza_id`;
- `fecha_guardado`.

#### Like

Registra likes en ensenanzas publicas:

- `id`;
- `usuario_id`;
- `ensenanza_id`;
- `fecha`.

Regla recomendada: un usuario solo puede dar un like por ensenanza.

#### Etiqueta

Catalogo de temas o categorias:

- `id`;
- `nombre`.

#### EnsenanzaEtiqueta

Relacion muchos-a-muchos entre ensenanzas y etiquetas:

- `id`;
- `ensenanza_id`;
- `etiqueta_id`.

#### Comentario

Comentarios en ensenanzas publicas:

- `id`;
- `usuario_id`;
- `ensenanza_id`;
- `contenido`;
- `fecha_creacion`.

Regla recomendada: solo comentar ensenanzas con `visibilidad = PUBLICA`.

### Reglas de consistencia de la red

- Todo usuario puede crear ensenanzas.
- Lectores pueden guardar ensenanzas privadas o sincronizadas en nube.
- Solo usuarios con `estadoPublicador = APROBADO` pueden crear ensenanzas publicas.
- Likes, comentarios, descargas publicas y favoritos sociales aplican principalmente a ensenanzas publicas.
- Los contadores son valores derivados de las colecciones sociales.
- La visibilidad decide si el contenido es personal, sincronizado o publico.
- El estado de publicador decide si el usuario puede aparecer como publicador en la red.

---

## Tecnologias

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- AndroidX Lifecycle
- Kotlinx Coroutines + Flow (para busqueda en vivo con debounce y collectLatest)
- Room (con multiples databases: study, search_history, bibi_chat)
- Kotlinx Serialization
- KSP
- Firebase Auth
- Firestore
- Firebase Storage
- Cloudflare Workers
- Qwen3-8B / DashScope compatible con OpenAI
- NVIDIA API como fallback configurable
- Robolectric / pruebas unitarias Android

---

## Instalacion

### Requisitos

- Android Studio reciente.
- JDK 11 compatible con el proyecto.
- SDK de Android.

### Pasos

```bash
git clone https://github.com/<tu-usuario>/Biblion.git
cd Biblion
```

Luego:

1. Abre el proyecto en Android Studio.
2. Sincroniza Gradle.
3. Configura Firebase si vas a probar autenticacion, sincronizacion, perfil o red.
4. Configura `bibiEndpointUrl` en `local.properties` si vas a probar Bibi online.
5. Ejecuta en emulador o dispositivo fisico Android.

### Firebase local

Para probar Google Sign-In en debug:

1. Ejecuta:

```powershell
.\gradlew.bat :app:signingReport
```

2. Copia el SHA-1 de la variante `debug`.
3. Agrega esa huella a la app Android en Firebase Console.
4. Descarga el nuevo `google-services.json`.
5. Reemplaza `app/google-services.json`.

Las huellas no se agregan por usuario. Se agregan por certificado de firma de la app: debug, release, upload key o Play App Signing.

Para probar foto de perfil, Firebase Storage debe permitir que un usuario autenticado escriba su propia imagen:

```text
profile_photos/{uid}/avatar.jpg
```

---

## Validacion local

Compilar:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Ejecutar pruebas unitarias:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Validar Worker de Bibi:

```powershell
node --check workers\bibi\src\index.js
```

Probar modelos candidatos de Bibi:

```powershell
cd workers\bibi
$env:NVIDIA_API_KEY="tu_api_key"
npm run eval:models -- nvidia/llama-3.1-nemotron-nano-8b-v1
```

El evaluador revisa:

- JSON valido;
- dominio (`bible` u `out_of_domain`);
- idioma esperado;
- referencias permitidas;
- textos obligatorios y prohibidos;
- estructura minima para bosquejos;
- latencia promedio.

Tambien se pueden comparar varios modelos pasando sus IDs exactos del proveedor:

```powershell
npm run eval:models -- modelo_1 modelo_2 modelo_3
```

Para endpoints compatibles con OpenAI, por ejemplo DashScope/Alibaba:

```powershell
cd workers\bibi
$env:BIBI_EVAL_PROVIDER="openai-compatible"
$env:OPENAI_COMPATIBLE_BASE_URL="https://tu-endpoint/compatible-mode/v1"
$env:OPENAI_COMPATIBLE_API_KEY="tu_api_key"
npm run eval:models -- qwen3-8b
```

No guardes API keys en el repositorio. Si una clave se comparte por error, debe rotarse en el proveedor.

Para comparar proveedores distintos en una sola corrida, usa prefijos:

```powershell
cd workers\bibi
$env:NVIDIA_API_KEY="tu_nvidia_key"
$env:OPENAI_COMPATIBLE_BASE_URL="https://tu-endpoint/compatible-mode/v1"
$env:OPENAI_COMPATIBLE_API_KEY="tu_openai_compatible_key"
npm run eval:models -- nvidia:nvidia/llama-3.1-nemotron-nano-8b-v1 openai:qwen3-8b
```

Pruebas relevantes del modo estudio:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.cristiancogollo.biblion.ReaderCitationGroupingTest --tests com.cristiancogollo.biblion.StudyViewModelTest --tests com.cristiancogollo.biblion.StudyTagSelectorTest
```

---

## Roadmap

Ideas pendientes o en evolucion:

- **Plantillas de estudio**: predicacion expositiva, devocional, estudio biblico, clase. Prellenado de estructura.
- **Reordenar bloques**: agarre y arrastre para reorganizar secciones en el editor.
- **Contador de palabras**: para conciencia de extension en predicaciones.
- **Exportacion real de ensenanzas a PDF**.
- **Busqueda avanzada dentro de ensenanzas**.
- **Mejoras de accesibilidad**.
- Mayor cobertura de pruebas UI.
- Mejoras visuales para pantallas grandes.
- Gestion avanzada de cuadernos.
- Sincronizacion mas robusta ante conflictos.
- Implementacion completa de la red de Biblion: publicaciones, seguidores, likes, comentarios, favoritos y descargas publicas.
- Reglas de seguridad Firestore/Storage para roles, publicadores aprobados y propiedad de documentos.
- Recuperacion automatica de pasajes biblicos para Bibi desde assets locales.
- Diccionario biblico ampliado y administrable.
- Insercion de `suggestedBlocks` de Bibi como bloques reales del modo estudio.

---

## Equipo

- Cristian Felipe Cogollo Rodriguez - Co-fundador & Lead Developer.
- Anderson Geovanny Duarte Largo - Co-fundador & Estrategia / Alianzas.

---

## Contribuciones

Las contribuciones son bienvenidas.

Flujo recomendado:

1. Crea una rama enfocada.
2. Realiza cambios pequenos y verificables.
3. Ejecuta compilacion y pruebas relacionadas.
4. Abre PR con problema, solucion, riesgos y evidencia.

Para detalles de estandares internos, revisa `AGENTS.md`.
