package com.example.kurs.models

// Данные о больном (п. 9.4.2)
data class Patient(
    val id: String,          // Регистрационный номер «MM-NNNNNN»
    val fio: String,         // ФИО
    val birthYear: Int,      // Год рождения
    val address: String,     // Адрес
    val workPlace: String    // Место работы
)

// Данные о враче (п. 9.4.4)
data class Doctor(
    val fio: String,         // ФИО врача (до 25 символов)
    val position: String,    // Должность
    val cabinet: Int,        // Номер кабинета
    val schedule: String     // График приема
)

// Данные о направлении (п. 9.4.6)
data class Appointment(
    val patientId: String,   // Рег. номер больного
    val doctorFio: String,   // ФИО врача
    val date: String,        // Дата
    val time: String         // Время
)