package org.telegram.messenger.banana;

/** A window preference, never a change to the device's global display settings. */
public final class RefreshRatePolicy {
    public static final String KEY = "bananaDisplayRefreshRate";
    public static final float MAXIMUM = -1f, SYSTEM = 0f;
    private RefreshRatePolicy() { }
    public static float resolve(float chosen, float maximum) {
        if (!Float.isFinite(maximum) || maximum <= 0) return 0;
        if (!Float.isFinite(chosen)) return 0;
        if (chosen < 0) return maximum;
        return Math.min(chosen, maximum);
    }
}
