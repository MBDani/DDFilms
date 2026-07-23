package com.merino.ddfilms.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Locale

class DDFilmsMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo FCM Token recibido: $token")
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Mensaje FCM recibido de: ${remoteMessage.from}")

        val data = remoteMessage.data
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val senderUid = data["senderUid"]
        if (currentUid != null && senderUid == currentUid) {
            Log.d(TAG, "Omitiendo notificación recibida creada por el propio usuario ($senderUid)")
            return
        }

        val lang = Locale.getDefault().language
        val isSpanish = lang.lowercase().startsWith("es")

        val title = if (isSpanish) {
            data["titleEs"] ?: remoteMessage.notification?.title ?: data["title"] ?: "DDFilms"
        } else {
            data["titleEn"] ?: remoteMessage.notification?.title ?: data["title"] ?: "DDFilms"
        }

        val body = if (isSpanish) {
            data["bodyEs"] ?: remoteMessage.notification?.body ?: data["body"] ?: ""
        } else {
            data["bodyEn"] ?: remoteMessage.notification?.body ?: data["body"] ?: ""
        }

        if (body.isNotEmpty()) {
            NotificationHelper.showNotification(applicationContext, title, body)
        }
    }

    private fun saveTokenToFirestore(token: String) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseFirestore.getInstance().collection("users").document(currentUid)
        userRef.update("fcmToken", token)
            .addOnFailureListener {
                val data = HashMap<String, Any>()
                data["fcmToken"] = token
                userRef.set(data, com.google.firebase.firestore.SetOptions.merge())
            }
    }

    companion object {
        private const val TAG = "DDFilmsMessagingService"
    }
}
