package com.cristiancogollo.biblion.data.repository.cache

/**
 * Caché LRU thread-safe basada en [LinkedHashMap] con [accessOrder] habilitado.
 */
class BibleLruCache<K, V>(private val maxSize: Int) {
    init {
        require(maxSize > 0) { "maxSize must be greater than 0" }
    }

    private val map = object : LinkedHashMap<K, V>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > maxSize
        }
    }

    private val lock = Any()

    operator fun get(key: K): V? = synchronized(lock) {
        map[key]
    }

    operator fun set(key: K, value: V) {
        synchronized(lock) {
            map[key] = value
        }
    }

    fun remove(key: K): V? = synchronized(lock) {
        map.remove(key)
    }

    fun clear() {
        synchronized(lock) {
            map.clear()
        }
    }

    fun containsKey(key: K): Boolean = synchronized(lock) {
        map.containsKey(key)
    }

    fun keysInLruOrder(): List<K> = synchronized(lock) {
        map.keys.toList()
    }
}
