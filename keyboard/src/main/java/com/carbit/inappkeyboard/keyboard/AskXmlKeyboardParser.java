package com.carbit.inappkeyboard.keyboard;

import android.content.res.AssetManager;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
public final class AskXmlKeyboardParser {
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    private static final String TAG = "AskXmlKeyboardParser";
    private static final boolean DEBUG = true;
    private static int debugLoggedKeys = 0;

    public static final class Key {
        public final Integer code;
        public final String label;
        public final boolean isModifier;
        public final boolean isSticky;
        public final boolean isRepeatable;

        public Key(Integer code, String label, boolean isModifier, boolean isSticky, boolean isRepeatable) {
            this.code = code;
            this.label = label;
            this.isModifier = isModifier;
            this.isSticky = isSticky;
            this.isRepeatable = isRepeatable;
        }
    }

    public static final class Layout {
        public final List<List<Key>> rows;

        public Layout(List<List<Key>> rows) {
            this.rows = rows;
        }
    }

    private AskXmlKeyboardParser() {
    }

    public static Layout parseAsset(AssetManager assetManager, String assetPath) throws IOException, XmlPullParserException {
        InputStream input = null;
        try {
            input = assetManager.open(assetPath);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(input, "utf-8");
            parser.nextTag();
            return parseKeyboard(parser);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static void requireStartTag(XmlPullParser parser, String name) throws XmlPullParserException {
        if (parser.getEventType() != XmlPullParser.START_TAG || !name.equals(parser.getName())) {
            throw new XmlPullParserException("Expected START_TAG " + name);
        }
    }

    private static Layout parseKeyboard(XmlPullParser parser) throws IOException, XmlPullParserException {
        requireStartTag(parser, "Keyboard");
        List<List<Key>> rows = new ArrayList<>();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            if ("Row".equals(parser.getName())) {
                rows.add(parseRow(parser));
            } else {
                skip(parser);
            }
        }
        return new Layout(rows);
    }

    private static List<Key> parseRow(XmlPullParser parser) throws IOException, XmlPullParserException {
        requireStartTag(parser, "Row");
        List<Key> keys = new ArrayList<>();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            if ("Key".equals(parser.getName())) {
                keys.add(parseKey(parser));
            } else {
                skip(parser);
            }
        }
        return keys;
    }

    private static String attr(XmlPullParser parser, String nameSuffix) {
        String direct = parser.getAttributeValue(ANDROID_NS, nameSuffix);
        if (direct != null) return direct;
        direct = parser.getAttributeValue(null, nameSuffix);
        if (direct != null) return direct;
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String name = parser.getAttributeName(i);
            if (nameSuffix.equals(name) || name.endsWith(":" + nameSuffix)) {
                return parser.getAttributeValue(i);
            }
        }
        return null;
    }

    private static Key parseKey(XmlPullParser parser) throws IOException, XmlPullParserException {
        requireStartTag(parser, "Key");

        String codesStr = attr(parser, "codes");
        Integer code = null;
        if (codesStr != null && !codesStr.isEmpty()) {
            String[] parts = codesStr.split(",");
            if (parts.length > 0) {
                String first = parts[0].trim();
                if (first.startsWith("@integer/")) {
                    if (first.contains("key_code_shift")) {
                        code = -1;
                    } else if (first.contains("key_code_delete")) {
                        code = -5;
                    }
                } else {
                    try {
                        code = Integer.parseInt(first);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        String label = attr(parser, "keyLabel");

        boolean isModifier = parseBoolean(attr(parser, "isModifier"), false);
        boolean isSticky = parseBoolean(attr(parser, "isSticky"), false);
        boolean isRepeatable = parseBoolean(attr(parser, "isRepeatable"), false);

        if (DEBUG && (codesStr == null && label == null) && debugLoggedKeys < 5) {
            debugLoggedKeys++;
            StringBuilder sb = new StringBuilder();
            sb.append("Key attrs:");
            for (int i = 0; i < parser.getAttributeCount(); i++) {
                sb.append(" ").append(parser.getAttributeName(i))
                  .append("=").append(parser.getAttributeValue(i));
            }
            Log.d(TAG, sb.toString());
        }
//        Log.d(TAG, "Parsed Key: code=" + code + " label=" + label +
//                " isModifier=" + isModifier +
//                " isSticky=" + isSticky +
//                " debugLoggedKeys=" + debugLoggedKeys);

        int next = parser.next();
        if (next != XmlPullParser.END_TAG || !"Key".equals(parser.getName())) {
            while (parser.getEventType() != XmlPullParser.END_TAG || !"Key".equals(parser.getName())) {
                if (parser.getEventType() == XmlPullParser.START_TAG) skip(parser);
                else parser.next();
            }
        }

        return new Key(code, label, isModifier, isSticky, isRepeatable);
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null) return defaultValue;
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        return defaultValue;
    }

    private static void skip(XmlPullParser parser) throws IOException, XmlPullParserException {
        if (parser.getEventType() != XmlPullParser.START_TAG) return;
        int depth = 1;
        while (depth != 0) {
            int next = parser.next();
            if (next == XmlPullParser.END_TAG) depth--;
            else if (next == XmlPullParser.START_TAG) depth++;
        }
    }
}
