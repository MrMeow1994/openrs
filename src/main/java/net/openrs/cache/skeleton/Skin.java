package net.openrs.cache.skeleton;

import net.openrs.cache.skeleton.rt7_anims.AnimationBone;
import net.openrs.cache.skeleton.rt7_anims.SkeletalAnimBase;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class Skin {

    public int id;
    public int count;
    public int[] transformationTypes;
    public int[][] skinList;
    public static SkeletalAnimBase skeletalAnimBase;

    public static Skin decode(ByteBuffer buffer, boolean highrev, int bufferSize) {
        Skin skin = new Skin();
        int start = buffer.position();

            skin.count = highrev ? (buffer.getShort() & 0xFFFF) : (buffer.get() & 0xFF);
            skin.transformationTypes = new int[skin.count];
            skin.skinList = new int[skin.count][];

            // Read transformation types
            for (int i = 0; i < skin.count; i++) {
                skin.transformationTypes[i] = highrev ? (buffer.getShort() & 0xFFFF) : (buffer.get() & 0xFF);
            }

            // Read label counts
            for (int i = 0; i < skin.count; i++) {
                int labelCount = highrev ? (buffer.getShort() & 0xFFFF) : (buffer.get() & 0xFF);
                skin.skinList[i] = new int[labelCount];
            }

            // Read label values
            for (int i = 0; i < skin.count; i++) {
                for (int j = 0; j < skin.skinList[i].length; j++) {
                    skin.skinList[i][j] = highrev ? (buffer.getShort() & 0xFFFF) : (buffer.get() & 0xFF);
                }
            }

            // Only attempt SkeletalAnimBase if enough bytes remain and it's lowrev
            int readSize = buffer.position() - start;
            if (!highrev) {
                if (readSize != bufferSize) {
                    try {
                        int size = buffer.getShort() & 0xFFFF;
                        if (size > 0) {
                            skin.skeletalAnimBase = new SkeletalAnimBase(buffer, size);
                        }
                    } catch (Throwable t) {
                        System.err.println("Tried to load base because there was extra base data but skeletal failed to load.");
                        t.printStackTrace();
                    }
                }
                int read2_size = buffer.position() - start;

                if (read2_size != bufferSize) {
                    throw new RuntimeException("base data size mismatch: " + read2_size + ", expected " + bufferSize);
                }
            }

        return skin;
    }

    public void encode(DataOutputStream dos, boolean highrev) throws IOException {
        if (highrev) dos.writeShort(count);
        else dos.writeByte(count);

        for (int type : transformationTypes) {
            if (highrev) dos.writeShort(type);
            else dos.writeByte(type);
        }

        for (int[] labels : skinList) {
            if (highrev) dos.writeShort(labels.length);
            else dos.writeByte(labels.length);
        }

        for (int[] labels : skinList) {
            for (int l : labels) {
                if (highrev) dos.writeShort(l);
                else dos.writeByte(l);
            }
        }

        if (!highrev && skeletalAnimBase != null) {
            dos.writeShort(skeletalAnimBase.bones.length);
            dos.writeByte(skeletalAnimBase.max_connections);
            for (AnimationBone bone : skeletalAnimBase.bones) {
                if (bone != null) bone.encode(dos, false);
            }
        }
    }

    public SkeletalAnimBase get_skeletal_animbase() {
        return skeletalAnimBase;
    }

    public int transforms_count() {
        return count;
    }

    public boolean hasSkeletal() {
        return skeletalAnimBase != null && skeletalAnimBase.bones != null && skeletalAnimBase.bones.length > 0;
    }
}
