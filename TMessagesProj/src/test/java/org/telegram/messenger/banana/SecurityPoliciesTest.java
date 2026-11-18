package org.telegram.messenger.banana;

import org.junit.Test;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

public class SecurityPoliciesTest {
    @Test public void loginCodesAreFoundInTelegramsMessage() {
        assertEquals(Collections.singleton("12345"), LoginCodeGuard.codesIn("Login code: 12345. Do not give this code to anyone"));
        assertEquals(Collections.singleton("654321"), LoginCodeGuard.codesIn("Код для входа в Telegram: 654321"));
        assertTrue(LoginCodeGuard.codesIn("1234567 and 1234").isEmpty());
        assertEquals(2, LoginCodeGuard.codesIn("12345 or 67890").size());
        assertTrue(LoginCodeGuard.codesIn(null).isEmpty());
    }

    @Test public void spacedOutCodesAreStillCaught() {
        Set<String> codes = Collections.singleton("12345");
        assertTrue(LoginCodeGuard.containsCode("my code 1 2-3 4 5", codes));
        assertTrue(LoginCodeGuard.containsCode("12345", codes));
        assertFalse(LoginCodeGuard.containsCode("hello 1234", codes));
        assertFalse(LoginCodeGuard.containsCode("hello", Collections.<String>emptySet()));
        assertFalse(LoginCodeGuard.mayHoldCode("12 34"));
        assertTrue(LoginCodeGuard.mayHoldCode("123-45"));
        assertTrue(LoginCodeGuard.isFresh(1000, 1899));
        assertFalse(LoginCodeGuard.isFresh(1000, 1900));
    }

    @Test public void websitesAreToldApartFromTelegramLinks() {
        assertTrue(LinkKinds.isWebsiteLink("https://example.com"));
        assertTrue(LinkKinds.isWebsiteLink("example.com/x"));
        assertTrue(LinkKinds.isWebsiteLink("https://t.me.evil.com/login"));
        for (String telegram : new String[]{"https://t.me/x", "https://user.t.me", "tg://resolve?domain=x", "https://telegram.dog./a", "t.me/x"}) {
            assertFalse(telegram, LinkKinds.isWebsiteLink(telegram));
        }
        assertFalse(LinkKinds.isWebsiteLink("mailto:a@b.com"));
        assertFalse(LinkKinds.isWebsiteLink("hello"));
        assertFalse(LinkKinds.isWebsiteLink(null));
    }

    @Test public void fakeSupportNamesAreFlaggedUnlessVerified() {
        assertTrue(FakeSupport.looksFake(5, false, "Служба безопасности", null, null));
        assertFalse(FakeSupport.looksFake(5, true, "Telegram Support", null, null));
        assertFalse(FakeSupport.looksFake(777000, false, "Telegram", null, null));
        assertTrue(FakeSupport.looksFake(5, false, null, null, "tg_support_bot"));
        assertTrue(FakeSupport.looksFake(5, false, "SECURITY", "TEAM", null));
        assertFalse(FakeSupport.looksFake(5, false, "Alice", null, null));
    }

    @Test public void threeShakesInFiveSecondsLock() {
        ShakeCounter counter = new ShakeCounter();
        assertFalse(counter.note(0));
        assertFalse(counter.note(1000));
        assertTrue(counter.note(2000));
        assertFalse(counter.note(2500));
        assertFalse(counter.note(3000));
        ShakeCounter slow = new ShakeCounter();
        assertFalse(slow.note(0));
        assertFalse(slow.note(3000));
        assertFalse(slow.note(6000));
        slow.reset();
        assertFalse(slow.note(7000));
    }

    @Test public void duressVerifierRoundTripsAndRejectsTampering() {
        SecureRandom random = new SecureRandom();
        String verifier = DuressVerifier.make("4321", random);
        assertTrue(DuressVerifier.matches("4321", verifier));
        assertFalse(DuressVerifier.matches("4322", verifier));
        assertNotEquals(verifier, DuressVerifier.make("4321", random));
        String[] parts = verifier.split("\\$");
        assertFalse(DuressVerifier.matches("4321", "sha$" + parts[1] + "$" + parts[2] + "$" + parts[3]));
        assertFalse(DuressVerifier.matches("4321", "pbkdf2$5$" + parts[2] + "$" + parts[3]));
        assertFalse(DuressVerifier.matches("4321", "pbkdf2$" + parts[1] + "$!!!$" + parts[3]));
        assertFalse(DuressVerifier.matches("4321", null));
        assertNull(DuressVerifier.make("", random));
    }

    @Test public void duressRules() {
        assertEquals(0, DuressVerifier.clampAction(-1));
        assertEquals(0, DuressVerifier.clampAction(3));
        assertEquals(1, DuressVerifier.clampAction(1));
        assertArrayEquals(new long[]{1, 2}, DuressVerifier.parseIds("1,,x,2,2,-5"));
        assertEquals("1,2", DuressVerifier.formatIds(new long[]{1, 0, 2}));
        long[] slots = {5, 0, 7};
        assertArrayEquals(new int[0], DuressVerifier.slotsToLogOut(0, new long[]{5}, slots));
        assertArrayEquals(new int[]{0}, DuressVerifier.slotsToLogOut(1, new long[]{5}, slots));
        assertArrayEquals(new int[]{0, 2}, DuressVerifier.slotsToLogOut(2, null, slots));
        assertTrue(DuressVerifier.fitsType("0042", true));
        assertFalse(DuressVerifier.fitsType("42a1", true));
        assertFalse(DuressVerifier.fitsType("12345", true));
        assertTrue(DuressVerifier.fitsType("secret", false));
        assertFalse(DuressVerifier.fitsType("", false));
    }
}
