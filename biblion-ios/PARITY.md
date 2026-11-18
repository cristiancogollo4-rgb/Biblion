# Biblion iOS — paridad con Android

Objetivo: portar las funciones de Biblion Android a iOS; modo estudio visible como «Próximamente», por petición del usuario. Android permanece sin cambios en esta implementación.

## Implementación 0.2.0

| Área | Implementación | Validación |
|---|---|---|
| Biblia | Seis versiones reales, catálogo por versión, capítulos, títulos, posición de lectura, navegación y gestos | Pruebas con bible_content.db; lectura 0.1 verificada en Appetize |
| Resaltados | Persistencia por versión, cuatro colores, listado, eliminación, copiar y compartir | Persistencia comprobada; resaltado amarillo comprobado en Appetize; otros colores pendientes |
| Búsqueda | Texto local sin distinción de acentos, testamento/libro, páginas, búsquedas recientes y seis versículos iniciales | Pruebas SQL reales con filtros, paginación y caracteres literales |
| Diccionario | Ocho categorías, búsqueda, definiciones y metadatos | 5.758 entradas reales en dictionary_v2.db actual; el AGENTS histórico menciona 6.346 |
| Temas | Once categorías, 66 libros en orden canónico, aliases, descripciones, pasajes e hijos | Pruebas de orden canónico y referencias; alias Cantar de los Cantares/Cantares normalizado |
| Referencias cruzadas | Base original, umbral de ranking interno, navegación a pasajes | Consulta real comprobada; los votos no se muestran en la interfaz |
| Strong | Consulta local H/G desde Bibi | H1254 comprobado con strongs.db |
| Bibi | Local primero, definiciones y metadatos, referencias, temas, anáforas básicas, comparación de versiones reales, Worker autenticado, sesiones locales renombrables/eliminables | Motor nuevo; requiere prueba visual y comparación más amplia con Android. Respuesta local sobre Abraham comprobada en Appetize. No se afirma equivalencia completa de todas las políticas del orquestador Android |
| Cuenta | Correo/contraseña, registro, recuperación, tokens en Keychain, renovación, Google OAuth con PKCE | Configuración Firebase iOS oficial registrada e importada; flujo abierto hasta la página oficial de Google en Appetize, sin introducir credenciales |
| Perfil | Identidad, alias, biografía, métricas remotas, avatar y foto | Cliente real Firebase REST; pendiente de prueba con cuenta autenticada y Storage disponible |
| Sincronización | Preferencias y resaltados compatibles con los documentos Android, timestamps, tombstones, consulta inicial y cada 30 s, envío diferido tras cambios | Pendiente de prueba con dos dispositivos/cuenta real; no es un listener Firestore en tiempo real |
| Tutorial | Once pasos, elección real de testamento/libro/capítulo, lector, práctica de resaltado, versiones y Bibi; reinicio en Perfil | Pasos 1–8 practicados en Appetize; finalización completa pendiente |
| Modo estudio | Icono visible en Inicio, Explorar y Perfil; pantalla «Próximamente» | Exclusión intencional, no editor simulado |

## Fuentes y datos

Las cinco bases SQLite se copian desde `app/src/main/assets/databases` sin generar contenido ficticio. La clave de Firebase incluida es configuración pública de cliente, nunca una clave de cuenta de servicio o de proveedor IA. Los secretos del proveedor de Bibi permanecen en el Worker. El nombre de perfil se usa para el saludo local y no se incluye como campo de identidad en el payload al Worker.

APIs oficiales consultadas: [Firebase Auth REST](https://firebase.google.com/docs/reference/rest/auth), [Firestore REST](https://firebase.google.com/docs/firestore/use-rest-api), [Google OAuth para apps nativas](https://developers.google.com/identity/protocols/oauth2/native-app).

## Registro Firebase

Proyecto `biblion-9e8ab`, aplicación `com.cristiancogollo.biblion.ios`, alias Biblion iOS. Configuración descargada con Firebase CLI y aplicada mediante `Scripts/configure-google.py`. No se cambiaron reglas de Firestore, permisos IAM ni plan de facturación.

## Verificación

16 pruebas XCTest completadas, 0 fallos en `reports/parity-tests.log`. Incluyen compatibilidad del archivo de lectura anterior. La compilación y las pruebas visuales de la versión final se registran en `reports/PARITY-VALIDATION.md` al completarse.

## Límites que requieren cierre antes de afirmar paridad completa

- Validar login real Google y correo, Keychain, perfil, foto y sincronización cruzada. No se crearon usuarios de prueba ni se solicitaron contraseñas.
- Comparar Bibi contra la batería Android vigente, especialmente políticas doctrinales, atribuciones institucionales, ambigüedad y memoria. El Worker mantiene sus políticas actuales, pero el enrutador local iOS es una implementación nueva.
- Probar iPad, accesibilidad ampliada, iOS anteriores a 26 y un iPhone físico.
- La firma para instalar en iPhone sigue siendo un asunto separado de la prueba en Appetize.

## Reversión

Todos los cambios de esta fase están bajo `biblion-ios/`. Las bases y el código Android no se modificaron. Los archivos JSON previos siguen siendo compatibles; los archivos por cuenta usan nombres distintos del archivo de invitado. Los cambios no se han comprometido a Git. La aplicación iOS registrada en Firebase es adicional a Android y no altera su registro.
