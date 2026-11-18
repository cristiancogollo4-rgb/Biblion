---
title: Datos y sincronización
description: Propiedad de datos, guardado local y alcance de Firestore en fase 1.
---

# Datos y sincronización

## Quién es dueño de cada dato

| Dato | Base primaria en fase 1 | Regla clave |
| --- | --- | --- |
| Texto bíblico | SQLite/Room preempaquetado | Leer localmente; no solicitar cada versículo a Firestore |
| Diccionario y temas | Bases locales incluidas en la app | Consultas sin conexión |
| Historial de búsqueda y de Bibi | Room | Persistencia local |
| Borradores de enseñanza | `study_docs.db` | `isPublished=false`; no subir a Firestore |
| Enseñanzas guardadas | Room + Firestore de la cuenta | Publicación privada en fase 1, no repositorio social |
| Perfil y preferencias sincronizables | Almacenamiento local + Firebase | Asociar cambios al usuario autenticado |
| Foto de perfil | Firebase Storage | No guardar binarios en Firestore |

## De «Guardar» a otro dispositivo

```text
StudyDocViewModel.saveNow()
  → StudyDocRepository.save()
  → StudyDocDao guarda en Room con isDirty=true e isPublished=true
  → FirestoreSyncManager.requestStudiesSync()
  → espera la primera lectura remota de la sesión
  → pushStudies() escribe users/{uid}/studies/{remoteId}
  → markSyncedIfUnchanged() limpia la marca solo si el documento no cambió de nuevo
```

`saveDraft()` también persiste en Room, pero **un borrador nunca publicado no entra** en la consulta `StudyDocDao.getDirtyForSync()` porque exige `is_published = 1`. En un documento ya publicado, el autosave conserva `isPublished=true` y las ediciones sí quedan pendientes de sincronización.

## Inicio y cierre de una sesión de sync

`AppNavigation` observa el `uid` autenticado: llama a `FirestoreSyncManager.start(user)` al entrar y a `stop()` al salir. `start()` crea una sesión, agrega listeners y aplica cambios remotos. El listener de estudios espera una **primera respuesta del servidor** antes del push inicial para no sobrescribir datos de otro dispositivo con una copia local antigua. Después, `requestStudiesSync()` agrupa solicitudes cercanas, reintenta y envía solo registros pendientes.

`stop()` cancela jobs y elimina los listeners. `applyRemoteStudies()` consulta `StudyDocSyncPolicy.resolve()` para decidir entre aplicar remoto, mantener local, eliminar, ignorar un propietario ajeno o preservar una copia ante conflicto. Las eliminaciones de documentos publicados viajan como marcas de borrado; descartar un borrador no publicado es una operación local.

| Si ocurre esto… | Revisa primero… |
| --- | --- |
| Se guarda aquí, pero no aparece en otro dispositivo | `StudyDocDao.getDirtyForSync()`, `requestStudiesSync()` y autenticación |
| Aparece una copia antigua al volver a entrar | `applyRemoteStudies()` y `StudyDocSyncPolicy.resolve()` |
| Siguen llegando eventos de la cuenta anterior | `FirestoreSyncManager.stop()` y la transición de `uid` |
| Un borrador aparece en «Mis enseñanzas» | `StudyDocRepository.saveDraft()` y filtro de la lista |

Una comprobación de sincronización requiere **la misma cuenta en dos dispositivos** o un emulador adicional. La prueba local puede confirmar política y DAO, pero no sustituye ese recorrido extremo a extremo.

## Límites de seguridad

- No incluyas secretos del proveedor IA en el APK o en archivos versionados. El Worker conserva las credenciales.
- Nunca atribuyas datos de un usuario a otro `uid` durante el pull o el push.
- No subas borradores por cambiar el flujo de guardado.
- No incorpores datos de la fase 2 al esquema de fase 1 sin definir propiedad, permisos y migración.

Archivos clave: `feature/studydocs/data/StudyDocRepository.kt`, `StudyDocDao.kt`, `StudyDocSyncPolicy.kt` y `feature/sync/data/FirestoreSyncManager.kt`. La prueba de reglas de conflicto es `app/src/test/java/com/cristiancogollo/biblion/feature/studydocs/data/StudyDocSyncPolicyTest.kt`.

::: warning No confundir «publicado» con «público»
`isPublished` hace visible una enseñanza en la biblioteca personal y habilita su sincronización. No concede acceso a otros usuarios; la red de publicaciones de fase 2 requiere reglas y rutas independientes.
:::
