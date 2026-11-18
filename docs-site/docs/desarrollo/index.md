---
title: Mapa técnico
description: Una ruta sencilla para entender qué hace cada parte de Biblion y dónde modificarla.
---

# Entender el código de Biblion

Esta guía parte de acciones reales de la app. En cada recorrido verás **qué toca el usuario**, **qué función responde**, **qué dato cambia** y **cómo comprobarlo**. Así puedes seguir una función sin tener que leer todo el proyecto de una vez.

## Elige el recorrido que necesitas

- [Cómo seguir el código](/desarrollo/arquitectura): pantallas, estado, reglas y datos.
- [Inicio, navegación y tutorial](/desarrollo/navegacion): apertura, rutas y pasos guiados.
- [Cuenta, perfil y preferencias](/desarrollo/cuenta): sesión, foto, versión y ajustes.
- [Lectura bíblica](/desarrollo/lectura): capítulos, lectura continua, paralelo y resaltados.
- [Búsqueda y exploración](/desarrollo/busqueda): filtros, temas, diccionario e historial.
- [Bibi](/desarrollo/bibi): clasificación de preguntas y respuesta local o remota.
- [Editor de enseñanzas](/desarrollo/estudio): bloques, estilos, páginas y guardado.
- [Datos y sincronización](/desarrollo/datos): qué queda local y qué viaja a la cuenta.

## Un ejemplo en 30 segundos

Si quieres cambiar cómo se abre un capítulo, sigue esta cadena:

```text
Toque en la interfaz
  → loadChapter() dentro de ReaderContent
  → BibleRepository.getChapter(...)
  → BibleDao.getChapterContent(...)
  → chapterContent cambia
  → Compose vuelve a dibujar los versículos
```

La [guía de lectura](/desarrollo/lectura) explica cada salto, el manejo de errores y el caso de lectura continua.

## Qué significa «cada función» en esta guía

Documentamos las **funciones que gobiernan un comportamiento** y sus llamadas importantes, no cada helper de formato o cada componente visual. Los nombres de métodos, archivos y pruebas se tomaron del código de esta rama. Usa la búsqueda del sitio para localizar un método; si cambias su contrato, actualiza el recorrido correspondiente.

Los archivos Kotlin mencionados viven bajo `app/src/main/java/com/cristiancogollo/biblion/`, salvo cuando se indica otra raíz. La guía corresponde a **fase 1**, basada en `test`; las funciones sociales del repositorio público pertenecen a fase 2. Para detalles de contrato del editor, consulta también `STUDY_DOCS_V2.md` en el repositorio.
