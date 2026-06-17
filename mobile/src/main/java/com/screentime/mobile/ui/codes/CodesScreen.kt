package com.screentime.mobile.ui.codes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.R
import com.screentime.mobile.ui.components.IconBadge
import kotlinx.coroutines.delay
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodesScreen(viewModel: CodesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var selected by remember { mutableStateOf(30) }
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_codes)) }) },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconBadge(Icons.Filled.Key, size = 56.dp)
                Text(
                    "Generate a one-time unlock code",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "Codes are valid for 5 minutes and can be used once.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )

                val active = state.active
                if (active != null) {
                    ActiveCodeCard(
                        code = active.code,
                        extraMinutes = active.extraMinutes,
                        expiresAt = active.expiresAt,
                        onDismiss = viewModel::dismiss,
                    )
                } else {
                    PresetRow(selected = selected, onSelect = { selected = it })
                    Button(
                        enabled = !state.isGenerating,
                        onClick = { viewModel.generate(selected) },
                    ) {
                        if (state.isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        }
                        Text("Generate ($selected min)")
                    }
                }

                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetRow(selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(15, 30, 60).forEach { minutes ->
            FilterChip(
                selected = minutes == selected,
                onClick = { onSelect(minutes) },
                label = { Text("$minutes min") },
            )
        }
    }
}

@Composable
private fun ActiveCodeCard(
    code: String,
    extraMinutes: Int,
    expiresAt: Instant,
    onDismiss: () -> Unit,
) {
    var remaining by remember(code) { mutableStateOf(secondsUntil(expiresAt)) }
    LaunchedEffect(code) {
        while (remaining > 0) {
            delay(1_000)
            remaining = secondsUntil(expiresAt)
        }
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconBadge(
                icon = Icons.Filled.CheckCircle,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            )
            Text("$extraMinutes minute code", style = MaterialTheme.typography.titleMedium)
            Text(
                text = code,
                style = MaterialTheme.typography.displayLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
            )
            Text(
                if (remaining > 0) "Expires in ${remaining}s" else "Expired",
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    }
}

private fun secondsUntil(instant: Instant): Long =
    (instant.epochSecond - Instant.now().epochSecond).coerceAtLeast(0)
