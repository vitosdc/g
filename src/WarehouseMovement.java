
// File: WarehouseMovement.java
import java.util.Date;

public class WarehouseMovement {
    private int id;
    private int prodottoId;
    private String prodottoNome;
    private Date data;
    private String tipo;
    private int quantita;
    private String causale;
    private String documentoNumero;
    private String documentoTipo;
    private String note;
    
    public WarehouseMovement(int id, int prodottoId, String prodottoNome, Date data, 
                            String tipo, int quantita, String causale, String documentoNumero, 
                            String documentoTipo, String note) {
        this.id = id;
        this.prodottoId = prodottoId;
        this.prodottoNome = prodottoNome;
        this.data = data;
        this.tipo = tipo;
        this.quantita = quantita;
        this.causale = causale;
        this.documentoNumero = documentoNumero;
        this.documentoTipo = documentoTipo;
        this.note = note;
    }
    
    // Getters e Setters standard
    public int getId() { return id; }
    public int getProdottoId() { return prodottoId; }
    public String getProdottoNome() { return prodottoNome; }
    public Date getData() { return data; }
    public String getTipo() { return tipo; }
    public int getQuantita() { return quantita; }
    public String getCausale() { return causale; }
    public String getDocumentoNumero() { return documentoNumero; }
    public String getDocumentoTipo() { return documentoTipo; }
    public String getNote() { return note; }
    
    public void setId(int id) { this.id = id; }
    public void setProdottoId(int prodottoId) { this.prodottoId = prodottoId; }
    public void setProdottoNome(String prodottoNome) { this.prodottoNome = prodottoNome; }
    public void setData(Date data) { this.data = data; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setQuantita(int quantita) { this.quantita = quantita; }
    public void setCausale(String causale) { this.causale = causale; }
    public void setDocumentoNumero(String documentoNumero) { this.documentoNumero = documentoNumero; }
    public void setDocumentoTipo(String documentoTipo) { this.documentoTipo = documentoTipo; }
    public void setNote(String note) { this.note = note; }
}
