# Tetris Mobile

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Compose-BOM%202024.09-green.svg?style=flat&logo=android)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%203-Enabled-purple.svg?style=flat&logo=material-design)](https://m3.material.io/)
[![Platform](https://img.shields.io/badge/Platform-Android%2013%2B-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A foss, high-performance Tetris experience for Android, built from the ground up using **Kotlin** and **Jetpack Compose**. 

> **Status:** This project is passively maintained and not in fully active development.

---

## Why?

I've forever been a tetris addict, and I don't like the absurdly maximalist UIs built for the game everywhere, since it's a pacifier game for me. It helps me feel calm, so I wanted an equally calming aesthetic designed around the game so I can always have my personal pocket tetris be as I'd like.

---

## Architecture & Tech Stack

- **UI Layer:** 100% Jetpack Compose. No XML layouts.
- **State Management:** `StateFlow` and `ViewModel` for reactive, lifecycle-aware updates.
- **Game Engine:** Pure Kotlin logic decoupled from the Android Framework, making it highly testable.
- **Concurrency:** Kotlin Coroutines for the game loop, ensuring 60fps-equivalent smoothness without blocking the UI thread. (120 fps soon? idk)

### Key Components

| Component | Responsibility |
|-----------|----------------|
| `GameEngine` | Core mechanics: collision detection, line clearing, and scoring logic. |
| `GameViewModel` | State orchestration, gravity timing, and input handling. |
| `TetrisPiece` | Immutable data structure representing tetrominoes and their rotations. |
| `SwipeController` | Custom PointerInput handler for complex gesture recognition. |
| `HighScoreManager` | SharedPrefs-backed persistence for game statistics. |

---

## Getting Started

### Prerequisites

- **Android Studio Koala+** (2024.1.1+)
- **JDK 17** (bundled with Android Studio)
- **Android SDK 36** (`compileSdk` / `targetSdk` = 36, `minSdk` = 33 — Android 13+)

### Installation & Build

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/tetris-mobile.git
   cd tetris-mobile
   ```

2. **Build the project:**
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install on device:**
   ```bash
   ./gradlew installDebug
   ```

---

## Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the MIT License. See [`LICENSE`](LICENSE) for full text.
