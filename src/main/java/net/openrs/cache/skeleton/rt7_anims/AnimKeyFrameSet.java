package net.openrs.cache.skeleton.rt7_anims;

import net.openrs.cache.skeleton.Skin;
import net.openrs.cache.util.jagex.core.constants.SerialEnum;
import net.openrs.cache.util.jagex.jagex3.math.Matrix4f;
import net.openrs.cache.util.jagex.jagex3.math.Quaternion;
import net.openrs.util.ByteBufferUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class AnimKeyFrameSet {
   boolean modifies_trans;
   AnimationKeyFrame[][] skeletal_transforms = null;
   public int frameset_id;
   int keyframe_id = 0;
   public AnimationKeyFrame[][] transforms = null;
   public Skin base;

   public static AnimKeyFrameSet[] keyframesetset;
   private int frame_size;
   private int rtog;
   private int rtog2;
   private int var4;
   public static Map<Integer, AnimKeyFrameSet> keyframesetMap = new HashMap<>();

   public static void register(int groupId, AnimKeyFrameSet set) {
      keyframesetMap.put(groupId, set);
   }

   public static AnimKeyFrameSet get(int groupId) {
      return keyframesetMap.get(groupId);
   }

   public static boolean contains(int groupId) {
      return keyframesetMap.containsKey(groupId);
   }

   public static void clear() {
      keyframesetMap.clear();
   }

   public AnimKeyFrameSet() {
   }

   public static AnimKeyFrameSet init() {
      return null;
   }

   public static AnimKeyFrameSet load(int group, ByteBuffer keyframeBuffer) {
      try {
         if (keyframeBuffer == null || keyframeBuffer.remaining() < 3) {
            System.err.println("⚠️ Buffer too small for keyframe set " + group);
            return null;
         }

         keyframeBuffer.mark();

         int baseSize = keyframeBuffer.getShort() & 0xFFFF;
         boolean baseDecoded = false;
         AnimKeyFrameSet keyframeSet = new AnimKeyFrameSet();
         keyframeSet.frameset_id = group;

         if (baseSize > 0 && keyframeBuffer.remaining() >= baseSize) {
            byte[] baseData = new byte[baseSize];
            keyframeBuffer.get(baseData);
            ByteBuffer baseBuffer = ByteBuffer.wrap(baseData);

            try {
               keyframeSet.base = Skin.decode(baseBuffer, false, baseSize);
               baseDecoded = true;
            } catch (RuntimeException e) {
               keyframesetMap.remove(group);
               System.err.println("Error decoding base for keyframe set: " + group + ", skipping base.");
               e.printStackTrace();
               keyframeSet.base = null;
            }
         } else {
            keyframeBuffer.reset(); // no base present
         }

         // Read version and baseId safely
         int version = 0;
         int baseId = 0;
         if (keyframeBuffer.remaining() >= 3) {
            version = keyframeBuffer.get() & 0xFF;
            baseId = keyframeBuffer.getShort() & 0xFFFF;
         }

         System.out.println("Loading keyframe set " + group + ", base_id: " + baseId + ", version: " + version);

         // Decode keyframes **only if possible**
         if (keyframeBuffer.remaining() > 0) {
            try {
               keyframeSet.decode(keyframeBuffer, version);
               register(group, keyframeSet);
            } catch (RuntimeException e) {
               keyframesetMap.remove(group);
               System.err.println("Error decoding keyframes for group: " + group + ". Buffer remaining: " + keyframeBuffer.remaining());
               e.printStackTrace();
            }
         }

         if (!baseDecoded) {
            System.out.println("⚠️ No valid base for keyframe set " + group + ", keyframes may still exist.");
         }

         return keyframeSet;

      } catch (BufferUnderflowException e) {
         System.err.println("⚠️ Buffer underflow when loading keyframe set " + group);
         e.printStackTrace();
         return null;
      } catch (Exception e) {
         System.err.println("Error unpacking keyframes for group: " + group);
         e.printStackTrace();
         return null;
      }
   }


   public void encode(DataOutputStream dos, int keyframeIndex) throws IOException {
      // Ensure transforms exist
      if (transforms == null || keyframeIndex >= transforms.length) {
         dos.writeInt(0); // write empty for missing group
         return;
      }

      AnimationKeyFrame[] group = transforms[keyframeIndex];
      if (group == null) {
         dos.writeInt(0); // empty group
         return;
      }

      // Write length of this keyframe group
      dos.writeInt(group.length);
      for (AnimationKeyFrame frame : group) {
         if (frame != null) {
            frame.encode(dos);
         } else {
            dos.writeInt(0); // placeholder for null frame
         }
      }

      // Only write base for the first keyframe
      if (keyframeIndex == 0) {
         if (base != null) {
            dos.writeBoolean(true);
            base.encode(dos, false);
         } else {
            dos.writeBoolean(false);
         }
      }
   }

   /**
    * Writes a signed short in OSRS style (big-endian) to a ByteBuffer.
    * Matches readShortOSRS() behavior.
    */
   public static void writeShortOSRS(ByteBuffer buffer, int value) {
      // Ensure value fits in signed 16-bit
      if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
         value = value & 0xFFFF; // wrap around to 16-bit unsigned
      }

      // Big-endian: high byte first
      buffer.put((byte) ((value >> 8) & 0xFF)); // high byte
      buffer.put((byte) (value & 0xFF));        // low byte
   }

   public static short readShortOSRS(ByteBuffer buffer) {
      // OSRS style: big-endian signed short
      int high = buffer.get() & 0xFF;
      int low = buffer.get() & 0xFF;
      int value = (high << 8) | low;

      if (value > Short.MAX_VALUE) {
         value -= 0x10000; // convert to signed short
      }
      return (short) value;
   }

   public void decode(ByteBuffer packet, int version) {
      int beforeRead = packet.position();

      rtog = readShortOSRS(packet);
      rtog2 = readShortOSRS(packet);
      this.keyframe_id = packet.get() & 0xFF;
      int frameCount = packet.getShort() & 0xFFFF;

      // --- SAFETY CHECKS ---
      SkeletalAnimBase skeletalBase = (this.base != null) ? this.base.get_skeletal_animbase() : null;

      if (skeletalBase != null) {
         this.skeletal_transforms = new AnimationKeyFrame[skeletalBase.bones.length][];
      } else {
         this.skeletal_transforms = new AnimationKeyFrame[0][];
         System.err.println("⚠️ No skeletal base for keyframe " + keyframe_id + ", skipping skeletal transforms.");
      }

      this.transforms = (this.base != null) ? new AnimationKeyFrame[this.base.transforms_count()][] : new AnimationKeyFrame[0][];

      // --- Read frames ---
      for (int i = 0; i < frameCount; i++) {
         int transformId = packet.get() & 0xFF;
         AnimTransform[] possibleTransforms = {AnimTransform.NULL, AnimTransform.VERTEX, AnimTransform.field1210,
                 AnimTransform.COLOUR, AnimTransform.TRANSPARENCY, AnimTransform.field1213};

         AnimTransform transform = (AnimTransform) SerialEnum.for_id(possibleTransforms, transformId);
         if (transform == null) transform = AnimTransform.NULL;

         int var14 = ByteBufferUtils.get_short(packet);
         AnimationChannel channel = AnimationChannel.lookup_by_id(packet.get() & 0xFF);
         AnimationKeyFrame keyFrame = new AnimationKeyFrame();
         keyFrame.deserialise(packet, version);

         int count = transform.get_dimensions();
         AnimationKeyFrame[][] targetArray = (AnimTransform.VERTEX == transform) ? this.skeletal_transforms : this.transforms;

         if (targetArray.length == 0) {
            // Skip if no skeletal/base or transforms
            continue;
         }

         if (var14 >= targetArray.length) {
            System.err.println("⚠️ Transform index out of bounds: " + var14 + " (length " + targetArray.length + ")");
            continue;
         }

         if (targetArray[var14] == null) {
            targetArray[var14] = new AnimationKeyFrame[count];
         }

         targetArray[var14][channel.get_component()] = keyFrame;

         if (AnimTransform.TRANSPARENCY == transform) {
            this.modifies_trans = true;
         }
      }

      int readSize = packet.position() - beforeRead;
      if (readSize != frame_size) {
         throw new RuntimeException("AnimKeyFrameSet size mismatch! keyframe " + keyframe_id
                 + ", frame size: " + frame_size + ", actual read: " + readSize);
      }
   }

}
