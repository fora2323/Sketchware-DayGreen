package org.sketchware.daygreen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import a.a.a.iI
import a.a.a.wq
import mod.hey.studios.util.Helper
import pro.sketchware.R
import pro.sketchware.utility.SketchwareUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewKeyStoreScreen(
    onBack: () -> Unit,
    onSuccess: (password: String) -> Unit,
) {
    MaterialTheme(colorScheme = sketchwareColorScheme()) {
        val colors = MaterialTheme.colorScheme
        val context = LocalContext.current
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

        val inputShape = RoundedCornerShape(16.dp)

        var alias by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var passwordConfirm by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var passwordConfirmVisible by remember { mutableStateOf(false) }
        var validity by remember { mutableStateOf("25") }

        var commonName by remember { mutableStateOf("") }
        var organizationalUnit by remember { mutableStateOf("") }
        var organization by remember { mutableStateOf("") }
        var locality by remember { mutableStateOf("") }
        var state by remember { mutableStateOf("") }
        var country by remember { mutableStateOf("") }

        fun validateAndSave() {
            if (alias.trim().isEmpty()) {
                SketchwareUtil.toast("Alias name cannot be empty")
                return
            }
            if (password.length < 4 || password.length > 32) {
                SketchwareUtil.toast("Password must be 4 to 32 characters long")
                return
            }
            if (password != passwordConfirm) {
                SketchwareUtil.toast(Helper.getResString(R.string.myprojects_sign_apk_incorrect_password))
                return
            }
            if (commonName.trim().isEmpty()) {
                SketchwareUtil.toast("First and Last Name (CN) cannot be empty")
                return
            }
            if (organizationalUnit.trim().isEmpty()) {
                SketchwareUtil.toast("Organizational Unit (OU) cannot be empty")
                return
            }
            if (organization.trim().isEmpty()) {
                SketchwareUtil.toast("Organization (O) cannot be empty")
                return
            }
            if (locality.trim().isEmpty()) {
                SketchwareUtil.toast("City or Locality (L) cannot be empty")
                return
            }
            if (state.trim().isEmpty()) {
                SketchwareUtil.toast("State or Province (ST) cannot be empty")
                return
            }
            if (country.trim().isEmpty()) {
                SketchwareUtil.toast("Country Code (C) cannot be empty")
                return
            }

            val validityYears = validity.toIntOrNull() ?: 25
            val dname = "CN=${commonName.trim()}OU=${organizationalUnit.trim()}O=${organization.trim()}L=${locality.trim()}ST=${state.trim()}C=${country.trim()}"

            try {
                val generator = iI()
                generator.a(wq.j(), dname, validityYears, alias.trim(), password)
                MaterialAlertDialogBuilder(context)
                    .setTitle(Helper.getResString(R.string.myprojects_sign_apk_new_certificate_title_certificate))
                    .setMessage(Helper.getResString(R.string.myprojects_sign_apk_dialog_complete_create_certificate))
                    .setIcon(R.drawable.certificate_96_blue)
                    .setPositiveButton(Helper.getResString(R.string.common_word_close)) { _, _ ->
                        onSuccess(password)
                    }
                    .show()
            } catch (e: Exception) {
                e.printStackTrace()
                MaterialAlertDialogBuilder(context)
                    .setTitle(Helper.getResString(R.string.myprojects_sign_apk_new_certificate_title_certificate))
                    .setMessage(Helper.getResString(R.string.myprojects_sign_apk_error_failed_create_new_certificate))
                    .setIcon(R.drawable.error_96_yellow)
                    .setPositiveButton(Helper.getResString(R.string.common_word_close), null)
                    .show()
            }
        }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = colors.background,
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(text = Helper.getResString(R.string.myprojects_sign_apk_new_certificate_title_new_certificate))
                            Text(
                                text = "Export path: ${wq.D}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = colors.background,
                        scrolledContainerColor = colors.surfaceVariant,
                        titleContentColor = colors.onBackground,
                        navigationIconContentColor = colors.onBackground,
                    ),
                )
            },
        ) { padding ->
            LazyColumn(
                contentPadding = padding,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                item {
                    SectionTitle("KeyStore Credentials")
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = alias,
                                onValueChange = { alias = it },
                                label = { Text("Alias") },
                                singleLine = true,
                                shape = inputShape,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                singleLine = true,
                                shape = inputShape,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = null,
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = passwordConfirm,
                                onValueChange = { passwordConfirm = it },
                                label = { Text("Confirm Password") },
                                singleLine = true,
                                shape = inputShape,
                                visualTransformation = if (passwordConfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordConfirmVisible = !passwordConfirmVisible }) {
                                        Icon(
                                            imageVector = if (passwordConfirmVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = null,
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = validity,
                                onValueChange = { validity = it },
                                label = { Text("Validity (Years)") },
                                singleLine = true,
                                shape = inputShape,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                item {
                    SectionTitle("Certificate Information")
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = commonName,
                                onValueChange = { commonName = it },
                                label = { Text("First and Last Name (CN)") },
                                singleLine = true,
                                shape = inputShape,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = organizationalUnit,
                                onValueChange = { organizationalUnit = it },
                                label = { Text("Organizational Unit (OU)") },
                                singleLine = true,
                                shape = inputShape,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = organization,
                                onValueChange = { organization = it },
                                label = { Text("Organization (O)") },
                                singleLine = true,
                                shape = inputShape,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = locality,
                                onValueChange = { locality = it },
                                label = { Text("City or Locality (L)") },
                                singleLine = true,
                                shape = inputShape,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = state,
                                onValueChange = { state = it },
                                label = { Text("State or Province (ST)") },
                                singleLine = true,
                                shape = inputShape,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = country,
                                onValueChange = { country = it },
                                label = { Text("Country Code (C, 2 letters)") },
                                singleLine = true,
                                shape = inputShape,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f),
                            shape = inputShape,
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = { validateAndSave() },
                            modifier = Modifier.weight(1f),
                            shape = inputShape,
                        ) {
                            Text("Save KeyStore")
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp),
    )
}
