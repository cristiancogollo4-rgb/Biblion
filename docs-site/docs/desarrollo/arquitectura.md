---
title: Cómo seguir el código
description: Método práctico para rastrear una acción desde Compose hasta los datos.
---

# Cómo seguir el código

## Las cinco preguntas de cualquier recorrido

Cuando algo funciona mal o quieres ampliarlo, busca estas respuestas en este orden:

1. **Entrada:** ¿qué pantalla, gesto o ruta inicia la acción?
2. **Estado:** ¿qué variable de Compose o `StateFlow` cambia?
3. **Regla:** ¿qué función decide si se permite y cómo se transforma el dato?
4. **Persistencia:** ¿se lee o escribe en Room, preferencias locales, Firestore o Worker?
5. **Salida:** ¿qué ve el usuario si funciona, no hay datos o falla?

```text
Interacción → estado/coordinación → regla de negocio → fuente de datos
                  ↓                                        │
          resultado observable ←──────────────────────────┘
                  ↓
              interfaz
```

### Una distinción importante

La arquitectura busca separar UI y datos, pero **no todas las pantallas usan un ViewModel propio**. `SearchScreen` coordina su búsqueda con estado Compose, un `MutableStateFlow` y corrutinas; `ReaderContent` también conserva estado de lectura en Compose. En cambio, `BibiViewModel` y `StudyDocViewModel` sí coordinan esos flujos. Al rastrear un error, mira el código real y no supongas una capa intermedia que no existe.

## Cómo leer los archivos

| Pieza | Qué debes buscar | Ejemplo real |
| --- | --- | --- |
| UI Compose | Eventos y estado visible | `feature/reader/ReaderScreen.kt` contiene `ReaderContent` y `VerseItem` |
| Coordinador | Cancelación, validación y actualización del estado | `feature/bibi/ui/BibiViewModel.kt` contiene `sendQuestion()` |
| Motor | Transformación independiente de la pantalla | `feature/studydocs/engine/StudyDocEngine.kt` contiene `apply()` |
| Repositorio | Lectura, guardado, caché y origen de datos | `feature/bible/data/BibleRepository.kt` contiene `getChapter()` |
| Fuente | Consulta concreta a Room o llamada remota | `feature/bible/data/BibleDatabase.kt` declara `BibleDao` |

Estos caminos son relativos a `app/src/main/java/com/cristiancogollo/biblion/`. Para abrir un archivo rápido en Android Studio, usa **Search Everywhere** dos veces `Shift`, escribe el nombre de clase o función y entra en su definición. En una terminal, `rg -n "getChapter" app/src/main/java` encuentra usos cuando el nombre ya se conoce.

## Ejemplo: seguir «Guardar» en el editor

```text
Botón Guardar en StudyDocEditorScreen
  → StudyDocViewModel.saveNow(title, tags)
  → StudyDocViewModel.saveNow()
  → StudyDocRepository.save(doc)
  → StudyDocDao inserta/actualiza en study_docs.db
  → FirestoreSyncManager.requestStudiesSync()
```

`saveNow(title, tags)` rechaza un título vacío. `save()` marca el documento como visible en «Mis enseñanzas». El guardado remoto es posterior y depende de una sesión de usuario; guardar localmente no equivale a comprobar que otro dispositivo ya recibió el cambio. El [recorrido del editor](/desarrollo/estudio) y [datos y sincronización](/desarrollo/datos) muestran ambas partes.

## Regla para cambiar una función sin perderte

Antes de editar, anota su **entrada**, su **resultado**, quién la llama y qué prueba protege esa regla. Cambia la capa que decide el comportamiento: una validación del documento va al motor o repositorio; una etiqueta visual va a la UI; una consulta SQL va al DAO. Después sigue el recorrido inverso hasta la pantalla y comprueba que los estados de carga, vacío y error sigan claros.
