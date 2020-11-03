/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xokundevs.cchmavenserver.model;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

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
    public static final int NO = -1;
    public static final int OK = 1;
    private static final int LOGIN = 102;
    private static final int CREATE_USER = 101;
    private static final int ERASE_USER = 103;
    private static final int GET_BASIC_INFO_BARAJA = 104;
    private static final int GET_CARTAS_BARAJA = 105;
    private static final int GUARDA_BARAJA = 106;
    private static final int BORRA_PROPIA_BARAJA = 107;
    private static final int GET_PARTIDAS = 108;
    private static final int CREAR_PARTIDA = 109;
    private static final int ENTRAR_PARTIDA = 110;

    //ERRORES
    private static final int CREATE_USER_ERROR_EXISTING_USER = -1;
    private static final int CREATE_USER_ERROR_INVALID_EMAIL = -2;
    private static final int CREATE_USER_ERROR_INVALID_PARAMETERS = -3;
    private static final int USER_ERROR_INVALID_PASSWORD = -4;
    private static final int USER_ERROR_NON_EXISTANT_USER = -5;
    private static final int BARAJA_ERROR_NON_EXISTANT_BARAJA = -6;
    private static final int CREATE_USER_ERROR_LONG_EMAIL = -7;
    private static final int CREATE_USER_ERROR_LONG_USERNAME = -8;
    private static final int CREATE_USER_ERROR_INVALID_USERNAME = -9;
    private static final int PARTIDA_ERROR_NON_EXISTANT_PARTIDA = -10;
    private static final int PARTIDA_ERROR_EXISTING_PARTIDA = -11;
    private static final int PARTIDA_ERROR_NO_ENTRAR_DENIED = -12;

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
                case GUARDA_BARAJA:
                    RecibeBarajaNueva();
                    break;
                case BORRA_PROPIA_BARAJA:
                    BorraBarajaPropia();
                    break;
                case GET_PARTIDAS:
                    GetPartidas();
                    break;
                case CREAR_PARTIDA:
                    CreaPartida();
                    break;
                case ENTRAR_PARTIDA:
                    UnirsePartida();
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

    public static boolean checkValidEmail(String email) {

        Pattern p = Pattern.compile("^[a-zA-Z.0-9]+@[a-z]+[.][a-z]{2,3}$");
        Matcher matcher = p.matcher(email);

        return matcher.matches();
    }

    public static boolean checkValidUsername(String username) {
        Pattern pattern = Pattern.compile("[\\w]+");
        Matcher matcher = pattern.matcher(username);
        return matcher.matches();
    }

    //USUARIOS
    public void RegistrarUsuario() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, NoSuchPaddingException {
        SecretKey simetricKey = iControl.sendPublicKeyAndRecieveAES();

        String email = iControl.recibirString(simetricKey);
        String contra = iControl.recibirHex(simetricKey);
        String name = iControl.recibirString(simetricKey);

        Usuario user = new Usuario(email);
        if (checkValidEmail(email) == false || email.length() > 50) {
            iControl.enviarInt(NO, simetricKey);
            if (email.length() > 50) {
                iControl.enviarInt(CREATE_USER_ERROR_LONG_EMAIL, simetricKey);
            } else {
                iControl.enviarInt(CREATE_USER_ERROR_INVALID_EMAIL, simetricKey);
            }
        } else if (!checkValidUsername(name) || name.length() > 15) {
            iControl.enviarInt(NO, simetricKey);
            if (name.length() > 15) {
                iControl.enviarInt(CREATE_USER_ERROR_LONG_USERNAME, simetricKey);
            } else {
                iControl.enviarInt(CREATE_USER_ERROR_INVALID_USERNAME, simetricKey);
            }
        } else if (!UsuarioDao.getInstance().exists(user)) {
            iControl.enviarInt(OK, simetricKey);

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
        Usuario user;
        if ((user = UsuarioDao.getInstance().getUsuario(email)) != null && user.getContrasenya().equals(password)) {
            iControl.enviarInt(OK, scrKey);

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
        } else {
            iControl.enviarInt(NO, scrKey);
            if (user != null) {
                iControl.enviarInt(USER_ERROR_INVALID_PASSWORD, scrKey);
            } else {
                iControl.enviarInt(USER_ERROR_NON_EXISTANT_USER, scrKey);
            }
        }
    }

    public void EraseUsuario() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        SecretKey secretKey = iControl.sendPublicKeyAndRecieveAES();

        String password = iControl.recibirHex(secretKey);

        String email = iControl.recibirString(secretKey);

        Usuario user;
        UsuarioDao uDao = UsuarioDao.getInstance();
        if ((user = uDao.getUsuario(email)) != null) {
            if (password.equals(user.getContrasenya())) {

                List<Cartanegra> listaCartaNegra = CartanegraDao.getInstance().getCartaFromOnlyUser(email);
                List<Cartablanca> listaCartaBlanca = CartablancaDao.getInstance().getCartaFromOnlyUser(email);
                List<Carta> listaCartas = CartaDao.getInstance().getCartaFromOnlyUser(email);
                List<Baraja> listaBarajas = BarajaDao.getInstance().getBarajas(email);

                for (Cartablanca c : listaCartaBlanca) {
                    CartablancaDao.getInstance().deleteCarta(c);
                }

                for (Cartanegra c : listaCartaNegra) {
                    CartanegraDao.getInstance().deleteCarta(c);
                }

                for (Carta c : listaCartas) {
                    CartaDao.getInstance().deleteCarta(c);
                }

                for (Baraja b : listaBarajas) {
                    BarajaDao.getInstance().deleteBaraja(b);
                }

                uDao.deleteUsuario(user);

                File f = new File(user.getImagenPerfil());
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

    //BARAJAS
    public void SendBasicBarajasInfo() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        SecretKey secretKey = iControl.sendPublicKeyAndRecieveAES();

        String email = iControl.recibirString(secretKey);

        String password = iControl.recibirHex(secretKey);

        DataOutputStream dos = iControl.getDos();

        Usuario user;
        System.out.println("Llego -- GET_MAZOS");
        if ((user = UsuarioDao.getInstance().getUsuario(email)) != null && user.getContrasenya().equals(password)) {

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

        Usuario user;
        if ((user = UsuarioDao.getInstance().getUsuario(email)) != null && user.getContrasenya().equals(password)) {
            iControl.enviarInt(OK, secretKey);

            String barajaname = iControl.recibirString(secretKey);

            String idioma = iControl.recibirString(secretKey);

            int numeroCartas = iControl.recibirInt(secretKey);

            Baraja b;

            int cuentaId = 1;
            if ((b = BarajaDao.getInstance().getBaraja(email, barajaname)) != null) {
                BarajaId bId = b.getId();

                List<Cartanegra> listaCartaNegra = CartanegraDao.getInstance().getCartas(user.getEmailUsuario(), barajaname);
                List<Cartablanca> listaCartaBlanca = CartablancaDao.getInstance().getCartas(user.getEmailUsuario(), barajaname);
                List<Carta> listaCartas = CartaDao.getInstance().getCartas(user.getEmailUsuario(), barajaname);

                listaCartaBlanca.forEach((c) -> {
                    CartablancaDao.getInstance().deleteCarta(c);
                });

                listaCartaNegra.forEach((c) -> {
                    CartanegraDao.getInstance().deleteCarta(c);
                });

                listaCartas.forEach((c) -> {
                    CartaDao.getInstance().deleteCarta(c);
                });

                for (int i = 0; i < numeroCartas; i++) {
                    int isNegra = iControl.recibirInt(secretKey);

                    int id = cuentaId++;

                    String texto = iControl.recibirString(secretKey);

                    CartaId cId = new CartaId(id, email, barajaname);

                    Carta c = new Carta(cId, b);
                    c.setTexto(texto);
                    CartaDao.getInstance().saveCarta(c);

                    if (isNegra == 1) {
                        int numEspacios = iControl.recibirInt(secretKey);

                        Cartanegra cNegra = new Cartanegra(c, numEspacios);
                        cNegra.setId(cId);
                        CartanegraDao.getInstance().saveCarta(cNegra);
                    } else {
                        Cartablanca cBlanca = new Cartablanca(c);
                        cBlanca.setId(cId);
                        CartablancaDao.getInstance().saveCarta(cBlanca);
                    }
                }
            } else {
                BarajaId bId = new BarajaId(email, barajaname);
                b = new Baraja(bId, user);
                b.setIdioma(idioma);

                if (BarajaDao.getInstance().saveBaraja(b)) {

                    for (int i = 0; i < numeroCartas; i++) {
                        int isNegra = iControl.recibirInt(secretKey);

                        int id = cuentaId++;

                        String texto = iControl.recibirString(secretKey);

                        CartaId cId = new CartaId(id, email, barajaname);

                        Carta c = new Carta(cId, b);
                        c.setTexto(texto);

                        System.out.println("Es negra? " + isNegra + ", id+ ");
                        CartaDao.getInstance().saveCarta(c);

                        if (isNegra == 1) {
                            int numEspacios = iControl.recibirInt(secretKey);

                            Cartanegra cNegra = new Cartanegra(c, numEspacios);
                            cNegra.setId(cId);
                            CartanegraDao.getInstance().saveCarta(cNegra);
                        } else {
                            Cartablanca cBlanca = new Cartablanca(c);
                            cBlanca.setId(cId);
                            CartablancaDao.getInstance().saveCarta(cBlanca);
                        }
                    }
                }
            }
        } else {
            iControl.enviarInt(NO, secretKey);
            if (user != null) {
                iControl.enviarInt(USER_ERROR_INVALID_PASSWORD, secretKey);
            } else {
                iControl.enviarInt(USER_ERROR_NON_EXISTANT_USER, secretKey);
            }
        }
    }

    public void BorraBarajaPropia() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        SecretKey secretKey = iControl.sendPublicKeyAndRecieveAES();

        String usuarioEmail = iControl.recibirString(secretKey);
        String usuarioPassword = iControl.recibirHex(secretKey);
        String nombreBaraja = iControl.recibirString(secretKey);
        Usuario user;
        if ((user = UsuarioDao.getInstance().getUsuario(usuarioEmail)) != null && user.getContrasenya().equals(usuarioPassword)) {
            Baraja baraja;
            if ((baraja = BarajaDao.getInstance().getBaraja(usuarioEmail, nombreBaraja)) != null) {
                CartablancaDao cBlancaDao = CartablancaDao.getInstance();
                CartanegraDao cNegraDao = CartanegraDao.getInstance();
                CartaDao cDao = CartaDao.getInstance();

                List<Cartablanca> blancas = cBlancaDao.getCartas(usuarioEmail, nombreBaraja);
                List<Cartanegra> negras = cNegraDao.getCartas(usuarioEmail, nombreBaraja);
                List<Carta> carta = cDao.getCartas(usuarioEmail, nombreBaraja);

                if (blancas != null) {
                    blancas.forEach((c) -> {
                        cBlancaDao.deleteCarta(c);
                    });
                }

                if (negras != null) {
                    negras.forEach((c) -> {
                        cNegraDao.deleteCarta(c);
                    });
                }

                if (carta != null) {
                    carta.forEach((c) -> {
                        cDao.deleteCarta(c);
                    });
                }
                BarajaDao.getInstance().deleteBaraja(baraja);

                iControl.enviarInt(OK, secretKey);
            } else {
                iControl.enviarInt(NO, secretKey);
                iControl.enviarInt(BARAJA_ERROR_NON_EXISTANT_BARAJA, secretKey);
            }
        } else {
            iControl.enviarInt(NO, secretKey);
            if (user != null) {
                iControl.enviarInt(USER_ERROR_INVALID_PASSWORD, secretKey);
            } else {
                iControl.enviarInt(USER_ERROR_NON_EXISTANT_USER, secretKey);
            }
        }
    }

    //PARTIDAS
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
            int maxPlayers = iControl.recibirInt(secretKey);
            int cantidadMazos = iControl.recibirInt(secretKey);

            for (int i = 0; i < cantidadMazos; i++) {
                String emailMazo, nombreMazo;
                emailMazo = iControl.recibirString(secretKey);
                nombreMazo = iControl.recibirString(secretKey);

                if (emailMazo.equals(user.getEmailUsuario()) || emailMazo.equals("default")) {
                    Baraja b = BarajaDao.getInstance().getBaraja(emailMazo, nombreMazo);
                    if (b != null) {
                        listaBarajas.add(b);
                    }
                }
            }
            if (listaBarajas.isEmpty()) {
                iControl.enviarInt(NO, secretKey);
                iControl.enviarInt(BARAJA_ERROR_NON_EXISTANT_BARAJA, secretKey);
            } else {
                ArrayList<Cartablanca> listaCartasBlancas = new ArrayList<>();
                ArrayList<Cartanegra> listaCartasNegras = new ArrayList<>();
                for (Baraja b : listaBarajas) {
                    String e_temp, b_temp;
                    e_temp = b.getId().getEmailUsuario();
                    b_temp = b.getId().getNombreBaraja();
                    listaCartasBlancas.addAll(CartablancaDao.getInstance().getCartas(e_temp, b_temp));
                    listaCartasNegras.addAll(CartanegraDao.getInstance().getCartas(e_temp, b_temp));
                }
                Partida p = new Partida(namePartida, contrasenaPartida,
                        maxPlayers, listaCartasBlancas,
                        listaCartasNegras, user.getEmailUsuario());
                if (Partida.addPartida(p)) {
                    if (p.conectarAPartida(contrasenaPartida, iControl, user, secretKey) != null) {
                        iControl.enviarInt(OK, secretKey);
                        try{
                            p.join();
                        }catch(InterruptedException e){}
                    }
                    else{
                        p.BorraPartida();
                        iControl.enviarInt(NO, secretKey);
                        iControl.enviarInt(PARTIDA_ERROR_NO_ENTRAR_DENIED, secretKey);
                    }
                } else {
                    iControl.enviarInt(NO, secretKey);
                    iControl.enviarInt(PARTIDA_ERROR_EXISTING_PARTIDA, secretKey);
                }
            }
        } else {
            iControl.enviarInt(NO, secretKey);
            if (user != null) {
                iControl.enviarInt(USER_ERROR_INVALID_PASSWORD, secretKey);
            } else {
                iControl.enviarInt(USER_ERROR_NON_EXISTANT_USER, secretKey);
            }
        }
    }

    public void GetPartidas() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        SecretKey secretKey = iControl.sendPublicKeyAndRecieveAES();

        ArrayList<Partida> partida = Partida.getGames();
        iControl.enviarInt(partida.size(), secretKey);

        for (int i = 0; i < partida.size(); i++) {
            Partida p = partida.get(i);
            iControl.enviarString(p.getNombrePartida(), secretKey);
            iControl.enviarString(p.getCreatorUserName(), secretKey);
            iControl.enviarInt(p.getCurrentPlayers(), secretKey);
            iControl.enviarInt(p.getMaxPlayers(), secretKey);
        }
    }

    public void UnirsePartida() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        SecretKey secretKey = iControl.sendPublicKeyAndRecieveAES();

        String email, contra;

        email = iControl.recibirString(secretKey);
        contra = iControl.recibirHex(secretKey);
        String nombrePartida = iControl.recibirString(secretKey);
        String contraPartida = iControl.recibirString(secretKey);
        Usuario user = null;
        if ((user = UsuarioDao.getInstance().getUsuario(email)) != null && user.getContrasenya().equals(contra)) {
            Partida p = Partida.BuscarPartida(nombrePartida);
            System.out.println(nombrePartida);
            if (p != null) {
                Thread t = p.conectarAPartida(contraPartida, iControl, user, secretKey);
                if (t != null) {
                    System.out.println(user.getEmailUsuario() + " se ha unido a la partida " + nombrePartida);
                    try {
                        t.join();
                    } catch (InterruptedException e) {
                    }
                } else {
                    iControl.enviarInt(NO, secretKey);
                    iControl.enviarInt(PARTIDA_ERROR_NO_ENTRAR_DENIED, secretKey);
                }
            } else {
                iControl.enviarInt(NO, secretKey);
                iControl.enviarInt(PARTIDA_ERROR_NON_EXISTANT_PARTIDA, secretKey);
            }
        } else {
            iControl.enviarInt(NO, secretKey);
            if (user != null) {
                iControl.enviarInt(USER_ERROR_INVALID_PASSWORD, secretKey);
            } else {
                iControl.enviarInt(USER_ERROR_NON_EXISTANT_USER, secretKey);
            }
        }
    }

}
