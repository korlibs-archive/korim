package com.soywiz.korim.font

actual val nativeSystemFontProvider: NativeSystemFontProvider = FallbackNativeSystemFontProvider(DefaultTtfFont)
