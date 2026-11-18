---
title: Pruebas y validación
description: Comandos y recorridos manuales para comprobar la fase 1.
---

# Pruebas y validación

## Comprobaciones locales

Ejecuta los comandos desde la raíz del repositorio Android en PowerShell. Mantén la compilación y los filtros de pruebas en comandos separados:

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
```

Para una prueba concreta:

```powershell
.\gradlew.bat :app:testDebugUnitTest `
    --tests "*PaginationEngineTest" `
    --no-daemon
```

`--tests` pertenece a la tarea de pruebas; no lo pases junto a `assembleDebug` en el mismo comando.

## Relaciona el cambio con su prueba

| Si modificaste… | Empieza con… | Comprueba manualmente… |
| --- | --- | --- |
| Carga o caché de capítulos | `BibleRepositoryConcurrencyTest` | Cambio rápido de libro, capítulo y versión |
| Resaltados | `HighlightsCacheTest` | Colores al avanzar y retroceder en lectura continua |
| Intenciones o reglas de Bibi | `KnowledgeEngineTest`, `BibiDoctrinalRoutingTest` | Pregunta general y atribución denominacional |
| Bloques, listas o paginación | `StudyDocEngineListTest`, `PaginationEngineTest` | Texto, estilos y cursor tras editar varias hojas |
| Publicación o conflictos | `StudyDocSaveRulesTest`, `StudyDocSyncPolicyTest` | Borrador local y documento visible en otro dispositivo |

Ejemplo de filtro:

```powershell
.\gradlew.bat :app:testDebugUnitTest `
    --tests "*KnowledgeEngineTest" `
    --tests "*BibiDoctrinalRoutingTest" `
    --no-daemon
```

Una prueba de motor verifica una regla aislada; no demuestra que la pantalla, la red o Firestore se comporten bien. Usa la página del recorrido correspondiente para ver qué otras piezas intervienen.

## Pruebas que requieren dispositivo

Las pruebas unitarias y la compilación pueden ejecutarse sin teléfono. La validación visual, los gestos, el teclado, la lectura paralela y las pruebas instrumentadas o de rendimiento sí requieren un dispositivo o emulador configurado.

## Recorrido manual mínimo

| Área | Comprueba |
| --- | --- |
| Tutorial | Reinicio desde Perfil, foco de libros/capítulos/versículos al desplazar y finalización |
| Lectura | Cambio de libro, versión y capítulo; avance y retroceso en lectura continua |
| Selección | Pulsación prolongada inicial, toques para selección múltiple, resaltados persistentes |
| Paralelo | Navegación independiente en ambos paneles y regreso al modo normal |
| Buscar | Filtros, resultados, temas, diccionario e historial reciente |
| Bibi | Contexto del versículo seleccionado, respuesta local, aviso de límites y consulta remota |
| Estudio | Borrador, formato, listas, paginación, zoom, guardado y reapertura |
| Sincronización | Misma cuenta en dos dispositivos: documento guardado visible; borrador solo local |

Registra para cada fallo: dispositivo, versión de Android, pantalla, versión bíblica, datos concretos y pasos para reproducirlo. Una compilación exitosa no sustituye este recorrido.
