package com.example.kurs.data

import com.example.kurs.models.Appointment
import kotlin.random.Random

class MySkipList(private val maxLevel: Int = 4) {

    private class Node(
        val key: String, // ФИО врача (ключ сортировки)
        val value: Appointment,
        val next: Array<Node?>
    )

    private var head = Node("", Appointment("", "", "", ""), arrayOfNulls(maxLevel))
    private var currentLevels = 1

    // Вставка элемента в упорядоченный слоеный список (п. 4.5)
    fun insert(appointment: Appointment) {
        val update = arrayOfNulls<Node>(maxLevel)
        var curr = head
        val key = appointment.doctorFio

        // Поиск позиции для вставки на каждом уровне
        for (i in currentLevels - 1 downTo 0) {
            while (curr.next[i] != null && curr.next[i]!!.key < key) {
                curr = curr.next[i]!!
            }
            update[i] = curr
        }

        val level = randomLevel()
        if (level > currentLevels) {
            for (i in currentLevels until level) {
                update[i] = head
            }
            currentLevels = level
        }

        val newNode = Node(key, appointment, arrayOfNulls(level))
        for (i in 0 until level) {
            newNode.next[i] = update[i]?.next?.get(i)
            update[i]?.next?.set(i, newNode)
        }
    }

    // Рандомное определение высоты башни узла
    private fun randomLevel(): Int {
        var lvl = 1
        while (Random.nextFloat() < 0.5 && lvl < maxLevel) lvl++
        return lvl
    }

    // Удаление направления (п. 9.4.8)
    fun remove(doctorFio: String, patientId: String): Boolean {
        val update = arrayOfNulls<Node>(maxLevel)
        var curr = head
        for (i in currentLevels - 1 downTo 0) {
            while (curr.next[i] != null && (curr.next[i]!!.key < doctorFio ||
                        (curr.next[i]!!.key == doctorFio && curr.next[i]!!.value.patientId != patientId))) {
                curr = curr.next[i]!!
            }
            update[i] = curr
        }

        val target = curr.next[0]
        if (target != null && target.key == doctorFio && target.value.patientId == patientId) {
            for (i in 0 until currentLevels) {
                if (update[i]?.next?.get(i) != target) break
                update[i]?.next?.set(i, target.next[i])
            }
            return true
        }
        return false
    }

    // Проверка занятости времени (п. 9.4.12)
    fun isTimeBusy(doctorFio: String, date: String, time: String): Boolean {
        var curr = head.next[0]
        while (curr != null) {
            if (curr.key == doctorFio && curr.value.date == date && curr.value.time == time) return true
            curr = curr.next[0]
        }
        return false
    }

    fun getAll(): List<Appointment> {
        val list = mutableListOf<Appointment>()
        var curr = head.next[0]
        while (curr != null) {
            list.add(curr.value)
            curr = curr.next[0]
        }
        return list
    }

    fun clear() {
        head = Node("", Appointment("", "", "", ""), arrayOfNulls(maxLevel))
        currentLevels = 1
    }
}