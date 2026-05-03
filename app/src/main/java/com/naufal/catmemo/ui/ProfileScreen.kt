package com.naufal.catmemo.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.naufal.catmemo.UserProfile
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import com.naufal.catmemo.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentUsername: String,
    initialProfile: UserProfile,
    onUsernameUpdate: (String) -> Boolean,
    onNicknameUpdate: (String) -> Unit,
    onDobUpdate: (String) -> Unit,
    onLanguageUpdate: (String) -> Unit,
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf(currentUsername) }
    var nickname by remember { mutableStateOf(initialProfile.nickname) }
    var dob by remember { mutableStateOf(initialProfile.dob) }
    var language by remember { mutableStateOf(initialProfile.language) }
    var photoUri by remember { mutableStateOf<Uri?>(if (initialProfile.photoUri.isNotEmpty()) Uri.parse(initialProfile.photoUri) else null) }

    // Real-time update logic
    LaunchedEffect(nickname) {
        if (nickname != initialProfile.nickname) {
            onNicknameUpdate(nickname)
        }
    }

    LaunchedEffect(language) {
        if (language != initialProfile.language) {
            onLanguageUpdate(language)
        }
    }

    LaunchedEffect(dob) {
        if (dob != initialProfile.dob) {
            onDobUpdate(dob)
        }
    }
    
    // For Username, we might want to wait for focus loss or a specific action, 
    // but here we'll use a longer debounce to satisfy "real-time"
    LaunchedEffect(username) {
        if (username != currentUsername && username.isNotBlank()) {
            delay(1000) // 1 second debounce for username
            val success = onUsernameUpdate(username)
            if (!success) {
                // Optionally revert or show error handled by MainActivity
            }
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { photoUri = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Picture
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable { galleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = photoUri ?: R.drawable.ic_default_cat,
                    contentDescription = stringResource(R.string.profile_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.tap_to_change_photo),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Username
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.username)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nickname
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text(stringResource(R.string.nickname)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date of Birth
            OutlinedTextField(
                value = dob,
                onValueChange = { },
                label = { Text(stringResource(R.string.dob)) },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.select_date))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Language Selection
            Text(
                text = stringResource(R.string.select_language),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = language == "in",
                    onClick = { language = "in" }
                )
                Text("Bahasa Indonesia", modifier = Modifier.clickable { language = "in" })
                
                Spacer(modifier = Modifier.width(16.dp))
                
                RadioButton(
                    selected = language == "en",
                    onClick = { language = "en" }
                )
                Text("English", modifier = Modifier.clickable { language = "en" })
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (language == "in") "Semua perubahan disimpan otomatis ✨" else "All changes are saved automatically ✨",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                        dob = formatter.format(Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.select))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
