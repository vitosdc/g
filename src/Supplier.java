
// File: Supplier.java
public class Supplier {
    private int id;
    private String ragioneSociale;
    private String partitaIva;
    private String codiceFiscale;
    private String indirizzo;
    private String telefono;
    private String email;
    private String pec;
    private String sitoWeb;
    private String note;
    
    public Supplier(int id, String ragioneSociale, String partitaIva, String codiceFiscale,
                   String indirizzo, String telefono, String email, String pec, 
                   String sitoWeb, String note) {
        this.id = id;
        this.ragioneSociale = ragioneSociale;
        this.partitaIva = partitaIva;
        this.codiceFiscale = codiceFiscale;
        this.indirizzo = indirizzo;
        this.telefono = telefono;
        this.email = email;
        this.pec = pec;
        this.sitoWeb = sitoWeb;
        this.note = note;
    }
    
    // Getters
    public int getId() { return id; }
    public String getRagioneSociale() { return ragioneSociale; }
    public String getPartitaIva() { return partitaIva; }
    public String getCodiceFiscale() { return codiceFiscale; }
    public String getIndirizzo() { return indirizzo; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }
    public String getPec() { return pec; }
    public String getSitoWeb() { return sitoWeb; }
    public String getNote() { return note; }
    
    // Setters
    public void setId(int id) { this.id = id; }
    public void setRagioneSociale(String ragioneSociale) { this.ragioneSociale = ragioneSociale; }
    public void setPartitaIva(String partitaIva) { this.partitaIva = partitaIva; }
    public void setCodiceFiscale(String codiceFiscale) { this.codiceFiscale = codiceFiscale; }
    public void setIndirizzo(String indirizzo) { this.indirizzo = indirizzo; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public void setEmail(String email) { this.email = email; }
    public void setPec(String pec) { this.pec = pec; }
    public void setSitoWeb(String sitoWeb) { this.sitoWeb = sitoWeb; }
    public void setNote(String note) { this.note = note; }
}
