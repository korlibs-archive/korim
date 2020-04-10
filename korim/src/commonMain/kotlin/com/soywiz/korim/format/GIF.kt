@file:ExperimentalUnsignedTypes

package com.soywiz.korim.format

import com.soywiz.korim.color.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import kotlin.math.*

// https://github.com/lecram/gifdec/blob/master/gifdec.c
@ExperimentalUnsignedTypes
object GifDec {
    class gd_Palette(
        var size: Int,
        val colors: RgbaArray = RgbaArray(0x100) // @TODO: Convert into RgbaArray once working
    )

    class gd_GCE(
        var delay: Int,
        var tindex: Int,
        var disposal: Int,
        var input: Int,
        var transparency: Boolean
    )

    class gd_GIF(
        var fd: SyncStream,
        var anim_start: Int,
        var width: Int,
        var height: Int,
        var depth: Int,
        var loop_count: Int,
        var gce: gd_GCE,
        var palette: gd_Palette,
        var lct: gd_Palette,
        var gct: gd_Palette,
        var plain_text: ((
            gif: gd_GIF, tx: Int, ty: Int,
            tw: Int, th: Int, cw: Int, ch: Int,
            fg: Int, bg: Int
        ) -> Unit)? = null,
        var comment: ((gif: gd_GIF) -> Unit) ? =null,
        var application: ((gif: gd_GIF, id: String, auth: String) -> Unit)? = null,
        var fx: Int,
        var fy: Int,
        var fw: Int,
        var fh: Int,
        var bgindex: Int,
        var canvas: RgbaArray,
        var frame: UByteArray
    )

    class Entry(
        var length: Int,
        var prefix: Int,
        var suffix: Int
    )

    class Table(
        var bulk: Int,
        var nentries: Int,
        var entries: Array<Entry>
    ) {
        fun resize(count: Int) {
            bulk = count
            entries = entries.copyOf(bulk)
        }
    }

    fun read_num(fd: SyncStream): Int {
        val a = fd.readU8()
        val b = fd.readU8()
        return a or (b shl 8)
    }

    suspend fun gd_open_gif(fname: String): gd_GIF {
        var depth: UShort
        var bgidx: UByte
        var aspect: UByte
        var gct_sz: Int
        var gif: gd_GIF

        val fd = open(fname, O_RDONLY);
        /* Header */
        val sigver = fd.readBytes(3)
        if (memcmp(sigver, "GIF", 3) != 0) error("invalid signature");
        /* Version */
        val sigver = fd.readBytes(3)
        if (memcmp(sigver, "89a", 3) != 0) error("invalid version")
        /* Width x Height */
        val width  = read_num(fd);
        val height = read_num(fd);
        /* FDSZ */
        val fdsz = fd.readU8()
        /* Presence of GCT */
        if ((fdsz and 0x80) == 0) {
            error("no global color table")
        }
        /* Color Space's Depth */
        val depth = ((fdsz ushr 4) and 7) + 1;
        /* Ignore Sort Flag. */
        /* GCT Size */
        val gct_sz = 1 shl ((fdsz and 0x07) + 1);
        /* Background Color Index */
        val bgidx = fd.readU8()
        /* Aspect Ratio */
        val aspect = fd.readU8()
        /* Create gd_GIF Structure. */
        gif = calloc(1, sizeof(*gif) + 4 * width * height);
        if (!gif) goto fail;
        gif.fd = fd;
        gif.width  = width;
        gif.height = height;
        gif.depth  = depth;
        /* Read GCT */
        gif.gct.size = gct_sz;
        read(fd, gif.gct.colors, 3 * gif.gct.size);
        gif.palette = &gif.gct;
        gif.bgindex = bgidx;
        gif.canvas = (uint8_t *) &gif[1];
        gif.frame = &gif.canvas[3 * width * height];
        if (gif.bgindex)
        memset(gif.frame, gif.bgindex, gif.width * gif.height);
        gif.anim_start = lseek(fd, 0, SEEK_CUR);
        goto ok;
        fail:
        close(fd);
        ok:
        return gif;
    }

    fun discard_sub_blocks(gif: gd_GIF )
    {
        do {
            val size = gif.fd.readU8()
            lseek(gif.fd, size.toLong(), SEEK_CUR);
        } while (size != 0);
    }

