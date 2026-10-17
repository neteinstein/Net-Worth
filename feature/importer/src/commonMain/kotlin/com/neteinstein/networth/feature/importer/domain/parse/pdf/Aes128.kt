package com.neteinstein.networth.feature.importer.domain.parse.pdf

/** AES-128 decryption in CBC mode (FIPS-197), as the PDF AESV2 crypt filter uses it: IV first, PKCS#5 padding. */
internal object Aes128 {
    private val SBOX = IntArray(256)
    private val INV_SBOX = IntArray(256)

    init {
        // Build the S-box from the multiplicative inverse in GF(2^8) plus the affine transform.
        var p = 1
        var q = 1
        do {
            p = p xor (p shl 1) xor (if (p and 0x80 != 0) 0x1B else 0)
            p = p and 0xFF
            q = q xor (q shl 1)
            q = q xor (q shl 2)
            q = q xor (q shl 4)
            q = q and 0xFF
            if (q and 0x80 != 0) q = q xor 0x09
            val x = q xor rotl8(q, 1) xor rotl8(q, 2) xor rotl8(q, 3) xor rotl8(q, 4)
            SBOX[p] = (x xor 0x63) and 0xFF
        } while (p != 1)
        SBOX[0] = 0x63
        for (i in 0 until 256) INV_SBOX[SBOX[i]] = i
    }

    private fun rotl8(
        x: Int,
        shift: Int,
    ): Int = ((x shl shift) or (x ushr (8 - shift))) and 0xFF

    private fun xtime(x: Int): Int = ((x shl 1) xor (if (x and 0x80 != 0) 0x1B else 0)) and 0xFF

    private fun mul(
        a: Int,
        b: Int,
    ): Int {
        var x = a
        var y = b
        var r = 0
        while (y != 0) {
            if (y and 1 != 0) r = r xor x
            x = xtime(x)
            y = y ushr 1
        }
        return r
    }

    internal fun expandKey(key: ByteArray): IntArray {
        val w = IntArray(176)
        for (i in 0 until 16) w[i] = key[i].toInt() and 0xFF
        var rcon = 1
        for (i in 16 until 176 step 4) {
            var t0 = w[i - 4]
            var t1 = w[i - 3]
            var t2 = w[i - 2]
            var t3 = w[i - 1]
            if (i % 16 == 0) {
                val tmp = t0
                t0 = SBOX[t1] xor rcon
                t1 = SBOX[t2]
                t2 = SBOX[t3]
                t3 = SBOX[tmp]
                rcon = xtime(rcon)
            }
            w[i] = w[i - 16] xor t0
            w[i + 1] = w[i - 15] xor t1
            w[i + 2] = w[i - 14] xor t2
            w[i + 3] = w[i - 13] xor t3
        }
        return w
    }

    fun decryptBlock(
        roundKeys: IntArray,
        input: ByteArray,
        offset: Int,
    ): IntArray {
        val s = IntArray(16) { (input[offset + it].toInt() and 0xFF) xor roundKeys[160 + it] }
        for (round in 9 downTo 0) {
            // InvShiftRows + InvSubBytes
            val t = IntArray(16)
            for (c in 0 until 4) {
                for (r in 0 until 4) {
                    t[((c + r) % 4) * 4 + r] = INV_SBOX[s[c * 4 + r]]
                }
            }
            for (i in 0 until 16) t[i] = t[i] xor roundKeys[round * 16 + i]
            if (round > 0) {
                for (c in 0 until 4) {
                    val a0 = t[c * 4]
                    val a1 = t[c * 4 + 1]
                    val a2 = t[c * 4 + 2]
                    val a3 = t[c * 4 + 3]
                    s[c * 4] = mul(a0, 14) xor mul(a1, 11) xor mul(a2, 13) xor mul(a3, 9)
                    s[c * 4 + 1] = mul(a0, 9) xor mul(a1, 14) xor mul(a2, 11) xor mul(a3, 13)
                    s[c * 4 + 2] = mul(a0, 13) xor mul(a1, 9) xor mul(a2, 14) xor mul(a3, 11)
                    s[c * 4 + 3] = mul(a0, 11) xor mul(a1, 13) xor mul(a2, 9) xor mul(a3, 14)
                }
            } else {
                t.copyInto(s)
            }
        }
        return s
    }

    fun decryptCbc(
        key: ByteArray,
        data: ByteArray,
    ): ByteArray {
        if (data.size < 32 || data.size % 16 != 0) return ByteArray(0)
        val roundKeys = expandKey(key)
        val out = ByteArray(data.size - 16)
        var block = 16
        while (block < data.size) {
            val plain = decryptBlock(roundKeys, data, block)
            for (i in 0 until 16) out[block - 16 + i] = (plain[i] xor (data[block - 16 + i].toInt() and 0xFF)).toByte()
            block += 16
        }
        val pad = out.last().toInt() and 0xFF
        return if (pad in 1..16) out.copyOf(out.size - pad) else out
    }
}
