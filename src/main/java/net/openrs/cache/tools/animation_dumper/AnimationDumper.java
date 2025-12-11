package net.openrs.cache.tools.animation_dumper;

import net.openrs.cache.*;
import net.openrs.cache.skeleton.Skeleton;
import net.openrs.cache.skeleton.Skin;
import net.openrs.cache.skeleton.rt7_anims.AnimKeyFrameSet;
import net.openrs.cache.type.CacheIndex;
import net.openrs.cache.util.CompressionUtils;

import java.io.*;
import java.nio.ByteBuffer;

public class AnimationDumper {

    public static boolean headerPacked;

    public static void main(String[] args) throws Exception {
        try (Cache cache = new Cache(FileStore.open(Constants.CACHE_PATH))) {
            File dir = new File("E:/dump3/test/");
            if (!dir.exists()) dir.mkdirs();

            AnimKeyFrameSet.init();

            final ReferenceTable skeletonTable = cache.getReferenceTable(CacheIndex.FRAMES);
            final ReferenceTable keyframeTable = cache.getReferenceTable(CacheIndex.SKELETAL_KEYFRAMES);
            final Skeleton[][] skeletons = new Skeleton[skeletonTable.capacity()][];

            for (int mainId = 0; mainId < skeletons.length; mainId++) {
                if (skeletonTable.getEntry(mainId) == null) continue;

                System.out.println("=== Processing mainId: " + mainId + " ===");
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try (DataOutputStream dos = new DataOutputStream(bos)) {

                    // 1️⃣ Normal skeleton frames
                    Container frameContainer = cache.read(CacheIndex.FRAMES, mainId);
                    if (frameContainer != null) {
                        Archive skeletonArchive = Archive.decode(frameContainer.getData(),
                                skeletonTable.getEntry(mainId).size());
                        if (skeletonArchive != null) {
                            skeletons[mainId] = new Skeleton[skeletonArchive.size()];
                            boolean localHeader = false;

                            for (int subId = 0; subId < skeletonArchive.size(); subId++) {
                                ByteBuffer buffer = skeletonArchive.getEntry(subId);
                                if (buffer == null || buffer.remaining() == 0) continue;

                                int skinId = ((buffer.array()[0] & 0xFF) << 8) | (buffer.array()[1] & 0xFF);
                                Container skinContainer = cache.read(CacheIndex.FRAMEMAPS, skinId);
                                if (skinContainer == null) {
                                    System.out.println("Missing skin container for skinId=" + skinId);
                                    continue;
                                }

                                ByteBuffer skinBuffer = skinContainer.getData();
                                if (skinBuffer == null) {
                                    System.out.println("Missing skin buffer for skinId=" + skinId);
                                    continue;
                                }

                                Skin skin = Skin.decode(skinBuffer, false, skinBuffer.remaining());
                                if (skin == null) {
                                    System.out.println("Failed decoding skin for skinId=" + skinId);
                                    continue;
                                }

                                if (!localHeader) {
                                    // ✅ FIX: use hasSkeletal() instead of direct skeletalAnimBase check
                                    dos.writeShort(skin.hasSkeletal() ? 420 : 710);
                                    dos.writeInt(skeletons.length);
                                    skin.encode(dos, false);
                                    dos.writeShort(skeletonArchive.size());
                                    localHeader = true;
                                }

                                dos.writeShort(subId);
                                skeletons[mainId][subId] = Skeleton.decode(buffer, skin, dos);

// ✅ FIX: proper debug
                                System.out.printf("Normal frame: mainId=%d, subId=%d, skinId=%d, hasSkeletal=%s%n",
                                        mainId, subId, skinId, skin.hasSkeletal());

                            }
                        }
                    } else {
                        System.out.println("No normal frame container for mainId=" + mainId);
                    }

                    // 2️⃣ Skeletal keyframes after normal frames
                    Container skeletalContainer = cache.read(CacheIndex.SKELETAL_KEYFRAMES, mainId);
                    if (skeletalContainer == null || skeletalContainer.getData().remaining() == 0) {
                        System.out.println("No skeletal keyframes found for mainId=" + mainId);
                    } else {
                        ByteBuffer skeletalBuffer = skeletalContainer.getData().duplicate();
                        skeletalBuffer.rewind();
                        int subIdOffset = (skeletons[mainId] != null) ? skeletons[mainId].length : 0;

                        System.out.printf("Skeletal keyframes detected for mainId=%d, buffer remaining=%d%n",
                                mainId, skeletalBuffer.remaining());

                        try {
                            AnimKeyFrameSet keyFrameSet = AnimKeyFrameSet.load(mainId, skeletalBuffer);
                            if (keyFrameSet == null || keyFrameSet.base.transforms_count() == 0) {
                                System.out.println("No valid skeletal keyframes for mainId=" + mainId);
                            } else {
                                for (int i = 0; i < keyFrameSet.base.transforms_count(); i++) {
                                    dos.writeShort(subIdOffset + i);
                                    keyFrameSet.encode(dos, i);
                                    System.out.printf("Wrote skeletal keyframe: mainId=%d, keyframeIndex=%d%n", mainId, i);
                                }
                            }

                        } catch (Exception e) {
                            System.err.println("⚠️ Failed loading skeletal keyframes for mainId=" + mainId);
                            e.printStackTrace();
                            byte[] raw = new byte[skeletalBuffer.remaining()];
                            skeletalBuffer.get(raw);
                            dos.write(raw);
                        }
                    }
                }

                // Write merged output
                try (FileOutputStream fos = new FileOutputStream(new File(dir, mainId + ".gz"))) {
                    fos.write(CompressionUtils.gzip(bos.toByteArray()));
                }

                System.out.printf("Progress: %.2f%%%n", (double)(mainId+1)/skeletonTable.capacity()*100);
            }

            System.out.println("✅ Finished dumping normal frames + skeletal keyframes.");
        }
    }
}
