# Sequence

<p align="center">
  <img src="assets/brain.jpg" alt="App Icon" width="128"/>
</p>

<p align="center">
  <strong>An alternative to to-do lists for people with ADHD</strong><br>
</p>

<p align="center">
  <img src="assets/poster.jpg" alt="Screenshot 1" width="900" style="border-radius:26px;"/>
  <img src="assets/1.jpg" alt="Screenshot 1" width="200" style="border-radius:26px;"/>
  <img src="assets/2.jpg" alt="Screenshot 2" width="200" style="border-radius:26px;"/>
  <img src="assets/3.jpg" alt="Screenshot 3" width="200" style="border-radius:26px;"/>
  <img src="assets/5.jpg" alt="Screenshot 4" width="200" style="border-radius:26px;"/>
</p>

<p align="center">
    <a href="https://github.com/Cognaque/Sequence/releases">
        <img src="https://img.shields.io/github/v/release/Cognaque/Sequence?include_prereleases&logo=github&style=for-the-badge&label=Latest%20Release" alt="Latest Release">
    </a>
    <a href="https://github.com/Cognaque/Sequence/releases">
        <img src="https://img.shields.io/github/downloads/Cognaque/Sequence/total?logo=github&style=for-the-badge" alt="Total Downloads">
    </a>
    <img src="https://img.shields.io/badge/Android-10%2B-green?style=for-the-badge&logo=android" alt="Android 10+">
    <img src="https://img.shields.io/badge/Kotlin-100%25-purple?style=for-the-badge&logo=kotlin" alt="Kotlin">
</p>


---
## The Logic Core

**Impact Score = (LongTerm * 0.4) + (Proximity * 0.3) + (Immediate * 0.2) + (Accumulation * 0.1)**

> Note: Long-term goals (0.4) are weighted twice as heavily as immediate needs (0.2).

Time Factor: Every day a task exists, its Proximity score effectively increases by 0.05 (up to a max boost of 0.5). This simulates a deadline getting closer, pushing tasks up the priority list automatically.

Quadrant Rules
---
**Q1 Priority (Do Now)**: Impact Score ≥ 0.5 OR marked as Daily Chore/Manually Promoted.
**Q2 Schedule (Plan)**: Average Importance (LongTerm + Accumulation) ≥ 0.5.
**Q3 Delegate (Push)**: Average Urgency (Immediate + Proximity) ≥ 0.5.
**Q4 Later (Eliminate)**: Everything else.

**5 Example Tasks & Their Journey**

Here is how 5 different tasks would be arranged and how time affects them:

**1. "Pay Rent" (High Urgency, High Importance)**

Scores: Immediate: 0.9, LongTerm: 0.9, Proximity: 0.9

Calculation: (0.9*0.4) + (0.9*0.3) + (0.9*0.2) + ... ≈ 0.86

> Result: Q1 Priority.

Time Effect: It starts in Q1 and stays there until completed.

**2. "Learn French" (Low Urgency, High Importance)**

Scores: Immediate: 0.1, LongTerm: 0.9, Proximity: 0.1

Calculation: Impact ≈ 0.43 (Below 0.5 threshold).

Check Q2: Avg Importance (0.9 + 0.2) / 2 = 0.55.

> Result: Q2 Schedule.

Time Effect: As days pass, the "Age Factor" adds to Proximity. After ~5 days, the Impact score rises above 0.5, moving it into Q1 Priority, forcing you to act on your goals.

**3. "Respond to generic email" (High Urgency, Low Importance)**
 
Scores: Immediate: 0.8, LongTerm: 0.1, Proximity: 0.8

Calculation: Impact ≈ 0.45 (Below threshold).

Check Q2: Avg Importance is low.

Check Q3: Avg Urgency (0.8 + 0.8) / 2 = 0.8.

> Result: Q3 Delegate.

Time Effect: It stays in Q3 unless it sits for so long (10+ days) that the age boost pushes its Impact Score > 0.5, eventually annoying you enough to become a Q1 Priority.

**4. "Watch random TV re-runs" (Low Urgency, Low Importance)**
   
Scores: All values low (~0.1).

Calculation: Impact ≈ 0.1.

> Result: Q4 Later.

Time Effect: Even with the maximum age boost (+0.5 after 10 days), the Impact Score only reaches ~0.25. It will never naturally move to Q1, effectively filtering out noise.

**5. "Buy Anniversary Gift" (Future Deadline)**
   
Day 0: LongTerm: 0.9, but Immediate/Proximity are low (0.2).

Result: Q2 Schedule (Important, but not urgent).

Day 4: The task is now 4 days old.

Logic: Age Factor adds 4 * 0.05 = 0.20 to the Proximity score.

