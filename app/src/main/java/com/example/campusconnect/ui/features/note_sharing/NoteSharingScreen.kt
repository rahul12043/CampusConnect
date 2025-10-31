package com.example.campusconnect.ui.features.note_sharing
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.campusconnect.auth.AuthViewModel
import com.example.campusconnect.data.NotePost
import com.example.campusconnect.data.Subject
import com.example.campusconnect.navigation.Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteSharingScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    noteViewModel: NoteSharingViewModel = viewModel()
) {
    val state by noteViewModel.state.collectAsState()
    val user by authViewModel.currentUser.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.CreateNote.route) }) {
                Icon(Icons.Default.Add, "Share Note")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            SubjectFilterRow(
                subjects = state.subjects,
                selectedSubject = state.selectedSubject,
                onSubjectSelected = { subjectName -> noteViewModel.filterBySubject(subjectName) }
            )

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                NoteList(
                    posts = state.posts,
                    userId = user?.uid ?: "",
                    onUpvoteClick = { post -> noteViewModel.toggleUpvote(post, user?.uid ?: "") }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectFilterRow(
    subjects: List<Subject>,
    selectedSubject: String?,
    onSubjectSelected: (String?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedSubject == null,
                onClick = { onSubjectSelected(null) },
                label = { Text("All") }
            )
        }
        items(subjects) { subject ->
            FilterChip(
                selected = selectedSubject == subject.name,
                onClick = { onSubjectSelected(subject.name) },
                label = { Text(subject.name) }
            )
        }
    }
}
@Composable
fun NoteList(posts: List<NotePost>, userId: String, onUpvoteClick: (NotePost) -> Unit) {
    if (posts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("No notes found for this subject.")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(posts) { post ->
                NotePostCard(
                    post = post,
                    hasUpvoted = post.upvotedBy.contains(userId),
                    onUpvoteClick = { onUpvoteClick(post) }
                )
            }
        }
    }
}
@Composable
fun NotePostCard(post: NotePost, hasUpvoted: Boolean, onUpvoteClick: () -> Unit) {
    val context = LocalContext.current
    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(post.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            val authorText = if (post.authorName.isNotBlank()) post.authorName else post.authorSapId
            Text("Shared by: $authorText", style = MaterialTheme.typography.bodySmall)
            Text("Subject: ${post.subject}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            post.description?.let {
                if (it.isNotBlank()) {
                    Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onUpvoteClick) {
                        Icon(
                            imageVector = if (hasUpvoted) Icons.Filled.ArrowUpward else Icons.Outlined.ArrowUpward,
                            contentDescription = "Upvote",
                            tint = if (hasUpvoted) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                    Text("${post.upvoteCount}", fontWeight = FontWeight.Bold)
                }

                Button(onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.fileUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Handle case where no app can open the URL
                    }
                }) {
                    Icon(Icons.Default.Download, "Download", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("View/Download")
                }
            }
        }
    }
}