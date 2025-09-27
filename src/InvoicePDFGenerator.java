import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class InvoicePDFGenerator {
    private static final float MARGIN = 40f;
    private static final float LINE_HEIGHT = 12f;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    private static final String LAST_DIRECTORY_KEY = "last_pdf_directory";
    
    private Invoice invoice;
    private Customer customer;
    private CompanyData companyData;
    private List<InvoiceItem> invoiceItems;
    
    public InvoicePDFGenerator(Invoice invoice, Customer customer) {
        this.invoice = invoice;
        this.customer = customer;
        this.companyData = CompanyData.getInstance();
        this.invoiceItems = new ArrayList<>();
        loadInvoiceItems();
    }
    
    private void loadInvoiceItems() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String query = """
                SELECT df.*, COALESCE(p.codice, 'N/A') as prodotto_codice, 
                       COALESCE(p.nome, 'Prodotto N/D') as prodotto_nome,
                       COALESCE(p.descrizione, '') as prodotto_descrizione
                FROM dettagli_fattura df
                LEFT JOIN prodotti p ON df.prodotto_id = p.id
                WHERE df.fattura_id = ?
                ORDER BY df.id
            """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, invoice.getId());
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    InvoiceItem item = new InvoiceItem(
                        rs.getInt("id"),
                        rs.getInt("fattura_id"),
                        rs.getInt("prodotto_id"),
                        rs.getString("prodotto_nome"),
                        rs.getString("prodotto_codice"),
                        rs.getInt("quantita"),
                        rs.getDouble("prezzo_unitario"),
                        rs.getDouble("aliquota_iva"),
                        rs.getDouble("totale")
                    );
                    invoiceItems.add(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error loading invoice items: " + e.getMessage());
        }
    }
    
    public void generateAndSave(Component parent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Invoice PDF");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
        
        // Remember last directory
        String lastDirectory = getLastDirectory();
        if (lastDirectory != null) {
            fileChooser.setCurrentDirectory(new File(lastDirectory));
        }
        
        // Improved file name
        String invoiceNumber = invoice.getNumero();
        if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
            invoiceNumber = "INV_" + invoice.getId();
        }
        invoiceNumber = invoiceNumber.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        String customerName = "";
        if (customer.getCognome() != null && !customer.getCognome().trim().isEmpty()) {
            customerName = customer.getCognome().trim();
        } else if (customer.getNome() != null && !customer.getNome().trim().isEmpty()) {
            customerName = customer.getNome().trim();
        } else {
            customerName = "Customer_" + customer.getId();
        }
        customerName = customerName.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        String dateString = "";
        try {
            if (invoice.getData() != null) {
                dateString = DATE_FORMAT.format(invoice.getData());
            } else {
                dateString = DATE_FORMAT.format(new Date());
            }
            dateString = dateString.replace("/", "-");
        } catch (Exception e) {
            dateString = String.valueOf(System.currentTimeMillis() / 1000);
        }
        
        String defaultName = String.format("Invoice_%s_%s_%s.pdf", 
            invoiceNumber, customerName, dateString);
            
        fileChooser.setSelectedFile(new File(defaultName));
        
        if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".pdf")) {
                file = new File(file.getAbsolutePath() + ".pdf");
            }
            
            // Save last directory
            saveLastDirectory(file.getParent());
            
            try {
                generatePDF(file);
                JOptionPane.showMessageDialog(parent,
                    "Invoice PDF generated successfully!\nSaved to: " + file.getAbsolutePath(),
                    "PDF Generated",
                    JOptionPane.INFORMATION_MESSAGE);
                
                int choice = JOptionPane.showConfirmDialog(parent,
                    "Would you like to open the PDF file?",
                    "Open PDF",
                    JOptionPane.YES_NO_OPTION);
                
                if (choice == JOptionPane.YES_OPTION) {
                    try {
                        Desktop.getDesktop().open(file);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(parent,
                            "Cannot open PDF automatically. File saved to:\n" + file.getAbsolutePath(),
                            "Information",
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                }
                
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(parent,
                    "Error generating PDF: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void generatePDF(File outputFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float yPosition = page.getMediaBox().getHeight() - MARGIN;
                
                // Header con titolo FATTURA
                yPosition = drawHeader(contentStream, yPosition, page);
                yPosition -= 25f;
                
                // Informazioni azienda e cliente
                yPosition = drawCompanyAndCustomerInfo(contentStream, yPosition, page);
                yPosition -= 25f;
                
                // Dettagli fattura
                yPosition = drawInvoiceDetails(contentStream, yPosition, page);
                yPosition -= 25f;
                
                // Tabella prodotti migliorata
                yPosition = drawItemsTable(contentStream, yPosition, page);
                yPosition -= 20f;
                
                // Totali
                yPosition = drawTotals(contentStream, yPosition, page);
                
                // Footer con marchio WorkGenio
                drawFooter(contentStream, page);
            }
            
            document.save(outputFile);
        }
    }
    
    private float drawHeader(PDPageContentStream contentStream, float yPosition, PDPage page) throws IOException {
        // Invoice title centered and prominent
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 28f);
        String title = "INVOICE";
        // Approximate width estimation for title
        float titleWidth = title.length() * 28f * 0.7f;
        float titleX = (page.getMediaBox().getWidth() - titleWidth) / 2;
        contentStream.newLineAtOffset(titleX, yPosition);
        contentStream.showText(title);
        contentStream.endText();
        
        // Line under title
        float lineY = yPosition - 8f;
        contentStream.moveTo(MARGIN, lineY);
        contentStream.lineTo(page.getMediaBox().getWidth() - MARGIN, lineY);
        contentStream.setLineWidth(1.5f);
        contentStream.stroke();
        
        return lineY - 15f;
    }
    
    private float drawCompanyAndCustomerInfo(PDPageContentStream contentStream, float yPosition, PDPage page) throws IOException {
        float leftColumn = MARGIN;
        float rightColumn = page.getMediaBox().getWidth() / 2f + 10f;
        float startY = yPosition;
        
        // Box for company
        drawInfoBox(contentStream, leftColumn, yPosition - 130f, 250f, 130f, "FROM");
        
        // Company information
        yPosition -= 35f;
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11f);
        contentStream.newLineAtOffset(leftColumn + 5f, yPosition);
        contentStream.showText(safeTruncate(companyData.getCompanyName(), 35));
        contentStream.endText();
        
        yPosition -= 15f;
        String[] companyInfo = {
            companyData.getAddress(),
            companyData.getCity() + (companyData.getPostalCode().isEmpty() ? "" : " " + companyData.getPostalCode()),
            companyData.getCountry(),
            "VAT: " + companyData.getVatNumber(),
            "Tax Code: " + companyData.getTaxCode(),
            "Phone: " + companyData.getPhone(),
            "Email: " + companyData.getEmail()
        };
        
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9f);
        for (String line : companyInfo) {
            if (line != null && !line.trim().isEmpty() && 
                !line.equals("VAT: ") && !line.equals("Tax Code: ") && 
                !line.equals("Phone: ") && !line.equals("Email: ")) {
                contentStream.beginText();
                contentStream.newLineAtOffset(leftColumn + 5f, yPosition);
                contentStream.showText(safeTruncate(line, 40));
                contentStream.endText();
                yPosition -= 12f;
            }
        }
        
        // Reset position for customer
        yPosition = startY;
        
        // Box for customer
        drawInfoBox(contentStream, rightColumn, yPosition - 130f, 250f, 130f, "BILL TO");
        
        // Customer information
        yPosition -= 35f;
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11f);
        contentStream.newLineAtOffset(rightColumn + 5f, yPosition);
        String customerFullName = customer.getNome() + " " + customer.getCognome();
        contentStream.showText(safeTruncate(customerFullName, 35));
        contentStream.endText();
        
        yPosition -= 15f;
        String[] customerInfo = {
            customer.getEmail(),
            customer.getTelefono(),
            customer.getIndirizzo()
        };
        
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9f);
        for (String line : customerInfo) {
            if (line != null && !line.trim().isEmpty()) {
                contentStream.beginText();
                contentStream.newLineAtOffset(rightColumn + 5f, yPosition);
                contentStream.showText(safeTruncate(line, 40));
                contentStream.endText();
                yPosition -= 12f;
            }
        }
        
        return startY - 140f;
    }
    
    private void drawInfoBox(PDPageContentStream contentStream, float x, float y, float width, float height, String title) throws IOException {
        // Box principale
        contentStream.addRect(x, y, width, height);
        contentStream.setLineWidth(1f);
        contentStream.stroke();
        
        // Header del box
        contentStream.addRect(x, y + height - 20f, width, 20f);
        contentStream.setNonStrokingColor(240f/255f, 240f/255f, 240f/255f);
        contentStream.fill();
        contentStream.setNonStrokingColor(0f, 0f, 0f); // Reset colore
        
        // Bordatura header
        contentStream.addRect(x, y + height - 20f, width, 20f);
        contentStream.stroke();
        
        // Titolo
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10f);
        contentStream.newLineAtOffset(x + 5f, y + height - 15f);
        contentStream.showText(title);
        contentStream.endText();
    }
    
    private float drawInvoiceDetails(PDPageContentStream contentStream, float yPosition, PDPage page) throws IOException {
        float rightColumn = page.getMediaBox().getWidth() - MARGIN - 150f;
        
        // Box for invoice details
        drawInfoBox(contentStream, rightColumn, yPosition - 90f, 150f, 90f, "INVOICE DETAILS");
        
        yPosition -= 35f;
        String[][] details = {
            {"Number:", invoice.getNumero()},
            {"Date:", DATE_FORMAT.format(invoice.getData())},
            {"Status:", invoice.getStato()}
        };
        
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9f);
        for (String[] detail : details) {
            contentStream.beginText();
            contentStream.newLineAtOffset(rightColumn + 5f, yPosition);
            contentStream.showText(detail[0]);
            contentStream.endText();
            
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 9f);
            contentStream.newLineAtOffset(rightColumn + 50f, yPosition);
            contentStream.showText(safeTruncate(detail[1], 15));
            contentStream.endText();
            
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9f);
            yPosition -= 15f;
        }
        
        return yPosition - 20f;
    }
    
    private float drawItemsTable(PDPageContentStream contentStream, float yPosition, PDPage page) throws IOException {
        float tableWidth = page.getMediaBox().getWidth() - 2 * MARGIN;
        float[] columnWidths = {60f, 180f, 40f, 70f, 50f, 70f}; // Codice, Descrizione, Qta, Prezzo, IVA%, Totale
        float rowHeight = 18f;
        float headerHeight = 20f;
        
        // Table header with background
        contentStream.addRect(MARGIN, yPosition - headerHeight, tableWidth, headerHeight);
        contentStream.setNonStrokingColor(220f/255f, 220f/255f, 220f/255f);
        contentStream.fill();
        contentStream.setNonStrokingColor(0f, 0f, 0f); // Reset color
        
        // Header border
        contentStream.addRect(MARGIN, yPosition - headerHeight, tableWidth, headerHeight);
        contentStream.setLineWidth(1f);
        contentStream.stroke();
        
        // Header texts
        String[] headers = {"Code", "Description", "Qty", "Price €", "VAT%", "Total €"};
        drawTableRow(contentStream, MARGIN, yPosition, columnWidths, headerHeight, headers, true);
        yPosition -= headerHeight;
        
        // Product rows
        for (InvoiceItem item : invoiceItems) {
            String code = item.getProdottoCodice();
            String name = item.getProdottoNome();
            
            String[] rowData = {
                code != null ? safeTruncate(code, 10) : "N/A",
                name != null ? safeTruncate(name, 28) : "Product N/A",
                String.valueOf(item.getQuantita()),
                String.format("%.2f", item.getPrezzoUnitario()),
                String.format("%.1f", item.getAliquotaIva()),
                String.format("%.2f", item.getTotale())
            };
            
            // Alternate rows with color
            if ((invoiceItems.indexOf(item) % 2) == 1) {
                contentStream.addRect(MARGIN, yPosition - rowHeight, tableWidth, rowHeight);
                contentStream.setNonStrokingColor(248f/255f, 248f/255f, 248f/255f);
                contentStream.fill();
                contentStream.setNonStrokingColor(0f, 0f, 0f); // Reset color
            }
            
            drawTableRow(contentStream, MARGIN, yPosition, columnWidths, rowHeight, rowData, false);
            
            // Row border
            contentStream.addRect(MARGIN, yPosition - rowHeight, tableWidth, rowHeight);
            contentStream.setLineWidth(0.5f);
            contentStream.stroke();
            
            yPosition -= rowHeight;
        }
        
        return yPosition;
    }
    
    private void drawTableRow(PDPageContentStream contentStream, float x, float y, float[] columnWidths, 
                             float rowHeight, String[] data, boolean isHeader) throws IOException {
        
        if (isHeader) {
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 9f);
        } else {
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8f);
        }
        
        float currentX = x;
        for (int i = 0; i < data.length && i < columnWidths.length; i++) {
            String text = data[i] != null ? data[i] : "";
            
            if (!text.trim().isEmpty()) {
                contentStream.beginText();
                
                // Allineamento diverso per colonne numeriche
                float textX = currentX + 3f;
                if (i >= 2) { // Colonne numeriche (Qta, Prezzo, IVA%, Totale)
                    // Stima approssimativa della larghezza del testo per allineamento a destra
                    float fontSize = isHeader ? 9f : 8f;
                    float estimatedTextWidth = text.length() * fontSize * 0.6f;
                    textX = currentX + columnWidths[i] - estimatedTextWidth - 3f;
                }
                
                contentStream.newLineAtOffset(textX, y - rowHeight + 5f);
                contentStream.showText(text);
                contentStream.endText();
            }
            
            currentX += columnWidths[i];
        }
    }
    
    private float drawTotals(PDPageContentStream contentStream, float yPosition, PDPage page) throws IOException {
        float rightColumn = page.getMediaBox().getWidth() - MARGIN - 120f;
        
        // Totals box
        drawInfoBox(contentStream, rightColumn, yPosition - 80f, 120f, 80f, "TOTALS");
        
        yPosition -= 35f;
        String[][] totals = {
            {"Subtotal:", String.format("€ %.2f", invoice.getImponibile())},
            {"VAT:", String.format("€ %.2f", invoice.getIva())},
            {"TOTAL:", String.format("€ %.2f", invoice.getTotale())}
        };
        
        for (int i = 0; i < totals.length; i++) {
            boolean isTotal = i == totals.length - 1;
            
            if (isTotal) {
                // Line above total - moved even higher to avoid cutting text
                contentStream.moveTo(rightColumn + 5f, yPosition + 12f);
                contentStream.lineTo(rightColumn + 115f, yPosition + 12f);
                contentStream.setLineWidth(1f);
                contentStream.stroke();
            }
            
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 
                isTotal ? 11f : 9f);
            contentStream.newLineAtOffset(rightColumn + 5f, yPosition);
            contentStream.showText(totals[i][0]);
            contentStream.endText();
            
            contentStream.beginText();
            contentStream.newLineAtOffset(rightColumn + 60f, yPosition);
            contentStream.showText(totals[i][1]);
            contentStream.endText();
            
            yPosition -= isTotal ? 20f : 15f;
        }
        
        return yPosition;
    }
    
    private void drawFooter(PDPageContentStream contentStream, PDPage page) throws IOException {
        // Discrete footer with WorkGenio branding
        float footerY = 30f;
        
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 7f);
        contentStream.setNonStrokingColor(0.6f, 0.6f, 0.6f);
        
        String footerText = "Document generated with WorkGenio - Business Management System";
        // Approximate width estimation for text
        float textWidth = footerText.length() * 7f * 0.5f;
        float footerX = (page.getMediaBox().getWidth() - textWidth) / 2;
        
        contentStream.newLineAtOffset(footerX, footerY);
        contentStream.showText(footerText);
        contentStream.endText();
        
        contentStream.setNonStrokingColor(0f, 0f, 0f); // Reset color
    }
    
    private String safeTruncate(String text, int maxLength) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        text = text.trim();
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }
    
    // Metodo statico per uso facile
    public static void generateInvoicePDF(Invoice invoice, Customer customer, Component parent) {
        InvoicePDFGenerator generator = new InvoicePDFGenerator(invoice, customer);
        generator.generateAndSave(parent);
    }
}