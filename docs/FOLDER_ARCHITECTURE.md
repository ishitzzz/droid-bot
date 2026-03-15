# DroidBot Project Structure & Folder Architecture

This document breaks down the major directories of the DroidBot repository, explaining their function, contents, and how they interact in the Gradle/Android ecosystem.

---

## 1. 📱 `app/` (Application Module)
The `app` directory contains the actual source code, resources, and build configurations for DroidBot. This is the heart of the project.

```mermaid
graph TD
    App[app/] --> Src[src/main/]
    App --> BuildGradle[build.gradle.kts]
    App --> Proguard[proguard-rules.pro]
    
    Src --> Kotlin[java/com/droidbot/agent/]
    Src --> Res[res/]
    Src --> Manifest[AndroidManifest.xml]
    
    Kotlin --> Brain["brain/ - ReAct & LLM"]
    Kotlin --> Service["service/ - Accessibility & Foreground"]
    Kotlin --> UI["ui/ - Compose UI"]
    Kotlin --> Nav["navigation/ - UI Parsing & Execution"]
    
    Res --> Layout["layout/ - XML Fallbacks"]
    Res --> Values["values/ - Themes & Strings"]
```

### Purpose
- **`/src/main/java`**: Contains all Kotlin source code divided by feature packages (Brain, Navigation, Hive, Identity).
- **`/src/main/res`**: Contains Android resources, including vector icons, strings, and the Accessibility Service configuration XML.
- **`build.gradle.kts`**: The module-specific build script that manages dependencies (Compose, Gemini SDK, Hilt) and compiles the APK.

---

## 2. 🐘 `.gradle/` (Gradle Configuration Cache)
A hidden, auto-generated directory managed by the Gradle Build Tool. It stores configurations to speed up subsequent builds.

```mermaid
graph LR
    GradleRoot[.gradle/] --> Version[8.x/]
    Version --> Checksums[checksums/]
    Version --> Dependencies[dependencies-accessors/]
    Version --> Execution[executionHistory/]
    
    GradleRoot --> BuildOutputCache[buildOutputCleanup/]
    
    Developer["Developer runs: ./gradlew assembleDebug"] -->|"Checks Cache"| GradleRoot
    GradleRoot -->|"Cache Hit"| FastCompile("Fast Incremental Build")
    GradleRoot -->|"Cache Miss"| FullCompile("Full Project Compilation")
```

### Purpose
- **Performance**: Skips re-compiling tasks that haven't changed by saving hashes of source files.
- **Exclusion**: This directory is massive and machine-specific, which is why it is explicitly excluded in `.gitignore`.

---

## 3. 🏗️ `build/` and `app/build/` (Build Artifacts)
These directories contain the compiled outputs of the Gradle build process.

```mermaid
graph TD
    BuildRoot["build/ & app/build/"] --> Intermediates[intermediates/]
    BuildRoot --> Outputs[outputs/]
    BuildRoot --> Generated[generated/]
    BuildRoot --> Tmp[tmp/]
    
    Outputs --> APK["apk/debug/app-debug.apk"]
    Outputs --> Logs[logs/]
    
    Intermediates --> Dex["dex/ - Dalvik Executables"]
    Intermediates --> Res["res/ - Merged Resources"]
    Intermediates --> Javac["javac/ - Compiled classes"]
    
    Generated --> KSP["ksp/ - Hilt & Room generated code"]
    Generated --> BuildConfig["source/buildConfig/"]
```

### Purpose
- **`/outputs/apk/`**: Contains the final, installable `.apk` file that goes to the device or Play Store.
- **`/intermediates/`**: The "scratch pad" for the compiler. Raw Kotlin is turned into Java Bytecode, then into DEX files here.
- **`/generated/`**: Holds code that was auto-written by plugins like KSP (Kotlin Symbol Processing) for Hilt Dependency Injection. 

---

## 4. 🧠 `.idea/` (Android Studio IDE Settings)
A hidden directory created by IntelliJ/Android Studio to save your personal workspace settings.

```mermaid
graph TD
    Idea[.idea/] --> CodeStyles[codeStyles/]
    Idea --> Modules[modules.xml]
    Idea --> Workspace[workspace.xml]
    Idea --> NavEditor[navEditor.xml]
    Idea --> GradleXML[gradle.xml]
    
    Developer[Android Studio] -->|"Reads Preferences"| Idea
    Idea -->|"Configures Editor"| Editor["Syntax Highlighting, Run Configs, Formatting"]
```

### Purpose
- **`workspace.xml`**: Remembers which files you left open and where your cursors were.
- **`gradle.xml`**: Tells Android Studio where your local Gradle installation lives and how to sync the project.
- **Exclusion**: Like `.gradle/`, this folder contains local paths and user-specific view models. We exclude `workspace.xml`, `tasks.xml`, and caches via `.gitignore` so developers don't overwrite each other's UI preferences on Git.
