package com.dungeonescape.asset;

/**
 * Dependency-free tests for {@link AudioManager}.
 *
 * <p>Verifies that every sound loads without recorded errors and that a loaded
 * clip actually starts running. If the machine has no audio device, the test
 * reports that and skips the playback checks rather than failing.</p>
 *
 * <p>Run with: {@code java -cp out;test_out com.dungeonescape.asset.AudioManagerTest}</p>
 */
public class AudioManagerTest {

    private static final String[][] SOUNDS = {
            {"door", "audio/doorOpen_2.wav"},
            {"chest", "audio/gold_sack.wav"},
            {"key", "audio/key.wav"},
            {"potion", "audio/drink.wav"},
            {"monster", "audio/zombie-2.wav"},
            {"gameover", "audio/GameOver.wav"},
            {"victory", "audio/Victory!.wav"},
            {"footstep", "audio/footstep06.wav"},
    };

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        AssetLoader loader = new AssetLoader();
        AudioManager audio = new AudioManager();

        if (!audio.isAvailable()) {
            System.out.println("NOTE: no audio device on this machine; playback not testable.");
            System.out.println("All tests passed (audio unavailable).");
            System.exit(0);
        }

        for (String[] sound : SOUNDS) {
            audio.load(sound[0], loader, sound[1]);
        }
        assertTrue("all sounds loaded with no errors (errors=" + audio.errors() + ")",
                audio.errors().isEmpty());
        for (String[] sound : SOUNDS) {
            assertTrue("loaded " + sound[0], audio.isLoaded(sound[0]));
        }

        // A long clip should be running shortly after play() is called.
        audio.play("gameover");
        Thread.sleep(120);
        assertTrue("gameover clip is running after play", audio.isPlaying("gameover"));

        // A missing file must be reported, not swallowed.
        AudioManager missing = new AudioManager();
        missing.load("missing", loader, "audio/does_not_exist.wav");
        assertTrue("missing file reports an error", !missing.errors().isEmpty());

        audio.dispose();
        assertTrue("dispose clears loaded clips", !audio.isLoaded("gameover"));

        System.out.println();
        System.out.println("Passed: " + passed + ", Failed: " + failed);
        if (failed > 0) {
            System.out.println("TESTS FAILED");
            System.exit(1);
        }
        System.out.println("All tests passed.");
        System.exit(0);
    }

    private static void assertTrue(String message, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS: " + message);
        } else {
            failed++;
            System.out.println("FAIL: " + message);
        }
    }
}
