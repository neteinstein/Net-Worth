package com.neteinstein.networth.feature.importer.domain.parse.pdf

/** WinAnsiEncoding and the Adobe glyph names of its characters — enough for Latin-script statements. */
internal object GlyphNames {
    private val HIGH =
        mapOf(
            0x80 to '€',
            0x82 to '‚',
            0x83 to 'ƒ',
            0x84 to '„',
            0x85 to '…',
            0x86 to '†',
            0x87 to '‡',
            0x88 to 'ˆ',
            0x89 to '‰',
            0x8A to 'Š',
            0x8B to '‹',
            0x8C to 'Œ',
            0x8E to 'Ž',
            0x91 to '‘',
            0x92 to '’',
            0x93 to '“',
            0x94 to '”',
            0x95 to '•',
            0x96 to '–',
            0x97 to '—',
            0x98 to '˜',
            0x99 to '™',
            0x9A to 'š',
            0x9B to '›',
            0x9C to 'œ',
            0x9E to 'ž',
            0x9F to 'Ÿ',
        )

    private const val ASCII_NAMES =
        "space exclam quotedbl numbersign dollar percent ampersand quotesingle parenleft parenright asterisk plus " +
            "comma hyphen period slash zero one two three four five six seven eight nine colon semicolon less equal " +
            "greater question at A B C D E F G H I J K L M N O P Q R S T U V W X Y Z bracketleft backslash " +
            "bracketright asciicircum underscore grave a b c d e f g h i j k l m n o p q r s t u v w x y z braceleft " +
            "bar braceright asciitilde"

    private const val HIGH_NAMES =
        "Euro - quotesinglbase florin quotedblbase ellipsis dagger daggerdbl circumflex perthousand Scaron " +
            "guilsinglleft OE - Zcaron - - quoteleft quoteright quotedblleft quotedblright bullet endash emdash tilde " +
            "trademark scaron guilsinglright oe - zcaron Ydieresis"

    private const val LATIN1_NAMES =
        "nbspace exclamdown cent sterling currency yen brokenbar section dieresis copyright ordfeminine " +
            "guillemotleft logicalnot sfthyphen registered macron degree plusminus twosuperior threesuperior acute mu " +
            "paragraph periodcentered cedilla onesuperior ordmasculine guillemotright onequarter onehalf threequarters " +
            "questiondown Agrave Aacute Acircumflex Atilde Adieresis Aring AE Ccedilla Egrave Eacute Ecircumflex " +
            "Edieresis Igrave Iacute Icircumflex Idieresis Eth Ntilde Ograve Oacute Ocircumflex Otilde Odieresis " +
            "multiply Oslash Ugrave Uacute Ucircumflex Udieresis Yacute Thorn germandbls agrave aacute acircumflex " +
            "atilde adieresis aring ae ccedilla egrave eacute ecircumflex edieresis igrave iacute icircumflex " +
            "idieresis eth ntilde ograve oacute ocircumflex otilde odieresis divide oslash ugrave uacute ucircumflex " +
            "udieresis yacute thorn ydieresis"

    private val byName: Map<String, Char> =
        buildMap {
            ASCII_NAMES.split(' ').forEachIndexed { i, name -> put(name, (32 + i).toChar()) }
            HIGH_NAMES.split(' ').forEachIndexed { i, name -> if (name != "-") put(name, HIGH.getValue(0x80 + i)) }
            LATIN1_NAMES.split(' ').forEachIndexed { i, name -> put(name, (0xA0 + i).toChar()) }
            put("space", ' ')
            put("nbspace", ' ')
            put("sfthyphen", '-')
            put("hyphen", '-')
            put("minus", '-')
            put("fi", 'f') // ligatures are rare in statements; keep the first letter rather than drop them
            put("quotesingle", '\'')
            put("dotlessi", 'i')
        }

    private val hexName = Regex("^[A-Za-z]{1,2}([0-9A-Fa-f]{2})$")
    private val decimalName = Regex("^[0-9]{1,3}$")
    private val uniName = Regex("^(?:uni|u)([0-9A-Fa-f]{4,6})")

    fun winAnsi(code: Int): Char? =
        when (code) {
            in 32..126 -> code.toChar()
            in 0xA0..0xFF -> if (code == 0xA0) {
                ' '
            } else if (code == 0xAD) {
                '-'
            } else {
                code.toChar()
            }
            else -> HIGH[code]
        }

    /** Text for glyph [name], or `null` if the name means nothing to us (the caller falls back to the code). */
    fun toText(name: String): String? {
        byName[name]?.let { return it.toString() }
        uniName.find(name)?.let { return it.groupValues[1].toInt(16).toChar().toString() }
        hexName.matchEntire(name)?.let { return winAnsi(it.groupValues[1].toInt(16))?.toString() }
        decimalName.matchEntire(name)?.let { return winAnsi(name.toInt())?.toString() }
        // Suffixed variants such as "a.sc" or "one.oldstyle".
        val base = name.substringBefore('.')
        if (base != name) return byName[base]?.toString()
        return null
    }
}
