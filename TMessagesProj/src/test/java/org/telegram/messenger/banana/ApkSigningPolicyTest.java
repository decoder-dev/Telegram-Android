package org.telegram.messenger.banana;
import org.junit.Test;
import static org.junit.Assert.*;
public class ApkSigningPolicyTest {
 @Test public void requiresTheInstalledSignerSet() {
  assertTrue(ApkSigningPolicy.sameSigners(new byte[][]{{1,2}},new byte[][]{{1,2}}));
  assertTrue(ApkSigningPolicy.sameSigners(new byte[][]{{1},{2}},new byte[][]{{2},{1}}));
  assertFalse(ApkSigningPolicy.sameSigners(new byte[][]{{1}},new byte[][]{{2}}));
  assertFalse(ApkSigningPolicy.sameSigners(new byte[][]{{1},{2}},new byte[][]{{1}}));
  assertFalse(ApkSigningPolicy.sameSigners(new byte[][]{},new byte[][]{}));
  assertFalse(ApkSigningPolicy.sameSigners(null,new byte[][]{{1}}));
 }
}
