package com.example.clockapp

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class StopwatchState {
    var running by mutableStateOf(false)
    private var acc by mutableLongStateOf(0L)
    private var startedAt by mutableLongStateOf(0L)

    /** زمان تجمعی هر دور؛ جدیدترین دور اول لیست است. */
    val laps = mutableStateListOf<Long>()

    fun elapsed(now: Long): Long = acc + if (running) now - startedAt else 0L

    fun toggle() {
        val n = SystemClock.elapsedRealtime()
        if (running) {
            acc += n - startedAt
            running = false
        } else {
            startedAt = n
            running = true
        }
    }

    fun lap() {
        if (running) laps.add(0, elapsed(SystemClock.elapsedRealtime()))
    }

    fun reset() {
        running = false
        acc = 0L
        laps.clear()
    }
}

private fun formatStopwatch(ms: Long): String {
    val totalSec = ms / 1000
    return "%02d:%02d.%02d".format(totalSec / 60, totalSec % 60, (ms % 1000) / 10)
}

@Composable
fun StopwatchScreen(sw: StopwatchState) {
    val now by produceState(SystemClock.elapsedRealtime(), sw.running) {
        if (sw.running) {
            while (true) {
                value = SystemClock.elapsedRealtime()
                delay(30)
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Text(formatStopwatch(sw.elapsed(now)), fontSize = 60.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { sw.toggle() }) { Text(if (sw.running) "توقف" else "شروع") }
            OutlinedButton(onClick = { sw.lap() }, enabled = sw.running) { Text("دور") }
            OutlinedButton(
                onClick = { sw.reset() },
                enabled = !sw.running && (sw.elapsed(now) > 0 || sw.laps.isNotEmpty())
            ) { Text("ریست") }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(Modifier.fillMaxWidth()) {
            itemsIndexed(sw.laps.toList()) { i, cum ->
                val prev = sw.laps.getOrNull(i + 1) ?: 0L
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("دور ${Jalali.toPersianDigits((sw.laps.size - i).toString())}")
                    Text(formatStopwatch(cum - prev))
                    Text(formatStopwatch(cum))
                }
            }
        }
    }
}
