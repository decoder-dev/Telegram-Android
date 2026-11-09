package org.telegram.messenger.banana;

/** Automatic proxy rules that need no Android: which candidates to dial next, and when a route keeps falling off. */
public final class ProxyRoutePolicy {
    private ProxyRoutePolicy() {}

    /** Indices of the next {@code limit} candidates in a pool of {@code size}, from {@code offset}, wrapping around. */
    public static int[] window(int size, int offset, int limit) {
        if (size <= 0 || limit <= 0) {
            return new int[0];
        }
        int[] indices = new int[Math.min(size, limit)];
        int start = Math.floorMod(offset, size);
        for (int i = 0; i < indices.length; i++) {
            indices[i] = (start + i) % size;
        }
        return indices;
    }

    public static int nextOffset(int size, int offset, int limit) {
        return size <= 0 ? 0 : Math.floorMod(offset + Math.max(0, limit), size);
    }

    /**
     * Drops of a route that had connected. A route that connects, stalls and reconnects over and
     * over still answers a probe, so it is judged by how often it falls off instead.
     */
    public static final class Drops {
        private final long[] times;
        private final long spanMs;
        private int count;

        public Drops(int limit, long spanMs) {
            this.times = new long[Math.max(1, limit)];
            this.spanMs = spanMs;
        }

        /** Records a drop; true once the last {@code limit} drops fit into the span. Starts over after that. */
        public boolean record(long now) {
            if (count == times.length) {
                System.arraycopy(times, 1, times, 0, count - 1);
                count--;
            }
            times[count++] = now;
            if (count < times.length || now - times[0] > spanMs) {
                return false;
            }
            count = 0;
            return true;
        }

        public void reset() {
            count = 0;
        }
    }
}
