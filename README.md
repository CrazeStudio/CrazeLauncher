<h1 align="center">CrazeLauncher</h1>

<p align="center">
  <a href="https://github.com/CrazeStudio/CrazeLauncher"><img src="app_pojavlauncher/src/main/assets/pojavlauncher.png" width="120" height="120" alt="CrazeLauncher Logo"></a>
</p>

<p align="center">
  <b>Play Minecraft: Java Edition on your Android device with advanced modding, instance management, and high performance!</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform Android">
  <img src="https://img.shields.io/badge/License-LGPL%20v3-blue.svg" alt="License LGPL v3">
  <img src="https://img.shields.io/badge/Kotlin-Compose-orange.svg" alt="Kotlin Compose">
</p>

---

## 📖 About CrazeLauncher

**CrazeLauncher** is an advanced, high-performance Minecraft: Java Edition launcher for Android built upon the robust foundation of [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher). It empowers players to run almost every version of Minecraft (from classic alpha/beta and release builds to the latest snapshots and combat tests) directly on mobile devices with support for modloaders like **Forge**, **NeoForge**, **Fabric**, **Quilt**, and **OptiFine**.

---

## ✨ Key Features

- **Robust Instance Management**: Easily create, configure, and manage separate game instances with isolated directories and custom settings.
- **Modpack & Mod Integration**: Built-in support for searching and installing mods and modpacks directly from Modrinth and CurseForge (.mrpack and zip imports).
- **Comprehensive Modloader Support**: Out-of-the-box installation and execution support for Forge, NeoForge, Fabric, Quilt, and OptiFine.
- **Optimized Rendering & Performance**: Advanced Java runtime execution (OpenJDK), OpenGL/GL4ES rendering acceleration, and customizable graphical controls.
- **Refined User Experience**: Sleek Material Design user interface with improved spinners, expandable lists, and smooth navigation.

---

## 🗺️ Table of Contents

- [About CrazeLauncher](#-about-crazelauncher)
- [Key Features](#-key-features)
- [Getting Started](#-getting-started)
- [Building from Source](#-building-from-source)
- [Roadmap](#️-roadmap)
- [Credits & Acknowledgments](#-credits--acknowledgments)
- [License](#-license)

---

## 🚀 Getting Started

You can obtain CrazeLauncher through the following channels:
1. **Releases**: Download the latest pre-built APK from the [Releases Page](https://github.com/CrazeStudio/CrazeLauncher/releases).
2. **Source Build**: Clone the repository and build the APK locally.

---

## ⚙️ Building from Source

To build CrazeLauncher locally:

1. Clone the repository:
   ```bash
   git clone https://github.com/CrazeStudio/CrazeLauncher.git
   cd CrazeLauncher
   ```
2. Build the debug APK using Gradle:
   ```bash
   ./gradlew :app_pojavlauncher:assembleFullDebug
   ```
   *(On Windows, use `gradlew.bat :app_pojavlauncher:assembleFullDebug`)*

---

## 🎯 Roadmap

- [x] Instance system in favor of legacy profiles
- [x] Out-of-the-box modern Minecraft version support
- [x] Modrinth & CurseForge `.mrpack` / `.zip` import support
- [x] Enhanced mod version spinner and UI layouts
- [ ] Advanced compute shader and rendering extensions
- [ ] Comprehensive mod/modpack management tools

---

## 💖 Credits & Acknowledgments

CrazeLauncher is made possible thanks to the hard work of open-source developers and contributors worldwide:

- **[PojavLauncher Team](https://github.com/PojavLauncherTeam/PojavLauncher)**: For creating the core Minecraft Java Android launcher foundation.
- **CrazeStudio & Maintainers**: For custom enhancements, instance management, UI refinements, and maintenance.
- **Third-Party Libraries & Components**:
  - **OpenJDK**: Java runtime environment.
  - **GL4ES / Holy GL4ES**: OpenGL acceleration layer.
  - **GLFW & SDL**: Windowing and input management.
  - **LWJGL 2 & 3**: Lightweight Java Game Library.
  - **Mesa 3D Graphics Library**: Open-source OpenGL implementation.
  - **Authlib-Injector**: Authorization support.
  - **Gson & OkHttp**: Networking and JSON parsing.

---

## 📄 License

CrazeLauncher is licensed under the **GNU Lesser General Public License v3.0 (LGPLv3)**. See the [LICENSE](LICENSE) file for details.
