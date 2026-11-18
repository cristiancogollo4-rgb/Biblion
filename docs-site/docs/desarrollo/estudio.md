---
title: Editor de enseñanzas
description: Modelo, operaciones, texto con estilo, paginación, borradores y guardado.
---

# Editor de enseñanzas

## La idea central

Una enseñanza es un `StudyDoc` con título, metadatos y bloques `StudyBlock`. El **documento es la fuente de verdad**: las hojas son una representación calculada para leer y editar. Cambiar de página no debe partir el documento en registros independientes.

```text
Acción de edición
  → StudyDocViewModel actualiza estado
  → StudyDocEngine.apply(doc, StudyOp) para cambios estructurales
  → StudyDoc nuevo + OpResult
  → PaginationEngine.paginate(blocks, tamaño de página)
  → PageFragment visibles
  → autosave local o Guardar explícito
```

`StudyDocEditorScreen()` coordina el editor, los diálogos y el modo de trabajo. `StudyModeEditorPanel` muestra la barra de herramientas y las páginas. `StudyDocViewModel` conserva `StudyEditorUiState`, historial de deshacer/rehacer y el texto que se está editando.

## Dos clases de cambio

| Acción | Función clave | Qué debe conservar |
| --- | --- | --- |
| Insertar, eliminar, dividir o cambiar un bloque | `StudyDocViewModel.applyOp()` → `StudyDocEngine.apply()` | IDs de bloque y resultado válido |
| Escribir o colorear texto | `onRichTextChanged()` / `applyStyleToActive()` | Texto, selección y rangos de `StyledText` |
| Entrar/salir de una lista | `handleListEnter()` / `handleListBackspace()` | Tipo de lista, ítems y posición del cursor |
| Deshacer/rehacer | Historial del ViewModel | Documento y selección coherentes |

`StudyDocEngine.apply()` es puro: recibe documento y operación, y devuelve otro documento con un resultado de éxito o fallo. La UI no debería implementar por su cuenta la regla de eliminar o dividir bloques. Para texto enriquecido, `onRichTextChanged()` convierte el estado visual a `StyledText`, actualiza el documento y programa autosave.

## Cómo funciona la paginación

`PaginatedSheet` invoca `PaginationEngine.paginate()`. El motor mide bloques y texto, abre páginas cuando falta espacio y emite fragmentos (`PageFragment`). Un encabezado puede pasar a la página siguiente junto al contenido que lo sigue para evitar quedar huérfano. Si un bloque no cabe, se divide en fragmentos; el motor comprueba cancelación mientras trabaja.

La prueba importante no es solo el número de hojas: tras paginar, **el contenido y los estilos deben seguir íntegros**, el cursor debe corresponder al texto lógico y la edición no debe duplicar ni perder caracteres. `PaginationEngineTest` cubre el motor; el zoom, teclado y foco se validan en dispositivo.

## Borrador frente a enseñanza guardada

1. `scheduleAutoSave()` espera una pausa y llama a `StudyDocRepository.saveDraft()`.
2. Si nunca se guardó explícitamente, ese documento sigue **sin publicar**: queda local y no aparece en «Mis enseñanzas».
3. `saveNow(title, tags)` rechaza un título vacío; `saveNow()` llama a `StudyDocRepository.save()`.
4. `save()` marca el documento como publicado en la biblioteca personal y solicita sincronización.
5. Si un documento **ya estaba publicado**, un autosave posterior conserva esa condición y sus cambios pueden sincronizarse. `saveDraft()` no vuelve privado un documento publicado.

«Publicado» aquí significa visible en la **biblioteca personal**, no público para otros usuarios. La fase 2 necesitará reglas propias para el repositorio social.

## Archivos y pruebas

| Cambiar… | Abre… |
| --- | --- |
| Regla de bloques | `feature/studydocs/engine/StudyDocEngine.kt` y `StudyOp.kt` |
| Estado, formato, autosave | `feature/studydocs/domain/StudyDocViewModel.kt` |
| Hojas y fragmentos | `feature/studydocs/ui/pagination/PaginationEngine.kt` y `PaginatedSheet.kt` |
| Guardado local | `feature/studydocs/data/StudyDocRepository.kt` y `StudyDocDao.kt` |
| Pantalla y menús | `feature/studydocs/ui/editor/StudyDocEditorScreen.kt` |

Pruebas iniciales: `StudyDocEngineListTest`, `StudyDocSaveRulesTest`, `PaginationEngineTest` y `StudyDocSyncPolicyTest` bajo `app/src/test/java/`. La [guía de datos](/desarrollo/datos) continúa desde Room hasta Firestore.
