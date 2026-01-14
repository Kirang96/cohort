const { onCall, HttpsError, logger, db, admin } = require('./init');
const {
    MAX_POOL_TOTAL,
    MAX_BUFFER_PER_GENDER,
    SUPPORTED_CITIES,
    POOL_JOIN_DURATION_DAYS,
    POOL_MATCH_DURATION_DAYS,
    checkRestriction,
    enforceRateLimit,
    computeCreditBalance,
    sendNotification
} = require('./common');

// ============================================
// HELPERS
// ============================================

async function createNewPool(city, previousPoolId, reason) {
    const now = admin.firestore.Timestamp.now();
    const joinDeadline = admin.firestore.Timestamp.fromMillis(
        now.toMillis() + (POOL_JOIN_DURATION_DAYS * 24 * 60 * 60 * 1000)
    );
    const matchDeadline = admin.firestore.Timestamp.fromMillis(
        joinDeadline.toMillis() + (POOL_MATCH_DURATION_DAYS * 24 * 60 * 60 * 1000)
    );

    const newPool = {
        city: city,
        status: 'joining',
        male_count: 0,
        female_count: 0,
        buffer_m_count: 0,
        buffer_f_count: 0,
        created_at: now,
        join_deadline: joinDeadline,
        match_deadline: matchDeadline
    };

    const docRef = await db.collection('pools').add(newPool);

    logger.info(JSON.stringify({
        subsystem: "pool_lifecycle",
        action: "sequential_pool_created",
        city: city,
        previous_pool_id: previousPoolId,
        new_pool_id: docRef.id,
        reason: reason,
        timestamp: now.toDate().toISOString()
    }));

    return docRef.id;
}

async function ensureNextPoolExists(city, reason = 'routine_check') {
    try {
        if (!SUPPORTED_CITIES.includes(city)) {
            logger.warn(`ensureNextPoolExists: City ${city} not supported`);
            return null;
        }

        const joiningPools = await db.collection('pools')
            .where('city', '==', city)
            .where('status', '==', 'joining')
            .limit(1)
            .get();

        if (joiningPools.empty) {
            const newPoolId = await createNewPool(city, null, 'no_joinable_pool');
            return newPoolId;
        }

        const currentPool = joiningPools.docs[0];
        const currentPoolData = currentPool.data();
        const currentPoolId = currentPool.id;

        const poolM = currentPoolData.male_count || 0;
        const poolF = currentPoolData.female_count || 0;
        const bufferM = currentPoolData.buffer_m_count || 0;
        const bufferF = currentPoolData.buffer_f_count || 0;

        const totalM = poolM + bufferM;
        const totalF = poolF + bufferF;

        const maleAtCapacity = totalM >= MAX_BUFFER_PER_GENDER;
        const femaleAtCapacity = totalF >= MAX_BUFFER_PER_GENDER;

        const now = admin.firestore.Timestamp.now();
        const joinDeadline = currentPoolData.join_deadline;
        const oneHourFromNow = admin.firestore.Timestamp.fromMillis(now.toMillis() + (60 * 60 * 1000));
        const closingSoon = joinDeadline && joinDeadline < oneHourFromNow;

        if (maleAtCapacity || femaleAtCapacity || closingSoon) {
            const nextPools = await db.collection('pools')
                .where('city', '==', city)
                .where('status', '==', 'joining')
                .where('created_at', '>', currentPoolData.created_at)
                .limit(1)
                .get();

            if (nextPools.empty) {
                const triggerReason = maleAtCapacity ? 'male_capacity_reached' :
                    femaleAtCapacity ? 'female_capacity_reached' :
                        'joining_closing_soon';
                const newPoolId = await createNewPool(city, currentPoolId, triggerReason);
                return newPoolId;
            }
        }
        return null;
    } catch (e) {
        logger.error(`ensureNextPoolExists failed: ${e.message}`);
        return null;
    }
}

