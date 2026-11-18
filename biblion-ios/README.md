# Biblion iOS · 0.2.0

Primera versión nativa en SwiftUI para iPhone y iPad. El destino mínimo declarado es iOS 17; la ejecución se verificó en Appetize con iOS 26.0, y las versiones anteriores siguen pendientes de validación. La lectura usa una copia exacta de la base SQLite de Android y funciona sin conexión.

## Funciones implementadas

- Biblia local en seis versiones, libros en orden canónico, capítulos, títulos y última lectura.
- Navegación con botones y gestos; tema claro, oscuro y del dispositivo; tamaño de lectura adaptable.
- Resaltados con colores, listado, copiar y compartir; comparación de versículos entre versiones.
- Búsqueda local de versículos con filtros, paginación, historial y pasajes populares.
- Diccionario de ocho categorías con definiciones y metadatos; temas de once categorías con pasajes y relaciones.
- Referencias cruzadas locales con ranking interno; Strong desde Bibi.
- Bibi local primero, conversaciones persistentes, renombrar/eliminar sesiones y profundización con el Worker autenticado.
- Cuenta por correo/contraseña y Google, recuperación, sesión segura en Keychain, perfil, avatar y foto.
- Sincronización de preferencias y resaltados con el formato de Android, consulta inicial y periódica y envío de cambios.
- Tutorial de lectura interactivo de once pasos, reiniciable desde Perfil.
- Icono de modo estudio visible con «Próximamente», tal como pidió el usuario.

Ver [PARITY.md](PARITY.md) para distinguir implementación, pruebas realizadas y validaciones pendientes. Las funciones de cuenta y sincronización necesitan una cuenta real; no se consideran verificadas solo por compilar.

## Compilar desde Ubuntu / WSL

```sh
cd '/mnt/c/Users/LENOVO LOQ/Desktop/PROYECTOS/Biblion/biblion-ios'
swift test --build-system native --scratch-path .build/test-native -j 2
xtool dev build --ipa
```

El resultado se genera bajo `xtool/`. Es una compilación **sin firma** para ARM64 iOS; no se instala directamente en un iPhone. No requiere iniciar sesión en Apple para compilar.

Una vez resuelta la autenticación y conectado el iPhone a WSL, `xtool dev run` realiza el flujo de desarrollo con firma e instalación. La firma, la ejecución física y la distribución quedan para la siguiente sesión, por indicación del usuario.

También se puede usar `bash Scripts/check.sh` para ejecutar pruebas y compilar secuencialmente. No ejecutar dos procesos SwiftPM simultáneos sobre la misma carpeta `.build`.

## Compilación para Appetize

Appetize requiere una `.app` de simulador comprimida, no el IPA de dispositivo. Desde Ubuntu:

```sh
bash Scripts/build-appetize.sh
```

Genera `dist/Biblion-Appetize-arm64.zip` con firma local ad hoc, sin cuenta ni certificado de Apple. El empaquetador comprueba el ejecutable ARM64 para iOS Simulator, la presencia de su firma, la integridad del ZIP, las cinco bases originales de Android y la configuración de retorno de Google, y conserva permisos Unix. El resultado de la verificación local se guarda en `reports/appetize-package.json`.

Con Swift 6.4.1, el script utiliza xtool para preparar el bundle y el motor nativo de SwiftPM para el enlace final con `-adhoc_codesign`; el motor SwiftBuild no estaba transmitiendo esa opción al ejecutable. El motor nativo está deprecado en Swift, por lo que este flujo deberá revisarse antes de actualizar la herramienta. La verificación del paquete detecta si deja de generarse la firma local.

Subir ese ZIP desde https://appetize.io/upload con una sesión iniciada. No requiere configurar una cuenta de Apple para la firma. La ejecución se realiza en infraestructura de Appetize y no sustituye las pruebas físicas del iPhone.

La versión ya está subida y probada: [abrir Biblion en Appetize](https://appetize.io/app/ios/com.cristiancogollo.biblion.ios?device=iphone14pro&osVersion=26.0&toolbar=true). Requiere acceso a la cuenta de Appetize correspondiente. Ver `reports/APPETIZE.md` para los recorridos comprobados y las capturas.

## Estructura

```text
Sources/Biblion/          App SwiftUI, navegación, pantallas y ViewModels
Sources/BiblionCore/      Modelos, actor SQLite y persistencia de lectura
Sources/CSQLite/          Puente a SQLite del sistema, sin dependencias remotas
Tests/BiblionCoreTests/   Pruebas con la base real y almacenamiento temporal
Assets/                  Icono oficial de la app
Info.plist               Metadatos iOS
xtool.yml                Configuración de empaquetado
```

`BibleDatabase` abre el recurso exclusivamente en modo lectura y serializa el acceso mediante un actor. Los libros se identifican por `bookIndex`, evitando diferencias de nombres entre traducciones, como Cantares/Hechos en NTV. Las consultas usan parámetros enlazados.

`ReadingStateStore` guarda `reading-v1.json` en Application Support/Biblion. No modifica la Biblia ni comparte almacenamiento con Android. Si los datos locales no se pueden decodificar, se informa el error y se conserva el archivo; no se restablece silenciosamente.

Las bases (`bible_content.db`, `cross_references_votes.db`, `dictionary_v2.db`, `strongs.db` y `topics.db`) no se versionan en esta carpeta: son copias de `../app/src/main/assets/databases/`. Antes de compilar, y cada vez que cambien en Android, cópialas y ejecuta las pruebas:

```sh
cp ../app/src/main/assets/databases/{bible_content,cross_references_votes,dictionary_v2,strongs,topics}.db Sources/BiblionCore/Resources/
```

No editar las traducciones desde la app.

## Firebase y Google

La aplicación iOS está registrada en el mismo proyecto Firebase de Android. Su configuración de cliente vive en `Sources/Biblion/Resources/Services.plist` y el esquema de retorno en `Info.plist`. Igual que `google-services.json` y `local.properties` en Android, `Services.plist` y `GoogleService-Info.plist` no se versionan: tienen la API key de Firebase y la URL del Worker de Bibi. No se incluyen contraseñas, tokens de sesión ni secretos de proveedores IA en el repositorio.

Para preparar o actualizar la configuración:

```sh
cp Services.example.plist Sources/Biblion/Resources/Services.plist   # solo la primera vez; luego completa bibiURL
python3 Scripts/configure-google.py GoogleService-Info.plist          # el plist se descarga de Firebase
bash Scripts/build-appetize.sh
```

La cuenta de Apple usada para firmar es independiente del acceso de usuario a Biblion. Google abre el flujo de autenticación del sistema con PKCE. La sesión se almacena en Keychain; los datos de lectura se separan por cuenta. Las preguntas para profundizar y su contexto bíblico se envían al Worker de Bibi, que conserva sus políticas y validación de Firebase.

## Etapas siguientes

Validar en una cuenta real el acceso, perfil, Storage y sincronización entre Android/iOS; ampliar las pruebas de equivalencia de Bibi; comprobar iPad, accesibilidad y firma en iPhone. El editor de estudio queda fuera de esta entrega.

## Validación

Ver `reports/VALIDATION.md` para resultados y comprobaciones manuales pendientes. Una compilación correcta no confirma el comportamiento visual o de accesibilidad en un dispositivo físico.