New Impact: The score crosses the 0.5 threshold.

> Result: Moves to Q1 Priority.

Time Effect: The system automatically promotes this important task from "Plan it" to "Do it" as the "deadline" (simulated by age) approaches, without you manually changing the date.

---

## ✨ Sequence (ADHD to-do list)

A privacy-focused, offline-first productivity tool designed for the ADHD brain. **Sequence** is not just a to-do list; it is a "Consequence Engine" that uses a local AI to learn how _you_ perceive urgency and importance, helping you overcome executive dysfunction through momentum scoring and automated prioritization.

## ✨ Features

### 🧠 AI Learning Engine

- **Local Intelligence** - A custom `LearningEngine` that tokenizes your tasks and learns from your inputs.
    
- **Predictive Weighting** - Automatically predicts 5 key factors for new tasks based on past history:
    
    - _Immediate Consequences_
        
    - _Long-term Impact_
        
    - _Proximity (Point of No Return)_
        
    - _Accumulation (Compound effect)_
        
    - _Effort (Activation Energy)_
        
- **Training Mode** - A "Clarification Overlay" asks specific questions to train the AI when it encounters unknown tasks.
    

### 📝 Task Protocol

- **Impact & Momentum Scoring** - Tasks are ranked not just by date, but by an `Impact Score` (consequence severity) and `Momentum Score` (bang-for-your-buck).

- **Task Aging** - Tracks the lifespan of every task (e.g., "4d Old") and slowly escalates their Impact Score day-by-day so forgotten tasks eventually surface. 

- **Eisenhower Sorting** - Automatically categorizes tasks into Priority, Schedule, Delegate, or Later based on calculated urgency/importance.
    
- **Daily Chores** - Persistent daily routines that regenerate automatically every day (visually distinct in pink).
    
- **Brain Dump** - Quick-capture interface to offload mental clutter immediately.
    

### 🛡️ Neurodivergent-Friendly UX

- **The Vault** - A hidden "Focus Stack" for non-priority items. Access it by long-pressing the "PRIORITY" header for 5 seconds, to keep your main view distraction-free.
    
- **Wind Down Mode** - Automatically filters out high-effort tasks after a configurable hour (default 10 PM) to protect sleep hygiene.
    
- **Visual Dopamine** - Swipe-to-complete with satisfying animations and color transitions based on task urgency.
    
- **Dark Mode Native** - Designed with an OLED-black background to reduce sensory load.
    

### 📊 Insight & Data

- **Brain Memory** - View the raw weights and logic the AI has learned for specific keywords.
    
- **Impact Analysis** - Tap any task to see exactly why it was prioritized (e.g., "Critical Immediate Consequence").
    

## 🛠️ Tech Stack

This application is built as a modern, single-activity Android application.

|Category|Technology|
|---|---|
|**Language**|[Kotlin](https://kotlinlang.org/ "null")|
|**UI Framework**|[Jetpack Compose](https://developer.android.com/jetpack/compose "null") (Material 3)|
|**Architecture**|MVVM (Model-View-ViewModel)|
|**Database**|[Room](https://developer.android.com/training/data-storage/room "null") (SQLite)|
|**Concurrency**|Kotlin Coroutines & `StateFlow`|
|**Animation**|Compose Animation API (`animateColorAsState`, `Canvas`)|
|**DI**|Manual Dependency Injection (`ViewModelFactory`)|
|**Integration**|Android Calendar Intents|

_Note: This app is offline-first and does not require internet permissions._

## 📱 Requirements

- **Android 10** (API 29) or higher recommended.
    
- **Dark Mode** enabled device preferred.
    

## 🚀 Getting Started

### Prerequisites

- Android Studio Ladybug | 2024.2.1 or newer
    
- JDK 11+
    

### Installation

1. **Clone the repository**
    
    ```
    git clone https://github.com/Cognaque/Sequence.git
    ```
    
2. **Open in Android Studio**
    
    - Open Android Studio
        
    - Select "Open an Existing Project"
        
    
    - Navigate to the cloned directory
        
3. **Build & Run**
    
    - Sync Gradle files.
        
    - Connect a device or emulator.
        
    - Click Run (▶️).
        

### How to Use

1. **Brain Dump:** Type any task into the bottom bar (e.g., "Pay electricity bill").
    
2. **Train:** If the AI doesn't recognize the words, it will ask you to rate the consequences (Immediate, Long-term, etc.).
    
3. **Execute:** The app will calculate a score. High-impact tasks float to the top.
    
4. **Vault:** Long-press the "PRIORITY" title at the top to see your backlog (The Vault) or manage Daily Chores.

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

