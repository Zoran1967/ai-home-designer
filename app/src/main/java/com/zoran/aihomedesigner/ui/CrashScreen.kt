package com.zoran.aihomedesigner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoran.aihomedesigner.R

/** Prikazuje opis greške posle neočekivanog zatvaranja, da se može kopirati i poslati. */
@Composable
fun CrashScreen(report: String, onContinue: () -> Unit) {
    val clipboard = LocalClipboardManager.current

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.crash_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.crash_explanation),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { clipboard.setText(AnnotatedString(report)) }) {
                    Text(stringResource(R.string.crash_copy))
                }
                OutlinedButton(onClick = onContinue) {
                    Text(stringResource(R.string.crash_continue))
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = report,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }
    }
}
