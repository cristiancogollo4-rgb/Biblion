---
title: Lectura bíblica
description: Carga de capítulos, lectura continua, paneles paralelos, selección, resaltado y referencias.
---

# Lectura bíblica

## Abrir un capítulo

`ReaderContent()` mantiene el libro, capítulo, versión, contenido y estados de carga en Compose. Su función local `loadChapter()` cancela la carga anterior, llama al repositorio y **solo aplica el resultado si aún coincide** el libro, capítulo y versión solicitados. Esto impide que una respuesta antigua sobrescriba una navegación más reciente.

```text
Elegir capítulo → loadChapter(book, chapter, version) en ReaderContent
  → BibleRepository.getChapter(...)
  → BibleDao.getChapterCount() + getChapterContent()
  → ChapterContent(chapterCount, verses, titlesByVerse)
  → chapterContent / isChapterLoading / chapterLoadFailed
```

`BibleRepository.getChapter()` trabaja en `Dispatchers.IO`, normaliza libro y versión, consulta primero una caché LRU y usa un `Mutex` para evitar cargas duplicadas concurrentes. Si no está en caché, consulta la base bíblica local. Tras cargar un capítulo, el lector precarga los adyacentes con una breve demora. **Firestore no interviene en la lectura del texto bíblico.**

| Función | Responsabilidad | Si falla o no hay texto |
| --- | --- | --- |
| `loadChapter()` en `ReaderScreen.kt` | Iniciar/cancelar carga y actualizar estado visible | Marca `chapterLoadFailed`; distingue libro ausente en versión |
| `BibleRepository.getChapter()` | Entregar versículos y títulos de la versión pedida | Puede devolver capítulo vacío; conserva el contrato local |
| `BibleDao.getChapterContent()` | Consulta concreta al SQLite preempaquetado | No decide el mensaje de UI |

## Lectura continua

El lector mantiene una ventana de **capítulo anterior + actual + siguiente**. Un efecto carga los adyacentes; otro observa con `snapshotFlow` la clave del primer elemento visible. `continuousChapterDirection()` decide si el foco cruzó al capítulo anterior o siguiente y `ContinuousChapterWindow.shift()` mueve la ventana. En ese momento cambia `selectedChapter`, por lo que debe actualizarse también el indicador superior.

Esta lógica tiene dos riesgos distintos: mostrar el contenido nuevo sin actualizar el indicador, y conservar una precarga vieja tras cambiar de libro o versión. Por eso los efectos comprueban la selección vigente antes de aplicar resultados. Al modificar las claves de los ítems de la lista, prueba **avance y retroceso**, no solo scroll hacia abajo.

## Lectura paralela

En esta rama, `ReaderContent` ofrece lectura paralela cuando el ancho disponible es de al menos **600 dp** y hay un libro abierto. `ParallelBibleReader()` recibe dos `BiblePaneSelection`: cada una contiene libro, capítulo y versión. `IndependentBiblePane()` carga su capítulo y, si está activa la lectura continua, sus vecinos. Los cambios de un panel vuelven mediante `onPrimarySelectionChange` o `onSecondarySelectionChange`; no deben sustituir la selección del otro.

Al activar el modo desde el lector, ambos paneles comienzan en el pasaje visible y en la versión actual. Después pueden navegar de forma independiente. En el panel secundario, la última selección se guarda en `ReaderPreferencesStore`.

## Selección, resaltado y referencias

`VerseItem()` inicia la selección con **pulsación prolongada**. Cuando ya hay una selección activa, los demás versículos se pueden añadir o quitar con un toque. `saveHighlights()` actualiza el estado visible, la caché de resaltados y `AppPreferencesSyncStore`; la sincronización de esos datos de usuario se gestiona aparte.

Para referencias cruzadas, `CrossReferenceVoteEngine.getSourceVersesForChapter()` identifica los versículos que tienen relaciones. `ReaderCrossReferencesSheet` abre el contenido al tocar el icono correspondiente. El número de votos del dataset se usa internamente para ordenar/filtrar; **no debe aparecer en la UI**.

## Archivos y pruebas

| Tema | Archivo principal |
| --- | --- |
| Estado, capítulo y gestos | `feature/reader/ReaderScreen.kt` |
| Dos paneles independientes | `feature/reader/ui/ParallelBibleReader.kt` |
| Caché y consultas | `feature/bible/data/BibleRepository.kt` y `BibleDatabase.kt` |
| Resaltados | `feature/reader/HighlightsCache.kt` |
| Referencias | `feature/bibi/engine/CrossReferenceVoteEngine.kt` |

Pruebas de apoyo: `BibleRepositoryConcurrencyTest`, `HighlightsCacheTest` y `CrossReferenceVoteEngineTest` bajo `app/src/test/java/`. Para scroll, gestos y paneles, completa las [pruebas en dispositivo](/desarrollo/pruebas).
