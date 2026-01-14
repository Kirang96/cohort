const { onCall, HttpsError, logger, db, admin } = require('./init');

const registerDeviceToken = onCall(async (request) => {
    const auth = request.auth;
    const data = request.data;
    if (!auth) throw new HttpsError('unauthenticated', 'User must be logged in');

    const userId = auth.uid;
    const fcmToken = data.fcm_token;
    if (!fcmToken) throw new HttpsError('invalid-argument', 'FCM token is required');

    try {
        const existingSnapshot = await db.collection('user_devices')
            .where('user_id', '==', userId)
            .where('fcm_token', '==', fcmToken)
            .limit(1).get();

        const now = admin.firestore.Timestamp.now();

        if (existingSnapshot.empty) {
            await db.collection('user_devices').add({
                user_id: userId,
                fcm_token: fcmToken,
                platform: 'android',
                created_at: now,
                last_seen_at: now
            });
        } else {
            await existingSnapshot.docs[0].ref.update({ last_seen_at: now });
        }

        const deviceCount = (await db.collection('user_devices').where('user_id', '==', userId).get()).size;
        return { success: true, device_count: deviceCount };
    } catch (error) {
        logger.error(`Register Token Error: ${error.message}`);
        throw new HttpsError('internal', 'Failed to register device token');
    }
});

const unregisterDeviceToken = onCall(async (request) => {
    const data = request.data;
    const fcmToken = data.fcm_token;
    if (!fcmToken) throw new HttpsError('invalid-argument', 'FCM token is required');

    try {
        const snapshot = await db.collection('user_devices').where('fcm_token', '==', fcmToken).get();
        if (snapshot.empty) return { success: true, count: 0 };

        const batch = db.batch();
        snapshot.docs.forEach(doc => batch.delete(doc.ref));

        if (request.auth) {
            const userRef = db.collection('users').doc(request.auth.uid);
            batch.update(userRef, { last_logout_at: admin.firestore.Timestamp.now() });
        }

        await batch.commit();
        return { success: true, count: snapshot.size };
    } catch (error) {
        logger.error(`Unregister Token Error: ${error.message}`);
        throw new HttpsError('internal', 'Failed to unregister token');
    }
});

module.exports = {
    registerDeviceToken,
    unregisterDeviceToken
};
