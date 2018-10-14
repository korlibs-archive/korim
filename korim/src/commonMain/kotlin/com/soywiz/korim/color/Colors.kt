package com.soywiz.korim.color

import com.soywiz.korio.lang.*

// Colors are in RGBAInt for easy transition to inline classes
object Colors {
	private fun RGB(r: Int, g: Int, b: Int, a: Int = 0xFF): RGBA = RGBA(r, g, b, a)
	//private fun RGB(r: Int, g: Int, b: Int, a: Int = 0xFF) = Color(r, g, b, a)

	val WHITE = RGB(0xFF, 0xFF, 0xFF, 0xFF)
	val BLACK = RGB(0x00, 0x00, 0x00, 0xFF)
	val RED = RGB(0xFF, 0x00, 0x00, 0xFF)
	val GREEN = RGB(0x00, 0xFF, 0x00, 0xFF)
	val BLUE = RGB(0x00, 0x00, 0xFF, 0xFF)

	val TRANSPARENT_BLACK = RGB(0x00, 0x00, 0x00, 0x00)
	val TRANSPARENT_WHITE = RGB(0x00, 0x00, 0x00, 0x00)

	val ALICEBLUE = RGB(240, 248, 255)
	val ANTIQUEWHITE = RGB(250, 235, 215)
	val AQUA = RGB(0, 255, 255)
	val AQUAMARINE = RGB(127, 255, 212)
	val AZURE = RGB(240, 255, 255)
	val BEIGE = RGB(245, 245, 220)
	val BISQUE = RGB(255, 228, 196)
	val BLANCHEDALMOND = RGB(255, 235, 205)
	val BLUEVIOLET = RGB(138, 43, 226)
	val BROWN = RGB(165, 42, 42)
	val BURLYWOOD = RGB(222, 184, 135)
	val CADETBLUE = RGB(95, 158, 160)
	val CHARTREUSE = RGB(127, 255, 0)
	val CHOCOLATE = RGB(210, 105, 30)
	val CORAL = RGB(255, 127, 80)
	val CORNFLOWERBLUE = RGB(100, 149, 237)
	val CORNSILK = RGB(255, 248, 220)
	val CRIMSON = RGB(220, 20, 60)
	val DARKBLUE = RGB(0, 0, 139)
	val DARKCYAN = RGB(0, 139, 139)
	val DARKGOLDENROD = RGB(184, 134, 11)
	val DARKGRAY = RGB(169, 169, 169)
	val DARKGREEN = RGB(0, 100, 0)
	val DARKGREY = RGB(169, 169, 169)
	val DARKKHAKI = RGB(189, 183, 107)
	val DARKMAGENTA = RGB(139, 0, 139)
	val DARKOLIVEGREEN = RGB(85, 107, 47)
	val DARKORANGE = RGB(255, 140, 0)
	val DARKORCHID = RGB(153, 50, 204)
	val DARKRED = RGB(139, 0, 0)
	val DARKSALMON = RGB(233, 150, 122)
	val DARKSEAGREEN = RGB(143, 188, 143)
	val DARKSLATEBLUE = RGB(72, 61, 139)
	val DARKSLATEGRAY = RGB(47, 79, 79)
	val DARKSLATEGREY = RGB(47, 79, 79)
	val DARKTURQUOISE = RGB(0, 206, 209)
	val DARKVIOLET = RGB(148, 0, 211)
	val DEEPPINK = RGB(255, 20, 147)
	val DEEPSKYBLUE = RGB(0, 191, 255)
	val DIMGRAY = RGB(105, 105, 105)
	val DIMGREY = RGB(105, 105, 105)
	val DODGERBLUE = RGB(30, 144, 255)
	val FIREBRICK = RGB(178, 34, 34)
	val FLORALWHITE = RGB(255, 250, 240)
	val FORESTGREEN = RGB(34, 139, 34)
	val FUCHSIA = RGB(255, 0, 255)
	val GAINSBORO = RGB(220, 220, 220)
	val GHOSTWHITE = RGB(248, 248, 255)
	val GOLD = RGB(255, 215, 0)
	val GOLDENROD = RGB(218, 165, 32)
	val GREENYELLOW = RGB(173, 255, 47)
	val HONEYDEW = RGB(240, 255, 240)
	val HOTPINK = RGB(255, 105, 180)
	val INDIANRED = RGB(205, 92, 92)
	val INDIGO = RGB(75, 0, 130)
	val IVORY = RGB(255, 255, 240)
	val KHAKI = RGB(240, 230, 140)
	val LAVENDER = RGB(230, 230, 250)
	val LAVENDERBLUSH = RGB(255, 240, 245)
	val LAWNGREEN = RGB(124, 252, 0)
	val LEMONCHIFFON = RGB(255, 250, 205)
	val LIGHTBLUE = RGB(173, 216, 230)
	val LIGHTCORAL = RGB(240, 128, 128)
	val LIGHTCYAN = RGB(224, 255, 255)
	val LIGHTGOLDENRODYELLOW = RGB(250, 250, 210)
	val LIGHTGRAY = RGB(211, 211, 211)
	val LIGHTGREEN = RGB(144, 238, 144)
	val LIGHTGREY = RGB(211, 211, 211)
	val LIGHTPINK = RGB(255, 182, 193)
	val LIGHTSALMON = RGB(255, 160, 122)
	val LIGHTSEAGREEN = RGB(32, 178, 170)
	val LIGHTSKYBLUE = RGB(135, 206, 250)
	val LIGHTSLATEGRAY = RGB(119, 136, 153)
	val LIGHTSLATEGREY = RGB(119, 136, 153)
	val LIGHTSTEELBLUE = RGB(176, 196, 222)
	val LIGHTYELLOW = RGB(255, 255, 224)
	val LIME = RGB(0, 255, 0)
	val LIMEGREEN = RGB(50, 205, 50)
	val LINEN = RGB(250, 240, 230)
	val MAROON = RGB(128, 0, 0)
	val MEDIUMAQUAMARINE = RGB(102, 205, 170)
	val MEDIUMBLUE = RGB(0, 0, 205)
	val MEDIUMORCHID = RGB(186, 85, 211)
	val MEDIUMPURPLE = RGB(147, 112, 219)
	val MEDIUMSEAGREEN = RGB(60, 179, 113)
	val MEDIUMSLATEBLUE = RGB(123, 104, 238)
	val MEDIUMSPRINGGREEN = RGB(0, 250, 154)
	val MEDIUMTURQUOISE = RGB(72, 209, 204)
	val MEDIUMVIOLETRED = RGB(199, 21, 133)
	val MIDNIGHTBLUE = RGB(25, 25, 112)
	val MINTCREAM = RGB(245, 255, 250)
	val MISTYROSE = RGB(255, 228, 225)
	val MOCCASIN = RGB(255, 228, 181)
	val NAVAJOWHITE = RGB(255, 222, 173)
	val NAVY = RGB(0, 0, 128)
	val OLDLACE = RGB(253, 245, 230)
	val OLIVE = RGB(128, 128, 0)
	val OLIVEDRAB = RGB(107, 142, 35)
	val ORANGE = RGB(255, 165, 0)
	val ORANGERED = RGB(255, 69, 0)
	val ORCHID = RGB(218, 112, 214)
	val PALEGOLDENROD = RGB(238, 232, 170)
	val PALEGREEN = RGB(152, 251, 152)
	val PALETURQUOISE = RGB(175, 238, 238)
	val PALEVIOLETRED = RGB(219, 112, 147)
	val PAPAYAWHIP = RGB(255, 239, 213)
	val PEACHPUFF = RGB(255, 218, 185)
	val PERU = RGB(205, 133, 63)
	val PINK = RGB(255, 192, 203)
	val PLUM = RGB(221, 160, 221)
	val POWDERBLUE = RGB(176, 224, 230)
	val PURPLE = RGB(128, 0, 128)
	val ROSYBROWN = RGB(188, 143, 143)
	val ROYALBLUE = RGB(65, 105, 225)
	val SADDLEBROWN = RGB(139, 69, 19)
	val SALMON = RGB(250, 128, 114)
	val SANDYBROWN = RGB(244, 164, 96)
	val SEAGREEN = RGB(46, 139, 87)
	val SEASHELL = RGB(255, 245, 238)
	val SIENNA = RGB(160, 82, 45)
	val SILVER = RGB(192, 192, 192)
	val SKYBLUE = RGB(135, 206, 235)
	val SLATEBLUE = RGB(106, 90, 205)
	val SLATEGRAY = RGB(112, 128, 144)
	val SLATEGREY = RGB(112, 128, 144)
	val SNOW = RGB(255, 250, 250)
	val SPRINGGREEN = RGB(0, 255, 127)
	val STEELBLUE = RGB(70, 130, 180)
	val TAN = RGB(210, 180, 140)
	val TEAL = RGB(0, 128, 128)
	val THISTLE = RGB(216, 191, 216)
	val TOMATO = RGB(255, 99, 71)
	val TURQUOISE = RGB(64, 224, 208)
	val VIOLET = RGB(238, 130, 238)
	val WHEAT = RGB(245, 222, 179)
	val WHITESMOKE = RGB(245, 245, 245)
	val YELLOWGREEN = RGB(154, 205, 50)
	val YELLOW = RGB(255, 255, 0)

