package org.telegram.messenger.banana;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;

/**
 * Shake to Lock (ported from BananaGram iOS): three shakes within five seconds lock the app and close
 * the locked Archive. Listens only while the app is in the foreground and the switch is on.
 */
public final class BananaPanicShake implements SensorEventListener {
    private static final float THRESHOLD_G = 2.7f;
    private static final long DEBOUNCE_MS = 500;

    private final ShakeCounter counter = new ShakeCounter();
    private final Runnable onPanic;
    private SensorManager sensorManager;
    private boolean registered;
    private long lastShake;

    public BananaPanicShake(Runnable onPanic) {
        this.onPanic = onPanic;
    }

    public void resume(Context context) {
        if (registered || context == null) return;
        if (!BananaConfig.get(UserConfig.selectedAccount, BananaConfig.Option.PANIC_SHAKE_LOCK)) return;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) return;
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer == null) return;
        registered = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    public void pause() {
        if (registered && sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        registered = false;
        counter.reset();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.values == null || event.values.length < 3) return;
        float x = event.values[0], y = event.values[1], z = event.values[2];
        double g = Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;
        if (g < THRESHOLD_G) return;
        long now = SystemClock.elapsedRealtime();
        if (now - lastShake < DEBOUNCE_MS) return;
        lastShake = now;
        if (counter.note(now)) {
            AndroidUtilities.runOnUIThread(onPanic);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
