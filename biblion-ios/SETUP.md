# Entorno de Biblion iOS

El desarrollo se realiza con Swift y xtool dentro de Ubuntu en WSL 2.
Esta carpeta contiene la primera aplicación nativa de Biblion para iOS. Ver `README.md` para compilarla y revisar su alcance.

## Estado del entorno

- Ubuntu en WSL 2.
- Swift 6.4.1 y xtool 1.22.2 instalados.
- USBIPD 5.3.0 y usbmuxd instalados.
- SDK Darwin instalado desde `C:\Users\LENOVO LOQ\Downloads\Xcode_27.xip`.
- SDKs incluidos: iPhoneOS 27.0, iPhoneSimulator 27.0 y macOS 27.0. La inclusión del SDK de simulador no proporciona un simulador ejecutable en Windows.
- Pendiente: autenticación de Apple, firma y conexión de un iPhone para pruebas físicas.

## Verificar herramientas

Abrir Ubuntu y ejecutar:

```sh
swift --version
xtool --version
swift sdk list
```

## Completar el SDK de Apple

1. Descargar Xcode 27 en formato `.xip` desde https://developer.apple.com/download/all/?q=Xcode usando una cuenta de Apple. La autenticación y aceptación de la licencia se realizan personalmente en el navegador.
2. Desde Ubuntu, instalar el SDK indicando la ruta real del archivo descargado:

```sh
xtool sdk install '/mnt/c/Users/LENOVO LOQ/Downloads/Xcode_27.xip'
swift sdk list
```

El nombre del archivo es ilustrativo: sustituirlo por el descargado. La lista debe incluir `darwin`.

## Firma y pruebas en iPhone

Ejecutar `xtool setup` en Ubuntu para configurar la cuenta de Apple de forma interactiva. No guardar contraseñas, códigos de verificación ni claves en el repositorio o en el chat.

Conectar el iPhone por USB, desbloquearlo y aceptar la confianza en el equipo. USBIPD permite compartir el dispositivo con WSL; revisar `usbipd list` en Windows y compartir únicamente el dispositivo elegido. Activar el modo desarrollador en el iPhone cuando sea requerido.

## Fuentes oficiales

- https://xtool.sh/documentation/xtool/installation-linux/
- https://www.swift.org/install/linux/
- https://learn.microsoft.com/windows/wsl/connect-usb
