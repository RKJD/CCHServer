/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xokundevs.cchmavenserver.model;

import com.xokundevs.cchmavenserver.bddconnectivity.model.Baraja;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Carta;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Cartablanca;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Cartanegra;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Usuario;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;

/**
 *
 * @author Antonio
 */
class Partida extends Thread {

    private static final int EMPEZAR_PARTIDA = 1001;
    private static final int REPARTIR_CARTAS = 1002;
    private static final int ESCOGER_TZAR = 1003;
    private static final int MUESTRA_CARTA_NEGRA = 1004;
    private static final int ESCOGER_CARTAS = 1005;
    private static final int TZAR_ESCOGE_GANADOR = 1006;
    private static final int TZAR_YA_HA_ESCOGIDO_GANADOR = 1009;
    private static final int REPARTIR_CARTAS_FASE_2 = 1007;
    private static final int YA_HAY_GANADOR = 1008;
    private static final int CERRAR_PARTIDA = 1500;
    private static final int NEW_PLAYER = 1003;

    private static final int ERROR_NO_SUFFICIENT_PLAYERS = -1001;
    private static final int ERROR_FILLED_ROOM = -1002;
    private static final int ERROR_PLAYER_DISCONNECTED = -1003;

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
    private final String nombrePartida;
    private final String contrasena;
    private int currentPlayers;
    private final String emailCreator;
    private ArrayList<Cartablanca> listaCartasBlancas;
    private ArrayList<Cartanegra> listaCartasNegras;
    private final int maxPoints = 5;
    private Cartanegra lastBlackCard = null;
    private Player currentTzar = null;
    private final ArrayList<Player> jugadores;

    public Partida(String nombrePartida, String contrasena, int maxPlayers,
            ArrayList<Cartablanca> listaCartasBlancas,
            ArrayList<Cartanegra> listaCartasNegras, String emailString) {
        this.canEnter = false;
        this.maxPlayers = maxPlayers;

        this.nombrePartida = nombrePartida;
        this.contrasena = contrasena;

        currentPlayers = 0;

        this.listaCartasBlancas = listaCartasBlancas;
        this.listaCartasNegras = listaCartasNegras;

        emailCreator = emailString;

        jugadores = new ArrayList<>();
    }

    public String getNombrePartida() {
        return nombrePartida;
    }

