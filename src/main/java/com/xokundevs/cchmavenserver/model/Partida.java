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
import javax.crypto.SecretKey;

/**
 *
 * @author Antonio
 */
class Partida {
    private static final int EMPEZAR_PARTIDA = 1001;
    private static final int CERRAR_PARTIDA = 1002;
    
    private static final int ERROR_NO_SUFFICIENT_PLAYERS = -1001;
    
    private static final ArrayList<Partida> PARTIDAS_ABIERTAS = new ArrayList<>();
    
    public static void addPartida(Partida p){
        PARTIDAS_ABIERTAS.add(p);
    }
    
    private boolean canEnter;
    private int maxPlayers;
    private InternetControl[] iControls;
    private Usuario[] user;
    private String nombrePartida, contrasena;
    private SecretKey[] secretKeys;
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
    
    
    public void run() throws IOException{
        Partida.addPartida(this);
        InternetControl principal = iControls[0];
        int result;
        do{
            result = principal.recibirInt(secretKeys[0]);
            if(currentPlayers < 3 && result != CERRAR_PARTIDA){
                principal.enviarInt(ERROR_NO_SUFFICIENT_PLAYERS, secretKeys[0]);
            }
        }while(currentPlayers < 3 && result != CERRAR_PARTIDA);
    }
    
    public synchronized void conectarAPartida(InternetControl iControl, SecretKey secretKey){
        if(currentPlayers < maxPlayers){
            iControls[currentPlayers] = iControl;
            secretKeys[currentPlayers] = secretKey;
            currentPlayers++;
        }
    }
    
    class GameRunnable {
        
    }
}