async function processBufferToPool(poolId) {
    const poolRef = db.collection('pools').doc(poolId);

    const poolDoc = await poolRef.get();
    if (!poolDoc.exists) return { moved: 0 };

    const pool = poolDoc.data();
    let poolM = pool.male_count || 0;
    let poolF = pool.female_count || 0;
    let bufferM = pool.buffer_m_count || 0;
    let bufferF = pool.buffer_f_count || 0;

    let movedMales = 0;
    let movedFemales = 0;

    while (true) {
        if (poolM + poolF >= MAX_POOL_TOTAL) break;

        let movedSomeone = false;

        if (poolM <= poolF && bufferM > 0) {
            poolM++; bufferM--; movedMales++; movedSomeone = true;
        } else if (poolF <= poolM && bufferF > 0) {
            poolF++; bufferF--; movedFemales++; movedSomeone = true;
        } else if (bufferM > 0 && poolM <= poolF) {
            poolM++; bufferM--; movedMales++; movedSomeone = true;
        } else if (bufferF > 0 && poolF <= poolM) {
            poolF++; bufferF--; movedFemales++; movedSomeone = true;
        }

        if (!movedSomeone) break;
    }

    const movedCount = movedMales + movedFemales;
    if (movedCount === 0) return { moved: 0, movedMales: 0, movedFemales: 0 };

    const allBufferMemberships = await db.collection('pool_memberships')
        .where('pool_id', '==', poolId)
        .where('status', '==', 'buffer')
        .orderBy('joined_at', 'asc')
        .get();

    const maleDocs = allBufferMemberships.docs.filter(doc => doc.data().gender === 'Male');
    const femaleDocs = allBufferMemberships.docs.filter(doc => doc.data().gender === 'Female');

    const maleMemberships = { docs: maleDocs.slice(0, movedMales) };
    const femaleMemberships = { docs: femaleDocs.slice(0, movedFemales) };

    return db.runTransaction(async (transaction) => {
        const freshPoolDoc = await transaction.get(poolRef);
        if (!freshPoolDoc.exists) return { moved: 0 };

        transaction.update(poolRef, {
            male_count: poolM,
            female_count: poolF,
            buffer_m_count: bufferM,
            buffer_f_count: bufferF
        });

        for (const doc of maleMemberships.docs) {
            transaction.update(doc.ref, { status: 'active', entered_pool_at: admin.firestore.Timestamp.now() });
        }
        for (const doc of femaleMemberships.docs) {
            transaction.update(doc.ref, { status: 'active', entered_pool_at: admin.firestore.Timestamp.now() });
        }

        logger.info(JSON.stringify({
            subsystem: "pool_zipper",
            action: "process_buffer",
            pool_id: poolId,
            moved_total: movedCount,
            pool_state: `${poolM}M/${poolF}F`,
            timestamp: new Date().toISOString()
        }));

        return { moved: movedCount, movedMales, movedFemales };
    });
}

// ============================================
// EXPORTS
// ============================================

const createPoolIfNotExists = onCall(async (request) => {
    const data = request.data;
    const city = data.city;
    if (!city) throw new HttpsError('invalid-argument', 'City is required');

    const poolsRef = db.collection('pools');
    const activeSnapshot = await poolsRef
        .where('city', '==', city)
        .where('status', 'in', ['joining', 'validating'])
        .limit(1).get();

    if (!activeSnapshot.empty) {
        return { message: "Active pool exists", poolId: activeSnapshot.docs[0].id };
    }

    const newPoolId = await createNewPool(city, null, 'manual_create');
    return { message: "Pool created", poolId: newPoolId };
});

