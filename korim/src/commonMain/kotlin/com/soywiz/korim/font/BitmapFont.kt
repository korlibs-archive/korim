package com.soywiz.korim.font

import com.soywiz.kds.Extra
import com.soywiz.kds.IntMap
import com.soywiz.kds.toIntMap
import com.soywiz.klock.measureTimeWithResult
import com.soywiz.kmem.insert
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.ImageFormat
import com.soywiz.korim.format.RegisteredImageFormats
import com.soywiz.korim.format.readBitmapSlice
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.HorizontalAlign
import com.soywiz.korim.vector.VerticalAlign
import com.soywiz.korim.vector.paint.DefaultPaint
import com.soywiz.korio.dynamic.KDynamic
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.lang.String_fromIntArray
import com.soywiz.korio.lang.substr
import com.soywiz.korio.serialization.xml.Xml
import com.soywiz.korio.serialization.xml.get
import com.soywiz.korio.util.unquote
import com.soywiz.korma.geom.RectangleInt
import com.soywiz.korma.geom.setTo
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class BitmapFont(
    val fontSize: Int,
    val lineHeight: Int,
    val base: Int,
    val glyphs: IntMap<Glyph>,
    val kernings: IntMap<Kerning>,
    val atlas: Bitmap = glyphs.values.iterator().next()?.texture?.bmp ?: Bitmaps.transparent.bmp,
    override val name: String = "BitmapFont"
) : Extra by Extra.Mixin(), Font {
    override fun getFontMetrics(size: Double, metrics: FontMetrics): FontMetrics = metrics.also {
        val scale = getTextScale(size)
        it.size = size
        it.top = base.toDouble() * scale
        it.ascent = it.top * scale
        it.baseline = 0.0
        it.descent = (base - lineHeight).toDouble() * scale // No descent information
        it.bottom = it.descent * scale
        it.leading = 0.0
    }

    override fun getGlyphMetrics(size: Double, codePoint: Int, metrics: GlyphMetrics): GlyphMetrics = metrics.also {
        val scale = getTextScale(size)
        val glyph = glyphs[codePoint]
        it.existing = glyph != null
        it.codePoint = codePoint
        it.xadvance = 0.0
        it.bounds.setTo(0, 0, 0, 0)
        if (glyph != null) {
            it.xadvance = glyph.xadvance.toDouble() * scale
            it.bounds.setTo(
                glyph.xoffset * scale, glyph.yoffset * scale,
                glyph.texture.width * scale, glyph.texture.height * scale
            )
        }
    }

    override fun getKerning(size: Double, leftCodePoint: Int, rightCodePoint: Int): Double =
        getTextScale(size) * (getKerning(leftCodePoint, rightCodePoint)?.amount?.toDouble() ?: 0.0)

    override fun renderGlyph(ctx: Context2d, size: Double, codePoint: Int, x: Double, y: Double, fill: Boolean, metrics: GlyphMetrics) {
        val scale = getTextScale(size)
        val g = glyphs[codePoint] ?: return
        val metrics = getGlyphMetrics(size, codePoint, metrics).takeIf { it.existing } ?: return
        if (metrics.width == 0.0 && metrics.height == 0.0) return
        val bmpAlpha = Bitmap32(metrics.width.toInt(), metrics.height.toInt())
        bmpAlpha.draw(g.bmp, (x - metrics.left).toInt(), (y - metrics.top).toInt())
        //println("SCALE: $scale")
        val texX = x + (metrics.left - ctx.horizontalAlign.getOffsetX(metrics.width)) * scale
        val texY = y + (metrics.top - ctx.verticalAlign.getOffsetY(metrics.height, this.base.toDouble())) * scale

        //println("texX: $texX, texY: $texY")

        if (ctx.fillStyle == DefaultPaint) {
            ctx.drawImage(bmpAlpha, texX, texY, metrics.width * scale, metrics.height * scale)
        } else {
            val bmpFill = Bitmap32(metrics.width.toInt(), metrics.height.toInt())
            bmpFill.context2d {
                this.keepTransform {
                    this.scale(1.0 / scale)
                    this.fillStyle = ctx.fillStyle
                    fillRect(0, 0, width * scale, height * scale)
                }
            }
            bmpFill.writeChannel(BitmapChannel.ALPHA, bmpAlpha, BitmapChannel.ALPHA)
            ctx.drawImage(bmpFill, texX, texY, metrics.width * scale, metrics.height * scale)
        }
    }

    private fun getTextScale(size: Double) = size.toDouble() / fontSize.toDouble()

	fun measureWidth(text: String): Int {
		var x = 0
		for (c in text) {
			val glyph = glyphs[c.toInt()]
			if (glyph != null) x += glyph.xadvance
		}
		return x
	}

	fun getKerning(first: Char, second: Char): Kerning? = getKerning(first.toInt(), second.toInt())
	fun getKerning(first: Int, second: Int): Kerning? = kernings[Kerning.buildKey(first, second)]

	class Kerning(
		val first: Int,
		val second: Int,
		val amount: Int
	) {
		companion object {
			fun buildKey(f: Int, s: Int) = 0.insert(f, 0, 16).insert(s, 16, 16)
		}
	}

	class Glyph(
		val id: Int,
		val texture: BitmapSlice<Bitmap>,
		val xoffset: Int,
		val yoffset: Int,
		val xadvance: Int
	) {
        val bmp = texture.extract().toBMP32()
    }

	val dummyGlyph by lazy { Glyph(-1, Bitmaps.transparent, 0, 0, 0) }
	val anyGlyph: Glyph by lazy { glyphs[glyphs.keys.iterator().next()] ?: dummyGlyph }
	val baseBmp: Bitmap by lazy { anyGlyph.texture.bmp }

	operator fun get(charCode: Int): Glyph = glyphs[charCode] ?: glyphs[32] ?: dummyGlyph
	operator fun get(char: Char): Glyph = this[char.toInt()]

	companion object {
        /**
         * Creates a new [BitmapFont] of [fontSize] using an existing [Font] ([SystemFont] is valid).
         * Just creates the glyphs specified in [chars].
         * Allows to set a different [fontName] than the one provided at [Font].
         */
        operator fun invoke(
            font: Font,
            fontSize: Number,
            chars: CharacterSet = CharacterSet.LATIN_ALL,
            fontName: String = font.name,
            mipmaps: Boolean = true
        ): BitmapFont {
            val fontSize = fontSize.toDouble()
            val result = measureTimeWithResult {
                val bni = NativeImage(1, 1)
                val bnictx = bni.getContext2d()
                bnictx.font = font
                bnictx.fontSize = fontSize
                val bitmapHeight = bnictx.getTextBounds("a").bounds.height.toInt()

                val widths: List<Int> = chars.codePoints.map { bnictx.getTextBounds("${it.toChar()}").bounds.width.toInt() }
                val widthsSum = widths.map { it + 2 }.sum()
                val ni = NativeImage(widthsSum, bitmapHeight)

                class GlyphInfo(val char: Int, val rect: RectangleInt, val width: Int)

                val g = ni.getContext2d()
                g.fillStyle = g.createColor(Colors.WHITE)
                g.fontSize = fontSize
                g.font = font
                g.horizontalAlign = HorizontalAlign.LEFT
                g.verticalAlign = VerticalAlign.TOP
                val glyphsInfo = arrayListOf<GlyphInfo>()
                var x = 0
                val itemp = IntArray(1)
                for ((index, char) in chars.codePoints.withIndex()) {
                    val width = widths[index]
                    itemp[0] = char
                    g.fillText(String_fromIntArray(itemp, 0, 1), x.toDouble(), 0.0)
                    glyphsInfo += GlyphInfo(char, RectangleInt(x, 0, width, ni.height), width)
                    x += width + 2
                }

                val atlas = ni.toBMP32()

                BitmapFont(
                    fontSize.toInt(), fontSize.toInt(), fontSize.toInt(),
                    glyphsInfo.associate {
                        it.char to Glyph(it.char, atlas.slice(it.rect), 0, 0, it.width)
                    }.toIntMap(),
                    IntMap(),
                    atlas = atlas,
                    name = fontName
                )
            }

            return result.result
        }
	}
}

