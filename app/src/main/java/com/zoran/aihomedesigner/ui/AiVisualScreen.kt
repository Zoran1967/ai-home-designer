package com.zoran.aihomedesigner.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.zoran.aihomedesigner.R
import com.zoran.aihomedesigner.ai.AiSettings
import com.zoran.aihomedesigner.ai.GeminiClient
import com.zoran.aihomedesigner.domain.AiPrompt
import com.zoran.aihomedesigner.domain.Furniture
import com.zoran.aihomedesigner.domain.GeminiApi
import com.zoran.aihomedesigner.domain.GeminiResult
import com.zoran.aihomedesigner.domain.Opening
import com.zoran.aihomedesigner.domain.Room
import com.zoran.aihomedesigner.domain.Stairs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

private sealed interface AiState {
    data object Idle : AiState
    data object Loading : AiState
    data class Done(val image: ImageBitmap) : AiState
    data class Failed(val message: String) : AiState
}

/**
 * AI ilustracija prostora: opis iz modela se šalje Gemini servisu za slike, uz ključ koji korisnik sam unese.
 * Slika je samo ilustracija i ne čuva se. Mere i raspored važe samo na planu.
 */
@Composable
fun AiVisualScreen(
    rooms: List<Room>?,
    openings: List<Opening>,
    furniture: List<Furniture>,
    stairs: List<Stairs>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val prompt = remember(rooms, openings, furniture, stairs) {
        AiPrompt.build(rooms.orEmpty(), openings, furniture, stairs)
    }
    var apiKey by remember { mutableStateOf(AiSettings.apiKey(context)) }
    var model by remember { mutableStateOf(AiSettings.model(context)) }
    var state by remember { mutableStateOf<AiState>(AiState.Idle) }

    val keyOk = AiSettings.isPlausibleKey(apiKey)
    val modelOk = GeminiApi.isValidModelName(model)
    val canSend = prompt != null && keyOk && modelOk && state !is AiState.Loading

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            }
            Text(stringResource(R.string.ai_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.ai_intro), style = MaterialTheme.typography.bodyMedium)

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text(stringResource(R.string.ai_key_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Text(stringResource(R.string.ai_key_note), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.ai_key_help), style = MaterialTheme.typography.bodySmall)
            if (apiKey.isNotEmpty()) {
                TextButton(onClick = {
                    AiSettings.clearApiKey(context)
                    apiKey = ""
                }) { Text(stringResource(R.string.ai_key_clear)) }
            }

            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text(stringResource(R.string.ai_model_label)) },
                singleLine = true,
                isError = !modelOk,
                modifier = Modifier.fillMaxWidth()
            )
            if (!modelOk) {
                Text(
                    stringResource(R.string.ai_model_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (prompt != null) {
                Text(stringResource(R.string.ai_prompt_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.ai_prompt_note), style = MaterialTheme.typography.bodySmall)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(prompt, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text(stringResource(R.string.ai_no_rooms), style = MaterialTheme.typography.bodyMedium)
            }

            if (prompt != null && !keyOk) {
                Text(stringResource(R.string.ai_need_key), style = MaterialTheme.typography.bodySmall)
            }
            Button(
                enabled = canSend,
                onClick = {
                    if (prompt != null) {
                        AiSettings.saveApiKey(context, apiKey)
                        AiSettings.saveModel(context, model)
                        state = AiState.Loading
                        scope.launch {
                            state = requestImage(context, apiKey.trim(), model.trim(), prompt)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.ai_send)) }

            when (val current = state) {
                AiState.Idle -> Unit
                AiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
                    Text(stringResource(R.string.ai_sending), style = MaterialTheme.typography.bodyMedium)
                }
                is AiState.Failed -> Text(
                    current.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                is AiState.Done -> {
                    Text(stringResource(R.string.ai_result_title), style = MaterialTheme.typography.titleSmall)
                    Image(
                        bitmap = current.image,
                        contentDescription = stringResource(R.string.ai_image_description),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(stringResource(R.string.ai_disclaimer), style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.padding(8.dp))
        }
    }
}

private suspend fun requestImage(
    context: android.content.Context,
    apiKey: String,
    model: String,
    prompt: String
): AiState {
    val result = try {
        GeminiClient.generate(apiKey, model, prompt)
    } catch (e: IOException) {
        return AiState.Failed(context.getString(R.string.ai_err_network))
    }
    return when (result) {
        is GeminiResult.Image -> {
            val bitmap = decodeImage(result.base64)
            if (bitmap != null) AiState.Done(bitmap) else AiState.Failed(context.getString(R.string.ai_err_decode))
        }
        is GeminiResult.Failure -> {
            val detail = result.detail.orEmpty().take(300)
            AiState.Failed(
                when (result.kind) {
                    GeminiResult.Kind.HTTP_ERROR -> context.getString(R.string.ai_err_http, detail)
                    GeminiResult.Kind.RATE_LIMITED -> context.getString(R.string.ai_err_rate, detail)
                    GeminiResult.Kind.BLOCKED -> context.getString(R.string.ai_err_blocked, detail)
                    GeminiResult.Kind.NO_IMAGE -> context.getString(R.string.ai_err_no_image)
                }
            )
        }
    }
}

private suspend fun decodeImage(base64: String): ImageBitmap? = withContext(Dispatchers.Default) {
    try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (e: IllegalArgumentException) {
        null
    } catch (e: OutOfMemoryError) {
        null
    }
}
