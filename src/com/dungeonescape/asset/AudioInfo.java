package com.dungeonescape.asset;

import javax.sound.sampled.AudioFormat;
import java.nio.file.Path;

/**
 * Metadata for an audio file that was successfully decoded by Java Sound.
 *
 * <p>This records that the container and format could be read. It is
 * deliberately <em>not</em> a claim that the sound can be played back on the
 * current machine.</p>
 *
 * @param relativePath path relative to the project root, e.g. {@code "audio/drink.wav"}
 * @param file         resolved absolute file
 * @param format       decoded audio format
 * @param frameLength  number of audio frames, or {@code -1} if unspecified
 * @param byteLength   file size in bytes
 */
public record AudioInfo(
        String relativePath,
        Path file,
        AudioFormat format,
        long frameLength,
        long byteLength) {
}
