
// File: SupplierPriceList.java
import java.util.Date;

public class SupplierPriceList {
    private int id;
    private int fornitoreId;
    private int prodottoId;
    private String codiceProdottoFornitore;
    private double prezzo;
    private int quantitaMinima;
    private Date dataValiditaInizio;
    private Date dataValiditaFine;
    private String note;
    
    public SupplierPriceList(int id, int fornitoreId, int prodottoId, String codiceProdottoFornitore,
                            double prezzo, int quantitaMinima, Date dataValiditaInizio,
                            Date dataValiditaFine, String note) {
        this.id = id;
        this.fornitoreId = fornitoreId;
        this.prodottoId = prodottoId;
        this.codiceProdottoFornitore = codiceProdottoFornitore;
        this.prezzo = prezzo;
        this.quantitaMinima = quantitaMinima;
        this.dataValiditaInizio = dataValiditaInizio;
        this.dataValiditaFine = dataValiditaFine;
        this.note = note;
    }
    
    // Getters
    public int getId() { return id; }
    public int getFornitoreId() { return fornitoreId; }
    public int getProdottoId() { return prodottoId; }
    public String getCodiceProdottoFornitore() { return codiceProdottoFornitore; }
    public double getPrezzo() { return prezzo; }
    public int getQuantitaMinima() { return quantitaMinima; }
    public Date getDataValiditaInizio() { return dataValiditaInizio; }
    public Date getDataValiditaFine() { return dataValiditaFine; }
    public String getNote() { return note; }
    
    // Setters
    public void setId(int id) { this.id = id; }
    public void setFornitoreId(int fornitoreId) { this.fornitoreId = fornitoreId; }
    public void setProdottoId(int prodottoId) { this.prodottoId = prodottoId; }
    public void setCodiceProdottoFornitore(String codiceProdottoFornitore) { this.codiceProdottoFornitore = codiceProdottoFornitore; }
    public void setPrezzo(double prezzo) { this.prezzo = prezzo; }
    public void setQuantitaMinima(int quantitaMinima) { this.quantitaMinima = quantitaMinima; }
    public void setDataValiditaInizio(Date dataValiditaInizio) { this.dataValiditaInizio = dataValiditaInizio; }
    public void setDataValiditaFine(Date dataValiditaFine) { this.dataValiditaFine = dataValiditaFine; }
    public void setNote(String note) { this.note = note; }
}
