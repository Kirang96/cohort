/**
 * Circle Management - Wrapper module with new terminology
 * Internally uses pool.js functions, exposes Circle-named exports
 * Firestore collection remains 'pools' for backward compatibility
 */

const {
    createPoolIfNotExists,
    joinPool,
    refundCreditsForCancelledPool,
    updatePoolStatus,
    addDummyUser,
    ensureNextPoolExists,
    checkAndCloseExpiredPools,
    checkAndRunMatchmaking
} = require('./pool');

// Re-export with Circle naming
const createCircleIfNotExists = createPoolIfNotExists;
const joinCircle = joinPool;
const refundCreditsForCancelledCircle = refundCreditsForCancelledPool;
const updateCircleStatus = updatePoolStatus;
const ensureNextCircleExists = ensureNextPoolExists;
const checkAndCloseExpiredCircles = checkAndCloseExpiredPools;

module.exports = {
    // New Circle-named exports (use these going forward)
    createCircleIfNotExists,
    joinCircle,
    refundCreditsForCancelledCircle,
    updateCircleStatus,
    addDummyUser,
    ensureNextCircleExists,
    checkAndCloseExpiredCircles,
    checkAndRunMatchmaking,

    // Backward compatibility - old Pool names (deprecated)
    createPoolIfNotExists,
    joinPool,
    refundCreditsForCancelledPool,
    updatePoolStatus,
    ensureNextPoolExists,
    checkAndCloseExpiredPools
};
