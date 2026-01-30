package com.example.keyboard

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.util.Log
import java.io.File

/**
 * Very small wrapper around AOSP PinyinIME native decoder.
 *
 * We use it ONLY to get candidate strings for a pinyin buffer.
 */
interface IPinyinDecoder {
    fun reset()
    fun candidates(pinyin: String, max: Int = 10): List<String>
    fun choose(index: Int): String
}

class PinyinDecoder(private val context: Context) : IPinyinDecoder { 

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

        // On some builds/devices, res/raw resources may be compressed in the APK.
        // In that case openRawResourceFd() will throw ("probably compressed").
        // We then fall back to extracting the dict into filesDir and opening via fd.
        val ok = try {
            val afd: AssetFileDescriptor = context.resources.openRawResourceFd(R.raw.dict_pinyin)
            val ret = nativeImOpenDecoderFd(
                afd.fileDescriptor,
                afd.startOffset,
                afd.length,
                (usr.absolutePath + "\u0000").encodeToByteArray()
            )
            afd.close()
            ret
        } catch (t: Throwable) {
            Log.w("PinyinDecoder", "openRawResourceFd failed (likely compressed). Falling back to extracted file.", t)

            val dictFile = File(context.filesDir, "dict_pinyin.dat")
            if (!dictFile.exists() || dictFile.length() == 0L) {
                context.resources.openRawResource(R.raw.dict_pinyin).use { input ->
                    dictFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            val pfd = android.os.ParcelFileDescriptor.open(dictFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val ret = nativeImOpenDecoderFd(
                pfd.fileDescriptor,
                0L,
                dictFile.length(),
                (usr.absolutePath + "\u0000").encodeToByteArray()
            )
            pfd.close()
            ret
        }

        if (ok) {
            nativeImSetMaxLens(64, 64)
            inited = true
        } else {
            Log.e("PinyinDecoder", "Failed to initialize pinyin decoder")
        }
    }

    override fun reset() {
        if (!inited) return
        nativeImResetSearch()
    }

    /** Returns list of candidates (0..max-1). */
    override fun candidates(pinyin: String, max: Int): List<String> {
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

    override fun choose(index: Int): String {
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
