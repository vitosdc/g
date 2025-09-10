import java.sql.*;
import java.util.*;
import java.util.List;

public class StockValidator {
    
    public static class ValidationResult {
        private boolean isValid;
        private String errorMessage;
        private String warningMessage;
        private List<String> suggestions;
        private int availableStock;
        private int reservedStock;
        
        public ValidationResult() {
            this.suggestions = new ArrayList<>();
            this.isValid = true;
        }
        
        // Getters and Setters
        public boolean isValid() { return isValid; }
        public void setValid(boolean valid) { this.isValid = valid; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { 
            this.errorMessage = errorMessage; 
            this.isValid = false;
        }
        
        public String getWarningMessage() { return warningMessage; }
        public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
        
        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
        public void addSuggestion(String suggestion) { this.suggestions.add(suggestion); }
        
        public int getAvailableStock() { return availableStock; }
        public void setAvailableStock(int availableStock) { this.availableStock = availableStock; }
        
        public int getReservedStock() { return reservedStock; }
        public void setReservedStock(int reservedStock) { this.reservedStock = reservedStock; }
    }
    
    public static ValidationResult validateStockOperation(int productId, int requestedQuantity, String operationType) {
        ValidationResult result = new ValidationResult();
        
        if (requestedQuantity <= 0) {
            result.setErrorMessage("Quantity must be greater than zero");
            return result;
        }
        
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            
            // Query to get complete product information
            String query = """
                SELECT p.quantita, p.nome, p.codice,
                       COALESCE(reserved.quantita_prenotata, 0) as quantita_prenotata,
                       COALESCE(sm.quantita_minima, 0) as scorta_minima,
                       COALESCE(sm.quantita_riordino, 0) as quantita_riordino,
                       f.ragione_sociale as fornitore_preferito
                FROM prodotti p
                LEFT JOIN (
                    SELECT prodotto_id, SUM(quantita) as quantita_prenotata
                    FROM dettagli_ordine d
                    JOIN ordini o ON d.ordine_id = o.id
                    WHERE o.stato IN ('New', 'In Progress', 'Confirmed')
                    GROUP BY prodotto_id
                ) reserved ON p.id = reserved.prodotto_id
                LEFT JOIN scorte_minime sm ON p.id = sm.prodotto_id
                LEFT JOIN fornitori f ON sm.fornitore_preferito_id = f.id
                WHERE p.id = ?
            """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, productId);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    int currentStock = rs.getInt("quantita");
                    int reservedStock = rs.getInt("quantita_prenotata");
                    int minStock = rs.getInt("scorta_minima");
                    int reorderQuantity = rs.getInt("quantita_riordino");
                    String productName = rs.getString("nome");
                    String productCode = rs.getString("codice");
                    String preferredSupplier = rs.getString("fornitore_preferito");
                    
                    int availableStock = Math.max(0, currentStock - reservedStock);
                    
                    result.setAvailableStock(availableStock);
                    result.setReservedStock(reservedStock);
                    
                    if ("OUTWARD".equals(operationType) || "SALE".equals(operationType)) {
                        validateOutwardMovement(result, requestedQuantity, currentStock, availableStock, 
                                               minStock, reorderQuantity, productName, productCode, preferredSupplier);
                    } else if ("INWARD".equals(operationType) || "PURCHASE".equals(operationType)) {
                        validateInwardMovement(result, requestedQuantity, currentStock, minStock, productName);
                    }
                    
                } else {
                    result.setErrorMessage("Product not found in system");
                }
            }
            
        } catch (SQLException e) {
            result.setErrorMessage("Error during validation: " + e.getMessage());
            System.err.println("Stock validation error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
    
    private static void validateOutwardMovement(ValidationResult result, int requestedQuantity, 
            int currentStock, int availableStock, int minStock, int reorderQuantity, 
            String productName, String productCode, String preferredSupplier) {
        
        // Check absolute availability
        if (requestedQuantity > currentStock) {
            result.setErrorMessage(String.format(
                "Insufficient stock for '%s' [%s]\n\n" +
                "Requested: %d pieces\n" +
                "Available: %d pieces\n" +
                "Reserved: %d pieces",
                productName, productCode, requestedQuantity, currentStock, result.getReservedStock()
            ));
            
            // Smart suggestions
            if (currentStock > 0) {
                result.addSuggestion("Confirm " + currentStock + " available pieces now");
            }
            
            if (result.getReservedStock() > 0) {
                result.addSuggestion("Release " + result.getReservedStock() + " pieces reserved for other orders");
            }
            
            if (reorderQuantity > 0 && preferredSupplier != null) {
                result.addSuggestion("Order " + reorderQuantity + " pieces from " + preferredSupplier);
            } else {
                result.addSuggestion("Check available suppliers for reorder");
            }
            
            result.addSuggestion("Check incoming orders from suppliers");
            
            return;
        }
        
        // Check reserved stock
        if (requestedQuantity > availableStock && result.getReservedStock() > 0) {
            int conflictQuantity = requestedQuantity - availableStock;
            result.setWarningMessage(String.format(
                "Warning: you will use %d pieces already reserved for other orders.\n" +
                "Free stock: %d | Reserved stock: %d",
                conflictQuantity, availableStock, result.getReservedStock()
            ));
            
            result.addSuggestion("Check pending orders before confirming");
            result.addSuggestion("Consider splitting order into multiple shipments");
        }
        
        // Check minimum stock
        int stockAfterOperation = currentStock - requestedQuantity;
        if (minStock > 0 && stockAfterOperation < minStock) {
            String warningMsg = String.format(
                "Stock will fall below minimum threshold (%d pieces).\n" +
                "Remaining stock: %d pieces",
                minStock, stockAfterOperation
            );
            
            if (result.getWarningMessage() == null) {
                result.setWarningMessage(warningMsg);
            } else {
                result.setWarningMessage(result.getWarningMessage() + "\n\n" + warningMsg);
            }
            
            if (reorderQuantity > 0) {
                result.addSuggestion("Schedule reorder of " + reorderQuantity + " pieces");
            }
            
            if (preferredSupplier != null) {
                result.addSuggestion("Contact " + preferredSupplier + " for urgent reorder");
            }
        }
        
        // Critical stock warning (< 10% of minimum stock)
        if (minStock > 0 && stockAfterOperation > 0 && stockAfterOperation < (minStock * 0.1)) {
            result.addSuggestion("⚠️ CRITICAL STOCK - Immediate reorder recommended");
        }
    }
    
    private static void validateInwardMovement(ValidationResult result, int requestedQuantity, 
            int currentStock, int minStock, String productName) {
        
        // For inward movements, less stringent validations
        if (requestedQuantity > 10000) { // Reasonable limit
            result.setWarningMessage("Very high quantity (" + requestedQuantity + "). Please verify entered data.");
        }
        
        // Notification if loading resolves stock issues
        if (minStock > 0 && currentStock < minStock && (currentStock + requestedQuantity) >= minStock) {
            result.addSuggestion("✓ This load will bring " + productName + " above minimum stock");
        }
        
        if (currentStock == 0) {
            result.addSuggestion("✓ Product will be back in stock");
        }
    }
    
    // Utility method to check if product exists
    public static boolean productExists(int productId) {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = "SELECT COUNT(*) FROM prodotti WHERE id = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, productId);
                ResultSet rs = pstmt.executeQuery();
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking product existence: " + e.getMessage());
            return false;
        }
    }
    
    // Get current stock for a product
    public static int getCurrentStock(int productId) {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = "SELECT quantita FROM prodotti WHERE id = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, productId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("quantita");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting current stock: " + e.getMessage());
        }
        return 0;
    }
}