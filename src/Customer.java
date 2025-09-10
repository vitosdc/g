public class Customer {
    private int id;
    private String nome;
    private String cognome;
    private String email;
    private String telefono;
    private String indirizzo;
    
    public Customer(int id, String nome, String cognome, String email, String telefono, String indirizzo) {
        this.id = id;
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
        this.telefono = telefono;
        this.indirizzo = indirizzo;
    }
    
    // Getters
    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getCognome() { return cognome; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public String getIndirizzo() { return indirizzo; }
    
    // Setters
    public void setId(int id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setCognome(String cognome) { this.cognome = cognome; }
    public void setEmail(String email) { this.email = email; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public void setIndirizzo(String indirizzo) { this.indirizzo = indirizzo; }
}