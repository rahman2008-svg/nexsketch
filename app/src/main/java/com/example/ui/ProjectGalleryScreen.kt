package com.example.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.data.CanvasTemplate
import com.example.data.Project
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectGalleryScreen(
    viewModel: DrawingViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.allProjects.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Dropdown menu context states
    var selectedProjectForMenu by remember { mutableStateOf<Project?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInputName by remember { mutableStateOf("") }

    val filteredProjects = remember(projects, searchQuery) {
        if (searchQuery.isBlank()) {
            projects
        } else {
            projects.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(com.example.ui.theme.ElegantDarkBackground) // Midnight dark AMOLED base
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // App Hero Header Title
            Spacer(modifier = Modifier.height(60.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NexSketch",
                        color = com.example.ui.theme.ElegantTextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "OFFLINE SYSTEM • ART STUDIO",
                        color = com.example.ui.theme.ElegantTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                
                // Developer credit pill shape
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(com.example.ui.theme.ElegantLavender.copy(alpha = 0.15f))
                        .border(1.dp, com.example.ui.theme.ElegantLavender.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { showAboutDialog = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("about_dialog_trigger")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About Developer & Company",
                            tint = com.example.ui.theme.ElegantLavender,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "About",
                            color = com.example.ui.theme.ElegantLavender,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Search Bar Component
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search projects...", color = com.example.ui.theme.ElegantTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = com.example.ui.theme.ElegantTextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = com.example.ui.theme.ElegantTextSecondary)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gallery_search"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = com.example.ui.theme.ElegantTextPrimary,
                    unfocusedTextColor = com.example.ui.theme.ElegantTextPrimary,
                    focusedBorderColor = com.example.ui.theme.ElegantLavender,
                    unfocusedBorderColor = com.example.ui.theme.ElegantBorder,
                    focusedContainerColor = com.example.ui.theme.ElegantDarkSurface,
                    unfocusedContainerColor = com.example.ui.theme.ElegantDarkSurface
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Project Grid Content
            if (filteredProjects.isEmpty()) {
                EmptyProjectsState(onNewProjectClick = { showCreateDialog = true })
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    items(filteredProjects) { project ->
                        ProjectGridCard(
                            project = project,
                            onClick = {
                                viewModel.loadProject(project)
                                viewModel.isGalleryMode.value = false
                            },
                            onLongClick = {
                                selectedProjectForMenu = project
                                renameInputName = project.name
                                menuExpanded = true
                            }
                        )
                    }
                }
            }
        }

        // Expanded Options Dropdown Menu anchor
        Box(modifier = Modifier.align(Alignment.Center)) {
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(Color(0xFF1E1E2C))
            ) {
                DropdownMenuItem(
                    text = { Text("Load Sketch", color = Color.White) },
                    onClick = {
                        menuExpanded = false
                        selectedProjectForMenu?.let {
                            viewModel.loadProject(it)
                            viewModel.isGalleryMode.value = false
                        }
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White) }
                )
                DropdownMenuItem(
                    text = { Text("Duplicate Project", color = Color.White) },
                    onClick = {
                        menuExpanded = false
                        selectedProjectForMenu?.let { viewModel.duplicateProject(it) }
                    },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White) }
                )
                DropdownMenuItem(
                    text = { Text("Rename", color = Color.White) },
                    onClick = {
                        menuExpanded = false
                        showRenameDialog = true
                    },
                    leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = Color.White) }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                DropdownMenuItem(
                    text = { Text("Delete", color = Color.Red) },
                    onClick = {
                        menuExpanded = false
                        selectedProjectForMenu?.let { viewModel.deleteProject(it) }
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                )
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .navigationBarsPadding()
                .testTag("new_project_fab"),
            containerColor = com.example.ui.theme.ElegantLavender,
            contentColor = Color(0xFF13131A), // Dark purple/black theme color
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create New Sketch",
                modifier = Modifier.size(28.dp)
            )
        }

        // Configure Create Project Dialog
        if (showCreateDialog) {
            CreateProjectDialog(
                onDismiss = { showCreateDialog = false },
                onConfirm = { name, template, bgColorInt, width, height ->
                    showCreateDialog = false
                    viewModel.createProject(name, template, bgColorInt, width, height)
                }
            )
        }

        // Configure Rename Dialog
        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename Project", color = Color.White) },
                containerColor = Color(0xFF1E1E2C),
                text = {
                    OutlinedTextField(
                        value = renameInputName,
                        onValueChange = { renameInputName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showRenameDialog = false
                            selectedProjectForMenu?.let { viewModel.renameProject(it, renameInputName) }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            )
        }

        if (showAboutDialog) {
            AboutDeveloperDialog(
                onDismiss = { showAboutDialog = false }
            )
        }
    }
}

@Composable
fun AboutDeveloperDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.ElegantDarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(1.dp, com.example.ui.theme.ElegantBorder, RoundedCornerShape(24.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Title Area
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = com.example.ui.theme.ElegantLavender,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Developer Hub",
                            color = com.example.ui.theme.ElegantTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = com.example.ui.theme.ElegantTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // About Developer Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.ElegantDarkInnerSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, com.example.ui.theme.ElegantBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.ElegantLavender,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "About Developer",
                                    color = com.example.ui.theme.ElegantLavender,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Prince AR Abdur Rahman",
                                color = com.example.ui.theme.ElegantTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Independent App Developer passionate about building modern Android applications, productivity tools, AI-powered experiences, media players, educational apps, and next-generation digital products.",
                                color = com.example.ui.theme.ElegantTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "CONNECT VIA WHATSAPP",
                                color = com.example.ui.theme.ElegantTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // WhatsApp Line 1
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(com.example.ui.theme.ElegantDarkSurface)
                                    .clickable {
                                        uriHandler.openUri("https://wa.me/8801707424006")
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "WhatsApp",
                                        tint = Color(0xFF25D366),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "+8801707424006",
                                        color = com.example.ui.theme.ElegantTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row {
                                    TextButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString("01707424006"))
                                            android.widget.Toast.makeText(context, "Number copied to Clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Copy", color = com.example.ui.theme.ElegantLavender, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // WhatsApp Line 2
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(com.example.ui.theme.ElegantDarkSurface)
                                    .clickable {
                                        uriHandler.openUri("https://wa.me/8801796951709")
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "WhatsApp",
                                        tint = Color(0xFF25D366),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "+8801796951709",
                                        color = com.example.ui.theme.ElegantTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row {
                                    TextButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString("01796951709"))
                                            android.widget.Toast.makeText(context, "Number copied to Clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Copy", color = com.example.ui.theme.ElegantLavender, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "SOCIAL NETWORKS",
                                color = com.example.ui.theme.ElegantTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Facebook Button
                                Button(
                                    onClick = { uriHandler.openUri("https://www.facebook.com/share/1BNn32qoJo/") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Facebook", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // Instagram Button
                                Button(
                                    onClick = { uriHandler.openUri("https://www.instagram.com/ur___abdur____rahman__2008") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Instagram", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // About Company Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.ElegantDarkInnerSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, com.example.ui.theme.ElegantBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.ElegantLavender,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "About Company",
                                    color = com.example.ui.theme.ElegantLavender,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "NexVora Lab's Ofc",
                                color = com.example.ui.theme.ElegantTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "NexVora Lab's Ofc focuses on creating innovative Android applications designed to improve productivity, entertainment, learning, and digital experiences.",
                                color = com.example.ui.theme.ElegantTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "MISSION",
                                color = com.example.ui.theme.ElegantTextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Build fast, beautiful, privacy-friendly, and user-focused applications accessible to everyone.",
                                color = com.example.ui.theme.ElegantTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "PORTFOLIO & PRODUCTS",
                                color = com.example.ui.theme.ElegantTextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• NexSketch Creator Studio (Flagship High-Performance Sketching Hub)",
                                color = com.example.ui.theme.ElegantTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Technical Info Card (Credits)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.ElegantDarkInnerSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, com.example.ui.theme.ElegantBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.ElegantLavender,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Technical Specifications",
                                    color = com.example.ui.theme.ElegantLavender,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("App Version", color = com.example.ui.theme.ElegantTextSecondary, fontSize = 12.sp)
                                Text("1.0.0", color = com.example.ui.theme.ElegantTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = com.example.ui.theme.ElegantBorder.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Developed By", color = com.example.ui.theme.ElegantTextSecondary, fontSize = 12.sp)
                                Text("Prince AR Abdur Rahman", color = com.example.ui.theme.ElegantTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = com.example.ui.theme.ElegantBorder.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Published By", color = com.example.ui.theme.ElegantTextSecondary, fontSize = 12.sp)
                                Text("NexVora Lab's Ofc", color = com.example.ui.theme.ElegantTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Footer Copyright block
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "© 2026 NexVora Lab's Ofc. All Rights Reserved.",
                            color = com.example.ui.theme.ElegantTextMuted,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.example.ui.theme.ElegantLavender,
                        contentColor = Color(0xFF13131A)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close Hub", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectGridCard(
    project: Project,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val formattedDate = remember(project.updatedAt) {
        val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
        sdf.format(Date(project.updatedAt))
    }

    val templateLabel = remember(project.canvasTemplate) {
        project.canvasTemplate.lowercase().replaceFirstChar { it.uppercase() }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.ElegantDarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .border(1.dp, com.example.ui.theme.ElegantBorder, RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("project_item_${project.id}")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Visual layout miniature preview representation layer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(project.canvasBgColor))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Subtle symbol detailing template format
                Icon(
                    imageVector = when (project.canvasTemplate) {
                        "GRID" -> Icons.Default.GridOn
                        "DOT" -> Icons.Default.Grid3x3
                        "PERSPECTIVE" -> Icons.Default.ViewInAr
                        else -> Icons.Default.FilterFrames
                    },
                    contentDescription = null,
                    tint = if (project.canvasBgColor == -1) Color.Gray.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxSize(0.5f)
                )

                // Show basic indicator if paper color matches background dark mode
                if (project.canvasBgColor == 0xFF000000.toInt() || project.canvasBgColor == 0xFF1C1C24.toInt() || project.canvasBgColor == 0xFF1A1C1E.toInt()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("AMOLED Paper", color = com.example.ui.theme.ElegantTextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Info summary metadata segment
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(com.example.ui.theme.ElegantDarkInnerSurface)
                    .padding(12.dp)
            ) {
                Text(
                    text = project.name,
                    color = com.example.ui.theme.ElegantTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${project.canvasWidth.toInt()}×${project.canvasHeight.toInt()}",
                        color = com.example.ui.theme.ElegantTextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = templateLabel,
                        color = com.example.ui.theme.ElegantLavender,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = formattedDate,
                    color = com.example.ui.theme.ElegantTextMuted,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EmptyProjectsState(
    onNewProjectClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("empty_projects_view"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Decorative geometric canvas symbol
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Create Your First Masterpiece",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Draw, sketch, paint, design, and explore perspective entirely offline. Your files are saved securely on device with no cloud snooping.",
            color = Color.Gray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNewProjectClick,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Sandbox Sketch")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, CanvasTemplate, Int, Float, Float) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf(CanvasTemplate.BLANK) }

    // Color swatches mapping
    val paperColorOptions = listOf(
        Pair("Plain White", 0xFFFFFFFF.toInt()),
        Pair("Cream Beige", 0xFFFFFDF8.toInt()),
        Pair("Charcoal", 0xFF1C1C24.toInt()),
        Pair("AMOLED Black", 0xFF000000.toInt())
    )
    var selectedColorIndex by remember { mutableStateOf(0) }

    // Dimension preset models
    val ratioPresets = listOf(
        Pair("1:1 Square (1080×1080)", Pair(1080f, 1080f)),
        Pair("9:16 Portrait (1080×1920)", Pair(1080f, 1920f)),
        Pair("16:9 Landscape (1920×1080)", Pair(1920f, 1080f)),
        Pair("3:4 Tablet (1536×2048)", Pair(1536f, 2048f)),
        Pair("Ultra HD 4K (2160×3840)", Pair(2160f, 3840f))
    )
    var selectedRatioPresetIndex by remember { mutableStateOf(1) } // Default 9:16 portrait

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.ElegantDarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(1.dp, com.example.ui.theme.ElegantBorder, RoundedCornerShape(24.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Initialize Sketch Setup",
                    color = com.example.ui.theme.ElegantTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("E.g. Portrait Sketch, Charcoal landscape", color = com.example.ui.theme.ElegantTextSecondary, fontSize = 13.sp) },
                    label = { Text("Project Title", color = com.example.ui.theme.ElegantTextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = com.example.ui.theme.ElegantTextPrimary,
                        unfocusedTextColor = com.example.ui.theme.ElegantTextPrimary,
                        focusedBorderColor = com.example.ui.theme.ElegantLavender,
                        unfocusedBorderColor = com.example.ui.theme.ElegantBorder,
                        focusedLabelColor = com.example.ui.theme.ElegantLavender,
                        unfocusedLabelColor = com.example.ui.theme.ElegantBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Grid Templates choice
                Text("Select Blueprint Template", color = com.example.ui.theme.ElegantTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CanvasTemplate.values().forEach { temp ->
                        val active = selectedTemplate == temp
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) com.example.ui.theme.ElegantLavender else com.example.ui.theme.ElegantDarkInnerSurface)
                                .border(1.dp, if (active) Color.White.copy(alpha = 0.6f) else com.example.ui.theme.ElegantBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedTemplate = temp }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = temp.name,
                                color = if (active) Color(0xFF13131A) else com.example.ui.theme.ElegantTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sheet Color Options
                Text("Art Sheet Background Paper", color = com.example.ui.theme.ElegantTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paperColorOptions.forEachIndexed { index, pair ->
                        val active = selectedColorIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(pair.second))
                                .border(
                                    width = if (active) 3.dp else 1.dp,
                                    color = if (active) com.example.ui.theme.ElegantLavender else com.example.ui.theme.ElegantBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedColorIndex = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pair.first.split(" ").last(),
                                color = if (pair.second == -1 || pair.second == 0xFFFFFDF8.toInt() || pair.second == 0xFFFFFFFF.toInt()) Color.Black else Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Canvas presets Aspect ratio
                Text("Canvas Resolution & Aspect Ratio", color = com.example.ui.theme.ElegantTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ratioPresets.forEachIndexed { index, pair ->
                        val active = selectedRatioPresetIndex == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) com.example.ui.theme.ElegantLavender.copy(alpha = 0.15f) else com.example.ui.theme.ElegantDarkInnerSurface)
                                .border(
                                    width = 1.dp,
                                    color = if (active) com.example.ui.theme.ElegantLavender else com.example.ui.theme.ElegantBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedRatioPresetIndex = index }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = pair.first,
                                color = if (active) com.example.ui.theme.ElegantTextPrimary else com.example.ui.theme.ElegantTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = com.example.ui.theme.ElegantTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val presetDimensions = ratioPresets[selectedRatioPresetIndex].second
                            val chosenColor = paperColorOptions[selectedColorIndex].second
                            onConfirm(
                                name,
                                selectedTemplate,
                                chosenColor,
                                presetDimensions.first,
                                presetDimensions.second
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.example.ui.theme.ElegantLavender,
                            contentColor = Color(0xFF13131A)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Create Studio", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
