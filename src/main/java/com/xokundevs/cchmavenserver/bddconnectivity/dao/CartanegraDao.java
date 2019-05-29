/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xokundevs.cchmavenserver.bddconnectivity.dao;

import com.xokundevs.cchmavenserver.bddconnectivity.model.Cartanegra;
import com.xokundevs.cchmavenserver.bddconnectivity.util.HibernateUtil;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 *
 * @author Antonio
 */
public class CartanegraDao {
    private static CartanegraDao INSTANCE;

    public static CartanegraDao getInstance() {
        if (INSTANCE == null) {
            synchronized (UsuarioDao.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CartanegraDao();
                }
            }
        }
        return INSTANCE;
    }
    public List<Cartanegra> getCartas(String correo, String nombreBaraja) {
        List list = null;
        try {
            Session s = HibernateUtil.getSessionFactory().openSession();
            Query q = s.createQuery("from Carta carta where carta.id.emailUsuario = :correo carta.id.nombreBaraja = :nombreBaraja");
            q.setString("correo", correo);
            q.setString("nombreBaraja", nombreBaraja);
            list = q.list();
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public Cartanegra getCarta(String correo, String nombreBaraja, int idCarta) {
        Cartanegra carta = null;
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            Query q = session.createQuery("from Carta carta where carta.id.emailUsuario = :correo and carta.id.nombreBaraja = :nombreBaraja and carta.id.idCarta = :idCarta");
            q.setString("correo", correo);
            q.setString("nombreBaraja", nombreBaraja);
            q.setInteger("idCarta", idCarta);
            List<Cartanegra> list = q.list();
            carta = (list.isEmpty()) ? null : list.get(0);
            session.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return carta;
    }
    
    public List<Cartanegra> getCartaFromOnlyUser(String correo){
        List<Cartanegra> cartas = null;
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            Query q = session.createQuery("from Cartanegra carta where carta.id.emailUsuario = :correo");
            q.setString("correo", correo);
            List<Cartanegra> list = q.list();
            cartas = (list.isEmpty()) ? null : list;
            session.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return cartas;
    }

    public boolean saveCarta(Cartanegra carta) {
        boolean result = false;
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            session.beginTransaction();
            session.save(carta);
            session.getTransaction().commit();
            session.close();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean updateCarta(Cartanegra carta) {
        boolean result = false;
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            session.beginTransaction();
            session.update(carta);
            session.getTransaction().commit();
            session.close();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean deleteCarta(Cartanegra carta) {
        boolean result = false;
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            session.beginTransaction();
            session.delete(carta);
            session.getTransaction().commit();
            session.close();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
