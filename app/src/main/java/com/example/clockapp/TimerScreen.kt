package com.example.clockapp

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** وضعیت تایمر در SharedPreferences ذخیره می‌شود تا با عوض شدن تب یا بسته شدن اپ از بین نرود. */
class TimerStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("timer", Context.MODE_PRIVATE)

    /** زمان پایان (میلی‌ثانیه). صفر یعنی تایمر در حال اجرا نیست. */
    var endAt: Long
        get() = prefs.getLong("endAt", 0L)
        set(v) { prefs.edit().putLong("endAt", v).apply() }

    /** زمان باقی‌مانده وقتی تایمر مکث شده است. */
    var left: Long
        get() = prefs.getLong("left", 0L)
        set(v) { prefs.edit().putLong("left", v).apply() }
}

private fun formatHms(ms: Long): String {
    val sec = (ms + 999) / 1000
    return "%02d:%02d:%02d".format(sec / 3600, (sec % 3600) / 60, sec % 60)
}

@Composable
fun TimerScreen() {
    val context = LocalContext.current
    val store = remember { TimerStore(context) }
    var endAt by remember { mutableLongStateOf(store.endAt) }
    var left by remember { mutableLongStateOf(store.left) }
    var finished by remember { mutableStateOf(false) }
    var h by remember { mutableStateOf("0") }
    var m by remember { mutableStateOf("5") }
    var s by remember { mutableStateOf("0") }

    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(200)
        }
    }

    fun save() {
        store.endAt = endAt
        store.left = left
    }

    // وقتی زمان تمام شد، وضعیت را پاک کن (صدا را AlarmService پخش می‌کند)
    LaunchedEffect(now) {
        if (endAt > 0 && now >= endAt) {
            endAt = 0
            left = 0
            finished = true
            save()
        }
    }

    val running = endAt > 0
    val paused = !running && left > 0
    val remaining = if (running) (endAt - now).coerceAtLeast(0) else left

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!running && !paused) {
            Text("تایمر", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeField("ساعت", h) { h = it }
                TimeField("دقیقه", m) { m = it }
                TimeField("ثانیه", s) { s = it }
            }
            Spacer(Modifier.height(24.dp))
            if (finished) {
                Text("زمان تمام شد ✓", fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
            }
            Button(onClick = {
                val total = ((h.toLongOrNull() ?: 0) * 3600 +
                        (m.toLongOrNull() ?: 0) * 60 +
                        (s.toLongOrNull() ?: 0)) * 1000
                if (total > 0) {
                    endAt = System.currentTimeMillis() + total
                    left = 0
                    finished = false
                    save()
                    AlarmScheduler.scheduleTimer(context, endAt)
                }
            }) { Text("شروع") }
        } else {
            Text(formatHms(remaining), fontSize = 64.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (running) {
                    Button(onClick = {
                        left = (endAt - System.currentTimeMillis()).coerceAtLeast(0)
                        endAt = 0
                        save()
                        AlarmScheduler.cancelTimer(context)
                    }) { Text("مکث") }
                } else {
                    Button(onClick = {
                        endAt = System.currentTimeMillis() + left
                        left = 0
                        save()
                        AlarmScheduler.scheduleTimer(context, endAt)
                    }) { Text("ادامه") }
                }
                OutlinedButton(onClick = {
                    endAt = 0
                    left = 0
                    finished = false
                    save()
                    AlarmScheduler.cancelTimer(context)
                }) { Text("لغو") }
            }
        }
    }
}

@Composable
private fun TimeField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(2)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.width(96.dp)
    )
}
