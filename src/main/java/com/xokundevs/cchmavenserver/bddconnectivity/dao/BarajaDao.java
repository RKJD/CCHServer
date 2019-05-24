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
public class BarajaDao {

    private static BarajaDao INSTANCE;

    public static BarajaDao getInstance() {
        if (INSTANCE == null) {
            synchronized (UsuarioDao.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BarajaDao();
                }
            }
        }
        return INSTANCE;
    }

    public List<Baraja> getBarajas(String correo) {
        List list = null;
        try {
            Session s = HibernateUtil.getSessionFactory().openSession();
            Query q = s.createQuery("from Baraja b where b.id.emailUsuario = :correo");
            q.setString("correo", correo);
            list = q.list();
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public Baraja getBaraja(String correo, String nombreBaraja) {
        Baraja baraja = null;
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            Query q = session.createQuery("from Baraja baraja where baraja.id.emailUsuario = :correo and baraja.id.nombreBaraja = :nombreBaraja");
            q.setString("correo" , correo);
            q.setString("nombreBaraja", nombreBaraja);
            List<Baraja> list = q.list();
            baraja = (list.isEmpty()) ? null : list.get(0);
            session.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return baraja;
    }

    public boolean saveBaraja(Baraja baraja) {
        boolean result = false;
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            session.beginTransaction();
            session.save(baraja);
            session.getTransaction().commit();
            session.close();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean updateBaraja(Baraja baraja) {
        boolean result = false;
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            session.beginTransaction();
            session.update(baraja);
            session.getTransaction().commit();
            session.close();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean deleteBaraja(Baraja baraja) {
        boolean result = false;
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            session.beginTransaction();
            session.delete(baraja);
            session.getTransaction().commit();
            session.close();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public Baraja getBarajaWithCards(String correo, String nombreBaraja) {
        Baraja baraja = null;
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            Query q = session.createQuery("from Baraja baraja where baraja.id.emailUsuario = :correo and baraja.id.nombreBaraja = :nombreBaraja");
            q.setString("correo", correo);
            q.setString("nombreBaraja", nombreBaraja);
            List<Baraja> list = q.list();
            baraja = (list.isEmpty()) ? null : list.get(0);
            if (baraja != null) {
                Iterator iterator = baraja.getCartas().iterator();
                while (iterator.hasNext()) {
                    ((Carta) iterator.next()).getTexto();
                }
            }
            session.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return baraja;
    }
}
