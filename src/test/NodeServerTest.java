package test;

import server.NodeServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * Created with IntelliJ IDEA.
 * User: 立翔
 * Date: 13-6-4
 * Time: 上午3:37
 * To change this template use File | Settings | File Templates.
 */
public class NodeServerTest {
    public static void main(String[] args) throws SocketException, InterruptedException {
        /*NodeServer ns1 = new NodeServer(1024, 2, 12345, new InetSocketAddress("localhost", 6666), 12346);
        ns1.run();
        NodeServer ns2 = new NodeServer(1024, 2, 12348, new InetSocketAddress("localhost", 6666), 12349);
        ns2.run();
        NodeServer ns3 = new NodeServer(1024, 2, 12351, new InetSocketAddress("localhost", 6666), 12352);
        ns3.run();
        NodeServer ns4 = new NodeServer(1024, 2, 12354, new InetSocketAddress("localhost", 6666), 12355);
        ns4.run();
        */

        for (int i = 0; i<100; ++i){
            NodeServer ns = new NodeServer(1024, 2, 12345+3*i, new InetSocketAddress("localhost", 6666), 12346+3*i);
            ns.run();
        }
    }
}
