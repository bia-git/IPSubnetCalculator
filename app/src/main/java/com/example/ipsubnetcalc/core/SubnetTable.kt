package com.example.ipsubnetcalc.core

/**
 * Reference table of all CIDR prefixes (/0 .. /32) with their netmask,
 * total IP count and usable host count. Used by the "Subnet Table" screen.
 *
 * Values are derived from [SubnetCalculator.prefixToMask] so they stay
 * consistent with the calculation engine.
 */
object SubnetTable {

    data class Row(
        val cidr: Int,
        val netmask: String,
        val totalIps: Long,
        val usableHosts: Long,
        val ipClass: String   // shorthand class for the mask itself (based on first octet)
    )

    /** All 33 rows, from /0 to /32. */
    val all: List<Row> = (0..32).map { prefix -> buildRow(prefix) }

    /** Convenience: rows for the "commonly used" filter (/8 .. /30). */
    val commonRange: IntRange = 8..30

    /** Rows the user sees most often. */
    val common: List<Row> = all.filter { it.cidr in commonRange }

    private fun buildRow(prefix: Int): Row {
        val mask = SubnetCalculator.prefixToMask(prefix)
        val totalIps = if (prefix == 0) 0xFFFFFFFFL + 1 else 1L shl (32 - prefix)
        val usable: Long = when (prefix) {
            32 -> 1L
            31 -> 2L
            else -> totalIps - 2
        }
        val firstOctet = ((mask ushr 24) and 0xFF).toInt()
        return Row(
            cidr = prefix,
            netmask = SubnetCalculator.formatIp(mask),
            totalIps = totalIps,
            usableHosts = usable,
            ipClass = when {
                prefix == 0 -> "Default"
                prefix <= 8 -> "A"
                prefix <= 16 -> "B"
                prefix <= 24 -> "C"
                else -> "C"
            }
        )
    }

    /** Human-friendly formatting of large host counts, e.g. "16,777,216" or "65,536". */
    fun formatCount(value: Long): String {
        // Group by thousands using comma.
        return "%,d".format(value)
    }
}
