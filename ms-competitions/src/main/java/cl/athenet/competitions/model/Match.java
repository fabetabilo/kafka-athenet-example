package cl.athenet.competitions.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "encuentros")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String equipoLocal;
    private String equipoVisita;
    private int puntosLocal;
    private int puntosVisita;
    // PROGRAMADO, EN_CURSO o FINALIZADO
    private String estado;

    public Long getId() { return id; }
    public String getEquipoLocal() { return equipoLocal; }
    public String getEquipoVisita() { return equipoVisita; }
    public int getPuntosLocal() { return puntosLocal; }
    public void setPuntosLocal(int puntosLocal) { this.puntosLocal = puntosLocal; }
    public int getPuntosVisita() { return puntosVisita; }
    public void setPuntosVisita(int puntosVisita) { this.puntosVisita = puntosVisita; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
