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

    private val patientTable = MyHashTable<Patient>(capacity = 101)
    private val doctorTree = MyAVLTree()
    private val appointmentsSkipList = MySkipList()

    private val _patients = MutableStateFlow<List<Patient>>(emptyList())
    val patients = _patients.asStateFlow()

    private val _doctors = MutableStateFlow<List<Doctor>>(emptyList())
    val doctors = _doctors.asStateFlow()

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments = _appointments.asStateFlow()

    // --- ВАЛИДАЦИЯ ФОРМАТА ---
    fun validatePatientData(id: String, fio: String): String? {
        if (id.isBlank() || fio.isBlank()) return "Поля не могут быть пустыми"
        if (!id.matches(Regex("^\\d{2}-\\d{6}$"))) return "Неверный формат ID (00-000000)"
        return null
    }

    // --- ГЕНЕРАТОР (20 ВРАЧЕЙ + ПАЦИЕНТЫ + ЗАПИСИ) ---
    fun generateTestData() {
        // Очищаем перед генерацией, чтобы не дублировать
        clearAllData()

        val pts = listOf(
            Patient("77-111222", "Иванов Иван", 1990, "Купчино", "Завод"),
            Patient("78-333444", "Петров Сидор", 1985, "Фрунзенская", "Офис"),
            Patient("99-555666", "Александров А.", 2000, "Центр", "Студент")
        )
        pts.forEach { patientTable.put(it.id, it) }

        val dcs = listOf(
            Doctor("Смирнов А.В.", "Терапевт", 101, "9:00-15:00"),
            Doctor("Васильев П.С.", "Хирург", 204, "12:00-18:00"),
            Doctor("Морозова Е.Н.", "Кардиолог", 305, "10:00-16:00"),
            Doctor("Павлов К.Д.", "Невролог", 401, "08:00-14:00"),
            Doctor("Соколова О.И.", "Окулист", 202, "14:00-20:00"),
            Doctor("Кузнецов М.А.", "ЛОР", 303, "09:00-17:00"),
            Doctor("Попова В.В.", "Стоматолог", 105, "10:00-15:00"),
            Doctor("Лебедев А.С.", "Уролог", 404, "11:00-19:00"),
            Doctor("Козлов Д.М.", "Дерматолог", 208, "08:00-13:00"),
            Doctor("Новикова С.А.", "Педиатр", 102, "12:00-18:00"),
            Doctor("Федоров И.П.", "Эндокринолог", 306, "09:00-14:00"),
            Doctor("Морозов В.Г.", "Гастроэнтеролог", 210, "13:00-19:00"),
            Doctor("Волкова Е.М.", "Акушер", 501, "08:00-16:00"),
            Doctor("Соловьев Р.Т.", "Психиатр", 405, "15:00-21:00"),
            Doctor("Андреева Н.С.", "Аллерголог", 108, "10:00-17:00"),
            Doctor("Николаев К.А.", "Онколог", 309, "09:00-15:00"),
            Doctor("Зайцев Б.В.", "Ревматолог", 212, "11:00-18:00"),
            Doctor("Егорова Л.Д.", "Инфекционист", 115, "08:00-14:00"),
            Doctor("Орлов С.С.", "Ортопед", 215, "12:00-20:00"),
            Doctor("Королева А.П.", "Гематолог", 312, "09:00-16:00")
        )
        dcs.forEach { doctorTree.insert(it) }

        // Добавляем пару записей для примера
        appointmentsSkipList.insert(Appointment("77-111222", "Смирнов А.В.", "25.03.2026", "10:00"))
        appointmentsSkipList.insert(Appointment("78-333444", "Васильев П.С.", "25.03.2026", "14:30"))

        refreshAll()
    }

    // --- ОПЕРАЦИИ ---
    fun registerPatient(p: Patient) { if (patientTable.put(p.id, p)) refreshPatients() }

    fun removePatient(id: String) {
        if (patientTable.remove(id)) {
            // Каскадное удаление (п. 9.4.13)
            appointmentsSkipList.getAll().filter { it.patientId == id }
                .forEach { appointmentsSkipList.remove(it.doctorFio, it.patientId) }
            refreshAll()
        }
    }

    fun registerDoctor(d: Doctor) { if (d.fio.isNotBlank()) { doctorTree.insert(d); refreshDoctors() } }

    fun removeDoctor(fio: String) {
        if (doctorTree.remove(fio)) {
            // Каскадное удаление (п. 9.4.13)
            appointmentsSkipList.getAll().filter { it.doctorFio == fio }
                .forEach { appointmentsSkipList.remove(it.doctorFio, it.patientId) }
            refreshAll()
        }
    }

    private fun directSearch(text: String, word: String): Boolean {
        if (word.isEmpty()) return true
        val n = text.length; val m = word.length
        if (m > n) return false
        for (i in 0..n - m) {
            var match = true
            for (j in 0 until m) {
                if (text[i + j].lowercaseChar() != word[j].lowercaseChar()) { match = false; break }
            }
            if (match) return true
        }
        return false
    }

    fun searchDoctorsByPosition(query: String) {
        _doctors.value = doctorTree.searchByPosition(query, ::directSearch)
    }

    // --- ПРОВЕРКА ССЫЛОЧНОЙ ЦЕЛОСТНОСТИ ---
    fun createAppointment(pId: String, dFio: String, date: String, time: String): String? {
        if (pId.isBlank() || dFio.isBlank()) return "Заполните все поля"

        // Проверка в Хеш-таблице
        if (patientTable.get(pId) == null) return "Больной $pId не найден!"

        // Проверка в АВЛ-дереве
        if (doctorTree.get(dFio) == null) return "Врач $dFio не найден!"

        // Проверка занятости (Skip List)
        if (appointmentsSkipList.isTimeBusy(dFio, date, time)) return "Время уже занято!"

        appointmentsSkipList.insert(Appointment(pId, dFio, date, time))
        refreshAppointments()
        return null
    }

    fun cancelAppointment(d: String, p: String) { if (appointmentsSkipList.remove(d, p)) refreshAppointments() }

    private fun refreshPatients() { _patients.value = patientTable.getAll() }
    private fun refreshDoctors() { _doctors.value = doctorTree.getAll() }
    private fun refreshAppointments() { _appointments.value = appointmentsSkipList.getAll() }
    private fun refreshAll() { refreshPatients(); refreshDoctors(); refreshAppointments() }

    fun clearAllData() {
        patientTable.clear(); doctorTree.clear(); appointmentsSkipList.clear()
        refreshAll()
    }
}