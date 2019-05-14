/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xokundevs.cchmavenserver.bddconnectivity.dao;

import com.xokundevs.cchmavenserver.bddconnectivity.model.Baraja;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Carta;
import com.xokundevs.cchmavenserver.bddconnectivity.util.HibernateUtil;
import java.util.Iterator;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 *
 * @author Antonio
 */
public class CartaDao {
    private static CartaDao INSTANCE;

    public static CartaDao getInstance() {
        if (INSTANCE == null) {
            synchronized (UsuarioDao.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CartaDao();
                }
            }
        }
        return INSTANCE;
    }
    public List<Carta> getCartas(String correo, String nombreBaraja) {
        List list = null;
        try {
            Session s = HibernateUtil.getSessionFactory().openSession();
            Query q = s.createQuery("from Carta carta where carta.id.emailUsuario = ? carta.id.nombreBaraja = ?");
            q.setString(0, correo);
            q.setString(1, nombreBaraja);
            list = q.list();
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public Baraja getCarta(String correo, String nombreBaraja, int idCarta) {
        Baraja baraja = null;
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            Query q = session.createQuery("from Carta carta where baraja.id.emailUsuario = ? and baraja.id.nombreBaraja = ? and baraja.id.idCarta = ?");
            q.setString(0, correo);
            q.setString(1, nombreBaraja);
            q.setInteger(2, idCarta);
            List<Baraja> list = q.list();
            baraja = (list.isEmpty()) ? null : list.get(0);
            session.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return baraja;
    }

    public boolean saveCarta(Carta carta) {
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

    public boolean updateCarta(Carta carta) {
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

    public boolean deleteCarta(Carta carta) {
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
