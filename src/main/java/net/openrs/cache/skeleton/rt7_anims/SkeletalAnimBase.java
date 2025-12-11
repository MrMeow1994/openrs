package net.openrs.cache.skeleton.rt7_anims;


import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SkeletalAnimBase extends AnimKeyFrameSet {
   public AnimationBone[] bones;
   public int max_connections;
   // Constructor to read data
   public SkeletalAnimBase(ByteBuffer packet, int count) {
      this.bones = new AnimationBone[count];
       max_connections = packet.get() & 0xFF;

      for (int i = 0; i < count; ++i) {
         // Initialize bones without processing transformations
         this.bones[i] = new AnimationBone(max_connections, packet, false);
      }
   }

   // Encode method to write data
   public void encode(DataOutputStream dos) throws IOException {
      // First, write max_connections like the constructor expects
      dos.writeByte(max_connections);

      // Then encode each bone
      for (AnimationBone bone : bones) {
         bone.encode(dos, false);
      }
   }

}
