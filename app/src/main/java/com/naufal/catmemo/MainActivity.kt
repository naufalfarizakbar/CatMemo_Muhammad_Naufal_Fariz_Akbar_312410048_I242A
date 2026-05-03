package com.naufal.catmemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatAlignJustify
import com.naufal.catmemo.ui.LoginScreen
import com.naufal.catmemo.ui.RegisterScreen
import com.naufal.catmemo.ui.ProfileScreen
import com.naufal.catmemo.ui.SplashScreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay
import com.google.ai.client.generativeai.GenerativeModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naufal.catmemo.ui.theme.CatMemoTheme
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import java.util.Locale
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.text.input.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val authManager = AuthManager(this)

        setContent {
            CatMemoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RootApp(authManager)
                }
            }
        }
    }
}

enum class AppScreen { Splash, Login, Register, Home, Profile }

@Composable
fun RootApp(authManager: AuthManager) {
    var currentScreen by remember { 
        mutableStateOf(AppScreen.Splash) 
    }

    val username = authManager.getLoggedInUser() ?: ""
    val profile = authManager.getUserProfile(username)
    val language = profile.language

    val locale = if (language == "in") Locale("in") else Locale("en")
    val configuration = LocalConfiguration.current
    configuration.setLocale(locale)
    val context = LocalContext.current
    val resources = context.resources
    resources.updateConfiguration(configuration, resources.displayMetrics)

    when (currentScreen) {
        AppScreen.Splash -> {
            SplashScreen(
                onFinished = {
                    currentScreen = if (authManager.isLoggedIn()) AppScreen.Home else AppScreen.Login
                }
            )
        }
        AppScreen.Login -> {
            LoginScreen(
                onLoginSuccess = { currentScreen = AppScreen.Home },
                onNavigateToRegister = { currentScreen = AppScreen.Register },
                onLoginAttempt = { user, pass -> authManager.login(user, pass) }
            )
        }
        AppScreen.Register -> {
            RegisterScreen(
                onRegisterSuccess = { currentScreen = AppScreen.Login },
                onNavigateToLogin = { currentScreen = AppScreen.Login },
                onRegisterAttempt = { user, pass -> authManager.register(user, pass) }
            )
        }
        AppScreen.Home -> {
            CatMemoApp(
                authManager = authManager,
                onLogout = {
                    authManager.logout()
                    currentScreen = AppScreen.Login
                },
                onNavigateToProfile = {
                    currentScreen = AppScreen.Profile
                }
            )
        }
        AppScreen.Profile -> {
            val context = LocalContext.current
            val currentUsername = authManager.getLoggedInUser() ?: ""
            ProfileScreen(
                currentUsername = currentUsername,
                initialProfile = authManager.getUserProfile(currentUsername),
                onUsernameUpdate = { newUsername -> 
                    if (authManager.changeUsername(currentUsername, newUsername)) {
                        true
                    } else {
                        Toast.makeText(context, context.getString(R.string.username_taken), Toast.LENGTH_SHORT).show()
                        false
                    }
                },
                onNicknameUpdate = { newNickname ->
                    val p = authManager.getUserProfile(currentUsername)
                    authManager.saveUserProfile(currentUsername, p.copy(nickname = newNickname))
                },
                onDobUpdate = { newDob ->
                    val p = authManager.getUserProfile(currentUsername)
                    authManager.saveUserProfile(currentUsername, p.copy(dob = newDob))
                },
                onLanguageUpdate = { newLang ->
                    val p = authManager.getUserProfile(currentUsername)
                    authManager.saveUserProfile(currentUsername, p.copy(language = newLang))
                },
                onBack = { currentScreen = AppScreen.Home }
            )
        }
    }
}

// Note class is now in AuthManager.kt

