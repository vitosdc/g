
// File: Invoice.java
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class Invoice {
    private int id;
    private String numero;
    private Date data;
    private int clienteId;
    private String clienteNome;
    private double imponibile;
    private double iva;
    private double totale;
    private String stato;
    private List<InvoiceItem> items;
    
    public Invoice(int id, String numero, Date data, int clienteId, String clienteNome, 
                  double imponibile, double iva, double totale, String stato) {
        this.id = id;
        this.numero = numero;
        this.data = data;
        this.clienteId = clienteId;
        this.clienteNome = clienteNome;
        this.imponibile = imponibile;
        this.iva = iva;
        this.totale = totale;
        this.stato = stato;
        this.items = new ArrayList<>();
    }
    
    // Getters
    public int getId() { return id; }
    public String getNumero() { return numero; }
    public Date getData() { return data; }
    public int getClienteId() { return clienteId; }
    public String getClienteNome() { return clienteNome; }
    public double getImponibile() { return imponibile; }
    public double getIva() { return iva; }
    public double getTotale() { return totale; }
    public String getStato() { return stato; }
    public List<InvoiceItem> getItems() { return items; }
    
    // Setters
    public void setId(int id) { this.id = id; }
    public void setNumero(String numero) { this.numero = numero; }
    public void setData(Date data) { this.data = data; }
    public void setClienteId(int clienteId) { this.clienteId = clienteId; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }
    public void setImponibile(double imponibile) { this.imponibile = imponibile; }
    public void setIva(double iva) { this.iva = iva; }
    public void setTotale(double totale) { this.totale = totale; }
    public void setStato(String stato) { this.stato = stato; }
    public void setItems(List<InvoiceItem> items) { this.items = items; }
}
