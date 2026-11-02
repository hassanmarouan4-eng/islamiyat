package org.dorossi.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkRulesTest {
    @Test
    fun platformPagesStayInsideTheApp() {
        assertEquals(LinkTarget.INTERNAL, LinkRules.classify("https://dorossi.org/#subject-math"))
        assertEquals(LinkTarget.INTERNAL, LinkRules.classify("https://WWW.Dorossi.org/api/file/x?t=1&dl=1"))
        assertEquals(LinkTarget.INTERNAL, LinkRules.classify("https://islamiyat.hassanmarouan4.workers.dev/"))
        assertEquals(LinkTarget.INTERNAL, LinkRules.classify("about:blank"))
        assertEquals(LinkTarget.INTERNAL, LinkRules.classify("blob:https://dorossi.org/5b1c"))
    }

    @Test
    fun otherSitesAndAppsOpenOutside() {
        assertEquals(LinkTarget.EXTERNAL, LinkRules.classify("https://wa.me/212600000000?text=%D8%B3"))
        assertEquals(LinkTarget.EXTERNAL, LinkRules.classify("https://www.facebook.com/page"))
        assertEquals(LinkTarget.EXTERNAL, LinkRules.classify("https://dorossi.org.evil.example/"))
        assertEquals(LinkTarget.EXTERNAL, LinkRules.classify("whatsapp://send?phone=212600000000"))
        assertEquals(LinkTarget.EXTERNAL, LinkRules.classify("tel:+212600000000"))
        assertEquals(LinkTarget.EXTERNAL, LinkRules.classify("mailto:contact@dorossi.org"))
        assertEquals(LinkTarget.INTENT, LinkRules.classify("intent://send#Intent;scheme=whatsapp;end"))
    }

    @Test
    fun dangerousOrEmptyLinksAreIgnored() {
        assertEquals(LinkTarget.IGNORE, LinkRules.classify("javascript:alert(1)"))
        assertEquals(LinkTarget.IGNORE, LinkRules.classify("file:///sdcard/a.pdf"))
        assertEquals(LinkTarget.IGNORE, LinkRules.classify("data:text/html,hi"))
        assertEquals(LinkTarget.IGNORE, LinkRules.classify("no scheme here"))
    }

    @Test
    fun hostIgnoresUserInfoAndPort() {
        assertEquals("dorossi.org", LinkRules.host("https://user@dorossi.org:443/x"))
        assertEquals("dorossi.org", LinkRules.host("https://dorossi.org#home"))
        assertNull(LinkRules.host("tel:0600000000"))
        assertTrue(LinkRules.isInternal("http://dorossi.org/"))
        assertFalse(LinkRules.isInternal(null))
    }

    @Test
    fun homePageIsRecognised() {
        assertTrue(LinkRules.isHome("https://dorossi.org"))
        assertTrue(LinkRules.isHome("https://dorossi.org/"))
        assertTrue(LinkRules.isHome("https://dorossi.org/#home"))
        assertTrue(LinkRules.isHome("https://dorossi.org/#"))
        assertFalse(LinkRules.isHome("https://dorossi.org/#subjects"))
        assertFalse(LinkRules.isHome("https://dorossi.org/api/x"))
        assertFalse(LinkRules.isHome("https://wa.me/"))
        assertFalse(LinkRules.isHome(null))
    }

    @Test
    fun officialAddressComesFirst() {
        assertEquals("https://www.dorossi.org", PlatformHosts.candidates.first())
        assertEquals(
            "https://www.dorossi.org/#subject-math",
            LinkRules.rebase("https://dorossi.org/#subject-math", PlatformHosts.candidates.first()),
        )
        assertTrue(LinkRules.isHome("https://www.dorossi.org/#home"))
    }

    @Test
    fun linksAreMovedToTheActiveAddress() {
        val base = "https://islamiyat.hassanmarouan4.workers.dev"
        assertEquals("$base/#level-1ac", LinkRules.rebase("https://dorossi.org/#level-1ac", base))
        assertEquals("$base?x=1", LinkRules.rebase("https://www.dorossi.org?x=1", base))
        assertEquals(base, LinkRules.rebase("https://dorossi.org", base))
        assertEquals("https://wa.me/1", LinkRules.rebase("https://wa.me/1", base))
    }
}
