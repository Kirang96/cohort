# Pool Dating Debug Playbook

> For solo-founder emergency response. Read this when something breaks at 3 AM.

---

## 🚨 TRIAGE CHECKLIST

1. **Is it affecting all users?** → Check `severity: critical` in logs
2. **Is it affecting one city?** → Check `blast_radius: city` 
3. **Is it one angry user?** → Check `blast_radius: single_user`
4. **Is money involved?** → Credit ledger issue = PRIORITY 1

---

## 🔍 ADMIN INSPECTION COMMANDS

Call these Cloud Functions with admin token:

```javascript
// Inspect any user
firebase functions:shell
> inspectUser({userId: "USER_UID"})

// Inspect any pool
> inspectPool({poolId: "POOL_ID"})

// Inspect any chat
> inspectChat({matchId: "MATCH_ID"})

// Inspect credits
> inspectCredits({userId: "USER_UID"})
```

---

## 📋 COMMON FAILURES & FIXES

### 1. User Can't Join Pool
**Symptoms**: "Rate limit exceeded" or "Restricted" error

**Check**:
```
inspectUser({userId: "..."})
→ Look at trust_state.restriction_level
→ Look at active_pool (should be null)
```

**Fix Options**:
- If restricted: `restoreUserAccess({userId: "...", reason: "manual review"})`
- If in another pool: Wait for pool to complete

---

### 2. Chat Stuck Active After Expiry
**Symptoms**: Chat shows active but expired timer

**Check**:
```
inspectChat({matchId: "..."})
→ Compare expires_at vs current time
```

**Fix**:
```
forceExpireChat({matchId: "...", reason: "stuck after expiry"})
```

---

### 3. Pool Stuck After Match Time
**Symptoms**: Pool never matched, past join_deadline

**Check**:
```
inspectPool({poolId: "..."})
→ Look at status, join_deadline, count.active
```

**Fix**:
```
forceCompletePool({poolId: "...", reason: "stuck pool"})
```

---

### 4. Credit Balance Wrong
**Symptoms**: User claims wrong balance

**Check**:
```
inspectCredits({userId: "..."})
→ Compare stored_balance vs computed_balance
```

**Fix** (if mismatch):
```
recomputeCreditBalance({userId: "...", dryRun: false})
```

---

### 5. User Wrongly Banned
**Symptoms**: User can't access features, claims they did nothing

**Check**:
```
inspectUser({userId: "..."})
→ Look at trust_state, recent_safety_events
```

**Fix**:
```
restoreUserAccess({userId: "...", reason: "false positive"})
```

---

## 📊 LOG QUERIES

### Firebase Console > Functions > Logs

**Find critical errors**:
```
severity="error" AND jsonPayload.type="SYSTEM_ERROR"
```

**Find specific function failures**:
```
jsonPayload.function_name="matchmaking"
```

**Find admin overrides**:
```
jsonPayload.event_type=~"admin_override"
```

---

## ✅ SYSTEM HEALTH CHECK

Run daily or when suspicious:
```
verifySystemInvariants({fix: false})
```

**What it checks**:
- No user in multiple active pools
- No expired chats marked active
- Banned users aren't active

---

## 🛑 WHEN TO PANIC

| Signal | Action |
|--------|--------|
| `severity: critical` in logs | WAKE UP |
| Credit ledger mismatch | STOP PURCHASES |
| Matchmaking failure > 3/hr | Check pool logic |
| Pool stuck > deadline+1h | Force complete |

---

## 🛠️ RECOVERY PROCEDURES

### Database Restore
1. Go to Firebase Console > Firestore > Import/Export
2. Restore from last backup
3. Run `verifySystemInvariants()` after

### Function Rollback
```bash
firebase functions:log --only bad_function
firebase deploy --only functions:bad_function
```

### Clear Rate Limits (if blocking legit users)
1. Go to Firestore > `rate_limits` collection
2. Delete documents for affected user
3. OR set TTL to trigger cleanup

---

## 📞 ESCALATION

**If you can't fix it**:
1. Document the symptoms
2. Check this playbook again
3. Check Firebase Status page
4. Sleep on it (seriously)

---

*Last updated: 2026-01-12*
