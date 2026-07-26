package com.example.ipsubnetcalc.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Additional audit tests that cross-check the engine against well-known
 * reference values (from ipcalc / Cisco / RFC documents). Designed to catch
 * subtle bugs around bit math, /31 and /32 edge cases, and boundary values.
 */
class SubnetCalculatorAuditTest {

    // ─────────────────────────────────────────────────────────────────────────
    // prefixToMask: every prefix 0..32 must produce the canonical netmask
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `prefix to mask covers all 33 prefixes correctly`() {
        // (prefix, expected dotted mask, expected hex)
        val expected = listOf(
            Triple(0,  "0.0.0.0", 0x00000000L),
            Triple(1,  "128.0.0.0", 0x80000000L),
            Triple(8,  "255.0.0.0", 0xFF000000L),
            Triple(12, "255.240.0.0", 0xFFF00000L),
            Triple(16, "255.255.0.0", 0xFFFF0000L),
            Triple(20, "255.255.240.0", 0xFFFFF000L),
            Triple(22, "255.255.252.0", 0xFFFFFC00L),
            Triple(23, "255.255.254.0", 0xFFFFFE00L),
            Triple(24, "255.255.255.0", 0xFFFFFF00L),
            Triple(25, "255.255.255.128", 0xFFFFFF80L),
            Triple(26, "255.255.255.192", 0xFFFFFFC0L),
            Triple(27, "255.255.255.224", 0xFFFFFFE0L),
            Triple(28, "255.255.255.240", 0xFFFFFFF0L),
            Triple(29, "255.255.255.248", 0xFFFFFFF8L),
            Triple(30, "255.255.255.252", 0xFFFFFFFCL),
            Triple(31, "255.255.255.254", 0xFFFFFFFEL),
            Triple(32, "255.255.255.255", 0xFFFFFFFFL)
        )
        for ((prefix, dotted, hex) in expected) {
            val mask = SubnetCalculator.prefixToMask(prefix)
            assertEquals("prefix /$prefix hex mask", hex, mask)
            assertEquals("prefix /$prefix dotted mask", dotted, SubnetCalculator.formatIp(mask))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Classic Cisco-style examples (verified against ipcalc)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `slash 23 VLSM boundary`() {
        // 192.168.0.0/23 covers .0.0 - .1.255
        val r = SubnetCalculator.calculate("192.168.0.50/23")
        assertEquals("192.168.0.0", r.networkAddress)
        assertEquals("192.168.1.255", r.broadcastAddress)
        assertEquals("192.168.0.1", r.firstHost)
        assertEquals("192.168.1.254", r.lastHost)
        assertEquals(512L, r.totalIps)
        assertEquals(510L, r.usableHosts)
        assertEquals("255.255.254.0", r.netmask)
        assertEquals("0.0.1.255", r.wildcard)
    }

    @Test
    fun `slash 22 covers four slash-24s`() {
        val r = SubnetCalculator.calculate("10.20.4.1/22")
        assertEquals("10.20.4.0", r.networkAddress)
        assertEquals("10.20.7.255", r.broadcastAddress)
        assertEquals(1024L, r.totalIps)
        assertEquals(1022L, r.usableHosts)
    }

    @Test
    fun `slash 12 class B private range`() {
        // 172.16.0.0/12 is the RFC 1918 private block
        val r = SubnetCalculator.calculate("172.20.35.6/12")
        assertEquals("172.16.0.0", r.networkAddress)
        assertEquals("172.31.255.255", r.broadcastAddress)
        assertEquals(1_048_576L, r.totalIps)
        assertEquals(1_048_574L, r.usableHosts)
        assertTrue(r.isPrivate)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // /31 RFC 3021 point-to-point links
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `slash 31 second subnet in range`() {
        val r = SubnetCalculator.calculate("10.0.0.2/31")
        assertEquals("10.0.0.2", r.networkAddress)
        assertEquals("10.0.0.3", r.broadcastAddress)
        assertEquals("10.0.0.2", r.firstHost)
        assertEquals("10.0.0.3", r.lastHost)
        assertEquals(2L, r.totalIps)
        assertEquals(2L, r.usableHosts)
    }

    @Test
    fun `slash 31 odd address falls in correct subnet`() {
        val r = SubnetCalculator.calculate("192.168.1.5/31")
        assertEquals("192.168.1.4", r.networkAddress)
        assertEquals("192.168.1.5", r.broadcastAddress)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // /0 default route edge case
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `slash 0 default route covers entire IPv4 space`() {
        val r = SubnetCalculator.calculate("8.8.8.8/0")
        assertEquals("0.0.0.0", r.networkAddress)
        assertEquals("255.255.255.255", r.broadcastAddress)
        assertEquals(4_294_967_296L, r.totalIps)  // 2^32
        assertEquals(4_294_967_294L, r.usableHosts)
        assertEquals("0.0.0.0", r.netmask)
        assertEquals("255.255.255.255", r.wildcard)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Boundary host addresses: all-0 and all-255 octets
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `network address itself as input`() {
        val r = SubnetCalculator.calculate("192.168.1.0/24")
        assertEquals("192.168.1.0", r.networkAddress)
        assertEquals("192.168.1.1", r.firstHost)
    }

    @Test
    fun `broadcast address itself as input`() {
        val r = SubnetCalculator.calculate("192.168.1.255/24")
        assertEquals("192.168.1.0", r.networkAddress)
        assertEquals("192.168.1.255", r.broadcastAddress)
    }

    @Test
    fun `max ip 255 255 255 255 slash 32`() {
        val r = SubnetCalculator.calculate("255.255.255.255/32")
        assertEquals("255.255.255.255", r.networkAddress)
        assertEquals("255.255.255.255", r.broadcastAddress)
        assertEquals(1L, r.usableHosts)
    }

    @Test
    fun `zero ip 0 0 0 0 slash 0`() {
        val r = SubnetCalculator.calculate("0.0.0.0/0")
        assertEquals("0.0.0.0", r.networkAddress)
        assertEquals("255.255.255.255", r.broadcastAddress)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IP class detection — all 5 classes + special ranges
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `class boundaries exact`() {
        assertEquals("A", SubnetCalculator.calculate("0.0.0.0/32").ipClass)
        assertEquals("A", SubnetCalculator.calculate("126.255.255.255/32").ipClass)
        // 127.x is loopback — code overrides the Class A label (correct per RFC 3330)
        assertEquals("Loopback", SubnetCalculator.calculate("127.0.0.1/32").ipClass)
        assertEquals("Loopback", SubnetCalculator.calculate("127.255.255.255/32").ipClass)
        assertEquals("B", SubnetCalculator.calculate("128.0.0.0/32").ipClass)
        assertEquals("B", SubnetCalculator.calculate("191.255.255.255/32").ipClass)
        assertEquals("C", SubnetCalculator.calculate("192.0.0.0/32").ipClass)
        assertEquals("C", SubnetCalculator.calculate("223.255.255.255/32").ipClass)
        assertEquals("D", SubnetCalculator.calculate("224.0.0.0/32").ipClass)
        assertEquals("D", SubnetCalculator.calculate("239.255.255.255/32").ipClass)
        assertEquals("E", SubnetCalculator.calculate("240.0.0.0/32").ipClass)
        assertEquals("E", SubnetCalculator.calculate("255.255.255.255/32").ipClass)
    }

    @Test
    fun `loopback 127 overrides class A label`() {
        val r = SubnetCalculator.calculate("127.0.0.1/8")
        assertEquals("Loopback", r.ipClass)
        assertTrue(r.isLoopback)
        // 127.x is NOT RFC 1918 private, but is reserved.
        assertFalse(r.isPrivate)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private range boundaries (RFC 1918)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `10 slash 8 entire range is private`() {
        assertTrue(SubnetCalculator.calculate("10.0.0.0/32").isPrivate)
        assertTrue(SubnetCalculator.calculate("10.255.255.255/32").isPrivate)
    }

    @Test
    fun `172 16-31 slash 12 boundaries`() {
        assertTrue(SubnetCalculator.calculate("172.16.0.0/32").isPrivate)
        assertTrue(SubnetCalculator.calculate("172.31.255.255/32").isPrivate)
        assertFalse(SubnetCalculator.calculate("172.15.255.255/32").isPrivate)
        assertFalse(SubnetCalculator.calculate("172.32.0.0/32").isPrivate)
    }

    @Test
    fun `192 168 slash 16 boundaries`() {
        assertTrue(SubnetCalculator.calculate("192.168.0.0/32").isPrivate)
        assertTrue(SubnetCalculator.calculate("192.168.255.255/32").isPrivate)
        assertFalse(SubnetCalculator.calculate("192.167.255.255/32").isPrivate)
        assertFalse(SubnetCalculator.calculate("192.169.0.0/32").isPrivate)
    }

    @Test
    fun `public addresses not private`() {
        assertFalse(SubnetCalculator.calculate("8.8.8.8/32").isPrivate)
        assertFalse(SubnetCalculator.calculate("172.32.0.1/32").isPrivate)
        assertFalse(SubnetCalculator.calculate("11.0.0.1/32").isPrivate)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Binary mask representation
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `binary mask formats for various prefixes`() {
        val cases = mapOf(
            8  to "11111111.00000000.00000000.00000000",
            16 to "11111111.11111111.00000000.00000000",
            24 to "11111111.11111111.11111111.00000000",
            25 to "11111111.11111111.11111111.10000000",
            30 to "11111111.11111111.11111111.11111100",
            32 to "11111111.11111111.11111111.11111111",
            0  to "00000000.00000000.00000000.00000000"
        )
        for ((prefix, expected) in cases) {
            val mask = SubnetCalculator.prefixToMask(prefix)
            assertEquals("binary for /$prefix", expected, SubnetCalculator.toBinary(mask))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // maskToPrefix round-trips with prefixToMask
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `mask to prefix round trips for all prefixes`() {
        for (prefix in 0..32) {
            val mask = SubnetCalculator.prefixToMask(prefix)
            val dotted = SubnetCalculator.formatIp(mask)
            val back = SubnetCalculator.maskToPrefix(dotted)
            assertEquals("round-trip /$prefix", prefix, back)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Wildcard = inverse of netmask
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `wildcard is bitwise inverse of netmask`() {
        for (prefix in listOf(0, 8, 16, 20, 24, 25, 30, 31, 32)) {
            val r = SubnetCalculator.calculate("10.0.0.1/$prefix")
            val mask = SubnetCalculator.parseIp(r.netmask)
            val wild = SubnetCalculator.parseIp(r.wildcard)
            assertEquals("wildcard XOR mask = all 1s for /$prefix", 0xFFFFFFFFL, mask xor wild)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Subnet boundary: addresses near the boundary fall in the correct subnet
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `slash 26 four subnets in a slash 24`() {
        // 192.168.1.0/24 split into /26 = 4 subnets of 64 addresses each
        val r0 = SubnetCalculator.calculate("192.168.1.0/26")
        val r1 = SubnetCalculator.calculate("192.168.1.65/26")
        val r2 = SubnetCalculator.calculate("192.168.1.130/26")
        val r3 = SubnetCalculator.calculate("192.168.1.200/26")

        assertEquals("192.168.1.0", r0.networkAddress)
        assertEquals("192.168.1.63", r0.broadcastAddress)

        assertEquals("192.168.1.64", r1.networkAddress)
        assertEquals("192.168.1.127", r1.broadcastAddress)

        assertEquals("192.168.1.128", r2.networkAddress)
        assertEquals("192.168.1.191", r2.broadcastAddress)

        assertEquals("192.168.1.192", r3.networkAddress)
        assertEquals("192.168.1.255", r3.broadcastAddress)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Big counts must not overflow Int — verify Long handling
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `large host counts fit in Long without overflow`() {
        val r = SubnetCalculator.calculate("10.0.0.1/8")
        assertEquals(16_777_216L, r.totalIps)
        assertEquals(16_777_214L, r.usableHosts)

        val r2 = SubnetCalculator.calculate("9.0.0.1/4")
        // /4 = 2^28 = 268,435,456 addresses
        assertEquals(268_435_456L, r2.totalIps)
        assertEquals(268_435_454L, r2.usableHosts)
    }
}
