# AGENTS.md

Este documento define los estandares operativos para contribuciones automatizadas y manuales en **Biblion**.
Su objetivo es mantener coherencia con el estado actual del repositorio: Android, Kotlin, Jetpack Compose, Material 3, MVVM, Room y sincronizacion con Firebase/Firestore.

## 1) Contexto del proyecto

- Plataforma: Android.
- Lenguaje: Kotlin.
- UI: Jetpack Compose + Material 3.
- Persistencia local: Room.
- Sincronizacion: Firebase Auth + Firestore para preferencias, estudios y citas.
- Arquitectura actual: enfoque por capas con patron MVVM.
- Modulo principal: `app/`.
- Paquete base: `com.cristiancogollo.biblion`.
- Compatibilidad:
  - `minSdk = 26`
  - Java/Kotlin `jvmTarget = 11`

## 2) Estado funcional actual

Biblion ya incluye:

- Lectura biblica por testamento, libro y capitulo.
- Busqueda de versiculos por texto.
- Selector de version biblica.
- Resaltado de versiculos con persistencia y sincronizacion.
- Modo claro/oscuro global.
- Autenticacion y sincronizacion de datos de usuario.
- Modo estudio con editor de ensenanzas.
- Listado de "Mis ensenanzas" con abrir, editar, eliminar, filtrar por titulo o etiqueta.
- Lectura de ensenanzas con controles de tamano de letra, modo claro/oscuro y pantalla dividida en pantallas grandes.
- Sistema de etiquetas sugeridas por seccion: proposito, audiencia, tema y estado.
- Validacion de guardado de ensenanzas: titulo obligatorio y etiquetas requeridas por seccion.
- Asistente biblico Bibi en modo estudio y lector normal.
- Integracion online de Bibi mediante Cloudflare Worker y modelo NVIDIA.
- Contexto para Bibi con versiones biblicas disponibles, version seleccionada, texto seleccionado, bloques actuales de la ensenanza, notas y diccionario biblico inicial.

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
- Usar componentes compartidos cuando aplique, por ejemplo `BiblionComponents` y `StudyTagSelector`.
- Textos visibles nuevos deberian ir a `res/values/strings.xml` y `values-es` cuando el cambio lo amerite.
- Mantener accesibilidad basica: `contentDescription` en iconos accionables.
- En pantallas compactas, evitar controles que saturen la barra superior.
- En pantallas grandes, se permite UI expandida como lectura dividida, siempre con fallback vertical en movil.

## 6) Modo estudio: herramientas y responsabilidades

El modo estudio se compone principalmente de `StudyEditorScreen`, `StudyViewModel`, `StudyData`, `ReaderScreen`, `StudyReadScreen` y `EnsenanzaScreen`.

### Herramientas del editor

- **Texto libre / parrafos**: bloque principal para escribir la ensenanza.
- **Encabezado**: cambia el rol visual del parrafo a titulo/seccion.
- **Lista con vinetas**: transforma parrafos en items de lista.
- **Lista numerada**: transforma parrafos en items numerados.
- **Columnas**: no es un bloque independiente; usa `StudyBlockNode.Paragraph` con `role = "columns"` y `parallelText`.
- **Citar**: inserta citas biblicas como `StudyBlockNode.QuotedVerse`.
- **Nota**: inserta un bloque de nota para observaciones, aclaraciones o recordatorios.
- **Reflexion**: inserta un bloque de reflexion vinculado a una idea o texto seleccionado.
- **Estilos de texto**: color, fondo, negrita, cursiva, subrayado y tamano para rangos seleccionados.
- **Aumentar/disminuir fuente de seleccion**: aplica tamano al texto seleccionado.
- **Modo enfoque**: oculta el panel del lector para concentrarse en el editor.
- **Guardar con metadata**: exige titulo y etiquetas validas.
- **Bibi**: asistente flotante para hacer preguntas biblicas, pedir ideas, pasajes relacionados, bosquejos, aplicaciones, notas o reflexiones. En modo estudio puede insertar respuestas como Nota o Reflexion.

### Bibi

Bibi es la asistente biblica oficial de Biblion. Esta integrada en:

- **Modo estudio**: respuestas mas profundas para preparar ensenanzas, predicaciones, devocionales, clases biblicas y discipulado.
- **Lector normal**: respuestas breves para comprender el pasaje actual, palabras, referencias y aplicaciones sencillas.

Reglas funcionales:

