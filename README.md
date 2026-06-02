# Biblion

**Biblion** es una aplicacion movil Android construida con **Kotlin + Jetpack Compose + Material 3**. Su objetivo es ofrecer una experiencia clara para lectura biblica, busqueda de versiculos y preparacion de ensenanzas desde un solo lugar.

El proyecto esta en desarrollo activo. La experiencia visual, los flujos de estudio y la sincronizacion siguen evolucionando.

---

## Estado actual

Biblion ya cuenta con:

- Lectura biblica por testamento, libro y capitulo.
- Busqueda de versiculos por texto.
- Selector de version biblica.
- Resaltado de versiculos.
- Preferencias de lectura, incluyendo tamano de fuente.
- Modo claro/oscuro global.
- Autenticacion con Firebase.
- Sincronizacion con Firestore para preferencias, resaltados, estudios y citas.
- Modo estudio con editor estructurado.
- Gestion de "Mis ensenanzas".
- Lectura enriquecida de ensenanzas.
- Sistema de etiquetas sugeridas y validacion de metadata.
- Filtros de ensenanzas por titulo o etiquetas.

---

## Caracteristicas principales

### Lectura biblica

- Navegacion por Antiguo y Nuevo Testamento.
- Lectura por libro/capitulo.
- Cambio de version biblica desde el lector.
- Busqueda de versiculos y navegacion directa al resultado.
- Seleccion multiple de versiculos.
- Resaltado por color.
- Insercion de citas al modo estudio.

### Citas por rango

Cuando se seleccionan versiculos contiguos, Biblion los agrupa como una sola cita:

```text
Genesis 1:1-3
1 En el principio... 2 Y la tierra... 3 Y dijo Dios...
```

Esto evita crear tres componentes separados para una sola referencia.

### Mis ensenanzas

La seccion **Mis ensenanzas** permite:

- Ver ensenanzas guardadas.
- Abrir una ensenanza en modo lectura.
- Editar una ensenanza en modo estudio.
- Editar titulo y etiquetas.
- Eliminar ensenanzas.
- Filtrar por titulo o etiquetas.

---

## Modo estudio

El modo estudio combina el lector biblico con un editor para preparar ensenanzas, bosquejos, devocionales o clases.

### Herramientas del editor

#### Texto libre

Bloque principal para redactar la ensenanza. Permite escribir parrafos continuos y separar ideas.

#### Encabezado

Convierte un parrafo en titulo/seccion. Sirve para estructurar el bosquejo.

#### Lista con vinetas

Convierte parrafos en items de lista para puntos no secuenciales.

#### Lista numerada

Convierte parrafos en items ordenados para pasos, argumentos o secuencias.

#### Columnas

Permite comparar o presentar dos ideas lado a lado. Internamente no es un bloque independiente: usa un parrafo con `role = "columns"` y `parallelText`.

Usos recomendados:

- contraste de ideas;
- predicar/refugio;
- antes/despues;
- texto/aplicacion.

#### Citar

Inserta una cita biblica como bloque interactivo.

Funciones:

- insertar texto completo de la cita;
- mantener rangos como una sola referencia;
- mostrar indicadores inline de versiculo;
- cambiar version biblica;
- comparar con otra version;
- ocultar/mostrar comparacion.

#### Nota

Bloque para observaciones, ideas auxiliares, datos de contexto o recordatorios del expositor.

#### Reflexion

Bloque para desarrollar una idea espiritual o pastoral vinculada al tema.

#### Estilos de texto

Permite aplicar estilos a rangos seleccionados:

- color de texto;
- color de fondo;
- negrita;
- cursiva;
- subrayado;
- tamano de fuente.

#### Modo enfoque

Oculta el panel del lector y deja el editor como area principal de trabajo.

#### Guardar con metadata

Antes de guardar, Biblion solicita titulo y etiquetas organizadas por secciones.

---

## Lectura de ensenanzas

La pantalla de lectura de ensenanzas renderiza el documento estructurado, no solo texto plano.

Funciones actuales:

- visualizacion de parrafos, encabezados, listas, columnas, notas, reflexiones y citas;
- cambio de version en citas;
- comparacion de versiones en citas;
- ocultar/mostrar comparacion;
- numeracion inline de versiculos con color diferenciado para modo claro/oscuro;
- aumentar/disminuir tamano de letra;
- alternar modo claro/oscuro;
- lectura vertical en moviles;
- lectura en pantalla dividida en pantallas grandes.

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
- `BooksScreen`
- `ReaderScreen`
- `SearchScreen`
- `StudyEditorScreen`
- `StudyReadScreen`
- `EnsenanzaScreen`
- `StudyTagSelector`

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

- `BibleRepository`: acceso a textos biblicos desde `assets` y cache.
- `StudyDatabase`: Room para cuadernos, estudios y citas vinculadas.
- `FirestoreSyncManager`: sincronizacion de preferencias, resaltados y estudios.
- `AppPreferencesSyncStore`: preferencias locales sincronizables.

### Navegacion

- `NavigationRoutes`
- `AppNavigation`
- `NavGraphShared`

---

## Tecnologias

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- AndroidX Lifecycle
- Room
- Kotlinx Serialization
- KSP
- Firebase Auth
- Firestore
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
3. Configura Firebase si vas a probar autenticacion/sincronizacion.
4. Ejecuta en emulador o dispositivo fisico Android.

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

Pruebas relevantes del modo estudio:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.cristiancogollo.biblion.ReaderCitationGroupingTest --tests com.cristiancogollo.biblion.StudyViewModelTest --tests com.cristiancogollo.biblion.StudyTagSelectorTest
```

---

## Roadmap

Ideas pendientes o en evolucion:

- Exportacion real de ensenanzas a PDF.
- Mejoras de accesibilidad.
- Mayor cobertura de pruebas UI.
- Mejoras visuales para pantallas grandes.
- Gestion avanzada de cuadernos.
- Busqueda avanzada dentro de ensenanzas.
- Sincronizacion mas robusta ante conflictos.

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
