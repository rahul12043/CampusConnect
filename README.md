# CampusConnect 🎓

CampusConnect is a comprehensive mobile application designed to enhance student collaboration, streamline campus efficiency, and foster a culture of interactive learning. By integrating peer-to-peer knowledge sharing, AI-powered study tools, and robust administrative controls, CampusConnect serves as the ultimate digital hub for modern campuses.

## Key Features

### AI & Collaborative Learning
* **AI Flashcards:** Smart, auto-generated flashcards that leverage AI to help students quickly revise and retain key concepts from their study materials.
* **AI Notes Summarize & Answer:** An intelligent study assistant that ingests class notes, provides concise summaries, and answers specific contextual queries.
* **PeerSkill Hub:** A collaborative exchange platform where students can connect to teach, learn, and trade micro-skills in short, focused sessions.
* **Note Sharing Ecosystem:** A community-driven repository where students can securely upload, browse, download, and upvote high-quality lecture notes.

### Campus Services
* **Cafeteria Order Management:** An end-to-end digital ordering system. Students can place orders and track their food status in real-time. Statuses progress from **ORDERED** → **PENDING** → **COMPLETED**, triggering instant push notifications at each stage.
* **Faculty Locator & Timetable:** A dedicated tab displaying faculty sitting locations, office hours, and current timetables, eliminating the guesswork of finding professors on campus.
* **Lost & Found:** A digital dashboard to report lost or found items, complete with an administrative approval workflow.

### Admin Controls & Moderation
* **Lost & Found Admin Panel:** Designated moderators can view incoming lost/found requests and choose to **Accept** or **Reject** them before they are published to the campus-wide feed, preventing spam.
* **Main Admin Dashboard:** A centralized control panel for campus authorities. The main admin can broadcast push-notification announcements to the entire student body and dynamically update faculty details, sitting locations, and timetables.

## How We Achieved Real-Time Functionality

CampusConnect feels fast and responsive because it is built on a real-time event-driven architecture rather than traditional pull-to-refresh REST APIs. 

* **Firestore `onSnapshot` Listeners:** Instead of fetching data once, the app opens a continuous listener to Cloud Firestore. When the Main Admin updates a faculty timetable or the Lost & Found Admin approves a post, the database pushes that change directly to the clients. The UI updates instantly across all active devices without the user having to refresh the page.
* **Firebase Cloud Functions & FCM:** For the Cafeteria system, when the cafeteria staff changes an order status to "COMPLETED" in the database, a backend Cloud Function is automatically triggered. This function generates a payload and routes it through Firebase Cloud Messaging (FCM), delivering an instant push notification directly to that specific student's phone.

## Technology Stack

**Frontend**
* **Android / Kotlin:** Native mobile development prioritizing a smooth, responsive UI.
* **XML:** Layout design and structuring.

**Backend & Cloud**
* **Firebase Authentication:** Secure login and role-based access control (Student vs. Admin).
* **Cloud Firestore:** NoSQL real-time database powering the live updates for notes, orders, and feeds.
* **Firebase Cloud Functions:** Serverless backend logic handling status triggers and automated tasks.
* **Firebase Cloud Messaging (FCM):** Infrastructure for targeted and broadcast push notifications.

**AI & Integrations**
* **AI Generative APIs:** Integrated LLMs powering the Note Summarization and Flashcard modules.

## Getting Started

### Prerequisites
* [Android Studio](https://developer.android.com/studio) (Latest Version)
* A Firebase Account
* Required SDKs: Android SDK 33+

### Installation

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/yourusername/CampusConnect.git](https://github.com/yourusername/CampusConnect.git)
    ```
2.  **Open the project:**
    Launch Android Studio and select `File > Open`, navigating to the cloned directory.
3.  **Connect to Firebase:**
    * Create a new project in the [Firebase Console](https://console.firebase.google.com/).
    * Register your app using the project's package name.
    * Download the `google-services.json` file and place it in the `app/` directory.
    * Enable **Authentication**, **Firestore**, **Storage**, and **Cloud Messaging**.
4.  **Set up API Keys:**
    * Obtain keys for your chosen AI service.
    * Create a `local.properties` file in the root directory:
        ```properties
        AI_API_KEY="your_api_key_here"
        ```
5.  **Build and Run:**
    Sync the Gradle files and deploy the application to an emulator or physical device.

