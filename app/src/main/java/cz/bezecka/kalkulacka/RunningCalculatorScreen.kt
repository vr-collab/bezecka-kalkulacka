package cz.bezecka.kalkulacka

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class FieldType { DISTANCE, PACE, TIME }

@Composable
fun RunningCalculatorScreen() {
    val accent = Color(0xFF19E3C6)
    var distance by remember { mutableStateOf<Double?>(null) }
    var pace by remember { mutableStateOf<Double?>(null) }
    var timeSeconds by remember { mutableStateOf<Int?>(null) }
    var computing by remember { mutableStateOf(false) }
    var pendingChangedField by remember { mutableStateOf<FieldType?>(null) }
    val scope = rememberCoroutineScope()
    var recalcJob by remember { mutableStateOf<Job?>(null) }

    fun formatDistance(v: Double?) = v?.let { String.format(java.util.Locale.US, "%.1f km", it) } ?: "—"

    fun formatPace(v: Double?): String {
        if (v == null) return "—"
        val totalSec = (v * 60.0).roundToInt()
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format(java.util.Locale.US, "%d:%02d min/km", m, s)
    }

    fun formatTime(sec: Int?): String {
        if (sec == null) return "—"
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
    }

    fun calculateMissing(changed: FieldType?) {
        val filled = listOf(distance, pace, timeSeconds).count { it != null }
        if (filled != 2) {
            computing = false
            return
        }
        when {
            distance != null && pace != null && timeSeconds == null -> {
                timeSeconds = ((distance!! * pace!! * 60.0).roundToInt()).coerceIn(0, 7200)
            }
            distance != null && timeSeconds != null && pace == null && distance!! > 0.0 -> {
                pace = ((timeSeconds!!.toDouble() / 60.0) / distance!!).coerceIn(3.0, 8.0)
            }
            pace != null && timeSeconds != null && distance == null && pace!! > 0.0 -> {
                distance = ((timeSeconds!!.toDouble() / 60.0) / pace!!).coerceIn(0.1, 22.0)
                distance = kotlin.math.round(distance!! * 10.0) / 10.0
            }
            changed == FieldType.DISTANCE && distance != null && pace != null && timeSeconds != null -> {
                timeSeconds = ((distance!! * pace!! * 60.0).roundToInt()).coerceIn(0, 7200)
            }
            changed == FieldType.PACE && distance != null && pace != null && timeSeconds != null -> {
                timeSeconds = ((distance!! * pace!! * 60.0).roundToInt()).coerceIn(0, 7200)
            }
            changed == FieldType.TIME && distance != null && pace != null && timeSeconds != null -> {
                pace = ((timeSeconds!!.toDouble() / 60.0) / distance!!).coerceIn(3.0, 8.0)
            }
        }
        computing = false
    }

    fun triggerRecalc(changed: FieldType) {
        pendingChangedField = changed
        computing = true
        recalcJob?.cancel()
        recalcJob = scope.launch {
            delay(3000)
            calculateMissing(pendingChangedField)
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Běžecká kalkulačka",
                    color = accent,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
                CalcCard(
                    title = "Vzdálenost",
                    value = formatDistance(distance),
                    accent = accent,
                    onUp = {
                        distance = (((distance ?: 0.0) + 0.1).coerceIn(0.1, 22.0) * 10.0).roundToInt() / 10.0
                        triggerRecalc(FieldType.DISTANCE)
                    },
                    onDown = {
                        val next = ((distance ?: 0.1) - 0.1).coerceAtLeast(0.1)
                        distance = (next * 10.0).roundToInt() / 10.0
                        triggerRecalc(FieldType.DISTANCE)
                    }
                )
                CalcCard(
                    title = "Tempo",
                    value = formatPace(pace),
                    accent = accent,
                    onUp = {
                        pace = (((pace ?: 3.0) + 0.1).coerceIn(3.0, 8.0) * 10.0).roundToInt() / 10.0
                        triggerRecalc(FieldType.PACE)
                    },
                    onDown = {
                        val next = ((pace ?: 3.0) - 0.1).coerceAtLeast(3.0)
                        pace = (next * 10.0).roundToInt() / 10.0
                        triggerRecalc(FieldType.PACE)
                    }
                )
                CalcCard(
                    title = "Čas",
                    value = formatTime(timeSeconds),
                    accent = accent,
                    onUp = {
                        timeSeconds = ((timeSeconds ?: 0) + 1).coerceIn(0, 7200)
                        triggerRecalc(FieldType.TIME)
                    },
                    onDown = {
                        timeSeconds = ((timeSeconds ?: 0) - 1).coerceAtLeast(0)
                        triggerRecalc(FieldType.TIME)
                    }
                )
                if (computing) {
                    Text(
                        text = "🏃 Počítám...",
                        color = accent,
                        fontSize = 18.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        recalcJob?.cancel()
                        distance = null
                        pace = null
                        timeSeconds = null
                        computing = false
                        pendingChangedField = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Vymazat", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Rumburští Draci",
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CalcCard(
    title: String,
    value: String,
    accent: Color,
    onUp: () -> Unit,
    onDown: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, color = Color.White, fontSize = 18.sp)
                Text(value, color = accent, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFloatingActionButton(
                    onClick = onUp,
                    containerColor = accent,
                    contentColor = Color.Black
                ) { Text("▲") }
                SmallFloatingActionButton(
                    onClick = onDown,
                    containerColor = accent,
                    contentColor = Color.Black
                ) { Text("▼") }
            }
        }
    }
}
