/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xokundevs.cchmavenserver.bddconnectivity.dao;

import com.xokundevs.cchmavenserver.bddconnectivity.model.Baraja;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Cartablanca;
import com.xokundevs.cchmavenserver.bddconnectivity.model.Cartanegra;
import com.xokundevs.cchmavenserver.bddconnectivity.util.HibernateUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.hibernate.HibernateException;
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
        List<Baraja> list = null;
        try {
            Session s = HibernateUtil.getSessionFactory().openSession();
            Query q = s.createQuery("from Baraja b where b.id.emailUsuario = :correo");
            q.setString("correo", correo);
            list = q.list();
            for (Baraja b : list) {
                b.getUsuario().getNombreUsuario();
                b.getCartasblancas().size();
                b.getCartasnegras().size();
            }
            s.close();
        } catch (HibernateException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Baraja getBaraja(String correo, String nombreBaraja) {
        Baraja baraja = null;
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            Query q = session.createQuery(
                    "from Baraja baraja where baraja.id.emailUsuario = :correo and baraja.id.nombreBaraja = :nombreBaraja");
            q.setString("correo", correo);
            q.setString("nombreBaraja", nombreBaraja);
            List<Baraja> list = q.list();
            baraja = (list.isEmpty()) ? null : list.get(0);
            if (baraja != null) {
                Set<Cartanegra> cartas = baraja.getCartasnegras();
                cartas.forEach((c) -> {
                    c.getTexto();
                });

                Set<Cartablanca> cartablancas = baraja.getCartasblancas();
                cartablancas.forEach((c) -> {
                    c.getTexto();
                });
            }
            session.close();
        } catch (Exception e) {
            baraja = null;
            e.printStackTrace();
        }

        return baraja;
    }

    public boolean saveBaraja(Baraja baraja, ArrayList<Cartablanca> cartasblancas,
            ArrayList<Cartanegra> cartasnegras) {
        boolean result = false;
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            session.beginTransaction();
            try {
                session.saveOrUpdate(baraja);
                for (Cartanegra carta : cartasnegras) {
                    session.saveOrUpdate(carta);
                }
                for (Cartablanca carta : cartasblancas) {
                    session.saveOrUpdate(carta);
                }

                //Get all cards of the deck greater than the id 
                Query query = session.createQuery("from Cartanegra c where c.id.emailUsuario = :correo and c.id.nombreBaraja = :nombreBaraja and c.id.idCarta > :idCarta");
                query.setParameter("correo", baraja.getId().getEmailUsuario());
                query.setParameter("nombreBaraja", baraja.getId().getNombreBaraja());
                query.setParameter("idCarta", cartasnegras.get(cartasnegras.size()-1).getId().getIdCarta());
                for(Cartanegra c : (List<Cartanegra>)query.list()){
                    session.delete(c);
                }

                query = session.createQuery("from Cartablanca c where c.id.emailUsuario = :correo and c.id.nombreBaraja = :nombreBaraja and c.id.idCarta > :idCarta");
                query.setParameter("correo", baraja.getId().getEmailUsuario());
                query.setParameter("nombreBaraja", baraja.getId().getNombreBaraja());
                query.setParameter("idCarta", cartasnegras.get(cartasnegras.size()-1).getId().getIdCarta());
                for(Cartablanca c : (List<Cartablanca>)query.list()){
                    session.delete(c);
                }
                session.getTransaction().commit();
            } catch (Exception e) {
                session.getTransaction().rollback();
            }
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
            for(Cartanegra cNegra : baraja.getCartasnegras()){
                session.delete(cNegra);
            }
            for(Cartablanca cBlanca : baraja.getCartasblancas()){
                session.delete(cBlanca);
            }
            session.delete(baraja);
            session.getTransaction().commit();
            session.close();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean deleteListBaraja(List<Baraja> listBaraja) {
        boolean result = false;
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            session.beginTransaction();
            for (Baraja baraja : listBaraja) {
                session.delete(baraja);
            }
            session.getTransaction().commit();
            session.close();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /*public Baraja getBarajaWithCards(String correo, String nombreBaraja) {
        Baraja baraja = null;
        try {
            Session session = HibernateUtil.getSessionFactory().openSession();
            Query q = session.createQuery(
                    "from Baraja baraja where baraja.id.emailUsuario = :correo and baraja.id.nombreBaraja = :nombreBaraja");
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
    }*/
}