    fun read_plain_text_ext(gif: gd_GIF) {
        val plain_text = gif.plain_text
        if (plain_text != null) {
            lseek(gif.fd, 1, SEEK_CUR); /* block size = 12 */
            val tx = read_num(gif.fd);
            val ty = read_num(gif.fd);
            val tw = read_num(gif.fd);
            val th = read_num(gif.fd);
            val cw = gif.fd.readU8()
            val ch = gif.fd.readU8()
            val fg = gif.fd.readU8()
            val bg = gif.fd.readU8()
            val sub_block = lseek(gif.fd, 0, SEEK_CUR);
            plain_text.invoke(gif, tx, ty, tw, th, cw, ch, fg, bg);
            lseek(gif.fd, sub_block, SEEK_SET);
        } else {
            /* Discard plain text metadata. */
            lseek(gif.fd, 13, SEEK_CUR);
        }
        /* Discard plain text sub-blocks. */
        discard_sub_blocks(gif);
    }

    fun read_graphic_control_ext(gif: gd_GIF) {
        /* Discard block size (always 0x04). */
        lseek(gif.fd, 1, SEEK_CUR);
        val rdit = gif.fd.readU8()
        gif.gce.disposal = (rdit ushr 2) and 3;
        gif.gce.input = rdit and 2;
        gif.gce.transparency = rdit and 1;
        gif.gce.delay = read_num(gif.fd);
        gif.gce.tindex = gif.fd.readU8()
        /* Skip block terminator. */
        lseek(gif.fd, 1, SEEK_CUR);
    }

    fun read_comment_ext(gif: gd_GIF) {
        val comment = gif.comment
        if (comment != null) {
            val sub_block = lseek(gif.fd, 0, SEEK_CUR);
            comment(gif);
            lseek(gif.fd, sub_block, SEEK_SET);
        }
        /* Discard comment sub-blocks. */
        discard_sub_blocks(gif);
    }

    fun read_application_ext(gif: gd_GIF) {

        /* Discard block size (always 0x0B). */
        lseek(gif.fd, 1, SEEK_CUR);
        /* Application Identifier. */
        val app_id = gif.fd.readBytesExact(8)
        /* Application Authentication Code. */
        val app_auth_code = gif.fd.readBytesExact(3)
        if (!strncmp(app_id, "NETSCAPE", sizeof(app_id))) {
            /* Discard block size (0x03) and constant byte (0x01). */
            lseek(gif.fd, 2, SEEK_CUR);
            gif.loop_count = read_num(gif.fd);
            /* Skip block terminator. */
            lseek(gif.fd, 1, SEEK_CUR);
        } else if (gif.application != null) {
            val sub_block = lseek(gif.fd, 0, SEEK_CUR);
            gif.application!!.invoke(gif, app_id, app_auth_code);
            lseek(gif.fd, sub_block, SEEK_SET);
            discard_sub_blocks(gif);
        } else {
            discard_sub_blocks(gif);
        }
    }

    fun read_ext(gif: gd_GIF) {
        val label = gif.fd.readU8()

        when (label) {
            0x01 -> read_plain_text_ext(gif);
            0xF9 -> read_graphic_control_ext(gif);
            0xFE -> read_comment_ext(gif);
            0xFF -> read_application_ext(gif);
            else -> error("unknown extension: %02X".format(label))
        }
    }

    fun new_table(key_size: Int): Table {
        val init_bulk = max(1 shl (key_size + 1), 0x100);
        return Table(
            init_bulk,
            (1 shl key_size) + 2,
            Array(init_bulk) { Entry(1, 0xFFF, it) }
        )
    }

    interface BasePointer

    data class Pointer<T>(val array: Array<T>, var offset: Int) : BasePointer {
        fun get() = array[offset]
        fun set(value: T) {
            array[offset] = value
        }
        fun incr() {
            offset++
        }
    }

    data class UByteArrayPointer(val array: UByteArray, var offset: Int) : BasePointer {
        fun get() = array[offset]
        fun set(value: UByte) {
            array[offset] = value
        }
        fun incr() {
            offset++
        }
    }

