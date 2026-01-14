# Pool Dating - Android App

Status: **Active Development**  
Version: **0.8.0**

## Overview
Pool Dating is an Android application that provides a localized, safe, and balanced dating experience. Users join "Pools" based on their city, where a 60:40 gender ratio is strictly enforced. After a joining period, the pool is "completed" and matches are generated.

## Key Features
- **Sequential Pools**: Only one active pool per city at a time.
- **Gender Balancing**: Enforced ratios (e.g., 60 Men / 40 Women).
- **Safe Messaging**: Image sharing and chat are monitored and expire after 4 days.
- **Admin Tools**: Comprehensive dashboard for monitoring and managing pool lifecycles.

## Tech Stack
- **Android**: Kotlin, MVVM, LiveData, Coroutines.
- **Backend**: Firebase Cloud Functions (Node.js 20).
- **Database**: Firestore (Real-time).
- **Auth**: Firebase Phone Auth.

## Setup
### Prerequisites
- JDK 17
- Android Studio Koala+
- Firebase Project with Blaze Plan (for Cloud Functions)

### Build
1. Clone repository.
2. Add `google-services.json` to `/app`.
3. Build with Gradle: `./gradlew assembleDebug`

## Project Structure
- `/app`: Android Application code.
- `/functions`: Firebase Cloud Functions (Backend).
- `/docs`: Documentation and Roadmaps.

## License
Proprietary.
