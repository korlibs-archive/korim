package com.soywiz.korim.vector

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korim.vector.rasterizer.*
import com.soywiz.korim.vector.rasterizer.Edge
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.bezier.*
import com.soywiz.korma.geom.vector.*

class FillStrokeTemp {
    internal val scale = RAST_FIXED_SCALE
    internal val iscale = 1.0 / RAST_FIXED_SCALE

    private var weight: Int = 1
    private lateinit var outFill: VectorPath
    private var startCap: LineCap = LineCap.BUTT
    private var endCap: LineCap = LineCap.BUTT
    private var joins: LineJoin = LineJoin.BEVEL
    private var miterLimit: Double = 4.0 // ratio of the width
    internal val fillPoints = Array(2) { PointIntArrayList(1024) }
    internal val fillPointsLeft = fillPoints[0]
    internal val fillPointsRight = fillPoints[1]

    private val prevEdge = Edge()
    private val prevEdgeLeft = Edge()
    private val prevEdgeRight = Edge()

    private val currEdge = Edge()
    private val currEdgeLeft = Edge()
    private val currEdgeRight = Edge()

    internal fun Edge.setEdgeDisplaced(edge: Edge, width: Int, angle: Angle) = this.apply {
        val ldx = (width * angle.cosine)
        val ldy = (width * angle.sine)
        this.setTo((edge.ax + ldx).toInt(), (edge.ay + ldy).toInt(), (edge.bx + ldx).toInt(), (edge.by + ldy).toInt(), edge.wind)
    }

    internal enum class EdgePoint(val n: Int) { A(0), B(1) }

    internal fun PointIntArrayList.addEdgePointA(e: Edge) = add(e.ax, e.ay)
    internal fun PointIntArrayList.addEdgePointB(e: Edge) = add(e.bx, e.by)
    internal fun PointIntArrayList.addEdgePointAB(e: Edge, point: EdgePoint) = if (point == EdgePoint.A) addEdgePointA(e) else addEdgePointB(e)
    internal fun PointIntArrayList.add(e: Point?) = run { if (e != null) add(e.x.toInt(), e.y.toInt()) }
    internal fun PointIntArrayList.add(x: Double, y: Double) = run { add(x.toInt(), y.toInt()) }

    private val tempP1 = Point()
    private val tempP2 = Point()
    private val tempP3 = Point()

    internal fun doJoin(out: PointIntArrayList, mainPrev: Edge, mainCurr: Edge, prev: Edge, curr: Edge, join: LineJoin, miterLimit: Double, scale: Double, forcedMiter: Boolean) {
        val rjoin = if (forcedMiter) LineJoin.MITER else join
        when (rjoin) {
            LineJoin.MITER -> {
                val intersection2 = tempP1.setTo(mainPrev.bx, mainPrev.by)
                val intersection = Edge.getIntersectXY(prev, curr, tempP3)
                if (intersection != null) {
                    val dist = Point.distance(intersection, intersection2)
                    if (forcedMiter || dist <= miterLimit) {
                        out.add(intersection)
                    } else {
                        out.addEdgePointB(prev)
                        out.addEdgePointA(curr)
                    }
                }
            }
            LineJoin.BEVEL -> {
                out.addEdgePointB(prev)
                out.addEdgePointA(curr)
            }
            LineJoin.ROUND -> {
                val i = Edge.getIntersectXY(prev, curr, tempP3)
                if (i != null) {
                    val count = (Point.distance(prev.bx, prev.by, curr.ax, curr.ay) * scale).toInt().clamp(4, 64)
                    for (n in 0..count) {
                        out.add(Bezier.quadCalc(prev.bx.toDouble(), prev.by.toDouble(), i.x, i.y, curr.ax.toDouble(), curr.ay.toDouble(), n.toDouble() / count, tempP2))
                    }
                } else {
                    out.addEdgePointB(prev)
                    out.addEdgePointA(curr)
                }
            }
        }
    }

    internal fun doCap(l: PointIntArrayList, r: PointIntArrayList, left: Edge, right: Edge, epoint: EdgePoint, cap: LineCap, scale: Double) {
        val angle = if (epoint == EdgePoint.A) -left.angle else +left.angle
        val lx = left.getX(epoint.n)
        val ly = left.getY(epoint.n)
        val rx = right.getX(epoint.n)
        val ry = right.getY(epoint.n)
        when (cap) {
            LineCap.BUTT -> {
                l.add(lx, ly)
                r.add(rx, ry)
            }
            LineCap.ROUND, LineCap.SQUARE -> {
                val ax = (angle.cosine * weight / 2).toInt()
                val ay = (angle.sine * weight / 2).toInt()
                val lx2 = lx + ax
                val ly2 = ly + ay
                val rx2 = rx + ax
                val ry2 = ry + ay
                if (cap == LineCap.SQUARE) {
                    l.add(lx2, ly2)
                    r.add(rx2, ry2)
                } else {
                    val count = (Point.distance(lx, ly, rx, ry) * scale).toInt().clamp(4, 64)
                    l.add(lx, ly)
                    for (n in 0 .. count) {
                        val m = if (epoint == EdgePoint.A) n else count - n
                        val ratio = m.toDouble() / count
                        r.add(Bezier.cubicCalc(
                            lx.toDouble(), ly.toDouble(),
                            lx2.toDouble(), ly2.toDouble(),
                            rx2.toDouble(), ry2.toDouble(),
                            rx.toDouble(), ry.toDouble(),
                            ratio,
                            tempP2
                        ))
                    }
                }
            }
        }
    }

