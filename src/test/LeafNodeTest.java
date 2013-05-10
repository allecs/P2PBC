package test;

import protocol.RequestHeader;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class LeafNodeTest {

    public static final int port = 5004;
    public static void main(String args[]) throws IOException, ClassNotFoundException {
        byte[] buf = new byte[1024];
        DatagramSocket controlSocket = new DatagramSocket(port);
        DatagramSocket dataSocket = new DatagramSocket(port + 1);


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;

        oos = new ObjectOutputStream(baos);
        oos.writeObject(new RequestHeader(RequestHeader.reqCode.SYN, 0));

        new DataReciever(dataSocket).start();

        byte[] req = baos.toByteArray();
        DatagramPacket datagramPacket = new DatagramPacket(req, req.length, new InetSocketAddress("localhost", 6666));
        controlSocket.send(datagramPacket);
        controlSocket.receive(datagramPacket);

        ByteArrayInputStream bais = new ByteArrayInputStream(datagramPacket.getData());
        ObjectInputStream ois = new ObjectInputStream(bais);

        Object resp = ois.readObject();
        System.out.println(resp.toString());

        //System.out.println("retcode = "+resp.ret+", seq = "+resp.seq+", children: "+resp.addr1+resp.addr2);

    }

    private static class DataReciever extends Thread{
        private byte[] data;
        private DatagramSocket socket;
        private DatagramPacket packet;

        private DataReciever(DatagramSocket socket) {
            this.socket = socket;
        }

        public void run(){
            while (true){
                data = new byte[1024];
                packet = new DatagramPacket(data, data.length);
                try {
                    socket.receive(packet);
                    System.out.println("recv data on "+socket.getLocalAddress()+":"+socket.getLocalPort()+
                            " from "+packet.getAddress()+":"+packet.getPort());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
