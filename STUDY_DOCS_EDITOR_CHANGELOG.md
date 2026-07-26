# Study Docs Editor Changelog

## 2026-07-25

- Modo estudio bloqueado en horizontal para telefonos, plegables y tabletas.
- Se usa `sensorLandscape`, por lo que funcionan ambas direcciones horizontales.
- La orientacion previa se restaura al abandonar el editor.
- El bloqueo cubre el editor independiente y el modo dividido del lector.
- Compatibilidad temporal de Android 16 habilitada para que las pantallas `sw600dp` respeten la orientacion solicitada.
- Validacion manual en Motorola Edge 60 Fusion: entrada vertical (`rotation=0`) a estudio horizontal (`rotation=1`).
- Pantallas compactas migradas de split 50/50 a panel unico con selector lateral Biblia/Documento.
- Los dos paneles conservan su composicion y estado al alternar.
- El teclado virtual deja de abrirse automaticamente; durante escritura compacta el encabezado se repliega para conservar hoja visible.
- Corregido el desbordamiento visual del panel Biblia que cubria la barra lateral al cambiar de panel o navegar a libros/lector.
- Validado el recorrido Inicio biblico -> Libros -> Mateo 1 -> Documento -> Mateo 1 sin perder navegacion, hoja ni controles laterales.

## 2026-07-24

- Paginacion editable por fragmentos con `PagedEditorLayout`, `PagedEditorUnitRenderer` y mapeo visual de saltos de pagina.
- Correcciones de foco, Enter, navegacion por flechas, union/division de bloques y reglas de listas.
- Undo/redo con checkpoints de texto enriquecido.
- Zoom pinch estable 0.75x-2.0x, presets y ajuste al ancho.
- Hoja carta centrada y geometria logica consistente entre dispositivos.
- Modo oscuro conectado al tema global.
- Selector de tamano 8-72 y formato que conserva color/resaltado al cambiar tamano.
- Room v3 con `is_published`: autosave de borradores ocultos y publicacion manual solo con titulo.
- Advertencia de salida con contenido pendiente, validada en dispositivo TB336FU.
- Pruebas nuevas para paginacion, offsets, Enter/listas, rich text y reglas de guardado.

### Pendiente para cierre de produccion

- Recuperacion de borradores despues de cierre forzado.
- Separar snapshot publicado y copia de trabajo.
- Esperar confirmacion de descarte antes de navegar.
- Rehabilitar sincronizacion Firestore.
- Pruebas instrumentadas de migracion y ciclo de proceso.

---

Fecha: 2026-06-24

## Resumen

Se ajusto el modo estudio para acercarlo mas a una experiencia tipo Google Docs sin abandonar la arquitectura por bloques de Biblion.

## Cambios aplicados

- Se centralizo el ritmo visual en `DocConfig`:
  - `PageContentVerticalPadding`
  - `PageGap`
  - `LineHeight`
- Se reemplazo la paginacion por conteo de bloques con una paginacion basada en altura estimada real del contenido.
- Se elimino el `chunked(30)` como mecanismo real de layout.
- `StyledTextEditor` ahora muestra el placeholder solo cuando el campo tiene foco real.
- Se redujo el padding local de los bloques para disminuir la sensacion de tarjetas separadas.
- Se normalizo el `lineHeight` del texto para mejorar densidad visual.

## Archivos tocados

- `app/src/main/java/com/cristiancogollo/biblion/feature/studydocs/ui/editor/DocConfig.kt`
- `app/src/main/java/com/cristiancogollo/biblion/feature/studydocs/ui/editor/DocumentPagination.kt`
- `app/src/main/java/com/cristiancogollo/biblion/feature/studydocs/ui/editor/PaginatedPaperSheet.kt`
- `app/src/main/java/com/cristiancogollo/biblion/feature/studydocs/ui/editor/StudyDocEditorScreen.kt`
- `app/src/main/java/com/cristiancogollo/biblion/feature/studydocs/ui/editor/blocks/StyledTextEditor.kt`
- `app/src/main/java/com/cristiancogollo/biblion/feature/studydocs/ui/editor/blocks/TextBlockEditors.kt`
- `app/src/main/java/com/cristiancogollo/biblion/feature/studydocs/ui/editor/blocks/MoreBlockEditors.kt`

## Verificacion realizada

- `:app:compileDebugKotlin` ejecutado con exito.

## Pruebas manuales sugeridas

1. Abrir `Mis Documentos`.
2. Crear o abrir un documento de estudio.
3. Probar escribir en:
   - Parrafo
   - Encabezado
   - Nota
   - Reflexion
   - Cita
   - Versiculo
4. Verificar que:
   - el placeholder solo aparezca al enfocar un bloque vacio,
   - el texto se sienta mas compacto,
   - el salto entre paginas siga siendo estable,
   - el zoom no rompa la lectura.

## Observacion

La paginacion sigue siendo una estimacion visual, no una maquetacion tipografica perfecta por linea. Si luego se quiere mayor precision, el siguiente paso es medir altura real por bloque con mas fidelidad y afinar listas/tablas.
