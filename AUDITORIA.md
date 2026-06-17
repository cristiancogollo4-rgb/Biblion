# Auditoria de Cambios

Fecha: 2026-06-17

## Alcance

Esta entrega consolida la correccion de bugs del modo estudio, la optimizacion de la barra de herramientas del editor y la actualizacion de la documentacion del proyecto.

## Cambios principales

### Correccion de bugs en modo estudio

- **Enter en encabezados**: normalizeStudyFlow eliminaba los parrafos vacios creados al presionar Enter en bloques con rol "heading". Corregido agregando "heading" al conjunto de roles preservados en StudyDocumentEngine.kt:435.

- **Race condition en LaunchedEffect**: LaunchedEffect(block.text) sobrescribia fieldValue con texto desactualizado del ViewModel cuando el usuario escribia rapido. Corregido con delay(300) antes de sincronizar en StudyEditorScreen.kt:830.

- **Duplicacion de citas en lectura**: addCitation() agregaba un bloque Citation inmediatamente y guardaba una solicitud pendiente que luego se convertia en QuotedVerse. Corregido eliminando la escritura inmediata del bloque Citation en StudyViewModel.kt:643.

- **Filtro de citas duplicadas en lectura**: toReadItems() en StudyReadScreen.kt:209 ahora filtra bloques Citation cuando existe un QuotedVerse con la misma referencia.

### Optimizacion de barra de herramientas

- Eliminadas herramientas de alineacion de texto (izquierda, centrar, derecha).
- Eliminadas herramientas de transformacion de mayusculas/minusculas.
- Barra reducida de 12 a 9 botones en la fila superior.

### UI de colores

- Burbuja de colores en Popup con offset de (-200).dp.
- Sombra eliminada, tonalElevation = 4.dp para fondo visible.
- focusable = false para evitar interferencia con IME.

### Documentacion

- README.md y AGENTS.md actualizados con herramientas eliminadas y nuevo estado.
- AUDITORIA.md actualizada.

## Riesgos controlados

- Ensenanzas existentes con Citation y QuotedVerse duplicados se corrigen en lectura sin modificar datos persistidos.
- La eliminacion de la escritura inmediata de Citation no afecta el flujo de citas pendientes.

## Evidencia de validacion

Comandos ejecutados:
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:testDebugUnitTest