@Composable
fun CatMemoApp(
    authManager: AuthManager,
    onLogout: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
){
    val username = authManager.getLoggedInUser() ?: ""

    var notes by remember {
        mutableStateOf(authManager.getNotes(username).ifEmpty {
            listOf(
                Note(title = "Shopping", content = "Buy cat food"),
                Note(title = "Reminder", content = "Finish Android project"),
                Note(title = "Ideas", content = "Make cute cat memo app")
            )
        })
    }

    LaunchedEffect(notes) {
        authManager.saveNotes(username, notes)
    }

    var selectedIndex by remember {
        mutableStateOf<Int?>(null)
    }

    if(selectedIndex == null){

        CatMemoHome(
            notes = notes,
            onUpdateNotes = { notes = it },
            onOpenNote = { selectedIndex = it },
            onLogout = onLogout,
            onNavigateToProfile = onNavigateToProfile
        )

    } else {

        NoteDetailScreen(
            note = notes[selectedIndex!!],

            onBack = {
                selectedIndex = null
            },

            onUpdateNote = { newTitle, newContent, newAlign ->
                notes = notes.toMutableList().apply {
                    this[selectedIndex!!] =
                        this[selectedIndex!!].copy(
                            title = newTitle, 
                            content = newContent, 
                            textAlign = newAlign,
                            timestamp = System.currentTimeMillis()
                        )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatMemoHome(
    notes: List<Note>,
    onUpdateNotes:(List<Note>)->Unit,
    onOpenNote:(Int)->Unit,
    onLogout: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
){

    var deleteIndex by remember {
        mutableStateOf<Int?>(null)
    }

    var showAddDialog by remember {
        mutableStateOf(false)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    val filteredNotes = remember(notes, searchQuery) {
        val baseList = if (searchQuery.isBlank()) {
            notes
        } else {
            notes.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true)
            }
        }
        baseList.sortedWith(
            compareByDescending<Note> { it.isPinned }
                .thenByDescending { it.timestamp }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = stringResource(R.string.profile_title))
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showAddDialog = true
                }
            ){
                Icon(Icons.Default.Add,null)
            }
        }
    ){ padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ){

            item{
                Text(
                    "🐱 CatMemo",
                    style = MaterialTheme.typography.headlineLarge
                )

                Spacer(Modifier.height(8.dp))

                Text(stringResource(R.string.tap_note_hint))

                Spacer(Modifier.height(20.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_search))
                            }
                        }
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))
            }

            if (filteredNotes.isEmpty() && searchQuery.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "😿",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.no_results),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                itemsIndexed(filteredNotes) { _, note ->
                    NoteCard(
                        note = note,
                        searchQuery = searchQuery,
                        onOpen = {
                            onOpenNote(notes.indexOf(note))
                        },
                        onDeleteClick = {
                            deleteIndex = notes.indexOf(note)
                        },
                        onPinClick = {
                            val noteIndex = notes.indexOf(note)
                            if (noteIndex != -1) {
                                onUpdateNotes(
                                    notes.toMutableList().apply {
                                        this[noteIndex] = this[noteIndex].copy(isPinned = !note.isPinned)
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }

        if(showAddDialog){

            AddNoteDialog(
                onDismiss = {
                    showAddDialog = false
                },
                onSave = { title,content ->
                    onUpdateNotes(
                        notes + Note(title = title, content = content)
                    )
                    showAddDialog = false
                }
            )
        }

        if(deleteIndex != null){

            AlertDialog(
                onDismissRequest = {
                    deleteIndex = null
                },

                title = {
                    Text(stringResource(R.string.delete_note))
                },

                text = {
                    Text(stringResource(R.string.delete_confirm))
                },

                confirmButton = {
                    TextButton(
                        onClick = {

                            deleteIndex?.let{
                                onUpdateNotes(
                                    notes.toMutableList().apply{
                                        removeAt(it)
                                    }
                                )
                            }

                            deleteIndex = null
                        }
                    ){
                        Text(stringResource(R.string.delete))
                    }
                },

                dismissButton = {
                    TextButton(
                        onClick = {
                            deleteIndex = null
                        }
                    ){
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun AddNoteDialog(
    onDismiss:()->Unit,
    onSave:(String,String)->Unit
){

    var title by remember {
        mutableStateOf("")
    }

    var content by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(stringResource(R.string.add_new_note))
        },

        text = {
            Column {

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                    },
                    label = {
                        Text(stringResource(R.string.title))
                    }
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = {
                        content = it
                    },
                    label = {
                        Text(stringResource(R.string.content))
                    }
                )
            }
        },

        confirmButton = {
            TextButton(
                onClick = {
                    if(
                        title.isNotBlank() &&
                        content.isNotBlank()
                    ){
                        onSave(title,content)
                    }
                }
            ){
                Text(stringResource(R.string.save))
            }
        },

        dismissButton = {
            TextButton(
                onClick = onDismiss
            ){
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCard(
    note: Note,
    searchQuery: String = "",
    onOpen:()->Unit,
    onDeleteClick:()->Unit,
    onPinClick:()->Unit
){
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth()
    ){

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ){

            Column(
                modifier = Modifier.weight(1f)
            ){
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp).padding(end = 4.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        highlightSearchText(note.title, searchQuery, MaterialTheme.typography.titleLarge),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    highlightSearchText(note.content, searchQuery, MaterialTheme.typography.bodyMedium, isMarkdown = true),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row {
                IconButton(
                    onClick = onPinClick
                ){
                    Icon(
                        if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = stringResource(if (note.isPinned) R.string.unpin else R.string.pin),
                        tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDeleteClick
                ){
                    Icon(
                        Icons.Default.Delete,
                        null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    note: Note,
    onBack:()->Unit,
    onUpdateNote:(String, String, String)->Unit
){

    var editMode by remember {
        mutableStateOf(false)
    }

    var title by remember {
        mutableStateOf(note.title)
    }
    var contentValue by remember {
        mutableStateOf(TextFieldValue(note.content))
    }
    var textAlign by remember {
        mutableStateOf(note.textAlign)
    }
    
    val historyManager = remember { HistoryManager() }
    val content = contentValue.text

    // Save state to history on significant changes
    LaunchedEffect(contentValue.text) {
        delay(500) // Small debounce for typing
        historyManager.pushState(contentValue)
    }

    LaunchedEffect(title, content, textAlign) {
        if (title != note.title || content != note.content || textAlign != note.textAlign) {
            delay(1000) // 1 second debounce
            onUpdateNote(title, content, textAlign)
        }
    }

    var showAiSummary by remember { mutableStateOf(false) }
    var aiLoading by remember { mutableStateOf(false) }
    var aiSummaryText by remember { mutableStateOf("") }

    val currentAlignment = when(textAlign) {
        "CENTER" -> TextAlign.Center
        "RIGHT" -> TextAlign.Right
        "JUSTIFY" -> TextAlign.Justify
        else -> TextAlign.Left
    }

    Scaffold(
        topBar = {

            TopAppBar(
                title = {
                    Text(
                        if(editMode)
                            stringResource(R.string.edit_note)
                        else
                            note.title
                    )
                },

                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ){
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            null
                        )
                    }
                },

                actions = {
                    if (editMode) {
                        IconButton(
                            onClick = {
                                title = note.title
                                contentValue = TextFieldValue(note.content)
                                textAlign = note.textAlign
                                editMode = false
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel"
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            aiLoading = true
                            showAiSummary = true
                        }
                    ){
                        Text("✨", style = MaterialTheme.typography.titleLarge)
                    }

                    IconButton(
                        onClick = {
                            editMode = !editMode
                        }
                    ){
                        Icon(
                            if (editMode) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (editMode) "Done" else "Edit"
                        )
                    }
                }
            )
        }

    ){ padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ){

            if(editMode){
                // Formatting & History & Alignment Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undo/Redo
                    IconButton(
                        onClick = { 
                            historyManager.undo(contentValue)?.let { contentValue = it }
                        },
                        enabled = historyManager.canUndo(),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("↺", style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(
                        onClick = { 
                            historyManager.redo(contentValue)?.let { contentValue = it }
                        },
                        enabled = historyManager.canRedo(),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("↻", style = MaterialTheme.typography.titleLarge)
                    }

                    VerticalDivider(modifier = Modifier.height(24.dp))

                    val toolbarButtons = listOf(
                        "B" to "**",
                        "I" to "*",
                        "U" to "__"
                    )
                    toolbarButtons.forEach { (label, marker) ->
                        OutlinedButton(
                            onClick = {
                                historyManager.pushState(contentValue)
                                val selection = contentValue.selection
                                val text = contentValue.text
                                val before = text.substring(0, selection.start)
                                val middle = text.substring(selection.start, selection.end)
                                val after = text.substring(selection.end)
                                
                                val newText = "$before$marker$middle$marker$after"
                                val newSelection = TextRange(selection.start + marker.length, selection.end + marker.length)
                                contentValue = TextFieldValue(newText, newSelection)
                            },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Text(label, fontWeight = if(label=="B") FontWeight.Bold else FontWeight.Normal, 
                                 fontStyle = if(label=="I") FontStyle.Italic else FontStyle.Normal,
                                 textDecoration = if(label=="U") TextDecoration.Underline else TextDecoration.None)
                        }
                    }

                    VerticalDivider(modifier = Modifier.height(24.dp))

                    // Alignment Buttons
                    val alignments = listOf(
                        Triple("LEFT", Icons.Default.FormatAlignLeft, R.string.align_left),
                        Triple("CENTER", Icons.Default.FormatAlignCenter, R.string.align_center),
                        Triple("RIGHT", Icons.Default.FormatAlignRight, R.string.align_right),
                        Triple("JUSTIFY", Icons.Default.FormatAlignJustify, R.string.align_justify)
                    )
                    alignments.forEach { (type, icon, hintRes) ->
                        IconButton(
                            onClick = { textAlign = type },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                icon, 
                                contentDescription = stringResource(hintRes),
                                tint = if (textAlign == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                    },
                    label = {
                        Text(stringResource(R.string.title))
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = contentValue,
                    onValueChange = {
                        contentValue = it
                    },
                    label = {
                        Text(stringResource(R.string.content))
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    visualTransformation = MarkdownVisualTransformation(),
                    textStyle = LocalTextStyle.current.copy(textAlign = currentAlignment)
                )

            } else {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    parseMarkdown(content),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = currentAlignment,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showAiSummary) {
        LaunchedEffect(aiLoading) {
            if (aiLoading) {
                try {
                    val generativeModel = GenerativeModel(
                        modelName = "gemini-2.5-flash",
                        apiKey = BuildConfig.GEMINI_API_KEY
                    )
                    val prompt = "Sebagai Asisten AI meow, tolong buatkan ringkasan dan poin-poin kesimpulan dari isi catatan berikut tanpa menggunakan format markdown bintang. Tambahkan sedikit gaya bahasa meow/kucing:\n\n$content"
                    val response = generativeModel.generateContent(prompt)
                    aiSummaryText = response.text ?: "Gagal mendapatkan ringkasan, meow."
                } catch (e: Exception) {
                    aiSummaryText = "Error: ${e.localizedMessage}\n\nPastikan API Key sudah diset dan ada koneksi internet!"
                } finally {
                    aiLoading = false
                }
            }
        }
        
        AlertDialog(
            onDismissRequest = { showAiSummary = false },
            title = { Text(stringResource(R.string.ai_assistant)) },
            text = {
                if (aiLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(aiSummaryText)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAiSummary = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}



@Preview(showBackground = true)
@Composable
fun PreviewCatMemo(){
    val context = LocalContext.current
    CatMemoTheme{
        CatMemoApp(authManager = AuthManager(context))
    }
}

// Markdown Helpers
fun parseMarkdown(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder(text)
    
    // Bold: **text**
    Regex("""\*\*(.*?)\*\*""").findAll(text).forEach { match ->
        builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
        builder.addStyle(SpanStyle(color = Color.Gray.copy(alpha = 0.4f)), match.range.first, match.range.first + 2)
        builder.addStyle(SpanStyle(color = Color.Gray.copy(alpha = 0.4f)), match.range.last - 1, match.range.last + 1)
    }
    
    // Underline: __text__
    Regex("""__(.*?)__""").findAll(text).forEach { match ->
        builder.addStyle(SpanStyle(textDecoration = TextDecoration.Underline), match.range.first, match.range.last + 1)
        builder.addStyle(SpanStyle(color = Color.Gray.copy(alpha = 0.4f)), match.range.first, match.range.first + 2)
        builder.addStyle(SpanStyle(color = Color.Gray.copy(alpha = 0.4f)), match.range.last - 1, match.range.last + 1)
    }

    // Italic: *text* (Ensuring it's exactly one asterisk, not two)
    Regex("""(?<!\*)\*(?!\*)(.*?)(?<!\*)\*(?!\*)""").findAll(text).forEach { match ->
        builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
        builder.addStyle(SpanStyle(color = Color.Gray.copy(alpha = 0.4f)), match.range.first, match.range.first + 1)
        builder.addStyle(SpanStyle(color = Color.Gray.copy(alpha = 0.4f)), match.range.last, match.range.last + 1)
    }

    return builder.toAnnotatedString()
}

@Composable
fun highlightSearchText(
    text: String,
    query: String,
    baseStyle: TextStyle,
    isMarkdown: Boolean = false
): AnnotatedString {
    val annotatedString = if (isMarkdown) parseMarkdown(text) else AnnotatedString(text)
    if (query.isBlank()) return annotatedString

    val builder = AnnotatedString.Builder(annotatedString)
    val highlightStyle = SpanStyle(
        background = MaterialTheme.colorScheme.primaryContainer,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        fontWeight = FontWeight.Bold
    )

    var startIndex = text.indexOf(query, ignoreCase = true)
    while (startIndex != -1) {
        val endIndex = startIndex + query.length
        builder.addStyle(highlightStyle, startIndex, endIndex)
        startIndex = text.indexOf(query, startIndex + query.length, ignoreCase = true)
    }

    return builder.toAnnotatedString()
}

class HistoryManager {
    private val undoStack = mutableListOf<TextFieldValue>()
    private val redoStack = mutableListOf<TextFieldValue>()
    private val maxHistorySize = 50

    fun pushState(state: TextFieldValue) {
        if (undoStack.isNotEmpty() && undoStack.last().text == state.text) return
        undoStack.add(state)
        if (undoStack.size > maxHistorySize) undoStack.removeAt(0)
        redoStack.clear()
    }

    fun undo(currentState: TextFieldValue): TextFieldValue? {
        if (undoStack.isEmpty()) return null
        redoStack.add(currentState)
        return undoStack.removeAt(undoStack.size - 1)
    }

    fun redo(currentState: TextFieldValue): TextFieldValue? {
        if (redoStack.isEmpty()) return null
        undoStack.add(currentState)
        return redoStack.removeAt(redoStack.size - 1)
    }
    
    fun canUndo() = undoStack.isNotEmpty()
    fun canRedo() = redoStack.isNotEmpty()
}

class MarkdownVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(parseMarkdown(text.text), OffsetMapping.Identity)
    }
}