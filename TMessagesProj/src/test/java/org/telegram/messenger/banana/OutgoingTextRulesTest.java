package org.telegram.messenger.banana;

import org.junit.Test;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class OutgoingTextRulesTest {
    private static final List<OutgoingTextRules.Rule> NONE = Collections.emptyList();

    private static <T extends TLRPC.MessageEntity> T span(T entity, int offset, int length) {
        entity.offset = offset;
        entity.length = length;
        return entity;
    }

    private static String caps(String text) {
        return OutgoingTextRules.apply(text, new ArrayList<>(), NONE, true).text;
    }

    private static String replace(String text, String rules) {
        return OutgoingTextRules.apply(text, new ArrayList<>(), OutgoingTextRules.parseRules(rules), false).text;
    }

    @Test public void rulesParseOnePerLineAndSkipBrokenLines() {
        List<OutgoingTextRules.Rule> rules = OutgoingTextRules.parseRules("spb = Saint Petersburg\nno separator\n = empty\nmsk=Moscow");
        assertEquals(2, rules.size());
        assertEquals("spb", rules.get(0).source);
        assertEquals("Saint Petersburg", rules.get(0).replacement);
        assertEquals("Moscow", rules.get(1).replacement);
        assertTrue(OutgoingTextRules.parseRules(null).isEmpty());
    }

    @Test public void replacementMatchesWholeWordsAndKeepsTheirCase() {
        assertEquals("I live in Saint Petersburg", replace("I live in spb", "spb=Saint Petersburg"));
        assertEquals("SAINT PETERSBURG!", replace("SPB!", "spb=Saint Petersburg"));
        assertEquals("Saint petersburg rocks", replace("Spb rocks", "spb=saint petersburg"));
        assertEquals("spbx stays", replace("spbx stays", "spb=Saint Petersburg"));
        assertEquals("no chaining: b", replace("no chaining: a", "a=b\nb=c"));
    }

    @Test public void longestRuleWinsAtTheSamePosition() {
        assertEquals("nope", replace("ny city", "ny=New York\nny city=nope"));
        assertEquals("New York town", replace("ny town", "ny=New York\nny city=nope"));
        assertEquals("nope", replace("ny city", "ny city=nope\nny=New York"));
    }

    @Test public void linksMentionsCommandsAndCodeAreNeverChanged() {
        String rules = "test=exam";
        assertEquals("exam https://test.example/test @test_user /test #test", replace("test https://test.example/test @test_user /test #test", rules));
        TLRPC.TL_messageEntityCode code = span(new TLRPC.TL_messageEntityCode(), 0, 4);
        OutgoingTextRules.Result result = OutgoingTextRules.apply("test test", new ArrayList<>(Arrays.asList(code)),
                OutgoingTextRules.parseRules(rules), false);
        assertEquals("test exam", result.text);
    }

    @Test public void antiCapsTurnsShoutingIntoSentences() {
        assertEquals("Hello world. How are you?", caps("HELLO WORLD. HOW ARE YOU?"));
        assertEquals("Привет! Как дела", caps("ПРИВЕТ! КАК ДЕЛА"));
        assertEquals("Hello\nNew line", caps("HELLO\nNEW LINE"));
    }

    @Test public void antiCapsLeavesNormalTextAcronymsAndCommandsAlone() {
        assertEquals("NASA launched a rocket", caps("NASA launched a rocket"));
        assertEquals("OK", caps("OK"));
        assertEquals("/START NOW PLEASE", caps("/START NOW PLEASE"));
        assertEquals("这是NASA的API文档", caps("这是NASA的API文档"));
        assertFalse(OutgoingTextRules.apply("Normal text", new ArrayList<>(), NONE, true).changed);
    }

    @Test public void antiCapsKeepsLinksAndMentionsAsTyped() {
        assertEquals("Look at https://EXAMPLE.com/ABC now @SomeUser", caps("LOOK AT https://EXAMPLE.com/ABC NOW @SomeUser"));
    }

    @Test public void formattingMovesWithTheTextAndComposerEntitiesStayUntouched() {
        TLRPC.TL_messageEntityBold bold = span(new TLRPC.TL_messageEntityBold(), 10, 5);
        ArrayList<TLRPC.MessageEntity> entities = new ArrayList<>(Arrays.asList(bold));
        OutgoingTextRules.Result result = OutgoingTextRules.apply("I live in spb, ok", entities,
                OutgoingTextRules.parseRules("live=reside\nspb=Saint Petersburg"), false);
        assertEquals("I reside in Saint Petersburg, ok", result.text);
        assertEquals(1, result.entities.size());
        TLRPC.MessageEntity moved = result.entities.get(0);
        // The bold run covered "spb, " and now covers its replacement and the same punctuation.
        assertEquals("Saint Petersburg, ", result.text.substring(moved.offset, moved.offset + moved.length));
        assertEquals(10, bold.offset);
        assertEquals(5, bold.length);
        assertNotSame(bold, moved);
    }

    @Test public void entityInsideAReplacedWordFollowsItsNewBounds() {
        TLRPC.TL_messageEntityItalic italic = span(new TLRPC.TL_messageEntityItalic(), 1, 1);
        OutgoingTextRules.Result result = OutgoingTextRules.apply("spb", new ArrayList<>(Arrays.asList(italic)),
                OutgoingTextRules.parseRules("spb=Saint Petersburg"), false);
        assertEquals("Saint Petersburg", result.text);
        assertEquals(0, result.entities.get(0).offset);
        assertEquals(16, result.entities.get(0).length);
    }

    @Test public void nothingChangesWhenRulesAreOffOrEntitiesAreBroken() {
        OutgoingTextRules.Result off = OutgoingTextRules.apply("SHOUT SHOUT", new ArrayList<>(), NONE, false);
        assertFalse(off.changed);
        assertEquals("SHOUT SHOUT", off.text);
        TLRPC.TL_messageEntityBold broken = span(new TLRPC.TL_messageEntityBold(), 5, 100);
        assertFalse(OutgoingTextRules.apply("SHOUT SHOUT", new ArrayList<>(Arrays.asList(broken)), NONE, true).changed);
        assertFalse(OutgoingTextRules.apply(null, null, NONE, true).changed);
    }

    @Test public void emojiAndSurrogatesSurvive() {
        assertEquals("Hello 😀 world", caps("HELLO 😀 WORLD"));
        assertEquals("😀 Saint Petersburg 😀", replace("😀 spb 😀", "spb=Saint Petersburg"));
    }
}
