package com.dungeonescape.asset;

import javax.sound.sampled.AudioFormat;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Reports which assets were found and whether each one actually loaded.
 *
 * <p>This is a read-only diagnostic: it never plays audio and never modifies
 * asset files. Every file is either reported as loaded/decoded or as failed
 * with the exact reason, so the output can be trusted at startup.</p>
 */
public final class AssetDiagnostic {

    private AssetDiagnostic() {
    }

    /**
     * Scans {@code assets/} and {@code audio/} and prints a per-file status
     * report followed by a summary.
     *
     * @param loader the loader used to open each asset
     * @param out    destination for the report
     */
    public static void run(AssetLoader loader, PrintStream out) {
        out.println("=== DungeonEscapeV2 asset diagnostics ===");
        out.println("Project root : " + loader.getProjectRoot());
        out.println("Assets dir   : " + loader.getAssetsDir());
        out.println("Audio dir    : " + loader.getAudioDir());
        out.println();

        int[] imageStats = scanImages(loader, out);
        out.println();
        int[] audioStats = scanAudio(loader, out);

        out.println();
        out.println("Summary: images found=" + imageStats[0] + ", loaded=" + imageStats[1]
                + ", failed=" + imageStats[2]
                + " | audio found=" + audioStats[0] + ", decoded=" + audioStats[1]
                + ", failed=" + audioStats[2]);
    }

    /** Returns a 3-element array: found, loaded, failed. */
    private static int[] scanImages(AssetLoader loader, PrintStream out) {
        out.println("[images] scanning " + loader.getAssetsDir());
        int[] stats = new int[3];

        List<Path> files;
        try (var stream = Files.list(loader.getAssetsDir())) {
            files = stream.filter(Files::isRegularFile).sorted().toList();
        } catch (IOException e) {
            out.println("  [FAIL] could not list assets directory: " + e.getMessage());
            return stats;
        }

        for (Path file : files) {
            String name = file.getFileName().toString();
            stats[0]++;
            String rel = "assets/" + name;
            try {
                BufferedImage image = loader.loadImage(rel);
                out.printf("  [OK]   %-34s -> loaded %dx%d%n",
                        name, image.getWidth(), image.getHeight());
                stats[1]++;
            } catch (AssetLoadException e) {
                out.println("  [FAIL] " + name + " -> " + e.getMessage());
                stats[2]++;
            }
        }
        return stats;
    }

    /** Returns a 3-element array: found, decoded, failed. */
    private static int[] scanAudio(AssetLoader loader, PrintStream out) {
        out.println("[audio] scanning " + loader.getAudioDir());
        int[] stats = new int[3];

        List<Path> files;
        try (var stream = Files.list(loader.getAudioDir())) {
            files = stream.filter(Files::isRegularFile).sorted().toList();
        } catch (IOException e) {
            out.println("  [FAIL] could not list audio directory: " + e.getMessage());
            return stats;
        }

        for (Path file : files) {
            String name = file.getFileName().toString();
            stats[0]++;
            String rel = "audio/" + name;
            try {
                AudioInfo info = loader.loadAudio(rel);
                out.printf("  [OK]   %-34s -> decoded %s%n", name, describe(info));
                stats[1]++;
            } catch (AssetLoadException e) {
                out.println("  [FAIL] " + name + " -> " + e.getMessage());
                stats[2]++;
            }
        }
        out.println("  (audio decoded only; playback is NOT verified and no sound is played)");
        return stats;
    }

    private static String describe(AudioInfo info) {
        AudioFormat f = info.format();
        StringBuilder sb = new StringBuilder();
        sb.append(f.getEncoding()).append(' ')
                .append(f.getSampleRate()).append(" Hz, ")
                .append(f.getSampleSizeInBits()).append("-bit, ")
                .append(f.getChannels() == 1 ? "mono" : f.getChannels() + " channels");
        if (info.frameLength() >= 0 && f.getFrameRate() > 0) {
            double seconds = info.frameLength() / f.getFrameRate();
            sb.append(", ").append(info.frameLength()).append(" frames (~")
                    .append(String.format("%.2f", seconds)).append(" s)");
        } else {
            sb.append(", ").append(info.frameLength()).append(" frames");
        }
        return sb.toString();
    }
}
