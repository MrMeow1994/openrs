package net.openrs.cache.type.texture

import net.openrs.cache.type.Type
import java.io.DataOutputStream
import java.io.IOException
import net.openrs.util.uShort
import net.openrs.util.uByte
import java.nio.ByteBuffer

class TextureType(private val id: Int) : Type {
    var fileIds: IntArray = IntArray(0)
    var field2293: Boolean = false
    var field2301: IntArray = IntArray(0)
    var field2296: IntArray = IntArray(0)
    var field2295: IntArray = IntArray(0)
    var animationSpeed: Int = 0
    var animationDirection: Int = 0
    var averageRGB: Int = -1
    var isLowDetail: Boolean = false
    var fileId: Int = -1
    var field5890: Int = 233

    override fun decode(buffer: ByteBuffer) {
        // Check version
        if (field5890 >= 233) {
            // Version 233+ format
            fileId = buffer.uShort
            averageRGB = buffer.uShort
            isLowDetail = buffer.uByte == 1
            animationDirection = buffer.uByte
            animationSpeed = buffer.uByte
        } else {
            // Legacy format ≤232
            averageRGB = buffer.uShort
            field2293 = buffer.uByte == 1
            val count: Int = buffer.uByte

            if (count in 1..4) {
                fileIds = IntArray(count)
                for (i in 0 until count) fileIds[i] = buffer.uShort

                if (count > 1) {
                    field2301 = IntArray(count - 1)
                    for (i in 0 until count - 1) field2301[i] = buffer.uByte
                }

                if (count > 1) {
                    field2296 = IntArray(count - 1)
                    for (i in 0 until count - 1) field2296[i] = buffer.uByte
                }

                field2295 = IntArray(count)
                for (i in 0 until count) field2295[i] = buffer.getInt()

                animationDirection = buffer.uByte
                animationSpeed = buffer.uByte
            } else {
                println("Texture: $id Out of range 1..4 [$count]")
            }
        }
    }

    @Throws(IOException::class)
    override fun encode(dos: DataOutputStream) {
        dos.writeByte(1)
        dos.writeShort(id)

        if (field2293) {
            dos.writeByte(2)
            dos.writeByte(if (field2293) 1 else 0)
        }

        if (fileIds.isNotEmpty()) {
            dos.writeByte(3)
            dos.writeByte(fileIds.size)

            dos.writeByte(4)
            for (v in fileIds) dos.writeShort(v)
        }

        if (field2301.isNotEmpty()) {
            dos.writeByte(5)
            for (v in field2301) dos.writeByte(v)
        }

        if (field2296.isNotEmpty()) {
            dos.writeByte(6)
            for (v in field2296) dos.writeByte(v)
        }

        if (field2295.isNotEmpty()) {
            dos.writeByte(7)
            for (v in field2295) dos.writeInt(v)
        }

        if (animationSpeed != 0) {
            dos.writeByte(8)
            dos.writeShort(animationSpeed)
        }

        if (animationDirection != 0) {
            dos.writeByte(9)
            dos.writeShort(animationDirection)
        }

        if (averageRGB != -1) {
            dos.writeByte(10)
            dos.write24bitInt(averageRGB)
        }

        dos.writeByte(0)
    }

    private fun DataOutputStream.write24bitInt(content: Int) {
        writeByte(content shr 16)
        writeByte(content shr 8)
        writeByte(content)
    }

    override fun getID(): Int = id
}
