# Study Docs Editor Changelog

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
