
// File: InvoiceItem.java
public class InvoiceItem {
    private int id;
    private int fatturaId;
    private int prodottoId;
    private String prodottoNome;
    private String prodottoCodice;
    private int quantita;
    private double prezzoUnitario;
    private double aliquotaIva;
    private double totale;
    
    public InvoiceItem(int id, int fatturaId, int prodottoId, String prodottoNome, 
                      String prodottoCodice, int quantita, double prezzoUnitario, 
                      double aliquotaIva, double totale) {
        this.id = id;
        this.fatturaId = fatturaId;
        this.prodottoId = prodottoId;
        this.prodottoNome = prodottoNome;
        this.prodottoCodice = prodottoCodice;
        this.quantita = quantita;
        this.prezzoUnitario = prezzoUnitario;
        this.aliquotaIva = aliquotaIva;
        this.totale = totale;
    }
    
    // Getters
    public int getId() { return id; }
    public int getFatturaId() { return fatturaId; }
    public int getProdottoId() { return prodottoId; }
    public String getProdottoNome() { return prodottoNome; }
    public String getProdottoCodice() { return prodottoCodice; }
    public int getQuantita() { return quantita; }
    public double getPrezzoUnitario() { return prezzoUnitario; }
    public double getAliquotaIva() { return aliquotaIva; }
    public double getTotale() { return totale; }
    
    // Setters
    public void setId(int id) { this.id = id; }
    public void setFatturaId(int fatturaId) { this.fatturaId = fatturaId; }
    public void setProdottoId(int prodottoId) { this.prodottoId = prodottoId; }
    public void setProdottoNome(String prodottoNome) { this.prodottoNome = prodottoNome; }
    public void setProdottoCodice(String prodottoCodice) { this.prodottoCodice = prodottoCodice; }
    public void setQuantita(int quantita) { this.quantita = quantita; }
    public void setPrezzoUnitario(double prezzoUnitario) { this.prezzoUnitario = prezzoUnitario; }
    public void setAliquotaIva(double aliquotaIva) { this.aliquotaIva = aliquotaIva; }
    public void setTotale(double totale) { this.totale = totale; }
}

