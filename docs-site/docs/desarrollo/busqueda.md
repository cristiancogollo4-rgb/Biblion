---
title: Búsqueda y exploración
description: Cómo se ejecuta la búsqueda bíblica, se aplican filtros y se consultan temas y diccionario.
---

# Búsqueda y exploración

## Escribir en el buscador bíblico

`SearchScreen()` conserva el texto, testamento, libro y resultado visible en estado Compose. Cada cambio crea un `VerseSearchRequest`. Un `MutableStateFlow` recibe la solicitud; `debounce(400)` espera una pausa y `collectLatest` cancela la búsqueda anterior si llega otra.

```text
Texto o filtro → VerseSearchRequest(query, testament, bookName)
  → searchRequestFlow → debounce → executeSearch(request)
  ├─ BibleRepository.searchVerses(..., request.toFilter())
  └─ TopicEngine.searchTopics(...)
  → SearchUiState.Success / Empty / Error
```

`executeSearch()` inicia la consulta de versículos y la de temas en paralelo. El repositorio bíblico usa la **versión seleccionada**, normaliza el libro y aplica el rango de libros del testamento antes de consultar `BibleDao.searchVersesFiltered()`. `TopicEngine` consulta su base local. No hace falta Firestore para mostrar estos resultados.

## Funciones que conviene conocer

| Función | Entrada | Resultado o efecto |
| --- | --- | --- |
| `VerseSearchRequest.toFilter()` | Testamento y libro seleccionados | Construye el filtro para la consulta bíblica |
| `executeSearch()` | Solicitud completa | Publica `Loading`, luego resultados, vacío o error |
| `setTestament()` / `setBook()` | Filtro elegido | Cambia el filtro y repite una búsqueda activa |
| `BibleRepository.searchVerses()` | Texto y `BibleSearchFilter` | Busca versículos en Room para la versión actual |
| `SearchHistoryRepository.recordQuery()` | Búsqueda válida | Conserva un término reciente en Room |
| `openVerse()` | Libro, capítulo y versículo del resultado | Navega al lector con la referencia exacta |

La pantalla **no acepta un resultado tardío** si ya cambiaron texto o filtros. Cuando la consulta tiene menos de dos caracteres, muestra el estado inicial. El historial es local y puede borrarse con `SearchHistoryRepository.clearAll()`.

## Buscar en el diccionario no es lo mismo

La ruta de diccionario usa `DictionarySearchContent()` y `DictionaryRepository.searchEntries()`. También espera una pausa mientras se escribe, pero consulta la base del diccionario, no la tabla de versículos. Si un fallo solo afecta definiciones, empieza en `feature/search/ui/DictionarySearchContent.kt` y `feature/dictionary/data/`; si afecta referencias bíblicas, empieza en `SearchScreen.kt` y `BibleRepository.kt`.

## Dónde mirar y cómo probar

Los archivos de entrada son `feature/search/ui/SearchScreen.kt`, `feature/bible/data/BibleRepository.kt`, `feature/bibi/engine/TopicEngine.kt` y `feature/search/data/SearchHistoryRepository.kt`.

Prueba texto de 1 y 2 caracteres, cambios rápidos de consulta, filtro por testamento/libro, término sin resultados, apertura de un versículo y borrado del historial. `VerseSearchRequestTest`, `SearchHistoryTest` y `ScreenSearchRouteTest` cubren parte de las reglas; la interacción completa requiere el [recorrido manual](/desarrollo/pruebas).
