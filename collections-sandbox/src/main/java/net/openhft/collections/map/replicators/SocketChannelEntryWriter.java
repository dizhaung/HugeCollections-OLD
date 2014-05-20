/*
 * Copyright 2014 Higher Frequency Trading
 * <p/>
 * http://www.higherfrequencytrading.com
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.collections.map.replicators;

import net.openhft.collections.ReplicatedSharedHashMap;
import net.openhft.collections.VanillaSharedReplicatedHashMap;
import net.openhft.lang.io.ByteBufferBytes;
import net.openhft.lang.io.NativeBytes;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author Rob Austin.
 */
public class SocketChannelEntryWriter {

    private final int entryMaxSize;
    private final ByteBuffer byteBuffer;
    private final ByteBufferBytes bytes;
    private final ReplicatedSharedHashMap.EntryExternalizable externalizable;
    private final EntryCallback entryCallback = new EntryCallback();

    public SocketChannelEntryWriter(final int entryMaxSize,
                                    final short maxNumberOfEntriesPerChunk,
                                    @NotNull final ReplicatedSharedHashMap.EntryExternalizable externalizable) {
        this.entryMaxSize = entryMaxSize;
        byteBuffer = ByteBuffer.allocateDirect(entryMaxSize * maxNumberOfEntriesPerChunk);
        bytes = new ByteBufferBytes(byteBuffer);
        this.externalizable = externalizable;
    }


    /**
     * writes all the entries that have changed, to the tcp socket
     *
     * @param socketChannel
     * @param modificationIterator
     * @throws InterruptedException
     * @throws java.io.IOException
     */
    void writeAll(@NotNull final SocketChannel socketChannel,
                  final ReplicatedSharedHashMap.ModificationIterator modificationIterator) throws InterruptedException, IOException {


        //todo if bytes.position() ==0 it would make sense to call a blocking version of modificationIterator.nextEntry(entryCallback);
        for (; ; ) {

            byteBuffer.limit(byteBuffer.capacity());

            boolean wasDataRead = false;
            try {
                wasDataRead = modificationIterator.nextEntry(entryCallback);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
                int i = 1;
            }

            if (!wasDataRead && bytes.position() == 0)
                return;

            if (bytes.remaining() > entryMaxSize && (wasDataRead || bytes.position() == 0))
                continue;


            byteBuffer.limit((int) bytes.position());
            //        byteBuffer.position((int) bytes.position());

            socketChannel.write(byteBuffer);

            //   byteBuffer.position(byteBuffer.position() + bytesWritten);

            // clear the bytes for reuse, we can store a maximum of MAX_NUMBER_OF_ENTRIES_PER_CHUNK in this bytes

            if (byteBuffer.remaining() == 0) {
                byteBuffer.position(0);
                byteBuffer.limit(0);
            } else {
                byteBuffer.compact();
                byteBuffer.flip();
            }

            bytes.position(byteBuffer.limit());
            bytes.limit(bytes.capacity());

            // we've filled up one bytes lets give another channel a chance to send data
            return;
        }

    }


    /**
     * {@inheritDoc}
     */
    class EntryCallback implements VanillaSharedReplicatedHashMap.EntryCallback {

        /**
         * {@inheritDoc}
         */
        public boolean onEntry(final NativeBytes entry) {

            bytes.skip(2);
            final long start = (int) bytes.position();
            externalizable.writeExternalEntry(entry, bytes);

            if (bytes.position() - start == 0) {
                bytes.position(bytes.position() - 2);
                return false;
            }

            // write the length of the entry, just before the start, so when we read it back
            // we read the length of the entry first and hence know how many preceding bytes to read
            final int entrySize = (int) (bytes.position() - start);
            bytes.writeUnsignedShort(start - 2L, entrySize);

            return true;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void onAfterEntry() {

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onBeforeEntry() {

        }
    }

    public void sendWelcomeMessage(@NotNull final SocketChannel socketChannel,
                                   final long timeStampOfLastMessage,
                                   final int localIdentifier1) throws IOException {
        bytes.clear();
        byteBuffer.clear();

        // send a welcome message to the remote server to ask for data for our localIdentifier
        // and any missed messages
        bytes.writeByte(localIdentifier1);
        bytes.writeLong(timeStampOfLastMessage);
        byteBuffer.limit((int) bytes.position());
        socketChannel.write(byteBuffer);

        if (byteBuffer.remaining() == 0) {
            byteBuffer.position(0);
            byteBuffer.limit(0);
        } else {
            byteBuffer.compact();
            byteBuffer.flip();
        }

        bytes.position(byteBuffer.limit());
        bytes.limit(bytes.capacity());


    }


}