    /* Add table entry. Return value:
     *  0 on success
     *  +1 if key size must be incremented after this addition
     *  -1 if could not realloc table */
    fun add_entry(tablep: Pointer<Table>, length: Int, prefix: Int, suffix: Int): Int {
        var table = tablep.get()
        if (table.nentries == table.bulk) {
            table.resize(table.bulk * 2)
            table.bulk *= 2;
            table = realloc(table, sizeof(*table) + sizeof(Entry) * table.bulk);
            //if (!table) return -1;
            table.entries = (Entry *) &table[1];
            tablep.set(table)
        }
        table.entries[table.nentries] = Entry(length = length, prefix = prefix, suffix = suffix)
        table.nentries++;
        if ((table.nentries and (table.nentries - 1)) == 0)
        return 1;
        return 0;
    }

    fun get_key(gif: gd_GIF, key_size: Int, sub_len: UByteArray, shift: UByteArray, byte: UByteArray): Int {
        var bits_read;
        var rpad;
        var frag_size;

        var key = 0;
        bits_read = 0
        while (bits_read < key_size) {
            rpad = (*shift + bits_read) % 8;
            if (rpad == 0) {
                /* Update byte. */
                if (*sub_len == 0) {
                    read(gif.fd, sub_len, 1); /* Must be nonzero! */
                }
                read(gif.fd, byte, 1);
                (*sub_len)--;
            }
            frag_size = MIN(key_size - bits_read, 8 - rpad);
            key = key or (((uint16_t) ((*byte) ushr rpad)) shl bits_read)
            bits_read += frag_size
        }
        /* Clear extra bits to the left. */
        key = key and ((1 shl key_size) - 1);
        *shift = (*shift + key_size) % 8;
        return key;
    }

    /* Compute output index of y-th input line, in frame of height h. */
    fun interlaced_line_index(h: Int, y: Int): Int {
        var y = y
        var p = (h - 1) / 8 + 1;
        if (y < p) return y * 8; /* pass 1 */
        y -= p;
        p = (h - 5) / 8 + 1;
        if (y < p) return y * 8 + 4; /* pass 2 */
        y -= p;
        p = (h - 3) / 4 + 1;
        if (y < p) return y * 4 + 2; /* pass 3 */
        y -= p;
        /* pass 4 */
        return y * 2 + 1;
    }

    /* Decompress image pixels.
     * Return 0 on success or -1 on out-of-memory (w.r.t. LZW code table). */
    fun read_image_data(gif: gd_GIF, interlace: Boolean): Int {
        var table_is_full: Boolean = false
        var str_len: Int
        var p: Int
        var x: Int
        var y: Int
        var entry: Entry

        val byte = gif.fd.readU8()
        var key_size = byte.toInt()
        val start = lseek(gif.fd, 0, SEEK_CUR);
        discard_sub_blocks(gif);
        val end = lseek(gif.fd, 0, SEEK_CUR);
        lseek(gif.fd, start, SEEK_SET);
        val clear = 1 shl key_size;
        val stop = clear + 1;
        val table = new_table(key_size);
        key_size++;
        var init_key_size = key_size;
        var shift = 0
        var sub_len = 0
        var key = get_key(gif, key_size, &sub_len, &shift, &byte); /* clear code */
        var frm_off = 0;
        var ret = 0;
        while (true) {
            if (key == clear) {
                key_size = init_key_size;
                table.nentries = (1 shl (key_size - 1)) + 2;
                table_is_full = false
            } else if (!table_is_full) {
                ret = add_entry(&table, str_len + 1, key, entry.suffix);
                if (ret == -1) {
                    free(table);
                    return -1;
                }
                if (table.nentries == 0x1000) {
                    ret = 0;
                    table_is_full = true
                }
            }
            key = get_key(gif, key_size, &sub_len, &shift, &byte);
            if (key == clear) continue;
            if (key == stop) break;
            if (ret == 1) key_size++;
            entry = table.entries[key.toInt()];
            str_len = entry.length;
            while (true) {
                p = frm_off + entry.length - 1;
                x = p % gif.fw;
                y = p / gif.fw;
                if (interlace) {
                    y = interlaced_line_index(gif.fh, y);
                }
                gif.frame[(gif.fy + y) * gif.width + gif.fx + x] = entry.suffix.toUByte()
                if (entry.prefix == 0xFFF) {
                    break;
                } else {
                    entry = table.entries[entry.prefix];
                }
            }
            frm_off += str_len;
            if (key.toInt() < table.nentries - 1 && !table_is_full)
            table.entries[table.nentries - 1].suffix = entry.suffix;
        }
        free(table);
        sub_len = gif.fd.readU8()  /* Must be zero! */
        lseek(gif.fd, end, SEEK_SET);
        return 0;
    }