    @Override
    public void run() {
        InternetControl principal = jugadores.get(0).iControl;
        try {
            int result;
            do {
                try {
                    result = principal.recibirInt(jugadores.get(0).secretKey);
                } catch (IOException e) {
                    result = CERRAR_PARTIDA;
                }
                synchronized (this) {
                    if (currentPlayers < 3 && result != CERRAR_PARTIDA) {
                        principal.enviarInt(ERROR_NO_SUFFICIENT_PLAYERS, jugadores.get(0).secretKey);
                    }
                }
            } while (currentPlayers < 3 && result != CERRAR_PARTIDA);

            if (result == EMPEZAR_PARTIDA) {
                cicloDeJuego();
            } else {
                cerrarPartida();
            }
        } catch (IOException ex) {
            Logger.getLogger(Partida.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private int ESTADO = EMPEZAR_PARTIDA;

    private void cicloDeJuego() {
        AvisaEmpiezaPartida();
        ESTADO = REPARTIR_CARTAS;
        while (ESTADO != CERRAR_PARTIDA) {
            switch (ESTADO) {
                case REPARTIR_CARTAS:
                    repartirCartasInicial();
                    ESTADO = ESCOGER_TZAR;
                    break;
                case ESCOGER_TZAR:
                    escogeTzar();
                    ESTADO = MUESTRA_CARTA_NEGRA;
                    break;
                case MUESTRA_CARTA_NEGRA:
                    GetNewBlackCard();
                    ESTADO = ESCOGER_CARTAS;
                    break;
                case ESCOGER_CARTAS:
                    JugarCartas();
                    ESTADO = TZAR_ESCOGE_GANADOR;
                    break;
                case TZAR_ESCOGE_GANADOR:
                    if (TzarEscogeGanador()) {
                        ESTADO = YA_HAY_GANADOR;
                    } else {
                        ESTADO = REPARTIR_CARTAS_FASE_2;
                    }
                    break;
                case REPARTIR_CARTAS_FASE_2:
                    repartirCartasInicial();
                    ESTADO = ESCOGER_TZAR;
                    break;

                case YA_HAY_GANADOR:
                    if (AcabarPartidaYaGanador()) {
                        ESTADO = CERRAR_PARTIDA;
                    } else {
                        ESTADO = ESCOGER_TZAR;
                    }
                    break;
            }
        }
        cerrarPartida();
    }

    public void cerrarPartida() {
        Thread[] threads = new Thread[currentPlayers];

        for (int i = 0; i < currentPlayers; i++) {
            final InternetControl iC = jugadores.get(i).iControl;
            final SecretKey secretKey = jugadores.get(i).secretKey;
            final int tempCount = i;
            threads[i] = new Thread(() -> {
                try {
                    synchronized (iC) {
                        iC.enviarInt(CERRAR_PARTIDA, secretKey);
                    }
                } catch (IOException ex) {
                    borrarJugador(tempCount);
                }
            });
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Partida.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        BorraPartida();
    }

    public boolean AcabarPartidaYaGanador() {
        Player ganador = null;
        for (Player p : jugadores) {
            if (p.puntos >= maxPoints) {
                ganador = p;
                break;//SAL DEL FOR BREAK
            }
        }
        final Player realGanador = ganador;
        if (realGanador != null) {
            Thread[] threads = new Thread[currentPlayers];

            for (int i = 0; i < threads.length; i++) {
                final InternetControl iC = jugadores.get(i).iControl;
                final SecretKey secretKey = jugadores.get(i).secretKey;
                final int countTemp = i;
                threads[i] = new Thread(() -> {
                    try {
                        iC.enviarInt(YA_HAY_GANADOR, secretKey);
                        iC.enviarString(realGanador.user.getEmailUsuario(), secretKey);
                    } catch (IOException e) {
                        borrarJugador(countTemp);
                    }
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Partida.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return true;
        }
        return false;
    }

    public boolean TzarEscogeGanador() {
        Thread[] threads = new Thread[currentPlayers];

        final Player[] OrderPlayer = new Player[currentPlayers - 1];
        int intTemp = 0;
        for (int i = 0; i < currentPlayers; i++) {
            if (!jugadores.get(i).equals(currentTzar)) {
                OrderPlayer[intTemp] = jugadores.get(i);
                intTemp++;
            }
        }
        Player temp = null;

        for (int i = 0; i < currentPlayers * 3; i++) {
            int random1, random2;
            random1 = (int) (Math.random() * currentPlayers);
            random2 = (int) (Math.random() * currentPlayers);
            temp = OrderPlayer[random1];
            OrderPlayer[random1] = OrderPlayer[random2];
            OrderPlayer[random2] = temp;
        }

        for (int i = 0; i < threads.length; i++) {
            final InternetControl iC = jugadores.get(i).iControl;
            final SecretKey secretKey = jugadores.get(i).secretKey;
            final int tempCount = i;
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        synchronized (iC) {
                            iC.enviarInt(TZAR_ESCOGE_GANADOR, secretKey);
                            iC.enviarInt(OrderPlayer.length, secretKey);
                            for (int i = 0; i < OrderPlayer.length; i++) {
                                Cartablanca[] cartasJug = OrderPlayer[i].cartaEscogida;
                                for (int j = 0; j < cartasJug.length; i++) {
                                    iC.enviarString(cartasJug[j].getCarta().getTexto(), secretKey);
                                }
                            }
                        }
                    } catch (IOException ex) {
                        borrarJugador(tempCount);
                    }
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Partida.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        int ganador = Integer.MIN_VALUE;
        try {
            synchronized (currentTzar.iControl) {
                ganador = currentTzar.iControl.recibirInt(currentTzar.secretKey);
            }
        } catch (IOException ex) {
            borrarJugador(currentTzar);
        }

        boolean hayGanador = false;
        if (ganador > -1) {
            Player pGanador = OrderPlayer[ganador];
            pGanador.puntos++;
            threads = new Thread[currentPlayers];
            for (int i = 0; i < threads.length; i++) {
                final InternetControl iC = jugadores.get(i).iControl;
                final SecretKey secretKey = jugadores.get(i).secretKey;
                final int tempCount = i;
                threads[i] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            synchronized (iC) {
                                iC.enviarInt(TZAR_YA_HA_ESCOGIDO_GANADOR, secretKey);
                                iC.enviarString(pGanador.user.getEmailUsuario(), secretKey);
                                iC.enviarInt(pGanador.puntos, secretKey);
                            }
                        } catch (IOException ex) {
                            borrarJugador(tempCount);
                        }
                    }
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Partida.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            currentTzar.wasTzar = true;

            hayGanador = (pGanador.puntos >= maxPoints);
        }

        return hayGanador;
    }

    public void JugarCartas() {

        Thread[] threads = new Thread[currentPlayers];

        for (int i = 0; i < currentPlayers; i++) {
            final InternetControl iC = jugadores.get(i).iControl;
            final SecretKey secretKey = jugadores.get(i).secretKey;
            final int tempCount = i;
            threads[i] = new Thread(() -> {
                try {
                    synchronized (iC) {
                        iC.enviarInt(ESCOGER_CARTAS, secretKey);
                        int cantidad = iC.recibirInt(secretKey);
                        jugadores.get(tempCount).cartaEscogida = new Cartablanca[cantidad];
                        for (int j = 0; j < cantidad; j++) {
                            jugadores.get(tempCount).cartaEscogida[j] = jugadores.get(tempCount).cartasEnMano.get(iC.recibirInt(secretKey));
                        }
                    }
                } catch (IOException ex) {
                    borrarJugador(tempCount);
                }
            });
        }

        for (Thread t : threads) {
            t.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Partida.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void GetNewBlackCard() {
        Cartanegra enviar;
        do {
            enviar = listaCartasNegras.get((int) (Math.random() * listaCartasNegras.size()));
        } while (lastBlackCard != null && lastBlackCard.equals(enviar));

        lastBlackCard = enviar;

        Thread[] threads = new Thread[currentPlayers];

        for (int i = 0; i < currentPlayers; i++) {
            final InternetControl iC = jugadores.get(i).iControl;
            final SecretKey secretKey = jugadores.get(i).secretKey;
            final int tempCount = i;
            threads[i] = new Thread(() -> {
                try {
                    synchronized (iC) {
                        iC.enviarInt(MUESTRA_CARTA_NEGRA, secretKey);
                        Carta c = lastBlackCard.getCarta();
                        iC.enviarString(c.getTexto(), secretKey);
                        iC.enviarInt(c.getId().getIdCarta(), secretKey);
                        iC.enviarString(c.getId().getEmailUsuario(), secretKey);
                        iC.enviarString(c.getId().getNombreBaraja(), secretKey);
                        iC.enviarInt(lastBlackCard.getNumeroEspacios(), secretKey);
                    }
                } catch (IOException ex) {
                    borrarJugador(tempCount);
                }
            });
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Partida.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void repartirCartasInicial() {
        ArrayList<Cartablanca> cartasValidas = (ArrayList<Cartablanca>) listaCartasBlancas.clone();

        for (Player p : jugadores) {
            cartasValidas.removeAll(p.cartasEnMano);
        }

        for (int j = 0; j < currentPlayers; j++) {
            while (jugadores.get(j).cartasEnMano.size() < 10) {
                Cartablanca c = cartasValidas.get((int) (Math.random() * cartasValidas.size()));
                cartasValidas.remove(c);
                jugadores.get(j).cartasEnMano.add(c);
            }
        }
        Thread[] threads = new Thread[currentPlayers];

        for (int i = 0; i < currentPlayers; i++) {
            final InternetControl iC = jugadores.get(i).iControl;
            final SecretKey secretKey = jugadores.get(i).secretKey;
            final int tempCount = i;
            threads[i] = new Thread(() -> {
                try {
                    synchronized (iC) {
                        iC.enviarInt(REPARTIR_CARTAS, secretKey);
                        iC.enviarInt(10, secretKey);
                        for (int j = 0; j < 10; j++) {
                            Carta c = jugadores.get(tempCount).cartasEnMano.get(j).getCarta();
                            iC.enviarString(c.getTexto(), secretKey);
                            iC.enviarInt(c.getId().getIdCarta(), secretKey);
                            iC.enviarString(c.getId().getEmailUsuario(), secretKey);
                            iC.enviarString(c.getId().getNombreBaraja(), secretKey);
                        }
                    }
                } catch (IOException ex) {
                    borrarJugador(tempCount);
                }
            });
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Partida.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void AvisaEmpiezaPartida() {
        synchronized (this) {
            canEnter = false;
        }
        Thread t[] = new Thread[currentPlayers];
        for (int i = 0; i < currentPlayers; i++) {

            final InternetControl iC = jugadores.get(i).iControl;
            final SecretKey ScKey = jugadores.get(i).secretKey;
            final int tempCount = i;

            if (iC != null) {
                t[i] = new Thread(() -> {
                    try {
                        synchronized (iC) {
                            iC.enviarInt(EMPEZAR_PARTIDA, ScKey);
                        }
                    } catch (IOException ex) {
                        borrarJugador(tempCount);
                    }
                });
            }
        }
        for (Thread tt : t) {
            tt.start();
        }
        for (Thread tt : t) {
            try {
                tt.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Partida.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void escogeTzar() {
        ArrayList<Player> array = new ArrayList<>();

        for (int i = 0; i < currentPlayers;) {
            jugadores.get(i).isTzar = false;
            if (!jugadores.get(i).wasTzar) {
                array.add(jugadores.get(i));
            }
        }

        if (array.isEmpty()) {
            for (int i = 0; i < currentPlayers;) {
                jugadores.get(i).wasTzar = false;
            }
            for (int i = 0; i < currentPlayers;) {
                if (!jugadores.get(i).wasTzar) {
                    array.add(jugadores.get(i));
                }
            }
        }

        int randonNum = (int) (Math.random() * array.size());

        Player tzar = array.get(randonNum);
        tzar.isTzar = true;
        currentTzar = tzar;
        Thread[] threads = new Thread[currentPlayers];

        for (int i = 0; i < currentPlayers; i++) {
            final InternetControl iC = jugadores.get(i).iControl;
            final SecretKey secretKey = jugadores.get(i).secretKey;
            final int tempCount = i;
            threads[i] = new Thread(() -> {
                try {
                    synchronized (iC) {
                        iC.enviarInt(ESCOGER_TZAR, secretKey);
                        iC.enviarString(currentTzar.user.getEmailUsuario(), secretKey);
                    }
                } catch (IOException ex) {
                    borrarJugador(tempCount);
                }
            });
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Partida.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void borrarJugador(int i) {
        synchronized (this) {
            Player tempPlayer = jugadores.get(i);
            jugadores.remove(i);
            currentPlayers--;
            for (int z = 0; z < currentPlayers; z++) {
                final int temp = z;
                new Thread(() -> {
                    synchronized (Partida.this) {
                        try {
                            jugadores.get(temp).iControl.enviarInt(ERROR_PLAYER_DISCONNECTED, jugadores.get(temp).secretKey);
                            jugadores.get(temp).iControl.enviarString(tempPlayer.user.getEmailUsuario(), jugadores.get(temp).secretKey);
                        } catch (IOException ex) {
                        }
                    }
                }).start();
            }
        }
    }

    private void borrarJugador(Player player) {
        synchronized (this) {
            jugadores.remove(player);
            currentPlayers--;
            for (int z = 0; z < currentPlayers; z++) {
                final int temp = z;
                new Thread(() -> {
                    synchronized (Partida.this) {
                        try {
                            jugadores.get(temp).iControl.enviarInt(ERROR_PLAYER_DISCONNECTED, jugadores.get(temp).secretKey);
                            jugadores.get(temp).iControl.enviarString(player.user.getEmailUsuario(), jugadores.get(temp).secretKey);
                        } catch (IOException ex) {
                        }
                    }
                }).start();
            }
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
                        jugadores.add(new Player(user, iControl, secretKey));
                        currentPlayers++;
                        canEnter = true;
                        this.start();
                        return this;
                    } else {
                        return null;
                    }
                } else if (currentPlayers < maxPlayers && canEnter) {
                    jugadores.add(new Player(user, iControl, secretKey));
                    synchronized (jugadores) {
                        for (int i = 0; i < currentPlayers; i++) {
                            jugadores.get(i).iControl.enviarInt(NEW_PLAYER, secretKey);
                            jugadores.get(i).iControl.enviarString(user.getEmailUsuario(), secretKey);
                            iControl.enviarInt(NEW_PLAYER, secretKey);
                            iControl.enviarString(jugadores.get(i).user.getEmailUsuario(), secretKey);
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
        return jugadores.get(0).user.getNombreUsuario();
    }

    public class Player {

        ArrayList<Cartablanca> cartasEnMano;
        Cartablanca[] cartaEscogida = null;
        SecretKey secretKey;
        InternetControl iControl;
        int puntos = 0;
        Usuario user;
        boolean isTzar = false, wasTzar = false;

        private Player(Usuario user, InternetControl iControl, SecretKey secretKey) {
            this.user = user;
            this.iControl = iControl;
            this.secretKey = secretKey;
            cartasEnMano = new ArrayList<>();
        }
    }
}
