package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ByteProcessor;
import io.netty.util.CharsetUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2021/9/22
 */
public class CommandDecoder extends ByteToMessageDecoder {

    private List<Command> commands;

    private byte[][] bytes;
    private int index = 0;

    private int commandDecodeMaxBatchSize = Constants.Server.commandDecodeMaxBatchSize;
    private int commandDecodeBufferInitializerSize = Constants.Server.commandDecodeBufferInitializerSize;

    public CommandDecoder(int commandDecodeMaxBatchSize, int commandDecodeBufferInitializerSize) {
        super();
        if (commandDecodeMaxBatchSize > 0) {
            this.commandDecodeMaxBatchSize = commandDecodeMaxBatchSize;
        }
        if (commandDecodeBufferInitializerSize > 0) {
            this.commandDecodeBufferInitializerSize = commandDecodeBufferInitializerSize;
        }
        this.commands = new ArrayList<>(this.commandDecodeBufferInitializerSize);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        try {
            while (true) {
                if (bytes == null) {
                    if (in.readableBytes() <= 0) {
                        return;
                    }
                    int readerIndex = in.readerIndex();
                    byte b = in.readByte();
                    if (b == '*') {
                        ByteBuf byteBuf = readLine(in);
                        if (byteBuf == null) {
                            in.readerIndex(readerIndex);
                            return;
                        }
                        int number = (int)parseRedisNumber(byteBuf);
                        bytes = new byte[number][];
                    } else {
                        throw new IllegalArgumentException();
                    }
                } else {
                    int numArgs = bytes.length;
                    for (int i = index; i < numArgs; i++) {
                        if (in.readableBytes() <= 0) {
                            return;
                        }
                        int readerIndex = in.readerIndex();
                        if (in.readByte() == '$') {
                            ByteBuf byteBuf = readLine(in);
                            if (byteBuf == null) {
                                in.readerIndex(readerIndex);
                                return;
                            }
                            int size = (int)parseRedisNumber(byteBuf);
                            if (in.readableBytes() >= size + 2) {
                                bytes[i] = new byte[size];
                                in.readBytes(bytes[i]);
                                in.skipBytes(2);
                            } else {
                                in.readerIndex(readerIndex);
                                return;
                            }
                            index = i+1;
                        } else {
                            throw new IllegalArgumentException("Unexpected character");
                        }
                    }
                    try {
                        Command command = new Command(bytes);
                        commands.add(command);
                        if (commands.size() >= commandDecodeMaxBatchSize) {
                            out.add(commands);
                            commands = new ArrayList<>(commandDecodeBufferInitializerSize);
                        }
                    } finally {
                        bytes = null;
                        index = 0;
                    }
                }
            }
        } finally {
            if (!commands.isEmpty()) {
                out.add(commands);
                commands = new ArrayList<>(commandDecodeBufferInitializerSize);
            }
        }
    }

    private static final int POSITIVE_LONG_MAX_LENGTH = 19; // length of Long.MAX_VALUE
    private static final int EOL_LENGTH = 2;

    private final NumberProcessor numberProcessor = new NumberProcessor();

    private long parseRedisNumber(ByteBuf byteBuf) {
        final int readableBytes = byteBuf.readableBytes();
        final boolean negative = readableBytes > 0 && byteBuf.getByte(byteBuf.readerIndex()) == '-';
        final int extraOneByteForNegative = negative ? 1 : 0;
        if (readableBytes <= extraOneByteForNegative) {
            throw new IllegalArgumentException("no number to parse: " + byteBuf.toString(CharsetUtil.US_ASCII));
        }
        if (readableBytes > POSITIVE_LONG_MAX_LENGTH + extraOneByteForNegative) {
            throw new IllegalArgumentException("too many characters to be a valid RESP Integer: " +
                    byteBuf.toString(CharsetUtil.US_ASCII));
        }
        if (negative) {
            numberProcessor.reset();
            byteBuf.skipBytes(extraOneByteForNegative);
            byteBuf.forEachByte(numberProcessor);
            return -1 * numberProcessor.content();
        }
        numberProcessor.reset();
        byteBuf.forEachByte(numberProcessor);
        return numberProcessor.content();
    }

    private static final class NumberProcessor implements ByteProcessor {
        private long result;
        @Override
        public boolean process(byte value) {
            if (value < '0' || value > '9') {
                throw new IllegalArgumentException("bad byte in number: " + value);
            }
            result = result * 10 + (value - '0');
            return true;
        }
        public long content() {
            return result;
        }
        public void reset() {
            result = 0;
        }
    }

    private static ByteBuf readLine(ByteBuf in) {
        if (!in.isReadable(EOL_LENGTH)) {
            return null;
        }
        final int lfIndex = in.forEachByte(ByteProcessor.FIND_LF);
        if (lfIndex < 0) {
            return null;
        }
        ByteBuf data = in.readSlice(lfIndex - in.readerIndex() - 1); // `-1` is for CR
        in.skipBytes(2);
        return data;
    }
}
