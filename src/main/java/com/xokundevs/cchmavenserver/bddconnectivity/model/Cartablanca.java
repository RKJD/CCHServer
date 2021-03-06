package com.xokundevs.cchmavenserver.bddconnectivity.model;
// Generated 26-may-2019 19:42:04 by Hibernate Tools 4.3.1


import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * Cartablanca generated by hbm2java
 */
@Entity
@Table(name="cartablanca"
    ,catalog="cardshumanity"
)
public class Cartablanca  implements java.io.Serializable {


     private CartaId id;
     private Carta carta;

    public Cartablanca() {
    }

    public Cartablanca(Carta carta) {
       this.carta = carta;
    }
   
     @EmbeddedId

    
    @AttributeOverrides( {
        @AttributeOverride(name="idCarta", column=@Column(name="idCarta", nullable=false) ), 
        @AttributeOverride(name="emailUsuario", column=@Column(name="emailUsuario", nullable=false, length=50) ), 
        @AttributeOverride(name="nombreBaraja", column=@Column(name="nombreBaraja", nullable=false, length=30) ) } )
    public CartaId getId() {
        return this.id;
    }
    
    public void setId(CartaId id) {
        this.id = id;
    }

@OneToOne(fetch=FetchType.LAZY)@PrimaryKeyJoinColumn
    public Carta getCarta() {
        return this.carta;
    }
    
    public void setCarta(Carta carta) {
        this.carta = carta;
    }




}


