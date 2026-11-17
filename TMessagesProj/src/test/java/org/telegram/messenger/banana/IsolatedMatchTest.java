package org.telegram.messenger.banana;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class IsolatedMatchTest {
    @Test
    public void finishedSearchesReportTheirResult() {
        assertEquals(IsolatedMatch.Outcome.MATCHED, IsolatedMatch.classify(RegexProtocol.OK, true));
        assertEquals(IsolatedMatch.Outcome.NOT_MATCHED, IsolatedMatch.classify(RegexProtocol.OK, false));
    }

    @Test
    public void runawayAndBrokenExpressionsAreQuarantined() {
        assertEquals(IsolatedMatch.Outcome.QUARANTINE, IsolatedMatch.classify(RegexProtocol.TIMEOUT, false));
        assertEquals(IsolatedMatch.Outcome.QUARANTINE, IsolatedMatch.classify(RegexProtocol.BAD_PATTERN, false));
    }

    @Test
    public void transientWorkerFailuresDoNotDisableARule() {
        // A bind timeout or a busy worker says nothing about the expression: callers fail open
        // for this message and try the rule again later.
        for (int status : new int[]{RegexProtocol.UNAVAILABLE, RegexProtocol.BUSY,
                RegexProtocol.INPUT_LIMIT, RegexProtocol.EVALUATION_ERROR}) {
            assertEquals(IsolatedMatch.Outcome.UNKNOWN, IsolatedMatch.classify(status, true));
        }
    }

    @Test
    public void quarantineIsPerExpressionAndFlags() {
        IsolatedMatch.clearQuarantine();
        assertNotEquals(IsolatedMatch.key("(a|aa)+$", 0), IsolatedMatch.key("(a|aa)+$", 2));
        assertFalse(IsolatedMatch.isQuarantined("(a|aa)+$", 0));
    }
}
