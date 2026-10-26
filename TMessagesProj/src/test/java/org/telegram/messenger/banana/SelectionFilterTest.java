package org.telegram.messenger.banana;

import java.util.regex.Pattern;
import org.junit.Test;
import static org.junit.Assert.*;

public class SelectionFilterTest {
    @Test public void selectedRegexSyntaxAndQuoteTerminatorsStayLiteral() {
        for (String selected : new String[]{".*", "[offer](sale)", "\\E.*\\Q", "Привет 👋", "line\nnext", "(a+)+$"}) {
            Pattern pattern = Pattern.compile(SelectionFilter.expression(selected));
            assertTrue(pattern.matcher("before " + selected + " after").find());
            assertFalse(pattern.matcher("unrelated text").find());
        }
    }
    @Test public void quotingExpansionIsIncludedInTheExpressionBudget() {
        assertNull(SelectionFilter.expression(null));
        assertNull(SelectionFilter.expression(""));
        String plain = new String(new char[RegexProtocol.MAX_PATTERN - 4]).replace('\0', 'x');
        assertEquals(RegexProtocol.MAX_PATTERN, SelectionFilter.expression(plain).length());
        assertNull(SelectionFilter.expression(plain + "x"));
        assertNull(SelectionFilter.expression(new String(new char[1200]).replace("\0", "\\E")));
    }
    @Test public void aDraftSnapshotsMutableSelectionText() {
        StringBuilder text = new StringBuilder("literal");
        String expression = SelectionFilter.expression(text);
        text.replace(0, text.length(), ".*");
        assertTrue(Pattern.compile(expression).matcher("literal").matches());
        assertFalse(Pattern.compile(expression).matcher("other").matches());
    }
}