    /* Read image.
     * Return 0 on success or -1 on out-of-memory (w.r.t. LZW code table). */
    fun read_image(gif: gd_GIF): Int {
        /* Image Descriptor. */
        gif.fx = read_num(gif.fd);
        gif.fy = read_num(gif.fd);
        gif.fw = read_num(gif.fd);
        gif.fh = read_num(gif.fd);
        val fisrz = gif.fd.readU8()
        val interlace = (fisrz and 0x40) != 0
        /* Ignore Sort Flag. */
        /* Local Color Table? */
        if ((fisrz and 0x80) != 0) {
            /* Read LCT */
            gif.lct.size = 1 shl ((fisrz and 0x07) + 1);
            for (n in 0 until gif.lct.size) {
                val r = gif.fd.readU8()
                val g = gif.fd.readU8()
                val b = gif.fd.readU8()
                gif.lct.colors[n] = RGBA(r, g, b)
            }
            gif.palette = gif.lct;
        } else {
            gif.palette = gif.gct;
        }
        /* Image Data. */
        return read_image_data(gif, interlace);
    }

    fun render_frame_rect(gif: gd_GIF, buffer: RgbaArray) {
        var i = gif.fy * gif.width + gif.fx;
        for (j in 0 until gif.fh) {
            for (k in 0 until gif.fw) {
                val index = gif.frame[(gif.fy + j) * gif.width + gif.fx + k].toInt()
                val color = gif.palette.colors[index*3];
                if (!gif.gce.transparency || index != gif.gce.tindex) {
                    buffer[i+k] = color
                }
            }
            i += gif.width;
        }
    }

    fun UByteArray.ptr(index: Int) = UByteArrayPointer(this, index)
    fun <T> Array<T>.ptr(index: Int) = Pointer(this, index)

    fun memcpy(dst: BasePointer, src: BasePointer, count: Int) {
        TODO()
    }

    fun dispose(gif: gd_GIF) {
        when (gif.gce.disposal) {
            2 -> { /* Restore to background color. */
                val bgcolor = gif.palette.colors[gif.bgindex*3]
                var i = gif.fy * gif.width+gif.fx;
                for (j in 0 until gif.fh) {
                    for (k in 0 until gif.fw) {
                        gif.canvas[i+k] = bgcolor
                    }
                    i += gif.width;
                }
            }
            3 -> { /* Restore to previous, i.e., don't update canvas.*/
            }
            else -> {
                /* Add frame non-transparent pixels to canvas. */
                render_frame_rect(gif, gif.canvas);
            }
        }
    }

    /* Return 1 if got a frame; 0 if got GIF trailer; -1 if error. */
    fun gd_get_frame(gif: gd_GIF): Int {
        var sep: Char

        dispose(gif);
        sep = readChar(gif.fd)
        while (sep != ',') {
            if (sep == ';')
                return 0;
            if (sep == '!')
                read_ext(gif);
            else return -1;
            sep = readChar(gif.fd)
        }
        if (read_image(gif) == -1)
            return -1;
        return 1;
    }

    fun gd_render_frame(gif: gd_GIF, buffer: RgbaArray) {
        arraycopy(gif.canvas, 0, buffer, 0, gif.width * gif.height)
        render_frame_rect(gif, buffer);
    }

    fun gd_rewind(gif: gd_GIF) {
        lseek(gif.fd, gif.anim_start.toLong(), SEEK_SET);
    }

    fun gd_close_gif(gif: gd_GIF) {
        close(gif.fd);
        free(gif);
    }

    val SEEK_SET = 0
    val SEEK_CUR = 1

    fun close(fd: SyncStream) {
        fd.close()
    }

    fun free(obj: Any?) {
    }

    fun readChar(fd: SyncStream): Char {
        return fd.readU8().toChar()
    }

    fun lseek(fd: SyncStream, pos: Long, kind: Int): Long {
        when (kind) {
            SEEK_SET -> fd.position = pos.toLong()
            SEEK_CUR -> fd.position += pos.toLong()
        }
        return fd.position
    }

    val O_RDONLY = 0
    suspend fun open(name: String, mode: Int): SyncStream {
        return name.uniVfs.readAll().openSync()
    }
}

private inline class GifPtr(val pos: Int)
