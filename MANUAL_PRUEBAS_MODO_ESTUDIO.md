# Manual de Pruebas - Modo Estudio v2

## Instrucciones Generales

1. Completar o saltar el tutorial interactivo de Bibi (si es la primera vez)
2. Navegar a "Mis enseñanzas" desde el menú lateral
3. Crear o abrir un documento para probar las funcionalidades

## Fase 1 - Mejoras del Editor

### 1.1 Status Bar
**Ubicación**: Parte inferior del editor

**Pasos de prueba**:
1. Abrir cualquier documento de estudio
2. Verificar que aparece una barra en la parte inferior
3. Debe mostrar:
   - Conteo de palabras
   - Conteo de caracteres
   - Número de bloques
   - Indicador de página (si hay múltiples páginas)

**Resultado esperado**: La barra se actualiza en tiempo real al editar el documento.

### 1.2 Find & Replace
**Ubicación**: Botón de búsqueda (lupa) en la barra superior del editor

**Pasos de prueba**:
1. Abrir un documento con contenido de texto
2. Tocar el ícono de búsqueda (lupa) en la barra superior
3. Verificar que aparece la barra de búsqueda
4. Escribir una palabra común (ej: "Dios")
5. Verificar que muestra el conteo de coincidencias
6. Usar las flechas ↑↓ para navegar entre coincidencias
7. Escribir texto en "Reemplazar con"
8. Tocar "Reemplazar" para cambiar solo la coincidencia actual
9. Tocar "Todo" para cambiar todas las coincidencias
10. Tocar X para cerrar la barra

**Resultado esperado**: 
- Las coincidencias se resaltan en el documento
- El reemplazo funciona correctamente
- El conteo se actualiza después de reemplazar

### 1.3 Drag & Drop (Reordenar bloques)
**Ubicación**: Ícono de arrastre (≡) a la izquierda de cada bloque

**Pasos de prueba**:
1. Abrir un documento con múltiples bloques
2. Mantener presionado el ícono ≡ de un bloque
3. Arrastrar el bloque a otra posición
4. Soltar en la nueva posición
5. Verificar que el bloque se movió correctamente

**Resultado esperado**: 
- El bloque se reordena en la nueva posición
- El contenido se mantiene intacto
- Se puede deshacer con Ctrl+Z

## Fase 2 - Integraciones

### 2.1 Plantillas de Estudio
**Ubicación**: Botón "+" en "Mis enseñanzas"

**Pasos de prueba**:
1. Ir a "Mis enseñanzas" desde el menú
2. Tocar el botón "+" (FAB)
3. Verificar que aparece la pantalla "Seleccionar Plantilla"
4. Verificar que hay 5 plantillas:
   - Predicación Expositiva
   - Devocional
   - Estudio Bíblico
   - Clase Bíblica
   - Documento en Blanco
5. Seleccionar "Predicación Expositiva"
6. Verificar que se abre el editor con la estructura predefinida
7. Verificar que incluye:
   - Texto Base (bloque Verse)
   - Introducción
   - Puntos numerados
   - Aplicación
   - Conclusión

**Resultado esperado**: 
- La plantilla se carga con la estructura correcta
- Se puede editar el contenido
- Al guardar, se almacena correctamente

### 2.2 Bibi Overlay
**Ubicación**: Botón de libro (📖) en la barra superior del editor

**Pasos de prueba**:
1. Abrir un documento en el editor
2. Tocar el ícono de libro (📖) en la barra superior
3. Verificar que se abre un panel lateral derecho
4. Verificar que muestra:
   - Header con "Bibi - Asistente bíblico"
   - Área de mensajes
   - Campo de entrada
   - Botón de enviar
5. Escribir una pregunta (ej: "¿Qué es la fe?")
6. Tocar el botón de enviar
7. Verificar que Bibi responde
8. Tocar "Insertar como nota" en la respuesta
9. Verificar que se agrega un bloque Note al documento

**Resultado esperado**: 
- El panel se abre/cierra correctamente
- Bibi responde a las preguntas
- Las respuestas se pueden insertar como notas

### 2.3 Sync Firestore
**Ubicación**: Automático al autenticarse

