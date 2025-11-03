package com.example.campusconnect.navigation

sealed class Screen(val route: String) {
    // Authentication
    data object Login : Screen("login_screen")
    data object Register : Screen("register_screen")

    // Main Student Dashboard
    data object Home : Screen("home_screen")

    // Core Features
    data object DigitalQueue : Screen("digital_queue_screen")
    data object LostAndFound : Screen("lost_and_found_screen")
    data object ReportLostFoundItem : Screen("report_lost_found_item")

    // Placeholder Features
    data object MindMingle : Screen("mind_mingle_screen")
    data object PeerSkill : Screen("peer_skill_screen")
    data object IdeaIncubator : Screen("idea_incubator_screen")

    // --- NEW: This is the route for creating a new Peer Skill request ---
    data object CreateSkillRequest : Screen("create_skill_request") // <-- THIS IS THE FIX

    data object NoteSharing : Screen("note_sharing_screen")
    data object CreateNote : Screen("create_note_screen")

    // --- UPDATED: Faculty Connect Feature ---
    data object FacultyConnect : Screen("faculty_connect_list")
    data object FlashcardGenerator : Screen("flashcard_generator")
    data object FlashcardViewer : Screen("flashcard_viewer")

    // --- NEW: This is the route for a specific faculty member's detail page.
    data object FacultyDetail : Screen("faculty_detail/{facultyId}")
}