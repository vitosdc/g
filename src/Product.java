// File: Product.java
public class Product {
    private int id;
    private String codice;
    private String nome;
    private String descrizione;
    private double prezzo;
    private int quantita;
    
    public Product(int id, String codice, String nome, String descrizione, double prezzo, int quantita) {
        this.id = id;
        this.codice = codice;
        this.nome = nome;
        this.descrizione = descrizione;
        this.prezzo = prezzo;
        this.quantita = quantita;
    }
    
    // Getters
    public int getId() { return id; }
    public String getCodice() { return codice; }
    public String getNome() { return nome; }
    public String getDescrizione() { return descrizione; }
    public double getPrezzo() { return prezzo; }
    public int getQuantita() { return quantita; }
    
    // Setters
    public void setId(int id) { this.id = id; }
    public void setCodice(String codice) { this.codice = codice; }
    public void setNome(String nome) { this.nome = nome; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
    public void setPrezzo(double prezzo) { this.prezzo = prezzo; }
    public void setQuantita(int quantita) { this.quantita = quantita; }
}