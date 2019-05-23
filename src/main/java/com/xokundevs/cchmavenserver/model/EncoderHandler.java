/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xokundevs.cchmavenserver.model;

import com.xokundevs.cchmavenserver.Main;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author Antonio
 */
public class EncoderHandler {

    static {
        String asymmetric_algorithm = null, asymmetric_cipher_format = null,
                symmetric_algorithm = null, symmetric_cipher_format = null;

        try (InputStream is = new FileInputStream("Server.properties")) {
            Properties properties = new Properties();
            properties.load(is);

            asymmetric_algorithm = properties.getProperty("asymmetric_algorithm");
            asymmetric_cipher_format = properties.getProperty("asymmetric_cipher_format");
            symmetric_algorithm = properties.getProperty("symmetric_algorithm");
            symmetric_cipher_format = properties.getProperty("symmetric_cipher_format");

        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Server.properties not found");
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read Server.properties");
        }
        if (asymmetric_algorithm == null || asymmetric_cipher_format == null || symmetric_algorithm == null
                || symmetric_cipher_format == null) {
            throw new NullPointerException("null or non existant properties");
        } else {
            ASYMMETRIC_ALGORITHM = asymmetric_algorithm;
            ASYMMETRIC_CIPHER_FORMAT = asymmetric_cipher_format;
            SYMMETRIC_ALGORITHM = symmetric_algorithm;
            SYMMETRIC_CIPHER_FORMAT = symmetric_cipher_format;
        }
    }

//ENCODING VARIABLES
    public static final String ASYMMETRIC_ALGORITHM;
    public static final String ASYMMETRIC_CIPHER_FORMAT;
    public static final String SYMMETRIC_ALGORITHM;
    public static final String SYMMETRIC_CIPHER_FORMAT;
    private static KeyPair asymmetricPair;

    public static PrivateKey getPrivateKey() {
        if (asymmetricPair == null) {
            synchronized (EncoderHandler.class) {
                if (asymmetricPair == null) {
                    try {
                        asymmetricPair = KeyPairGenerator.getInstance(ASYMMETRIC_ALGORITHM).generateKeyPair();
                    } catch (NoSuchAlgorithmException ex) {
                        Logger.getLogger(EncoderHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        return asymmetricPair.getPrivate();
    }

    public static PublicKey getPublicKey() {
        if (asymmetricPair == null) {
            synchronized (EncoderHandler.class) {
                if (asymmetricPair == null) {
                    try {
                        asymmetricPair = KeyPairGenerator.getInstance(ASYMMETRIC_ALGORITHM).generateKeyPair();
                    } catch (NoSuchAlgorithmException ex) {
                        Logger.getLogger(EncoderHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        return asymmetricPair.getPublic();
    }

    public static SecretKey encryptSecretKey(byte[] keyEncoded, boolean encode) {

        int mode = (encode)? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
        SecretKey key = null;
        try {
            Cipher c1 = Cipher.getInstance(ASYMMETRIC_CIPHER_FORMAT);
            c1.init(mode, getPrivateKey());
            byte[] asdf = c1.doFinal(keyEncoded);
            System.out.println(Parser.toHex(asdf));
            key = new SecretKeySpec(asdf, SYMMETRIC_ALGORITHM);
        } catch (InvalidKeyException | BadPaddingException
                | NoSuchAlgorithmException | NoSuchPaddingException
                | IllegalBlockSizeException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        return key;
    }

    public static int BLOCK_SIZE = 1024;

    public static void encodeFileSymmetricAlgorithm(File input, File output, SecretKey symmetricKey, boolean destroyTempFile, boolean encode) {

        if (input == null || output == null) {
            throw new NullPointerException("The input/output files cannot be null");
        }

        
        int mode = (encode)? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
        DataInputStream fileDis = null;
        DataOutputStream fileDos = null;

        try {

            fileDis = new DataInputStream(new FileInputStream(input));
            fileDos = new DataOutputStream(new FileOutputStream(output));
            byte[] dataToDecipher = new byte[BLOCK_SIZE];

            long length = input.length();
            long actual = 0;

            Cipher cipher = Cipher.getInstance(SYMMETRIC_CIPHER_FORMAT);
            cipher.init(mode, symmetricKey);

            while (length > actual) {
                int read;
                if (length - actual > BLOCK_SIZE) {
                    read = fileDis.read(dataToDecipher, 0, BLOCK_SIZE);
                } else {
                    read = fileDis.read(dataToDecipher, 0, (int) (length - actual));
                }

                actual += read;

                fileDos.write(cipher.update(dataToDecipher, 0, read));
            }

            fileDos.write(cipher.doFinal());

            fileDos.flush();

        } catch (NoSuchAlgorithmException | IOException | NoSuchPaddingException
                | InvalidKeyException | IllegalBlockSizeException
                | BadPaddingException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {fileDos.close();} catch (NullPointerException | IOException e){}
            try {fileDis.close();} catch (NullPointerException | IOException e){}
            if (destroyTempFile) {
            System.out.println("File deleted: " + input.delete());
        }
        }
    }
    
    public static byte[] encodeSimetricEncode(byte[] encoded, SecretKey simetricKey, boolean encode) {
        byte[] decoded = null;
        
        int mode = (encode)? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
        try {
            Cipher c = Cipher.getInstance(SYMMETRIC_CIPHER_FORMAT);
            c.init(mode, simetricKey);
            decoded = c.doFinal(encoded);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchPaddingException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        return decoded;
    }
}
