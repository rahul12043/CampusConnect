package com.example.campusconnect.ui.features

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.material.icons.filled.AutoAwesome
import com.example.campusconnect.data.Announcement
import com.example.campusconnect.navigation.Screen

@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel()
) {
    val state by homeViewModel.state.collectAsState()

    val features = listOf(
        Feature("Digital Queue", Icons.Default.People, Screen.DigitalQueue.route),
        Feature("Lost & Found", Icons.Default.Search, Screen.LostAndFound.route),
        Feature("Peer Help", Icons.Default.Forum, Screen.NoteSharing.route),
        Feature("PeerSkill Hub", Icons.Default.School, Screen.PeerSkill.route),
        Feature("Idea Incubator", Icons.Default.Lightbulb, Screen.IdeaIncubator.route),
        Feature("Faculty Connect", Icons.Default.PersonPin, Screen.FacultyConnect.route),
                Feature("AI Flashcards", Icons.Default.AutoAwesome, Screen.FlashcardGenerator.route)
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(features) { feature ->
                FeatureCard(
                    feature = feature,
                    onClick = { navController.navigate(feature.route) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (state.announcements.isNotEmpty()) {
            AnnouncementsCarousel(announcements = state.announcements)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnnouncementsCarousel(announcements: List<Announcement>) {
    val pagerState = rememberPagerState(pageCount = { announcements.size })

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Latest Announcements",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 32.dp),
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            AnnouncementCard(announcement = announcements[page])
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            Modifier.wrapContentHeight(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) { iteration ->
                // --- STYLE CHANGE: Dots are now white to contrast with the page background ---
                val color = if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(10.dp)
                )
            }
        }
    }
}

@Composable
fun AnnouncementCard(announcement: Announcement) {
    // --- THIS IS THE CORE STYLE CHANGE ---
    val cardColors = CardDefaults.cardColors(
        // Set the background to your primary red with 90% opacity
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
        // Set the default text color for all content inside the card to white
        contentColor = Color.White
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        // Apply the rounded border shape
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        // Apply your custom colors
        colors = cardColors
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (announcement.isUrgent) {
                Text(
                    "URGENT",
                    // The color is now inherited from the card's contentColor (White)
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = announcement.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = announcement.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4
            )
        }
    }
}

@Composable
fun FeatureCard(feature: Feature, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = feature.name,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = feature.name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

data class Feature(val name: String, val icon: ImageVector, val route: String)