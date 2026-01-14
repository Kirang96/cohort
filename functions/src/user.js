const { onCall, HttpsError, logger, db, admin } = require('./init');
const {
    logSafetyEvent,
    checkBlockStatus,
    PENALTIES
} = require('./common');

async function evaluateTrustScore(userId, currentScore) {
    let newLevel = 'none';
    let newReason = null;
    let autoBan = false;

    if (currentScore < 40) {
        newLevel = 'blocked';
        newReason = 'trust_score_below_threshold';
        autoBan = true;
    } else if (currentScore < 60) {
        newLevel = 'blocked';
        newReason = 'trust_score_low';
    } else if (currentScore < 80) {
        newLevel = 'limited';
        newReason = 'trust_score_warning';
    }

    const updates = { restriction_level: newLevel };
    if (newReason) updates.restriction_reason = newReason;
    if (autoBan) updates.trust_flags = admin.firestore.FieldValue.arrayUnion('auto_banned');

    await db.collection('users').doc(userId).update(updates);
    await logSafetyEvent(userId, 'restriction_update', 'medium', { new_level: newLevel, score: currentScore, reason: newReason });
}

const blockUser = onCall(async (request) => {
    const auth = request.auth;
    const { blockedId, reason, matchId } = request.data;
    if (!auth) throw new HttpsError('unauthenticated', 'User must be logged in');
    if (!blockedId) throw new HttpsError('invalid-argument', 'Missing blockedId');

    const blockerId = auth.uid;
    let hasInteraction = false;

    // Logic to verify interaction (simplified from original for brevity but preserving intent)
    // Check Matches
    const matchQuery = await db.collection('matches').where(
        admin.firestore.Filter.or(
            admin.firestore.Filter.and(admin.firestore.Filter.where('user_a', '==', blockerId), admin.firestore.Filter.where('user_b', '==', blockedId)),
            admin.firestore.Filter.and(admin.firestore.Filter.where('user_a', '==', blockedId), admin.firestore.Filter.where('user_b', '==', blockerId))
        )
    ).limit(1).get();
    if (!matchQuery.empty) hasInteraction = true;

    if (!hasInteraction) {
        // Check Chats
        const chatQuery = await db.collection('chats').where(
            admin.firestore.Filter.or(
                admin.firestore.Filter.and(admin.firestore.Filter.where('user_a', '==', blockerId), admin.firestore.Filter.where('user_b', '==', blockedId)),
                admin.firestore.Filter.and(admin.firestore.Filter.where('user_a', '==', blockedId), admin.firestore.Filter.where('user_b', '==', blockerId))
            )
        ).limit(1).get();
        if (!chatQuery.empty) hasInteraction = true;
    }

    if (!hasInteraction) throw new HttpsError('permission-denied', 'You can only block users you have interacted with');

    await db.collection('user_blocks').add({
        blocker_id: blockerId,
        blocked_id: blockedId,
        reason: reason || null,
        created_at: admin.firestore.Timestamp.now()
    });

    // Update Chat if exists
    let interactionRef;
    if (matchId) interactionRef = db.collection('chats').doc(matchId);
    if (interactionRef) {
        await interactionRef.set({
            blocked_by: admin.firestore.FieldValue.arrayUnion(blockerId)
        }, { merge: true });
    }

    await logSafetyEvent(blockerId, 'user_blocked', 'medium', { blocked_id: blockedId, reason: reason });
    return { success: true };
});

const unblockUser = onCall(async (request) => {
    const auth = request.auth;
    const { blockedId, matchId } = request.data;
    if (!auth) throw new HttpsError('unauthenticated', 'User must be logged in');

    const snapshot = await db.collection('user_blocks')
        .where('blocker_id', '==', auth.uid)
        .where('blocked_id', '==', blockedId).get();

    const batch = db.batch();
    snapshot.forEach(doc => batch.delete(doc.ref));

    // Update Chat if exists
    let interactionRef;
    if (matchId) interactionRef = db.collection('chats').doc(matchId);
    if (interactionRef) {
        batch.set(interactionRef, {
            blocked_by: admin.firestore.FieldValue.arrayRemove(auth.uid)
        }, { merge: true });
    }

    await batch.commit();
    await logSafetyEvent(auth.uid, 'user_unblocked', 'low', { unblocked_id: blockedId });
    return { success: true };
});

const reportUser = onCall(async (request) => {
    const auth = request.auth;
    const { reportedId, reason, context, matchId } = request.data;
    if (!auth) throw new HttpsError('unauthenticated', 'User must be logged in');
    if (!reportedId || !reason) throw new HttpsError('invalid-argument', 'Missing fields');

    const penalty = PENALTIES[reason] || 5;

    await db.collection('user_reports').add({
        reporter_id: auth.uid,
        reported_id: reportedId,
        reason: reason,
        context: context || null,
        created_at: admin.firestore.Timestamp.now()
    });

    if (penalty > 0) {
        const targetRef = db.collection('users').doc(reportedId);
        const targetDoc = await targetRef.get();
        if (targetDoc.exists) {
            const currentScore = targetDoc.data()?.trust_score ?? 100;
            const newScore = Math.max(0, currentScore - penalty);
            await targetRef.update({
                trust_score: newScore,
                last_violation_at: admin.firestore.Timestamp.now()
            });
            await evaluateTrustScore(reportedId, newScore);
        }
    }

    await logSafetyEvent(reportedId, 'user_reported', 'high', { reporter_id: auth.uid, reason: reason, penalty: penalty });
    return { success: true };
});

module.exports = {
    blockUser,
    unblockUser,
    reportUser
};
