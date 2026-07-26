package com.example.ipsubnetcalc.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SubnetCalculatorTest {

    @Test
    fun `parses IP to long correctly`() {
        assertEquals(0xC0A8010AL, SubnetCalculator.parseIp("192.168.1.10"))
        assertEquals(0x0A000005L, SubnetCalculator.parseIp("10.0.0.5"))
        assertEquals(0xFFFFFFFFL, SubnetCalculator.parseIp("255.255.255.255"))
        assertEquals(0x00000000L, SubnetCalculator.parseIp("0.0.0.0"))
    }

    @Test
    fun `formats long back to dotted decimal`() {
        assertEquals("192.168.1.10", SubnetCalculator.formatIp(0xC0A8010AL))
        assertEquals("255.255.255.255", SubnetCalculator.formatIp(0xFFFFFFFFL))
    }

    @Test
    fun `prefix to mask converts correctly`() {
        assertEquals(0xFFFFFFFFL, SubnetCalculator.prefixToMask(32))
        assertEquals(0xFFFFFFFEL, SubnetCalculator.prefixToMask(31))
        assertEquals(0xFFFFFF00L, SubnetCalculator.prefixToMask(24))
        assertEquals(0xFFFF0000L, SubnetCalculator.prefixToMask(16))
        assertEquals(0xFF000000L, SubnetCalculator.prefixToMask(8))
        assertEquals(0x00000000L, SubnetCalculator.prefixToMask(0))
    }

    @Test
    fun `mask to prefix converts correctly`() {
        assertEquals(24, SubnetCalculator.maskToPrefix("255.255.255.0"))
        assertEquals(8, SubnetCalculator.maskToPrefix("255.0.0.0"))
        assertEquals(32, SubnetCalculator.maskToPrefix("255.255.255.255"))
        assertEquals(22, SubnetCalculator.maskToPrefix("255.255.252.0"))
    }

    @Test
    fun `non-contiguous mask rejected`() {
        try {
            SubnetCalculator.maskToPrefix("255.0.255.0")
            fail("expected ParseException")
        } catch (e: SubnetCalculator.ParseException) {
            // ok
        }
    }

    @Test
    fun `typical slash-24 calculation`() {
        val r = SubnetCalculator.calculate("192.168.1.10/24")
        assertEquals("192.168.1.0", r.networkAddress)
        assertEquals("192.168.1.255", r.broadcastAddress)
        assertEquals("192.168.1.1", r.firstHost)
        assertEquals("192.168.1.254", r.lastHost)
        assertEquals("255.255.255.0", r.netmask)
        assertEquals("0.0.0.255", r.wildcard)
        assertEquals(256L, r.totalIps)
        assertEquals(254L, r.usableHosts)
        assertEquals("C", r.ipClass)
        assertTrue(r.isPrivate)
        assertFalse(r.isLoopback)
        assertEquals("192.168.1.0/24", r.cidrNotation)
    }

    @Test
    fun `class A private range slash-8`() {
        val r = SubnetCalculator.calculate("10.20.30.40/8")
        assertEquals("10.0.0.0", r.networkAddress)
        assertEquals("10.255.255.255", r.broadcastAddress)
        assertEquals("10.0.0.1", r.firstHost)
        assertEquals("10.255.255.254", r.lastHost)
        assertEquals("255.0.0.0", r.netmask)
        assertEquals(16777216L, r.totalIps)
        assertEquals(16777214L, r.usableHosts)
        assertEquals("A", r.ipClass)
        assertTrue(r.isPrivate)
    }

    @Test
    fun `slash 30 yields 2 usable hosts`() {
        val r = SubnetCalculator.calculate("192.168.1.4/30")
        assertEquals("192.168.1.4", r.networkAddress)
        assertEquals("192.168.1.7", r.broadcastAddress)
        assertEquals("192.168.1.5", r.firstHost)
        assertEquals("192.168.1.6", r.lastHost)
        assertEquals(4L, r.totalIps)
        assertEquals(2L, r.usableHosts)
    }

    @Test
    fun `slash 31 point-to-point RFC 3021`() {
        val r = SubnetCalculator.calculate("10.0.0.0/31")
        assertEquals("10.0.0.0", r.networkAddress)
        assertEquals("10.0.0.1", r.broadcastAddress)
        assertEquals("10.0.0.0", r.firstHost)
        assertEquals("10.0.0.1", r.lastHost)
        assertEquals(2L, r.totalIps)
        assertEquals(2L, r.usableHosts)
    }

    @Test
    fun `slash 32 single host`() {
        val r = SubnetCalculator.calculate("192.168.1.50/32")
        assertEquals("192.168.1.50", r.networkAddress)
        assertEquals("192.168.1.50", r.broadcastAddress)
        assertEquals("192.168.1.50", r.firstHost)
        assertEquals("192.168.1.50", r.lastHost)
        assertEquals(1L, r.totalIps)
        assertEquals(1L, r.usableHosts)
    }

    @Test
    fun `accepts IP plus dotted mask`() {
        val r = SubnetCalculator.calculate("192.168.10.0/255.255.255.192")
        assertEquals("192.168.10.0", r.networkAddress)
        assertEquals("192.168.10.63", r.broadcastAddress)
        assertEquals(26, r.prefix)
        assertEquals("255.255.255.192", r.netmask)
        assertEquals(62L, r.usableHosts)
    }

    @Test
    fun `accepts space-separated IP and mask`() {
        val r = SubnetCalculator.calculate("172.16.50.20 255.255.240.0")
        assertEquals("172.16.48.0", r.networkAddress)
        assertEquals("172.16.63.255", r.broadcastAddress)
        assertEquals(20, r.prefix)
    }

    @Test
    fun `loopback detection`() {
        val r = SubnetCalculator.calculate("127.0.0.1/8")
        assertTrue(r.isLoopback)
        assertEquals("Loopback", r.ipClass)
        assertFalse(r.isPrivate)
    }

    @Test
    fun `public address is not private`() {
        val r = SubnetCalculator.calculate("203.0.113.7/24")
        assertFalse(r.isPrivate)
        assertEquals("C", r.ipClass)
    }

    @Test
    fun `private range 172 16-31 detected correctly`() {
        assertTrue(SubnetCalculator.calculate("172.16.0.1/12").isPrivate)
        assertTrue(SubnetCalculator.calculate("172.31.255.254/12").isPrivate)
        assertFalse(SubnetCalculator.calculate("172.32.0.1/12").isPrivate)
        assertFalse(SubnetCalculator.calculate("172.15.0.1/12").isPrivate)
    }

    @Test
    fun `invalid IP rejected`() {
        try { SubnetCalculator.calculate("999.1.1.1/24"); fail() }
        catch (e: SubnetCalculator.ParseException) { /* ok */ }

        try { SubnetCalculator.calculate("1.2.3/24"); fail() }
        catch (e: SubnetCalculator.ParseException) { /* ok */ }
    }

    @Test
    fun `invalid prefix rejected`() {
        try { SubnetCalculator.calculate("192.168.1.1/40"); fail() }
        catch (e: SubnetCalculator.ParseException) { /* ok */ }
    }

    @Test
    fun `binary mask formatting`() {
        val r = SubnetCalculator.calculate("192.168.1.1/24")
        assertEquals("11111111.11111111.11111111.00000000", r.binaryMask)
    }

    @Test
    fun `subnet table has 33 rows`() {
        assertEquals(33, SubnetTable.all.size)
        assertEquals(0, SubnetTable.all.first().cidr)
        assertEquals(32, SubnetTable.all.last().cidr)
    }

    @Test
    fun `subnet table host counts`() {
        val slash24 = SubnetTable.all.first { it.cidr == 24 }
        assertEquals(256L, slash24.totalIps)
        assertEquals(254L, slash24.usableHosts)

        val slash30 = SubnetTable.all.first { it.cidr == 30 }
        assertEquals(4L, slash30.totalIps)
        assertEquals(2L, slash30.usableHosts)

        val slash32 = SubnetTable.all.first { it.cidr == 32 }
        assertEquals(1L, slash32.totalIps)
        assertEquals(1L, slash32.usableHosts)
    }
}