	val colorsByName = mapOf(
		"black" to BLACK,
		"white" to WHITE,
		"red" to RED,
		"green" to GREEN,
		"blue" to BLUE,

		"aliceblue" to ALICEBLUE,
		"antiquewhite" to ANTIQUEWHITE,
		"aqua" to AQUA,
		"aquamarine" to AQUAMARINE,
		"azure" to AZURE,
		"beige" to BEIGE,
		"bisque" to BISQUE,
		"blanchedalmond" to BLANCHEDALMOND,
		"blueviolet" to BLUEVIOLET,
		"brown" to BROWN,
		"burlywood" to BURLYWOOD,
		"cadetblue" to CADETBLUE,
		"chartreuse" to CHARTREUSE,
		"chocolate" to CHOCOLATE,
		"coral" to CORAL,
		"cornflowerblue" to CORNFLOWERBLUE,
		"cornsilk" to CORNSILK,
		"crimson" to CRIMSON,
		"darkblue" to DARKBLUE,
		"darkcyan" to DARKCYAN,
		"darkgoldenrod" to DARKGOLDENROD,
		"darkgray" to DARKGRAY,
		"darkgreen" to DARKGREEN,
		"darkgrey" to DARKGREY,
		"darkkhaki" to DARKKHAKI,
		"darkmagenta" to DARKMAGENTA,
		"darkolivegreen" to DARKOLIVEGREEN,
		"darkorange" to DARKORANGE,
		"darkorchid" to DARKORCHID,
		"darkred" to DARKRED,
		"darksalmon" to DARKSALMON,
		"darkseagreen" to DARKSEAGREEN,
		"darkslateblue" to DARKSLATEBLUE,
		"darkslategray" to DARKSLATEGRAY,
		"darkslategrey" to DARKSLATEGREY,
		"darkturquoise" to DARKTURQUOISE,
		"darkviolet" to DARKVIOLET,
		"deeppink" to DEEPPINK,
		"deepskyblue" to DEEPSKYBLUE,
		"dimgray" to DIMGRAY,
		"dimgrey" to DIMGREY,
		"dodgerblue" to DODGERBLUE,
		"firebrick" to FIREBRICK,
		"floralwhite" to FLORALWHITE,
		"forestgreen" to FORESTGREEN,
		"fuchsia" to FUCHSIA,
		"gainsboro" to GAINSBORO,
		"ghostwhite" to GHOSTWHITE,
		"gold" to GOLD,
		"goldenrod" to GOLDENROD,
		"greenyellow" to GREENYELLOW,
		"honeydew" to HONEYDEW,
		"hotpink" to HOTPINK,
		"indianred" to INDIANRED,
		"indigo" to INDIGO,
		"ivory" to IVORY,
		"khaki" to KHAKI,
		"lavender" to LAVENDER,
		"lavenderblush" to LAVENDERBLUSH,
		"lawngreen" to LAWNGREEN,
		"lemonchiffon" to LEMONCHIFFON,
		"lightblue" to LIGHTBLUE,
		"lightcoral" to LIGHTCORAL,
		"lightcyan" to LIGHTCYAN,
		"lightgoldenrodyellow" to LIGHTGOLDENRODYELLOW,
		"lightgray" to LIGHTGRAY,
		"lightgreen" to LIGHTGREEN,
		"lightgrey" to LIGHTGREY,
		"lightpink" to LIGHTPINK,
		"lightsalmon" to LIGHTSALMON,
		"lightseagreen" to LIGHTSEAGREEN,
		"lightskyblue" to LIGHTSKYBLUE,
		"lightslategray" to LIGHTSLATEGRAY,
		"lightslategrey" to LIGHTSLATEGREY,
		"lightsteelblue" to LIGHTSTEELBLUE,
		"lightyellow" to LIGHTYELLOW,
		"lime" to LIME,
		"limegreen" to LIMEGREEN,
		"linen" to LINEN,
		"maroon" to MAROON,
		"mediumaquamarine" to MEDIUMAQUAMARINE,
		"mediumblue" to MEDIUMBLUE,
		"mediumorchid" to MEDIUMORCHID,
		"mediumpurple" to MEDIUMPURPLE,
		"mediumseagreen" to MEDIUMSEAGREEN,
		"mediumslateblue" to MEDIUMSLATEBLUE,
		"mediumspringgreen" to MEDIUMSPRINGGREEN,
		"mediumturquoise" to MEDIUMTURQUOISE,
		"mediumvioletred" to MEDIUMVIOLETRED,
		"midnightblue" to MIDNIGHTBLUE,
		"mintcream" to MINTCREAM,
		"mistyrose" to MISTYROSE,
		"moccasin" to MOCCASIN,
		"navajowhite" to NAVAJOWHITE,
		"navy" to NAVY,
		"oldlace" to OLDLACE,
		"olive" to OLIVE,
		"olivedrab" to OLIVEDRAB,
		"orange" to ORANGE,
		"orangered" to ORANGERED,
		"orchid" to ORCHID,
		"palegoldenrod" to PALEGOLDENROD,
		"palegreen" to PALEGREEN,
		"paleturquoise" to PALETURQUOISE,
		"palevioletred" to PALEVIOLETRED,
		"papayawhip" to PAPAYAWHIP,
		"peachpuff" to PEACHPUFF,
		"peru" to PERU,
		"pink" to PINK,
		"plum" to PLUM,
		"powderblue" to POWDERBLUE,
		"purple" to PURPLE,
		"rosybrown" to ROSYBROWN,
		"royalblue" to ROYALBLUE,
		"saddlebrown" to SADDLEBROWN,
		"salmon" to SALMON,
		"sandybrown" to SANDYBROWN,
		"seagreen" to SEAGREEN,
		"seashell" to SEASHELL,
		"sienna" to SIENNA,
		"silver" to SILVER,
		"skyblue" to SKYBLUE,
		"slateblue" to SLATEBLUE,
		"slategray" to SLATEGRAY,
		"slategrey" to SLATEGREY,
		"snow" to SNOW,
		"springgreen" to SPRINGGREEN,
		"steelblue" to STEELBLUE,
		"tan" to TAN,
		"teal" to TEAL,
		"thistle" to THISTLE,
		"tomato" to TOMATO,
		"turquoise" to TURQUOISE,
		"violet" to VIOLET,
		"wheat" to WHEAT,
		"whitesmoke" to WHITESMOKE,
		"yellowgreen" to YELLOWGREEN,
		"yellow" to YELLOW
	)

