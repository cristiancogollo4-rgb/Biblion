package com.cristiancogollo.biblion

enum class Testament(
    val label: String,
    val routeArg: String,
    val shortLabel: String
) {
    OLD(
        label = "Antiguo Testamento",
        routeArg = "ANTIGUO TESTAMENTO",
        shortLabel = "ANTIGUO"
    ),
    NEW(
        label = "Nuevo Testamento",
        routeArg = "NUEVO TESTAMENTO",
        shortLabel = "NUEVO"
    );

    fun toRouteArg(): String = encodeArg(routeArg)

    companion object {
        fun fromRouteArg(raw: String?): Testament {
            val decoded = decodeArg(raw.orEmpty()).trim().uppercase()
            return entries.firstOrNull { it.routeArg == decoded } ?: OLD
        }
    }
}
