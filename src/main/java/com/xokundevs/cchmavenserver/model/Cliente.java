/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xokundevs.cchmavenserver.model;
import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.NoSuchPaddingException;

import com.xokundevs.cchmavenserver.Main;
import com.xokundevs.cchmavenserver.bddconnectivity.dao.BarajaDao;
import com.xokundevs.cchmavenserver.bddconnectivity.dao.CartablancaDao;
import com.xokundevs.cchmavenserver.bddconnectivity.dao.CartanegraDao;
import com.xokundevs.cchmavenserver.bddconnectivity.dao.UsuarioDao;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Baraja;
import com.xokundevs.cchmavenserver.bddconnectivity.model.BarajaId;
import com.xokundevs.cchmavenserver.bddconnectivity.model.CartaId;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Cartablanca;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Cartanegra;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Usuario;

/**
 *
 * @author Antonio
 */
public class Cliente extends Thread {

    // ENCODING VARIABLES

    // ORDENES
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

    // ERRORES
    private static final int CREATE_USER_ERROR_EXISTING_USER = -1;
    private static final int CREATE_USER_ERROR_INVALID_EMAIL = -2;
    private static final int CREATE_USER_ERROR_INVALID_PARAMETERS = -3;
    private static final int INVALID_CREDENTIALS_ERROR = -4;
    private static final int USER_ERROR_NON_EXISTANT_USER = -5;
    private static final int BARAJA_ERROR_NON_EXISTANT_BARAJA = -6;
    private static final int CREATE_USER_ERROR_LONG_EMAIL = -7;
    private static final int CREATE_USER_ERROR_LONG_USERNAME = -8;
    private static final int CREATE_USER_ERROR_INVALID_USERNAME = -9;
    private static final int PARTIDA_ERROR_NON_EXISTANT_PARTIDA = -10;
    private static final int PARTIDA_ERROR_EXISTING_PARTIDA = -11;
    private static final int PARTIDA_ERROR_NO_ENTRAR_DENIED = -12;
    private static final int UNKOWN_ERROR = -9999;

    private Socket sk;
    private InternetControl iControl;

    public Cliente(Socket sk) throws IOException {
        this.sk = sk;
        iControl = new InternetControl(sk);
    }

    @Override
    public void run() {
        try {
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
        } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException ex) {
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

    // USUARIOS
    public void RegistrarUsuario()
            throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, NoSuchPaddingException {
        iControl.sendPublicKeyAndRecieveAES();

        String email = iControl.recibirString();
        String contra = iControl.recibirHex();
        String name = iControl.recibirString();

        Usuario user = new Usuario(email);
        if (checkValidEmail(email) == false || email.length() > 50) {
            iControl.enviarInt(NO);
            if (email.length() > 50) {
                iControl.enviarInt(CREATE_USER_ERROR_LONG_EMAIL);
            } else {
                iControl.enviarInt(CREATE_USER_ERROR_INVALID_EMAIL);
            }
        } else if (!checkValidUsername(name) || name.length() > 15) {
            iControl.enviarInt(NO);
            if (name.length() > 15) {
                iControl.enviarInt(CREATE_USER_ERROR_LONG_USERNAME);
            } else {
                iControl.enviarInt(CREATE_USER_ERROR_INVALID_USERNAME);
            }
        } else if (UsuarioDao.getInstance().exists(user)) {
            System.out.println("NO");
            iControl.enviarInt(NO);
            iControl.enviarInt(CREATE_USER_ERROR_EXISTING_USER);
        } else {
            iControl.enviarInt(OK);

            System.out.println(email + " " + contra + " " + name);
            user.setContrasenya(contra);
            user.setNombreUsuario(name);
            user.setPartidasGanadas(0);

            if (UsuarioDao.getInstance().saveUsuario(user)) {
                System.out.println("OK");
            } else {
                iControl.enviarInt(NO);
                iControl.enviarInt(CREATE_USER_ERROR_INVALID_PARAMETERS);
            }
        }
    }

    public void LoginUsuario() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {

        iControl.sendPublicKeyAndRecieveAES();

        String email = iControl.recibirString();

        String password = iControl.recibirHex();

        System.out.println(email + " - " + password);
        Usuario user;
        if ((user = UsuarioDao.getInstance().getUsuario(email)) != null && user.getContrasenya().equals(password)) {
            iControl.enviarInt(OK);

            iControl.enviarString(user.getNombreUsuario());
            iControl.enviarInt(user.getPartidasGanadas());

        } else {
            iControl.enviarInt(NO);
            iControl.enviarInt(INVALID_CREDENTIALS_ERROR);
        }
    }

    public void EraseUsuario() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        iControl.sendPublicKeyAndRecieveAES();

        String password = iControl.recibirHex();

        String email = iControl.recibirString();

        Usuario user;
        UsuarioDao uDao = UsuarioDao.getInstance();
        if ((user = uDao.exists(email, password)) != null) {
            if (uDao.deleteUsuario(user)) {
                iControl.enviarInt(OK);
            } else {
                // DATABASE_ERROR
                iControl.enviarInt(NO);
                iControl.enviarInt(UNKOWN_ERROR);
            }
        } else {
            // USER NON EXISTANT OR INVALID PASSWORD
            iControl.enviarInt(NO);
            iControl.enviarInt(INVALID_CREDENTIALS_ERROR);
        }
    }

