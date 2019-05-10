package com.xocundevs.cchmavenserver;

import com.xokundevs.cchmavenserver.bddconnectivity.model.Usuario;
import com.xokundevs.cchmavenserver.bddconnectivity.dao.UsuarioDao;
import com.xokundevs.cchmavenserver.bddconnectivity.util.HibernateUtil;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.Hibernate;

public class Main {

    private static final int puerto = 55555;

    public static void main(String[] args) {
        HibernateUtil.getSessionFactory();
        List<Usuario> users = UsuarioDao.getInstance().getUsuarios();
        if(users.isEmpty()){
            System.out.println("\tEsta vacia");
        }
        else{
            for(Usuario user : users){
                System.out.println("\t>> "+user.getEmailUsuario());
            }
        }
        HibernateUtil.getSessionFactory().close();
        /*try {
            ServerSocket ssk = new ServerSocket(puerto);
            System.out.println("Escuchando");

            while (true) {
                Socket sk = ssk.accept();
                Servidor ser = new Servidor(sk);
                ser.start();
            }

        } catch (IOException ex) {
            System.out.println(ex);
        }*/
    }

    static class Servidor extends Thread {

        Socket sk;

        Servidor(Socket sk) {
            this.sk = sk;
        }

        @Override
        public void run() {

        }
    }
}
