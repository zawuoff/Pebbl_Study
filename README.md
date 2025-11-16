# Pebbl - AI-Powered Academic Voice Assistant

Pebbl is an Android application that helps students transform spoken ideas into structured academic drafts through AI-guided conversations.

## Features

- **Voice-to-Text**: Offline speech recognition using Vosk SDK
- **AI-Guided Dialogue**: Intelligent follow-up questions from OpenRouter AI
- **Draft Generation**: Automatic conversion of conversations into academic drafts
- **Project Management**: Organize multiple writing projects
- **Privacy-First**: Audio processing happens offline; only text is sent to AI

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Repository pattern
- **Database**: Room (SQLite)
- **Speech Recognition**: Vosk SDK (offline)
- **AI Integration**: OpenRouter API (openai/gpt-4o-mini)
- **Networking**: Retrofit + OkHttp
- **Async**: Kotlin Coroutines + Flow

## Prerequisites

1. **Android Studio**: Hedgehog (2023.1.1) or later
2. **Minimum SDK**: 24 (Android 7.0)
3. **Target SDK**: 36
4. **OpenRouter API Key**: Get one from [OpenRouter](https://openrouter.ai/)
5. **Vosk Model**: Download speech recognition model

## Setup Instructions

### 1. Clone the Repository

```bash
git clone <repository-url>
cd studypal
```

### 2. Configure OpenRouter API Key

Edit `local.properties` and add your API key:

```properties
OPENROUTER_API_KEY=your_actual_api_key_here
```

**Important**: Never commit `local.properties` to version control!

### 3. Download Vosk Model

1. Download the Vosk model from: https://alphacephei.com/vosk/models
   - Recommended: `vosk-model-small-en-us-0.15.zip` (40MB)
   - Alternative: `vosk-model-en-us-0.22.zip` (1.8GB, higher accuracy)

2. Extract the model and place it in your app's files directory:
   ```
   /data/data/com.fouwaz.studypal/files/vosk-model-small-en-us-0.15/
   ```

3. **For Development**: You can use ADB to push the model:
   ```bash
   adb push vosk-model-small-en-us-0.15 /sdcard/
   # Then move it via app code or manually
   ```

### 4. Sync Gradle

Open the project in Android Studio and let Gradle sync all dependencies.

### 5. Build and Run

- Connect an Android device or start an emulator
- Click "Run" in Android Studio
- Grant microphone permission when prompted

## Project Structure

```
studypal/
├── app/
│   └── src/
│       └── main/
│           └── java/com/fouwaz/studypal/
│               ├── data/
│               │   ├── local/
│               │   │   ├── entity/          # Room entities
│               │   │   ├── dao/             # Data Access Objects
│               │   │   └── VoiceStreamDatabase.kt
│               │   ├── remote/
│               │   │   ├── api/             # Retrofit services
│               │   │   └── model/           # API models
│               │   └── repository/          # Repository layer
│               ├── domain/
│               │   └── model/               # Domain models
│               ├── speech/
│               │   └── VoiceRecognitionManager.kt
│               ├── ui/
│               │   ├── screens/             # Compose screens
│               │   ├── viewmodel/           # ViewModels
│               │   ├── navigation/          # Navigation
│               │   └── theme/               # App theme
│               ├── MainActivity.kt
│               └── VoiceStreamApplication.kt
├── gradle/
├── build.gradle.kts
└── local.properties
```

## Usage

### 1. Create a Project

- Tap the "+" button on the home screen
- Enter a project title (e.g., "Essay on Climate Change")
- Optionally add tags
- Click "Create"

### 2. Start a Voice Session

- Tap "Start Session" on a project card
- Speak your initial thoughts on the topic
- The AI will ask follow-up questions
- Continue the conversation to develop your ideas
- Tap "Finish" when ready to generate a draft

### 3. View Generated Draft

- Navigate to "View Draft" from the project card
- Review the AI-generated academic draft
- Export as TXT or PDF (if export feature is enabled)

## Database Schema

### Projects Table
```sql
CREATE TABLE projects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    tags TEXT,
    created_at INTEGER,
    updated_at INTEGER,
    is_active INTEGER
);
```

### Voice Streams Table
```sql
CREATE TABLE voice_streams (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER,
    transcribed_text TEXT,
    ai_question TEXT,
    sequence_number INTEGER,
    created_at INTEGER,
    FOREIGN KEY(project_id) REFERENCES projects(id)
);
```

### Drafts Table
```sql
CREATE TABLE drafts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER,
    content TEXT,
    version INTEGER,
    created_at INTEGER,
    updated_at INTEGER,
    is_current INTEGER,
    FOREIGN KEY(project_id) REFERENCES projects(id)
);
```

## API Integration

### OpenRouter Configuration

Base URL: `https://openrouter.ai/api/v1/`

**Headers Required:**
```
Authorization: Bearer YOUR_API_KEY
Content-Type: application/json
HTTP-Referer: https://github.com/voicestream-android
X-Title: VoiceStream Android App
```

**Example Request:**
```json
{
  "model": "openai/gpt-4o-mini",
  "messages": [
    {"role": "system", "content": "You are an academic assistant..."},
    {"role": "user", "content": "Student's response..."}
  ],
  "temperature": 0.7,
  "max_tokens": 150
}
```

## Troubleshooting

### Issue: "Voice recognition model not found"

**Solution**: Ensure the Vosk model is correctly placed in the app's files directory. Check:
- Model folder name matches `MODEL_NAME` constant in `VoiceRecognitionManager.kt`
- Model files are complete and not corrupted
- App has proper file permissions

### Issue: "OpenRouter API key not configured"

**Solution**:
1. Check `local.properties` has the correct API key
2. Rebuild the project (Build > Rebuild Project)
3. Verify the key is valid at https://openrouter.ai/

### Issue: Microphone not working

**Solution**:
1. Go to Android Settings > Apps > StudyPal > Permissions
2. Enable "Microphone" permission
3. Restart the app

### Issue: Build fails with KSP errors

**Solution**:
```bash
./gradlew clean
./gradlew build
```

## Performance Considerations

- **Vosk Model Size**: Use the small model (40MB) for faster loading
- **API Rate Limits**: OpenRouter free tier may have rate limits
- **Database**: Room handles migrations automatically
- **Memory**: Voice recognition keeps minimal audio in memory

## Security & Privacy

- Audio is processed **offline** using Vosk
- Only transcribed text is sent to OpenRouter
- API key is stored securely in BuildConfig (never exposed in code)
- Local database is encrypted by Android's file system
- No telemetry or tracking

## Future Enhancements

- [ ] Cloud sync with user accounts
- [ ] Export to Google Docs / Word
- [ ] Multiple language support
- [ ] Voice activity detection
- [ ] Offline AI using on-device LLM
- [ ] Collaboration features
- [ ] Citation management
- [ ] Style presets (APA, MLA, Chicago)

## Dependencies

Major libraries used:

```gradle
// Core
androidx.core:core-ktx:1.10.1
androidx.lifecycle:lifecycle-runtime-ktx:2.6.1
androidx.activity:activity-compose:1.8.0

// Compose
androidx.compose:compose-bom:2024.09.00
androidx.compose.material3:material3

// Room
androidx.room:room-runtime:2.6.1
androidx.room:room-ktx:2.6.1

// Retrofit
com.squareup.retrofit2:retrofit:2.9.0
com.squareup.retrofit2:converter-gson:2.9.0

// Vosk
com.alphacephei:vosk-android:0.3.38

// Navigation
androidx.navigation:navigation-compose:2.7.7
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- **Vosk**: Open-source speech recognition toolkit
- **OpenRouter**: Unified API for LLM access
- **Jetpack Compose**: Modern Android UI toolkit
- **Material Design 3**: Google's design system

## Contact & Support

For issues, questions, or contributions:
- Open an issue on GitHub
- Email: zawuoff@gmail.com

## Version History

### v1.0.0 (Current)
- Initial release
- Project management
- Voice recognition with Vosk
- AI-powered follow-up questions
- Draft generation
- Material 3 UI

---

**Built for students by students**

