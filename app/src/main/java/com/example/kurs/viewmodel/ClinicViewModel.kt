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
    private val patientTable = MyHashTable<Patient>(capacity = 101)
    private val doctorTree = MyAVLTree()
    private val appointmentsSkipList = MySkipList()

    // Состояния для UI
    private val _patients = MutableStateFlow<List<Patient>>(emptyList())
    val patients = _patients.asStateFlow()

    private val _doctors = MutableStateFlow<List<Doctor>>(emptyList())
    val doctors = _doctors.asStateFlow()

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments = _appointments.asStateFlow()

    // --- ВАЛИДАЦИЯ (Новое) ---
    fun validatePatientData(id: String, fio: String): String? {
        if (id.isBlank() || fio.isBlank()) return "Поля не могут быть пустыми"
        // Проверка формата MM-NNNNNN
        val regex = Regex("^\\d{2}-\\d{6}$")
        if (!id.matches(regex)) return "Неверный формат ID (нужно 00-000000)"
        return null
    }

    // --- ГЕНЕРАТОР ТЕСТОВЫХ ДАННЫХ (Новое) ---
    fun generateTestData() {
        val pts = listOf(
            Patient("77-111222", "Иванов Иван", 1990, "Купчино", "Завод"),
            Patient("78-333444", "Петров Сидор", 1985, "Фрунзенская", "Офис"),
            Patient("99-555666", "Александров А.", 2000, "Центр", "Студент")
        )
        pts.forEach { patientTable.put(it.id, it) }

        val dcs = listOf(
            Doctor("Смирнов А.В.", "Терапевт", 101, "9:00-15:00"),
            Doctor("Васильев П.С.", "Хирург", 204, "12:00-18:00"),
            Doctor("Морозова Е.Н.", "Кардиолог", 305, "10:00-16:00")
        )
        dcs.forEach { doctorTree.insert(it) }
        refreshAll()
    }

    // --- ОПЕРАЦИИ С БОЛЬНЫМИ ---
    fun registerPatient(patient: Patient) {
        if (patientTable.put(patient.id, patient)) {
            refreshPatients()
        }
    }

    fun removePatient(id: String) {
        if (patientTable.remove(id)) {
            val relatedApps = appointmentsSkipList.getAll().filter { it.patientId == id }
            relatedApps.forEach { appointmentsSkipList.remove(it.doctorFio, it.patientId) }
            refreshPatients()
            refreshAppointments()
        }
    }

    // --- ОПЕРАЦИИ С ВРАЧАМИ ---
    fun registerDoctor(doctor: Doctor) {
        if (doctor.fio.isNotBlank()) {
            doctorTree.insert(doctor)
            refreshDoctors()
        }
    }

    fun removeDoctor(fio: String) {
        if (doctorTree.remove(fio)) {
            val relatedApps = appointmentsSkipList.getAll().filter { it.doctorFio == fio }
            relatedApps.forEach { appointmentsSkipList.remove(it.doctorFio, it.patientId) }
            refreshDoctors()
            refreshAppointments()
        }
    }

    /**
     * ТВОЙ АЛГОРИТМ ПРЯМОГО ПОИСКА (п. 7.1) - ОСТАВИЛ БЕЗ ИЗМЕНЕНИЙ
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

    fun searchDoctorsByPosition(query: String) {
        _doctors.value = doctorTree.searchByPosition(query, ::directSearch)
    }

    // --- ОПЕРАЦИИ С НАПРАВЛЕНИЯМИ ---
    fun createAppointment(patientId: String, doctorFio: String, date: String, time: String): String? {
        if (patientId.isBlank() || doctorFio.isBlank()) return "Заполните все поля"
        // Проверка занятости времени (п. 9.4.12)
        if (appointmentsSkipList.isTimeBusy(doctorFio, date, time)) return "Это время уже занято!"

        val newApp = Appointment(patientId, doctorFio, date, time)
        appointmentsSkipList.insert(newApp)
        refreshAppointments()
        return null // Ошибок нет
    }

    fun cancelAppointment(doctorFio: String, patientId: String) {
        if (appointmentsSkipList.remove(doctorFio, patientId)) {
            refreshAppointments()
        }
    }

    // --- СЕРВИСНЫЕ ФУНКЦИИ ---
    private fun refreshPatients() { _patients.value = patientTable.getAll() }
    private fun refreshDoctors() { _doctors.value = doctorTree.getAll() }
    private fun refreshAppointments() { _appointments.value = appointmentsSkipList.getAll() }

    private fun refreshAll() {
        refreshPatients()
        refreshDoctors()
        refreshAppointments()
    }

    fun clearAllData() {
        patientTable.clear()
        doctorTree.clear()
        appointmentsSkipList.clear()
        refreshAll()
    }
}