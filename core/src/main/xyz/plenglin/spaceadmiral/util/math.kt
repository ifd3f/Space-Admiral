package xyz.plenglin.spaceadmiral.util

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import java.lang.RuntimeException
import java.util.*

class Transform2D(val posLocal: Vector2 = Vector2(), angleLocal: Float = 0f, parent: Transform2D? = null) {
    private val children = mutableSetOf<Transform2D>()

    var angleLocal = angleLocal
        set(value) {
            field = value
            dirty = true
        }

    private var _dirty = true
    var dirty
        get() = _dirty
        set(value) {
            if (value) {
                val stack = LinkedList<Transform2D>()
                stack.push(this)
                while (stack.isNotEmpty()) {
                    val node = stack.pop()
                    stack.addAll(node.children)
                    node._dirty = true
                }
            } else {
                _dirty = false
            }
        }

    private val _posGlobal: Vector2 = Vector2()
    val posGlobal: Vector2 get() {
        if (dirty) {
            update()
        }
        return _posGlobal
    }
    var angleGlobal: Float = angleLocal
        private set

    var parent: Transform2D? = parent
        set(value) {
            var node: Transform2D? = value
            while (node != null) {
                if (node == this) {
                    throw CircularTransformStructureException(this, node)
                }
                node = node.parent
            }
            field?.children?.remove(this)
            value?.children?.add(this)
            dirty = true
            field = value
        }

    private fun updateSelf() {
        if (dirty) {
            parent?.let {
                _posGlobal.set(it._posGlobal).add(posLocal.cpy().rotateRad(it.angleGlobal))
                angleGlobal = it.angleGlobal + angleLocal
            } ?: kotlin.run {
                _posGlobal.set(posLocal)
                angleGlobal = angleLocal
            }
            dirty = false
        }
    }

    fun toGlobal(): Transform2D {
        return Transform2D(posGlobal, angleGlobal, null)
    }

    fun setLocalPosition(x: Float, y: Float) {
        posLocal.set(x, y)
        dirty = true
    }

    fun setLocalPosition(v: Vector2) {
        posLocal.set(v)
        dirty = true
    }

    fun update() {
        var node: Transform2D? = this
        val toUpdate = LinkedList<Transform2D>()
        while (node != null) {
            toUpdate.push(node)
            node = node.parent
        }
        toUpdate.forEach(Transform2D::updateSelf)
    }

    fun set(trs: Transform2D) {
        posLocal.set(trs.posLocal)
        angleLocal = trs.angleLocal
        dirty = true
    }

    override fun toString(): String {
        return "Transform($posLocal, $angleLocal, $parent)"
    }

    companion object {
        @JvmStatic
        val ZERO = Transform2D(Vector2.Zero, 0f)
    }
}

fun Vector2.multCpx(o: Vector2): Vector2 {
    x = x * o.x - y * o.y
    y = x * o.y + y * o.x
    return this
}

class CircularTransformStructureException(child: Transform2D, parent: Transform2D) : RuntimeException(
        "Circular transform structure detected while setting $parent to be the parent of $child"
)

data class MinMaxRectangle(val left: Float, val right: Float, val top: Float, val bottom: Float)

data class Capsule2D(val x0: Vector2, val x1: Vector2, val radius: Float) {

    val v by lazy { x1.cpy().sub(x0) }
    val r by lazy { v.cpy().rotate90(1).setLength(radius) }

    fun contains(x: Vector2): Boolean {
        TODO()
    }

}

fun lerp(x: Float, v0: Vector2, v1: Vector2): Vector2 {
    return (v1.cpy().sub(v0)).scl(x).add(v0)
}

fun lerp(x: Float, v0: Vector3, v1: Vector3): Vector3 {
    return (v1.cpy().sub(v0)).scl(x).add(v0)
}
