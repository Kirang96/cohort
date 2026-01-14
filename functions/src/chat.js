const { onCall, HttpsError, logger, db, admin } = require('./init');
const {
    checkRestriction,
    enforceRateLimit,
    checkBlockStatus,
    sendNotification
} = require('./common');

const sendMessage = onCall(async (request) => {
    const auth = request.auth;
    const data = request.data;
    if (!auth) throw new HttpsError('unauthenticated', 'User must be logged in');

    await checkRestriction(auth.uid, 'blocked');
    await enforceRateLimit(auth.uid, 'sendMessage', 100, 60);

    const { matchId, text, image_url, image_width, image_height } = data;

    if (!matchId) throw new HttpsError('invalid-argument', 'Missing matchId');
    if (!text && !image_url) throw new HttpsError('invalid-argument', 'Text or image required');

    if (image_url && image_url.length > 500) {
        throw new HttpsError('invalid-argument', 'Invalid image URL');
    }

    const chatRef = db.collection('chats').doc(matchId);
    const chatSnap = await chatRef.get();
    if (!chatSnap.exists) throw new HttpsError('not-found', 'Chat not found');
    const chatData = chatSnap.data();

    if (chatData.user_a !== auth.uid && chatData.user_b !== auth.uid) {
        throw new HttpsError('permission-denied', 'Not a participant');
    }

    const targetUid = chatData.user_a === auth.uid ? chatData.user_b : chatData.user_a;
    const isBlocked = await checkBlockStatus(auth.uid, targetUid);
    if (isBlocked) throw new HttpsError('permission-denied', 'Message rejected (User Blocked)');

    const recipientId = await db.runTransaction(async (t) => {
        const chatDoc = await t.get(chatRef);
        if (!chatDoc.exists) throw new HttpsError('not-found', 'Chat not found');
        const chat = chatDoc.data();

        if (chat.user_a !== auth.uid && chat.user_b !== auth.uid) throw new HttpsError('permission-denied');
        if (chat.status === 'expired') throw new HttpsError('failed-precondition', 'Chat is expired');

        if (chat.status === 'active') {
            const now = admin.firestore.Timestamp.now();
            if (chat.expires_at && now.toMillis() > chat.expires_at.toMillis()) {
                throw new HttpsError('failed-precondition', 'Chat time limit reached');
            }
        }

        const msgRef = chatRef.collection('messages').doc();
        const messageData = {
            sender_id: auth.uid,
            text: text || null,
            sent_at: admin.firestore.Timestamp.now()
        };
        if (image_url) {
            messageData.image_url = image_url;
            messageData.image_width = image_width || null;
            messageData.image_height = image_height || null;
        }

        t.set(msgRef, messageData);

        const updateData = { last_message_at: admin.firestore.Timestamp.now() };
        if (chat.user_a === auth.uid) {
            updateData.unread_count_b = admin.firestore.FieldValue.increment(1);
        } else {
            updateData.unread_count_a = admin.firestore.FieldValue.increment(1);
        }
        t.update(chatRef, updateData);

        return chat.user_a === auth.uid ? chat.user_b : chat.user_a;
    });

    if (recipientId) {
        try {
            await sendNotification(recipientId, {
                type: "CHAT_MESSAGE",
                entity_id: matchId,
                title: "New Message",
                body: "You have a new message. Tap to read."
            });
        } catch (e) { }
    }

    return { success: true };
});

const requestImageUploadUrl = onCall(async (request) => {
    const auth = request.auth;
    const data = request.data;
    if (!auth) throw new HttpsError('unauthenticated', 'User must be logged in');

    const { matchId } = data;
    if (!matchId) throw new HttpsError('invalid-argument', 'Missing matchId');

    try {
        logger.info('requestImageUploadUrl: Starting', { matchId, uid: auth.uid });

        await checkRestriction(auth.uid, 'blocked');
        await enforceRateLimit(auth.uid, 'imageUpload', 20, 3600);

        const chatRef = db.collection('chats').doc(matchId);
        const chatDoc = await chatRef.get();
        if (!chatDoc.exists) throw new HttpsError('not-found', 'Chat not found');
        const chat = chatDoc.data();

        if (chat.user_a !== auth.uid && chat.user_b !== auth.uid) throw new HttpsError('permission-denied', 'Not a participant');
        if (chat.status !== 'active' && chat.status !== 'continued') throw new HttpsError('failed-precondition', 'Chat is not active');

        if (chat.status === 'active') {
            const now = admin.firestore.Timestamp.now();
            if (chat.expires_at && now.toMillis() > chat.expires_at.toMillis()) {
                throw new HttpsError('failed-precondition', 'Chat has expired');
            }
        }

        const otherUid = chat.user_a === auth.uid ? chat.user_b : chat.user_a;
        const isBlocked = await checkBlockStatus(auth.uid, otherUid);
        if (isBlocked) throw new HttpsError('permission-denied', 'User Blocked');

        const messageId = chatRef.collection('messages').doc().id;
        const storagePath = `chat_images/${matchId}/${messageId}.jpg`;

        // Get bucket name - this is where INTERNAL error was happening
        logger.info('requestImageUploadUrl: Getting bucket...');
        const bucket = admin.storage().bucket();
        const bucketName = bucket.name;
        logger.info('requestImageUploadUrl: Bucket name = ' + bucketName);

        const downloadUrl = `https://firebasestorage.googleapis.com/v0/b/${bucketName}/o/${encodeURIComponent(storagePath)}?alt=media`;

        logger.info('requestImageUploadUrl: SUCCESS', { storagePath, downloadUrl });

        return {
            storagePath,
            downloadUrl,
            messageId,
            bucketName
        };
    } catch (error) {
        logger.error('requestImageUploadUrl FAILED:', error);
        // Re-throw HttpsError as-is, wrap others
        if (error.code) {
            throw error; // Already an HttpsError
        }
        throw new HttpsError('internal', `Image upload setup failed: ${error.message}`);
    }
});

