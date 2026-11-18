# Validación de Biblion iOS 0.1.0

Fecha: 16 de octubre de 2026 (America/Bogota).

Actualización del 18 de octubre: se ejecutó y probó la app en Appetize con iOS 26.0. Ver `APPETIZE.md` para el resultado, los recorridos y las capturas. Las limitaciones de ejecución de la entrega inicial descritas abajo corresponden al 16 de octubre.

## Ran · Pruebas del núcleo

`swift test` sobre Ubuntu/WSL, Swift 6.4.1: 11 pruebas con la base real y almacenamiento temporal.

- Versiones y orden canónico, 39 libros del Antiguo y 27 del Nuevo en RVR1960.
- Lectura de Génesis 1 con sus 31 versículos y títulos de sección.
- Identificación por índice de libro entre RVR1960, NVI y NTV.
- Capítulos inexistentes y versiones no válidas sin devolver contenido ajeno.
- Lecturas concurrentes aisladas por el actor.
- Paso entre libros/testamentos y límites de la Biblia.
- Navegación solo por contenido disponible en PDT (49 libros).
- Tema claro inicial y límites de tamaño de fuente.
- Identidad del resaltado separada por traducción y eliminación por alternancia.
- Persistencia y recuperación de la posición exacta y los resaltados.
- Detección de JSON corrupto sin sobrescribir los datos.

## Ran · Compilación y paquete iOS

`xtool dev build --ipa`, xtool 1.22.2 con SDK iPhoneOS 27.0: compilación completada y `xtool/Biblion.ipa` generado sin firma.

La primera compilación detectó una ruta de biblioteca Linux introducida por `pkg-config`. Se eliminó ese uso del manifiesto y la compilación final enlaza SQLite del sistema iOS correctamente, sin bibliotecas Linux.

`python Scripts/verify_ipa.py xtool/Biblion.ipa`: pasó sobre el archivo final. Comprueba:

- Identificador `com.cristiancogollo.biblion.ios`, nombre Biblion y versión 0.1.0.
- Ejecutable Mach-O ARM64 para dispositivo iOS, mínimo iOS 17.0.
- SQLite enlazado a `/usr/lib/libsqlite3.dylib`.
- Recurso bíblico único e idéntico al de Android, mediante SHA-256.
- Logos claro y oscuro presentes.
- Paquete sin perfil de aprovisionamiento.

SHA-256 de `bible_content.db`:

```text
0f323ad248d6dc25a099075f076924b3c9bff31aecad5d3be8bf9321e5ec9573
```

Inspección adicional del paquete: `UIDeviceFamily = [1, 2]` (iPhone/iPad), orientaciones declaradas para ambos y archivo `AppIcon.png` con metadato `CFBundleIconFile`.

## Ran · Contraste de tokens

Cálculo de luminancia relativa sobre los valores de `Theme.swift`; son mediciones de tokens, no capturas renderizadas.

| Combinación | Relación |
| --- | ---: |
| Texto claro/fondo | 13.68:1 |
| Texto secundario claro/fondo | 5.99:1 |
| Dorado claro/superficie | 5.57:1 |
| Texto oscuro/fondo | 15.85:1 |
| Texto secundario oscuro/superficie | 8.35:1 |
| Dorado oscuro/superficie | 9.04:1 |
| Texto sobre resaltado claro | 12.59:1 |
| Texto sobre resaltado oscuro | 8.43:1 |

## Skipped · Ejecución y verificación visual de iOS

No se ejecutó la app en iPhone/iPad ni se produjeron capturas nativas: la autenticación de Apple y la firma quedaron aplazadas por petición del usuario. Windows/WSL no proporciona un simulador iOS ejecutable. La compilación no confirma el comportamiento de las pantallas ni del scroll.

## Manual · Comprobaciones al disponer del dispositivo

- Abrir por primera vez y leer Génesis sin internet.
- Cambiar testamento, filtrar libros y abrir un capítulo de cada versión.
- Pasar del último capítulo de un libro al siguiente y verificar los límites inicial/final.
- Confirmar que la versión alternativa conserve libro/capítulo y oculte opciones sin ese contenido.
- Desplazarse, cerrar y abrir la app; verificar que Continuar lectura recupere el versículo visible.
- Resaltar, copiar, compartir, abrir desde Resaltados y quitar un resaltado.
- Probar tema claro/oscuro, el tamaño de fuente máximo y VoiceOver.
- Revisar en iPhone compacto, orientación horizontal e iPad; comprobar safe areas y controles del lector con Dynamic Type extremo.
- Verificar el vaciado de persistencia al enviar la app a segundo plano y el mensaje de error si una escritura falla.

## Alcance preservado

Los cambios de esta entrega se limitan a `biblion-ios/`. No se cambiaron archivos de Android, web, Firebase ni Bibi. No se ejecutó Gradle: no se modificó Android ni se propone un merge. Los cambios ajenos existentes en el working tree se conservaron.

La búsqueda de versículos, diccionario, temas, Firebase, Bibi y modo estudio no forman parte de 0.1.0. El modo estudio queda explícitamente para la última etapa.
