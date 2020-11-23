/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xokundevs.cchmavenserver.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 *
 * @author Antonio
 */
public class InternetControl {

    private static final int BLOCK_SIZE = 4096;

    private Socket sk;
    private DataInputStream dis;
    private DataOutputStream dos;
    private SecretKey secretKey;

    public InternetControl(Socket sk) throws IOException {

        this.sk = sk;
        dis = new DataInputStream(sk.getInputStream());
        dos = new DataOutputStream(sk.getOutputStream());
    }

    public void sendPublicKeyAndRecieveAES() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        byte[] publicKey = EncoderHandler.getPublicKey().getEncoded();

        dos.writeUTF(Parser.toHex(publicKey));

        String codeRecieved = dis.readUTF();
        byte[] claveCodificacion = Parser.fromHex(codeRecieved);

        secretKey = EncoderHandler.encryptSecretKey(claveCodificacion, false);
    }

    public void enviarInt(int num) throws IOException {
        dos.writeUTF(
                Parser.toHex(
                        EncoderHandler.encodeSimetricEncode(
                                Parser.fromHex(Parser.parseIntToHex(num)),
                                secretKey,
                                true
                        )
                )
        );
    }

    public void enviarLong(long num) throws IOException {
        dos.writeUTF(
                Parser.toHex(
                        EncoderHandler.encodeSimetricEncode(
                                Parser.fromHex(Parser.parseLongToHex(num)),
                                secretKey,
                                true
                        )
                )
        );
    }

    public void enviarHex(String hexString) throws IOException {
        dos.writeUTF(
                Parser.toHex(
                        EncoderHandler.encodeSimetricEncode(
                                Parser.fromHex(hexString),
                                secretKey,
                                true
                        )
                )
        );
    }

    public void enviarString(String frase) throws IOException {
        dos.writeUTF(
                Parser.toHex(
                        EncoderHandler.encodeSimetricEncode(
                                frase.getBytes(StandardCharsets.UTF_8),
                                secretKey,
                                true
                        )
                )
        );
    }
    
    public int recibirInt() throws IOException {
        return Parser.parseHexToInt(
                Parser.toHex(
                        EncoderHandler.encodeSimetricEncode(
                                Parser.fromHex(dis.readUTF()),
                                secretKey,
                                false
                        )
                )
        );
    }

    public String recibirHex() throws IOException {
        return Parser.toHex(
                EncoderHandler.encodeSimetricEncode(
                        Parser.fromHex(dis.readUTF()),
                        secretKey,
                        false
                )
        );
    }

    public String recibirString() throws IOException {
        return new String(
                EncoderHandler.encodeSimetricEncode(
                        Parser.fromHex(dis.readUTF()),
                        secretKey,
                        false
                ),
                StandardCharsets.UTF_8
        );
    }

    public long recibirLong() throws IOException {
        return Parser.parseHexToLong(
                Parser.toHex(
                        EncoderHandler.encodeSimetricEncode(
                                Parser.fromHex(dis.readUTF()),
                                secretKey,
                                false
                        )
                )
        );
    }

    public void createFileFromInput(File f, long length) throws IOException {
        long totalRead = 0;

        DataOutputStream fileDos = new DataOutputStream(new FileOutputStream(f));

        byte[] readData = new byte[BLOCK_SIZE];
        while (totalRead < length) {
            int readed;
            if (length - totalRead > BLOCK_SIZE) {
                readed = getDis().read(readData, 0, BLOCK_SIZE);
            } else {
                readed = getDis().read(readData, 0, (int) (length - totalRead));
            }
            totalRead += readed;
            fileDos.write(readData, 0, readed);
        }
        fileDos.flush();
        fileDos.close();
    }

    public DataInputStream getDis() {
        return dis;
    }

    public DataOutputStream getDos() {
        return dos;
    }

    public void SendFile(File f, SecretKey secretKey) {
        if (f == null) {
            throw new NullPointerException("Null file");
        } else if (!f.exists()) {
            throw new IllegalArgumentException("File doesn't exists");
        }

        DataInputStream disFile = null;
        try {
            disFile = new DataInputStream(new FileInputStream(f));

            enviarLong(f.length());

            long fileLength = f.length();
            long actual = 0;
            byte[] array = new byte[BLOCK_SIZE];
            while (fileLength > actual) {
                int leido;
                if (fileLength - actual > BLOCK_SIZE) {
                    leido = disFile.read(array, 0, BLOCK_SIZE);
                } else {
                    leido = disFile.read(array, 0, (int) (fileLength - actual));
                }
                actual += leido;
                dos.write(array, 0, leido);
            }
            dos.flush();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(InternetControl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(InternetControl.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                disFile.close();
            } catch (IOException | NullPointerException ignore) {
            }
        }
    }

}
