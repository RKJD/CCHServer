package com.xokundevs.cchmavenserver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.Scanner;

import com.xokundevs.cchmavenserver.bddconnectivity.util.HibernateUtil;
import com.xokundevs.cchmavenserver.model.Cliente;

public class Main {

    static {
        int puerto = Integer.MIN_VALUE;
        String asymmetric_algorithm = null, asymmetric_cipher_format = null, symmetric_algorithm = null,
                symmetric_cipher_format = null;

        try (InputStream is = new FileInputStream("Server.properties")) {
            Properties properties = new Properties();
            properties.load(is);

            Object temp;
            puerto = ((temp = properties.get("puerto")) == null) ? Integer.MIN_VALUE
                    : Integer.parseInt(temp.toString());
            asymmetric_algorithm = properties.getProperty("asymmetric_algorithm");
            asymmetric_cipher_format = properties.getProperty("asymmetric_cipher_format");
            symmetric_algorithm = properties.getProperty("symmetric_algorithm");
            symmetric_cipher_format = properties.getProperty("symmetric_cipher_format");

        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Server.properties not found");
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read Server.properties");
        }
        if (puerto == Integer.MIN_VALUE || asymmetric_algorithm == null || asymmetric_cipher_format == null
                || symmetric_algorithm == null || symmetric_cipher_format == null) {
            throw new NullPointerException("null or non existant properties");
        } else {
            PUERTO = puerto;
            ASYMMETRIC_ALGORITHM = asymmetric_algorithm;
            ASYMMETRIC_CIPHER_FORMAT = asymmetric_cipher_format;
            SYMMETRIC_ALGORITHM = symmetric_algorithm;
            SYMMETRIC_CIPHER_FORMAT = symmetric_cipher_format;
        }
    }

    public static final int PUERTO;
    public static KeyPair pairKeys = null;

    // ENCODING VARIABLES
    public static final String ASYMMETRIC_ALGORITHM;
    public static final String ASYMMETRIC_CIPHER_FORMAT;
    public static final String SYMMETRIC_ALGORITHM;
    public static final String SYMMETRIC_CIPHER_FORMAT;

    public static void main(String[] args) throws NoSuchAlgorithmException {
        HibernateUtil.getSessionFactory();

        KeyPairGenerator kpg = KeyPairGenerator.getInstance(ASYMMETRIC_ALGORITHM);
        pairKeys = kpg.genKeyPair();
        Scanner sc = new Scanner(System.in);


        try(ServerSocket ssk = new ServerSocket(PUERTO)) {
            
            System.out.println("Escuchando");
            new Thread(new Runnable(){
                @Override
                public void run() {
                    Main.run(ssk);
                }
            }).start();

            while(!sc.nextLine().equals("close"));

        } catch (IOException exception) {
            System.out.println(exception);
        }

        sc.close();
    }

    private static void run(ServerSocket ssk) {
        if (ssk != null) {
            while (!ssk.isClosed()) {
                try {
                    Socket sk = ssk.accept();
                    Cliente ser = new Cliente(sk);
                    ser.start();
                    System.out.println("Connexión entrante");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
