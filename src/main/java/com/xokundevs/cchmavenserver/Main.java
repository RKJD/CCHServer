package com.xokundevs.cchmavenserver;

import com.xokundevs.cchmavenserver.bddconnectivity.dao.UsuarioDao;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Usuario;
import com.xokundevs.cchmavenserver.bddconnectivity.util.HibernateUtil;
import com.xokundevs.cchmavenserver.model.Cliente;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Main {

    static {
        int puerto = Integer.MIN_VALUE;
        String asymmetric_algorithm = null, asymmetric_cipher_format = null,
                symmetric_algorithm = null, symmetric_cipher_format = null,
                root_directory_image = null;

        try (InputStream is = new FileInputStream("Server.properties")) {
            Properties properties = new Properties();
            properties.load(is);

            Object temp;
            puerto = ((temp = properties.get("puerto")) == null) ? Integer.MIN_VALUE : Integer.parseInt(temp.toString());
            asymmetric_algorithm = properties.getProperty("asymmetric_algorithm");
            asymmetric_cipher_format = properties.getProperty("asymmetric_cipher_format");
            symmetric_algorithm = properties.getProperty("symmetric_algorithm");
            symmetric_cipher_format = properties.getProperty("symmetric_cipher_format");
            root_directory_image = properties.getProperty("root_directory_image");

        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Server.properties not found");
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read Server.properties");
        }
        if (puerto == Integer.MIN_VALUE || asymmetric_algorithm == null
                || asymmetric_cipher_format == null || symmetric_algorithm == null
                || symmetric_cipher_format == null || root_directory_image == null) {
            throw new NullPointerException("null or non existant properties");
        } else {
            PUERTO = puerto;
            ASYMMETRIC_ALGORITHM = asymmetric_algorithm;
            ASYMMETRIC_CIPHER_FORMAT = asymmetric_cipher_format;
            SYMMETRIC_ALGORITHM = symmetric_algorithm;
            SYMMETRIC_CIPHER_FORMAT = symmetric_cipher_format;
            ROOT_IMAGE = root_directory_image;
        }
    }

    public static final int PUERTO;
    public static KeyPair pairKeys = null;

    //ENCODING VARIABLES
    public static final String ASYMMETRIC_ALGORITHM;
    public static final String ASYMMETRIC_CIPHER_FORMAT;
    public static final String SYMMETRIC_ALGORITHM;
    public static final String SYMMETRIC_CIPHER_FORMAT;

    //ROOT IMAGE DIRECTORY
    private static final String ROOT_IMAGE;

    public static void main(String[] args) throws NoSuchAlgorithmException {
        HibernateUtil.getSessionFactory();
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(ASYMMETRIC_ALGORITHM);
        pairKeys = kpg.genKeyPair();

        try {
            ServerSocket ssk = new ServerSocket(PUERTO);
            System.out.println("Escuchando");
            while (true) {
                Socket sk = ssk.accept();
                Cliente ser = new Cliente(sk);
                ser.start();
                System.out.println("Connexión entrante");
            }

        } catch (IOException ex) {
            System.out.println(ex);
        }
    }
}
