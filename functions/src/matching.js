const { onCall, HttpsError, logger, db, admin } = require('./init');
const { sendNotification } = require('./common');

// Internal Logic Function
async function executeMatchmaking(poolId) {
    const result = await db.runTransaction(async (t) => {
        const poolRef = db.collection('pools').doc(poolId);
        const pDoc = await t.get(poolRef);
        if (!pDoc.exists) throw new Error('Pool not found');
        const poolData = pDoc.data();

        if (poolData.matching_executed_at) {
            throw new Error('Matchmaking already executed');
        }

        const membershipSnapshot = await t.get(db.collection('pool_memberships')
            .where('pool_id', '==', poolId)
            .where('status', '==', 'active'));

        const validMembers = membershipSnapshot.docs.map(d => d.data());
        const validMaleMembers = validMembers.filter(m => m.gender === 'Male');
        const validFemaleMembers = validMembers.filter(m => m.gender === 'Female');

        if (validMaleMembers.length === 0 || validFemaleMembers.length === 0) {
            return { success: false, message: "Insufficient users to match", matches_count: 0, pool_id: poolId };
        }

        const userIds = validMembers.map(m => m.user_id);
        const userDocs = await Promise.all(userIds.map(uid => t.get(db.collection('users').doc(uid))));
        const userMap = new Map();
        userDocs.forEach(d => {
            if (d.exists) userMap.set(d.id, { ...d.data(), id: d.id });
        });

        // 4. Algorithm: "The Draft"
        const MIN_MATCHES = 2;
        const SCORE_THRESHOLD = 10;
        const AGE_WEIGHT = 0.5;
        const INTEREST_WEIGHT = 10;

        const males = validMaleMembers.map(m => userMap.get(m.user_id)).filter(u => u);
        const females = validFemaleMembers.map(u => userMap.get(u.user_id)).filter(u => u);

        if (males.length === 0 || females.length === 0) {
            return { success: false, message: "No users to match", matches_count: 0, pool_id: poolId };
        }

        const TARGET_MATCHES = 5;
        const ratio_m = Math.ceil(females.length / Math.max(1, males.length));
        const ratio_f = Math.ceil(males.length / Math.max(1, females.length));

        const cap_m = Math.max(ratio_m, TARGET_MATCHES);
        const cap_f = Math.max(ratio_f, TARGET_MATCHES);
        const maxPossibleRounds = Math.max(cap_m, cap_f);

        logger.info(`Draft Config: Males=${males.length}, Females=${females.length}, RatioM=${ratio_m}, RatioF=${ratio_f}, CapM=${cap_m}, CapF=${cap_f}`);

        const matches = new Map();
        userIds.forEach(uid => matches.set(uid, new Set()));

        const calculateScore = (userA, userB) => {
            const getAge = (user) => {
                if (user.age) return user.age;
                if (!user.dob) return 25;
                const dob = new Date(user.dob);
                const year = isNaN(dob.getFullYear()) ? 2000 : dob.getFullYear();
                return new Date().getFullYear() - year;
            }
            const ageA = getAge(userA);
            const ageB = getAge(userB);
            const ageDelta = Math.abs(ageA - ageB);
            const ageScore = Math.max(0, (10 - ageDelta) * AGE_WEIGHT);

            const interestsA = userA.interests ? (Array.isArray(userA.interests) ? userA.interests : userA.interests.split(',').map(i => i.trim())) : [];
            const interestsB = userB.interests ? (Array.isArray(userB.interests) ? userB.interests : userB.interests.split(',').map(i => i.trim())) : [];
            const setB = new Set(interestsB.map(i => i.toLowerCase()));
            const overlap = interestsA.filter(i => setB.has(i.toLowerCase())).length;
            const interestScore = overlap * INTEREST_WEIGHT;

            return ageScore + interestScore;
        };

        const malePreferences = new Map();
        males.forEach(male => {
            const myCandidates = females.map(female => {
                return { female, score: calculateScore(male, female) };
            });
            myCandidates.sort((a, b) => b.score - a.score);
            malePreferences.set(male.id, myCandidates);
        });

        let currentRound = 1;
        const shuffleArray = (array) => {
            for (let i = array.length - 1; i > 0; i--) {
                const j = Math.floor(Math.random() * (i + 1));
                [array[i], array[j]] = [array[j], array[i]];
            }
        };

        const finalPairs = [];

        while (currentRound <= maxPossibleRounds) {
            shuffleArray(males);
            for (const male of males) {
                const maleId = male.id;
                if (matches.get(maleId).size >= cap_m) continue;

                const candidates = malePreferences.get(maleId);
                for (let i = 0; i < candidates.length; i++) {
                    const candidate = candidates[i];
                    const female = candidate.female;
                    const femaleId = female.user_id;

                    if (matches.get(femaleId).size >= cap_f) continue;
                    if (matches.get(maleId).has(femaleId)) continue;

                    const isQuality = candidate.score >= SCORE_THRESHOLD;
                    const maleCount = matches.get(maleId).size;
                    const femaleCount = matches.get(femaleId).size;

                    if (!isQuality && (maleCount >= MIN_MATCHES && femaleCount >= MIN_MATCHES)) {
                        continue;
                    }

                    matches.get(maleId).add(femaleId);
                    matches.get(femaleId).add(maleId);
                    finalPairs.push({
                        user_a: maleId,
                        user_b: femaleId,
                        score: candidate.score,
                        user_a_name: male.name,
                        user_b_name: female.name
                    });
                    break;
                }
            }
            currentRound++;
        }

        finalPairs.forEach(m => {
            const matchRef = db.collection('matches').doc();
            const nameA = userMap.get(m.user_a)?.name || "Unknown";
            const nameB = userMap.get(m.user_b)?.name || "Unknown";

            t.set(matchRef, {
                pool_id: poolId,
                user_a: m.user_a,
                user_b: m.user_b,
                user_a_name: nameA,
                user_b_name: nameB,
                compatibility_score: m.score,
                created_at: admin.firestore.Timestamp.now()
            });

            const chatRef = db.collection('chats').doc(matchRef.id);
            const expiryTime = admin.firestore.Timestamp.fromMillis(Date.now() + 24 * 60 * 60 * 1000);

            t.set(chatRef, {
                match_id: matchRef.id,
                pool_id: poolId,
                user_a: m.user_a,
                user_b: m.user_b,
                user_a_name: nameA,
                user_b_name: nameB,
                status: 'active',
                expires_at: expiryTime,
                continued_by: [],
                created_at: admin.firestore.Timestamp.now()
            });
        });

        t.update(poolRef, {
            status: 'completed',
            matching_executed_at: admin.firestore.Timestamp.now()
        });

        return { success: true, matches_count: finalPairs.length, pool_id: poolId };
    });

    try {
        const membershipsToUpdate = await db.collection('pool_memberships')
            .where('pool_id', '==', poolId)
            .where('status', 'in', ['active', 'buffer'])
            .get();

        const batch = db.batch();
        membershipsToUpdate.docs.forEach(doc => {
            batch.update(doc.ref, { status: 'completed' });
        });
        await batch.commit();

        const notifiedUsers = new Set();
        membershipsToUpdate.docs.forEach(doc => {
            const userId = doc.data().user_id;
            if (userId) notifiedUsers.add(userId);
        });

        for (const userId of notifiedUsers) {
            try {
                await sendNotification(userId, {
                    type: "MATCH_READY",
                    entity_id: poolId,
                    title: "Matches Ready! 🎉",
                    body: "Your matches are ready. Open the app to see who you've been matched with!"
                });
            } catch (e) { }
        }
    } catch (e) {
        logger.error(`Failed to update memberships/notify: ${e.message}`);
    }

    return result;
}

const runMatchmaking = onCall(async (request) => {
    const auth = request.auth;
    const data = request.data;
    if (!auth) throw new HttpsError('unauthenticated', 'User must be logged in');

    const poolId = data.poolId;
    if (!poolId) throw new HttpsError('invalid-argument', 'Pool ID required');

    try {
        return await executeMatchmaking(poolId);
    } catch (e) {
        logger.error(`Matchmaking Callable Error: ${e.message}`);
        // Consider mapping Error to HttpsError
        throw new HttpsError('internal', e.message);
    }
});

module.exports = {
    runMatchmaking,
    executeMatchmaking
};


