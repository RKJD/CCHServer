package com.xokundevs.cchmavenserver.bddconnectivity.model;
// Generated 26-may-2019 19:42:04 by Hibernate Tools 4.3.1


import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * CartaId generated by hbm2java
 */
@Embeddable
public class CartaId  implements java.io.Serializable {


     private int idCarta;
     private String emailUsuario;
     private String nombreBaraja;

    public CartaId() {
    }

    public CartaId(int idCarta, String emailUsuario, String nombreBaraja) {
       this.idCarta = idCarta;
       this.emailUsuario = emailUsuario;
       this.nombreBaraja = nombreBaraja;
    }
   


    @Column(name="idCarta", nullable=false)
    public int getIdCarta() {
        return this.idCarta;
    }
    
    public void setIdCarta(int idCarta) {
        this.idCarta = idCarta;
    }


    @Column(name="emailUsuario", nullable=false, length=50)
    public String getEmailUsuario() {
        return this.emailUsuario;
    }
    
    public void setEmailUsuario(String emailUsuario) {
        this.emailUsuario = emailUsuario;
    }


    @Column(name="nombreBaraja", nullable=false, length=30)
    public String getNombreBaraja() {
        return this.nombreBaraja;
    }
    
    public void setNombreBaraja(String nombreBaraja) {
        this.nombreBaraja = nombreBaraja;
    }


   public boolean equals(Object other) {
         if ( (this == other ) ) return true;
		 if ( (other == null ) ) return false;
		 if ( !(other instanceof CartaId) ) return false;
		 CartaId castOther = ( CartaId ) other; 
         
		 return (this.getIdCarta()==castOther.getIdCarta())
 && ( (this.getEmailUsuario()==castOther.getEmailUsuario()) || ( this.getEmailUsuario()!=null && castOther.getEmailUsuario()!=null && this.getEmailUsuario().equals(castOther.getEmailUsuario()) ) )
 && ( (this.getNombreBaraja()==castOther.getNombreBaraja()) || ( this.getNombreBaraja()!=null && castOther.getNombreBaraja()!=null && this.getNombreBaraja().equals(castOther.getNombreBaraja()) ) );
   }
   
   public int hashCode() {
         int result = 17;
         
         result = 37 * result + this.getIdCarta();
         result = 37 * result + ( getEmailUsuario() == null ? 0 : this.getEmailUsuario().hashCode() );
         result = 37 * result + ( getNombreBaraja() == null ? 0 : this.getNombreBaraja().hashCode() );
         return result;
   }   


}


