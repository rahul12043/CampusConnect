
package com.example.campusconnect
import android.app.Application
import com.cloudinary.android.MediaManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Cloudinary
        val config = mapOf(
            "cloud_name" to "dniaiovio",
            "api_key" to "311587717353395",
            "api_secret" to "aVYyc9xXNF_NL2uAAkyVRo_27lY"
        )

        MediaManager.init(this, config)
    }
}