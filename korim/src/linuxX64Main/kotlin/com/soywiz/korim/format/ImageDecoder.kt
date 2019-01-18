package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.native.concurrent.*

private val ImageIOWorker by lazy { Worker.start() }

actual val nativeImageFormatProvider: NativeImageFormatProvider = object : BaseNativeNativeImageFormatProvider() {
    override suspend fun decode(data: ByteArray): NativeImage = wrapNative(
        ImageIOWorker.execute(
            TransferMode.SAFE,
            { if (data.isFrozen) data else data.copyOf().freeze() },
            { data ->
                data.usePinned { dataPin ->
                    memScoped {
                        val width = alloc<IntVar>()
                        val height = alloc<IntVar>()
                        val comp = alloc<IntVar>()
                        //val success = stbi_info_from_memory(pin.addressOf(0).reinterpret(), data.size, width.ptr, height.ptr, comp.ptr) != 0

                        val pixelsPtr = stb_image.stbi_load_from_memory(dataPin.addressOf(0).reinterpret(), data.size, width.ptr, height.ptr, comp.ptr, 4)
                        if (pixelsPtr != null) {
                            val bmp = Bitmap32(width.value, height.value)
                            bmp.data.array.usePinned { pixelsPin ->
                                memcpy(pixelsPin.addressOf(0), pixelsPtr, (width.value * height.value * comp.value).convert())
                            }
                            stb_image.stbi_image_free(pixelsPtr)
                            bmp
                        } else {
                            null
                        }
                    }
                } ?: throw IOException("Failed to decode image using stbi_load_from_memory")
            }
        ).await()
    )
}

