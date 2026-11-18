import { defineConfig } from 'vitepress'

export default defineConfig({
  lang: 'es-CO',
  title: 'Biblion · Fase 1',
  description: 'Guías de uso y documentación técnica de la fase 1 de Biblion.',
  lastUpdated: true,
  head: [
    ['meta', { name: 'theme-color', content: '#103250' }],
    ['link', { rel: 'icon', href: '/brand/biblion-light.png' }],
  ],
  themeConfig: {
    logo: {
      light: '/brand/biblion-light.png',
      dark: '/brand/biblion-dark.png',
      alt: '',
    },
    siteTitle: 'Biblion',
    nav: [
      { text: 'Inicio', link: '/' },
      { text: 'Usar Biblion', link: '/uso/' },
      { text: 'Desarrollar Biblion', link: '/desarrollo/' },
    ],
    sidebar: {
      '/uso/': [
        {
          text: 'Guía de uso',
          items: [
            { text: 'Primeros pasos', link: '/uso/' },
            { text: 'Lectura bíblica', link: '/uso/lectura' },
            { text: 'Buscar y explorar', link: '/uso/explorar' },
            { text: 'Consultar a Bibi', link: '/uso/bibi' },
            { text: 'Crear enseñanzas', link: '/uso/estudio' },
            { text: 'Cuenta, datos y uso sin conexión', link: '/uso/cuenta-y-datos' },
          ],
        },
      ],
      '/desarrollo/': [
        {
          text: 'Guía técnica',
          items: [
            { text: 'Mapa técnico', link: '/desarrollo/' },
            { text: 'Cómo seguir el código', link: '/desarrollo/arquitectura' },
            { text: 'Inicio, navegación y tutorial', link: '/desarrollo/navegacion' },
            { text: 'Cuenta, perfil y preferencias', link: '/desarrollo/cuenta' },
            { text: 'Lectura bíblica', link: '/desarrollo/lectura' },
            { text: 'Búsqueda y exploración', link: '/desarrollo/busqueda' },
            { text: 'Bibi', link: '/desarrollo/bibi' },
            { text: 'Editor de enseñanzas', link: '/desarrollo/estudio' },
            { text: 'Datos y sincronización', link: '/desarrollo/datos' },
            { text: 'Pruebas y validación', link: '/desarrollo/pruebas' },
            { text: 'Mantenimiento', link: '/desarrollo/mantenimiento' },
          ],
        },
      ],
    },
    search: {
      provider: 'local',
      options: {
        locales: {
          root: {
            translations: {
              button: {
                buttonText: 'Buscar',
                buttonAriaLabel: 'Buscar en la documentación',
              },
              modal: {
                displayDetails: 'Mostrar detalles',
                resetButtonTitle: 'Limpiar búsqueda',
                backButtonTitle: 'Cerrar búsqueda',
                noResultsText: 'No hay resultados',
                footer: {
                  selectText: 'seleccionar',
                  selectKeyAriaLabel: 'Intro',
                  navigateText: 'navegar',
                  navigateUpKeyAriaLabel: 'flecha arriba',
                  navigateDownKeyAriaLabel: 'flecha abajo',
                  closeText: 'cerrar',
                  closeKeyAriaLabel: 'Esc',
                },
              },
            },
          },
        },
      },
    },
    outline: { label: 'En esta página', level: [2, 3] },
    skipToContentLabel: 'Ir al contenido',
    docFooter: { prev: 'Anterior', next: 'Siguiente' },
    returnToTopLabel: 'Volver arriba',
    sidebarMenuLabel: 'Menú',
    mobileMenuLabel: 'Menú',
    darkModeSwitchLabel: 'Apariencia',
    lightModeSwitchTitle: 'Tema claro',
    darkModeSwitchTitle: 'Tema oscuro',
    footer: {
      message: 'Documentación local de la fase 1 · Biblion App',
      copyright: 'En desarrollo activo',
    },
  },
})
