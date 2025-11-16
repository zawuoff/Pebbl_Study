# VoiceStream Setup Guide

This guide will walk you through setting up VoiceStream from scratch.

## Quick Start Checklist

- [ ] Android Studio installed
- [ ] OpenRouter API key obtained
- [ ] Vosk model downloaded
- [ ] Project dependencies synced
- [ ] Microphone permission granted
- [ ] App tested and running

## Step-by-Step Setup

### Step 1: Install Android Studio

1. Download Android Studio from: https://developer.android.com/studio
2. Install with default settings
3. Open Android Studio and complete the setup wizard

### Step 2: Get OpenRouter API Key

1. Visit https://openrouter.ai/
2. Sign up for a free account
3. Navigate to "Keys" section
4. Create a new API key
5. Copy the key (you'll need it in Step 4)

**Free Tier Limits:**
- The `openai/gpt-4o-mini` model is free to use
- Rate limits apply (check OpenRouter docs for current limits)

### Step 3: Download Vosk Model

**Option A: Small Model (Recommended for Testing)**
- Model: `vosk-model-small-en-us-0.15`
- Size: ~40 MB
- Download: https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip

**Option B: Full Model (Better Accuracy)**
- Model: `vosk-model-en-us-0.22`
- Size: ~1.8 GB
- Download: https://alphacephei.com/vosk/models/vosk-model-en-us-0.22.zip

**Installation Steps:**
1. Download and extract the model
2. You'll need to place it in the app's data directory after installation

### Step 4: Configure the Project

1. Open the project in Android Studio
2. Edit `local.properties` file (at project root)
3. Add your OpenRouter API key:

```properties
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
OPENROUTER_API_KEY=sk-or-v1-xxxxxxxxxxxxxxxxxxxxx
```

Replace `sk-or-v1-xxxxxxxxxxxxxxxxxxxxx` with your actual API key.

**Security Note:** Never commit `local.properties` to git. It's already in `.gitignore`.

### Step 5: Sync Gradle Dependencies

1. In Android Studio, click "File" → "Sync Project with Gradle Files"
2. Wait for the sync to complete (may take a few minutes first time)
3. Resolve any errors that appear

**Common Gradle Issues:**

**Issue: "Failed to resolve: androidx.room:room-compiler"**
Solution: Update Gradle plugin in `gradle/wrapper/gradle-wrapper.properties`

**Issue: "KSP plugin not found"**
Solution: Ensure KSP version matches Kotlin version in `gradle/libs.versions.toml`

### Step 6: Install Vosk Model on Device

**Method A: Via ADB (Development)**

```bash
# Push model to device storage
adb push vosk-model-small-en-us-0.15 /sdcard/Download/

# Then use Android file manager to move it to:
# /data/data/com.fouwaz.studypal/files/vosk-model-small-en-us-0.15/
```

**Method B: Manual Installation (Recommended)**

1. Build and install the app first
2. Use Android Studio's Device File Explorer:
   - View → Tool Windows → Device File Explorer
   - Navigate to `/data/data/com.fouwaz.studypal/files/`
   - Right-click → Upload
   - Select your extracted Vosk model folder

**Method C: Programmatic Download (Future Enhancement)**
Add code to download model on first launch.

### Step 7: Build and Run

1. Connect an Android device via USB (or start an emulator)
2. Enable USB Debugging on your device:
   - Settings → About Phone → Tap "Build Number" 7 times
   - Settings → Developer Options → Enable USB Debugging
3. In Android Studio, click the "Run" button (green triangle)
4. Select your device from the list
5. Wait for build and installation

### Step 8: Grant Permissions

When the app launches for the first time:

1. A permission dialog will appear requesting microphone access
2. Tap "Allow" to grant permission
3. If you accidentally deny:
   - Go to Settings → Apps → StudyPal → Permissions
   - Enable Microphone

### Step 9: Test the App

1. **Create a Project:**
   - Tap the "+" floating action button
   - Enter "Test Project" as the title
   - Add tags like "test, demo" (optional)
   - Tap "Create"

2. **Start a Voice Session:**
   - Tap "Start Session" on your test project
   - Speak clearly: "I want to write about climate change"
   - Wait for AI follow-up question
   - Continue the conversation

3. **Generate a Draft:**
   - After 2-3 exchanges, tap "Finish Session"
   - Wait for the AI to generate your draft
   - View the generated academic text

## Troubleshooting

### App crashes on launch

**Check:**
1. Is the OpenRouter API key configured correctly?
2. Are all Gradle dependencies synced?
3. Check Logcat in Android Studio for error messages

**Solution:**
```bash
# Clean and rebuild
./gradlew clean
./gradlew build
```

### Voice recognition not working

**Check:**
1. Is the Vosk model installed in the correct location?
2. Is the model folder name correct (check `VoiceRecognitionManager.kt`)?
3. Does the app have microphone permission?

**Verify model location:**
Use Device File Explorer to check:
`/data/data/com.fouwaz.studypal/files/vosk-model-small-en-us-0.15/`

### AI not responding

**Check:**
1. Is your device connected to the internet?
2. Is the OpenRouter API key valid?
3. Check Logcat for API errors

**Test API key:**
```bash
curl https://openrouter.ai/api/v1/auth/key \
  -H "Authorization: Bearer YOUR_API_KEY"
```

### Build errors with KSP

**Solution:**
Ensure Kotlin and KSP versions match in `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.0.21"
ksp = "2.0.21-1.0.25"
```

## Updating the Model Name

If you use a different Vosk model, update the constant:

File: `app/src/main/java/com/fouwaz/studypal/speech/VoiceRecognitionManager.kt`

```kotlin
private const val MODEL_NAME = "vosk-model-small-en-us-0.15"
// Change to your model name, e.g.:
// private const val MODEL_NAME = "vosk-model-en-us-0.22"
```

## Performance Optimization

### Reduce APK Size
- Use the small Vosk model
- Enable ProGuard in release builds
- Remove unused resources

### Improve Recognition Accuracy
- Use the larger Vosk model (1.8GB)
- Speak clearly and at moderate pace
- Reduce background noise

### API Cost Management
- The free `openai/gpt-4o-mini` model has rate limits
- For production, consider purchasing OpenRouter credits
- Implement exponential backoff for retries

## Next Steps

Once setup is complete:

1. **Read the README.md** for detailed usage instructions
2. **Explore the code** to understand the architecture
3. **Customize** the AI prompts in `AiRepository.kt`
4. **Add features** like export functionality
5. **Deploy** to Google Play Store (requires signed APK)

## Getting Help

- **GitHub Issues**: Report bugs or request features
- **Stack Overflow**: Tag questions with `voicestream` and `android`
- **Documentation**: Check inline comments in the code

## Configuration Files Reference

### local.properties
```properties
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
OPENROUTER_API_KEY=your_api_key_here
```

### gradle/libs.versions.toml
Contains all dependency versions - do not modify unless updating dependencies

### AndroidManifest.xml
Contains permissions and app configuration - already configured

## Development Workflow

1. Make changes to code
2. Build: `./gradlew build`
3. Test on device
4. Commit to git
5. Push to repository

## Production Checklist

Before releasing to production:

- [ ] Update version code and name in `build.gradle.kts`
- [ ] Enable ProGuard/R8 code shrinking
- [ ] Remove all debug logs
- [ ] Test on multiple devices and Android versions
- [ ] Generate signed APK/Bundle
- [ ] Upload to Google Play Console
- [ ] Create release notes

---

**Setup Complete! 🎉**

You're now ready to use VoiceStream. Create your first project and start transforming spoken ideas into academic drafts!
