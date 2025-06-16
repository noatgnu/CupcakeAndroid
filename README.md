# Cupcake Android

A modern Android application with full-featured functionality and WearOS companion app support.

## Overview

Cupcake is a comprehensive Android application that demonstrates modern Android development practices. It includes features like camera integration, audio recording, and real-time communication through WebSockets. The app is built with a modular architecture, separating core functionality, communication layers, and UI components.

## Project Structure

The project is organized into several modules:

- **app**: Main Android application module
- **shared**: Shared code and utilities used across modules
- **communication**: Network and communication-related functionality (Not yet implemented)
- **wearOS**: Companion app for WearOS devices (Not yet implemented)

## Features

- Camera integration
- Audio recording capabilities
- Real-time communication via WebSockets
- Support for both light and dark modes
- Multi-device support with WearOS integration (Not yet implemented)

## Technical Specifications

- **Min SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 35
- **Compile SDK**: 35
- **Language**: Kotlin
- **Architecture**: MVVM with modular design

## Technologies and Libraries

- **Dependency Injection**: Hilt
- **Navigation**: Jetpack Navigation Component with SafeArgs
- **UI Binding**: View Binding
- **Networking**: WebSockets for real-time communication
- **Concurrency**: Kotlin Coroutines
- **JVM Target**: 11

## Building the Project

### Prerequisites

- Android Studio Electric Eel (2022.1.1) or newer
- JDK 11 or higher
- Kotlin 1.9 or newer

### Steps to Build

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Build the project using the Build menu or Gradle tasks

## Permissions

The application requires the following permissions:

- `INTERNET`: For network communication
- `CAMERA`: For camera functionality
- `RECORD_AUDIO`: For audio recording features
- `VIBRATE`: For haptic feedback

## Contributing

Contributions to the Cupcake Android project are welcome. Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT license with terms found in the LICENSE file at the root of this repository.

## Contact

For any inquiries or issues, please open an issue in the project repository.
