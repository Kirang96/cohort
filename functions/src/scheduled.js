const { onSchedule, logger, db, admin } = require('./init');
const { sendNotification } = require('./common');

const sendChatExpiryWarnings = onSchedule("every 30 minutes", async (event) => {
    logger.info("Running sendChatExpiryWarnings...");
    const now = admin.firestore.Timestamp.now();
    const nowMs = now.toMillis();
    const minExpiry = admin.firestore.Timestamp.fromMillis(nowMs + (90 * 60 * 1000));
    const maxExpiry = admin.firestore.Timestamp.fromMillis(nowMs + (120 * 60 * 1000));

    try {
        const chatsSnapshot = await db.collection('chats')
            .where('status', '==', 'active')
            .where('expires_at', '>=', minExpiry)
            .where('expires_at', '<=', maxExpiry)
            .get();

        for (const doc of chatsSnapshot.docs) {
            const chat = doc.data();
            const chatId = doc.id;
            const users = [chat.user_a, chat.user_b];
            for (const userId of users) {
                await sendNotification(userId, {
                    type: "CHAT_EXPIRING",
                    entity_id: chatId,
                    title: "Chat Expiring Soon",
                    body: "Your chat will expire in about 2 hours. Continue talking!"
                });
            }
        }
        logger.info(`Sent warnings for ${chatsSnapshot.size} chats`);
    } catch (error) {
        logger.error(`sendChatExpiryWarnings Error: ${error.message}`);
    }
});

const liftExpiredRestrictions = onSchedule("every 1 hours", async (event) => {
    const now = admin.firestore.Timestamp.now();
    const snapshot = await db.collection('users')
        .where('restriction_expires_at', '<', now)
        .where('restriction_level', 'in', ['limited', 'blocked'])
        .get();

    if (snapshot.empty) return;

    const batch = db.batch();
    snapshot.forEach(doc => {
        batch.update(doc.ref, {
            restriction_level: 'none',
            restriction_reason: admin.firestore.FieldValue.delete(),
            restriction_expires_at: admin.firestore.FieldValue.delete()
        });
    });
    await batch.commit();
    logger.info(`Lifted restrictions for ${snapshot.size} users`);
});

// TODO: Fix pubsub.schedule compatibility for these functions
// exports.closeExpiredPools = ...
// exports.runPendingMatchmaking = ...
// exports.archiveOldPools = ...

module.exports = {
    sendChatExpiryWarnings,
    liftExpiredRestrictions
};
