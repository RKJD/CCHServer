/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xokundevs.cchmavenserver.model;

import com.xokundevs.cchmavenserver.Main;
import com.xokundevs.cchmavenserver.bddconnectivity.dao.BarajaDao;
import com.xokundevs.cchmavenserver.bddconnectivity.dao.UsuarioDao;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Baraja;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Carta;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Usuario;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Properties;
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

/**
 *
 * @author Antonio
 */
public class Cliente extends Thread {

    static {
        String root_directory_image = null;

        try (InputStream is = new FileInputStream("Server.properties")) {
            Properties properties = new Properties();
            properties.load(is);

            root_directory_image = properties.getProperty("root_directory_image");

        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Server.properties not found");
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read Server.properties");
        }
        if (root_directory_image == null) {
            throw new NullPointerException("null or non existant properties");
        } else {
            ROOT_IMAGE = root_directory_image;
        }
    }

//ENCODING VARIABLES
    //ROOT IMAGE DIRECTORY
    private static final String ROOT_IMAGE;

    //ORDENES
    private static final int NO = -1;
    private static final int OK = 1;
    private static final int LOGIN = 102;
    private static final int CREATE_USER = 101;
    private static final int ERASE_USER = 103;
    private static final int GET_BASIC_INFO_BARAJA = 104;
    private static final int GET_CARTAS_BARAJA = 105;

    //ERRORES
    private static final int CREATE_USER_ERROR_EXISTING_USER = -1;
    private static final int CREATE_USER_ERROR_INVALID_EMAIL = -2;
    private static final int CREATE_USER_ERROR_INVALID_PARAMETERS = -3;
    private static final int USER_ERROR_INVALID_PASSWORD = -4;
    private static final int USER_ERROR_NON_EXISTANT_USER = -5;
    private static final int BARAJA_ERROR_NON_EXISTANT_BARAJA = -6;

    private Socket sk;
    private InternetControl iControl;

    public Cliente(Socket sk) throws IOException {
        this.sk = sk;
    }

