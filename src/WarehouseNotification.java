
// File: WarehouseNotification.java
import java.util.Date;

public class WarehouseNotification {
    private int id;
    private int prodottoId;
    private String prodottoNome;
    private Date data;
    private String tipo;
    private String messaggio;
    private String stato;
    
    public WarehouseNotification(int id, int prodottoId, String prodottoNome, 
                                Date data, String tipo, String messaggio, String stato) {
        this.id = id;
        this.prodottoId = prodottoId;
        this.prodottoNome = prodottoNome;
        this.data = data;
        this.tipo = tipo;
        this.messaggio = messaggio;
        this.stato = stato;
    }
    
    // Getters e Setters
    public int getId() { return id; }
    public int getProdottoId() { return prodottoId; }
    public String getProdottoNome() { return prodottoNome; }
    public Date getData() { return data; }
    public String getTipo() { return tipo; }
    public String getMessaggio() { return messaggio; }
    public String getStato() { return stato; }
    
    public void setId(int id) { this.id = id; }
    public void setProdottoId(int prodottoId) { this.prodottoId = prodottoId; }
    public void setProdottoNome(String prodottoNome) { this.prodottoNome = prodottoNome; }
    public void setData(Date data) { this.data = data; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setMessaggio(String messaggio) { this.messaggio = messaggio; }
    public void setStato(String stato) { this.stato = stato; }
}

