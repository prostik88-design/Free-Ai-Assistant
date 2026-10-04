package com.example.ui.screens.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CyberIndigo
import com.example.ui.theme.CyberRose
import com.example.ui.theme.CyberTeal
import com.example.ui.viewmodels.AppViewModel

@Composable
fun AuthScreen(
    viewModel: AppViewModel,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.userSettings.collectAsState()
    val validation by viewModel.keyValidation.collectAsState()
    val isValidating by viewModel.isValidatingKey.collectAsState()

    var keyInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Hero Banner
            Image(
                painter = painterResource(id = R.drawable.agent_banner_1791102602888),
                contentDescription = "RouterAgent Header",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "RouterAgent",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Автономный агент с OpenRouter, Skills & MCP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Keystore Security Badge
            Surface(
                color = CyberTeal.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = CyberTeal, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Android Keystore Encryption (AES-GCM-256)",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberTeal,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Вход и настройка API Key",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Введите ключ OpenRouter. Ключ будет сохранён в зашифрованном хранилище Android Keystore и никогда не появится в логах.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("OpenRouter API Key") },
                        placeholder = { Text("sk-or-v1-••••••••") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle visibility"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_api_key_input")
                    )

                    // Verify Key Button
                    OutlinedButton(
                        onClick = {
                            if (keyInput.isNotBlank()) {
                                viewModel.validateApiKey(keyInput.trim())
                            } else {
                                Toast.makeText(context, "Сначала введите API Key", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = keyInput.isNotBlank() && !isValidating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_verify_key_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isValidating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Проверка через OpenRouter API...")
                        } else {
                            Text("Проверить ключ запросом к API")
                        }
                    }

                    // Validation Result
                    val res = validation
                    if (res != null) {
                        Surface(
                            color = if (res.isValid) CyberTeal.copy(alpha = 0.15f) else CyberRose.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    if (res.isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (res.isValid) CyberTeal else CyberRose
                                )
                                Column {
                                    Text(
                                        text = if (res.isValid) "Ключ успешно проверен!" else "Ошибка проверки ключа",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (res.isValid) CyberTeal else CyberRose
                                    )
                                    if (res.label != null) {
                                        Text("Метка: ${res.label}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (res.isFreeTier == true) {
                                        Text("Тариф: Free Tier / OpenRouter Free Router доступен", style = MaterialTheme.typography.labelSmall, color = CyberTeal)
                                    }
                                    if (res.errorMessage != null) {
                                        Text(res.errorMessage, style = MaterialTheme.typography.labelSmall, color = CyberRose)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Save and Enter Button
                    Button(
                        onClick = {
                            if (keyInput.isNotBlank()) {
                                viewModel.saveApiKey(keyInput.trim())
                                Toast.makeText(context, "Ключ зашифрован в Keystore", Toast.LENGTH_SHORT).show()
                                onContinue()
                            }
                        },
                        enabled = keyInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_save_and_continue_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Сохранить в Keystore и войти")
                    }

                    // Demo / Skip Button
                    OutlinedButton(
                        onClick = {
                            // Set a placeholder demo key if none present to allow full local exploration
                            if (!settings.hasApiKey) {
                                viewModel.saveApiKey("sk-or-v1-demo-router-key-preview-mode")
                            }
                            onContinue()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_demo_mode_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Продолжить в ознакомительном режиме")
                    }
                }
            }
        }
    }
}
