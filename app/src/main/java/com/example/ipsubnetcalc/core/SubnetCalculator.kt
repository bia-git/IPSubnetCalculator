package com.example.ipsubnetcalc.core

/**
 * Core IPv4 subnet calculation logic. Pure Kotlin (no Android dependencies)
 * so it can be unit-tested independently of the UI.
 *
 * All 32-bit values are stored as [Long] in the range 0..0xFFFFFFFF to avoid
 * sign-extension problems with Kotlin's signed [Int].
 */
object SubnetCalculator {

    private const val MASK_32 = 0xFFFFFFFFL

    /** A validated, fully-computed subnet result. */
    data class Result(
        val inputIp: String,          // the IP the user typed
        val prefix: Int,              // CIDR /n
        val networkAddress: String,   // e.g. 192.168.1.0
        val broadcastAddress: String, // e.g. 192.168.1.255
        val firstHost: String,        // e.g. 192.168.1.1
        val lastHost: String,         // e.g. 192.168.1.254
        val netmask: String,          // e.g. 255.255.255.0
        val wildcard: String,         // e.g. 0.0.0.255
        val totalIps: Long,           // 2^(32-n)
        val usableHosts: Long,        // totalIps minus network/broadcast where applicable
        val ipClass: String,          // A / B / C / D / E / Loopback
        val isPrivate: Boolean,
        val isLoopback: Boolean,
        val cidrNotation: String,     // e.g. 192.168.1.0/24 (network/cidr)
        val binaryMask: String        // 32-char binary representation of the mask
    )

    class ParseException(message: String) : Exception(message)

    /**
     * Calculate a subnet from an arbitrary input string. Accepts:
     *   "192.168.1.10/24"      (IP + CIDR)
     *   "192.168.1.10"          (IP only — assumes /32 host)
     *   "192.168.1.10 255.255.255.0"  (IP + dotted mask)
     *   "192.168.1.10/255.255.255.0"
     *
     * @throws ParseException on invalid input
     */
    fun calculate(input: String): Result {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) throw ParseException("กรุณากรอก IP address")

        // Split off the suffix after '/' if present.
        val (ipPart, suffixPart) = if ('/' in trimmed) {
            val slashIndex = trimmed.indexOf('/')
            trimmed.substring(0, slashIndex) to trimmed.substring(slashIndex + 1)
        } else {
            // Could be "ip mask" separated by whitespace.
            val tokens = trimmed.split(Regex("\\s+"))
            if (tokens.size == 2) {
                tokens[0] to tokens[1]
            } else {
                trimmed to null
            }
        }

        val ipLong = parseIp(ipPart)
        val prefix = suffixPart?.let { parsePrefix(it) } ?: 32

