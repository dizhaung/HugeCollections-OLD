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

package net.openhft.collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;

import static net.openhft.collections.Builder.getPersistenceFile;
import static org.junit.Assert.assertTrue;

/**
 * Test  VanillaSharedReplicatedHashMap where the Replicated is over a TCP Socket
 *
 * @author Rob Austin.
 */

public class UDPSocketReplicationTest1 {

    static final int identifier = Integer.getInteger("hostId", 1).byteValue();
    private SharedHashMap<Integer, Integer> map1;

    @Before
    public void setup() throws IOException {

        final UdpReplicatorBuilder udpReplicatorBuilder = new UdpReplicatorBuilder(8079, "192.168.0.255");

        assertTrue(identifier >= 1 && identifier <= Byte.MAX_VALUE);

        map1 = new SharedHashMapBuilder()
                .entries(1000)
                .identifier((byte) identifier)
                .udpReplicatorBuilder(udpReplicatorBuilder)
                .entries(20000)
                .create(getPersistenceFile(), Integer.class, Integer.class);
    }

    @After
    public void tearDown() throws InterruptedException {

        for (final Closeable closeable : new Closeable[]{map1}) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Test
    @Ignore
    public void testContinueToReceive() throws IOException, InterruptedException {
        if (identifier <= 2)
            System.out.println("Publishing");
        int first = 0, updates = 0, last = 0;
        for (; ; ) {
            for (int i = 0; i >= 0; i++) {
                Thread.sleep(10);
                Integer val = map1.get(1);
                if (val == null) continue;
                if (first == 0) {
                    first = last = val;
                    continue;
                }
                if (val.intValue() != last)
                    updates++;
                int delta = val - first;
                if (identifier <= 2)
                    map1.put(identifier, i);

                if (delta > 1 && val.intValue() != last)
                    System.out.println("val: " + val+", ratio missed= " + 1000 * (delta - updates) / delta / 10.0 + " missed=" + (delta - updates));
                last = val;
            }
        }
    }


}


