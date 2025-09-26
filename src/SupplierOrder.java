
// File: SupplierOrder.java
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class SupplierOrder {
    private int id;
    private int fornitoreId;
    private String fornitoreNome;
    private String numero;
    private Date dataOrdine;
    private Date dataConsegnaPrevista;
    private String stato;
    private double totale;
    private String note;
    private List<SupplierOrderItem> items;
    
    public SupplierOrder(int id, int fornitoreId, String fornitoreNome, String numero,
                        Date dataOrdine, Date dataConsegnaPrevista, String stato, 
                        double totale, String note) {
        this.id = id;
        this.fornitoreId = fornitoreId;
        this.fornitoreNome = fornitoreNome;
        this.numero = numero;
        this.dataOrdine = dataOrdine;
        this.dataConsegnaPrevista = dataConsegnaPrevista;
        this.stato = stato;
        this.totale = totale;
        this.note = note;
        this.items = new ArrayList<>();
    }
    
    // Getters
    public int getId() { return id; }
    public int getFornitoreId() { return fornitoreId; }
    public String getFornitoreNome() { return fornitoreNome; }
    public String getNumero() { return numero; }
    public Date getDataOrdine() { return dataOrdine; }
    public Date getDataConsegnaPrevista() { return dataConsegnaPrevista; }
    public String getStato() { return stato; }
    public double getTotale() { return totale; }
    public String getNote() { return note; }
    public List<SupplierOrderItem> getItems() { return items; }
    
    // Setters
    public void setId(int id) { this.id = id; }
    public void setFornitoreId(int fornitoreId) { this.fornitoreId = fornitoreId; }
    public void setFornitoreNome(String fornitoreNome) { this.fornitoreNome = fornitoreNome; }
    public void setNumero(String numero) { this.numero = numero; }
    public void setDataOrdine(Date dataOrdine) { this.dataOrdine = dataOrdine; }
    public void setDataConsegnaPrevista(Date dataConsegnaPrevista) { this.dataConsegnaPrevista = dataConsegnaPrevista; }
    public void setStato(String stato) { this.stato = stato; }
    public void setTotale(double totale) { this.totale = totale; }
    public void setNote(String note) { this.note = note; }
    public void setItems(List<SupplierOrderItem> items) { this.items = items; }
}