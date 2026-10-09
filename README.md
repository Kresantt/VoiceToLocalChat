# VoiceToLocalChat — development source (NOT a working transcription plugin)

This is a safe integration foundation for Paper 26.2. **It does not decode audio, call Vosk, or generate transcripts.** Do not install expecting speech-to-text to work. No compiled JAR is included.

## Verified integration

SimpleGlobChat v1.05 stores the local-chat toggle at `chat.local.toggle` and the radius at `chat.local.radius` (default 50). `localRadius()` reads the **live SimpleGlobChat config**, returning -1 if unavailable or disabled. `deliverLocal` uses that radius, same-world recipients, and direct chat messages rather than broadcasting chat events. This prevents accidental DiscordSRV/global-chat forwarding, but does not guarantee exact formatting parity with SimpleGlobChat.

The plugin's `/vt` alias is declared alongside `/voicetext`. The master switch defaults off. `/vt on` **cannot** activate speech recognition in this development build and truthfully reports that it is unavailable. The session opt-in set is cleared on logout. Audio is never accessed by this build.

## To complete before deployment

1. Resolve the matching Paper 26.2 and Simple Voice Chat API dependencies and verify exact API event signatures.
2. Register the Voice Chat API plugin and audio microphone listener; filter UUIDs before any decode or buffering.
3. Decode Opus audio into Vosk-compatible PCM, with sample-rate handling, VAD, and bounded per-player buffers.
4. Integrate native Vosk binaries and an offline language model; validate CPU/RAM usage and backpressure.
5. Add cancellation tokens for `/vt off`, logout, master disable, and server shutdown.
6. Schedule recognized text back to the Paper main thread, then call `deliverLocal`.
7. Build with Java 26 and test against the user's exact Paper, Simple Voice Chat, SimpleGlobChat and PronounHover versions.

## Dependencies

The Gradle build requires external repositories. This environment does not have access to them or Java 26, so compilation and runtime testing have not been completed.

## Recommended settings for 20 GB RAM, 30–50 concurrent players

The bundled `config.yml` selects the lightweight Apache-2.0 Vosk English model (`vosk-model-small-en-us-0.15`), 2 recognition workers, a maximum of 10 opted-in players, 5-second audio segments, and a bounded 20-segment queue. All transcription remains OFF by default. These values are *configuration intentions*, not implemented limits until the audio/recognition engine is completed. Monitor CPU/TPS under load and adjust accordingly.

Official model download: https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip
Extract into `plugins/VoiceToLocalChat/models/vosk-model-small-en-us-0.15/` when the working plugin is ready. The model is not bundled in this source archive.

## CI build workflow

A GitHub Actions Java 26 build workflow is included at `.github/workflows/build.yml`.
Upload the project contents to a GitHub repository, then select Actions > Build VoiceToLocalChat > Run workflow.
**Important:** This only attempts to compile the development scaffold. A successful CI build does NOT mean voice transcription works; the audio-to-text engine is not implemented.
