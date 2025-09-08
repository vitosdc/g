
class ProductComboItem {
    private Product product;
    
    public ProductComboItem(Product product) {
        this.product = product;
    }
    
    public Product getProduct() { return product; }
    
    @Override
    public String toString() {
        return String.format("%s (€ %.2f)", product.getNome(), product.getPrezzo());
    }
}