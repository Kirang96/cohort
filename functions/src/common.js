const { admin, db, HttpsError, logger } = require('./init');

// ============================================
// CONSTANTS
// ============================================

const RESTRICTION_LEVELS = {
    none: 0,
    limited: 1, // Cannot join pools
    blocked: 2  // Cannot join pools OR send messages
};

const PENALTIES = {
    spam: 5,
    fake_profile: 10,
    harassment: 15,
    abuse: 20,
    other: 5
};

const MAX_POOL_TOTAL = 50;
const MAX_BUFFER_PER_GENDER = 25;

const SUPPORTED_CITIES = ["Kochi"];
const POOL_JOIN_DURATION_DAYS = 5;
const POOL_MATCH_DURATION_DAYS = 2;

// ============================================
// HELPERS
// ============================================

async function logSafetyEvent(userId, eventType, severity, context = {}) {
    try {
        await db.collection('safety_events').add({
            user_id: userId,
            event_type: eventType,
            severity: severity,
            context: context,
            created_at: admin.firestore.Timestamp.now()
        });

        logger.info(JSON.stringify({
            subsystem: "safety",
            action: "flag",
            user_id: userId,
            severity: severity,
            reason: eventType,
            result: "logged",
            timestamp: new Date().toISOString()
        }));
    } catch (e) {
        logger.error("Failed to log safety event", e);
    }
}

async function logSystemError({ subsystem, functionName, context = {}, error, severity = 'medium', blastRadius = 'single_user', recoverable = true }) {
    const errorMessage = error instanceof Error ? error.message : String(error);
    const errorStack = error instanceof Error ? error.stack : null;

    const logEntry = {
        subsystem,
        function_name: functionName,
        context,
        error_message: errorMessage,
        error_stack: errorStack,
        severity,
        blast_radius: blastRadius,
        recoverable,
        created_at: admin.firestore.Timestamp.now(),
        timestamp_iso: new Date().toISOString()
    };

    const logMethod = severity === 'critical' ? logger.error :
        severity === 'high' ? logger.warn : logger.info;

    logMethod(JSON.stringify({
        type: 'SYSTEM_ERROR',
        ...logEntry
    }));

    if (severity === 'critical' || severity === 'high') {
        try {
            await db.collection('system_errors').add(logEntry);
        } catch (e) {
            logger.error('Failed to persist system error to Firestore', e);
        }
    }
}

function requireAdmin(auth) {
    if (!auth) {
        throw new HttpsError('unauthenticated', 'User must be logged in');
    }
    // logger.info(`requireAdmin check: uid=${auth.uid}, token keys=${Object.keys(auth.token || {}).join(',')}, admin=${auth.token?.admin}`);
    if (!auth.token?.admin) {
        throw new HttpsError('permission-denied', `Admin access required (uid: ${auth.uid})`);
    }
}

async function checkRestriction(userId, blockedActionLevel = 'none') {
    const userDoc = await db.collection('users').doc(userId).get();
    if (!userDoc.exists) return;

    const userData = userDoc.data();
    const userLevel = userData.restriction_level || 'none';

    if (userData.restriction_expires_at) {
        const now = admin.firestore.Timestamp.now();
        if (userData.restriction_expires_at < now) {
            return;
        }
    }

    const userSeverity = RESTRICTION_LEVELS[userLevel] || 0;
    const actionSeverity = RESTRICTION_LEVELS[blockedActionLevel] || 0;

    if (userSeverity >= actionSeverity && actionSeverity > 0) {
        const reason = userData.restriction_reason || 'Community Guidelines violation';
        await logSafetyEvent(userId, 'restriction_enforced', 'low', {
            attempted_level: blockedActionLevel,
            user_level: userLevel
        });
        throw new HttpsError('permission-denied', `Restricted: ${reason}`, {
            restriction_level: userLevel,
            reason: reason
        });
    }
}

