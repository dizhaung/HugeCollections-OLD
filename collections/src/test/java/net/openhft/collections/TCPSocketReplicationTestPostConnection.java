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
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

import static java.util.concurrent.TimeUnit.SECONDS;
import static net.openhft.collections.Builder.getPersistenceFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test  VanillaSharedReplicatedHashMap where the Replicated is over a TCP Socket
 *
 * @author Rob Austin.
 */

public class TCPSocketReplicationTestPostConnection {

    private SharedHashMap<Integer, CharSequence> map1;
    private SharedHashMap<Integer, CharSequence> map2;

    @Test
    public void testPostConnection() throws IOException, InterruptedException {


        map1 = TCPSocketReplication4WayMapTest.newTcpSocketShmIntString((byte) 1, 8076);
        map1.put(5, "EXAMPLE-2");
        Thread.sleep(1);
        map2 = TCPSocketReplication4WayMapTest.newTcpSocketShmIntString((byte) 2, 8077, new InetSocketAddress("localhost", 8076));

        // allow time for the recompilation to resolve
        waitTillEqual(500);

        assertEquals(map1, map2);
        assertTrue(!map1.isEmpty());

    }

    @Test
    public void testPostConnectionNoSleep() throws IOException, InterruptedException {


        map1 = TCPSocketReplication4WayMapTest.newTcpSocketShmIntString((byte) 1, 8076);
        map1.put(5, "EXAMPLE-2");
        map2 = TCPSocketReplication4WayMapTest.newTcpSocketShmIntString((byte) 2, 8077, new InetSocketAddress("localhost", 8076));

        // allow time for the recompilation to resolve
        waitTillEqual(1000);

        assertEquals(map1, map2);
        assertTrue(!map1.isEmpty());

    }

    @Test
    public void testBootStrapIntoNewMapWithNewFile() throws IOException, InterruptedException {

        final TcpReplicatorBuilder tcpReplicatorBuilder =
                new TcpReplicatorBuilder(8077, new InetSocketAddress("localhost", 8076))
                        .heartBeatInterval(1, SECONDS);


        final SharedHashMapBuilder builder = new SharedHashMapBuilder()
                .entries(1000)
                .identifier((byte) 2)
                .tcpReplicatorBuilder(tcpReplicatorBuilder)
                .entries(20000);
        final SharedHashMap<Integer, CharSequence> map2a = (SharedHashMap<Integer, CharSequence>) builder.file(getPersistenceFile()).kClass(Integer.class).vClass(CharSequence.class).create();
        map1 = TCPSocketReplication4WayMapTest.newTcpSocketShmIntString((byte) 1, 8076);

        Thread.sleep(1);
        map1.put(5, "EXAMPLE-2");

        map2a.close();


        Thread.sleep(1);
        map1.put(6, "EXAMPLE-1");

        // recreate map2 with new unique file
        map2 = builder.file(getPersistenceFile()).kClass(Integer.class).vClass(CharSequence.class).create();


        // allow time for the recompilation to resolve
        waitTillEqual(5000);

        assertEquals(map1, map2);
        assertTrue(!map1.isEmpty());
        assertTrue(map2.get(6).equals("EXAMPLE-1"));

    }


    @After
    public void tearDown() {

        for (final Closeable closeable : new Closeable[]{map1, map2}) {
            try {
                if (closeable != null)
                    closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * * waits until map1 and map2 show the same value
     *
     * @param timeOutMs timeout in milliseconds
     * @throws InterruptedException
     */
    private void waitTillEqual(final int timeOutMs) throws InterruptedException {
        int t = 0;
        for (; t < timeOutMs; t++) {
            if (map1.equals(map2))
                break;
            Thread.sleep(1);
        }

    }

}



