---
title: Cuenta, perfil y preferencias
description: De la autenticación al perfil y al guardado de preferencias de lectura.
---

# Cuenta, perfil y preferencias

## La sesión decide el alcance de los datos

`AppNavigation()` observa `AuthViewModel.state`. Cuando cambia el usuario, actualiza `ProfileViewModel.setCurrentUser()` y activa o detiene `FirestoreSyncManager` para ese `uid`. La cuenta no es necesaria para leer el texto bíblico local, pero sí para llevar datos sincronizables a otro dispositivo.

```text
Iniciar sesión → AuthViewModel.process(...) / signInWithGoogleIdToken(...)
  → AuthViewModel.state cambia
  → AppNavigation observa el uid
  → ProfileViewModel.setCurrentUser(user)
  → FirestoreSyncManager.start(user)
```

Al cerrar sesión, el sincronizador llama a `stop()` y elimina listeners de la cuenta anterior. Si ves datos mezclados entre cuentas, investiga primero esa transición y los filtros por propietario, no la pantalla que los muestra.

## Qué función cambia qué dato

| Acción | Punto de entrada | Almacenamiento / efecto |
| --- | --- | --- |
| Iniciar sesión o registrarse | `AuthViewModel.process()` | Firebase Auth y estado observable |
| Completar perfil | `ProfileViewModel.saveProfile()` | `UserProfileRepository` / Firestore |
| Cambiar alias, nombres o biografía | Métodos `update…()` de `ProfileViewModel` | Estado de formulario hasta guardar |
| Subir foto | `ProfileViewModel.uploadProfilePhoto()` | `UserProfileRepository` y Firebase Storage |
| Cambiar versión bíblica | `BibleRepository.setSelectedVersionKey()` | Preferencia local; el lector vuelve a cargar la versión |
| Cambiar tamaño de texto o recordar lectura | `AppPreferencesSyncStore` | Preferencias locales con alcance de sync según el campo |

Para el modo de lectura, `ReaderPreferencesStore` conserva opciones como la selección secundaria del lector paralelo. No confundas esas preferencias con el contenido de la base bíblica preempaquetada: cambiar un ajuste no altera versículos ni tablas de Room.

## Recorrido de una foto

La UI pide seleccionar una imagen; `ProfileViewModel.uploadProfilePhoto()` coordina el estado de carga y el repositorio sube el archivo a Storage. El perfil conserva la referencia necesaria para volver a mostrarla. Si una imagen tarda o no aparece, comprueba el estado de carga, la sesión activa, el resultado del repositorio y la URL almacenada; no repitas la subida desde una recomposición de Compose.

## Archivos y validación

Abre `feature/auth/presentation/AuthViewModel.kt`, `feature/profile/presentation/ProfileViewModel.kt`, `feature/profile/data/UserProfileRepository.kt`, `feature/sync/data/AppPreferencesSyncStore.kt` y `app/AppNavigation.kt`.

Prueba el perfil sin sesión, inicio con Google, edición y reapertura, cierre de sesión y cambio de cuenta. Para preferencias, cambia versión y tamaño de letra, cierra y abre la app y verifica si persisten. Para datos remotos, repite la prueba en otro dispositivo con la misma cuenta.
