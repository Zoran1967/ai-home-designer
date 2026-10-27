package com.zoran.aihomedesigner.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.zoran.aihomedesigner.R
import com.zoran.aihomedesigner.domain.PdfPageChoice

/** Pita korisnika koju stranu PDF-a sa [pageCount] strana da uveze kao plan. */
@Composable
fun PdfPageDialog(
    pageCount: Int,
    onConfirm: (Int) -> Unit,
    onCancel: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf("1") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.plan_pdf_page_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    showError = false
                },
                label = { Text(stringResource(R.string.plan_pdf_page_field)) },
                singleLine = true,
                isError = showError,
                supportingText = {
                    Text(
                        stringResource(
                            if (showError) R.string.plan_pdf_page_error else R.string.plan_pdf_page_hint,
                            pageCount
                        )
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val page = PdfPageChoice.parse(text, pageCount)
                if (page == null) showError = true else onConfirm(page)
            }) { Text(stringResource(R.string.plan_pdf_page_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        }
    )
}
