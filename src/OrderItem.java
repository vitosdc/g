
public class OrderItem {
    private int id;
    private int ordineId;
    private int prodottoId;
    private String prodottoNome;
    private int quantita;
    private double prezzoUnitario;
    
    public OrderItem(int id, int ordineId, int prodottoId, String prodottoNome, int quantita, double prezzoUnitario) {
        this.id = id;
        this.ordineId = ordineId;
        this.prodottoId = prodottoId;
        this.prodottoNome = prodottoNome;
        this.quantita = quantita;
        this.prezzoUnitario = prezzoUnitario;
    }
    
    // Getters
    public int getId() { return id; }
    public int getOrdineId() { return ordineId; }
    public int getProdottoId() { return prodottoId; }
    public String getProdottoNome() { return prodottoNome; }
    public int getQuantita() { return quantita; }
    public double getPrezzoUnitario() { return prezzoUnitario; }
    public double getTotale() { return quantita * prezzoUnitario; }
    
    // Setters
    public void setId(int id) { this.id = id; }
    public void setOrdineId(int ordineId) { this.ordineId = ordineId; }
    public void setProdottoId(int prodottoId) { this.prodottoId = prodottoId; }
    public void setProdottoNome(String prodottoNome) { this.prodottoNome = prodottoNome; }
    public void setQuantita(int quantita) { this.quantita = quantita; }
    public void setPrezzoUnitario(double prezzoUnitario) { this.prezzoUnitario = prezzoUnitario; }
}