package org.telegram.banana;

import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.telegram.messenger.utils.OnPostDrawView;
import java.util.ArrayList;
import java.util.Arrays;
import static org.junit.Assert.*;

public class GlassRedrawTest {
    @Test public void redrawRequestedDuringCaptureSurvivesForNextFrame() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ArrayList<Integer> flags = new ArrayList<>();
            OnPostDrawView[] view = new OnPostDrawView[1];
            view[0] = new OnPostDrawView(InstrumentationRegistry.getInstrumentation().getTargetContext(), true, value -> {
                flags.add(value);
                if (flags.size() == 1) view[0].invalidate(2);
            });
            view[0].invalidate(1);
            view[0].onPreDraw();
            view[0].onPreDraw();
            view[0].onPreDraw();
            assertEquals(Arrays.asList(1, 2), flags);
        });
    }
}
