package com.xokundevs.cchmavenserver;

import com.xokundevs.cchmavenserver.bddconnectivity.dao.UsuarioDao;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Usuario;
import com.xokundevs.cchmavenserver.bddconnectivity.util.HibernateUtil;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Main {

    private static final int puerto = 55555;
    private static KeyPair pairKeys = null;
    private static final String ASSIMETRIC_ALGORITHM = "RSA";
    private static final String ASSIMETRIC_CIPHER_FORMAT = "RSA/ECB/PKCS1PADDING";
    private static final String SIMETRIC_ALGORITHM = "AES";
    private static final String SIMETRIC_CIPHER_FORMAT = "AES/ECB/PKCS5Padding";

    public static void main(String[] args) throws NoSuchAlgorithmException {
        HibernateUtil.getSessionFactory();
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(ASSIMETRIC_ALGORITHM);
        pairKeys = kpg.genKeyPair();

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

        //ORDENES
        private static final int NO = -1;
        private static final int OK = 1;
        private static final int LOGIN = 2;
        private static final int CREATE_USER = 101;
        private static final int BLOCK_SIZE = 1024;
        Socket sk;
        DataInputStream dis;
        DataOutputStream dos;
        private Usuario user;

        Cliente(Socket sk) throws IOException {
            this.sk = sk;
            this.dis = new DataInputStream(sk.getInputStream());
            this.dos = new DataOutputStream(sk.getOutputStream());
        }

        @Override
        public void run() {
            try {
                int code = dis.readInt();
                if(code == CREATE_USER){
                    SecretKey simetricKey = sendPublicKeyAndRecieveAES();
                    String email = new String(decodeSimetricEncode(fromHex(dis.readUTF())
                            , simetricKey));
                    String contra = toHex(decodeSimetricEncode(fromHex(dis.readUTF())
                            , simetricKey));
                    String name = new String(decodeSimetricEncode(fromHex(dis.readUTF())
                            , simetricKey));
                    
                    System.out.println(email +" " + contra + " " + name);
                    Usuario user = new Usuario(email);
                    user.setContrasenya(contra);
                    user.setNombreUsuario(name);
                    
                    if(!UsuarioDao.getInstance().exists(user)){
                        UsuarioDao.getInstance().saveUsuario(user);
                        dos.writeInt(OK);
                    }else{
                        dos.writeInt(NO);
                    }
                }

                //String passwordUtf = dis.readUTF();
                //Cipher c = Cipher.getInstance("AES");
                //c.init(Cipher.DECRYPT_MODE, messageCodifier);
                //System.out.println(toHex(c.doFinal(fromHex(passwordUtf))));
            } catch (IOException | NoSuchAlgorithmException
                    | NoSuchPaddingException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
            finally{
                try{
                    sk.close();
                }catch(IOException e){
                    
                }
            }
        }

        /**
         * Envia la clave publica y recibe la clave AES para la codificación
         *
         * @throws IOException
         * @throws NoSuchAlgorithmException
         * @throws NoSuchPaddingException
         */
        public SecretKey sendPublicKeyAndRecieveAES() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
            byte[] publicKey = pairKeys.getPublic().getEncoded();
            //System.out.println(toHex(publicKey));
            dos.writeUTF(toHex(publicKey));

            String codeRecieved = dis.readUTF();
            byte[] claveCodificacion = fromHex(codeRecieved);
            
            SecretKey secretKey = null;
            try {
                Cipher c1 = Cipher.getInstance(ASSIMETRIC_CIPHER_FORMAT);
                c1.init(Cipher.DECRYPT_MODE, pairKeys.getPrivate());
                byte[] asdf = c1.doFinal(claveCodificacion);
                System.out.println(toHex(asdf));
                secretKey = new SecretKeySpec(asdf, SIMETRIC_ALGORITHM);
                
            } catch (InvalidKeyException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalBlockSizeException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BadPaddingException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
            return secretKey;
        }
        
        public byte[] decodeSimetricEncode(byte[] encoded, SecretKey simetricKey){
            byte[] decoded = null;
            try{
                Cipher c = Cipher.getInstance(SIMETRIC_CIPHER_FORMAT);
                c.init(Cipher.DECRYPT_MODE, simetricKey);
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

    public static byte[] fromHex(String hex) {
        int length = hex.length() / 2;
        byte[] array = new byte[length];
        for (int i = 0; i < length; i++) {
            String firstPart = hex.charAt(i * 2) + "", secondPart = hex.charAt(i * 2 + 1) + "";
            byte by = 0;
            try {
                by = (byte) (Integer.parseInt(firstPart) * 16);
            } catch (NumberFormatException e) {
                switch (firstPart) {
                    case "a":
                        by = (byte) (10 * 16);
                        break;
                    case "b":
                        by = (byte) (11 * 16);
                        break;
                    case "c":
                        by = (byte) (12 * 16);
                        break;
                    case "d":
                        by = (byte) (13 * 16);
                        break;
                    case "e":
                        by = (byte) (14 * 16);
                        break;
                    case "f":
                        by = (byte) (15 * 16);
                        break;
                }
            }
            try {
                by += (byte) (Integer.parseInt(secondPart));
            } catch (NumberFormatException e) {
                switch (secondPart) {
                    case "a":
                        by += 10;
                        break;
                    case "b":
                        by += 11;
                        break;
                    case "c":
                        by += 12;
                        break;
                    case "d":
                        by += 13;
                        break;
                    case "e":
                        by += 14;
                        break;
                    case "f":
                        by += 15;
                        break;
                }
            }
            array[i] = by;
        }
        return array;
    }

    public static String toHex(byte[] array) {
        StringBuilder str = new StringBuilder();

        for (int i = 0; i < array.length; i++) {
            int a = array[i] & 0xf;
            int b = (array[i] & 0xf0) >> 4;
            if (b < 10) {
                str.append(b);
            } else {
                str.append(Integer.toHexString(b));
            }
            if (a < 10) {
                str.append(a);
            } else {
                str.append(Integer.toHexString(a));
            }
        }

        return str.toString();
    }
}
