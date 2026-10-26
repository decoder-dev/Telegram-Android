package org.telegram.messenger.banana;

public final class RegexProtocol {
    public static final int OK = 0, BAD_PATTERN = 1, INPUT_LIMIT = 2, EVALUATION_ERROR = 3,
            UNAVAILABLE = 4, TIMEOUT = 5, BUSY = 6;
    public static final int MAX_PATTERN = 4096, MAX_INPUT = 262144, MAX_BATCH = 32;
    private RegexProtocol() {}

    public static boolean valid(String expression, String[] inputs) {
        if (expression == null || expression.length() > MAX_PATTERN || inputs == null || inputs.length > MAX_BATCH) return false;
        long total = 0;
        for (String input : inputs) {
            if (input == null || input.length() > MAX_INPUT) return false;
            total += input.length();
        }
        return total <= MAX_INPUT;
    }
}
