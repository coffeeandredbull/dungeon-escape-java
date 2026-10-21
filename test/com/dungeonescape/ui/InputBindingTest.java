package com.dungeonescape.ui;

import com.dungeonescape.asset.AssetLoader;
import com.dungeonescape.game.GameSession;
import com.dungeonescape.game.Levels;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Dependency-free tests for the keyboard bindings on {@link DungeonPanel}.
 *
 * <p>The bindings use {@code WHEN_IN_FOCUSED_WINDOW}, which only fires inside a
 * focused window. This test therefore verifies the wiring headlessly: every key
 * has a press/release binding whose action updates the merged input state.</p>
 *
 * <p>Run with: {@code java -cp out;test_out com.dungeonescape.ui.InputBindingTest}
 * Exit code 0 means every assertion passed.</p>
 */
public class InputBindingTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        DungeonPanel panel = new DungeonPanel(new AssetLoader(), new HudPanel(),
                new GameSession(Levels.create(), key -> {
                }));
        InputMap inputMap = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = panel.getActionMap();

        assertTrue("16 press/release bindings registered", inputMap.size() == 16);

        press(inputMap, actionMap, KeyEvent.VK_D);
        assertTrue("D sets right", panel.inputState().right());
        release(inputMap, actionMap, KeyEvent.VK_D);
        assertFalse("D release clears right", panel.inputState().right());

        press(inputMap, actionMap, KeyEvent.VK_A);
        assertTrue("A sets left", panel.inputState().left());
        release(inputMap, actionMap, KeyEvent.VK_A);

        press(inputMap, actionMap, KeyEvent.VK_UP);
        assertTrue("Up arrow sets up", panel.inputState().up());
        release(inputMap, actionMap, KeyEvent.VK_UP);

        // WASD and arrows merge: W and Up both held is still just "up".
        press(inputMap, actionMap, KeyEvent.VK_W);
        press(inputMap, actionMap, KeyEvent.VK_UP);
        assertTrue("W+Up sets up", panel.inputState().up());
        assertFalse("W+Up does not set down", panel.inputState().down());
        release(inputMap, actionMap, KeyEvent.VK_W);
        release(inputMap, actionMap, KeyEvent.VK_UP);
        assertFalse("release clears up", panel.inputState().up());

        press(inputMap, actionMap, KeyEvent.VK_S);
        press(inputMap, actionMap, KeyEvent.VK_LEFT);
        assertTrue("S sets down", panel.inputState().down());
        assertTrue("Left sets left", panel.inputState().left());
        panel.clearInput();
        assertFalse("clear drops down", panel.inputState().down());
        assertFalse("clear drops left", panel.inputState().left());

        System.out.println();
        System.out.println("Passed: " + passed + ", Failed: " + failed);
        if (failed > 0) {
            System.out.println("TESTS FAILED");
            System.exit(1);
        }
        System.out.println("All tests passed.");
        System.exit(0);
    }

    private static void press(InputMap inputMap, ActionMap actionMap, int code) {
        invoke(inputMap, actionMap, code, false);
    }

    private static void release(InputMap inputMap, ActionMap actionMap, int code) {
        invoke(inputMap, actionMap, code, true);
    }

    private static void invoke(InputMap inputMap, ActionMap actionMap, int code, boolean onRelease) {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(code, 0, onRelease);
        Object name = inputMap.get(keyStroke);
        assertTrue("binding exists for " + keyStroke, name != null);
        Action action = actionMap.get(name);
        assertTrue("action exists for " + name, action != null);
        action.actionPerformed(new ActionEvent(new Object(), ActionEvent.ACTION_PERFORMED, "test"));
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

    private static void assertFalse(String message, boolean condition) {
        assertTrue(message, !condition);
    }
}