const requestChatContinuation = onCall(async (request) => {
    const auth = request.auth;
    const data = request.data;
    if (!auth) throw new HttpsError('unauthenticated', 'User must be logged in');

    const { matchId } = data;
    if (!matchId) throw new HttpsError('invalid-argument', 'Missing matchId');

    await checkRestriction(auth.uid, 'blocked');
    await enforceRateLimit(auth.uid, 'continuation', 2, 86400, matchId);

    const chatRef = db.collection('chats').doc(matchId);
    const chatSnap = await chatRef.get();
    if (!chatSnap.exists) throw new HttpsError('not-found', 'Chat not found');
    const chatData = chatSnap.data();

    const otherUid = chatData.user_a === auth.uid ? chatData.user_b : chatData.user_a;
    const isBlocked = await checkBlockStatus(auth.uid, otherUid);
    if (isBlocked) throw new HttpsError('permission-denied', 'User Blocked');

    const result = await db.runTransaction(async (t) => {
        const chatDoc = await t.get(chatRef);
        const chat = chatDoc.data();

        if (chat.status !== 'active') {
            if (chat.status === 'continued') return { status: 'continued' };
            throw new HttpsError('failed-precondition', 'Chat cannot be continued');
        }

        const currentContinuations = chat.continued_by || [];
        if (!currentContinuations.includes(auth.uid)) {
            currentContinuations.push(auth.uid);
        }

        let newStatus = 'active';
        let newExpiresAt = chat.expires_at;

        if (currentContinuations.includes(chat.user_a) && currentContinuations.includes(chat.user_b)) {
            newStatus = 'continued';
            newExpiresAt = null;
        }

        t.update(chatRef, {
            continued_by: currentContinuations,
            status: newStatus,
            expires_at: newExpiresAt
        });

        return { status: newStatus, waiting_for_other: newStatus === 'active', user_a: chat.user_a, user_b: chat.user_b };
    });

    if (result.status === 'continued') {
        try {
            await sendNotification(result.user_a, { type: "CHAT_CONTINUED", entity_id: matchId, title: "Chat Continued!", body: "Chat will no longer expire." });
            await sendNotification(result.user_b, { type: "CHAT_CONTINUED", entity_id: matchId, title: "Chat Continued!", body: "Chat will no longer expire." });
        } catch (e) { }
    }

    return result;
});

const expireChats = onCall(async (request) => {
    const now = admin.firestore.Timestamp.now();
    const expirableChatsQuery = db.collection('chats')
        .where('status', '==', 'active')
        .where('expires_at', '<', now)
        .limit(500);

    const snapshot = await expirableChatsQuery.get();
    if (snapshot.empty) return { expired_count: 0 };

    const batch = db.batch();
    snapshot.docs.forEach(doc => {
        batch.update(doc.ref, { status: 'expired' });
    });

    await batch.commit();

    for (const doc of snapshot.docs) {
        const chat = doc.data();
        const chatId = doc.id;
        try {
            await sendNotification(chat.user_a, { type: "CHAT_EXPIRED", entity_id: chatId, title: "Chat Expired", body: "Chat has expired." });
            await sendNotification(chat.user_b, { type: "CHAT_EXPIRED", entity_id: chatId, title: "Chat Expired", body: "Chat has expired." });
        } catch (e) { }
    }

    return { expired_count: snapshot.size };
});

const markChatRead = onCall(async (request) => {
    const auth = request.auth;
    if (!auth) throw new HttpsError('unauthenticated');
    const { matchId } = request.data;
    if (!matchId) throw new HttpsError('invalid-argument');

    const chatRef = db.collection('chats').doc(matchId);
    const chatDoc = await chatRef.get();
    if (!chatDoc.exists) throw new HttpsError('not-found');
    const chat = chatDoc.data();

    let updateData = {};
    if (chat.user_a === auth.uid) updateData.unread_count_a = 0;
    else if (chat.user_b === auth.uid) updateData.unread_count_b = 0;
    else throw new HttpsError('permission-denied');

    await chatRef.update(updateData);
    return { success: true };
});

module.exports = {
    sendMessage,
    requestImageUploadUrl,
    requestChatContinuation,
    expireChats,
    markChatRead
};
