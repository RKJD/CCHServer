/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xokundevs.cchmavenserver.bddconnectivity.dao;

import com.xokundevs.cchmavenserver.bddconnectivity.model.Usuario;
import com.xokundevs.cchmavenserver.bddconnectivity.util.HibernateUtil;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 *
 * @author Antonio
 */
public class UsuarioDao {
    private static UsuarioDao INSTANCE;
    
    public static UsuarioDao getInstance(){
        if(INSTANCE == null){
            synchronized(UsuarioDao.class){
                if(INSTANCE == null){
                    INSTANCE = new UsuarioDao();
                }
            }
        }
        return INSTANCE;
    }
    
    public List<Usuario> getUsuarios(){
        List list = null;
        try{
        Session s = HibernateUtil.getSessionFactory().openSession();
        Query q = s.createQuery("from Usuario u");
        list = q.list();
        s.close();
        } catch(Exception e){
            e.printStackTrace();
        }
        return list;
    }
    
    public Usuario getUsuario(String correo){
        Usuario user = null;
        try{
            Session session = HibernateUtil.getSessionFactory().openSession();
            Query q = session.createQuery("from Usuario user where user.email = ?");
            q.setString(0, correo);
            List<Usuario> list = q.list();
            user = (list.isEmpty())? null : list.get(0);
            session.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        
        return user; 
    }
}
