package com.example.kurs.data

class MyHashTable<V>(private val capacity: Int = 101) {

    private enum class Status { FREE, ACTIVE, DELETED }

    private data class Entry<V>(
        var key: String? = null,
        var value: V? = null,
        var status: Status = Status.FREE
    )

    private var table = Array(capacity) { Entry<V>() }
    private var size = 0

    // Основная хеш-функция (h1)
    private fun hash1(key: String): Int = Math.abs(key.hashCode()) % capacity

    // Вторая хеш-функция для шага (h2) - двойное хеширование (рис. 2.6)
    private fun hash2(key: String): Int = 1 + (Math.abs(key.hashCode()) % (capacity - 1))

    // Вставка (алгоритм из пункта 2.1)
    fun put(key: String, value: V): Boolean {
        if (size >= capacity) return false
        var index = hash1(key)
        val step = hash2(key)

        while (table[index].status == Status.ACTIVE) {
            if (table[index].key == key) return false // Ключ уже есть
            index = (index + step) % capacity
        }

        table[index] = Entry(key, value, Status.ACTIVE)
        size++
        return true
    }

    // Поиск (алгоритм из пункта 2.1)
    fun get(key: String): V? {
        var index = hash1(key)
        val step = hash2(key)
        val start = index

        do {
            if (table[index].status == Status.FREE) return null
            if (table[index].status == Status.ACTIVE && table[index].key == key) {
                return table[index].value
            }
            index = (index + step) % capacity
        } while (index != start)
        return null
    }

    // Удаление с использованием состояния "удалено" (пункт 2.1)
    fun remove(key: String): Boolean {
        var index = hash1(key)
        val step = hash2(key)
        val start = index

        do {
            if (table[index].status == Status.FREE) return false
            if (table[index].status == Status.ACTIVE && table[index].key == key) {
                table[index].status = Status.DELETED
                size--
                return true
            }
            index = (index + step) % capacity
        } while (index != start)
        return false
    }

    fun getAll(): List<V> = table.filter { it.status == Status.ACTIVE }.mapNotNull { it.value }

    fun clear() {
        table = Array(capacity) { Entry() }
        size = 0
    }
}