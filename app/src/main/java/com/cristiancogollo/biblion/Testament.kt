package com.cristiancogollo.biblion

enum class Testament(
    val labelRes: Int,
    val routeArg: String,
    val shortLabelRes: Int
) {
    OLD(
        labelRes = R.string.testament_old_label,
        routeArg = "ANTIGUO TESTAMENTO",
        shortLabelRes = R.string.testament_old_short_label
    ),
    NEW(
        labelRes = R.string.testament_new_label,
        routeArg = "NUEVO TESTAMENTO",
        shortLabelRes = R.string.testament_new_short_label
    );

    fun toRouteArg(): String = encodeArg(routeArg)

    companion object {
        fun fromRouteArg(raw: String?): Testament {
            val decoded = decodeArg(raw.orEmpty()).trim().uppercase()
            return entries.firstOrNull { it.routeArg == decoded } ?: OLD
        }
    }
}