async function enforceRateLimit(userId, action, limit, windowSeconds, scopeId = null) {
    const now = admin.firestore.Timestamp.now();
    const windowStartMs = Math.floor(now.toMillis() / (windowSeconds * 1000)) * (windowSeconds * 1000);
    const windowStart = admin.firestore.Timestamp.fromMillis(windowStartMs);

    let docId = `${userId}_${action}_${windowStartMs}`;
    if (scopeId) {
        docId = `${userId}_${action}_${scopeId}_${windowStartMs}`;
    }

    const limitRef = db.collection('rate_limits').doc(docId);

    try {
        await db.runTransaction(async (t) => {
            const doc = await t.get(limitRef);
            let count = 0;
            if (doc.exists) {
                count = doc.data().count;
            }

            if (count >= limit) {
                throw new Error(`RateLimitExceeded`);
            }

            t.set(limitRef, {
                user_id: userId,
                action: action,
                scope_id: scopeId,
                window_start: windowStart,
                count: admin.firestore.FieldValue.increment(1),
                expires_at: admin.firestore.Timestamp.fromMillis(windowStartMs + (windowSeconds * 1000 * 2))
            }, { merge: true });
        });
    } catch (e) {
        if (e.message === 'RateLimitExceeded') {
            await logSafetyEvent(userId, 'rate_limit_exceeded', 'medium', {
                action: action,
                limit: limit,
                scope_id: scopeId
            });
            throw new HttpsError('resource-exhausted', `Rate limit exceeded for ${action}. Try again later.`);
        }
        throw e;
    }
}

async function checkBlockStatus(uid1, uid2) {
    const block1 = await db.collection('user_blocks')
        .where('blocker_id', '==', uid1)
        .where('blocked_id', '==', uid2)
        .limit(1).get();
    if (!block1.empty) return true;

    const block2 = await db.collection('user_blocks')
        .where('blocker_id', '==', uid2)
        .where('blocked_id', '==', uid1)
        .limit(1).get();
    return !block2.empty;
}

async function computeCreditBalance(userId, transaction = null) {
    const ledgerRef = db.collection('credit_ledger').where('user_id', '==', userId);
    const snapshot = await ledgerRef.get(); // Transaction support later if needed
    let balance = 0;
    snapshot.forEach(doc => {
        balance += doc.data().credits_delta || 0;
    });
    return balance;
}

async function sendNotification(userId, payload) {
    const startTime = Date.now();
    try {
        const devicesSnapshot = await db.collection('user_devices')
            .where('user_id', '==', userId)
            .get();

        if (devicesSnapshot.empty) {
            return { sent: 0, skipped: true, reason: "no_devices" };
        }

        const tokens = devicesSnapshot.docs.map(doc => doc.data().fcm_token);
        const staleTokenDocs = [];

        const message = {
            data: {
                type: payload.type,
                entity_id: payload.entity_id || "",
                timestamp: new Date().toISOString()
            },
            notification: {
                title: payload.title || "Pool Dating",
                body: payload.body || "You have a new notification"
            }
        };

        let successCount = 0;
        let failCount = 0;

        for (let i = 0; i < tokens.length; i++) {
            const token = tokens[i];
            const docRef = devicesSnapshot.docs[i].ref;
            try {
                await admin.messaging().send({ ...message, token: token });
                successCount++;
            } catch (error) {
                failCount++;
                if (error.code === 'messaging/invalid-registration-token' ||
                    error.code === 'messaging/registration-token-not-registered') {
                    staleTokenDocs.push(docRef);
                }
            }
        }

        for (const docRef of staleTokenDocs) {
            try { await docRef.delete(); } catch (e) { }
        }

        return { sent: successCount, failed: failCount, stale_cleaned: staleTokenDocs.length };
    } catch (error) {
        logger.error(`Notification Error for ${userId}: ${error.message}`);
        return { sent: 0, error: error.message };
    }
}

module.exports = {
    RESTRICTION_LEVELS,
    PENALTIES,
    MAX_POOL_TOTAL,
    MAX_BUFFER_PER_GENDER,
    SUPPORTED_CITIES,
    POOL_JOIN_DURATION_DAYS,
    POOL_MATCH_DURATION_DAYS,
    logSafetyEvent,
    logSystemError,
    requireAdmin,
    checkRestriction,
    enforceRateLimit,
    checkBlockStatus,
    computeCreditBalance,
    sendNotification
};
