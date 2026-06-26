package com.cristiancogollo.biblion.feature.search.model

import androidx.annotation.StringRes
import com.cristiancogollo.biblion.R

/**
 * Ambito de busqueda en la pantalla de Buscar.
 *
 * - [BIBLE]: comportamiento clasico: versiculos biblicos + temas canonicos.
 * - [DICTIONARY]: busqueda en el diccionario biblico (terminos, personas, lugares).
 */
enum class SearchScope(@StringRes val labelRes: Int) {
    BIBLE(R.string.search_scope_bible),
    DICTIONARY(R.string.search_scope_dictionary);

    companion object {
        /**
         * Parsea el valor string del argumento de navegacion.
         * Devuelve [BIBLE] por defecto si el valor es null o desconocido.
         */
        fun fromRouteArg(value: String?): SearchScope = when (value?.lowercase()) {
            "dictionary" -> DICTIONARY
            else -> BIBLE
        }
    }
}