const joinPool = onCall(async (request) => {
    const data = request.data;
    const auth = request.auth;

    if (!auth) throw new HttpsError('unauthenticated', 'User must be logged in');

    await checkRestriction(auth.uid, 'limited');
    await enforceRateLimit(auth.uid, 'joinPool', 10, 86400);

    const userId = auth.uid;
    const poolId = data.poolId;
    if (!poolId) throw new HttpsError('invalid-argument', 'Pool ID required');

    const poolDoc = await db.collection('pools').doc(poolId).get();
    if (!poolDoc.exists) throw new HttpsError('not-found', 'Pool not found');
    const poolCity = poolDoc.data().city;

    if (!SUPPORTED_CITIES.includes(poolCity)) {
        throw new HttpsError('failed-precondition', `City ${poolCity} is not currently supported`);
    }

    await ensureNextPoolExists(poolCity, 'pre_join_check');

    const creditBalance = await computeCreditBalance(userId);

    const existingMembershipSnapshot = await db.collection('pool_memberships')
        .where('user_id', '==', userId)
        .where('pool_id', '==', poolId)
        .limit(1).get();

    if (!existingMembershipSnapshot.empty) {
        throw new HttpsError('already-exists', 'You are already in this pool');
    }

    const poolRef = db.collection('pools').doc(poolId);
    const userRef = db.collection('users').doc(userId);

    const result = await db.runTransaction(async (transaction) => {
        const poolDoc = await transaction.get(poolRef);
        const userDoc = await transaction.get(userRef);

        if (!poolDoc.exists) throw new HttpsError('not-found', 'Pool not found');
        if (!userDoc.exists) throw new HttpsError('not-found', 'User profile not found');

        const pool = poolDoc.data();
        const user = userDoc.data();

        if (pool.status !== 'joining') throw new HttpsError('failed-precondition', 'Pool is not accepting members');

        const gender = user.gender;
        if (!gender) throw new HttpsError('failed-precondition', 'User gender not set');

        const CREDIT_COST_MALE = 1;
        if (gender === 'Male' && creditBalance < CREDIT_COST_MALE) {
            throw new HttpsError('failed-precondition', 'Insufficient credits');
        }

        const poolM = pool.male_count || 0;
        const poolF = pool.female_count || 0;
        const bufferM = pool.buffer_m_count || 0;
        const bufferF = pool.buffer_f_count || 0;
        const totalM = poolM + bufferM;
        const totalF = poolF + bufferF;

        if (gender === 'Male' && totalM >= MAX_BUFFER_PER_GENDER) throw new HttpsError('failed-precondition', 'Male slots are full');
        if (gender === 'Female' && totalF >= MAX_BUFFER_PER_GENDER) throw new HttpsError('failed-precondition', 'Female slots are full');

        const membershipRef = db.collection('pool_memberships').doc();
        transaction.set(membershipRef, {
            pool_id: poolId,
            user_id: userId,
            gender: gender,
            joined_at: admin.firestore.Timestamp.now(),
            status: 'buffer',
            entered_pool_at: null
        });

        transaction.update(poolRef, {
            buffer_m_count: gender === 'Male' ? bufferM + 1 : bufferM,
            buffer_f_count: gender === 'Female' ? bufferF + 1 : bufferF
        });

        let creditsDebited = 0;
        if (gender === 'Male') {
            const ledgerRef = db.collection('credit_ledger').doc();
            transaction.set(ledgerRef, {
                user_id: userId,
                action: 'spend',
                credits_delta: -CREDIT_COST_MALE,
                reference_id: poolId,
                reason: 'Pool join fee',
                created_at: admin.firestore.Timestamp.now()
            });
            creditsDebited = CREDIT_COST_MALE;
        }

        logger.info(JSON.stringify({
            subsystem: "pool_join",
            action: "added_to_buffer",
            user_id: userId,
            pool_id: poolId,
            gender: gender,
            timestamp: new Date().toISOString()
        }));

        return { success: true, status: 'buffer', credits_debited: creditsDebited, new_balance: creditBalance - creditsDebited };
    });

    try {
        await processBufferToPool(poolId);
    } catch (zipperError) {
        logger.error(`processBufferToPool failed: ${zipperError.message}`);
    }

    try {
        await sendNotification(userId, {
            type: "POOL_JOINED",
            entity_id: poolId,
            title: "Pool Joined!",
            body: "You've joined the pool. We'll notify you when matches are ready."
        });
    } catch (e) { }

    try {
        await ensureNextPoolExists(poolCity, 'post_join_check');
    } catch (e) { }

    return result;
});

