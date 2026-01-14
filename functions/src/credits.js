const { onCall, HttpsError, logger, db, admin } = require('./init');
const { computeCreditBalance } = require('./common');

const getUserCreditBalance = onCall(async (request) => {
    const auth = request.auth;
    if (!auth) throw new HttpsError('unauthenticated', 'User must be logged in');

    try {
        const balance = await computeCreditBalance(auth.uid);
        logger.info(JSON.stringify({
            subsystem: "credits",
            action: "balance_check",
            user_id: auth.uid,
            balance: balance,
            timestamp: new Date().toISOString()
        }));
        return { balance: balance };
    } catch (e) {
        logger.error("Error computing credit balance", e);
        throw new HttpsError('internal', 'Failed to compute credit balance');
    }
});

const mockPurchaseCredits = onCall(async (request) => {
    const auth = request.auth;
    const data = request.data;
    if (!auth) throw new HttpsError('unauthenticated', 'User must be logged in');

    const amount = data.amount || 5;
    if (amount <= 0 || amount > 100) throw new HttpsError('invalid-argument', 'Amount must be between 1 and 100');

    const referenceId = `mock_txn_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    const balanceBefore = await computeCreditBalance(auth.uid);

    await db.collection('credit_ledger').add({
        user_id: auth.uid,
        action: 'purchase',
        credits_delta: amount,
        reference_id: referenceId,
        reason: 'Mock purchase for testing',
        created_at: admin.firestore.Timestamp.now()
    });

    const balanceAfter = balanceBefore + amount;

    logger.info(JSON.stringify({
        subsystem: "credits",
        action: "purchase",
        user_id: auth.uid,
        credits_delta: amount,
        balance_before: balanceBefore,
        balance_after: balanceAfter,
        reference_id: referenceId,
        timestamp: new Date().toISOString()
    }));

    return { success: true, credits_added: amount, new_balance: balanceAfter, reference_id: referenceId };
});

module.exports = {
    getUserCreditBalance,
    mockPurchaseCredits
};
