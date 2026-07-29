# Manual de Testing - Bibi (Motor de Intenciones)

**Fecha**: 26 de junio de 2026
**Version**: v1.1 - Post-correccion de bugs
**Ambiente**: Android, Bibi overlay en lector

---

## 1. Resumen de cambios

- **Bug 1**: TOPICS no detectaba "tema" singular (solo "temas")
- **Bug 2**: `extractTermFromQuestion` extraía "me genesis 1:1" en vez de "genesis 1:1"
- **Bug 3**: "explícame genesis 1:1" caía en DEFINE en vez de EXPLAIN_VERSE
- **Bug 4**: Frame drops por DB en main thread (corregido con Dispatchers.IO)
- **Bug 5**: "hi" como greeting matcheaba "historia" (corregido)
- **Mejora**: Patrones de intenciones expandidos para cubrir mas formas en espanol

---

## 2. Matriz de intenciones

| Intencion | Que detecta | Fuente de datos |
|-----------|-------------|-----------------|
| GREETING | Saludos: hola, buenas, hey, hi, hello, que tal, etc. | Respuesta local fija |
| WHO | Quien fue/es/era, cuentame de, hablame de, historia de, etc. | TopicEngine + DictionaryEngine |
| WHERE | Donde queda/esta/nacio/vivio, ubicacion de, en que lugar, etc. | TopicEngine + DictionaryEngine |
| DEFINE | Que significa, define, que es, que fue, explícame (sin ref), etc. | TopicEngine + DictionaryEngine |
| EXPLAIN_VERSE | Explícame este versiculo/pasaje, explícame genesis 1:1, etc. | DictionaryEngine + TopicEngine |
| RELATED | Pasajes relacionados, versiculos similares, paralelo, comparar con, etc. | CrossReferenceVoteEngine + TopicEngine |
| TOPICS | Temas de/del, de que tema(s) habla, de que trata, que temas toca, etc. | TopicEngine.getTopicsForVerse() |
| ORIGINAL_LANG | Hebreo, griego, arameo, Strong H1254, etimologia, raiz, etc. | StrongEngine |
| DIVE_DEEPER | Profundiza, explica mas, cuentame mas, y que mas, sigue, etc. | KnowledgeEngine local o Worker IA |
| FALLBACK | Todo lo anterior no matchea | Worker IA (si hay conexion) |

---

## 3. Pruebas manuales por intencion

### 3.1 GREETING (Saludo)

| # | Entrada esperada | Intencion esperada | Que debe ocurrir |
|---|------------------|--------------------|------------------|
| 1 | "hola" | GREETING | Bibi saluda con nombre visible (si inicio sesion) |
| 2 | "buenas" | GREETING | Saludo generico |
| 3 | "buenos dias" | GREETING | Saludo generico |
| 4 | "buenas tardes" | GREETING | Saludo generico |
| 5 | "buenas noches" | GREETING | Saludo generico |
| 6 | "hey" | GREETING | Saludo generico |
| 7 | "saludos" | GREETING | Saludo generico |
| 8 | "que tal" | GREETING | Saludo generico |
| 9 | "como estas" | GREETING | Saludo generico |
| 10 | "hello" | GREETING | Saludo generico |
| 11 | "hi" | GREETING | Saludo generico |
| 12 | "hi Juan" | GREETING | Saludo generico |

**NO debe detectar como GREETING**:
- "historia de David" (debe ser WHO)
- "hijo de Dios" (debe ser DEFINE o FALLBACK)

---

### 3.2 WHO (Quien)

| # | Entrada esperada | Intencion esperada | Que debe ocurrir |
|---|------------------|--------------------|------------------|
| 1 | "quien fue Abraham" | WHO | TopicEngine + DictionaryEngine: nombre, genero, fechas, definicion |
| 2 | "quien es Moises" | WHO | Busqueda por termino en ambos motores |
| 3 | "quien era David" | WHO | Respuesta local con metadata |
| 4 | "cuéntame de Pablo" | WHO | Busqueda por termino |
| 5 | "háblame de Pedro" | WHO | Busqueda por termino |
| 6 | "cuéntame sobre Juan" | WHO | Busqueda por termino |
| 7 | "historia de Rut" | WHO | Busqueda por termino |
| 8 | "dime quién fue Moisés" | WHO | Busqueda por termino |
| 9 | "sabes quién es Abraham" | WHO | Busqueda por termino |
| 10 | "informacion de Egipto" | WHO | Busqueda por termino |
| 11 | "dime sobre David" | WHO | Busqueda por termino |

