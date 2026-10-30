package org.telegram.messenger.banana;

import java.util.regex.Pattern;

/**
 * A regex search with a time budget, for user expressions matched on the UI thread or a shared
 * queue. java.util.regex cannot be interrupted, so the text checks the deadline as it is read; an
 * expression like ^(a|aa)+$ or a long chain of .*x would otherwise backtrack for seconds.
 */
public final class BoundedMatch {
    private BoundedMatch() {}

    private static final class Timeout extends RuntimeException {
        Timeout() {
            super(null, null, false, false);
        }
    }

    private static final class DeadlineText implements CharSequence {
        private final CharSequence text;
        private final long deadline;
        private int reads;

        DeadlineText(CharSequence text, long deadline) {
            this.text = text;
            this.deadline = deadline;
        }

        @Override
        public char charAt(int index) {
            if ((++reads & 255) == 0 && System.nanoTime() > deadline) {
                throw new Timeout();
            }
            return text.charAt(index);
        }

        @Override
        public int length() {
            return text.length();
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new DeadlineText(text.subSequence(start, end), deadline);
        }

        @Override
        public String toString() {
            return text.toString();
        }
    }

    /** True or false for a finished search; null when it ran out of time or overflowed the stack. */
    public static Boolean find(Pattern pattern, CharSequence text, long budgetNanos) {
        try {
            return pattern.matcher(new DeadlineText(text, System.nanoTime() + budgetNanos)).find();
        } catch (Timeout | StackOverflowError runaway) {
            return null;
        }
    }
}