        return calculateFromLong(ipLong, prefix, ipPart)
    }

    /** Calculate from already-parsed numeric values. */
    fun calculateFromLong(ip: Long, prefix: Int, displayIp: String = formatIp(ip)): Result {
        if (prefix !in 0..32) throw ParseException("CIDR ต้องอยู่ระหว่าง 0 ถึง 32")

        val mask = prefixToMask(prefix)
        val wildcard = MASK_32 and mask.inv()

        val network = ip and mask
        val broadcast = network or wildcard

        val totalIps = if (prefix == 0) MASK_32 + 1 else 1L shl (32 - prefix)

        // Usable hosts follow RFC 3021 for /31 (point-to-point) and /32 (single host).
        val usableHosts: Long = when (prefix) {
            32 -> 1L
            31 -> 2L
            else -> totalIps - 2
        }

        // First/last usable host depend on prefix size.
        val (firstHost, lastHost) = when (prefix) {
            32 -> network to network
            31 -> network to broadcast
            else -> (network + 1) to (broadcast - 1)
        }

        val firstOctet = ((ip ushr 24) and 0xFF).toInt()
        val ipClass = classify(firstOctet)
        val isLoopback = firstOctet == 127
        val isPrivate = isPrivate(ip)

        return Result(
            inputIp = displayIp,
            prefix = prefix,
            networkAddress = formatIp(network),
            broadcastAddress = if (prefix == 32) formatIp(network) else formatIp(broadcast),
            firstHost = formatIp(firstHost),
            lastHost = formatIp(lastHost),
            netmask = formatIp(mask),
            wildcard = formatIp(wildcard),
            totalIps = totalIps,
            usableHosts = usableHosts,
            ipClass = if (isLoopback) "Loopback" else ipClass,
            isPrivate = isPrivate,
            isLoopback = isLoopback,
            cidrNotation = "${formatIp(network)}/$prefix",
            binaryMask = toBinary(mask)
        )
    }

    /** Convert a CIDR prefix (0..32) to a 32-bit netmask. */
    fun prefixToMask(prefix: Int): Long {
        if (prefix == 0) return 0L
        val shift = 32 - prefix
        return (MASK_32 shl shift) and MASK_32
    }

    /** Convert a dotted-decimal mask like "255.255.255.0" to a prefix length. */
    fun maskToPrefix(mask: String): Int {
        val value = parseIp(mask)
        // Count leading 1-bits by counting trailing zeros of the inverted mask.
        // For example: 255.255.255.0  -> ~ = 0.0.0.255 -> 8 trailing zeros... but
        // we want the count of leading ones. Simplest robust approach: try every
        // prefix 0..32 and find the one whose generated mask equals `value`.
        for (prefix in 0..32) {
            if (prefixToMask(prefix) == value) return prefix
        }
        // No contiguous mask matched → the user supplied a non-contiguous mask.
        throw ParseException("subnet mask ไม่ถูกต้อง (บิตไม่ต่อเนื่อง): $mask")
    }

    /** Parse "192.168.1.10" → Long. Throws [ParseException] on bad input. */
    fun parseIp(text: String): Long {
        val parts = text.trim().split(".")
        if (parts.size != 4) throw ParseException("IP address ไม่ถูกต้อง: $text")
        var result = 0L
        for (part in parts) {
            val octet = part.trim().toIntOrNull()
                ?: throw ParseException("octet ไม่ใช่ตัวเลข: $part")
            if (octet !in 0..255) throw ParseException("octet ต้องอยู่ระหว่าง 0-255: $octet")
            result = (result shl 8) or octet.toLong()
        }
        return result and MASK_32
    }

    /** Format a Long back to dotted-decimal. */
    fun formatIp(value: Long): String {
        val v = value and MASK_32
        val a = (v ushr 24) and 0xFF
        val b = (v ushr 16) and 0xFF
        val c = (v ushr 8) and 0xFF
        val d = v and 0xFF
        return "$a.$b.$c.$d"
    }

    /** Convert a 32-bit value to a 32-character binary string. */
    fun toBinary(value: Long): String {
        val v = value and MASK_32
        val sb = StringBuilder(35)
        for (i in 31 downTo 0) {
            sb.append(if ((v ushr i) and 1L == 1L) '1' else '0')
            if (i > 0 && i % 8 == 0) sb.append('.')
        }
        return sb.toString()
    }

    private fun classify(firstOctet: Int): String = when (firstOctet) {
        in 0..127 -> "A"
        in 128..191 -> "B"
        in 192..223 -> "C"
        in 224..239 -> "D"
        in 240..255 -> "E"
        else -> "?"
    }

    /** RFC 1918 private address ranges + loopback detection. */
    fun isPrivate(ip: Long): Boolean {
        val a = ((ip ushr 24) and 0xFF).toInt()
        val b = ((ip ushr 16) and 0xFF).toInt()
        return when {
            a == 10 -> true                                  // 10.0.0.0/8
            a == 172 && b in 16..31 -> true                  // 172.16.0.0/12
            a == 192 && b == 168 -> true                     // 192.168.0.0/16
            else -> false
        }
    }

    private fun parsePrefix(suffix: String): Int {
        val trimmed = suffix.trim()
        // If it contains a dot, treat as a dotted mask.
        return if ('.' in trimmed) {
            maskToPrefix(trimmed)
        } else {
            val value = trimmed.toIntOrNull()
                ?: throw ParseException("CIDR ไม่ถูกต้อง: $trimmed")
            if (value !in 0..32) throw ParseException("CIDR ต้องอยู่ระหว่าง 0 ถึง 32: $value")
            value
        }
    }
}