    @Override
    public void run() {
        try {
            iControl = new InternetControl(sk);
            int code = iControl.getDis().readInt();
            System.out.println(code);
            switch (code) {
                case CREATE_USER:
                    RegistrarUsuario();
                    break;
                case LOGIN:
                    LoginUsuario();
                    break;
                case ERASE_USER:
                    EraseUsuario();
                    break;
                case GET_BASIC_INFO_BARAJA:
                    SendBasicBarajasInfo();
                    break;
                case GET_CARTAS_BARAJA:
                    SendCartasBaraja();
                    break;
                default:
                    System.out.println("El valor " + code + " no esta definido.");
                    break;
            }
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

    public static boolean checkValidEmail(String email) {

        Pattern p = Pattern.compile("^[a-zAz]+@[a-z]+[.][a-z]{2,3}$");
        Matcher matcher = p.matcher(email);

        return matcher.matches();
    }

    public void RegistrarUsuario() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, NoSuchPaddingException {
        SecretKey simetricKey = iControl.sendPublicKeyAndRecieveAES();
        String email = new String(EncoderHandler.encodeSimetricEncode(Parser.fromHex(iControl.getDis().readUTF()),
                simetricKey, false));
        String contra = Parser.toHex(EncoderHandler.encodeSimetricEncode(Parser.fromHex(iControl.getDis().readUTF()),
                simetricKey, false));
        String name = new String(EncoderHandler.encodeSimetricEncode(Parser.fromHex(iControl.getDis().readUTF()),
                simetricKey, false));

        String format = new String(EncoderHandler.encodeSimetricEncode(Parser.fromHex(iControl.getDis().readUTF()),
                simetricKey, false));

        long length = Parser.parseHexToLong(
                Parser.toHex(
                        EncoderHandler.encodeSimetricEncode(
                                Parser.fromHex(iControl.getDis().readUTF()),
                                simetricKey,
                                false
                        )
                )
        );

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
                        System.out.println(f.getAbsolutePath());
                        f.createNewFile();
                        control = false;
                    }
                }
            }
            if (control) {
                numFile++;
            }
        } while (control);

        iControl.createFileFromInput(f, length);

        File destiny;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ROOT_IMAGE)
                .append("\\")
                .append(email)
                .append("-")
                .append(name)
                .append(format);
        destiny = new File(stringBuilder.toString());
        if (!destiny.exists()) {
            destiny.createNewFile();
        } else {
            destiny = null;
        }

        try {
            EncoderHandler.encodeFileSymmetricAlgorithm(f, destiny, simetricKey, true, false);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        //System.out.println(email + " " + contra + " " + name);
        Usuario user = new Usuario(email);
        user.setContrasenya(contra);
        user.setNombreUsuario(name);
        user.setPartidasGanadas(0);
        if (destiny != null) {
            user.setImagenPerfil(destiny.getAbsolutePath());
        }

        if (checkValidEmail(email) == false) {
            iControl.getDos().writeInt(NO);
            iControl.getDos().writeInt(CREATE_USER_ERROR_INVALID_EMAIL);
        } else if (!UsuarioDao.getInstance().exists(user)) {

            if (UsuarioDao.getInstance().saveUsuario(user)) {
                iControl.getDos().writeInt(OK);
                System.out.println("OK");
            } else {
                iControl.getDos().writeInt(NO);
                iControl.getDos().writeInt(CREATE_USER_ERROR_INVALID_PARAMETERS);
            }
        } else {
            System.out.println("NO");
            iControl.getDos().writeInt(NO);
            iControl.getDos().writeInt(CREATE_USER_ERROR_EXISTING_USER);
        }
    }

    public void LoginUsuario() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {

        SecretKey scrKey = iControl.sendPublicKeyAndRecieveAES();

        String email = new String(
                EncoderHandler.encodeSimetricEncode(
                        Parser.fromHex(iControl.getDis().readUTF()), scrKey, false
                )
        );

        String password = Parser.toHex(
                EncoderHandler.encodeSimetricEncode(
                        Parser.fromHex(iControl.getDis().readUTF()), scrKey, false
                )
        );

        System.out.println(email + " - " + password);
        boolean continuar = false;
        Usuario user = new Usuario(email);
        if (UsuarioDao.getInstance().exists(user)) {
            user = UsuarioDao.getInstance().getUsuario(email);
            if (user.getContrasenya().equals(password)) {
                continuar = true;
            }
        }

        iControl.getDos().writeInt((continuar) ? OK : NO);

        if (continuar) {
            byte[] nombreBytes = user.getNombreUsuario().getBytes(StandardCharsets.UTF_8);
            String nombre = Parser.toHex(EncoderHandler.encodeSimetricEncode(nombreBytes, scrKey, true));

            iControl.getDos().writeUTF(nombre);

            File f = new File(user.getImagenPerfil());

            if (f.exists() && !f.isDirectory()) {
                System.out.println("SI IMAGEN");
                File temp = new File(user.getImagenPerfil() + ".temp");
                if (!temp.exists()) {
                    temp.createNewFile();
                }
                try {
                    EncoderHandler.encodeFileSymmetricAlgorithm(f, temp, scrKey, false, true);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

                byte[] lengthHex = Parser.fromHex(Parser.parseLongToHex(temp.length()));
                String encodedLength = Parser.toHex(EncoderHandler.encodeSimetricEncode(lengthHex, scrKey, true));
                System.out.println("Long encoded: " + encodedLength + "|| real length: " + temp.length());
                iControl.SendFile(temp, encodedLength);
                temp.delete();
            } else {
                System.out.println("NO IMAGEN");
                iControl.getDos().writeUTF(
                        Parser.toHex(
                                EncoderHandler.encodeSimetricEncode(
                                        Parser.fromHex(Parser.parseLongToHex(0)),
                                        scrKey,
                                        continuar
                                )
                        )
                );
            }

            String wins = Parser.toHex(
                    EncoderHandler.encodeSimetricEncode(
                            Parser.fromHex(Parser.parseIntToHex(user.getPartidasGanadas())), scrKey, continuar
                    )
            );

            iControl.getDos().writeUTF(wins);
        }
    }

    public void EraseUsuario() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        SecretKey secretKey = iControl.sendPublicKeyAndRecieveAES();

        String password = Parser.toHex(
                EncoderHandler.encodeSimetricEncode(Parser.fromHex(iControl.getDis().readUTF()), secretKey, false)
        );

        String email = new String(EncoderHandler.encodeSimetricEncode(Parser.fromHex(iControl.getDis().readUTF()), secretKey, false));

        Usuario user = new Usuario();
        user.setEmailUsuario(email);
        UsuarioDao uDao = UsuarioDao.getInstance();
        if (uDao.exists(user)) {
            user = uDao.getUsuario(email);
            if (password.equals(user.getContrasenya())) {
                File f = new File(user.getImagenPerfil());
                uDao.deleteUsuario(user);
                if (f.exists()) {
                    f.delete();
                }
                iControl.getDos().writeInt(OK);
            } else {
                iControl.getDos().writeInt(NO);
                iControl.getDos().writeInt(USER_ERROR_INVALID_PASSWORD);
            }
        } else {
            iControl.getDos().writeInt(NO);
            iControl.getDos().writeInt(USER_ERROR_NON_EXISTANT_USER);
        }
    }

    public void SendBasicBarajasInfo() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        SecretKey secretKey = iControl.sendPublicKeyAndRecieveAES();

        String email = new String(EncoderHandler.encodeSimetricEncode(Parser.fromHex(iControl.getDis().readUTF()),
                secretKey, false));

        String password = Parser.toHex(
                EncoderHandler.encodeSimetricEncode(Parser.fromHex(iControl.getDis().readUTF()),
                        secretKey,
                        false
                )
        );

        DataOutputStream dos = iControl.getDos();
        Usuario user = new Usuario(email);
        System.out.println("Llego -- GET_MAZOS");
        if (UsuarioDao.getInstance().exists(user) && (user = UsuarioDao.getInstance().getUsuario(email)).getContrasenya().equals(password)) {

            System.out.println("OK -- GET_MAZOS");
            dos.writeInt(OK);

            BarajaDao bDao = BarajaDao.getInstance();

            List<Baraja> list = bDao.getBarajas(email);

            list.addAll(bDao.getBarajas("default"));

            String listSize = Parser.toHex(
                    EncoderHandler.encodeSimetricEncode(
                            Parser.fromHex(Parser.parseIntToHex(list.size())),
                            secretKey,
                            true
                    )
            );

            dos.writeUTF(listSize);

            System.out.println(list.size());

            for (Baraja baraja : list) {

                String emailCoded = Parser.toHex(
                        EncoderHandler.encodeSimetricEncode(
                                baraja.getId().getEmailUsuario().getBytes(StandardCharsets.UTF_8),
                                secretKey,
                                true
                        )
                );

                String barajaNameEncoded = Parser.toHex(
                        EncoderHandler.encodeSimetricEncode(
                                baraja.getId().getNombreBaraja().getBytes(StandardCharsets.UTF_8),
                                secretKey,
                                true
                        )
                );

                String barajaUserNameEncoded = Parser.toHex(
                        EncoderHandler.encodeSimetricEncode(
                                baraja.getId().getEmailUsuario().getBytes(StandardCharsets.UTF_8),
                                secretKey,
                                true
                        )
                );

                String cantidadCartas = Parser.toHex(
                        EncoderHandler.encodeSimetricEncode(
                                Parser.fromHex(
                                        Parser.parseIntToHex(baraja.getCartas().size())
                                ),
                                secretKey,
                                true
                        )
                );

                String barajaIdioma = Parser.toHex(
                        EncoderHandler.encodeSimetricEncode(
                                baraja.getIdioma().getBytes(StandardCharsets.UTF_8),
                                secretKey,
                                true
                        )
                );

                dos.writeUTF(emailCoded);

                dos.writeUTF(barajaNameEncoded);

                dos.writeUTF(barajaUserNameEncoded);

                dos.writeUTF(cantidadCartas);

                dos.writeUTF(barajaIdioma);
            }

        } else {

            System.out.println("NO -- GET_MAZOS");
            dos.writeInt(NO);
            if (!UsuarioDao.getInstance().exists(user)) {
                System.out.println("User dont exists -- GET_MAZOS");
                dos.writeInt(USER_ERROR_NON_EXISTANT_USER);
            } else {
                System.out.println("Bad password -- GET_MAZOS");
                System.out.println("Recibida: " + password + ", Guardada: " + user.getContrasenya());
                dos.writeInt(USER_ERROR_INVALID_PASSWORD);
            }
        }

    }

    public void SendCartasBaraja() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        SecretKey secretKey = iControl.sendPublicKeyAndRecieveAES();

        DataInputStream dis = iControl.getDis();
        DataOutputStream dos = iControl.getDos();

        System.out.println("Usuario");
        String email = new String(EncoderHandler.encodeSimetricEncode(Parser.fromHex(dis.readUTF()),
                secretKey, false));

        System.out.println("Mazo");
        String nombreMazo = new String(
                EncoderHandler.encodeSimetricEncode(
                        Parser.fromHex(dis.readUTF()),
                        secretKey,
                        false
                )
        );
        BarajaDao bDao = BarajaDao.getInstance();
        Baraja b = null;
        Usuario user = null;
        if ((user = UsuarioDao.getInstance().getUsuario(email)) != null) {
            if ((b = bDao.getBarajaWithCards(user.getEmailUsuario(), nombreMazo)) != null) {
                dos.writeInt(OK);

                dos.writeUTF(
                        Parser.toHex(
                                EncoderHandler.encodeSimetricEncode(
                                        Parser.fromHex(
                                                Parser.parseIntToHex(b.getCartas().size())
                                        ),
                                        secretKey,
                                        true
                                )
                        )
                );

                for (Carta c : b.getCartas()) {
                    boolean isNegra = c.getCartanegra() != null && c.getCartablanca() == null;
                    System.out.print("Es negra: " + isNegra + ", texto: " + c.getTexto()
                            + ", codi: " + c.getId().getIdCarta());
                    if(isNegra)
                        System.out.print(", Espacios: " + c.getCartanegra().getNumeroEspacios());
                    System.out.println();
                    dos.writeUTF(
                            Parser.toHex(
                                    EncoderHandler.encodeSimetricEncode(
                                            Parser.fromHex(
                                                    Parser.parseIntToHex((isNegra) ? 1 : 0)
                                            ),
                                            secretKey,
                                            true
                                    )
                            )
                    );

                    dos.writeUTF(
                            Parser.toHex(
                                    EncoderHandler.encodeSimetricEncode(
                                            c.getTexto().getBytes(StandardCharsets.UTF_8),
                                            secretKey,
                                            true
                                    )
                            )
                    );

                    dos.writeUTF(
                            Parser.toHex(
                                    EncoderHandler.encodeSimetricEncode(
                                            Parser.fromHex(
                                                    Parser.parseIntToHex(c.getId().getIdCarta())
                                            ),
                                            secretKey,
                                            true
                                    )
                            )
                    );

                    if (isNegra) {
                        dos.writeUTF(
                                Parser.toHex(
                                        EncoderHandler.encodeSimetricEncode(
                                                Parser.fromHex(
                                                        Parser.parseIntToHex(c.getCartanegra().getNumeroEspacios())
                                                ),
                                                secretKey,
                                                true
                                        )
                                )
                        );
                    }
                }

            } else {
                dos.writeInt(NO);
                dos.writeInt(BARAJA_ERROR_NON_EXISTANT_BARAJA);
            }
        } else {
            dos.writeInt(NO);
            dos.writeInt(USER_ERROR_NON_EXISTANT_USER);
        }
    }

}
