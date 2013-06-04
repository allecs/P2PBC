package server;


import protocol.Request;
import protocol.Response;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Vector;

public abstract class ServerBase {
    protected Status status;
    protected ByteBuffer buf;
    protected final byte[] bufLock;
    protected long current;

    protected Vector<InetSocketAddress> children;
    protected byte[] childrenLock;
    protected int maxChildrenNum;
    protected int listenPort;

    protected ServerBase(int bufSize, int maxChildrenNum, int listenPort) throws SocketException, InterruptedException {
        status = Status.NO_DATA;
        buf = ByteBuffer.allocate(bufSize);
        bufLock = new byte[0];
        current = -1;
        children = new Vector<InetSocketAddress>();
        childrenLock = new byte[0];
        this.maxChildrenNum = maxChildrenNum;
        this.listenPort = listenPort;
        start();
    }

    protected void start() throws SocketException, InterruptedException {
        Thread l = new Listener();
        l.start();
        Thread b = new Broadcaster();
        b.start();
    }

    private class Listener extends Thread{
        DatagramSocket socket;
        DatagramPacket packet;
        byte[] buffer;

        private Listener() throws SocketException {
            socket = new DatagramSocket(listenPort);
        }

        public void run(){
            while (true){
                try {
                    buffer = new byte[1024];
                    packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    new Handler(packet).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private class Handler extends Thread{
            private DatagramPacket packet;
            public Handler(DatagramPacket packet) {
                this.packet = packet;
            }

            public void run(){
                byte[] bufsend = null;
                byte[] bufrecv = new byte[1024];


                Request request = null;
                ByteArrayInputStream bais =
                        new ByteArrayInputStream(packet.getData());
                ObjectInputStream in = null;
                try {
                    in = new ObjectInputStream(bais);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    request = (Request) in.readObject();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    in.close();
                    bais.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ByteArrayOutputStream baos = null;
                if (inChildren(packet.getSocketAddress()) && request.req ==
                        Request.reqCode.SYN){//seems client missed a SYN_ACK
                    try {
                        Response response = new Response(Response.retCode.SYN_ACK, current, null);
                        baos = new ByteArrayOutputStream(1024);
                        ObjectOutputStream out =
                                new ObjectOutputStream(baos);
                        out.writeObject(request);
                        out.close();
                        makeAndSend(baos.toByteArray(), packet.getSocketAddress());
                        baos.close();
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (inChildren(packet.getSocketAddress()) && request.req ==
                        Request.reqCode.RST){//remove
                    synchronized (childrenLock){
                        children.remove(getIndex((InetSocketAddress)packet.getSocketAddress()));
                        System.out.println("Child "+packet.getSocketAddress()+" removed");
                    }
                }
                else if (request.req == Request.reqCode.SYN){
                    boolean full;
                    InetSocketAddress child1 = null;
                    InetSocketAddress child2 = null;
                    synchronized (childrenLock){
                        if (children.size() < maxChildrenNum){
                            full = false;
                            children.add((InetSocketAddress) packet.getSocketAddress());
                            System.out.println("Child "+packet.getSocketAddress()+" added");
                            Response response = new Response(Response.retCode.SYN_ACK,
                                    current, null);
                            ObjectOutputStream out = null;
                            try {
                                baos = new ByteArrayOutputStream(1024);
                                out = new ObjectOutputStream(baos);
                                out.writeObject(response);
                                out.close();
                                makeAndSend(baos.toByteArray(), packet.getSocketAddress());
                                baos.close();
                                return;
                            } catch (IOException e) {
                                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            }
                        }
                        else{
                            full = true;
                            child1 = new InetSocketAddress(children.firstElement().getHostName(), children.firstElement().getPort()-1);
                            child2 = new InetSocketAddress(children.lastElement().getHostName(), children.lastElement().getPort()-1);
                        }
                    }
                    if (full){
                        try {
                            Response response = new Response(Response.retCode.RECURSION,
                                    current, new InetSocketAddress[]{child1, child2});
                            baos = new ByteArrayOutputStream(1024);
                            ObjectOutputStream out = new ObjectOutputStream(baos);
                            out.writeObject(response);
                            out.close();
                            makeAndSend(baos.toByteArray(), packet.getSocketAddress());
                            baos.close();
                            return;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            private void appendCurrent(ObjectOutputStream oos) throws IOException {
                oos.write(buffer);
            }
        }
    }

    private int getIndex(InetSocketAddress socketAddress) {
        for(int i = 0; i < children.size(); ++i){
            if (children.get(i).equals(socketAddress)) return i;
        }
        return -1;
    }


    private class Broadcaster extends Thread{
        private long lastSent;
        private byte[] toBeSent;

        private Broadcaster() {
            lastSent = -1;
        }

        public void run(){
            InetSocketAddress[] curList = new InetSocketAddress[0];
            while (true){
                if (current != lastSent){
                    synchronized (bufLock){
                        lastSent = current;
                        toBeSent = buf.array().clone();
                    }
                    synchronized (childrenLock){
                        curList = children.toArray(curList);
                    }
                    for (Object child: curList){
                        makeAndSend(toBeSent, new InetSocketAddress(((InetSocketAddress)child).
                                getHostName(), ((InetSocketAddress)child).getPort()+1));
                    }
                }
                try {
                    sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void makeAndSend(byte[] bytes, SocketAddress socketAddress) {
        try {
            DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length, socketAddress);
            new DatagramSocket().send(datagramPacket);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected boolean inChildren(SocketAddress inetSocketAddress){
        synchronized (childrenLock){
            for (InetSocketAddress addr :children){
                if (addr.getHostName().equals(((InetSocketAddress)inetSocketAddress).getHostName()) &&
                        addr.getPort() == ((InetSocketAddress)inetSocketAddress).getPort()){
                    return true;
                }
            }
        }
        return false;
    }
}
