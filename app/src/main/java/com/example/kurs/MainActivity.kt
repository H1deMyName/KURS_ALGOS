package com.example.kurs

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kurs.models.Doctor
import com.example.kurs.models.Patient
import com.example.kurs.ui.theme.KursTheme
import com.example.kurs.viewmodel.ClinicViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KursTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: ClinicViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Поликлиника 279") },
                actions = {
                    // НОВОЕ: Кнопка генерации тестовых данных
                    IconButton(onClick = { vm.generateTestData() }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Заполнить данными")
                    }
                    IconButton(onClick = { vm.clearAllData() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Очистить всё")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Больные") },
                    icon = { Icon(Icons.Default.Person, null) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("Врачи") },
                    icon = { Icon(Icons.Default.MedicalServices, null) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    label = { Text("Записи") },
                    icon = { Icon(Icons.Default.DateRange, null) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> PatientScreen(vm)
                1 -> DoctorScreen(vm)
                2 -> AppointmentScreen(vm)
            }
        }
    }
}

@Composable
fun PatientScreen(vm: ClinicViewModel) {
    val patients by vm.patients.collectAsState()
    var id by remember { mutableStateOf("") }
    var fio by remember { mutableStateOf("") }
    val context = LocalContext.current // Для показа Toast

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Регистрация больного", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = id,
            onValueChange = { id = it },
            label = { Text("Номер (MM-NNNNNN)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = fio,
            onValueChange = { fio = it },
            label = { Text("ФИО пациента") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                // НОВОЕ: Проверка данных перед добавлением
                val error = vm.validatePatientData(id, fio)
                if (error == null) {
                    vm.registerPatient(Patient(id, fio, 2000, "Ул. Пушкина", "Завод"))
                    id = ""; fio = ""
                } else {
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.padding(top = 8.dp).align(Alignment.End)
        ) { Text("Добавить") }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        LazyColumn {
            items(patients) { p ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.fio, style = MaterialTheme.typography.bodyLarge)
                            Text(p.id, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { vm.removePatient(p.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DoctorScreen(vm: ClinicViewModel) {
    val doctors by vm.doctors.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Реестр врачей", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                vm.searchDoctorsByPosition(it)
            },
            label = { Text("Поиск по должности") },
            modifier = Modifier.fillMaxWidth()
        )

        // Можно оставить как тест, но теперь у нас есть кнопка "Генерация" в шапке
        Button(
            onClick = { vm.registerDoctor(Doctor("Петров И.С.", "Терапевт", 204, "8:00-14:00")) },
            modifier = Modifier.padding(top = 8.dp).align(Alignment.End)
        ) { Text("Тест: Добавить врача") }

        LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
            items(doctors) { d ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(d.fio, style = MaterialTheme.typography.bodyLarge)
                        Text("${d.position} | Каб. ${d.cabinet}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun AppointmentScreen(vm: ClinicViewModel) {
    val appointments by vm.appointments.collectAsState()
    val context = LocalContext.current

    var pId by remember { mutableStateOf("") }
    var dFio by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("10:00") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Выдача направления", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = pId, onValueChange = { pId = it }, label = { Text("ID Больного") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = dFio, onValueChange = { dFio = it }, label = { Text("ФИО Врача") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Время") }, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                // ОБНОВЛЕНО: Используем новую логику возврата ошибки строкой
                val error = vm.createAppointment(pId, dFio, "25.03.2026", time)
                if (error != null) {
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                } else {
                    pId = ""; dFio = ""
                }
            },
            modifier = Modifier.padding(top = 8.dp).align(Alignment.End)
        ) { Text("Записать") }

        Text("Журнал записей (Skip List)", modifier = Modifier.padding(top = 16.dp), style = MaterialTheme.typography.labelLarge)

        LazyColumn {
            items(appointments) { app ->
                ListItem(
                    headlineContent = { Text("К врачу: ${app.doctorFio}") },
                    supportingContent = { Text("Больной: ${app.patientId} в ${app.time}") },
                    trailingContent = {
                        IconButton(onClick = { vm.cancelAppointment(app.doctorFio, app.patientId) }) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                )
            }
        }
    }
}