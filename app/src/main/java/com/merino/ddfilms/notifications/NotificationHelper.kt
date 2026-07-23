package com.merino.ddfilms.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.merino.ddfilms.R
import com.merino.ddfilms.ui.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID = "ddfilms_notifications_channel"
    private const val CHANNEL_NAME = "DDFilms Notificaciones"
    private const val CHANNEL_DESC = "Notificaciones de listas compartidas y reseñas"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt(),
        intent: Intent? = null
    ) {
        val prefs = context.getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        if (!notificationsEnabled) return

        createNotificationChannel(context)

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent ?: Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.accent_red))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    fun subscribeToGeneralTopics() {
        FirebaseMessaging.getInstance().subscribeToTopic("all_reviews")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("NotificationHelper", "Subscripto con éxito a 'all_reviews'")
                }
            }
    }

    fun subscribeToListTopic(listId: String) {
        if (listId.isEmpty()) return
        val topicName = "list_$listId"
        FirebaseMessaging.getInstance().subscribeToTopic(topicName)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("NotificationHelper", "Subscripto a topic $topicName")
                }
            }
    }
}
