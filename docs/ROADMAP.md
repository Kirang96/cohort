# Project Roadmap & Status

This document tracks the feature implementation and task history.

## Current Status: Stabilization & Cleanup (Checklist from Task.md)

### Iteration 8: Sequential Pools, Images, Kochi
- [x] **Sequential Pool Logic**: `ensureNextPoolExists`, scheduled jobs, logging.
- [x] **Pool Lifetime**: Hourly and daily jobs (`closeExpiredPools`, `archiveOldPools`).
- [x] **Image Chat**: Backend support (URL generation), Android upload flow.
- [x] **Kochi-Only Rollout**: City validation and hardcoded profile setup.

### Recent Fixes
- [x] Fixed "FF Failed INTERNAL" crash (Selective Exports in `index.js`).
- [x] Increased Join Rate Limit to 10/day.
- [x] Fixed Deployment Timeouts (Major Refactor of `functions/index.js`).
- [x] Codebase Cleanup (Dead code removal).

## Next Steps
- [ ] **Image Display**: Implementing image rendering in `MessageAdapter` (Android).
- [ ] **Storage Rules**: Refining security for `chat_images/`.
- [ ] **Beta Testing**: Deploy to internal track.

## History
See git history for detailed changes.
