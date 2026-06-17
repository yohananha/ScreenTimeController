package com.screentime.mobile.ui.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screentime.mobile.ui.components.IconBadge

@Composable
fun FamilyOnboardingScreen(viewModel: FamilyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var enteringCode by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(Icons.Filled.Groups, size = 72.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Welcome!", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Create a family or join one that another parent already created.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp),
        )

        if (!enteringCode) {
            Button(
                onClick = { viewModel.createFamily() },
                modifier = Modifier.padding(bottom = 8.dp),
            ) { Text("Create a new family") }
            TextButton(onClick = { enteringCode = true }) {
                Text("Join with an invite code")
            }
        } else {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.filter(Char::isDigit).take(6) },
                label = { Text("6-digit code") },
                singleLine = true,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Button(
                enabled = code.length == 6 && !state.joining,
                onClick = { viewModel.joinByCode(code) },
            ) { Text(if (state.joining) "Joining…" else "Join") }
            TextButton(onClick = { enteringCode = false }) { Text("Back") }
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
