# Firma de desarrollo con cuenta gratuita

La compilación de Biblion no requiere una cuenta de Apple. La instalación en un iPhone sí requiere firma y un perfil de aprovisionamiento.

## Copia de xtool con alternativa al fallo de SMS

Se prepara una copia independiente a partir del tag oficial `1.22.2` (commit `65bf3cc5a1240fea53ac0748d29a01878222a6ce`) más el parche del commit `c719f26c6af58e0e27c3e7d32d57810ce6974b77`, propuesto en https://github.com/xtool-org/xtool/pull/349.

El parche agrega una alternativa cuando falla la solicitud de SMS: permite introducir un código de un dispositivo de confianza de la misma cuenta. Sigue siendo una corrección experimental, no incorporada a la versión oficial al momento de prepararla. No garantiza que Apple acepte la autenticación.

El código se encuentra en Ubuntu bajo `~/biblion-ios-tools/xtool-trusted`. La instalación original `xtool` se conserva. El comando independiente instalado es `xtool-trusted`.

Compilación completada con Swift 6.4.1. Se verificaron `xtool-trusted --version` y `xtool-trusted auth login --help`; la versión muestra `1.22.2 (dirty)` por el parche local revisado. Todavía no se ha verificado un inicio de sesión, una firma o una instalación con la cuenta del propietario.

## Inicio de sesión personal en Ubuntu

El titular de la cuenta debe introducir personalmente sus credenciales y su código en la terminal; no compartirlos en el chat ni guardarlos en este repositorio. En un iPhone prestado, solo usar la cuenta del propietario si este lo autorizó. No hace falta cambiar la cuenta de iCloud del teléfono para firmar con ella.

Una vez instalado el comando independiente:

```sh
xtool-trusted auth login --mode password
```

Si aparece una notificación de inicio de sesión en el dispositivo de confianza, el titular puede aceptarla y escribir el código en la terminal. Si el parche pide generar un código manualmente, buscar en Ajustes → nombre de la cuenta → Inicio de sesión y seguridad → Autenticación de doble factor → Obtener código de verificación, si la opción está disponible. El código debe pertenecer a la cuenta introducida en xtool.

La referencia de Apple para la verificación es https://support.apple.com/es-es/102606.

Si se muestra una selección de equipos, elegir el equipo personal autorizado. Si no se encuentra ningún equipo de desarrollo, el titular debe revisar su cuenta gratuita en https://developer.apple.com/account/ y completar los acuerdos que Apple le presente.

La terminal debe confirmar `Logged in`. No copiar al chat la salida completa si incluye datos de sesión. Para comprobar el estado localmente:

```sh
xtool-trusted auth status
```

## Conectar y probar

Conectar el iPhone por USB, desbloquearlo y aceptar la confianza en el equipo. Desde Windows se identifica el BUSID real con `usbipd list`; después se comparte únicamente ese dispositivo y se adjunta a WSL. No reutilizar un BUSID de otro ordenador.

Una vez autenticado xtool y detectado el iPhone:

```sh
cd '/mnt/c/Users/LENOVO LOQ/Desktop/PROYECTOS/Biblion/biblion-ios'
xtool-trusted devices
xtool-trusted dev run
```

Revisar cualquier solicitud relativa a certificados existentes antes de aceptarla: no revocar certificados del propietario que estén en uso por otras aplicaciones. La firma e instalación solo se consideran verificadas cuando el flujo real se complete y Biblion abra en el dispositivo.

El modo desarrollador y la confianza en el perfil pueden requerir acciones en Ajustes del iPhone. La cuenta gratuita tiene restricciones de aprovisionamiento y no habilita la distribución mediante TestFlight o App Store.

## Cerrar sesión al terminar

Cuando ya no se necesite mantener la sesión del propietario en este equipo:

```sh
xtool-trusted auth logout
```

La copia independiente y el xtool original utilizan la configuración de cuenta de xtool; no asumir que mantienen sesiones de Apple separadas.
