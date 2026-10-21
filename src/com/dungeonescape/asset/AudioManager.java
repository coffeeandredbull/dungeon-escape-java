
package com.dungeonescape.asset;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and plays the game's sound effects from {@code audio/}.
 *
 * <p>Each file is decoded to PCM_SIGNED 16-bit, read fully into memory, and
 * re-framed to its true length before being opened as a {@link Clip}. This
 * handles WAV headers that declare an incorrect frame length without
 * modifying the asset file.</p>
 *
 * <p>Failures are recorded in {@link #errors()}. If the audio device is
 * unavailable, playback is disabled.</p>
 */
public class AudioManager {

    private final Map<String, Clip> clips = new HashMap<>();
    private final List<String> errors = new ArrayList<>();
    private final boolean available;

    /** Probes the audio subsystem and prepares an empty clip cache. */
    public AudioManager() {
        boolean ok;
        try {
            ok = AudioSystem.getMixerInfo().length > 0;
        } catch (Throwable t) {
            ok = false;
            errors.add("Audio subsystem probe failed: " + t);
        }

        if (!ok) {
            errors.add("No audio output device is available; sound is disabled");
        }

        this.available = ok;
    }

    /** True if an audio output device is available. */
    public boolean isAvailable() {
        return available;
    }

    /** True if the named sound was loaded successfully. */
    public boolean isLoaded(String key) {
        return clips.containsKey(key);
    }

    /** True if the named sound is currently playing. */
    public boolean isPlaying(String key) {
        Clip clip = clips.get(key);
        return clip != null && clip.isRunning();
    }

    /** All problems encountered while loading, in order. */
    public List<String> errors() {
        return List.copyOf(errors);
    }

    /**
     * Loads a sound effect.
     *
     * @param key short name used by {@link #play(String)}
     * @param loader asset loader for the project root
     * @param relativePath project-root-relative path
     */
    public void load(String key, AssetLoader loader, String relativePath) {
        if (!available) {
            return;
        }

        Clip clip = null;

        try {
            Path file = loader.resolve(relativePath);

            try (AudioInputStream decoded =
                         AudioSystem.getAudioInputStream(file.toFile())) {

                AudioFormat base = decoded.getFormat();
                AudioFormat target = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        base.getSampleRate(),
                        16,
                        base.getChannels(),
                        base.getChannels() * 2,
                        base.getSampleRate(),
                        false);

                try (AudioInputStream converted =
                             AudioSystem.getAudioInputStream(target, decoded)) {

                    byte[] data = converted.readAllBytes();
                    int frameSize = target.getFrameSize();

                    if (frameSize <= 0 || data.length == 0) {
                        throw new IllegalStateException("no audio data");
                    }

                    long frames = data.length / frameSize;

                    try (AudioInputStream fixed = new AudioInputStream(
                            new ByteArrayInputStream(data), target, frames)) {

                        DataLine.Info info = new DataLine.Info(Clip.class, target);

                        if (!AudioSystem.isLineSupported(info)) {
                            throw new IllegalStateException(
                                    "clip line not supported");
                        }

                        clip = (Clip) AudioSystem.getLine(info);
                        clip.open(fixed);

                        clips.put(key, clip);
                        clip = null;
                    }
                }
            }
        } catch (Throwable t) {
            errors.add(key + " (" + relativePath + "): " + t);
        } finally {
            // Release a clip if loading failed after the line was acquired.
            if (clip != null) {
                try {
                    clip.close();
                } catch (Throwable t) {
                    errors.add("Failed to close clip for " + key + ": " + t);
                }
            }
        }
    }

    /**
     * Plays a loaded sound from the start.
     *
     * @param key sound key previously passed to {@link #load}
     */
    public void play(String key) {
        if (!available) {
            return;
        }

        Clip clip = clips.get(key);

        if (clip == null) {
            errors.add("play requested for unloaded sound: " + key);
            return;
        }

        if (clip.isRunning()) {
            clip.stop();
        }

        clip.setFramePosition(0);
        clip.start();
    }

    /** Releases every loaded clip. */
    public void dispose() {
        for (Clip clip : clips.values()) {
            try {
                clip.stop();
                clip.close();
            } catch (Throwable t) {
                errors.add("dispose failed: " + t);
            }
        }

        clips.clear();
    }
}
