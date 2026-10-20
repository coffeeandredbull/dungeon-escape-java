package com.dungeonescape.asset;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Loads and validates game assets relative to the project root.
 *
 * <p>Paths handed to this class are relative to the project root (the folder
 * that contains {@code assets/} and {@code audio/}), for example
 * {@code "assets/door_closed.png"} or {@code "audio/drink.wav"}. The project
 * root is located by walking up from the current working directory, so no
 * hardcoded user-specific absolute path is needed.</p>
 *
 * <p>Missing, unreadable, and unsupported assets are reported explicitly by
 * throwing {@link AssetLoadException}. Nothing is silently swallowed and no
 * asset is ever reported as loaded when it failed to load.</p>
 */
public class AssetLoader {

    private final Path projectRoot;
    private final Path assetsDir;
    private final Path audioDir;

    /**
     * Locates the project root and prepares the loader.
     *
     * @throws AssetLoadException if the project root, {@code assets/}, or
     *                            {@code audio/} cannot be found
     */
    public AssetLoader() {
        this.projectRoot = locateProjectRoot();
        this.assetsDir = projectRoot.resolve("assets");
        this.audioDir = projectRoot.resolve("audio");
        requireDirectory(assetsDir, "assets");
        requireDirectory(audioDir, "audio");
    }

    /**
     * Walks up from the current working directory until it finds a directory
     * that contains both an {@code assets/} and an {@code audio/} folder.
     */
    private static Path locateProjectRoot() {
        Path start = Paths.get("").toAbsolutePath().normalize();
        for (Path candidate = start; candidate != null; candidate = candidate.getParent()) {
            if (Files.isDirectory(candidate.resolve("assets"))
                    && Files.isDirectory(candidate.resolve("audio"))) {
                return candidate;
            }
        }
        throw new AssetLoadException(
                "Could not locate the project root. Expected a directory containing both "
                        + "'assets' and 'audio'. Searched upward from: " + start);
    }

    private static void requireDirectory(Path dir, String name) {
        if (!Files.isDirectory(dir)) {
            throw new AssetLoadException(
                    "Missing required '" + name + "' directory: " + dir);
        }
    }

    /** Returns the resolved project root directory. */
    public Path getProjectRoot() {
        return projectRoot;
    }

    /** Returns the resolved {@code assets/} directory. */
    public Path getAssetsDir() {
        return assetsDir;
    }

    /** Returns the resolved {@code audio/} directory. */
    public Path getAudioDir() {
        return audioDir;
    }

    /**
     * Resolves a project-root-relative path such as {@code "assets/key.png"}.
     */
    public Path resolve(String relativePath) {
        return projectRoot.resolve(relativePath.replace('\\', '/')).normalize();
    }

    /**
     * Loads an image from a project-root-relative path such as
     * {@code "assets/key.png"}.
     *
     * @throws AssetLoadException if the file is missing, unreadable, or not a
     *                            supported/decodable image
     */
    public BufferedImage loadImage(String relativePath) {
        Path file = resolve(relativePath);
        requireReadable(file, "image");

        BufferedImage image;
        try {
            image = ImageIO.read(file.toFile());
        } catch (IOException e) {
            throw new AssetLoadException(
                    "Failed to read image: " + file + " (" + e.getMessage() + ")", e);
        }
        if (image == null) {
            throw new AssetLoadException(
                    "Unsupported or corrupt image format: " + file);
        }
        return image;
    }

    /**
     * Decodes an audio file from a project-root-relative path such as
     * {@code "audio/drink.wav"} and returns its format metadata.
     *
     * <p>This proves the file exists and that Java Sound can decode its
     * container and format. It does <em>not</em> open an audio line, so it does
     * not claim that playback works.</p>
     *
     * @throws AssetLoadException if the file is missing, unreadable, or not a
     *                            supported/decodable audio format
     */
    public AudioInfo loadAudio(String relativePath) {
        Path file = resolve(relativePath);
        requireReadable(file, "audio");

        try (AudioInputStream stream = AudioSystem.getAudioInputStream(file.toFile())) {
            AudioFormat format = stream.getFormat();
            long frames = stream.getFrameLength();
            long bytes = Files.size(file);
            return new AudioInfo(relativePath, file, format, frames, bytes);
        } catch (UnsupportedAudioFileException e) {
            throw new AssetLoadException("Unsupported audio format: " + file, e);
        } catch (IOException e) {
            throw new AssetLoadException(
                    "Failed to read audio: " + file + " (" + e.getMessage() + ")", e);
        }
    }

    /** Validates existence, regular-file status, and readability. */
    private static void requireReadable(Path file, String kind) {
        if (!Files.exists(file)) {
            throw new AssetLoadException("Missing " + kind + " asset: " + file);
        }
        if (!Files.isRegularFile(file)) {
            throw new AssetLoadException("Not a regular file (" + kind + " asset): " + file);
        }
        if (!Files.isReadable(file)) {
            throw new AssetLoadException(
                    "Unreadable " + kind + " asset (permission denied): " + file);
        }
    }
}
