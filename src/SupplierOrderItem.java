
// File: SupplierOrderItem.java
public class SupplierOrderItem {
    private int id;
    private int ordineId;
    private int prodottoId;
    private String prodottoNome;
    private String prodottoCodice;
    private int quantita;
    private double prezzoUnitario;
    private double totale;
    private String note;
    
    public SupplierOrderItem(int id, int ordineId, int prodottoId, String prodottoNome,
                            String prodottoCodice, int quantita, double prezzoUnitario,
                            double totale, String note) {
        this.id = id;
        this.ordineId = ordineId;
        this.prodottoId = prodottoId;
        this.prodottoNome = prodottoNome;
        this.prodottoCodice = prodottoCodice;
        this.quantita = quantita;
        this.prezzoUnitario = prezzoUnitario;
        this.totale = totale;
        this.note = note;
    }
    
    // Getters
    public int getId() { return id; }
    public int getOrdineId() { return ordineId; }
    public int getProdottoId() { return prodottoId; }
    public String getProdottoNome() { return prodottoNome; }
    public String getProdottoCodice() { return prodottoCodice; }
    public int getQuantita() { return quantita; }
    public double getPrezzoUnitario() { return prezzoUnitario; }
    public double getTotale() { return totale; }
    public String getNote() { return note; }
    
    // Setters
    public void setId(int id) { this.id = id; }
    public void setOrdineId(int ordineId) { this.ordineId = ordineId; }
    public void setProdottoId(int prodottoId) { this.prodottoId = prodottoId; }
    public void setProdottoNome(String prodottoNome) { this.prodottoNome = prodottoNome; }
    public void setProdottoCodice(String prodottoCodice) { this.prodottoCodice = prodottoCodice; }
    public void setQuantita(int quantita) { this.quantita = quantita; }
    public void setPrezzoUnitario(double prezzoUnitario) { this.prezzoUnitario = prezzoUnitario; }
    public void setTotale(double totale) { this.totale = totale; }
    public void setNote(String note) { this.note = note; }
}
