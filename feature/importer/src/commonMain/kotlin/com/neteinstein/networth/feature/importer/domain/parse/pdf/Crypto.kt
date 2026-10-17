package com.neteinstein.networth.feature.importer.domain.parse.pdf

/**
 * The two primitives of the PDF Standard security handler (revisions 2–4): MD5 and RC4.
 *
 * Banks "encrypt" statements with an empty user password, which only stops copy/paste. Opening
 * them needs no secret, but every string and stream is still RC4-encrypted with a key derived via
 * MD5. Neither is used for anything security-related here.
 */
internal object Md5 {
    private val SHIFTS = arrayOf(intArrayOf(7, 12, 17, 22), intArrayOf(5, 9, 14, 20), intArrayOf(4, 11, 16, 23), intArrayOf(6, 10, 15, 21))
    private val S = IntArray(64) { i -> SHIFTS[i / 16][i % 4] }
    private val K = IntArray(64) { i -> (kotlin.math.abs(kotlin.math.sin(i + 1.0)) * 4294967296.0).toLong().toInt() }

    fun digest(message: ByteArray): ByteArray {
        val bitLength = message.size.toLong() * 8
        val paddedSize = ((message.size + 8) / 64 + 1) * 64
        val padded = message.copyOf(paddedSize)
        padded[message.size] = 0x80.toByte()
        for (i in 0 until 8) padded[paddedSize - 8 + i] = (bitLength ushr (8 * i)).toByte()

        var a0 = 0x67452301
        var b0 = 0xefcdab89.toInt()
        var c0 = 0x98badcfe.toInt()
        var d0 = 0x10325476
        val m = IntArray(16)
        for (chunk in 0 until paddedSize / 64) {
            for (i in 0 until 16) {
                val o = chunk * 64 + i * 4
                m[i] = (padded[o].toInt() and 0xFF) or ((padded[o + 1].toInt() and 0xFF) shl 8) or
                    ((padded[o + 2].toInt() and 0xFF) shl 16) or ((padded[o + 3].toInt() and 0xFF) shl 24)
            }
            var a = a0
            var b = b0
            var c = c0
            var d = d0
            for (i in 0 until 64) {
                val f: Int
                val g: Int
                when (i / 16) {
                    0 -> {
                        f = (b and c) or (b.inv() and d)
                        g = i
                    }
                    1 -> {
                        f = (d and b) or (d.inv() and c)
                        g = (5 * i + 1) % 16
                    }
                    2 -> {
                        f = b xor c xor d
                        g = (3 * i + 5) % 16
                    }
                    else -> {
                        f = c xor (b or d.inv())
                        g = (7 * i) % 16
                    }
                }
                val rotated = a + f + K[i] + m[g]
                a = d
                d = c
                c = b
                b += rotated.rotateLeft(S[i])
            }
            a0 += a
            b0 += b
            c0 += c
            d0 += d
        }
        val out = ByteArray(16)
        intArrayOf(a0, b0, c0, d0).forEachIndexed { i, word ->
            for (j in 0 until 4) out[i * 4 + j] = (word ushr (8 * j)).toByte()
        }
        return out
    }
}

internal object Rc4 {
    fun apply(
        key: ByteArray,
        data: ByteArray,
    ): ByteArray {
        val s = IntArray(256) { it }
        var j = 0
        for (i in 0 until 256) {
            j = (j + s[i] + (key[i % key.size].toInt() and 0xFF)) and 0xFF
            val t = s[i]
            s[i] = s[j]
            s[j] = t
        }
        var i = 0
        j = 0
        val out = ByteArray(data.size)
        for (n in data.indices) {
            i = (i + 1) and 0xFF
            j = (j + s[i]) and 0xFF
            val t = s[i]
            s[i] = s[j]
            s[j] = t
            out[n] = (data[n].toInt() xor s[(s[i] + s[j]) and 0xFF]).toByte()
        }
        return out
    }
}
