/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xokundevs.cchmavenserver.model;

import com.xokundevs.cchmavenserver.Main;
import com.xokundevs.cchmavenserver.bddconnectivity.dao.BarajaDao;
import com.xokundevs.cchmavenserver.bddconnectivity.dao.CartaDao;
import com.xokundevs.cchmavenserver.bddconnectivity.dao.CartablancaDao;
import com.xokundevs.cchmavenserver.bddconnectivity.dao.CartanegraDao;
import com.xokundevs.cchmavenserver.bddconnectivity.dao.UsuarioDao;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Baraja;
import com.xokundevs.cchmavenserver.bddconnectivity.model.BarajaId;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Carta;
import com.xokundevs.cchmavenserver.bddconnectivity.model.CartaId;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Cartablanca;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Cartanegra;
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
import java.util.ArrayList;
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

        String email = iControl.recibirString(simetricKey);

        Usuario user = new Usuario(email);
        if (checkValidEmail(email) == false) {
            iControl.enviarInt(NO, simetricKey);
            iControl.enviarInt(CREATE_USER_ERROR_INVALID_EMAIL, simetricKey);
        } else if (!UsuarioDao.getInstance().exists(user)) {
            iControl.enviarInt(OK, simetricKey);
            String contra = iControl.recibirHex(simetricKey);
            String name = iControl.recibirString(simetricKey);

            String format = iControl.recibirString(simetricKey);

            long length = iControl.recibirLong(simetricKey);

            System.out.println(email + " " + contra + " " + name + " " + format);

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
            user.setContrasenya(contra);
            user.setNombreUsuario(name);
            user.setPartidasGanadas(0);
            if (destiny != null) {
                user.setImagenPerfil(destiny.getAbsolutePath());
            }

            if (UsuarioDao.getInstance().saveUsuario(user)) {
                System.out.println("OK");
            } else {
                iControl.enviarInt(NO, simetricKey);
                iControl.enviarInt(CREATE_USER_ERROR_INVALID_PARAMETERS, simetricKey);
            }
        } else {
            System.out.println("NO");
            iControl.enviarInt(NO, simetricKey);
            iControl.enviarInt(CREATE_USER_ERROR_EXISTING_USER, simetricKey);
        }
    }

    public void LoginUsuario() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {

        SecretKey scrKey = iControl.sendPublicKeyAndRecieveAES();

        String email = iControl.recibirString(scrKey);

        String password = iControl.recibirHex(scrKey);

        System.out.println(email + " - " + password);
        boolean continuar = false;
        Usuario user = new Usuario(email);
        if (UsuarioDao.getInstance().exists(user)) {
            user = UsuarioDao.getInstance().getUsuario(email);
            if (user.getContrasenya().equals(password)) {
                continuar = true;
            }
        }

        iControl.enviarInt((continuar) ? OK : NO, scrKey);

        if (continuar) {
            iControl.enviarString(user.getNombreUsuario(), scrKey);

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
                iControl.SendFile(temp, scrKey);
                temp.delete();
            } else {
                System.out.println("NO IMAGEN");
                iControl.enviarLong(0, scrKey);
            }

            iControl.enviarInt(user.getPartidasGanadas(), scrKey);
        }
    }

    public void EraseUsuario() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        SecretKey secretKey = iControl.sendPublicKeyAndRecieveAES();

        String password = iControl.recibirHex(secretKey);

        String email = iControl.recibirString(secretKey);

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
                iControl.enviarInt(OK, secretKey);
            } else {
                iControl.enviarInt(NO, secretKey);
                iControl.enviarInt(USER_ERROR_INVALID_PASSWORD, secretKey);
            }
        } else {
            iControl.enviarInt(NO, secretKey);
            iControl.enviarInt(USER_ERROR_NON_EXISTANT_USER, secretKey);
        }
    }

    public void SendBasicBarajasInfo() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        SecretKey secretKey = iControl.sendPublicKeyAndRecieveAES();

        String email = iControl.recibirString(secretKey);

        String password = iControl.recibirHex(secretKey);

        DataOutputStream dos = iControl.getDos();

        Usuario user = new Usuario(email);
        System.out.println("Llego -- GET_MAZOS");
        if (UsuarioDao.getInstance().exists(user) && (user = UsuarioDao.getInstance().getUsuario(email)).getContrasenya().equals(password)) {

            System.out.println("OK -- GET_MAZOS");
            iControl.enviarInt(OK, secretKey);

            BarajaDao bDao = BarajaDao.getInstance();

            List<Baraja> list = bDao.getBarajas(email);

            list.addAll(bDao.getBarajas("default"));

            iControl.enviarInt(list.size(), secretKey);

            System.out.println(list.size());

            for (Baraja baraja : list) {

                iControl.enviarString(baraja.getId().getEmailUsuario(), secretKey);

                iControl.enviarString(baraja.getId().getNombreBaraja(), secretKey);

                iControl.enviarString(baraja.getUsuario().getNombreUsuario(), secretKey);

                iControl.enviarInt(baraja.getCartas().size(), secretKey);

                iControl.enviarString(baraja.getIdioma(), secretKey);

            }

        } else {

            System.out.println("NO -- GET_MAZOS");
            iControl.enviarInt(NO, secretKey);
            if (!UsuarioDao.getInstance().exists(user)) {
                System.out.println("User dont exists -- GET_MAZOS");
                iControl.enviarInt(USER_ERROR_NON_EXISTANT_USER, secretKey);
            } else {
                System.out.println("Bad password -- GET_MAZOS");
                iControl.enviarInt(USER_ERROR_INVALID_PASSWORD, secretKey);
            }
        }

    }

    public void SendCartasBaraja() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        SecretKey secretKey = iControl.sendPublicKeyAndRecieveAES();

        System.out.println("Usuario");
        String email = iControl.recibirString(secretKey);

        System.out.println("Mazo");
        String nombreMazo = iControl.recibirString(secretKey);

        BarajaDao bDao = BarajaDao.getInstance();
        Baraja b;
        Usuario user;
        if ((user = UsuarioDao.getInstance().getUsuario(email)) != null) {
            if ((b = bDao.getBarajaWithCards(user.getEmailUsuario(), nombreMazo)) != null) {
                iControl.enviarInt(OK, secretKey);

                iControl.enviarInt(b.getCartas().size(), secretKey);

                for (Carta c : b.getCartas()) {
                    boolean isNegra = c.getCartanegra() != null && c.getCartablanca() == null;

                    iControl.enviarInt((isNegra) ? 1 : 0, secretKey);

                    iControl.enviarString(c.getTexto(), secretKey);

                    iControl.enviarInt(c.getId().getIdCarta(), secretKey);

                    if (isNegra) {
                        iControl.enviarInt(c.getCartanegra().getNumeroEspacios(), secretKey);
                    }
                }

            } else {
                iControl.enviarInt(NO, secretKey);
                iControl.enviarInt(BARAJA_ERROR_NON_EXISTANT_BARAJA, secretKey);
            }
        } else {
            iControl.enviarInt(NO, secretKey);
            iControl.enviarInt(USER_ERROR_NON_EXISTANT_USER, secretKey);
        }
    }

    public void RecibeBarajaNueva() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        SecretKey secretKey = iControl.sendPublicKeyAndRecieveAES();

        String email = iControl.recibirString(secretKey);
        String password = iControl.recibirHex(secretKey);

        Usuario user = null;
        if ((user = UsuarioDao.getInstance().getUsuario(email)).getContrasenya().equals(password)) {
            iControl.enviarInt(OK, secretKey);

            String barajaname = iControl.recibirString(secretKey);

            String idioma = iControl.recibirString(secretKey);
            Baraja b;
            if ((b = BarajaDao.getInstance().getBaraja(email, barajaname)) != null) {
                BarajaId bId = b.getId();

                int numeroCartas = iControl.recibirInt(secretKey);

                for (int i = 0; i < numeroCartas; i++) {
                    int isNegra = iControl.recibirInt(secretKey);

                    int id = iControl.recibirInt(secretKey);

                    String texto = iControl.recibirString(secretKey);

                    CartaId cId = new CartaId(id, email, barajaname);

                    Carta c = new Carta(cId, b);
                    c.setTexto(texto);

                    if (CartaDao.getInstance().getCarta(cId.getEmailUsuario(), cId.getNombreBaraja(), cId.getIdCarta()) != null) {
                        CartaDao.getInstance().updateCarta(c);
                    } else {
                        CartaDao.getInstance().saveCarta(c);
                    }
                    CartaDao.getInstance().saveCarta(c);

                    if (isNegra == 1) {
                        int numEspacios = iControl.recibirInt(secretKey);

                        Cartanegra cNegra = new Cartanegra(c, numEspacios);
                        if (CartanegraDao.getInstance().getCarta(cId.getEmailUsuario(), cId.getNombreBaraja(), cId.getIdCarta()) != null) {
                            CartanegraDao.getInstance().updateCarta(cNegra);
                        } else {
                            Cartablanca cBlanca = CartablancaDao.getInstance().getCarta(cId.getEmailUsuario(), cId.getNombreBaraja(), cId.getIdCarta());
                            if (cBlanca != null) {
                                CartablancaDao.getInstance().deleteCarta(cBlanca);
                            }
                            CartanegraDao.getInstance().saveCarta(cNegra);
                        }
                    } else {
                        Cartablanca cBlanca = new Cartablanca(c);
                        if (CartablancaDao.getInstance().getCarta(cId.getEmailUsuario(), cId.getNombreBaraja(), cId.getIdCarta()) != null) {
                            CartablancaDao.getInstance().updateCarta(cBlanca);
                        } else {
                            Cartanegra cNegra = CartanegraDao.getInstance().getCarta(cId.getEmailUsuario(), cId.getNombreBaraja(), cId.getIdCarta());
                            if (cNegra != null) {
                                CartablancaDao.getInstance().deleteCarta(cBlanca);
                            }
                            CartanegraDao.getInstance().saveCarta(cNegra);
                        }
                    }
                }
            } else {
                BarajaId bId = new BarajaId(email, barajaname);
                b = new Baraja(bId, user);
                b.setIdioma(idioma);
            }
        }
    }

    public void CreaPartida() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        SecretKey secretKey = iControl.sendPublicKeyAndRecieveAES();

        String email, contra;

        email = iControl.recibirString(secretKey);
        contra = iControl.recibirHex(secretKey);

        Usuario user = null;
        if ((user = UsuarioDao.getInstance().getUsuario(email)) != null && user.getContrasenya().equals(contra)) {
            iControl.enviarInt(OK, secretKey);

            String namePartida = iControl.recibirString(secretKey);
            String contrasenaPartida = iControl.recibirString(secretKey);
            
            ArrayList<Baraja> listaBarajas = new ArrayList<>();
            
            int cantidadMazos = iControl.recibirInt(secretKey);
            
            for(int i = 0; i < cantidadMazos; i++){
                String emailMazo, nombreMazo;
                emailMazo = iControl.recibirString(secretKey);
                nombreMazo = iControl.recibirString(secretKey);
                
                Baraja b = BarajaDao.getInstance().getBaraja(emailMazo, nombreMazo);
                if(b != null){
                    listaBarajas.add(b);
                    iControl.enviarInt(OK, secretKey);
                }
                else{
                    iControl.enviarInt(NO, secretKey);
                    iControl.enviarInt(BARAJA_ERROR_NON_EXISTANT_BARAJA, secretKey);
                }
            }

            Partida p = new Partida(namePartida, contrasenaPartida, 3, iControl, user, secretKey, listaBarajas);
            Partida.addPartida(p);
            p.run();
        } else {
            iControl.enviarInt(NO, secretKey);
            if (user != null) {
                iControl.enviarInt(USER_ERROR_INVALID_PASSWORD, secretKey);
            }
            else{
                iControl.enviarInt(USER_ERROR_NON_EXISTANT_USER, secretKey);
            }
        }
    }
}
