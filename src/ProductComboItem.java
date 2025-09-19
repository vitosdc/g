public class ProductComboItem {
    private Product product;
    
    public ProductComboItem(Product product) {
        this.product = product;
    }
    
    public Product getProduct() { 
        return product; 
    }
    
    @Override
    public String toString() {
        return String.format("%s - %s (€ %.2f)", 
            product.getCodice(), 
            product.getNome(), 
            product.getPrezzo());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ProductComboItem that = (ProductComboItem) obj;
        return product != null && product.getId() == that.product.getId();
    }
    
    @Override
    public int hashCode() {
        return product != null ? Integer.hashCode(product.getId()) : 0;
    }
}