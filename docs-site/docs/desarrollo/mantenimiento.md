---
title: Mantenimiento
description: Reglas para cambiar la fase 1 y mantener estas guías actualizadas.
---

# Mantenimiento

## Antes de modificar una función

1. Identifica la pantalla, el ViewModel, el repositorio y las pruebas que la cubren.
2. Revisa si el dato es local, remoto o mixto; documenta quién tiene autoridad ante un conflicto.
3. Mantén el cambio pequeño y conserva las migraciones de Room y los datos existentes.
4. Comprueba teléfono y tablet cuando la interfaz depende del tamaño o la orientación.
5. Actualiza la guía de uso si cambia el comportamiento visible y la guía técnica si cambia el contrato interno.

## Convenciones del proyecto

- Kotlin y Compose con lógica de negocio fuera de los componentes UI.
- `Flow` y corrutinas para trabajo asíncrono; evita trabajo pesado en el hilo de composición.
- `BiblionTheme` y la paleta azul, dorado y blanco en las pantallas.
- Los cambios de rutas y navegación deben revisarse en `AppNavigation` y probarse desde todas las secciones principales.
- No publiques secretos ni incluyas archivos de configuración privada en commits.

La documentación más extensa del editor vive en `STUDY_DOCS_V2.md`; `PLAN_PAGINACION.md` es histórico y no debe usarse como descripción del motor vigente.

## Mantén clara la frontera de fases

La fase 1 ofrece lectura, consulta, asistencia y creación con biblioteca personal. La fase 2 añadirá el repositorio social. Al preparar una nueva función, pregunta si modifica la experiencia personal existente o si introduce permisos, publicaciones, interacciones y visibilidad entre usuarios.

::: tip Criterio de cierre
Una tarea está lista cuando el comportamiento funciona, las pruebas pertinentes pasan, se revisa manualmente donde corresponda y la documentación no promete algo que el código todavía no ofrece.
:::
