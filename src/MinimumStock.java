public class MinimumStock {
    private int prodottoId;
    private String prodottoNome;
    private int quantitaMinima;
    private int quantitaRiordino;
    private int leadTimeGiorni;
    private Integer fornitorePreferito;
    private String fornitoreNome;
    private String note;
    
    public MinimumStock(int prodottoId, String prodottoNome, int quantitaMinima, 
                       int quantitaRiordino, int leadTimeGiorni, Integer fornitorePreferito, 
                       String fornitoreNome, String note) {
        this.prodottoId = prodottoId;
        this.prodottoNome = prodottoNome;
        this.quantitaMinima = quantitaMinima;
        this.quantitaRiordino = quantitaRiordino;
        this.leadTimeGiorni = leadTimeGiorni;
        this.fornitorePreferito = fornitorePreferito;
        this.fornitoreNome = fornitoreNome;
        this.note = note;
    }
    
    // Getters
    public int getProdottoId() { return prodottoId; }
    public String getProdottoNome() { return prodottoNome; }
    public int getQuantitaMinima() { return quantitaMinima; }
    public int getQuantitaRiordino() { return quantitaRiordino; }
    public int getLeadTimeGiorni() { return leadTimeGiorni; }
    public Integer getFornitorePreferito() { return fornitorePreferito; }
    public String getFornitoreNome() { return fornitoreNome; }
    public String getNote() { return note; }
    
    // Setters
    public void setProdottoId(int prodottoId) { this.prodottoId = prodottoId; }
    public void setProdottoNome(String prodottoNome) { this.prodottoNome = prodottoNome; }
    public void setQuantitaMinima(int quantitaMinima) { this.quantitaMinima = quantitaMinima; }
    public void setQuantitaRiordino(int quantitaRiordino) { this.quantitaRiordino = quantitaRiordino; }
    public void setLeadTimeGiorni(int leadTimeGiorni) { this.leadTimeGiorni = leadTimeGiorni; }
    public void setFornitorePreferito(Integer fornitorePreferito) { this.fornitorePreferito = fornitorePreferito; }
    public void setFornitoreNome(String fornitoreNome) { this.fornitoreNome = fornitoreNome; }
    public void setNote(String note) { this.note = note; }
}