const refundCreditsForCancelledPool = onCall(async (request) => {
    const auth = request.auth;
    const data = request.data;
    if (!auth) throw new HttpsError('unauthenticated', 'User must be logged in');

    const poolId = data.poolId;
    if (!poolId) throw new HttpsError('invalid-argument', 'Pool ID required');

    const poolDoc = await db.collection('pools').doc(poolId).get();
    if (!poolDoc.exists || poolDoc.data().status !== 'cancelled') {
        throw new HttpsError('failed-precondition', 'Pool not found or not cancelled');
    }

    const membershipsSnapshot = await db.collection('pool_memberships')
        .where('pool_id', '==', poolId)
        .where('gender', '==', 'Male')
        .where('status', '==', 'active')
        .get();

    let refundsIssued = 0;
    const batch = db.batch();

    for (const membershipDoc of membershipsSnapshot.docs) {
        const memberId = membershipDoc.data().user_id;
        const existingRefund = await db.collection('credit_ledger')
            .where('user_id', '==', memberId)
            .where('reference_id', '==', `refund_${poolId}`)
            .limit(1).get();

        if (existingRefund.empty) {
            const ledgerRef = db.collection('credit_ledger').doc();
            batch.set(ledgerRef, {
                user_id: memberId,
                action: 'refund',
                credits_delta: 1,
                reference_id: `refund_${poolId}`,
                reason: 'Pool cancelled refund',
                created_at: admin.firestore.Timestamp.now()
            });
            refundsIssued++;
        }
    }

    if (refundsIssued > 0) await batch.commit();
    return { success: true, refunds_issued: refundsIssued };
});

const updatePoolStatus = onCall(async (request) => {
    const { poolId, status } = request.data;
    if (!poolId || !status) throw new HttpsError('invalid-argument', 'Pool ID and Status required');

    await db.collection('pools').doc(poolId).update({
        status: status,
        updated_at: admin.firestore.Timestamp.now()
    });
    return { success: true, poolId, status };
});

const addDummyUser = onCall(async (request) => {
    const data = request.data;
    const city = data.city || "New York";
    // Normalize gender to capitalized format (Male/Female)
    const rawGender = data.gender || "Male";
    const gender = rawGender.charAt(0).toUpperCase() + rawGender.slice(1).toLowerCase();
    const poolId = data.poolId;

    const randomId = Math.random().toString(36).substring(2, 10);
    const namesM = ["James", "John", "Robert", "Michael", "William", "David", "Richard", "Joseph", "Thomas", "Charles"];
    const namesF = ["Mary", "Patricia", "Jennifer", "Linda", "Elizabeth", "Barbara", "Susan", "Jessica", "Sarah", "Karen"];
    const lastNames = ["Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller", "Wilson", "Moore", "Taylor"];

    const nameList = gender === 'Male' ? namesM : namesF;
    const firstName = nameList[Math.floor(Math.random() * nameList.length)];
    const lastName = lastNames[Math.floor(Math.random() * lastNames.length)];
    const fullName = `${firstName} ${lastName} (Bot)`;

    const age = Math.floor(Math.random() * (40 - 20 + 1)) + 20;
    const birthYear = new Date().getFullYear() - age;
    const dob = `${birthYear}-01-01`;

    const interestsList = ["hiking", "movies", "reading", "travel", "cooking", "music", "gym", "tech", "art", "gaming"];
    const interestCount = Math.floor(Math.random() * 3) + 1;
    const pickedInterests = [];
    while (pickedInterests.length < interestCount) {
        const item = interestsList[Math.floor(Math.random() * interestsList.length)];
        if (!pickedInterests.includes(item)) pickedInterests.push(item);
    }

    const dummyUserId = `dummy_${gender.toLowerCase()}_${randomId}`;

    const userRef = db.collection('users').doc(dummyUserId);
    await userRef.set({
        user_id: dummyUserId,
        name: fullName,
        age: age,
        dob: dob,
        gender: gender,
        bio: "Auto-generated dummy user for testing.",
        interests: pickedInterests.join(", "),
        city: city,
        status: 'active',
        is_dummy: true,
        created_at: admin.firestore.Timestamp.now()
    });

    let targetPoolId = poolId;
    let poolRef;

    if (!targetPoolId) {
        const poolsSnapshot = await db.collection('pools')
            .where('city', '==', city)
            .where('status', 'in', ['joining', 'validating'])
            .limit(1)
            .get();

        if (poolsSnapshot.empty) {
            const now = admin.firestore.Timestamp.now();
            const newPool = {
                city: city,
                status: 'joining',
                male_count: 0,
                female_count: 0,
                buffer_m_count: 0,
                buffer_f_count: 0,
                created_at: now,
                join_deadline: admin.firestore.Timestamp.fromMillis(now.toMillis() + (5 * 86400000)),
                match_deadline: admin.firestore.Timestamp.fromMillis(now.toMillis() + (7 * 86400000))
            };
            const newPoolRef = await db.collection('pools').add(newPool);
            targetPoolId = newPoolRef.id;
            poolRef = newPoolRef;
        } else {
            targetPoolId = poolsSnapshot.docs[0].id;
            poolRef = poolsSnapshot.docs[0].ref;
        }
    } else {
        poolRef = db.collection('pools').doc(targetPoolId);
    }

    await db.runTransaction(async (t) => {
        const pDoc = await t.get(poolRef);
        if (!pDoc.exists) throw new HttpsError('not-found', "Pool not found");

        const poolData = pDoc.data();
        const poolM = poolData.male_count || 0;
        const poolF = poolData.female_count || 0;
        const bufferM = poolData.buffer_m_count || 0;
        const bufferF = poolData.buffer_f_count || 0;

        const totalM = poolM + bufferM;
        const totalF = poolF + bufferF;

        if (gender === 'Male' && totalM >= MAX_BUFFER_PER_GENDER) {
            throw new HttpsError('failed-precondition', 'Male slots are full (25/25)');
        }
        if (gender === 'Female' && totalF >= MAX_BUFFER_PER_GENDER) {
            throw new HttpsError('failed-precondition', 'Female slots are full (25/25)');
        }

        const membershipRef = db.collection('pool_memberships').doc();
        t.set(membershipRef, {
            pool_id: targetPoolId,
            user_id: dummyUserId,
            gender: gender,
            joined_at: admin.firestore.Timestamp.now(),
            status: 'buffer',
            entered_pool_at: null,
            is_dummy: true
        });

        t.update(poolRef, {
            buffer_m_count: gender === 'Male' ? bufferM + 1 : bufferM,
            buffer_f_count: gender === 'Female' ? bufferF + 1 : bufferF
        });
    });

    await processBufferToPool(targetPoolId);

    return {
        success: true,
        user_id: dummyUserId,
        pool_id: targetPoolId,
        name: fullName,
        age: age,
        interests: pickedInterests
    };
});

