---
title: Inicio, navegación y tutorial
description: Qué ocurre al abrir Biblion, cambiar de pantalla, iniciar sesión o seguir la guía.
---

# Inicio, navegación y tutorial

## Al abrir la app

`AppNavigation()` crea el `NavController` y declara el `NavHost` con `Home` como destino inicial. Cada destino toma los argumentos de su ruta y construye la pantalla correspondiente. Por ejemplo, la ruta del lector decodifica libro, capítulo y versículo antes de llamar a `ReaderScreen()`.

```text
AppNavigation → NavHost → ruta ReaderWithBook
  → argumentos bookName / chapter / verse
  → ReaderScreen → ReaderContent
```

**Archivo de entrada:** `app/AppNavigation.kt`. Las rutas principales se declaran en `navigation/NavigationRoutes.kt` y las del editor en `feature/studydocs/ui/StudyDocRoutes.kt`.

## Funciones y decisiones clave

| Función o efecto | Qué recibe / observa | Qué hace |
| --- | --- | --- |
| `AppNavigation()` | Tema, URI importada y estado de autenticación | Construye destinos, diálogos y eventos compartidos |
| `NavHost` | Ruta y argumentos actuales | Muestra la pantalla correspondiente; no guarda contenido bíblico |
| Efecto sobre `authState.currentUser?.uid` | Usuario autenticado o salida de sesión | Llama a `FirestoreSyncManager.start(user)` o `stop()` |
| `startGuidedTutorial()` | ID del recorrido | Guarda el paso inicial y muestra la guía |
| `advanceGuidedTutorial()` | Paso actual | Persiste el siguiente paso o marca el recorrido completo |
| `handleGuidedTutorialTargetAction()` | Clave del elemento tocado | Avanza solo si coincide con el objetivo exigido |

Al volver la app al primer plano, `AppNavigation` llama a `FirestoreSyncManager.refreshNow()`. Eso intenta recuperar una sesión de sincronización o pide revisar enseñanzas pendientes; **no equivale a forzar una descarga total en cada pantalla**.

## Ejemplo: tocar un libro durante el tutorial

1. La pantalla de libros comunica la acción con una clave de objetivo.
2. `handleGuidedTutorialTargetAction()` compara esa clave con el paso activo.
3. `advanceGuidedTutorial()` actualiza `GuidedTutorialProgress` y lo persiste en `AppPreferencesSyncStore`.
4. La navegación abre la ruta del lector con el libro elegido.
5. El siguiente paso del tutorial señala el selector de capítulo.

Si el foco visual está fuera de lugar, revisa tanto el **elemento que informa sus coordenadas** como el **paso que lo espera** en `feature/onboarding/GuidedTutorial.kt`; no basta con mover la burbuja.

## Dónde cambiar y cómo comprobar

| Necesidad | Lugar principal | Comprobación |
| --- | --- | --- |
| Añadir o cambiar una ruta | `navigation/NavigationRoutes.kt`, `feature/studydocs/ui/StudyDocRoutes.kt` y `app/AppNavigation.kt` | Entrar desde varias secciones y volver atrás |
| Cambiar un paso del tutorial | `feature/onboarding/GuidedTutorial.kt` y `AppNavigation.kt` | Reiniciar desde Perfil y recorrerlo completo |
| Corregir el destino de un resultado de búsqueda | `feature/search/ui/SearchScreen.kt` | Abrir un versículo y comprobar libro/capítulo/versículo |
| Investigar sincronización tras iniciar sesión | `AppNavigation.kt` y `feature/sync/data/FirestoreSyncManager.kt` | Iniciar/cerrar sesión y revisar otro dispositivo |

Las pruebas de flujo del tutorial están en `app/src/test/java/com/cristiancogollo/biblion/GuidedTutorialFlowTest.kt`. Para el comportamiento visible, usa además el [recorrido manual](/desarrollo/pruebas).
