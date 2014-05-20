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

package net.openhft.chronicle.sandbox.map.shared;

import net.openhft.collections.SharedHashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.TreeMap;

import static net.openhft.collections.map.replicators.ClientTcpSocketReplicator.ClientPort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test  VanillaSharedReplicatedHashMap where the Replicated is over a TCP Socket
 *
 * @author Rob Austin.
 */

public class TCPSocketReplicationTest3way {


    private SharedHashMap<Integer, CharSequence> map1;
    private SharedHashMap<Integer, CharSequence> map2;
    private SharedHashMap<Integer, CharSequence> map3;

    static int i;

    @Before
    public void setup() throws IOException {
        map1 = TCPSocketReplication4WayMapTest.newSocketShmIntString((byte) 1, 8076, new ClientPort(8077, "localhost"), new ClientPort(8078, "localhost"));
        map2 = TCPSocketReplication4WayMapTest.newSocketShmIntString((byte) 2, 8077, new ClientPort(8078, "localhost"));
        map3 = TCPSocketReplication4WayMapTest.newSocketShmIntString((byte) 3, 8078);
    }

    @After
    public void tearDown() {

        // todo fix close, it not blocking ( in other-words we should wait till everything is closed before running the next test)

        try {
            map1.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            map2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            map3.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Test
    public void test3() throws IOException, InterruptedException {


        map3.put(5, "EXAMPLE-2");


        // allow time for the recompilation to resolve
        waitTillEqual(5000000);

        assertEquals(new TreeMap(map1), new TreeMap(map2));
        assertEquals(new TreeMap(map3), new TreeMap(map2));
        assertTrue(!map1.isEmpty());

    }

    @Test
    public void test() throws IOException, InterruptedException {

        map1.put(1, "EXAMPLE-1");
        map1.put(2, "EXAMPLE-2");
        map1.put(3, "EXAMPLE-1");

        map2.put(5, "EXAMPLE-2");
        map2.put(6, "EXAMPLE-2");

        map1.remove(2);
        map2.remove(3);
        map1.remove(3);
        map2.put(5, "EXAMPLE-2");

        // allow time for the recompilation to resolve
        waitTillEqual(5000);

        assertEquals(new TreeMap(map1), new TreeMap(map2));
        assertEquals(new TreeMap(map3), new TreeMap(map3));
        assertTrue(!map1.isEmpty());

    }


    private void waitTillEqual(final int timeOutMs) throws InterruptedException {
        int t = 0;
        for (; t < timeOutMs; t++) {
            if (new TreeMap(map1).equals(new TreeMap(map2)) &&
                    new TreeMap(map1).equals(new TreeMap(map3)))
                break;
            Thread.sleep(1);
        }

    }
}


