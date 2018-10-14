package com.soywiz.korim.format.jpg

import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.*

val MyResourcesVfs = when {
    OS.isJs -> localCurrentDirVfs["src/commonTest/resources"]
    OS.isNative -> localCurrentDirVfs["../../../../../../src/commonTest/resources"]
    else -> ResourcesVfs
}
