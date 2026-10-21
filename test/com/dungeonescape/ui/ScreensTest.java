package com.dungeonescape.ui;

import javax.swing.JButton;
import java.awt.Component;
import java.awt.Container;

/**
 * Dependency-free tests for the title and result screens.
 *
 * <p>Run with: {@code java -cp out;test_out com.dungeonescape.ui.ScreensTest}</p>
 */
public class ScreensTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testTitleStartButton();
        testResultConfigureAndPlayAgain();

        System.out.println();
        System.out.println("Passed: " + passed + ", Failed: " + failed);
        if (failed > 0) {
            System.out.println("TESTS FAILED");
            System.exit(1);
        }
        System.out.println("All tests passed.");
        System.exit(0);
    }

    private static void testTitleStartButton() {
        boolean[] started = {false};
        TitlePanel panel = new TitlePanel(() -> started[0] = true);
        JButton button = findButton(panel);
        assertTrue("title has a start button", button != null);
        button.doClick();
        assertTrue("start button fires the callback", started[0]);
    }

    private static void testResultConfigureAndPlayAgain() {
        boolean[] restarted = {false};
        ResultPanel panel = new ResultPanel(() -> restarted[0] = true);
        panel.configure("GAME OVER", "The monster caught you.", true);
        JButton button = findButton(panel);
        assertTrue("result has a play-again button", button != null);
        button.doClick();
        assertTrue("play again fires the callback", restarted[0]);
    }

    private static JButton findButton(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton button) {
                return button;
            }
            if (component instanceof Container child) {
                JButton found = findButton(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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
