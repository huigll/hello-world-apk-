package com.example.helloworld

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.util.Log
import java.io.File

/**
 * Very small wrapper around AOSP PinyinIME native decoder.
 *
 * We use it ONLY to get candidate strings for a pinyin buffer.
 */
class PinyinDecoder(private val context: Context) {

    private var inited = false

    companion object {
        init {
            try {
                System.loadLibrary("jni_pinyinime")
            } catch (t: Throwable) {
                Log.e("PinyinDecoder", "Failed to load native library jni_pinyinime", t)
            }
        }

        @JvmStatic external fun nativeImOpenDecoderFd(
            fd: java.io.FileDescriptor,
            startOffset: Long,
            length: Long,
            usrDictPathBytes: ByteArray,
        ): Boolean

        @JvmStatic external fun nativeImSetMaxLens(maxSpsLen: Int, maxHzsLen: Int)
        @JvmStatic external fun nativeImCloseDecoder(): Boolean
        @JvmStatic external fun nativeImResetSearch()
        @JvmStatic external fun nativeImSearch(pyBuf: ByteArray, pyLen: Int): Int
        @JvmStatic external fun nativeImGetChoice(choiceId: Int): String
        @JvmStatic external fun nativeImChoose(choiceId: Int): Int
    }

    fun initIfNeeded() {
        if (inited) return

        val usr = File(context.filesDir, "usr_dict.dat")
        if (!usr.exists()) usr.writeBytes(byteArrayOf())

        val afd: AssetFileDescriptor = context.resources.openRawResourceFd(R.raw.dict_pinyin)
        val ok = nativeImOpenDecoderFd(
            afd.fileDescriptor,
            afd.startOffset,
            afd.length,
            (usr.absolutePath + "\u0000").encodeToByteArray()
        )
        afd.close()

        if (ok) {
            // Reasonable caps
            nativeImSetMaxLens(64, 64)
            inited = true
        } else {
            Log.e("PinyinDecoder", "nativeImOpenDecoderFd failed")
        }
    }

    fun reset() {
        if (!inited) return
        nativeImResetSearch()
    }

    /** Returns list of candidates (0..max-1). */
    fun candidates(pinyin: String, max: Int = 10): List<String> {
        initIfNeeded()
        if (!inited) return emptyList()

        val bytes = pinyin.encodeToByteArray()
        nativeImSearch(bytes + byteArrayOf(0), bytes.size)

        val out = ArrayList<String>(max)
        for (i in 0 until max) {
            val c = nativeImGetChoice(i)
            if (c.isBlank()) break
            out.add(c)
        }
        return out
    }

    fun choose(index: Int): String {
        initIfNeeded()
        if (!inited) return ""

        val chosen = nativeImGetChoice(index)
        nativeImChoose(index)
        return chosen
    }

    fun close() {
        if (!inited) return
        nativeImCloseDecoder()
        inited = false
    }
}