    internal fun computeStroke(scale: Double, closed: Boolean) {
        if (ipath.size == 0) return

        val weightD2 = weight / 2
        val weightD2D = weightD2.toDouble()
        fillPointsLeft.clear()
        fillPointsRight.clear()

        ipath.visit { n, cmd, x0, y0, cx1, cy1, cx2, cy2, ax, ay ->
            val isFirst = n == 1
            val isLast = n == ipath.size - 1
            val isMiddle = !isFirst && (!isLast || closed)
            //val n1 = when {
            //    isLast -> if (closed) 1 else n
            //    else -> n + 1
            //}

            prevEdge.copyFrom(currEdge)
            prevEdgeLeft.copyFrom(currEdgeLeft)
            prevEdgeRight.copyFrom(currEdgeRight)

            currEdge.setTo(x0, y0, ax, ay, +1)
            currEdgeLeft.setEdgeDisplaced(currEdge, weightD2, currEdge.angle - 90.degrees)
            currEdgeRight.setEdgeDisplaced(currEdge, weightD2, currEdge.angle + 90.degrees)

            if (cmd != FullVectorPathInt.Cmd.MOVE_TO) {
                //println("cmd: $cmd, first=$isFirst, middle=$isMiddle, last=$isLast, [${x0 * iscale},${y0 * iscale}]-[${ax * iscale},${ay * iscale}]")

                if (isFirst) {
                    doCap(fillPointsLeft, fillPointsRight, currEdgeLeft, currEdgeRight, EdgePoint.A, startCap, scale)
                } else {
                    val angle = Edge.angleBetween(prevEdge, currEdge)
                    val leftAngle = angle > 0.degrees

                    doJoin(fillPointsLeft, prevEdge, currEdge, prevEdgeLeft, currEdgeLeft, joins, miterLimit, scale, leftAngle)
                    doJoin(fillPointsRight, prevEdge, currEdge, prevEdgeRight, currEdgeRight, joins, miterLimit, scale, !leftAngle)
                }

                // Intermediary points (do not do the edges)
                if (cmd == FullVectorPathInt.Cmd.QUAD_TO || cmd == FullVectorPathInt.Cmd.CUBIC_TO) {
                    val nsteps = bezierNPoints(cmd, x0, y0, cx1, cy1, cx2, cy2, ax, ay, iscale)
                    var lastX = 0.0
                    var lastY = 0.0
                    var lastLX = 0.0
                    var lastLY = 0.0
                    var lastRX = 0.0
                    var lastRY = 0.0
                    for (i in 0..nsteps) {
                        val ratio = i.toDouble() / nsteps
                        val p = bezier(ratio, cmd, x0, y0, cx1, cy1, cx2, cy2, ax, ay, tempP2)
                        val curX = p.x
                        val curY = p.y
                        val angle = Point.angle(curX, curY, lastX, lastY)
                        val angleLeft = angle - 90.degrees
                        val angleRight = angle + 90.degrees
                        val curLX = p.x + angleLeft.cos(weightD2D)
                        val curLY = p.y + angleLeft.sin(weightD2D)
                        val curRX = p.x + angleRight.cos(weightD2D)
                        val curRY = p.y + angleRight.sin(weightD2D)
                        if (i > 0) {
                            if (i < nsteps) {
                                fillPointsLeft.add(curLX, curLY)
                                fillPointsRight.add(curRX, curRY)
                            } else {
                                currEdge.setTo(lastX.toInt(), lastY.toInt(), curX.toInt(), curY.toInt(), +1)
                                currEdgeLeft.setTo(lastLX.toInt(), lastLY.toInt(), curLX.toInt(), curLY.toInt(), +1)
                                currEdgeRight.setTo(lastRX.toInt(), lastRY.toInt(), curRX.toInt(), curRY.toInt(), +1)
                            }
                        }
                        lastX = p.x
                        lastY = p.y
                        lastLX = curLX
                        lastLY = curLY
                        lastRX = curRX
                        lastRY = curRY
                    }
                }

                if (isLast) {
                    doCap(fillPointsLeft, fillPointsRight, currEdgeLeft, currEdgeRight, EdgePoint.B, endCap, scale)
                }
            }
        }

        for (n in 0 until fillPointsLeft.size) {
            val x = fillPointsLeft.getX(n)
            val y = fillPointsLeft.getY(n)
            if (n == 0) {
                outFill.moveTo(x * scale, y * scale)
            } else {
                outFill.lineTo(x * scale, y * scale)
            }
        }
        // Draw the rest of the points
        for (n in 0 until fillPointsRight.size) {
            val m = fillPointsRight.size - n - 1
            outFill.lineTo(fillPointsRight.getX(m) * scale, fillPointsRight.getY(m) * scale)
        }
        outFill.close()
        _vpath.clear()
    }

