package com.screentime.mobile.ui.auth

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.screentime.mobile.R
import com.screentime.mobile.ui.components.SproutGhostButton
import com.screentime.mobile.ui.components.SproutPrimaryButton
import com.screentime.mobile.ui.theme.Sprout
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(viewModel: AuthViewModel = hiltViewModel()) {
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Sprout.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        // App icon: coral square with plum circle (matches Sprout app icon)
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(Sprout.colors.primary, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(40.dp).background(Sprout.colors.ink, CircleShape))
        }
        Spacer(Modifier.height(26.dp))
        Text(
            "Welcome to ScreenTime",
            style = Sprout.typography.display,
            color = Sprout.colors.ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Sign in to manage your family's screen time",
            style = Sprout.typography.bodyL,
            color = Sprout.colors.inkMuted,
            textAlign = TextAlign.Center,
        )

        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(
                it,
                color = Sprout.colors.overText,
                style = Sprout.typography.bodyStrong,
            )
        }

        Spacer(Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            SproutPrimaryButton(
                text = "Continue with Google",
                onClick = { scope.launch { signInWithGoogle(context, viewModel) } },
                leading = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.White, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("G", style = Sprout.typography.label, color = Color(0xFF4285F4))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            SproutGhostButton(
                text = "Sign in with email",
                onClick = { /* email path TODO */ },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "By continuing you agree to our Privacy Policy and Terms of Service",
            style = Sprout.typography.caption,
            color = Sprout.colors.inkFaint,
            textAlign = TextAlign.Center,
        )
        }
    }
}

private suspend fun signInWithGoogle(context: Context, viewModel: AuthViewModel) {
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(context.getString(R.string.default_web_client_id))
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    try {
        val result = CredentialManager.create(context).getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            viewModel.signInWithGoogle(idToken)
        } else {
            viewModel.reportError("Unexpected credential type")
        }
    } catch (e: GetCredentialCancellationException) {
        // User dismissed the account picker — not an error.
    } catch (e: GetCredentialException) {
        viewModel.reportError(e.message ?: "Google sign-in failed")
    }
}
