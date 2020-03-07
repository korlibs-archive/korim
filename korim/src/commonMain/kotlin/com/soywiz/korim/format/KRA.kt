package com.soywiz.korim.format

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.stream.*

object KRA : ImageFormat("kra") {
    private val mergedImagePng = "mergedimage.png"

    override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
        /*
        var out: ImageInfo? = null
        runBlockingNoSuspensions {
            val vfs = ZipVfs(s.toAsync())
            out = PNG.decodeHeader(vfs[mergedImagePng].readChunk(0L, 128).openSync(), props)
        }
        return out
         */
        return ImageInfo().apply {
            width = 1
            height = 1
            bitsPerPixel = 8
        }
    }

    override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
        return runBlockingNoSuspensions {
            val vfs = ZipVfs(s.readAll().openAsync())
            PNG.readImage(vfs[mergedImagePng].readAll().openSync())
        }
    }
}
