package com.soywiz.korim

import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.*

val MyResourcesVfs = when {
    OS.isJs -> {
        if (OS.isJsNodeJs)  {
            localCurrentDirVfs["src/commonTest/resources"]
        } else {
            localCurrentDirVfs
        }
    }
    OS.isNative -> localCurrentDirVfs["../../../../../../src/commonTest/resources"]
    else -> ResourcesVfs
}
