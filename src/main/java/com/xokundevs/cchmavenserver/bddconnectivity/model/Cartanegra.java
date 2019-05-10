package com.xokundevs.cchmavenserver.bddconnectivity.model;
// Generated 10-may-2019 17:18:01 by Hibernate Tools 4.3.1



/**
 * Cartanegra generated by hbm2java
 */
public class Cartanegra  implements java.io.Serializable {


     private CartanegraId id;
     private Carta carta;
     private Integer numeroEspacios;

    public Cartanegra() {
    }

	
    public Cartanegra(Carta carta) {
        this.carta = carta;
    }
    public Cartanegra(Carta carta, Integer numeroEspacios) {
       this.carta = carta;
       this.numeroEspacios = numeroEspacios;
    }
   
    public CartanegraId getId() {
        return this.id;
    }
    
    public void setId(CartanegraId id) {
        this.id = id;
    }
    public Carta getCarta() {
        return this.carta;
    }
    
    public void setCarta(Carta carta) {
        this.carta = carta;
    }
    public Integer getNumeroEspacios() {
        return this.numeroEspacios;
    }
    
    public void setNumeroEspacios(Integer numeroEspacios) {
        this.numeroEspacios = numeroEspacios;
    }




}


