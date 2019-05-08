package com.xokundevs.cchmavenserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final int puerto = 55555;

    public static void main(String[] args) {
        try {
            ServerSocket ssk = new ServerSocket(puerto);
            System.out.println("Escuchando");

            while (true) {
                Socket sk = ssk.accept();
                Cliente ser = new Cliente(sk);
                ser.start();
            }

        } catch (IOException ex) {
            System.out.println(ex);
        }
    }

    static class Cliente extends Thread {

        Socket sk;
        DataInputStream dis;
        DataOutputStream dos;
        String name;

        Cliente(Socket sk) throws IOException {
            this.sk = sk;
            this.dis = new DataInputStream(sk.getInputStream());
            this.dos = new DataOutputStream(sk.getOutputStream());
        }

        @Override
        public void run() {
            try {
                name = dis.readUTF();
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }
}
