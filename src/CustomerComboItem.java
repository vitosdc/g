
public class CustomerComboItem {
    private int id;
    private String display;
    
    public CustomerComboItem(int id, String display) {
        this.id = id;
        this.display = display;
    }
    
    public int getId() { return id; }
    
    @Override
    public String toString() {
        return display;
    }
}
