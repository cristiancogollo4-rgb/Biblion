# AGENTS.md

Este documento define los estándares operativos para contribuciones automatizadas y manuales en **Biblion**.
Su objetivo es mantener coherencia con el estado actual del repositorio (Kotlin + Compose + MVVM + Room).

## 1) Contexto del proyecto
- Plataforma: Android
- Lenguaje: Kotlin
- UI: Jetpack Compose + Material 3
- Persistencia: Room
- Arquitectura actual: enfoque por capas con patrón MVVM
- Módulo principal: `app/`

## 2) Principios de cambio
1. **Cambios pequeños y enfocados**: una tarea por PR siempre que sea posible.
2. **No romper navegación ni flujos existentes** (`Home`, `Books`, `Reader`, `Search`, `Study`).
3. **Priorizar legibilidad** sobre micro-optimizaciones prematuras.
4. **Mantener compatibilidad** con:
   - `minSdk = 26`
   - `Java/Kotlin jvmTarget = 11`

## 3) Estándares de código Kotlin
- Seguir convenciones oficiales de Kotlin (nombres descriptivos, `camelCase`, `PascalCase`).
- Evitar abreviaturas ambiguas en variables y funciones.
- Preferir inmutabilidad (`val`) y estado explícito.
- Limitar funciones extensas; extraer bloques reutilizables cuando mejore claridad.
- No introducir comentarios redundantes; documentar decisiones no obvias.
- Mantener el paquete base existente: `com.cristiancogollo.biblion`.

## 4) Estándares Compose/UI
- Cada pantalla debe mantener responsabilidad clara (render + eventos de UI).
- Evitar lógica de negocio compleja dentro de composables.
- Usar componentes compartidos existentes cuando aplique (ej. `BiblionComponents`).
- Respetar tema global (`BiblionTheme`) y `MaterialTheme` para colores/tipografía.
- Textos visibles al usuario deben ir en `res/values/strings.xml` (y `values-es` cuando aplique), evitar hardcode.
- Mantener accesibilidad básica: `contentDescription` cuando sea relevante.

## 5) Estado y ViewModel
- La lógica de negocio y persistencia vive fuera de UI (ViewModel/Repository/DAO).
- Para nuevas intenciones de usuario, extender de forma consistente los modelos tipo intent/state existentes (ej. `StudyIntent`, `StudyUiState`).
- Evitar efectos secundarios ocultos; preferir flujos explícitos con corrutinas/Flow.

## 6) Datos, repositorios y Room
- Cambios de esquema de Room deben ser compatibles y claramente justificados.
- Mantener separación entre entidades de persistencia y estado de UI cuando aplique.
- Si se agrega caché o acceso a assets bíblicos, seguir patrón actual de `BibleRepository` y caches dedicados.

## 7) Navegación
- Registrar rutas nuevas en archivos de navegación actuales (`NavigationRoutes`, `AppNavigation`, etc.).
- Evitar duplicidad de rutas o navegación que rompa `launchSingleTop`/`popUpTo` usado hoy.

## 8) Pruebas y validación
- Agregar o ajustar pruebas unitarias cuando se modifique lógica de negocio.
- Usar pruebas de UI/instrumentación solo cuando el cambio lo requiera.
- Antes de proponer merge, validar al menos:
  - compilación del módulo afectado
  - pruebas relacionadas al cambio

## 9) Dependencias y build
- No duplicar librerías ya administradas en `gradle/libs.versions.toml`.
- Nuevas dependencias deben declararse con versión centralizada en el catálogo.
- Evitar actualizar versiones masivamente sin necesidad del cambio solicitado.

## 10) Commits y PR
- Commits en imperativo y alcance claro.
  - Ejemplo: `docs: agrega AGENTS.md con estándares de desarrollo`
- PR debe incluir:
  - problema
  - solución aplicada
  - riesgos/impacto
  - evidencia (tests/capturas) cuando aplique

## 11) Qué evitar
- Refactors globales no solicitados.
- Cambios de estilo mezclados con cambios funcionales.
- Introducir deuda técnica marcada como TODO sin contexto o plan.
- Romper compatibilidad de datos locales sin estrategia de migración.

## 12) Regla de consistencia
Si existe conflicto entre este documento y una instrucción explícita del solicitante para una tarea puntual, prevalece la instrucción explícita para esa tarea, manteniendo el resto de estándares.
