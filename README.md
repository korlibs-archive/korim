## Korim: Kotlin cORoutines IMaging utilities depending on Korio for JVM, Kotlin-JS, Android, Jtransc+Node.JS and Jtransc+Browser

[![Build Status](https://travis-ci.org/soywiz/korim.svg?branch=master)](https://travis-ci.org/soywiz/korim)
[![Maven Version](https://img.shields.io/github/tag/soywiz/korim.svg?style=flat&label=maven)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22korim%22)

[KORIO](http://github.com/soywiz/korio) - [KORIM](http://github.com/soywiz/korim) - [KORUI](http://github.com/soywiz/korui)

Use with gradle:

```
compile "com.soywiz:korim:korimVersion"
```

I'm uploading it to bintray and maven central:

For bintray:
```
maven { url "https://dl.bintray.com/soywiz/soywiz-maven" }
```

### Bitmap classes

Bitmap base class + Bitmap8 and Bitmap32

### Image Formats

Korim provides utilities for reading and writing some image formats without any kind of additional dependency.

BMP, JPG, PNG and TGA.

### Color Formats

Korim provides color formats to convert easily and fast.

### AWT Utilities

Korim provides AWT utilities to convert bitmaps into AWT BufferedImages, and to display them.
These are just extensions so not referenced from the main code.

### Korio integration

Korim provides korio integration adding `VfsFile.readBitmap()` that allows Bitmap reading easily
and faster (with native implementations) in some targets like browsers.
