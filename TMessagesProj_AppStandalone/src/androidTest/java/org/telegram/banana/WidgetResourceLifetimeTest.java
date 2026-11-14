package org.telegram.banana;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.*;

/** Exercises service ownership without starting a native engine or using an account. */
public class WidgetResourceLifetimeTest {
    @Test
    public void warmupExpiresAndDoesNotUnbindARealSession() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                Object engine = engine();
                BindingContext context = new BindingContext();
                invoke(engine, "prewarm", new Class<?>[]{Context.class}, context);
                invoke(engine, "prewarm", new Class<?>[]{Context.class}, context);
                assertEquals(1, context.binds);
                Object binding = field(engine, "connection");
                assertEquals(1, field(binding, "live"));
                Handler main = (Handler) field(engine, "main");
                Runnable expiry = (Runnable) field(binding, "endWarmup");
                assertTrue("Warmup must have a bounded lifetime", main.hasCallbacks(expiry));
                expiry.run();
                assertEquals(1, context.unbinds);
                assertNull(field(engine, "connection"));
                assertFalse(main.hasCallbacks(expiry));

                invoke(engine, "prewarm", new Class<?>[]{Context.class}, context);
                binding = field(engine, "connection");
                // One actual session holds the service in addition to the independent warmup hold.
                set(binding, "live", 2);
                invoke(engine, "releasePrewarm", new Class<?>[0]);
                assertEquals(1, field(binding, "live"));
                assertEquals(1, context.unbinds);
                invoke(engine, "releasePrewarm", new Class<?>[0]);
                assertEquals(1, field(binding, "live"));
                invoke(engine, "released", new Class<?>[]{binding.getClass()}, binding);
                ((Runnable) field(binding, "idleUnbind")).run();
                assertEquals(2, context.unbinds);
                assertNull(field(engine, "connection"));
            } catch (Exception error) {
                throw new AssertionError(error);
            }
        });
    }

    @Test
    public void completedOrRejectedLargeArgumentsDoNotKeepTheirPeakBuffer() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                Object engine = engine();
                Class<?> bindingType = Class.forName(engine.getClass().getName() + "$Connection");
                Constructor<?> bindingConstructor = bindingType.getDeclaredConstructor(engine.getClass());
                bindingConstructor.setAccessible(true);
                Object binding = bindingConstructor.newInstance(engine);
                Class<?> clientType = Class.forName(engine.getClass().getName() + "$Client");
                Constructor<?> constructor = clientType.getDeclaredConstructor(engine.getClass(), bindingType,
                        int.class, Class.forName("org.telegram.utils.twidget.WidgetSandboxBackend"));
                constructor.setAccessible(true);
                Object client = constructor.newInstance(engine, binding, 0, null);
                byte[] part = new byte[128 * 1024];
                Arrays.fill(part, (byte) 42);
                invoke(client, "part", new Class<?>[]{byte[].class}, part);
                Object peakBuffer = field(client, "large");
                assertArrayEquals(part, (byte[]) invoke(client, "large", new Class<?>[]{byte[].class}, (Object) null));
                assertNotSame("A completed large frame must release the client's peak scratch allocation",
                        peakBuffer, field(client, "large"));
                assertEquals(0, ((java.io.ByteArrayOutputStream) field(client, "large")).size());
                invoke(client, "part", new Class<?>[]{byte[].class}, part);
                peakBuffer = field(client, "large");
                try {
                    invoke(client, "part", new Class<?>[]{byte[].class}, (Object) null);
                    fail("A null chunk must be rejected");
                } catch (java.lang.reflect.InvocationTargetException expected) {
                    assertTrue(expected.getCause() instanceof IllegalStateException);
                }
                assertNotSame(peakBuffer, field(client, "large"));
                assertArrayEquals(new byte[0], (byte[]) invoke(client, "large", new Class<?>[]{byte[].class}, (Object) null));
            } catch (Exception error) {
                throw new AssertionError(error);
            }
        });
    }

    private static Object engine() throws Exception {
        Constructor<?> constructor = Class.forName("org.telegram.utils.twidget.WidgetRemoteEngine").getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class BindingContext extends ContextWrapper {
        int binds, unbinds;
        BindingContext() { super(InstrumentationRegistry.getInstrumentation().getTargetContext()); }
        @Override public Context getApplicationContext() { return this; }
        @Override public boolean bindService(Intent intent, ServiceConnection connection, int flags) { binds++; return true; }
        @Override public void unbindService(ServiceConnection connection) { unbinds++; }
    }
}
