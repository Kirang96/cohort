const admin = require('firebase-admin');
const functions = require("firebase-functions");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { logger } = functions;

if (admin.apps.length === 0) {
    admin.initializeApp({
        storageBucket: 'pooldating.firebasestorage.app'
    });
}

const db = admin.firestore();

module.exports = {
    admin,
    db,
    functions,
    onCall,
    HttpsError,
    onSchedule,
    logger
};
