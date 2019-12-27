<p align="center"><img alt="Korim" src="https://raw.githubusercontent.com/korlibs/korlibs-logos/master/128/korim.png" /></p>

<h1 align="center">Korim</h1>

<p align="center">Kotlin cORoutines IMaging utilities for Multiplatform Kotlin</p>

<!-- BADGES -->
<p align="center">
	<a href="https://github.com/korlibs/korim/actions"><img alt="Build Status" src="https://github.com/korlibs/korim/workflows/CI/badge.svg" /></a>
	<a href="https://bintray.com/korlibs/korlibs/korim"><img alt="Maven Version" src="https://img.shields.io/bintray/v/korlibs/korlibs/korim.svg?style=flat&label=maven" /></a>
	<a href="https://slack.soywiz.com/"><img alt="Slack" src="https://img.shields.io/badge/chat-on%20slack-green?style=flat&logo=slack" /></a>
</p>
<!-- /BADGES -->

<!-- SUPPORT -->
<h2 align="center">Support korim</h2>
<p align="center">
If you like korim, or want your company logo here, please consider <a href="https://github.com/sponsors/soywiz">becoming a sponsor ★</a>,<br />
in addition to ensure the continuity of the project, you will get exclusive content.
</p>
<!-- /SUPPORT -->

[All KOR libraries](https://github.com/korlibs/kor)

Use with gradle:

```
compile "com.soywiz.korlibs.korim:korim:$korVersion"
```

### Bitmap classes

Bitmap base class + Bitmap8 and Bitmap32.
And other fancy bitmaps: BitmapIndexed as base + Bitmap1, Bitmap2, Bitmap4
Ad BitmapChannel

### Image Formats

Korim provides utilities for reading and writing some image formats without any kind of additional dependency.

PNG, JPG, TGA, BMP, ICO, PSD(WIP) and DDS (DXT1, DXT2, DXT3, DXT4 and DXT5).

### Native Image Formats

Korim also allows to use native image readers from your device for maximum performance for standard image formats.

### Color Formats

Korim provides color formats to convert easily and fast and to perform, mixing, de/premultiplication and other operations quickly.

### Vectorial Image Formats

Korim supports loading, rasterizing and drawing vectorial SVG files.

### Native vectorial rendering

It provides a single interface for vector rendering so you can use a single interface
and leverage JavaScript Canvas, AWT's Graphics2D, Android Canvas or any other rasterizer exposed by korim implementations.
It also allows to convert shapes into SVG.
Includes a feature to draw shapes with fills in contact without artifacts in a portable way by multisampling.
Useful for offline rasterizers.

### AWT Utilities

Korim provides AWT utilities to convert bitmaps into AWT BufferedImages, and to display them.
These are just extensions so not referenced from the main code.
And if you use native image loading, you can display those images as fast as possible without any conversion at all.

### Native Fonts

Korim provides native font rendering. You can rasterize glyph fonts on all targets without
actually including any font, using device fonts.

### TTF Reading and rendering

Korim provides a pure Kotlin-Common TTF reader, and using native vectorial rendering allows you to
render glyphs, texts and to get advanced font metrics.

### Korio integration

Korim provides korio integration adding `VfsFile.readBitmap()` that allows Bitmap reading easily
and faster (with native implementations) in some targets like browsers.
