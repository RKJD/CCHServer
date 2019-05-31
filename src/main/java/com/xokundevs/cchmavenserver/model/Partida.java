/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xokundevs.cchmavenserver.model;

import com.xokundevs.cchmavenserver.bddconnectivity.model.Baraja;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Usuario;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;

/**
 *
 * @author Antonio
 */
class Partida extends Thread {

    private static final int EMPEZAR_PARTIDA = 1001;
    private static final int CERRAR_PARTIDA = 1002;
    private static final int NEW_PLAYER = 1003;

    private static final int ERROR_NO_SUFFICIENT_PLAYERS = -1001;
    private static final int ERROR_FILLED_ROOM = -1002;

    private static final ArrayList<Partida> PARTIDAS_ABIERTAS = new ArrayList<>();

    public static boolean addPartida(Partida p) {
        boolean exists = false;
        synchronized (PARTIDAS_ABIERTAS) {
            for (Partida par : PARTIDAS_ABIERTAS) {
                if (par.nombrePartida.equals(p.nombrePartida)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                PARTIDAS_ABIERTAS.add(p);
            }
        }
        return !exists;
    }

    private boolean canEnter;
    private int maxPlayers;
    private final InternetControl[] iControls;
    private final Usuario[] users;
    private final String nombrePartida;
    private final String contrasena;
    private final SecretKey[] secretKeys;
    private int currentPlayers;
    private String emailCreator;
    private ArrayList<Baraja> listaBarajas;

    public Partida(String nombrePartida, String contrasena, int maxPlayers, ArrayList<Baraja> listaMazos, String emailString) {
        this.canEnter = false;
        this.maxPlayers = maxPlayers;
        this.iControls = new InternetControl[maxPlayers];
        this.users = new Usuario[maxPlayers];

        this.nombrePartida = nombrePartida;
        this.contrasena = contrasena;
        this.secretKeys = new SecretKey[maxPlayers];

        currentPlayers = 0;

        this.listaBarajas = listaMazos;

        emailCreator = emailString;
    }

    public String getNombrePartida() {
        return nombrePartida;
    }

    @Override
    public void run() {
        InternetControl principal = iControls[0];
        try {
            int result;
            do {
                try {
                    result = principal.recibirInt(secretKeys[0]);
                } catch (IOException e) {
                    result = CERRAR_PARTIDA;
                }
                if (currentPlayers < 3 && result != CERRAR_PARTIDA) {
                    principal.enviarInt(ERROR_NO_SUFFICIENT_PLAYERS, secretKeys[0]);
                }
            } while (currentPlayers < 3 && result != CERRAR_PARTIDA);

            if (result == EMPEZAR_PARTIDA) {

            } else {
                BorraPartida();
            }
        } catch (IOException ex) {
            Logger.getLogger(Partida.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void AvisaEmpiezaPartida() throws IOException {
        synchronized (this) {
            canEnter = false;
        }
        for (int i = 1; i < maxPlayers; i++) {
            iControls[i].enviarInt(EMPEZAR_PARTIDA, secretKeys[i]);
        }
    }

    private void BorraPartida() {
        synchronized (this) {
            synchronized (PARTIDAS_ABIERTAS) {
                PARTIDAS_ABIERTAS.remove(this);
            }
        }
    }

    public Partida conectarAPartida(String password, InternetControl iControl, Usuario user, SecretKey secretKey) throws IOException {
        synchronized (this) {
            if (password.equals(this.contrasena)) {
                int pos = currentPlayers;
                if (currentPlayers == 0) {
                    if (user.getEmailUsuario().equals(emailCreator)) {
                        iControl.enviarInt(Cliente.OK, secretKey);
                        users[pos] = user;
                        iControls[pos] = iControl;
                        secretKeys[pos] = secretKey;
                        currentPlayers++;
                        canEnter = true;
                        this.start();
                        return this;
                    } else {
                        return null;
                    }
                } else if (currentPlayers < maxPlayers && canEnter) {
                    iControls[pos] = iControl;
                    this.users[pos] = user;
                    secretKeys[pos] = secretKey;
                    synchronized (iControls) {
                        for (int i = 0; i < currentPlayers; i++) {
                            iControls[i].enviarInt(NEW_PLAYER, secretKey);
                            iControls[i].enviarString(user.getEmailUsuario(), secretKey);
                            iControl.enviarInt(NEW_PLAYER, secretKey);
                            iControl.enviarString(users[i].getEmailUsuario(), secretKey);
                        }
                        currentPlayers++;
                        iControl.enviarInt(Cliente.OK, secretKey);
                    }
                    return this;
                } else {
                    return null;
                }
            }
            return null;
        }
    }

    public static Partida BuscarPartida(String nombre) {
        Partida p = null;
        synchronized (PARTIDAS_ABIERTAS) {
            for (int i = 0; i < PARTIDAS_ABIERTAS.size(); i++) {
                Partida object = PARTIDAS_ABIERTAS.get(i);
                if (object.nombrePartida.equals(nombre)) {
                    p = object;
                    i = PARTIDAS_ABIERTAS.size();
                }

            }
        }
        return p;
    }

    public static ArrayList<Partida> getGames() {
        return (ArrayList<Partida>) PARTIDAS_ABIERTAS.clone();
    }

    public int getCurrentPlayers() {
        return currentPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public String getCreatorUserName() {
        return users[0].getNombreUsuario();
    }
}
