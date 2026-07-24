package com.cristiancogollo.biblion.feature.studydocs.ui.pagination

/**
 * Modo de vista del lienzo del editor de estudio.
 *
 * - [PAGINATED]: hojas fisicas carta (850x1100dp) con paginacion visual estilo
 *   Word/Google Docs. Es el modo por defecto y el que se usa para impresion/PDF.
 *
 * - [PAGELESS]: lienzo continuo estilo Notion, sin hojas ni separadores, ancho
 *   adaptativo al contenedor. Pensado para edicion/lectura tactil en movil/tablet.
 *
 * El modelo [com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc] es
 * 100% independiente del modo de vista: el cambio entre PAGINATED y PAGELESS
 * es puramente visual.
 */
enum class SheetViewMode {
    PAGINATED,
    PAGELESS,
}
