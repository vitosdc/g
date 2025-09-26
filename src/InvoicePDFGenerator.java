import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class InvoicePDFGenerator {
    private static final float MARGIN = 50f;
    private static final float LINE_HEIGHT = 15f;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    
    private Invoice invoice;
    private Customer customer;
    private CompanyData companyData;
    
    public InvoicePDFGenerator(Invoice invoice, Customer customer) {
        this.invoice = invoice;
        this.customer = customer;
        this.companyData = CompanyData.getInstance();
    }
    
    public void generateAndSave(Component parent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Invoice PDF");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
        
        String defaultName = String.format("Invoice_%s_%s.pdf", 
            invoice.getNumero().replace("/", "-"),
            DATE_FORMAT.format(new Date()));
        fileChooser.setSelectedFile(new File(defaultName));
        
        if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".pdf")) {
                file = new File(file.getAbsolutePath() + ".pdf");
            }
            
            try {
                generatePDF(file);
                JOptionPane.showMessageDialog(parent,
                    "Invoice PDF generated successfully!\nSaved to: " + file.getAbsolutePath(),
                    "PDF Generated",
                    JOptionPane.INFORMATION_MESSAGE);
                
                // Ask if user wants to open the PDF
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
                
                // Draw header
                yPosition = drawHeader(contentStream, yPosition);
                yPosition -= 30f;
                
                // Draw company and customer info
                yPosition = drawCompanyAndCustomerInfo(contentStream, yPosition);
                yPosition -= 30f;
                
                // Draw invoice details
                yPosition = drawInvoiceDetails(contentStream, yPosition);
                yPosition -= 30f;
                
                // Draw items table
                yPosition = drawItemsTable(contentStream, yPosition);
                yPosition -= 20f;
                
                // Draw totals
                yPosition = drawTotals(contentStream, yPosition);
                yPosition -= 30f;
                
                // Draw footer
                drawFooter(contentStream, yPosition);
            }
            
            document.save(outputFile);
        }
    }
    
    private float drawHeader(PDPageContentStream contentStream, float yPosition) throws IOException {
        // Company logo (if available)
        float logoHeight = 0f;
        if (companyData.getLogoPath() != null && !companyData.getLogoPath().trim().isEmpty()) {
            try {
                File logoFile = new File(companyData.getLogoPath());
                if (logoFile.exists()) {
                    PDImageXObject logo = PDImageXObject.createFromFile(logoFile.getAbsolutePath(), 
                        contentStream.getDocument());
                    float logoWidth = 100f;
                    logoHeight = logoWidth * logo.getHeight() / logo.getWidth();
                    contentStream.drawImage(logo, MARGIN, yPosition - logoHeight, logoWidth, logoHeight);
                }
            } catch (Exception e) {
                // Logo loading failed, continue without it
            }
        }
        
        // Invoice title
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 24f);
        contentStream.newLineAtOffset(page.getMediaBox().getWidth() - MARGIN - 150f, yPosition - 30f);
        contentStream.showText("INVOICE");
        contentStream.endText();
        
        return yPosition - Math.max(logoHeight, 50f);
    }
    
    private float drawCompanyAndCustomerInfo(PDPageContentStream contentStream, float yPosition) throws IOException {
        float leftColumn = MARGIN;
        float rightColumn = page.getMediaBox().getWidth() / 2f + 20f;
        
        // Company info (left column)
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12f);
        contentStream.newLineAtOffset(leftColumn, yPosition);
        contentStream.showText("FROM:");
        contentStream.endText();
        
        yPosition -= LINE_HEIGHT + 5f;
        
        String[] companyInfo = {
            companyData.getCompanyName(),
            companyData.getAddress(),
            companyData.getCity() + (companyData.getPostalCode().isEmpty() ? "" : " " + companyData.getPostalCode()),
            companyData.getCountry(),
            "VAT: " + companyData.getVatNumber(),
            "Tax Code: " + companyData.getTaxCode(),
            "Phone: " + companyData.getPhone(),
            "Email: " + companyData.getEmail(),
            companyData.getWebsite()
        };
        
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10f);
        for (String line : companyInfo) {
            if (line != null && !line.trim().isEmpty() && !line.equals("VAT: ") && !line.equals("Tax Code: ") && !line.equals("Phone: ") && !line.equals("Email: ")) {
                contentStream.beginText();
                contentStream.newLineAtOffset(leftColumn, yPosition);
                contentStream.showText(line);
                contentStream.endText();
                yPosition -= LINE_HEIGHT;
            }
        }
        
        // Reset position for customer info
        yPosition += (companyInfo.length * LINE_HEIGHT);
        
        // Customer info (right column)
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12f);
        contentStream.newLineAtOffset(rightColumn, yPosition);
        contentStream.showText("BILL TO:");
        contentStream.endText();
        
        yPosition -= LINE_HEIGHT + 5f;
        
        String[] customerInfo = {
            customer.getNome() + " " + customer.getCognome(),
            customer.getEmail(),
            customer.getTelefono(),
            customer.getIndirizzo()
        };
        
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10f);
        for (String line : customerInfo) {
            if (line != null && !line.trim().isEmpty() && !line.equals(" ")) {
                contentStream.beginText();
                contentStream.newLineAtOffset(rightColumn, yPosition);
                contentStream.showText(line);
                contentStream.endText();
                yPosition -= LINE_HEIGHT;
            }
        }
        
        return yPosition - 20f;
    }
    
    private float drawInvoiceDetails(PDPageContentStream contentStream, float yPosition) throws IOException {
        float rightColumn = page.getMediaBox().getWidth() - MARGIN - 200f;
        
        // Invoice details table
        String[][] details = {
            {"Invoice Number:", invoice.getNumero()},
            {"Invoice Date:", DATE_FORMAT.format(invoice.getData())},
            {"Status:", invoice.getStato()},
            {"Due Date:", DATE_FORMAT.format(new Date(invoice.getData().getTime() + 30L * 24 * 60 * 60 * 1000))} // +30 days
        };
        
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10f);
        for (String[] detail : details) {
            contentStream.beginText();
            contentStream.newLineAtOffset(rightColumn, yPosition);
            contentStream.showText(detail[0]);
            contentStream.endText();
            
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10f);
            contentStream.newLineAtOffset(rightColumn + 80f, yPosition);
            contentStream.showText(detail[1]);
            contentStream.endText();
            
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10f);
            yPosition -= LINE_HEIGHT;
        }
        
        return yPosition;
    }
    
    private float drawItemsTable(PDPageContentStream contentStream, float yPosition) throws IOException {
        float tableWidth = page.getMediaBox().getWidth() - 2 * MARGIN;
        float[] columnWidths = {80f, 200f, 60f, 80f, 60f, 80f}; // Code, Description, Qty, Price, VAT%, Total
        float rowHeight = 20f;
        
        // Table header
        drawTableRow(contentStream, MARGIN, yPosition, columnWidths, rowHeight, 
            new String[]{"Code", "Description", "Qty", "Unit Price", "VAT %", "Total"}, 
            true, true);
        yPosition -= rowHeight;
        
        // Table rows
        List<InvoiceItem> items = invoice.getItems();
        for (InvoiceItem item : items) {
            String[] rowData = {
                item.getProdottoCodice(),
                truncateText(item.getProdottoNome(), 25),
                String.valueOf(item.getQuantita()),
                String.format("€ %.2f", item.getPrezzoUnitario()),
                String.format("%.1f%%", item.getAliquotaIva()),
                String.format("€ %.2f", item.getTotale())
            };
            
            drawTableRow(contentStream, MARGIN, yPosition, columnWidths, rowHeight, rowData, false, true);
            yPosition -= rowHeight;
        }
        
        // Table border
        drawTableBorder(contentStream, MARGIN, yPosition, tableWidth, (items.size() + 1) * rowHeight);
        
        return yPosition;
    }
    
    private void drawTableRow(PDPageContentStream contentStream, float x, float y, float[] columnWidths, 
                             float rowHeight, String[] data, boolean isHeader, boolean drawBorder) throws IOException {
        
        if (isHeader) {
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10f);
        } else {
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9f);
        }
        
        float currentX = x;
        for (int i = 0; i < data.length && i < columnWidths.length; i++) {
            contentStream.beginText();
            contentStream.newLineAtOffset(currentX + 5f, y - 15f);
            contentStream.showText(data[i]);
            contentStream.endText();
            
            if (drawBorder) {
                // Draw cell border
                contentStream.addRect(currentX, y - rowHeight, columnWidths[i], rowHeight);
                contentStream.stroke();
            }
            
            currentX += columnWidths[i];
        }
    }
    
    private void drawTableBorder(PDPageContentStream contentStream, float x, float y, float width, float height) throws IOException {
        contentStream.addRect(x, y, width, height);
        contentStream.stroke();
    }
    
    private float drawTotals(PDPageContentStream contentStream, float yPosition) throws IOException {
        float rightColumn = page.getMediaBox().getWidth() - MARGIN - 150f;
        
        // Totals
        String[][] totals = {
            {"Subtotal:", String.format("€ %.2f", invoice.getImponibile())},
            {"VAT:", String.format("€ %.2f", invoice.getIva())},
            {"TOTAL:", String.format("€ %.2f", invoice.getTotale())}
        };
        
        for (int i = 0; i < totals.length; i++) {
            boolean isTotal = i == totals.length - 1;
            
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), isTotal ? 14f : 11f);
            contentStream.newLineAtOffset(rightColumn, yPosition);
            contentStream.showText(totals[i][0]);
            contentStream.endText();
            
            contentStream.beginText();
            contentStream.newLineAtOffset(rightColumn + 70f, yPosition);
            contentStream.showText(totals[i][1]);
            contentStream.endText();
            
            if (isTotal) {
                // Draw line above total
                contentStream.moveTo(rightColumn, yPosition + 5f);
                contentStream.lineTo(rightColumn + 140f, yPosition + 5f);
                contentStream.stroke();
            }
            
            yPosition -= isTotal ? 25f : 18f;
        }
        
        return yPosition;
    }
    
    private void drawFooter(PDPageContentStream contentStream, float yPosition) throws IOException {
        // Payment terms
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10f);
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Payment Terms:");
        contentStream.endText();
        
        yPosition -= LINE_HEIGHT + 5f;
        
        String[] paymentInfo = {
            "Payment due within 30 days",
            "Bank transfer to:",
            "IBAN: IT60 X054 2811 1010 0000 0123 456",
            "BIC/SWIFT: BNPIITRRXXX",
            "Reference: " + invoice.getNumero()
        };
        
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9f);
        for (String line : paymentInfo) {
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText(line);
            contentStream.endText();
            yPosition -= LINE_HEIGHT;
        }
        
        // Footer note
        yPosition = 50f;
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8f);
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("This invoice was generated electronically and is valid without signature.");
        contentStream.endText();
        
        // Page number
        contentStream.beginText();
        contentStream.newLineAtOffset(page.getMediaBox().getWidth() - MARGIN - 50f, yPosition);
        contentStream.showText("Page 1 of 1");
        contentStream.endText();
    }
    
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }
    
    // Static method for easy access
    public static void generateInvoicePDF(Invoice invoice, Customer customer, Component parent) {
        InvoicePDFGenerator generator = new InvoicePDFGenerator(invoice, customer);
        generator.generateAndSave(parent);
    }
}