# Auditoria de Cambios

Fecha: 2026-06-12

## Alcance

Esta entrega consolida la evolucion reciente de Biblion en lectura biblica, busqueda, perfil, Bibi y modo estudio. El foco principal fue reducir fragilidad del modo estudio, mejorar rendimiento del editor visual y conservar compatibilidad con el documento estructurado actual.

## Cambios principales

- Biblia local migrada a SQLite/Room con base preempaquetada en `app/src/main/assets/databases/bible_content.db`.
- Repositorio biblico ajustado para consultar desde la base local y mantener cache/concurrencia.
- Busqueda de versiculos con filtros por libro y testamento.
- Lector mejorado con deslizamiento entre capitulos, preservacion de posicion al cambiar version y navegacion desde versiculo diario.
- Perfil reorganizado con identidad, metricas, avatar/foto y edicion agrupada.
- Bibi integrada con saludo local por nombre sin enviar ese dato al Worker.
- Modo estudio reforzado con `StudyDocumentEngine` como motor puro de documento.
- Editor de estudio optimizado con `LazyColumn`, claves estables y flujo visual de columnas.
- Bloques dentro de columnas, como citas, notas y reflexiones, integrados por posicion de cursor en lugar de agregarse al final.
- Lectura, texto compartido y PDF respetan la estructura vertical de ensenanzas y el orden de bloques en columnas.
- Importacion/exportacion de ensenanzas en formato `.biblion`.
- Worker de Bibi y evaluador de modelos actualizados para reglas de dominio biblico y respuestas defensivas.

## Riesgos controlados

- Los estudios existentes siguen siendo compatibles: los bloques antiguos de columna sin posicion se normalizan al final, conservando su comportamiento previo.
- El formato `.biblion` mantiene el documento serializado actual y agrega compatibilidad hacia adelante mediante campos con valores por defecto.
- La base SQLite de Biblia queda como asset local; no contiene secretos.
- Los secretos de IA siguen fuera del APK y deben vivir en Cloudflare Worker secrets.

## Evidencia de validacion

Comandos ejecutados correctamente:

```powershell
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:testDebugUnitTest
git diff --check
```

Observacion: `git diff --check` solo reporto avisos normales de CRLF en Windows, sin errores de espacios.

## Pendientes recomendados

- Probar manualmente en dispositivo real el editor de columnas con teclado fisico y teclado movil.
- Revisar reglas de Firebase Storage para confirmar subida de foto de perfil en `profile_photos/{uid}/avatar.jpg`.
- Evaluar una futura migracion de metadatos derivados de estudios a columnas indexables para busqueda interna avanzada.
