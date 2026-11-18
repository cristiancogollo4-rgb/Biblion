---
title: Bibi
description: Recorrido de una pregunta, decisiones local/remoto, contexto bíblico y límites doctrinales.
---

# Cómo responde Bibi

## De la pregunta a la respuesta

`BibiViewModel.sendQuestion()` añade el mensaje del usuario, activa el estado de carga y resuelve referencias explícitas o el pasaje seleccionado. Después llama a `BibiOrchestrator.answer()`. El resultado se normaliza para mostrarlo, se añade al chat y se conserva el historial de la sesión.

```text
Pregunta en BibiChatPanel
  → BibiViewModel.sendQuestion()
  → BibiReferenceContextResolver.resolve()
  → BibiOrchestrator.answer()
      ├─ reglas de atribución y perspectiva doctrinal
      ├─ KnowledgeEngine.answer() si hay respuesta local suficiente
      └─ WorkerClient.askRich() cuando hace falta respuesta remota
  → validación de referencias → mensaje visible
```

`BibiContext.Reader` puede incluir pasajes seleccionados del lector; el contexto de estudio contiene información del documento. Una pregunta con una referencia explícita se resuelve antes de enviarla al orquestador. El nombre visible sirve para el saludo local, pero no debe incluirse en el payload del Worker por defecto.

## Qué decide cada función

| Función | Decisión |
| --- | --- |
| `KnowledgeEngine.detectIntent()` | Clasifica la pregunta: definición, personaje, lugar, pasaje, relación, idioma original, etc. |
| `InstitutionalDoctrinePolicy.needsOfficialSource()` | Evita atribuir a una institución una postura no verificada |
| `DoctrinalPerspectivePolicy.analyze()` | Detecta debate doctrinal y perspectiva expresada en la conversación |
| `KnowledgeEngine.answer()` | Intenta responder con diccionario, temas o referencias locales |
| `WorkerClient.buildRequest()` | Forma el contexto permitido para la solicitud remota |
| `BibiReferenceValidator.validate()` | Comprueba que las referencias remotas existen en la versión bíblica indicada |

La regla es **local primero**, pero no significa «Worker solo para una intención»: una respuesta local ausente o de baja confianza puede requerirlo. `forceRemote` también salta la respuesta local. Para una pregunta doctrinal debatida sin perspectiva del usuario, el orquestador devuelve una respuesta prudente; cuando existe una perspectiva, la atribuye sin presentarla como verdad absoluta para todos.

::: warning Diferencia importante
Preguntar «¿es necesario el bautismo para salvación?» no es lo mismo que preguntar «según la IPUC, ¿es necesario…?». La segunda atribuye una postura institucional y requiere una fuente verificada; la primera puede tratarse como pregunta bíblica/doctrinal general. Al cambiar estas reglas, comprueba ambos casos.
:::

## Qué ocurre si falla la red

Si el Worker no responde, `BibiOrchestrator.answer()` usa una respuesta local disponible o un mensaje de respaldo. Una respuesta remota tampoco se muestra sin más: se revisan atribuciones institucionales, afirmaciones doctrinales inseguras y referencias. La UI no debe convertir una referencia no validada en un pasaje confirmado.

## Archivos y comprobación

| Responsabilidad | Archivo |
| --- | --- |
| Estado y envío de mensajes | `feature/bibi/ui/BibiViewModel.kt` |
| Decisión local/remota | `feature/bibi/domain/BibiOrchestrator.kt` |
| Intenciones y conocimiento local | `feature/bibi/engine/KnowledgeEngine.kt` |
| Contexto de una referencia | `feature/bibi/domain/BibiReferenceContextResolver.kt` |
| Solicitud al Worker | `feature/bibi/ui/WorkerClient.kt` |
| Servicio remoto | `workers/bibi/src/index.js` |

Pruebas de referencia: `KnowledgeEngineTest`, `BibiDoctrinalRoutingTest`, `BibiReferenceContextResolverTest` y `BibiWorkerRequestTest` en `app/src/test/java/`. Valida además preguntas con y sin versículo seleccionado, una pregunta general, una atribución denominacional y el caso sin conexión.
