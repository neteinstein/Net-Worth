package com.neteinstein.networth.feature.importer.domain.parse.pdf

import com.neteinstein.networth.feature.importer.domain.parse.ParseException

/**
 * Standard security handler, revisions 2–4, opened with the empty user password (ISO 32000-1 §7.6.3).
 * That is how banks protect statements: anyone can open them, so no password is ever asked for.
 */
internal class Decryptor private constructor(
    private val fileKey: ByteArray,
    private val aes: Boolean,
) {
    fun decrypt(
        data: ByteArray,
        number: Int,
        generation: Int,
    ): ByteArray {
        val seed = ByteBuilder(fileKey.size + 9)
        seed.addAll(fileKey)
        seed.add(number and 0xFF)
        seed.add((number shr 8) and 0xFF)
        seed.add((number shr 16) and 0xFF)
        seed.add(generation and 0xFF)
        seed.add((generation shr 8) and 0xFF)
        if (aes) seed.addAll(byteArrayOf(0x73, 0x41, 0x6C, 0x54)) // "sAlT"
        val key = Md5.digest(seed.toByteArray()).copyOf(minOf(fileKey.size + 5, 16))
        return if (aes) Aes128.decryptCbc(key, data) else Rc4.apply(key, data)
    }

    companion object {
        /** The 32-byte password padding string from the specification. */
        private val PADDING =
            "28bf4e5e4e758a4164004e56fffa01082e2e00b6d0683e802f0ca9fe6453697a".chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        fun create(
            encrypt: PdfDict?,
            id: PdfObject,
        ): Decryptor {
            if (encrypt == null || encrypt.name("Filter") != "Standard") {
                throw ParseException("This PDF is protected in a way Net Worth can't open")
            }
            val v = encrypt.int("V") ?: 0
            val r = encrypt.int("R") ?: 2
            if (v >= 5 || r >= 5) throw ParseException("This PDF uses AES-256 protection, which isn't supported yet")
            var aes = false
            var lengthBits = encrypt.int("Length") ?: 40
            if (v == 4) {
                val filter = ((encrypt["CF"] as? PdfDict)?.get(encrypt.name("StmF") ?: "StdCF") as? PdfDict)
                aes = filter?.name("CFM") == "AESV2"
                lengthBits = 128
            }
            val n = if (r == 2) 5 else lengthBits / 8
            val o = (encrypt["O"] as? PdfString)?.bytes ?: throw ParseException("This PDF's protection is damaged")
            val p = (encrypt["P"] as? PdfNumber)?.value?.toLong()?.toInt() ?: 0
            val firstId = ((id as? PdfArray)?.items?.firstOrNull() as? PdfString)?.bytes ?: ByteArray(0)
            val input = ByteBuilder(128)
            input.addAll(PADDING)
            input.addAll(o.copyOf(32))
            for (i in 0 until 4) input.add((p shr (8 * i)) and 0xFF)
            input.addAll(firstId)
            if (r >= 4 && (encrypt["EncryptMetadata"] as? PdfBool)?.value == false) input.addAll(ByteArray(4) { -1 })
            var key = Md5.digest(input.toByteArray()).copyOf(n)
            if (r >= 3) repeat(50) { key = Md5.digest(key).copyOf(n) }
            return Decryptor(key, aes)
        }
    }
}