**Que contiene la respuesta**:
- Titulo: nombre del termino
- Definicion: desde dictionary_v2.db (Easton's) o topics.db
- Metadata: genero, fechas, coordenadas (si PERSON o PLACE)
- Versiculos: top 3 referencias del diccionario
- Sugerencias: "Pedir a IA: pasajes sobre X", "Comparar con Y"

---

### 3.3 WHERE (Donde)

| # | Entrada esperada | Intencion esperada | Que debe ocurrir |
|---|------------------|--------------------|------------------|
| 1 | "donde queda Jerusalen" | WHERE | TopicEngine + DictionaryEngine |
| 2 | "donde esta Galilea" | WHERE | Coordenadas GPS + featureType |
| 3 | "donde nacio Jesus" | WHERE | Busqueda por termino |
| 4 | "donde vivio David" | WHERE | Busqueda por termino |
| 5 | "ubicacion de Egipto" | WHERE | Coordenadas + tipo |
| 6 | "en que lugar quedaba Babilonia" | WHERE | Busqueda por termino |
| 7 | "donde quedaba Sodoma" | WHERE | Busqueda por termino |
| 8 | "se encuentra en Galilea" | WHERE | Busqueda por termino |
| 9 | "donde esta el Monte Sinaí" | WHERE | Coordenadas GPS |

---

### 3.4 DEFINE (Definicion)

| # | Entrada esperada | Intencion esperada | Que debe ocurrir |
|---|------------------|--------------------|------------------|
| 1 | "que significa gracia" | DEFINE | Busqueda en topics.db + dictionary_v2.db |
| 2 | "define pacto" | DEFINE | Busqueda por termino |
| 3 | "que es la fe" | DEFINE | Busqueda por termino |
| 4 | "que es un discipulo" | DEFINE | Busqueda por termino |
| 5 | "que es una alianza" | DEFINE | Busqueda por termino |
| 6 | "dime que es el bautismo" | DEFINE | Busqueda por termino |
| 7 | "cuál es la definición de fe" | DEFINE | Busqueda por termino |
| 8 | "qué quiere decir gracia" | DEFINE | Busqueda por termino |
| 9 | "qué representa el bautismo" | DEFINE | Busqueda por termino |
| 10 | "significado de amor" | DEFINE | Busqueda por termino |
| 11 | "define oracion" | DEFINE | Busqueda por termino |

**Que contiene la respuesta**:
- Si TopicEngine encuentra el tema: descripcion desde topics.db + subtemas + versiculos asociados
- Si solo DictionaryEngine: definicion de Easton's o metadata Theographic
- Sugerencias: "Profundizar", "Definicion", subtemas si existen

---

### 3.5 EXPLAIN_VERSE (Explicar versiculo)

| # | Entrada esperada | Intencion esperada | Que debe ocurrir |
|---|------------------|--------------------|------------------|
| 1 | "explícame este versículo" | EXPLAIN_VERSE | Necesita estar en lector activo |
| 2 | "explícame este pasaje" | EXPLAIN_VERSE | Necesita estar en lector activo |
| 3 | "explícame genesis 1:1" | EXPLAIN_VERSE | **NUEVO**: detecta ref por regex |
| 4 | "explica mateo 5:3" | EXPLAIN_VERSE | **NUEVO**: detecta ref por regex |
| 5 | "explica este texto" | EXPLAIN_VERSE | Necesita estar en lector activo |
| 6 | "explícame este capítulo" | EXPLAIN_VERSE | **NUEVO**: detecta "este capitulo" |
| 7 | "dime qué es juan 3:16" | EXPLAIN_VERSE | **NUEVO**: detecta ref por regex |
| 8 | "explica Romanos 8:28" | EXPLAIN_VERSE | **NUEVO**: detecta ref por regex |

**Sin contexto de lector**: Si el usuario no esta en un versiculo activo y escribe "explícame este versiculo", Bibi responde "Necesito que estes leyendo uno".

---

### 3.6 RELATED (Referencias cruzadas)

| # | Entrada esperada | Intencion esperada | Que debe ocurrir |
|---|------------------|--------------------|------------------|
| 1 | "versiculos relacionados con genesis 1:1" | RELATED | CrossReferenceVoteEngine.getRelatedBySource() |
| 2 | "pasajes similares" | RELATED | Necesita versiculo activo |
| 3 | "otros pasajes" | RELATED | Necesita versiculo activo |
| 4 | "paralelos de mateo 5:3" | RELATED | CrossReferenceVoteEngine |
| 5 | "comparar con juan 3:16" | RELATED | CrossReferenceVoteEngine |
| 6 | "tiene que ver con romanos 8" | RELATED | CrossReferenceVoteEngine |
| 7 | "donde más se habla de amor" | RELATED | CrossReferenceVoteEngine |

**Sin contexto de lector**: Si no hay versiculo activo, Bibi pide que se abra un capitulo primero.

**Que contiene la respuesta**:
- Lista de pasajes relacionados con formato "Libro Cap:V: texto corto"
- Los votos (score) **NUNCA** se muestran al usuario
- Topics del versiculo actual
- Sugerencias: "Explicar este versiculo", "Saber mas con IA"

---

### 3.7 TOPICS (Temas del versiculo)

| # | Entrada esperada | Intencion esperada | Que debe ocurrir |
|---|------------------|--------------------|------------------|
| 1 | "temas de genesis 1:1" | TOPICS | TopicEngine.getTopicsForVerse() |
| 2 | "temas del versiculo" | TOPICS | Necesita versiculo activo |
| 3 | "de que temas habla" | TOPICS | Necesita versiculo activo |
| 4 | "de qué tema habla genesis 1:1" | TOPICS | **NUEVO**: singular "tema" |
| 5 | "que temas habla juan 3:16" | TOPICS | TopicEngine |
| 6 | "de que trata este capitulo" | TOPICS | **NUEVO**: nuevo patron |
| 7 | "que temas toca romanos 8" | TOPICS | **NUEVO**: nuevo patron |
| 8 | "cuales son los temas de genesis 1" | TOPICS | **NUEVO**: nuevo patron |
| 9 | "que tema menciona genesis 1:1" | TOPICS | **NUEVO**: nuevo patron |
| 10 | "de qué se habla en mateo 5" | TOPICS | **NUEVO**: nuevo patron |

**Sin contexto de lector**: Si no hay versiculo activo, Bibi pide que se abra un capitulo.

**Que contiene la respuesta**:
- Lista de temas canonicos (topics.db, schema v3)
- Top 8 temas ordenados por relevancia
- Sugerencias: "Versiculos sobre X", "Explicar este versiculo"

---

### 3.8 ORIGINAL_LANG (Lengua original)

| # | Entrada esperada | Intencion esperada | Que debe ocurrir |
|---|------------------|--------------------|------------------|
| 1 | "que dice en hebreo" | ORIGINAL_LANG | StrongEngine |
| 2 | "que significa en griego" | ORIGINAL_LANG | StrongEngine |
| 3 | "H1254" | ORIGINAL_LANG | StrongEngine.lookupByNumber() |
| 4 | "G25" | ORIGINAL_LANG | StrongEngine.lookupByNumber() |
| 5 | "en original amor" | ORIGINAL_LANG | **NUEVO**: nuevo patron |
| 6 | "palabra original fe" | ORIGINAL_LANG | **NUEVO**: nuevo patron |
| 7 | "etimologia gracia" | ORIGINAL_LANG | **NUEVO**: nuevo patron |
| 8 | "raiz de la palabra amor" | ORIGINAL_LANG | **NUEVO**: nuevo patron |
| 9 | "traducción literal aleluya" | ORIGINAL_LANG | **NUEVO**: nuevo patron |

---

### 3.9 DIVE_DEEPER (Profundizar)

| # | Entrada esperada | Intencion esperada | Que debe ocurrir |
|---|------------------|--------------------|------------------|
| 1 | "profundiza" | DIVE_DEEPER | Worker IA (si hay conexion) |
| 2 | "explica más" | DIVE_DEEPER | Worker IA |
| 3 | "cuéntame más" | DIVE_DEEPER | Worker IA |
| 4 | "dime más" | DIVE_DEEPER | Worker IA |
| 5 | "háblame más de" | DIVE_DEEPER | **NUEVO**: nuevo patron |
| 6 | "qué más sabes" | DIVE_DEEPER | **NUEVO**: nuevo patron |
| 7 | "cuéntame otro" | DIVE_DEEPER | **NUEVO**: nuevo patron |
| 8 | "quiero saber más" | DIVE_DEEPER | **NUEVO**: nuevo patron |
| 9 | "hay más" | DIVE_DEEPER | **NUEVO**: nuevo patron |
| 10 | "qué más" | DIVE_DEEPER | **NUEVO**: nuevo patron |
| 11 | "y que más" | DIVE_DEEPER | **NUEVO**: nuevo patron |
| 12 | "sigue" | DIVE_DEEPER | **NUEVO**: nuevo patron |
| 13 | "y" (con historial) | DIVE_DEEPER | Follow-up corto |
| 14 | "pero" (con historial) | DIVE_DEEPER | Follow-up corto |
| 15 | "por qué" (con historial) | DIVE_DEEPER | Follow-up corto |

**Nota**: DIVE_DEEPER requiere Worker IA (Cloudflare Worker). Si el Worker esta caido (502), la respuesta es null y se usa fallback local.

---

### 3.10 FALLBACK (Sin match)

| # | Entrada esperada | Intencion esperada | Que debe ocurrir |
|---|------------------|--------------------|------------------|
| 1 | "el cielo esta nublado" | FALLBACK | Worker IA o mensaje por defecto |
| 2 | "como programar en Kotlin" | FALLBACK | Worker IA o mensaje por defecto |
| 3 | "receta de pastel" | FALLBACK | Worker IA o mensaje por defecto |
| 4 | "quien es el presidente" | FALLBACK | Worker IA o mensaje por defecto |
| 5 | "clima de hoy" | FALLBACK | Worker IA o mensaje por defecto |

**Sin Worker**: Si el Worker falla (502 o sin conexion), Bibi responde:
"No pude procesar tu pregunta. Intenta con algo como: Quien fue Abraham, Donde esta Jerusalen, o Que significa gracia."

---

## 4. Bugs corregidos - Pruebas de regresion

### 4.1 Bug: "de que tema habla genesis 1:1" → FALLBACK

**ANTES**: El patron TOPICS solo buscaba "temas" (plural). "tema" (singular) no matcheaba.
**AHORA**: Detecta "de que tema", "tema de", "que tema habla".

| # | Entrada | Intencion esperada |
|---|---------|--------------------|
| 1 | "de que tema habla 1samuel 1:1" | TOPICS |
| 2 | "de que tema habla genesis 1:1" | TOPICS |
| 3 | "tema de mateo 5:3" | TOPICS |
| 4 | "que tema habla juan 3:16" | TOPICS |

### 4.2 Bug: "explícame genesis 1:1" → DEFINE con "me genesis 1:1"

**ANTES**: `extractTermFromQuestion` matcheaba "explica" (substring de "explicame") y extraia "me genesis 1:1".
**AHORA**: "explícame" y "explicame" vienen ANTES de "explica" en la lista de patrones.

| # | Entrada | Termino esperado |
|---|---------|-----------------|
| 1 | "explícame genesis 1:1" | "genesis 1:1" |
| 2 | "explica mateo 5:3" | "mateo 5:3" |
| 3 | "explícame juan 3:16" | "juan 3:16" |
| 4 | "explica el bautismo" | "bautismo" |
| 5 | "explícame la gracia" | "gracia" |

### 4.3 Bug: "explícame genesis 1:1" → DEFINE en vez de EXPLAIN_VERSE

**ANTES**: Sub-condicion solo verificaba "versiculo", "pasaje", "este texto".
**AHORA**: Tambien detecta regex `\w+\s+\d+:\d+` y "este capitulo".

| # | Entrada | Intencion esperada |
|---|---------|--------------------|
| 1 | "explícame genesis 1:1" | EXPLAIN_VERSE |
| 2 | "explica mateo 5:3" | EXPLAIN_VERSE |
| 3 | "dime qué es juan 3:16" | EXPLAIN_VERSE |
| 4 | "explica Romanos 8:28" | EXPLAIN_VERSE |
| 5 | "explícame este capítulo" | EXPLAIN_VERSE |

### 4.4 Bug: Frame drops por DB en main thread

**ANTES**: `processBibiQuery` ejecutaba queries Room desde `Dispatchers.Main`.
**AHORA**: Envuelta en `withContext(Dispatchers.IO)`.

**Prueba**: Abrir Bibi, escribir una pregunta, y verificar que la UI no se congela. El indicador de carga debe aparecer inmediatamente.

### 4.5 Bug: "historia de David" → GREETING

**ANTES**: `q.startsWith("hi")` matcheaba "historia".
**AHORA**: Se verifica que la primera palabra (separada por espacio) sea exactamente "hi".

| # | Entrada | Intencion esperada |
|---|---------|--------------------|
| 1 | "historia de David" | WHO |
| 2 | "historia de Moises" | WHO |
| 3 | "hi" | GREETING |
| 4 | "hi Juan" | GREETING |

---

## 5. Pruebas de prioridad de intenciones

El orden de evaluacion en `detectIntent()` es:
1. DIVE_DEEPER (primero)
2. GREETING
3. ORIGINAL_LANG
4. WHO
5. WHERE
6. RELATED
7. DEFINE / EXPLAIN_VERSE (con sub-condicion)
8. TOPICS
9. FALLBACK

| # | Entrada | Intencion esperada | Razon |
|---|---------|--------------------|-------|
| 1 | "profundiza en temas" | DIVE_DEEPER | Tiene prioridad sobre TOPICS |
| 2 | "hola, temas de Genesis" | GREETING | Tiene prioridad sobre TOPICS |
| 3 | "quien fue Abraham, temas" | WHO | Tiene prioridad sobre TOPICS |
| 4 | "hebreo, temas de Genesis" | ORIGINAL_LANG | Tiene prioridad sobre TOPICS |
| 5 | "temas relacionados con Juan 3:16" | RELATED | "relacionad" matchea antes que TOPICS |
| 6 | "define versiculo juan 3:16" | EXPLAIN_VERSE | Sub-condicion: contiene "versiculo" |

---

## 6. Limitaciones conocidas (que Bibi NO puede responder)

### 6.1 Fuera de dominio biblico

Bibi **NO** responde preguntas que no sean biblicas/cristianas. Si el Worker detecta que esta fuera de dominio, responde con un mensaje de redireccion.

| Ejemplo | Que ocurre |
|---------|------------|
| "como programar en Kotlin" | FALLBACK → Worker IA redirige a dominio biblico |
| "receta de pastel" | FALLBACK → Worker IA redirige |
| "clima de hoy" | FALLBACK → Worker IA redirige |
| "quien es el presidente de Colombia" | FALLBACK → Worker IA redirige |

### 6.2 Versiculos

Bibi **NO** muestra versiculos completos del texto biblico (proteccion de licencia RV60). Solo muestra:
- Referencias: "Genesis 1:1"
- Texto truncado a 110 caracteres en pasajes relacionados

### 6.3 Worker caido (502)

Si el Worker de Cloudflare esta caido (limite gratuito del LLM agotado):
- DIVE_DEEPER → null → fallback local
- FALLBACK → null → "No pude procesar tu pregunta..."
- Intenciones locales (WHO, WHERE, DEFINE, EXPLAIN_VERSE, RELATED, TOPICS, ORIGINAL_LANG) → funcionan normalmente

### 6.4 Versiculo activo requerido

Algunas intenciones **necesitan** que el usuario este en el lector con un versiculo activo:
- **RELATED** sin versiculo → "Necesito que estes leyendo un versiculo"
- **TOPICS** sin versiculo → "Necesito que estes leyendo un versiculo"
- **EXPLAIN_VERSE** sin versiculo en contexto → "Necesito que estes leyendo uno"

### 6.5 Terminos no encontrados

Si un termino no existe en topics.db ni dictionary_v2.db:
- **WHO/WHERE/DEFINE**: "No encontre [termino]" + sugerencias de alternativas via AmbiguousTermResolver
- **RELATED/TOPICS** sin resultados: "No encontre pasajes relacionados" o "Sin temas especificos"

---

## 7. Flujo completo de prueba

### Prueba rapida (5 minutos)

1. **Abrir Bibi** → FAB azul con logo
2. **Saludo**: Escribir "hola" → debe saludar con nombre
3. **WHO**: Escribir "quien fue Abraham" → debe dar info de Abraham
4. **DEFINE**: Escribir "que significa gracia" → debe definir gracia
5. **TOPICS**: Dentro del lector en Genesis 1:1, escribir "de que tema habla genesis 1:1" → debe listar temas
6. **EXPLAIN_VERSE**: Escribir "explícame genesis 1:1" → debe explicar el versiculo
7. **RELATED**: Dentro del lector, escribir "versiculos relacionados con genesis 1:1" → debe listar pasajes
8. **ORIGINAL_LANG**: Escribir "H1254" → debe mostrar info del numero de Strong

### Prueba de regresion (10 minutos)

1. **Bug 1**: Escribir "de que tema habla 1samuel 1:1" → debe ser TOPICS, no FALLBACK
2. **Bug 2**: Escribir "explícame genesis 1:1" → termino extraido debe ser "genesis 1:1", no "me genesis 1:1"
3. **Bug 3**: Escribir "explícame genesis 1:1" → debe ser EXPLAIN_VERSE, no DEFINE
4. **Bug 5**: Escribir "historia de David" → debe ser WHO, no GREETING
5. **Frame drops**: Escribir cualquier pregunta y verificar que la UI no se congela

### Prueba de patrones nuevos

| Entrada | Intencion esperada |
|---------|--------------------|
| "buenas tardes" | GREETING |
| "que tal" | GREETING |
| "cuéntame sobre Pablo" | WHO |
| "dime quién fue Moisés" | WHO |
| "donde quedaba Babilonia" | WHERE |
| "en que lugar quedaba Sodoma" | WHERE |
| "pasajes parecidos a Juan 3:16" | RELATED |
| "comparar con Romanos 8:28" | RELATED |
| "cuál es la definición de pacto" | DEFINE |
| "qué quiere decir gracia" | DEFINE |
| "de que trata este capitulo" | TOPICS |
| "cuáles son los temas de Génesis 1" | TOPICS |
| "raíz de la palabra amor" | ORIGINAL_LANG |
| "etimologia gracia" | ORIGINAL_LANG |
| "quiero saber más" | DIVE_DEEPER |
| "qué más sabes" | DIVE_DEEPER |
| "hi" | GREETING |

---

## 8. Checklists de verificacion

### Antes de cada prueba:
- [ ] Bibi esta abierto (FAB visible)
- [ ] Si se prueba RELATED/TOPICS/EXPLAIN_VERSE, estar en el lector con un capitulo abierto
- [ ] Si se prueba DIVE_DEEPER, tener una pregunta anterior en el historial

### Despues de cada prueba:
- [ ] La respuesta contiene titulo, definicion y follow-up
- [ ] Las sugerencias (chips) son clickeables
- [ ] El scroll funciona (texto largo no se corta)
- [ ] El historial de chat se mantiene entre preguntas
- [ ] No hay frame drops visibles
