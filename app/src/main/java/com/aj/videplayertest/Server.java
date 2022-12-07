package com.aj.videplayertest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Server implements Runnable
{

    private Thread thread;
    private ServerSocket serverSocket;
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private OnClientConnected clientConnected;
    private long packetCounter =0 ;
    public Server(OnClientConnected clientConnected)
    {
        this.thread = new Thread( this );
        this.thread.setPriority( Thread.NORM_PRIORITY );
        this.thread.start();
        this.clientConnected = clientConnected;
    }

    @Override
    public void run()
    {
        // create a server socket
        try
        {
            this.serverSocket = new ServerSocket( 12345 );
        }
        catch ( IOException e )
        {
            System.out.println( "failed to start server socket" );
            this.clientConnected.onClientError(e.getMessage());

            e.printStackTrace();
        }

        // wait for a connection
        System.out.println( "waiting for connections..." );
        try
        {
            this.socket = serverSocket.accept();
        }
        catch ( IOException e )
        {
            System.out.println( "failed to accept" );
            this.clientConnected.onClientError(e.getMessage());

            e.printStackTrace();
        }
        System.out.println( "client connected" );
        this.clientConnected.onClientConnected();
        // create input and output streams
        try
        {
            this.dataInputStream = new DataInputStream( new BufferedInputStream( this.socket.getInputStream() ) );
            this.dataOutputStream = new DataOutputStream( new BufferedOutputStream( this.socket.getOutputStream() ) );
        }
        catch ( IOException e )
        {
            System.out.println( "failed to create streams" );
            this.clientConnected.onClientError(e.getMessage());

            e.printStackTrace();
        }

        // send some test data
//        try
//        {
//            this.dataOutputStream.writeInt( 123 );
//            this.dataOutputStream.flush();
//        }
//        catch ( IOException e )
//        {
//            System.out.println( "failed to send" );
//            e.printStackTrace();
//            this.clientConnected.onClientError(e.getMessage());
//
//        }

        // placeholder recv loop
        long start_time= System.currentTimeMillis();

        while ( true )
        {
            try
            {
                byte[] buffer = new byte[1024];
               /// byte b =this.dataInputStream.readByte();
                this.dataInputStream.read(buffer);
               // String s = new String(b, StandardCharsets.UTF_8);

                //test.
                //this.dataInputStream.
               // System.out.println( "byte received: "+test );
                this.clientConnected.onMessageRecived();
//                Long tsLong = System.currentTimeMillis()/1000;
//                String ts = tsLong.toString();
                packetCounter++;
                //this.clientConnected.onFrameRecieved(buffer);
                long end_time =System.currentTimeMillis();
                String s =""+ (end_time-start_time)/1000;

                this.clientConnected.onClientError("Total packet  = " + packetCounter +" "+s);
//                this.dataInputStream.

//                if ( test == 42 ) {
//                    break;
//                }
            }
            catch ( IOException e )
            {
                e.printStackTrace();
                this.clientConnected.onClientError(e.getMessage());

                break;
            }
        }
        this.clientConnected.onClientDisconnected("Client disconnected");

        System.out.println( "server thread stopped" );
    }


}