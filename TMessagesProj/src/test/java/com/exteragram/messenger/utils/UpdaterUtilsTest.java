package com.exteragram.messenger.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UpdaterUtilsTest {
    @Test
    public void comparesReleaseCandidatesNumerically() {
        assertTrue(UpdaterUtils.compareVersions("v13.1.1-rc.2", "13.1.1-rc.1") > 0);
        assertTrue(UpdaterUtils.compareVersions("13.1.1-rc.10", "13.1.1-rc.2") > 0);
    }

    @Test
    public void stableReleaseIsNewerThanItsReleaseCandidate() {
        assertTrue(UpdaterUtils.compareVersions("13.1.1", "13.1.1-rc.10") > 0);
        assertTrue(UpdaterUtils.compareVersions("13.1.2-rc.1", "13.1.1") > 0);
    }

    @Test
    public void rejectsUnsupportedTagFormatsAsEqual() {
        assertEquals(0, UpdaterUtils.compareVersions("latest", "13.1.1"));
        assertEquals(0, UpdaterUtils.compareVersions("13.1.1-rc.1", "latest"));
    }
}
