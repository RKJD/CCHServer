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

    private static final int ERROR_NO_SUFFICIENT_PLAYERS = -1001;
    private static final int ERROR_FILLED_ROOM = -1002;

    private static final ArrayList<Partida> PARTIDAS_ABIERTAS = new ArrayList<>();

    public static void addPartida(Partida p) {
        PARTIDAS_ABIERTAS.add(p);
    }

    private boolean canEnter;
    private int maxPlayers;
    private final InternetControl[] iControls;
    private final Usuario[] user;
    private final String nombrePartida;
    private final String contrasena;
    private final SecretKey[] secretKeys;
    private int currentPlayers;
    private ArrayList<Baraja> listaBarajas;

    public Partida(String nombrePartida, String contrasena, int maxPlayers,
            InternetControl iControl, Usuario user, SecretKey secretKey,
            ArrayList<Baraja> listaMazos) {
        this.canEnter = true;
        this.maxPlayers = maxPlayers;
        this.iControls = new InternetControl[maxPlayers];
        iControls[0] = iControl;
        this.user = new Usuario[maxPlayers];
        this.user[0] = user;

        this.nombrePartida = nombrePartida;
        this.contrasena = contrasena;
        this.secretKeys = new SecretKey[maxPlayers];
        this.secretKeys[0] = secretKey;
        currentPlayers = 1;

        this.listaBarajas = listaMazos;
    }

    @Override
    public void run() {
        synchronized (PARTIDAS_ABIERTAS) {
            Partida.addPartida(this);
        }
        InternetControl principal = iControls[0];
        try {
            int result;
            do {
                result = principal.recibirInt(secretKeys[0]);
                if (currentPlayers < 3 && result != CERRAR_PARTIDA) {
                    principal.enviarInt(ERROR_NO_SUFFICIENT_PLAYERS, secretKeys[0]);
                }
            } while (currentPlayers < 3 && result != CERRAR_PARTIDA);

            if (result == EMPEZAR_PARTIDA) {

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

    public Thread conectarAPartida(InternetControl iControl, Usuario user, SecretKey secretKey) throws IOException {
        synchronized (this) {
            if (currentPlayers < maxPlayers && canEnter) {
                int pos = currentPlayers;
                iControls[pos] = iControl;
                this.user[pos] = user;
                secretKeys[pos] = secretKey;
                currentPlayers++;
                return this;
            } else {
                return null;
            }
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
    
    public static ArrayList<Partida> getGames(){
        return (ArrayList<Partida>) PARTIDAS_ABIERTAS.clone();
    }

    public int getCurrentPlayers() {
        return currentPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }
    
    public String getCreatorUserName(){
        return user[0].getNombreUsuario();
    }
}
