package com.example.clockapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.OutlinedButton
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : ComponentActivity() {

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            MaterialTheme { App() }
        }
    }
}

@Composable
fun App() {
    var tab by remember { mutableIntStateOf(0) }
    val stopwatch = remember { StopwatchState() }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.AccessTime, null) },
                    label = { Text("ساعت") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Alarm, null) },
                    label = { Text("آلارم") }
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.HourglassBottom, null) },
                    label = { Text("تایمر") }
                )
                NavigationBarItem(
                    selected = tab == 3, onClick = { tab = 3 },
                    icon = { Icon(Icons.Filled.Timer, null) },
                    label = { Text("کرونومتر") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> ClockScreen()
                1 -> AlarmsScreen()
                2 -> TimerScreen()
                else -> StopwatchScreen(stopwatch)
            }
        }
    }
}

// ───────────────────────── صفحه ساعت ─────────────────────────

@Composable
fun ClockScreen() {
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(1000)
        }
    }
    val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val dateFmt = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault())

    val zones = listOf(
        "تهران" to "Asia/Tehran",
        "دبی" to "Asia/Dubai",
        "لندن" to "Europe/London",
        "نیویورک" to "America/New_York",
        "توکیو" to "Asia/Tokyo"
    )

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            timeFmt.format(now),
            fontSize = 64.sp,
            fontWeight = FontWeight.Light
        )
        Text(Jalali.format(now), fontSize = 20.sp)
        Text(dateFmt.format(now), fontSize = 14.sp)
        Spacer(Modifier.height(32.dp))
        Text("ساعت جهانی", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(zones) { (name, id) ->
                val f = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone(id)
                }
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(name, fontSize = 18.sp)
                        Text(f.format(now), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ───────────────────────── صفحه آلارم‌ها ─────────────────────────

// ترتیب هفته ایرانی: شنبه تا جمعه
private val weekDays = listOf(
    Calendar.SATURDAY to "ش",
    Calendar.SUNDAY to "ی",
    Calendar.MONDAY to "د",
    Calendar.TUESDAY to "س",
    Calendar.WEDNESDAY to "چ",
    Calendar.THURSDAY to "پ",
    Calendar.FRIDAY to "ج"
)

@Composable
fun AlarmsScreen() {
    val context = LocalContext.current
    val store = remember { AlarmStore(context) }
    val alarms: SnapshotStateList<Alarm> = remember { mutableStateListOf<Alarm>().also { it.addAll(store.load()) } }
    var editing by remember { mutableStateOf<Alarm?>(null) }

    fun persist(updated: Alarm) {
        val i = alarms.indexOfFirst { it.id == updated.id }
        if (i >= 0) alarms[i] = updated else alarms.add(updated)
        store.save(alarms.toList())
        AlarmScheduler.schedule(context, updated)
    }

    fun remove(a: Alarm) {
        alarms.remove(a)
        store.save(alarms.toList())
        AlarmScheduler.cancel(context, a.id)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val c = Calendar.getInstance()
                editing = Alarm(
                    id = (alarms.maxOfOrNull { it.id } ?: 0) + 1,
                    hour = c.get(Calendar.HOUR_OF_DAY),
                    minute = c.get(Calendar.MINUTE)
                )
            }) { Icon(Icons.Filled.Add, "افزودن آلارم") }
        }
    ) { padding ->
        if (alarms.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("هنوز آلارمی نساخته‌ای.\nبا دکمه + یکی اضافه کن.", textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(alarms.toList(), key = { it.id }) { a ->
                    AlarmRow(
                        alarm = a,
                        onToggle = { persist(a.copy(enabled = it)) },
                        onClick = { editing = a },
                        onDelete = { remove(a) }
                    )
                }
            }
        }
    }

    editing?.let { current ->
        AlarmEditor(
            initial = current,
            onDismiss = { editing = null },
            onSave = { persist(it.copy(enabled = true)); editing = null }
        )
    }
}

@Composable
fun AlarmRow(alarm: Alarm, onToggle: (Boolean) -> Unit, onClick: () -> Unit, onDelete: () -> Unit) {
    val daysText = if (alarm.days.isEmpty()) "فقط یک‌بار"
    else weekDays.filter { it.first in alarm.days }.joinToString(" ") { it.second }

    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "%02d:%02d".format(alarm.hour, alarm.minute),
                    fontSize = 36.sp, fontWeight = FontWeight.Light
                )
                Text(
                    listOf(alarm.label, daysText).filter { it.isNotBlank() }.joinToString(" · "),
                    fontSize = 14.sp
                )
            }
            Switch(checked = alarm.enabled, onCheckedChange = onToggle)
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "حذف") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditor(initial: Alarm, onDismiss: () -> Unit, onSave: (Alarm) -> Unit) {
    val timeState = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
    var label by remember { mutableStateOf(initial.label) }
    var days by remember { mutableStateOf(initial.days) }
    var ringtone by remember { mutableStateOf(initial.ringtone) }
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= 33) {
                r.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                r.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            ringtone = uri?.toString()
        }
    }
    val ringtoneName = remember(ringtone) {
        ringtone?.let {
            runCatching { RingtoneManager.getRingtone(context, Uri.parse(it)).getTitle(context) }.getOrNull()
        } ?: "پیش‌فرض"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(initial.copy(hour = timeState.hour, minute = timeState.minute, label = label.trim(), days = days, ringtone = ringtone))
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = timeState)
                OutlinedTextField(
                    value = label, onValueChange = { label = it },
                    label = { Text("عنوان") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val i = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "انتخاب زنگ")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtone?.let { Uri.parse(it) })
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        }
                        picker.launch(i)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("زنگ: $ringtoneName") }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    weekDays.forEach { (d, name) ->
                        FilterChip(
                            selected = d in days,
                            onClick = { days = if (d in days) days - d else days + d },
                            label = { Text(name) },
                            modifier = Modifier.width(40.dp)
                        )
                    }
                }
            }
        }
    )
}