**Pasos de prueba**:
1. Verificar que el usuario está autenticado (Perfil)
2. Crear un nuevo documento
3. Editar el contenido
4. Esperar 3-5 segundos
5. Cerrar la app y volver a abrir
6. Verificar que el documento aparece en "Mis enseñanzas"
7. Verificar que el contenido se mantuvo

**Resultado esperado**: 
- Los documentos se sincronizan automáticamente
- Los cambios se persisten entre sesiones

## Fase 3 - Funcionalidades Avanzadas

### 3.1 Historial de Versiones
**Ubicación**: Botón de historial (reloj) en la barra superior del editor

**Pasos de prueba**:
1. Abrir un documento existente
2. Hacer algunos cambios y guardar
3. Esperar 1 minuto (para auto-guardado de versión)
4. Tocar el ícono de historial (reloj) en la barra superior
5. Verificar que aparece la pantalla "Historial de versiones"
6. Verificar que muestra:
   - Lista de versiones con número (v1, v2, etc.)
   - Título del documento
   - Conteo de bloques y palabras
   - Fecha y hora
   - Botones de restaurar y eliminar
7. Tocar una versión para ver el visor de diferencias
8. Verificar que muestra:
   - Comparación de título
   - Comparación de bloques
   - Comparación de palabras
   - Contenido de la versión
9. Volver a la lista
10. Tocar "Restaurar" en una versión anterior
11. Confirmar la restauración
12. Verificar que se crea una nueva versión basada en la anterior

**Resultado esperado**: 
- Las versiones se guardan automáticamente cada 60 segundos
- Se pueden ver las diferencias entre versiones
- La restauración funciona correctamente

### 3.2 Import/Export Markdown
**Ubicación**: Funcionalidad programática (no hay UI directa aún)

**Pasos de prueba** (requiere acceso a código o logs):
1. Crear un documento con varios tipos de bloques
2. Usar `MarkdownExporter.export(doc)` para exportar
3. Verificar que el Markdown generado incluye:
   - Headings con # correctamente
   - Listas con - o 1.
   - Quotes con >
   - Estilos inline (**bold**, *italic*, etc.)
4. Usar `MarkdownImporter.import(markdown)` para importar
5. Verificar que los bloques se crean correctamente
6. Verificar que los estilos inline se preservan

**Resultado esperado**: 
- La exportación genera Markdown válido
- La importación crea los bloques correctos
- Los estilos se preservan en ambas direcciones

## Casos de Borde

### Documentos grandes
- Crear un documento con 50+ bloques
- Verificar que el rendimiento se mantiene aceptable
- Verificar que el status bar se actualiza correctamente

### Estilos complejos
- Aplicar múltiples estilos a un texto (bold + italic + color)
- Verificar que se preservan correctamente
- Exportar a Markdown y verificar que se genera correctamente

### Sincronización
- Editar un documento sin conexión
- Verificar que se guarda localmente
- Conectar a internet y verificar que se sincroniza

## Errores Comunes y Soluciones

### El tutorial bloquea la navegación
**Solución**: Completar el tutorial o usar la opción "Reiniciar tutorial" en Perfil

### Los cambios no se guardan
**Solución**: Verificar que el documento tiene título y etiquetas requeridas

### Bibi no responde
**Solución**: Verificar conexión a internet y que el usuario está autenticado

### Las versiones no se guardan
**Solución**: Esperar 60 segundos o usar "Guardar versión" manualmente

## Métricas de Éxito

- ✅ Compilación exitosa sin errores
- ✅ 64 tests unitarios pasando
- ✅ Status bar muestra métricas en tiempo real
- ✅ Find & Replace funciona correctamente
- ✅ Drag & Drop reordena bloques
- ✅ Plantillas se cargan con estructura correcta
- ✅ Bibi Overlay responde e inserta notas
- ✅ Historial de versiones guarda y restaura
- ✅ Import/Export Markdown preserva estilos

## Próximos Pasos

1. Completar pruebas manuales de todas las funcionalidades
2. Reportar bugs encontrados
3. Considerar agregar UI para Import/Export Markdown
4. Optimizar rendimiento en documentos grandes
5. Agregar más plantillas de estudio
