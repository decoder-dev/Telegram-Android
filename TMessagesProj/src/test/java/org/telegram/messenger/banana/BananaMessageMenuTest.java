package org.telegram.messenger.banana;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

public class BananaMessageMenuTest {
    @Test public void orderedItemsMoveAndUnkeyedOnesKeepTheirSlot() {
        List<String> keys = Arrays.asList("reply", null, "copy", "delete");
        assertArrayEquals(new int[]{3, 1, 2, 0},
                BananaMessageMenu.arrange(keys, Collections.<String>emptySet(), Arrays.asList("delete", "copy")));
    }

    @Test public void hiddenItemsAreRemoved() {
        List<String> keys = Arrays.asList("reply", "copy", null, "delete");
        assertArrayEquals(new int[]{0, 2, 3}, BananaMessageMenu.arrange(keys, new HashSet<>(Collections.singletonList("copy")), null));
    }

    @Test public void emptySettingsKeepTheMenuAsIs() {
        List<String> keys = Arrays.asList("reply", null, "forward", "delete");
        assertArrayEquals(new int[]{0, 1, 2, 3}, BananaMessageMenu.arrange(keys, null, null));
    }

    @Test public void duplicateKeysKeepTheirRelativeOrder() {
        List<String> keys = Arrays.asList("saveMedia", "copy", "saveMedia");
        assertArrayEquals(new int[]{1, 0, 2}, BananaMessageMenu.arrange(keys, null, Arrays.asList("copy")));
    }

    @Test public void unknownStoredKeysAreIgnoredAndMissingOnesAppended() {
        assertEquals(Arrays.asList("copy", "delete"), BananaMessageMenu.parse("copy, nope ,delete,copy"));
        List<String> order = BananaMessageMenu.order(Arrays.asList("delete"));
        assertEquals("delete", order.get(0));
        assertEquals(BananaMessageMenu.KEYS.size(), order.size());
        assertEquals("reply", order.get(1));
        assertEquals("copy,delete", BananaMessageMenu.format(Arrays.asList("copy", "x", "delete")));
    }
}
