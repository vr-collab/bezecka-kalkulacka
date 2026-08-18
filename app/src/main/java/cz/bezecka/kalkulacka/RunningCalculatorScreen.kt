package cz.bezecka.kalkulacka

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
    val scope = rememberCoroutineScope()

    var distance by remember { mutableStateOf(1.5) }
    var pace by remember { mutableStateOf(3.9) }
    var timeSeconds by remember { mutableStateOf(351) }

    var isCalculating by remember { mutableStateOf(false) }
    var recalcJob by remember { mutableStateOf<Job?>(null) }

    var editingField by remember { mutableStateOf<FieldType?>(null) }
    var inputText by remember { mutableStateOf("") }

    fun roundDistance(value: Double): Double {
        return ((value.coerceIn(0.1, 200.0) * 10).roundToInt() / 10.0)
    }

    fun formatDistance(value: Double): String {
        return String.format(java.util.Locale.US, "%.1f km", value)
    }

    fun formatPace(value: Double): String {
        val totalSeconds = (value * 60).roundToInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(java.util.Locale.US, "%d:%02d min/km", minutes, seconds)
    }

    fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, secs)
    }

    fun performCalculation(changedField: FieldType) {
        when (changedField) {
            FieldType.DISTANCE, FieldType.PACE -> {
                timeSeconds = (distance * pace * 60).roundToInt()
            }
            FieldType.TIME -> {
                if (distance > 0) {
                    pace = (timeSeconds.toDouble() / 60.0 / distance).coerceIn(1.0, 20.0)
                }
            }
        }
    }

    fun triggerRecalc(changedField: FieldType) {
        recalcJob?.cancel()
        recalcJob = scope.launch {
            isCalculating = true
            delay(3000)
            performCalculation(changedField)
            isCalculating = false
        }
    }

    fun startEditing(field: FieldType) {
        editingField = field
        inputText = when (field) {
            FieldType.DISTANCE -> String.format(java.util.Locale.US, "%.1f", distance)
            FieldType.PACE -> {
                val totalSeconds = (pace * 60).roundToInt()
                String.format(java.util.Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
            }
            FieldType.TIME -> formatTime(timeSeconds)
        }
    }

    fun parsePace(text: String): Double? {
        val clean = text.trim().replace(",", ".")
        if (!clean.contains(":")) {
            return clean.toDoubleOrNull()
        }
        val parts = clean.split(":")
        if (parts.size != 2) return null
        val minutes = parts[0].toIntOrNull() ?: return null
        val seconds = parts[1].toIntOrNull() ?: return null
        if (minutes < 0 || seconds !in 0..59) return null
        return minutes + (seconds / 60.0)
    }

    fun parseTime(text: String): Int? {
        val parts = text.trim().split(":")
        return when (parts.size) {
            1 -> parts[0].toIntOrNull()
            2 -> {
                val minutes = parts[0].toIntOrNull() ?: return null
                val seconds = parts[1].toIntOrNull() ?: return null
                if (minutes < 0 || seconds !in 0..59) null else minutes * 60 + seconds
            }
            3 -> {
                val hours = parts[0].toIntOrNull() ?: return null
                val minutes = parts[1].toIntOrNull() ?: return null
                val seconds = parts[2].toIntOrNull() ?: return null
                if (hours < 0 || minutes !in 0..59 || seconds !in 0..59) {
                    null
                } else {
                    hours * 3600 + minutes * 60 + seconds
                }
            }
            else -> null
        }
    }

    fun saveInput() {
        val field = editingField ?: return
        when (field) {
            FieldType.DISTANCE -> {
                inputText.replace(",", ".").toDoubleOrNull()?.let {
                    distance = roundDistance(it)
                    triggerRecalc(FieldType.DISTANCE)
                }
            }
            FieldType.PACE -> {
                parsePace(inputText)?.let {
                    pace = it.coerceIn(1.0, 20.0)
                    triggerRecalc(FieldType.PACE)
                }
            }
            FieldType.TIME -> {
                parseTime(inputText)?.let {
                    timeSeconds = it.coerceIn(1, 24 * 60 * 60)
                    triggerRecalc(FieldType.TIME)
                }
            }
        }
        editingField = null
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
                    onValueClick = { startEditing(FieldType.DISTANCE) },
                    onUp = {
                        distance = roundDistance(distance + 0.1)
                        triggerRecalc(FieldType.DISTANCE)
                    },
                    onDown = {
                        distance = roundDistance(distance - 0.1)
                        triggerRecalc(FieldType.DISTANCE)
                    }
                )

                CalcCard(
                    title = "Tempo",
                    value = formatPace(pace),
                    accent = accent,
                    onValueClick = { startEditing(FieldType.PACE) },
                    onUp = {
                        pace = (pace + 0.1).coerceIn(1.0, 20.0)
                        triggerRecalc(FieldType.PACE)
                    },
                    onDown = {
                        pace = (pace - 0.1).coerceAtLeast(1.0)
                        triggerRecalc(FieldType.PACE)
                    }
                )

                CalcCard(
                    title = "Čas",
                    value = formatTime(timeSeconds),
                    accent = accent,
                    onValueClick = { startEditing(FieldType.TIME) },
                    onUp = {
                        timeSeconds = (timeSeconds + 1).coerceAtMost(24 * 60 * 60)
                        triggerRecalc(FieldType.TIME)
                    },
                    onDown = {
                        timeSeconds = (timeSeconds - 1).coerceAtLeast(1)
                        triggerRecalc(FieldType.TIME)
                    }
                )

                if (isCalculating) {
                    Text(
                        text = "Počítám…",
                        color = accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        recalcJob?.cancel()
                        isCalculating = false
                        distance = 1.0
                        pace = 5.0
                        timeSeconds = 300
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Vymazat", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Rumburští Draci",
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        if (editingField != null) {
            val title = when (editingField) {
                FieldType.DISTANCE -> "Zadat vzdálenost v km"
                FieldType.PACE -> "Zadat tempo (např. 4:35)"
                FieldType.TIME -> "Zadat čas (např.
