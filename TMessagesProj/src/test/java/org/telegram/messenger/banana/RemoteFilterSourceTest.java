package org.telegram.messenger.banana;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.io.ByteArrayOutputStream;
import org.junit.Test;
import static org.junit.Assert.*;

public class RemoteFilterSourceTest {
    @Test public void documentedDeepLinksAndPublicRawSourcesUseHttps() {
        assertEquals("https://dpaste.com/H4EN4D8C4.txt", RemoteFilterSource.url("tg://ayu/filters/import/dpaste.com/H4EN4D8C4.txt").toString());
        assertEquals("gist.githubusercontent.com", RemoteFilterSource.url("https://gist.githubusercontent.com/u/id/raw/filters.json").getHost());
        assertEquals("https", RemoteFilterSource.url("pastebin.com/raw/id").getScheme());
        assertEquals(443, RemoteFilterSource.url("https://nekobin.com:443/raw/id").getPort());
        assertEquals("https://dpaste.com/raw/id?source=https://example.test", RemoteFilterSource.url("dpaste.com/raw/id?source=https://example.test").toString());
    }
    @Test public void hostLookalikesCredentialsPortsAndNonHttpsAreRejected() {
        for (String source : new String[]{"http://dpaste.com/a", "https://dpaste.com.evil.test/a", "https://dpaste.com@evil.test/a",
                "https://user@dpaste.com/a", "https://dpaste.com:8443/a", "file:///tmp/a", "https://127.0.0.1/a",
                "https://gist.github.com/u/id", "https://dpaste.com/a#secret", "tg://ayu/filters/import/evil.test/a"}) {
            try { RemoteFilterSource.url(source); fail("Rejected source was accepted"); } catch (IllegalArgumentException expected) { }
        }
    }
    @Test public void everyRedirectIsCheckedAndRelativePathsPreserveEscaping() {
        URI start = RemoteFilterSource.url("https://dpaste.com/path/source.txt");
        assertEquals("https://dpaste.com/raw/a%20b?x=1", RemoteFilterSource.redirect(start, "../raw/a%20b?x=1").toString());
        for (String target : new String[]{"https://evil.test/a", "//evil.test/a", "http://dpaste.com/a", "https://dpaste.com:80/a"}) {
            try { RemoteFilterSource.redirect(start, target); fail("Unsafe redirect accepted"); } catch (IllegalArgumentException expected) { }
        }
    }
    @Test public void bytesAndDecompressedOutputAreBoundedAndUtf8IsStrict() throws Exception {
        assertEquals("{\"text\":\"Привет\"}", RemoteFilterSource.read(new ByteArrayInputStream("\ufeff{\"text\":\"Привет\"}".getBytes(StandardCharsets.UTF_8))));
        try { RemoteFilterSource.read(new ByteArrayInputStream(new byte[]{(byte) 0xc3, 0x28})); fail("Malformed UTF-8 must not be rewritten silently"); } catch (IOException expected) { }
        try { RemoteFilterSource.read(new ByteArrayInputStream(new byte[FilterRules.MAX_JSON + 1])); fail("Oversized response accepted"); } catch (IOException expected) { }
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) { gzip.write(new byte[FilterRules.MAX_JSON + 1]); }
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed.toByteArray()))) {
            try { RemoteFilterSource.read(gzip); fail("Small compressed data must not bypass the decoded size limit"); } catch (IOException expected) { }
        }
    }
    @Test public void unknownLengthGzipHeadersCannotBypassTheWireLimit() throws Exception {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(new byte[]{0x1f, (byte) 0x8b, 8, 8, 0, 0, 0, 0, 0, 0});
        byte[] name = new byte[FilterRules.MAX_JSON + 1]; java.util.Arrays.fill(name, (byte) 'a');
        body.write(name); body.write(0);
        try {
            new GZIPInputStream(RemoteFilterSource.bounded(new ByteArrayInputStream(body.toByteArray())));
            fail("Unbounded gzip filename/header was read before checking decoded size");
        } catch (IOException expected) { }
        try {
            RemoteFilterSource.bounded(new ByteArrayInputStream(new byte[FilterRules.MAX_JSON + 1])).skip(FilterRules.MAX_JSON + 1L);
            fail("Skipping must also respect the wire budget");
        } catch (IOException expected) { }
    }
}
