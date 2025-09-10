import java.sql.*;
import java.util.*;
import java.util.List;

public class SmartSearchUtil {
    
    public static String prepareFuzzyQuery(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return "%";
        }
        
        // Remove extra spaces and convert to lowercase
        String cleaned = searchTerm.trim().toLowerCase()
            .replace("'", "''"); // SQL escape for security
        
        // Remove common special characters
        cleaned = cleaned.replaceAll("[()\\-.,]", " ");
        
        // Add wildcards between words for flexible search
        String[] words = cleaned.split("\\s+");
        StringBuilder fuzzyQuery = new StringBuilder("%");
        
        for (int i = 0; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                fuzzyQuery.append(words[i]);
                fuzzyQuery.append("%");
            }
        }
        
        return fuzzyQuery.toString();
    }
    
    public static List<Customer> searchCustomersImproved(String searchTerm) {
        List<Customer> results = new ArrayList<>();
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return results;
        }
        
        String fuzzyQuery = prepareFuzzyQuery(searchTerm);
        
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            
            // Multi-field search query with relevance ranking
            String query = """
                SELECT *, 
                    CASE 
                        WHEN LOWER(nome || ' ' || cognome) LIKE ? THEN 1
                        WHEN LOWER(cognome || ' ' || nome) LIKE ? THEN 2
                        WHEN LOWER(email) LIKE ? THEN 3
                        WHEN REPLACE(LOWER(telefono), ' ', '') LIKE ? THEN 4
                        ELSE 5
                    END as relevance_score
                FROM clienti 
                WHERE LOWER(nome || ' ' || cognome) LIKE ? 
                   OR LOWER(cognome || ' ' || nome) LIKE ?
                   OR LOWER(email) LIKE ?
                   OR REPLACE(LOWER(telefono), ' ', '') LIKE ?
                   OR LOWER(indirizzo) LIKE ?
                ORDER BY relevance_score, nome, cognome
                LIMIT 20
            """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                // Prepare search variants
                String exactMatch = searchTerm.toLowerCase() + "%";
                String phoneQuery = searchTerm.replaceAll("[\\s\\-().]", "") + "%";
                
                // Ranking parameters
                pstmt.setString(1, exactMatch);
                pstmt.setString(2, exactMatch);
                pstmt.setString(3, exactMatch);
                pstmt.setString(4, phoneQuery);
                
                // Search parameters
                pstmt.setString(5, fuzzyQuery);
                pstmt.setString(6, fuzzyQuery);
                pstmt.setString(7, fuzzyQuery);
                pstmt.setString(8, phoneQuery);
                pstmt.setString(9, fuzzyQuery);
                
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    Customer customer = new Customer(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getString("cognome"), 
                        rs.getString("email"),
                        rs.getString("telefono"),
                        rs.getString("indirizzo")
                    );
                    results.add(customer);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error searching customers: " + e.getMessage());
        }
        
        return results;
    }
    
    public static List<Product> searchProductsImproved(String searchTerm) {
        List<Product> results = new ArrayList<>();
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return results;
        }
        
        String fuzzyQuery = prepareFuzzyQuery(searchTerm);
        
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            
            String query = """
                SELECT *,
                    CASE 
                        WHEN LOWER(codice) LIKE ? THEN 1
                        WHEN LOWER(nome) LIKE ? THEN 2
                        WHEN LOWER(descrizione) LIKE ? THEN 3
                        ELSE 4
                    END as relevance_score
                FROM prodotti 
                WHERE LOWER(codice) LIKE ? 
                   OR LOWER(nome) LIKE ?
                   OR LOWER(descrizione) LIKE ?
                ORDER BY relevance_score, nome
                LIMIT 20
            """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                String exactMatch = searchTerm.toLowerCase() + "%";
                
                // Ranking
                pstmt.setString(1, exactMatch);
                pstmt.setString(2, exactMatch); 
                pstmt.setString(3, exactMatch);
                
                // Search
                pstmt.setString(4, fuzzyQuery);
                pstmt.setString(5, fuzzyQuery);
                pstmt.setString(6, fuzzyQuery);
                
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    Product product = new Product(
                        rs.getInt("id"),
                        rs.getString("codice"),
                        rs.getString("nome"),
                        rs.getString("descrizione"),
                        rs.getDouble("prezzo"),
                        rs.getInt("quantita")
                    );
                    results.add(product);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error searching products: " + e.getMessage());
        }
        
        return results;
    }
    
    public static List<Supplier> searchSuppliersImproved(String searchTerm) {
        List<Supplier> results = new ArrayList<>();
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return results;
        }
        
        String fuzzyQuery = prepareFuzzyQuery(searchTerm);
        
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            
            String query = """
                SELECT *,
                    CASE 
                        WHEN LOWER(ragione_sociale) LIKE ? THEN 1
                        WHEN LOWER(partita_iva) LIKE ? THEN 2
                        WHEN LOWER(email) LIKE ? THEN 3
                        ELSE 4
                    END as relevance_score
                FROM fornitori 
                WHERE LOWER(ragione_sociale) LIKE ? 
                   OR LOWER(partita_iva) LIKE ?
                   OR LOWER(email) LIKE ?
                   OR LOWER(telefono) LIKE ?
                ORDER BY relevance_score, ragione_sociale
                LIMIT 20
            """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                String exactMatch = searchTerm.toLowerCase() + "%";
                
                // Ranking
                pstmt.setString(1, exactMatch);
                pstmt.setString(2, exactMatch);
                pstmt.setString(3, exactMatch);
                
                // Search
                pstmt.setString(4, fuzzyQuery);
                pstmt.setString(5, fuzzyQuery);
                pstmt.setString(6, fuzzyQuery);
                pstmt.setString(7, fuzzyQuery);
                
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    Supplier supplier = new Supplier(
                        rs.getInt("id"),
                        rs.getString("ragione_sociale"),
                        rs.getString("partita_iva"),
                        rs.getString("codice_fiscale"),
                        rs.getString("indirizzo"),
                        rs.getString("telefono"),
                        rs.getString("email"),
                        rs.getString("pec"),
                        rs.getString("sito_web"),
                        rs.getString("note")
                    );
                    results.add(supplier);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error searching suppliers: " + e.getMessage());
        }
        
        return results;
    }
}