// Exports are at the end of file after all functions are defined

// ============================================
// LIFECYCLE LOGIC
// ============================================
// Lifecycle Logic
// ============================================

async function checkAndCloseExpiredPools() {
    const now = admin.firestore.Timestamp.now();
    const snapshot = await db.collection('pools')
        .where('status', '==', 'joining')
        .where('join_deadline', '<=', now)
        .get();

    if (snapshot.empty) return 0;

    let closedCount = 0;
    for (const doc of snapshot.docs) {
        const pool = doc.data();
        await db.collection('pools').doc(doc.id).update({
            status: 'validating',
            updated_at: now
        });
        closedCount++;
        logger.info(`Closed pool ${doc.id} (Deadline passed). Status: validating`);

        // Ensure next pool is ready
        await ensureNextPoolExists(pool.city, 'prev_pool_closed');
    }
    return closedCount;
}

async function checkAndRunMatchmaking() {
    const { executeMatchmaking } = require('./matching');
    const now = admin.firestore.Timestamp.now();
    // Logic: If status is 'validating' AND join_deadline was > 1 minute ago?
    // Or just run it if it is 'validating'.
    // We assume if it's validating, it's ready for matching.
    const snapshot = await db.collection('pools')
        .where('status', '==', 'validating')
        .get();

    if (snapshot.empty) return 0;

    let matchedCount = 0;
    for (const doc of snapshot.docs) {
        try {
            logger.info(`Running auto-matchmaking for ${doc.id}`);
            await executeMatchmaking(doc.id);
            matchedCount++;
        } catch (e) {
            logger.error(`Auto-match failed for ${doc.id}: ${e.message}`);
        }
    }
    return matchedCount;
}

// ============================================
// EXPORTS (must be after function definitions)
// ============================================
module.exports = {
    createPoolIfNotExists,
    joinPool,
    refundCreditsForCancelledPool,
    updatePoolStatus,
    addDummyUser,
    ensureNextPoolExists,
    checkAndCloseExpiredPools,
    checkAndRunMatchmaking
};
