/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xokundevs.cchmavenserver.bddconnectivity.dao;

import com.xokundevs.cchmavenserver.bddconnectivity.model.Cartablanca;
import com.xokundevs.cchmavenserver.bddconnectivity.util.HibernateUtil;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 *
 * @author Antonio
 */
public class CartablancaDao {
    private static CartablancaDao INSTANCE;

    public static CartablancaDao getInstance() {
        if (INSTANCE == null) {
            synchronized (UsuarioDao.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CartablancaDao();
                }
            }
        }
        return INSTANCE;
    }
    public List<Cartablanca> getCartas(String correo, String nombreBaraja) {
        List list = null;
        try {
            Session s = HibernateUtil.getSessionFactory().openSession();
            Query q = s.createQuery("from Cartablanca c where c.id.emailUsuario = :correo and c.id.nombreBaraja = :nombreBaraja");
            q.setString("correo", correo);
            q.setString("nombreBaraja", nombreBaraja);
            list = q.list();
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public Cartablanca getCarta(String correo, String nombreBaraja, int idCarta) {
        Cartablanca carta = null;
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            Query q = session.createQuery("from Cartablanca c where c.id.emailUsuario = :correo and c.id.nombreBaraja = :nombreBaraja and c.id.idCarta = :idCarta");
            q.setString("correo", correo);
            q.setString("nombreBaraja", nombreBaraja);
            q.setInteger("idCarta", idCarta);
            List<Cartablanca> list = q.list();
            carta = (list.isEmpty()) ? null : list.get(0);
            session.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return carta;
    }
    
    public List<Cartablanca> getCartaFromOnlyUser(String correo){
        List<Cartablanca> cartas = null;
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            Query q = session.createQuery("from Cartablanca c where c.id.emailUsuario = :correo");
            q.setString("correo", correo);
            List<Cartablanca> list = q.list();
            cartas = list;
            session.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return cartas;
    }

    public boolean saveCarta(Cartablanca carta) {
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

    public boolean updateCarta(Cartablanca carta) {
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

    public boolean deleteCarta(Cartablanca carta) {
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
