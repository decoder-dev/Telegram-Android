package org.telegram.messenger.banana;

import org.junit.Test;

import static org.junit.Assert.*;

public class AdminTitlesTest {
    @Test public void blankLookingTitlesAreInvisible() {
        String[] hidden = {" ", "   ", "​", "​‌‍", "⠀", "ㅤ", "ᅟᅠ",
                "﻿", " ", "\t\n", "⁠", "️", "　", new String(Character.toChars(0xE0020))};
        for (String title : hidden) assertTrue(title.codePointAt(0) + "", AdminTitles.isInvisible(title));
    }

    @Test public void realTitlesStayVisible() {
        String[] visible = {"Admin", "мод", " a ", "​x", "😀", ".", "★", "⠀⠁"};
        for (String title : visible) assertFalse(title, AdminTitles.isInvisible(title));
    }

    @Test public void emptyAndMissingTitlesAreLeftToTelegram() {
        assertFalse(AdminTitles.isInvisible(""));
        assertFalse(AdminTitles.isInvisible(null));
    }
}