    // BARAJAS
    public void SendBasicBarajasInfo() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        iControl.sendPublicKeyAndRecieveAES();

        String email = iControl.recibirString();

        String password = iControl.recibirHex();

        Usuario user;
        System.out.println("Llego -- GET_MAZOS");
        if ((user = UsuarioDao.getInstance().getUsuario(email)) != null && user.getContrasenya().equals(password)) {

            System.out.println("OK -- GET_MAZOS");
            iControl.enviarInt(OK);

            BarajaDao bDao = BarajaDao.getInstance();

            List<Baraja> list = bDao.getBarajas(email);

            list.addAll(bDao.getBarajas("default"));

            iControl.enviarInt(list.size());

            System.out.println(list.size());

            for (Baraja baraja : list) {

                iControl.enviarString(baraja.getId().getEmailUsuario());

                iControl.enviarString(baraja.getId().getNombreBaraja());

                iControl.enviarString(baraja.getUsuario().getNombreUsuario());

                iControl.enviarInt(baraja.getCartasnegras().size() + baraja.getCartasblancas().size());

                iControl.enviarString(baraja.getIdioma());

            }

        } else {
            System.out.println("NO -- GET_MAZOS");
            iControl.enviarInt(NO);
            iControl.enviarInt(INVALID_CREDENTIALS_ERROR);
        }

    }

    public void SendCartasBaraja() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        iControl.sendPublicKeyAndRecieveAES();

        String email = iControl.recibirString();
        String nombreMazo = iControl.recibirString();

        BarajaDao bDao = BarajaDao.getInstance();
        Baraja b;
        Usuario user;
        if ((user = UsuarioDao.getInstance().getUsuario(email)) != null) {
            if ((b = bDao.getBaraja(user.getEmailUsuario(), nombreMazo)) != null) {
                iControl.enviarInt(OK);

                iControl.enviarInt(b.getCartasnegras().size() + b.getCartasblancas().size());

                for (Cartanegra c : b.getCartasnegras()) {
                    boolean isNegra = true;

                    iControl.enviarInt((isNegra) ? 1 : 0);

                    iControl.enviarString(c.getTexto());

                    iControl.enviarInt(c.getId().getIdCarta());

                    iControl.enviarInt(c.getNumeroEspacios());
                }

                for (Cartablanca c : b.getCartasblancas()) {
                    boolean isNegra = false;

                    iControl.enviarInt((isNegra) ? 1 : 0);

                    iControl.enviarString(c.getTexto());

                    iControl.enviarInt(c.getId().getIdCarta());
                }

            } else {
                iControl.enviarInt(NO);
                iControl.enviarInt(BARAJA_ERROR_NON_EXISTANT_BARAJA);
            }
        } else {
            iControl.enviarInt(NO);
            iControl.enviarInt(USER_ERROR_NON_EXISTANT_USER);
        }
    }

    public void RecibeBarajaNueva() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        iControl.sendPublicKeyAndRecieveAES();

        String email = iControl.recibirString();
        String password = iControl.recibirHex();

        Usuario user;
        if ((user = UsuarioDao.getInstance().getUsuario(email)) != null && user.getContrasenya().equals(password)) {
            System.out.println("Barajaname");
            String barajaname = iControl.recibirString();

            System.out.println("idioma");
            String idioma = iControl.recibirString();

            System.out.println("numeroCartas");
            int numeroCartas = iControl.recibirInt();

            Baraja baraja;
            BarajaId bId = new BarajaId(email, barajaname);
            baraja = new Baraja(bId, user);
            baraja.setIdioma(idioma);

            ArrayList<Cartablanca> listWhiteCard = new ArrayList<>();
            ArrayList<Cartanegra> listBlackCard = new ArrayList<>();

            for (int i = 0; i < numeroCartas; i++) {
                int isNegra = iControl.recibirInt();

                int id = i;

                String texto = iControl.recibirString();

                CartaId cId = new CartaId(id, email, barajaname);

                if (isNegra == 1) {
                    int numEspacios = iControl.recibirInt();
                    Cartanegra cNegra = new Cartanegra(cId, baraja, numEspacios, texto);
                    listBlackCard.add(cNegra);
                } else {
                    Cartablanca cBlanca = new Cartablanca(cId, baraja, texto);
                    listWhiteCard.add(cBlanca);
                }
            }
            if (BarajaDao.getInstance().saveBaraja(baraja, listWhiteCard, listBlackCard)) {
                iControl.enviarInt(OK);
            } else {
                System.out.println("UNKOWN_ERROR");
                iControl.enviarInt(NO);
                iControl.enviarInt(UNKOWN_ERROR);
            }
        } else {
            iControl.enviarInt(NO);
            iControl.enviarInt(INVALID_CREDENTIALS_ERROR);
        }
    }

    public void BorraBarajaPropia() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        iControl.sendPublicKeyAndRecieveAES();

        String usuarioEmail = iControl.recibirString();
        String usuarioPassword = iControl.recibirHex();
        String nombreBaraja = iControl.recibirString();
        Usuario user;
        if ((user = UsuarioDao.getInstance().getUsuario(usuarioEmail)) != null
                && user.getContrasenya().equals(usuarioPassword)) {
            Baraja baraja;
            if ((baraja = BarajaDao.getInstance().getBaraja(usuarioEmail, nombreBaraja)) != null) {

                BarajaDao.getInstance().deleteBaraja(baraja);

                iControl.enviarInt(OK);
            } else {
                iControl.enviarInt(NO);
                iControl.enviarInt(BARAJA_ERROR_NON_EXISTANT_BARAJA);
            }
        } else {
            iControl.enviarInt(NO);
            iControl.enviarInt(INVALID_CREDENTIALS_ERROR);
        }
    }

    // PARTIDAS
    public void CreaPartida() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        iControl.sendPublicKeyAndRecieveAES();

        String email, contra;

        email = iControl.recibirString();
        contra = iControl.recibirHex();

        Usuario user = null;
        if ((user = UsuarioDao.getInstance().getUsuario(email)) != null && user.getContrasenya().equals(contra)) {
            iControl.enviarInt(OK);

            String namePartida = iControl.recibirString();
            String contrasenaPartida = iControl.recibirString();

            ArrayList<Baraja> listaBarajas = new ArrayList<>();
            int maxPlayers = iControl.recibirInt();
            int cantidadMazos = iControl.recibirInt();

            for (int i = 0; i < cantidadMazos; i++) {
                String emailMazo, nombreMazo;
                emailMazo = iControl.recibirString();
                nombreMazo = iControl.recibirString();

                if (emailMazo.equals(user.getEmailUsuario()) || emailMazo.equals("default")) {
                    Baraja b = BarajaDao.getInstance().getBaraja(emailMazo, nombreMazo);
                    if (b != null) {
                        listaBarajas.add(b);
                    }
                }
            }
            if (listaBarajas.isEmpty()) {
                iControl.enviarInt(NO);
                iControl.enviarInt(BARAJA_ERROR_NON_EXISTANT_BARAJA);
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
                Partida p = new Partida(namePartida, contrasenaPartida, maxPlayers, listaCartasBlancas,
                        listaCartasNegras, user.getEmailUsuario());
                if (Partida.addPartida(p)) {
                    if (p.conectarAPartida(contrasenaPartida, iControl, user) != null) {
                        iControl.enviarInt(OK);
                        try {
                            p.join();
                        } catch (InterruptedException e) {
                        }
                    } else {
                        p.BorraPartida();
                        iControl.enviarInt(NO);
                        iControl.enviarInt(PARTIDA_ERROR_NO_ENTRAR_DENIED);
                    }
                } else {
                    iControl.enviarInt(NO);
                    iControl.enviarInt(PARTIDA_ERROR_EXISTING_PARTIDA);
                }
            }
        } else {
            iControl.enviarInt(NO);
            iControl.enviarInt(INVALID_CREDENTIALS_ERROR);
        }
    }

    public void GetPartidas() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        iControl.sendPublicKeyAndRecieveAES();

        ArrayList<Partida> partida = Partida.getGames();
        iControl.enviarInt(partida.size());

        for (int i = 0; i < partida.size(); i++) {
            Partida p = partida.get(i);
            iControl.enviarString(p.getNombrePartida());
            iControl.enviarString(p.getCreatorUserName());
            iControl.enviarInt(p.getCurrentPlayers());
            iControl.enviarInt(p.getMaxPlayers());
        }
    }

    public void UnirsePartida() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
        iControl.sendPublicKeyAndRecieveAES();

        String email, contra;

        email = iControl.recibirString();
        contra = iControl.recibirHex();
        String nombrePartida = iControl.recibirString();
        String contraPartida = iControl.recibirString();
        Usuario user = null;
        if ((user = UsuarioDao.getInstance().exists(email, contra)) != null) {
            Partida p = Partida.BuscarPartida(nombrePartida);
            System.out.println(nombrePartida);
            if (p != null) {
                Thread t = p.conectarAPartida(contraPartida, iControl, user);
                if (t != null) {
                    System.out.println(user.getEmailUsuario() + " se ha unido a la partida " + nombrePartida);
                    try {
                        t.join();
                    } catch (InterruptedException e) {
                    }
                } else {
                    iControl.enviarInt(NO);
                    iControl.enviarInt(PARTIDA_ERROR_NO_ENTRAR_DENIED);
                }
            } else {
                iControl.enviarInt(NO);
                iControl.enviarInt(PARTIDA_ERROR_NON_EXISTANT_PARTIDA);
            }
        } else {
            iControl.enviarInt(NO);
            iControl.enviarInt(INVALID_CREDENTIALS_ERROR);
        }
    }
}
