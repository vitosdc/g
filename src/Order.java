import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class Order {
    private int id;
    private int clienteId;
    private String clienteNome;
    private Date dataOrdine;
    private String stato;
    private double totale;
    private List<OrderItem> items;
    
    public Order(int id, int clienteId, String clienteNome, Date dataOrdine, String stato, double totale) {
        this.id = id;
        this.clienteId = clienteId;
        this.clienteNome = clienteNome;
        this.dataOrdine = dataOrdine;
        this.stato = stato;
        this.totale = totale;
        this.items = new ArrayList<>();
    }
    
    // Getters
    public int getId() { return id; }
    public int getClienteId() { return clienteId; }
    public String getClienteNome() { return clienteNome; }
    public Date getDataOrdine() { return dataOrdine; }
    public String getStato() { return stato; }
    public double getTotale() { return totale; }
    public List<OrderItem> getItems() { return items; }
    
    // Setters
    public void setId(int id) { this.id = id; }
    public void setClienteId(int clienteId) { this.clienteId = clienteId; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }
    public void setDataOrdine(Date dataOrdine) { this.dataOrdine = dataOrdine; }
    public void setStato(String stato) { this.stato = stato; }
    public void setTotale(double totale) { this.totale = totale; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}