const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");
const { getFirestore } = require("firebase-admin/firestore");

initializeApp();

exports.sendPushNotificationOnCreated = onDocumentCreated(
  {
    document: "notifications/{notificationId}",
    region: "europe-west1",
  },
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;

    const data = snapshot.data();
    const { title, body, titleEs, titleEn, bodyEs, bodyEn, isGlobal, targetUserIds, senderUid, listId } = data;

    if (!title || !body) return;

    // Payload de datos de Alta Prioridad para permitir que la app cliente muestre la notificación en su idioma nativo (ES / EN)
    const messagingPayload = {
      data: {
        title: title,
        body: body,
        titleEs: titleEs || title,
        titleEn: titleEn || title,
        bodyEs: bodyEs || body,
        bodyEn: bodyEn || body,
        senderUid: senderUid || "",
        listId: listId ? String(listId) : "",
      },
      android: {
        priority: "high",
      },
    };

    try {
      const db = getFirestore();
      if (isGlobal) {
        // Notificación de nueva reseña: enviar a todos los usuarios excepto al autor
        console.log(`Enviando notificación global de reseña excluyendo al autor (${senderUid})`);
        const usersSnap = await db.collection("users").get();
        for (const doc of usersSnap.docs) {
          const uid = doc.id;
          if (uid === senderUid) continue;

          const fcmToken = doc.data()?.fcmToken;
          if (fcmToken) {
            console.log(`Enviando notificación Push de reseña a usuario ${uid}`);
            await getMessaging().send({
              token: fcmToken,
              ...messagingPayload,
            });
          }
        }
      } else if (Array.isArray(targetUserIds) && targetUserIds.length > 0) {
        // Notificación de película en lista compartida: enviar a miembros de la lista excepto al emisor
        for (const uid of targetUserIds) {
          if (uid === senderUid) continue;

          const userDoc = await db.collection("users").doc(uid).get();
          if (userDoc.exists) {
            const fcmToken = userDoc.data()?.fcmToken;
            if (fcmToken) {
              console.log(`Enviando notificación Push de lista a usuario ${uid}`);
              await getMessaging().send({
                token: fcmToken,
                ...messagingPayload,
              });
            }
          }
        }
      }
    } catch (error) {
      console.error("Error al enviar notificación FCM:", error);
    }
  }
);
