package com.xokundevs.cchmavenserver;

import com.xokundevs.cchmavenserver.bddconnectivity.dao.UsuarioDao;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Usuario;
import com.xokundevs.cchmavenserver.bddconnectivity.util.HibernateUtil;
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

    private static final int PUERTO;
    private static KeyPair pairKeys = null;

    //ENCODING VARIABLES
    private static final String ASYMMETRIC_ALGORITHM;
    private static final String ASYMMETRIC_CIPHER_FORMAT;
    private static final String SYMMETRIC_ALGORITHM;
    private static final String SYMMETRIC_CIPHER_FORMAT;

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
            }

        } catch (IOException ex) {
            System.out.println(ex);
        }
    }

    static class Cliente extends Thread {

        //ORDENES
        private static final int NO = -1;
        private static final int OK = 1;
        private static final int LOGIN = 102;
        private static final int CREATE_USER = 101;
        private static final int CREATE_USER_ERROR_EXISTING_USER = -1;
        private static final int CREATE_USER_ERROR_INVALID_EMAIL = -2;
        private static final int CREATE_USER_ERROR_INVALID_PARAMETERS = -3;
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
                if (code == CREATE_USER) {
                    SecretKey simetricKey = sendPublicKeyAndRecieveAES();
                    String email = new String(decodeSimetricEncode(fromHex(dis.readUTF()),
                            simetricKey));
                    String contra = toHex(decodeSimetricEncode(fromHex(dis.readUTF()),
                            simetricKey));
                    String name = new String(decodeSimetricEncode(fromHex(dis.readUTF()),
                            simetricKey));

                    String format = new String(decodeSimetricEncode(fromHex(dis.readUTF()),
                            simetricKey));

                    long length = parseHexToLong(
                            toHex(
                                    decodeSimetricEncode(
                                            fromHex(
                                                    dis.readUTF()),
                                            simetricKey
                                    )
                            )
                    );

                    File f = createFileFromInput(dis, name, length, format);
                    f = decodeFileSymmetricAlgorithm(f, email+name, format, simetricKey, true);
                    System.out.println(email + " " + contra + " " + name);
                    Usuario user = new Usuario(email);
                    user.setContrasenya(contra);
                    user.setNombreUsuario(name);
                    user.setPartidasGanadas(0);
                    user.setImagenPerfil(f.getAbsolutePath());
                    
                    if(checkValidEmail(email) == false){
                        dos.writeInt(NO);
                        dos.writeInt(CREATE_USER_ERROR_INVALID_EMAIL);
                    }
                    else if (!UsuarioDao.getInstance().exists(user)) {
                        
                        if(UsuarioDao.getInstance().saveUsuario(user)){
                        dos.writeInt(OK);
                        System.out.println("OK");
                        }
                        else{
                            dos.writeInt(NO);
                            dos.writeInt(CREATE_USER_ERROR_INVALID_PARAMETERS);
                        }
                    } else {
                        System.out.println("NO");
                        dos.writeInt(NO);
                        dos.writeInt(CREATE_USER_ERROR_EXISTING_USER);
                    }
                }

                //String passwordUtf = dis.readUTF();
                //Cipher c = Cipher.getInstance("AES");
                //c.init(Cipher.DECRYPT_MODE, messageCodifier);
                //System.out.println(toHex(c.doFinal(fromHex(passwordUtf))));
            } catch (IOException | NoSuchAlgorithmException
                    | NoSuchPaddingException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    sk.close();
                } catch (IOException e) {

                }
            }
        }

        public final String PATTERN_STRING = "";
        public static boolean checkValidEmail(String email){
            
            Pattern p = Pattern.compile("^[a-zAz]+@[a-z]+[.][a-z]{2,3}$");
            Matcher matcher = p.matcher(email);
            
            return matcher.matches();
        }
        public String parseLongToHex(long number) {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(number);
            return toHex(buffer.array());
        }

        public long parseHexToLong(String hex) {
            byte[] array = fromHex(hex);
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.put(array);
            buffer.flip();
            return buffer.getLong();
        }

        public File createFileFromInput(DataInputStream dis, String name, long length, String format) throws IOException {

            File f = null;
            boolean control = true;
            int numFile = 1;
            do {
                StringBuilder strBuilder = new StringBuilder();
                strBuilder.append(ROOT_IMAGE)
                        .append("\\temp_")
                        .append(name)
                        .append("_")
                        .append(numFile)
                        .append(format);

                f = new File(strBuilder.toString());

                if (!f.exists()) {
                    synchronized (Cliente.class) {
                        if (!f.exists()) {
                            f.createNewFile();
                            control = false;
                        }
                    }
                }
                if (control) {
                    numFile++;
                }
            } while (control);

            long totalRead = 0;

            DataOutputStream fileDos = new DataOutputStream(new FileOutputStream(f));

            byte[] readData = new byte[BLOCK_SIZE];
            while (totalRead < length) {
                int readed;
                if (length - totalRead > BLOCK_SIZE) {
                    readed = dis.read(readData, 0, BLOCK_SIZE);
                } else {
                    readed = dis.read(readData, 0, (int) (length - totalRead));
                }
                totalRead += readed;
                fileDos.write(readData, 0, readed);
            }
            fileDos.flush();
            fileDos.close();

            return f;
        }

        /**
         * Envia la clave publica y recibe la clave simetrica
         *
         * @throws IOException
         * @throws NoSuchAlgorithmException
         * @throws NoSuchPaddingException
         */
        public File decodeFileSymmetricAlgorithm(File tempFile, String name, String format, SecretKey symmetricKey, boolean destroyTempFile) throws IOException {

            File result = null;

            DataInputStream fileDis = null;
            DataOutputStream fileDos = null;

            try {

                result = new File(ROOT_IMAGE + "\\" + name + format);
                if (result.exists()) {
                    result.delete();
                }

                result.createNewFile();

                fileDis = new DataInputStream(new FileInputStream(tempFile));
                fileDos = new DataOutputStream(new FileOutputStream(result));
                byte[] dataToDecipher = new byte[BLOCK_SIZE];

                long length = tempFile.length();
                long actual = 0;

                Cipher cipher = Cipher.getInstance(SYMMETRIC_CIPHER_FORMAT);
                cipher.init(Cipher.DECRYPT_MODE, symmetricKey);

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

            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchPaddingException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvalidKeyException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalBlockSizeException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BadPaddingException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    fileDos.close();
                } catch (NullPointerException | IOException e) {
                }
                try {
                    fileDis.close();
                } catch (NullPointerException | IOException e) {
                }
            }
            if (destroyTempFile) {
                System.out.println("File deleted: " + tempFile.delete());
            }
            return result;
        }

        public SecretKey sendPublicKeyAndRecieveAES() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
            byte[] publicKey = pairKeys.getPublic().getEncoded();
            //System.out.println(toHex(publicKey));
            dos.writeUTF(toHex(publicKey));

            String codeRecieved = dis.readUTF();
            byte[] claveCodificacion = fromHex(codeRecieved);

            SecretKey secretKey = null;
            try {
                Cipher c1 = Cipher.getInstance(ASYMMETRIC_CIPHER_FORMAT);
                c1.init(Cipher.DECRYPT_MODE, pairKeys.getPrivate());
                byte[] asdf = c1.doFinal(claveCodificacion);
                System.out.println(toHex(asdf));
                secretKey = new SecretKeySpec(asdf, SYMMETRIC_ALGORITHM);

            } catch (InvalidKeyException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalBlockSizeException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BadPaddingException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
            return secretKey;
        }

        /**
         * Este metodo descodifica un array de bytes que esta codificada con una
         * clave simetrica
         *
         * @param encoded array de bytes por descodificar
         * @param simetricKey clave simetrica para descodificar
         * @return devuelve los bytes descifrados o null si ha fallado
         */
        public byte[] decodeSimetricEncode(byte[] encoded, SecretKey simetricKey) {
            byte[] decoded = null;
            try {
                Cipher c = Cipher.getInstance(SYMMETRIC_CIPHER_FORMAT);
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

    /**
     * Este méto
     *
     * @param hex
     * @return
     */
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
