/**
 * Pool Dating Cloud Functions
 * Top-level entry point that aggregates all sub-modules.
 * Version: 2026-01-14-v14 (Cleanup)
 */

// Initialize App
require('./src/init');

// 1. Admin & System
Object.assign(exports, require('./src/admin'));

// 2. Pool Management
const {
    createPoolIfNotExists,
    joinPool,
    refundCreditsForCancelledPool,
    updatePoolStatus,
    addDummyUser
} = require('./src/pool');

exports.createPoolIfNotExists = createPoolIfNotExists;
exports.joinPool = joinPool;
exports.refundCreditsForCancelledPool = refundCreditsForCancelledPool;
exports.updatePoolStatus = updatePoolStatus;
exports.addDummyUser = addDummyUser;

// 3. Chat & Messaging
const {
    sendMessage,
    requestImageUploadUrl,
    requestChatContinuation,
    expireChats,
    markChatRead
} = require('./src/chat');

exports.sendMessage = sendMessage;
exports.requestImageUploadUrl = requestImageUploadUrl;
exports.requestChatContinuation = requestChatContinuation;
exports.expireChats = expireChats;
exports.markChatRead = markChatRead;

// 4. Matchmaking
const { runMatchmaking } = require('./src/matching');
exports.runMatchmaking = runMatchmaking;

// 5. Credits & Billing
const {
    getUserCreditBalance,
    mockPurchaseCredits
} = require('./src/credits');

exports.getUserCreditBalance = getUserCreditBalance;
exports.mockPurchaseCredits = mockPurchaseCredits;

// 6. User Safety & Reporting
const {
    blockUser,
    unblockUser,
    reportUser
} = require('./src/user');

exports.blockUser = blockUser;
exports.unblockUser = unblockUser;
exports.reportUser = reportUser;

// 7. Device Notifications
const {
    registerDeviceToken,
    unregisterDeviceToken
} = require('./src/notifications');

exports.registerDeviceToken = registerDeviceToken;
exports.unregisterDeviceToken = unregisterDeviceToken;

// 8. Scheduled Tasks
const {
    sendChatExpiryWarnings,
    liftExpiredRestrictions
} = require('./src/scheduled');

exports.sendChatExpiryWarnings = sendChatExpiryWarnings;
exports.liftExpiredRestrictions = liftExpiredRestrictions;