	operator fun get(str: String): RGBA = get(str, Colors.TRANSPARENT_BLACK, errorOnDefault = true)

	operator fun get(str: String, default: RGBA, errorOnDefault: Boolean = false): RGBA {
		when {
			str.startsWith("#") -> {
				val hex = str.substr(1)
				if (hex.length !in setOf(3, 4, 6, 8)) return BLACK
				val chars = if (hex.length < 6) 1 else 2
				val scale = if (hex.length < 6) (255.0 / 15.0) else 1.0
				val hasAlpha = (hex.length / chars) >= 4
				val r = (hex.substr(0 * chars, chars).toInt(0x10) * scale).toInt()
				val g = (hex.substr(1 * chars, chars).toInt(0x10) * scale).toInt()
				val b = (hex.substr(2 * chars, chars).toInt(0x10) * scale).toInt()
				val a = if (hasAlpha) (hex.substr(3 * chars, chars).toInt(0x10) * scale).toInt() else 0xFF
				return RGBA(r, g, b, a)
			}
			str.startsWith("RGBA(", ignoreCase = true) -> {
				val parts = str.toUpperCase().removePrefix("RGBA(").removeSuffix(")").split(",")
				val r = parts.getOrElse(0) { "0" }.toIntOrNull() ?: 0
				val g = parts.getOrElse(1) { "0" }.toIntOrNull() ?: 0
				val b = parts.getOrElse(2) { "0" }.toIntOrNull() ?: 0
				val af = parts.getOrElse(3) { "1.0" }.toDoubleOrNull() ?: 1.0
				return RGBA(r, g, b, (af * 255).toInt())
			}
			else -> {
				val col = colorsByName[str.toLowerCase()]
				if (col == null && errorOnDefault) error("Unsupported color '$str'")
				return col ?: default
			}
		}
	}

	fun toHtmlString(color: Int) =
		"RGBA(" + RGBA.getR(color) + "," + RGBA.getG(color) + "," + RGBA.getB(color) + "," + RGBA.getAf(color) + ")"

	fun toHtmlStringSimple(color: Int) = "#%02x%02x%02x".format(RGBA.getR(color), RGBA.getG(color), RGBA.getB(color))

	object Default {
		operator fun get(str: String): RGBA = get(str, default = Colors.RED)
	}
}
