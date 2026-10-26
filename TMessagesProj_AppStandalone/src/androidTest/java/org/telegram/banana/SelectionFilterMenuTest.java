package org.telegram.banana;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.PopupMenu;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Cells.TextSelectionHelper;
import static org.junit.Assert.*;

/** Exercises the real selection toolbar callback, without opening a server-backed chat. */
public class SelectionFilterMenuTest {
    private static final class Helper extends TextSelectionHelper.SimpleTextSelectionHelper {
        boolean allowed = true, copying = true;
        int calls;
        String text = ".*[sale]\\E", delivered;
        Helper() { super(null, null); selectionStart = 0; selectionEnd = text.length(); }
        @Override protected boolean canAddFilter() { return allowed; }
        @Override protected boolean canCopy() { return copying; }
        @Override protected CharSequence getSelectedText() { return text; }
        @Override protected void onAddFilterText(CharSequence selected) { calls++; delivered = selected.toString(); }
    }
    private static ActionMode.Callback callback(TextSelectionHelper<?> helper) {
        try {
            java.lang.reflect.Method method = TextSelectionHelper.class.getDeclaredMethod("createActionCallback");
            method.setAccessible(true); return (ActionMode.Callback) method.invoke(helper);
        } catch (Exception error) { throw new AssertionError(error); }
    }
    private static MenuItem filter(Menu menu) {
        for (int i = 0; i < menu.size(); i++) if (LocaleController.getString(R.string.BananaFilterAdd).contentEquals(menu.getItem(i).getTitle())) return menu.getItem(i);
        throw new AssertionError("Native toolbar must contain the filter action");
    }
    @Test public void addFilterClearsSelectionAndDeliversAnUnchangedSnapshotOnce() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ApplicationLoader.postInitApplication();
            Helper helper = new Helper();
            Menu menu = new PopupMenu(ApplicationLoader.applicationContext, new android.view.View(ApplicationLoader.applicationContext)).getMenu();
            ActionMode.Callback callback = callback(helper);
            callback.onCreateActionMode(null, menu); callback.onPrepareActionMode(null, menu);
            MenuItem item = filter(menu);
            assertTrue(item.isVisible());
            callback.onActionItemClicked(null, item);
            assertEquals(helper.text, helper.delivered);
            assertFalse(helper.isInSelectionMode());
            callback.onActionItemClicked(null, item);
            assertEquals(1, helper.calls);
        });
    }
    @Test public void copyRestrictionsUnavailableContextsAndOversizeSelectionsCannotInvokeTheAction() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ApplicationLoader.postInitApplication();
            for (int mode = 0; mode < 3; mode++) {
                Helper helper = new Helper();
                if (mode == 0) helper.copying = false;
                if (mode == 1) helper.allowed = false;
                if (mode == 2) helper.text = new String(new char[4096]).replace('\0', 'x');
                Menu menu = new PopupMenu(ApplicationLoader.applicationContext, new android.view.View(ApplicationLoader.applicationContext)).getMenu();
                ActionMode.Callback callback = callback(helper);
                callback.onCreateActionMode(null, menu); callback.onPrepareActionMode(null, menu);
                MenuItem item = filter(menu);
                assertFalse(item.isVisible());
                callback.onActionItemClicked(null, item);
                assertEquals(0, helper.calls);
                assertTrue(helper.isInSelectionMode());
            }
        });
    }
}
