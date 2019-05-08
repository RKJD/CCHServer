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
                Servidor ser = new Servidor(sk);
                ser.start();
            }

        } catch (IOException ex) {
            System.out.println(ex);
        }
    }

    static class Servidor extends Thread {

        Socket sk;

        Servidor(Socket sk) {
            this.sk = sk;
        }

        @Override
        public void run() {

        }
    }
}