- Bibi no actua como asistente general.
- Su dominio es exclusivamente biblico/cristiano: Biblia, estudio biblico, contexto, personajes, lugares, historia biblica relacionada con las Escrituras, doctrina cristiana, discipulado, devocionales, predicacion, ensenanzas, reflexion, aplicacion, palabras biblicas, referencias cruzadas y comparacion de pasajes.
- Preguntas fuera de dominio deben responder con el mensaje de redireccion definido en `StudyAssistantRepository` y `workers/bibi/src/index.js`.
- Bibi no debe inventar versiculos, citas, personajes, eventos, doctrinas, revelaciones, profecias, mensajes personales de Dios ni interpretaciones sin fundamento biblico.
- Toda ensenanza, explicacion o aplicacion debe estar sustentada en las Escrituras o identificarse claramente como reflexion basada en ellas.
- Si una referencia no es segura, debe reconocerlo y sugerir verificar el pasaje.
- Si compara versiones, debe usar solo textos proporcionados por Biblion; no debe inventar traducciones.

Contexto enviado a Bibi:

- `mode`: `study` o `reader`.
- `intent`: `explain`, `define`, `cross_reference`, `application`, `outline`, `sermon`, `devotional`, `compare_versions` o `question`.
- `study.title`, `study.tags`, `study.selectedText`, `study.currentOutline`, `study.notes`.
- `bible.version`, `bible.availableVersions`, `bible.passages`.

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
- La URL se configura con `bibiEndpointUrl` en `local.properties` y se inyecta como `BuildConfig.BIBI_ENDPOINT_URL`.
- El Worker vive en `workers/bibi`.
- El Worker usa `NVIDIA_API_KEY` como secreto de Cloudflare, nunca en el APK.
- Si el endpoint falla o esta vacio, Android usa respuesta local de respaldo.
- El Worker tambien tiene respuestas de respaldo con diccionario biblico cuando NVIDIA tarda.

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

### Lectura de ensenanzas

`StudyReadScreen` debe mostrar el documento estructurado, no solo texto plano.

Funciones actuales:

- Render de parrafos, encabezados, listas, columnas, notas, reflexiones y citas.
- Cambio de version y comparacion en bloques de cita.
- Modo claro/oscuro desde la lectura.
- Aumentar/disminuir tamano de letra de lectura.
- Lectura en pantalla dividida solo en pantallas grandes.
- Lectura vertical en moviles.
- Filtro y administracion desde "Mis ensenanzas".
- Bibi en lector normal para preguntas biblicas basicas sobre el pasaje actual.

## 7) Estado y ViewModel

- Nuevas acciones de usuario deben agregarse de forma consistente en `StudyIntent`.
- El estado visible debe modelarse en `StudyUiState`.
- Evitar efectos secundarios ocultos; preferir flujos explicitos con corrutinas/Flow.
- Si se agrega logica testeable, preferir funciones puras o helpers internos con pruebas unitarias.
- Para tests, se permite inyectar dispatchers o desactivar semillas demo cuando mejore determinismo.

## 8) Datos, repositorios y Room

- Cambios de esquema Room deben ser compatibles y justificados.
- No romper datos existentes sin migracion.
- Mantener separacion entre entidades de persistencia y estado UI.
- Si se agrega cache o acceso a assets biblicos, seguir el patron de `BibleRepository` y caches dedicados.
- Las citas vinculadas deben conservar `book`, `chapter`, `verseStart`, `verseEnd` y `version`.

## 9) Navegacion

- Registrar rutas nuevas en `NavigationRoutes`, `AppNavigation` o `NavGraphShared` segun corresponda.
- Evitar duplicidad de rutas.
- Respetar `launchSingleTop` y `popUpTo` usados en la app.
- Las pantallas compartidas deben recibir dependencias como tema global mediante parametros, no accediendo a estado global oculto.

## 10) Pruebas y validacion

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

## 11) Dependencias y build

- No duplicar librerias ya administradas en `gradle/libs.versions.toml`.
- Nuevas dependencias deben declararse con version centralizada en el catalogo.
- Evitar actualizaciones masivas de versiones sin necesidad.
- No agregar secretos a Gradle, `local.properties`, commits ni recursos Android. Las claves online de Bibi deben vivir en Cloudflare Worker secrets.
- `workers/**/node_modules/` y `.wrangler/` deben permanecer ignorados.

## 12) Commits y PR

- Commits en imperativo y con alcance claro.
  - Ejemplo: `feat: agrega filtros de ensenanzas`
  - Ejemplo: `docs: actualiza estado de modo estudio`
- PR debe incluir:
  - problema;
  - solucion aplicada;
  - riesgos/impacto;
  - evidencia de compilacion/pruebas;
  - capturas si cambia UI.

## 13) Que evitar

- Refactors globales no solicitados.
- Mezclar cambios visuales con cambios funcionales grandes sin razon.
- Introducir deuda tecnica marcada como TODO sin contexto.
- Romper compatibilidad de datos locales sin estrategia.
- Revertir cambios ajenos del usuario.

## 14) Regla de consistencia

Si existe conflicto entre este documento y una instruccion explicita del solicitante para una tarea puntual, prevalece la instruccion explicita para esa tarea, manteniendo el resto de estandares.