    private fun bezierNPoints(cmd: FullVectorPathInt.Cmd, x0: Int, y0: Int, cx1: Int, cy1: Int, cx2: Int, cy2: Int, ax: Int, ay: Int, scale: Double): Int =
        when (cmd) {
            FullVectorPathInt.Cmd.CUBIC_TO -> Bezier.cubicNPoints(x0.toDouble(), y0.toDouble(), cx1.toDouble(), cy1.toDouble(), cx2.toDouble(), cy2.toDouble(), ax.toDouble(), ay.toDouble(), scale)
            else -> Bezier.quadNPoints(x0.toDouble(), y0.toDouble(), cx1.toDouble(), cy1.toDouble(), ax.toDouble(), ay.toDouble(), scale)
        }

    private fun bezier(ratio: Double, cmd: FullVectorPathInt.Cmd, x0: Int, y0: Int, cx1: Int, cy1: Int, cx2: Int, cy2: Int, ax: Int, ay: Int, out: Point = Point()): Point =
        when (cmd) {
            FullVectorPathInt.Cmd.CUBIC_TO -> Bezier.cubicCalc(x0.toDouble(), y0.toDouble(), cx1.toDouble(), cy1.toDouble(), cx2.toDouble(), cy2.toDouble(), ax.toDouble(), ay.toDouble(), ratio, out)
            else -> Bezier.quadCalc(x0.toDouble(), y0.toDouble(), cx1.toDouble(), cy1.toDouble(), ax.toDouble(), ay.toDouble(), ratio, out)
        }

    internal fun set(outFill: VectorPath, weight: Int, startCap: LineCap, endCap: LineCap, joins: LineJoin, miterLimit: Double) {
        this.outFill = outFill
        this.weight = weight
        this.startCap = startCap
        this.endCap = endCap
        this.joins = joins
        this.miterLimit = miterLimit * weight
    }

    private val _vpath: VectorPath = VectorPath()
    private val ipath: FullVectorPathInt = FullVectorPathInt()

    private fun flush(closed: Boolean) {
        ipath.clear()
        ipath.write(_vpath, scale.toDouble())
        computeStroke(iscale, closed)
        _vpath.clear()
    }

    internal fun strokeFill(
        stroke: VectorPath,
        lineWidth: Double, joins: LineJoin, startCap: LineCap, endCap: LineCap, miterLimit: Double, outFill: VectorPath
    ) {
        set(outFill, (lineWidth * scale).toInt(), startCap, endCap, joins, miterLimit)

        stroke.visitCmds(
            moveTo = { x, y ->
                flush(false)
                _vpath.moveTo(x, y)
                //tempPath.moveTo(x, y)
            },
            lineTo = { x, y -> _vpath.lineTo(x, y) },
            cubicTo = { x1, y1, x2, y2, x3, y3 -> _vpath.cubicTo(x1, y1, x2, y2, x3, y3) },
            quadTo = { x1, y1, x2, y2 -> _vpath.quadTo(x1, y1, x2, y2) },
            close = {
                _vpath.close()
                flush(true)
            }
        )

        flush(false)
    }
}

// @TODO: Move this to KorMA?
internal class FullVectorPathInt {
    val cmds = IntArrayList()
    val x0 = IntArrayList()
    val y0 = IntArrayList()
    val cx1 = IntArrayList()
    val cy1 = IntArrayList()
    val cx2 = IntArrayList()
    val cy2 = IntArrayList()
    val ax = IntArrayList()
    val ay = IntArrayList()

    val size get() = cmds.size
    var moveToX = 0
    var moveToY = 0
    var lastX = 0
    var lastY = 0

    enum class Cmd(val id: Int) {
        MOVE_TO(0), LINE_TO(1), QUAD_TO(2), CUBIC_TO(3), CLOSE(4);
        companion object {
            val COMMANDS = arrayOf(MOVE_TO, LINE_TO, QUAD_TO, CUBIC_TO, CLOSE)
            operator fun get(index: Int) = COMMANDS[index]
        }
    }

