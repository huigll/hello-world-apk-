package com.example.keyboard

import android.content.res.AssetManager
import android.util.Xml
import org.xmlpull.v1.XmlPullParser

/**
 * Parses AnySoftKeyboard-style XML keyboard layouts.
 *
 * We only need a small subset:
 * <Keyboard>
 *   <Row>
 *     <Key android:codes="113" android:keyLabel="q" />
 *   </Row>
 * </Keyboard>
 */
object AskXmlKeyboardParser {

    data class Key(
        val code: Int?,
        val label: String?,
        val isModifier: Boolean,
        val isSticky: Boolean,
        val isRepeatable: Boolean,
    )

    data class Layout(val rows: List<List<Key>>)

    fun parseAsset(assetManager: AssetManager, assetPath: String): Layout {
        assetManager.open(assetPath).use { input ->
            val parser = Xml.newPullParser()
            parser.setInput(input, "utf-8")
            parser.nextTag()
            return parseKeyboard(parser)
        }
    }

    private fun parseKeyboard(parser: XmlPullParser): Layout {
        parser.require(XmlPullParser.START_TAG, null, "Keyboard")
        val rows = mutableListOf<List<Key>>()

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "Row" -> rows.add(parseRow(parser))
                else -> skip(parser)
            }
        }
        return Layout(rows)
    }

    private fun parseRow(parser: XmlPullParser): List<Key> {
        parser.require(XmlPullParser.START_TAG, null, "Row")
        val keys = mutableListOf<Key>()

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "Key" -> keys.add(parseKey(parser))
                else -> skip(parser)
            }
        }
        return keys
    }

    private fun parseKey(parser: XmlPullParser): Key {
        parser.require(XmlPullParser.START_TAG, null, "Key")

        // Attribute names may be namespaced; we match by local-name suffix.
        fun attr(nameSuffix: String): String? {
            for (i in 0 until parser.attributeCount) {
                val n = parser.getAttributeName(i)
                if (n == nameSuffix) return parser.getAttributeValue(i)
            }
            return null
        }

        val codes = attr("codes")
        val code = codes
            ?.split(',')
            ?.firstOrNull()
            ?.trim()
            ?.toIntOrNull()

        val label = attr("keyLabel")

        val isModifier = attr("isModifier")?.toBooleanStrictOrNull() ?: false
        val isSticky = attr("isSticky")?.toBooleanStrictOrNull() ?: false
        val isRepeatable = attr("isRepeatable")?.toBooleanStrictOrNull() ?: false

        // Key is an empty tag in ASK layouts; still handle nested safely.
        if (parser.isEmptyElementTag.not()) {
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType == XmlPullParser.START_TAG) skip(parser)
            }
        } else {
            parser.next()
        }

        return Key(
            code = code,
            label = label,
            isModifier = isModifier,
            isSticky = isSticky,
            isRepeatable = isRepeatable,
        )
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) return
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}
