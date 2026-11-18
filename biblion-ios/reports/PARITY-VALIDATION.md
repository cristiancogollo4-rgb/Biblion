# Validación de Biblion iOS 0.2.0

## Ejecutado

- XCTest: 16 pruebas, 0 fallos. Registro: `parity-tests.log` (ignorado en Git).
- Pruebas con bases reales: búsqueda, filtros, paginación, literales SQL, categorías y conteo del diccionario, orden canónico de temas, referencias cruzadas y Strong.
- Limpieza de respuestas Bibi: texto, JSON, bloques JSON, respuestas vacías y envelopes malformados.
- Persistencia y compatibilidad: lectura anterior sin campos nuevos, resaltados por versión y datos corruptos conservados.
- Registro de Biblion iOS en Firebase y descarga oficial del archivo de cliente mediante Firebase CLI. Google client ID y esquema de retorno presentes.
- Paquete ARM64 de simulador con firma local ad hoc, ZIP íntegro y permisos conservados.
- Las cinco bases empaquetadas coinciden por SHA-256 con los recursos originales de Android.

## Appetize — comprobaciones reales de la primera subida 0.2

Build `b_onrzcg6lrg3f3pboai6h7o3pla`, versión 0.2.0, build number 2. Dispositivo iPhone 14 Pro, iOS 26.0.

- Arranque y presentación automática del tutorial.
- Elegir Antiguo Testamento, Génesis y capítulo 1.
- Lector con el texto bíblico real.
- Mantener pulsado un versículo, menú de acciones y resaltado amarillo persistido durante la sesión.
- Avance automático del tutorial tras resaltar.
- Selector de seis versiones; cambio a NVI conservando Génesis 1.
- Apertura de Bibi con contexto de Génesis 1:1 desde el tutorial.
- Salida del tutorial, navegación principal de cinco pestañas, pantalla Buscar con filtros y pasajes iniciales.
- Explorar: accesos a diccionario, temas, Bibi, resaltados y modo estudio futuro.
- Diccionario cargado con 5.758 entradas reales; búsqueda nativa revelada al desplazar hacia abajo (corregida después para quedar siempre visible).
- Bibi respondió a «abraham» usando la biblioteca local, con definición y metadatos. Captura: `parity-bibi-local.jpg`.

Correcciones derivadas: contraste de botones del tutorial, buscadores siempre visibles, género Male/Female presentado en español, acceso visual a profundización y autenticación Storage acorde al SDK oficial.

## Pendiente de validación humana / de dispositivo

- Completar inicio de sesión Google o correo con una cuenta real. No se solicitaron ni introdujeron contraseñas o códigos de usuario.
- Validar Keychain tras relanzar, edición remota de perfil, foto y sincronización en ambos sentidos con Android.
- Comparación amplia de Bibi con los tests Android y sus políticas locales actuales.
- Recorrido completo de los once pasos tras el paso de Bibi; iPad, accesibilidad ampliada, iOS 17/18 y un iPhone físico.
- Instalación y firma Apple de dispositivo.

Appetize limita cada sesión gratuita a tres minutos; se reiniciaron sesiones para continuar las comprobaciones. No se modificó el plan ni se compraron recursos.

## Paquete final subido

Versión **0.2.0 (build 5)**. Identificador Appetize: `b_6fyhjqdb2hafkpa4iw5uklhgfm`.

[Ejecutar el build final](https://appetize.io/app/b_6fyhjqdb2hafkpa4iw5uklhgfm?device=iphone14pro&osVersion=26.0&toolbar=true).

La compilación final y las 16 pruebas XCTest terminaron correctamente tras las correcciones. Archivo: `dist/Biblion-Appetize-arm64.zip`. La captura `parity-bibi-local.jpg` corresponde a la primera subida 0.2 (build 2), antes de traducir el metadato Male a Masculino.

## Ejecución comprobada del build final 5

iPhone 14 Pro con iOS 26.0 en Appetize:

- Arranque correcto, tutorial automático y botones con texto blanco sobre azul.
- Salida del tutorial hacia la app y navegación de cinco pestañas.
- Buscar muestra su campo desde el inicio. Consulta «amor» introducida con el teclado del simulador: resultados bíblicos reales visibles.
- Explorar temas carga su catálogo; se abrió «Matrimonio» con descripción y enlaces de pasajes.
- Perfil abre el formulario con acceso por correo, registro, recuperación y Google.
- «Continuar con Google» presenta la confirmación nativa y abre **accounts.google.com**, con su formulario real de acceso. No se introdujo correo ni contraseña. Captura: `parity-google-login.jpg`.

El intercambio posterior de credenciales y la sesión de Biblion continúan pendientes de intervención del titular de la cuenta; no se consideran probados por mostrar la página de Google.

## Cierre de artefactos

- IPA de dispositivo: `xtool/Biblion.ipa`, versión 0.2.0, build 5, ARM64 iOS, compilación correcta y verificador correcto (`parity-device-build.log`, `parity-device-check.log`). Las cinco bases coinciden con Android. Sin firma de desarrollador: no se instaló en iPhone.
- ZIP de Appetize: `dist/Biblion-Appetize-arm64.zip`, 0.2.0 build 5, subido y ejecutado correctamente en iOS 26.0.
- Pantalla de modo estudio abierta y comprobada: icono y «Próximamente», sin editor. Captura `parity-study-future.jpg`.
- Pantalla Explorar final: diccionario, temas, Bibi, resaltados y estudio futuro. Captura `parity-explore-final.jpg`.
- Se verificó la sintaxis de los tres scripts Python. No se ejecutaron pruebas Gradle Android: esta fase modifica exclusivamente `biblion-ios/` y conserva los cambios Android previos del usuario.
