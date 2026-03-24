package com.example.kurs.viewmodel

import androidx.lifecycle.ViewModel
import com.example.kurs.data.MyAVLTree
import com.example.kurs.data.MyHashTable
import com.example.kurs.data.MySkipList
import com.example.kurs.models.Appointment
import com.example.kurs.models.Doctor
import com.example.kurs.models.Patient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ClinicViewModel : ViewModel() {

    // Инициализация авторских структур данных (Вариант 279)
    private val patientTable = MyHashTable<Patient>(capacity = 101) // Закрытое двойное хеширование [cite: 1241]
    private val doctorTree = MyAVLTree()                            // АВЛ-дерево поиска [cite: 1024, 1513]
    private val appointmentsSkipList = MySkipList()                // Слоеный список [cite: 801, 1528]

    // Состояния для UI (Jetpack Compose)
    private val _patients = MutableStateFlow<List<Patient>>(emptyList())
    val patients = _patients.asStateFlow()

    private val _doctors = MutableStateFlow<List<Doctor>>(emptyList())
    val doctors = _doctors.asStateFlow()

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments = _appointments.asStateFlow()

    // --- ОПЕРАЦИИ С БОЛЬНЫМИ (п. 9.4.8) ---

    fun registerPatient(patient: Patient) {
        if (patientTable.put(patient.id, patient)) {
            refreshPatients()
        }
    }

    fun removePatient(id: String) {
        if (patientTable.remove(id)) {
            // Каскадное удаление направлений при удалении больного (п. 9.4.13) [cite: 1553]
            val relatedApps = appointmentsSkipList.getAll().filter { it.patientId == id }
            relatedApps.forEach { appointmentsSkipList.remove(it.doctorFio, it.patientId) }

            refreshPatients()
            refreshAppointments()
        }
    }

    private fun refreshPatients() {
        _patients.value = patientTable.getAll()
    }

    // --- ОПЕРАЦИИ С ВРАЧАМИ (п. 9.4.8) ---

    fun registerDoctor(doctor: Doctor) {
        doctorTree.insert(doctor)
        refreshDoctors()
    }

    fun removeDoctor(fio: String) {
        if (doctorTree.remove(fio)) {
            // Обработка ситуации, когда к врачу есть записи (п. 9.4.13)
            val relatedApps = appointmentsSkipList.getAll().filter { it.doctorFio == fio }
            relatedApps.forEach { appointmentsSkipList.remove(it.doctorFio, it.patientId) }

            refreshDoctors()
            refreshAppointments()
        }
    }

    /**
     * АЛГОРИТМ ПРЯМОГО ПОИСКА (п. 7.1) [cite: 1164]
     * Поиск фрагмента 'word' в тексте 'text' [cite: 1165]
     */
    private fun directSearch(text: String, word: String): Boolean {
        if (word.isEmpty()) return true
        val n = text.length
        val m = word.length
        if (m > n) return false

        for (i in 0..n - m) {
            var match = true
            for (j in 0 until m) {
                if (text[i + j].lowercaseChar() != word[j].lowercaseChar()) {
                    match = false
                    break
                }
            }
            if (match) return true
        }
        return false
    }

    // Поиск врачей по должности через симметричный обход дерева (п. 9.4.11) [cite: 1547]
    fun searchDoctorsByPosition(query: String) {
        // Передаем алгоритм прямого поиска в метод обхода дерева [cite: 1549]
        _doctors.value = doctorTree.searchByPosition(query, ::directSearch)
    }

    private fun refreshDoctors() {
        _doctors.value = doctorTree.getAll()
    }

    // --- ОПЕРАЦИИ С НАПРАВЛЕНИЯМИ (п. 9.4.8) ---

    fun createAppointment(patientId: String, doctorFio: String, date: String, time: String): Boolean {
        // Проверка на отсутствие уже выданного направления на то же время (п. 9.4.12)
        if (appointmentsSkipList.isTimeBusy(doctorFio, date, time)) return false

        val newApp = Appointment(patientId, doctorFio, date, time)
        appointmentsSkipList.insert(newApp)
        refreshAppointments()
        return true
    }

    fun cancelAppointment(doctorFio: String, patientId: String) {
        if (appointmentsSkipList.remove(doctorFio, patientId)) {
            refreshAppointments()
        }
    }

    private fun refreshAppointments() {
        _appointments.value = appointmentsSkipList.getAll()
    }

    // --- СЕРВИСНЫЕ ФУНКЦИИ (п. 9.4.8) ---

    fun clearAllData() {
        patientTable.clear()   // Очистка данных о больных [cite: 1534]
        doctorTree.clear()     // Очистка данных о врачах [cite: 1541]
        appointmentsSkipList.clear()
        refreshPatients()
        refreshDoctors()
        refreshAppointments()
    }
}