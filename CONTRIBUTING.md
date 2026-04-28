# Contributing to Tetris Mobile

Thank you for your interest in contributing! This guide will help you set up your development environment and understand our coding standards.

---

## Development Setup

### Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| Android Studio | Koala (2024.1.1+) | Latest stable recommended |
| JDK | 17+ | Bundled with Android Studio |
| Android SDK | API 35/36 | Target API 36, Min API 35 |
| Gradle | 8.13+ | Wrapper included in repo |

### Getting Started

```bash
# Fork and clone the repository
git clone https://github.com/your-username/tetris-mobile.git
cd tetris-mobile

# Build the debug APK
./gradlew assembleDebug
```

Open the project in Android Studio and let Gradle sync complete before making changes.

---

## Project Architecture

This project uses **MVVM** with **Unidirectional Data Flow**:

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│   (Jetpack Compose: MainActivity.kt, game screens)         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    ViewModel Layer                           │
│   (GameViewModel.kt - state management, input handling)     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Domain/Logic Layer                         │
│   (GameEngine.kt, TetrisPiece.kt, GameState.kt)            │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Data Layer                              │
│   (HighScoreManager.kt - SharedPrefs persistence)          │
└─────────────────────────────────────────────────────────────┘
```

### Key Components

- **GameEngine** — Core mechanics: collision detection, line clearing, scoring, wall kicks
- **GameViewModel** — StateFlow-based state management, game loop timing, input orchestration
- **TetrisPiece** — Immutable tetromino definitions with rotation states
- **SwipeController** — Custom gesture recognition for mobile-first controls

---

## Code Standards

### Kotlin Style

- Follow the [official Kotlin style guide](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful names for all identifiers
- Keep functions small and focused (single responsibility)
- Prefer immutability where practical

### Jetpack Compose Best Practices

- **State hoisting** — Lift state up to the lowest common ancestor
- **Unidirectional data flow** — State flows down, events flow up
- **Modifier parameters** — Use `Modifier` as the first optional parameter for customization
- **Remember & derivedStateOf** — Use appropriately to avoid unnecessary recompositions

### Architecture Guidelines

- Keep the UI layer free of business logic
- Game mechanics should be testable without Android dependencies
- Expose state via `StateFlow`, not mutable variables

---

## Working with Git

### Branch Naming

Use descriptive, lowercase branch names with slashes:

```
feature/ghost-piece
fix/rotation-collision
enhance/swipe-sensitivity
docs/readme-update
```

### Commit Messages

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add hard drop sound effect
fix: resolve wall kick boundary bug  
docs: update Android SDK requirements
style: reformat GameBoard composable
refactor: extract collision detection to GameEngine
test: add unit tests for TetrisPiece rotations
```

### Pull Request Process

1. **Create a feature branch** from `main`
2. **Make your changes** following the code standards
3. **Add tests** for new features or bug fixes
4. **Verify the build** passes: `./gradlew assembleDebug`
5. **Submit a PR** with a clear description of changes

For UI changes, include screenshots in your PR description.

---

## Testing

### Unit Tests

```bash
./gradlew test
```

Test files are located in `app/src/test/`. The game engine logic should have comprehensive unit test coverage.

### Instrumented Tests

```bash
./gradlew connectedAndroidTest
```

Run on a connected device or emulator. Located in `app/src/androidTest/`.

---

## Suggested Contribution Areas

Looking for something to work on? Here are some ideas from our roadmap:

- [ ] **Ghost Piece** — Visual indicator showing where the piece will land
- [ ] **Hold Piece** — Swap and hold tetrominos for later use
- [ ] **Haptic Feedback** — Tactile response for rotations and line clears
- [ ] **Theme Support** — Dark/Light mode and custom color palettes
- [ ] **Line Clear Animations** — Smoother transitions when clearing lines

---

## Getting Help

- Open an issue for bugs or feature requests
- Start a discussion for questions about the codebase
- Check existing issues before creating new ones

Happy coding!
