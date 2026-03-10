package org.loger.server;

import org.loger.data.TickData;
import org.loger.flatbuffers.FBTickData;
import org.loger.flatbuffers.FBTickDataSequence;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;
import com.google.flatbuffers.FlatBufferBuilder;

public class FlatBufferSerializer {
    private static final ThreadLocal<FlatBufferBuilder> BUILDER = ThreadLocal.withInitial(() -> {
        return new FlatBufferBuilder(4096);
    });

    public static byte[] serialize(List<TickData> ticks) {
        FlatBufferBuilder builder = BUILDER.get();
        builder.clear();
        int[] tickOffsets = new int[ticks.size()];
        for (int i = ticks.size() - 1; i >= 0; i--) {
            TickData tick = ticks.get(i);
            FBTickData.startFBTickData(builder);
            FBTickData.addDeltaYaw(builder, tick.deltaYaw);
            FBTickData.addDeltaPitch(builder, tick.deltaPitch);
            FBTickData.addAccelYaw(builder, tick.accelYaw);
            FBTickData.addAccelPitch(builder, tick.accelPitch);
            FBTickData.addJerkPitch(builder, tick.jerkPitch);
            FBTickData.addJerkYaw(builder, tick.jerkYaw);
            FBTickData.addGcdErrorYaw(builder, tick.gcdErrorYaw);
            FBTickData.addGcdErrorPitch(builder, tick.gcdErrorPitch);
            tickOffsets[i] = FBTickData.endFBTickData(builder);
        }
        int ticksVector = FBTickDataSequence.createTicksVector(builder, tickOffsets);
        FBTickDataSequence.startFBTickDataSequence(builder);
        FBTickDataSequence.addTicks(builder, ticksVector);
        int sequenceOffset = FBTickDataSequence.endFBTickDataSequence(builder);
        builder.finish(sequenceOffset);
        ByteBuffer buf = builder.dataBuffer();
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        return bytes;
    }

    public static byte[] serializeRaw(List<TickData> ticks) {
        ByteBuffer buf = ByteBuffer.allocate(ticks.size() * 8 * 4).order(ByteOrder.LITTLE_ENDIAN);
        FloatBuffer fb = buf.asFloatBuffer();
        for (TickData tick : ticks) {
            fb.put(tick.deltaYaw);
            fb.put(tick.deltaPitch);
            fb.put(tick.accelYaw);
            fb.put(tick.accelPitch);
            fb.put(tick.jerkPitch);
            fb.put(tick.jerkYaw);
            fb.put(tick.gcdErrorYaw);
            fb.put(tick.gcdErrorPitch);
        }
        return buf.array();
    }
}
