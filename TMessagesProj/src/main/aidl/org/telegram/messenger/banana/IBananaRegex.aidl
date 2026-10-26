package org.telegram.messenger.banana;

interface IBananaRegex {
    int getWorkerUid();
    int[] match(String expression, int flags, in String[] inputs);
    oneway void abort();
}