    inline fun visit(block: (index: Int, cmd: Cmd, x0: Int, y0: Int, cx1: Int, cy1: Int, cx2: Int, cy2: Int, ax: Int, ay: Int) -> Unit) {
        for (n in 0 until size) {
            block(n, Cmd.COMMANDS[cmds.getAt(n)], x0.getAt(n), y0.getAt(n), cx1.getAt(n), cy1.getAt(n), cx2.getAt(n), cy2.getAt(n), ax.getAt(n), ay.getAt(n))
        }
    }

    fun clear() {
        cmds.clear()
        x0.clear()
        cx1.clear()
        cx2.clear()
        ax.clear()
        y0.clear()
        cy1.clear()
        cy2.clear()
        ay.clear()
    }

    fun add(cmd: Cmd, x0: Int, y0: Int, cx1: Int, cy1: Int, cx2: Int, cy2: Int, ax: Int, ay: Int) {
        //println("ADD[$cmd]: [$x0,$y0] - [$ax,$ay]")
        this.cmds.add(cmd.id)
        Unit.also { this.x0.add(x0) }.also { this.y0.add(y0) }
        Unit.also { this.cx1.add(cx1) }.also { this.cy1.add(cy1) }
        Unit.also { this.cx2.add(cx2) }.also { this.cy2.add(cy2) }
        Unit.also { this.ax.add(ax) }.also { this.ay.add(ay) }
        lastX = ax
        lastY = ay
    }

    fun moveTo(x: Int, y: Int) = add(Cmd.MOVE_TO, x, y, x, y, x, y, x, y).also { moveToX = x }.also { moveToY = y }
    fun lineTo(x: Int, y: Int) = add(Cmd.LINE_TO, lastX, lastY, lastX, lastY, x, y, x, y)
    fun quadTo(cx: Int, cy: Int, ax: Int, ay: Int) = add(Cmd.QUAD_TO, lastX, lastY, cx, cy, cx, cy, ax, ay)
    fun cubicTo(cx1: Int, cy1: Int, cx2: Int, cy2: Int, ax: Int, ay: Int) = add(Cmd.CUBIC_TO, lastX, lastY, cx1, cy1, cx2, cy2, ax, ay)
    fun close() = add(Cmd.CLOSE, lastX, lastY, lastX, lastY, moveToX, moveToY, moveToX, moveToY).also { moveToX = lastX }.also { moveToY = lastY }

    fun write(path: VectorPath, scale: Double = 1.0) {
        path.visitCmds(
            moveTo = { x, y -> moveTo((x * scale).toInt(), (y * scale).toInt()) },
            lineTo = { x, y -> lineTo((x * scale).toInt(), (y * scale).toInt()) },
            quadTo = { cx, cy, ax, ay -> quadTo((cx * scale).toInt(), (cy * scale).toInt(), (ax * scale).toInt(), (ay * scale).toInt()) },
            cubicTo = { cx1, cy1, cx2, cy2, ax, ay -> cubicTo((cx1 * scale).toInt(), (cy1 * scale).toInt(), (cx2 * scale).toInt(), (cy2 * scale).toInt(), (ax * scale).toInt(), (ay * scale).toInt()) },
            close = { close() }
        )
    }
}

fun VectorPath.strokeToFill(
    lineWidth: Double,
    joins: LineJoin = LineJoin.MITER,
    startCap: LineCap = LineCap.BUTT,
    endCap: LineCap = startCap,
    miterLimit: Double = 4.0,
    temp: FillStrokeTemp = FillStrokeTemp(),
    outFill: VectorPath = VectorPath(winding = Winding.NON_ZERO)
): VectorPath {
    temp.strokeFill(
        this@strokeToFill, lineWidth, joins, startCap, endCap, miterLimit, outFill
    )
    return outFill
}

internal fun Bezier.Companion.quadNPoints(x0: Double, y0: Double, cx: Double, cy: Double, x1: Double, y1: Double, scale: Double = 1.0): Int {
    return ((Point.distance(x0, y0, cx, cy) + Point.distance(cx, cy, x1, y1)) * scale).toInt().clamp(5, 128)
}

internal fun Bezier.Companion.cubicNPoints(x0: Double, y0: Double, cx1: Double, cy1: Double, cx2: Double, cy2: Double, x1: Double, y1: Double, scale: Double = 1.0): Int {
    return ((Point.distance(x0, y0, cx1, cy1) + Point.distance(cx1, cy1, cx2, cy2) + Point.distance(cx2, cy2, x1, y1)) * scale).toInt().clamp(5, 128)
}

internal fun Angle.cos(scale: Double) = this.cosine * scale
internal fun Angle.sin(scale: Double) = this.sine * scale
