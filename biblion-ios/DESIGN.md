# Dirección de la primera versión

## Alcance

Lectura bíblica local primero. El usuario puede comenzar en Génesis o retomar la posición anterior, elegir testamento/libro/capítulo y volver a un resaltado. El modo estudio queda para la última etapa. Se preservan las funciones de Android; la navegación y los controles se implementan de forma nativa para iOS.

## Pantallas y recorrido

```text
Inicio → Antiguo/Nuevo → Libros → Capítulos → Lector
   └── Continuar lectura ────────────────────┘
Biblia → Libros → Capítulos → Lector
Resaltados → Versículo en su capítulo
Ajustes → Versión, tamaño, tema
```

Barra inferior con cuatro destinos reales: Inicio, Biblia, Resaltados y Ajustes. El lector usa navegación hacia atrás de iOS y controles visibles de capítulo; no captura el gesto lateral del sistema.

## Identidad y lectura

- Azul de marca para navegación, dorado para acentos y superficies blancas/cálidas. Modo oscuro con fondo azul profundo; colores centralizados en `Theme.swift`.
- Controles con tipografía del sistema. Texto bíblico en Georgia con escala relativa a Dynamic Type. Ancho de lectura limitado a 760 puntos en iPad.
- Márgenes de 16/20/24 puntos y controles táctiles de al menos 44 puntos.
- Sin animaciones decorativas, dependencia de red ni descargas de fuentes.
- Iconos SF Symbols; el logo y el icono provienen de los assets oficiales locales.
- Las variantes de tema mantienen identidad de Biblion. No se aplican paletas dinámicas del sistema.

## Estados

- Arranque: progreso, error y reintento, sin sobrescribir datos corruptos.
- Libros: filtro en vivo y estado vacío con explicación.
- Lector: carga, error/reintento, capítulo no disponible y bordes del catálogo con botones deshabilitados.
- Resaltados: estado vacío con instrucciones y lista navegable con opción de quitar.
- Persistencia: aviso y reintento si falla una escritura.
- Selector de versión: identifica contenido parcial y deshabilita versiones que no contienen el capítulo actual.

## Guías y límites de evidencia

Se consultó `ui-ux-pro-max` para SwiftUI: navegación tipada, Dynamic Type y cierre nativo de hojas. La primera búsqueda general devolvió recomendaciones de landing page y una paleta roja no pertinentes; se descartaron y se acotó la consulta a SwiftUI. La identidad vigente del proyecto prevalece.

Las referencias compartidas de privacidad/reporte de `senior-ui-ux-orchestrator` no están presentes en las rutas indicadas por esa skill. Se aplicaron sus reglas autocontenidas: trabajo local, ningún envío de código/assets a servicios de diseño y evidencia explícita de lo ejecutado.

No se dispone de ejecución de iOS en este equipo Windows. La revisión visual de SwiftUI, VoiceOver, Dynamic Type extremo, orientación y comportamiento del scroll se validará en un iPhone/iPad firmado. No se presentan maquetas de navegador como capturas de la aplicación nativa.

Actualización del 18 de octubre: se consiguió ejecutar la compilación nativa para simulador en Appetize sobre iOS 26.0, desde el navegador de Windows. Se comprobaron inicio, libros/capítulos, lectura y resaltados; las capturas reales y los límites de esa prueba están en `reports/APPETIZE.md`. Sigue pendiente la prueba física y de accesibilidad completa.