suspend fun VfsFile.readBitmapFont(imageFormat: ImageFormat = RegisteredImageFormats): BitmapFont {
	val fntFile = this
	val content = fntFile.readString().trim()
	val textures = hashMapOf<Int, BitmapSlice<Bitmap>>()

    return when {
        content.startsWith('<') -> readBitmapFontXml(content, fntFile, textures, imageFormat)
        content.startsWith("info") -> readBitmapFontTxt(content, fntFile, textures, imageFormat)
        else -> TODO("Unsupported font type starting with ${content.substr(0, 16)}")
    }
}

private suspend fun readBitmapFontTxt(
	content: String,
	fntFile: VfsFile,
	textures: HashMap<Int, BitmapSlice<Bitmap>>,
	imageFormat: ImageFormat = RegisteredImageFormats
): BitmapFont {
    val kernings = arrayListOf<BitmapFont.Kerning>()
	val glyphs = arrayListOf<BitmapFont.Glyph>()
	var lineHeight = 16
	var fontSize: Int? = null
	var base: Int? = null
	for (rline in content.lines()) {
		val line = rline.trim()
		val map = LinkedHashMap<String, String>()
		for (part in line.split(' ')) {
			val (key, value) = part.split('=') + listOf("", "")
			map[key] = value
		}
		when {
			line.startsWith("info") -> {
				fontSize = map["size"]?.toInt()
			}
			line.startsWith("page") -> {
				val id = map["id"]?.toInt() ?: 0
				val file = map["file"]?.unquote() ?: error("page without file")
				textures[id] = fntFile.parent[file].readBitmapSlice(imageFormat)
			}
			line.startsWith("common ") -> {
				lineHeight = map["lineHeight"]?.toIntOrNull() ?: 16
				base = map["base"]?.toIntOrNull()
			}
			line.startsWith("char ") -> {
				//id=54 x=158 y=88 width=28 height=42 xoffset=2 yoffset=8 xadvance=28 page=0 chnl=0
				val page = map["page"]?.toIntOrNull() ?: 0
				val texture = textures[page] ?: textures.values.first()
				glyphs += KDynamic {
                    BitmapFont.Glyph(
                        id = map["id"].int,
                        xoffset = map["xoffset"].int,
                        yoffset = map["yoffset"].int,
                        xadvance = map["xadvance"].int,
                        texture = texture.sliceWithSize(map["x"].int, map["y"].int, map["width"].int, map["height"].int)
                    )
				}
			}
			line.startsWith("kerning ") -> {
				kernings += BitmapFont.Kerning(
                    first = map["first"]?.toIntOrNull() ?: 0,
                    second = map["second"]?.toIntOrNull() ?: 0,
                    amount = map["amount"]?.toIntOrNull() ?: 0
                )
			}
		}
	}
	return BitmapFont(
        atlas = textures.values.first().bmp,
        fontSize = fontSize ?: 16,
        lineHeight = lineHeight,
        base = base ?: lineHeight,
        glyphs = glyphs.map { it.id to it }.toMap().toIntMap(),
        kernings = kernings.map {
            BitmapFont.Kerning.buildKey(
                it.first,
                it.second
            ) to it
        }.toMap().toIntMap()
    )
}

private suspend fun readBitmapFontXml(
	content: String,
	fntFile: VfsFile,
	textures: MutableMap<Int, BitmapSlice<Bitmap>>,
    imageFormat: ImageFormat = RegisteredImageFormats
): BitmapFont {
	val xml = Xml(content)

	val fontSize = xml["info"].firstOrNull()?.int("size", 16) ?: 16
	val lineHeight = xml["common"].firstOrNull()?.int("lineHeight", 16) ?: 16
	val base = xml["common"].firstOrNull()?.int("base", 16) ?: 16

	for (page in xml["pages"]["page"]) {
		val id = page.int("id")
		val file = page.str("file")
		val texFile = fntFile.parent[file]
		val tex = texFile.readBitmapSlice(imageFormat)
		textures[id] = tex
	}

	val glyphs = xml["chars"]["char"].map {
		val page = it.int("page")
		val texture = textures[page] ?: textures.values.first()
        BitmapFont.Glyph(
            id = it.int("id"),
            texture = texture.sliceWithSize(it.int("x"), it.int("y"), it.int("width"), it.int("height")),
            xoffset = it.int("xoffset"),
            yoffset = it.int("yoffset"),
            xadvance = it.int("xadvance")
        )
	}

	val kernings = xml["kernings"]["kerning"].map {
        BitmapFont.Kerning(
            first = it.int("first"),
            second = it.int("second"),
            amount = it.int("amount")
        )
	}

	return BitmapFont(
        atlas = textures.values.first().bmp,
        fontSize = fontSize,
        lineHeight = lineHeight,
        base = base,
        glyphs = glyphs.map { it.id to it }.toMap().toIntMap(),
        kernings = kernings.map {
            BitmapFont.Kerning.buildKey(
                it.first,
                it.second
            ) to it
        }.toMap().toIntMap()
    )
}

fun Bitmap32.drawText(
    font: BitmapFont,
    str: String,
    x: Int = 0, y: Int = 0,
    color: RGBA = Colors.WHITE,
    size: Double = font.fontSize.toDouble(),
    horizontalAlign: HorizontalAlign = HorizontalAlign.LEFT,
    verticalAlign: VerticalAlign = VerticalAlign.TOP
) = context2d {
    this.font = font
    this.fontSize = size
    this.horizontalAlign = horizontalAlign
    this.verticalAlign = verticalAlign
    this.fillStyle = createColor(color)
    this.fillText(str, x, y)
}
