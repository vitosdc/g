
import java.awt.*;
import java.awt.print.*;
import java.text.SimpleDateFormat;
import java.util.List;

public class InvoicePrinter implements Printable {
    private Invoice invoice;
    private SimpleDateFormat dateFormat;
    private Font titleFont;
    private Font normalFont;
    private Font boldFont;
    
    public InvoicePrinter(Invoice invoice) {
        this.invoice = invoice;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        this.titleFont = new Font("Arial", Font.BOLD, 16);
        this.normalFont = new Font("Arial", Font.PLAIN, 12);
        this.boldFont = new Font("Arial", Font.BOLD, 12);
    }
    
    public void print() {
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pageFormat = job.defaultPage();
        pageFormat.setOrientation(PageFormat.PORTRAIT);
        
        job.setPrintable(this, pageFormat);
        
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }
        
        Graphics2D g2d = (Graphics2D)graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        
        // Area stampabile
        double width = pageFormat.getImageableWidth();
        double height = pageFormat.getImageableHeight();
        
        int y = 20;
        int leftMargin = 50;
        
        // Intestazione
        g2d.setFont(titleFont);
        g2d.drawString("FATTURA", leftMargin, y);
        
        // Dati fattura
        y += 40;
        g2d.setFont(boldFont);
        g2d.drawString("Numero: " + invoice.getNumero(), leftMargin, y);
        g2d.drawString("Data: " + dateFormat.format(invoice.getData()), leftMargin + 300, y);
        
        // Dati cliente
        y += 40;
        g2d.drawString("Cliente:", leftMargin, y);
        y += 20;
        g2d.setFont(normalFont);
        g2d.drawString(invoice.getClienteNome(), leftMargin, y);
        
        // Tabella prodotti
        y += 40;
        int rowHeight = 20;
        int col1 = leftMargin;
        int col2 = leftMargin + 60;
        int col3 = leftMargin + 260;
        int col4 = leftMargin + 320;
        int col5 = leftMargin + 400;
        int col6 = leftMargin + 460;
        
        // Intestazione tabella
        g2d.setFont(boldFont);
        g2d.drawString("Codice", col1, y);
        g2d.drawString("Descrizione", col2, y);
        g2d.drawString("Q.tà", col3, y);
        g2d.drawString("Prezzo", col4, y);
        g2d.drawString("IVA%", col5, y);
        g2d.drawString("Totale", col6, y);
        
        // Linea separatrice
        y += 5;
        g2d.drawLine(leftMargin, y, (int)width - leftMargin, y);
        
        // Righe prodotti
        y += 15;
        g2d.setFont(normalFont);
        for (InvoiceItem item : invoice.getItems()) {
            g2d.drawString(item.getProdottoCodice(), col1, y);
            
            // Gestione testo lungo con troncamento
            String nome = item.getProdottoNome();
            if (nome.length() > 25) {
                nome = nome.substring(0, 22) + "...";
            }
            g2d.drawString(nome, col2, y);
            
            g2d.drawString(String.valueOf(item.getQuantita()), col3, y);
            g2d.drawString(String.format("%.2f €", item.getPrezzoUnitario()), col4, y);
            g2d.drawString(String.format("%.1f%%", item.getAliquotaIva()), col5, y);
            g2d.drawString(String.format("%.2f €", item.getTotale()), col6, y);
            y += rowHeight;
        }
        
        // Linea separatrice
        y += 5;
        g2d.drawLine(leftMargin, y, (int)width - leftMargin, y);
        
        // Totali
        y += 30;
        g2d.setFont(boldFont);
        g2d.drawString("Imponibile:", col5 - 80, y);
        g2d.drawString(String.format("%.2f €", invoice.getImponibile()), col6, y);
        
        y += 20;
        g2d.drawString("IVA:", col5 - 80, y);
        g2d.drawString(String.format("%.2f €", invoice.getIva()), col6, y);
        
        y += 20;
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("TOTALE:", col5 - 80, y);
        g2d.drawString(String.format("%.2f €", invoice.getTotale()), col6, y);
        
        // Note e condizioni di pagamento
        y += 60;
        g2d.setFont(normalFont);
        g2d.drawString("Note:", leftMargin, y);
        y += 20;
        g2d.drawString("Fattura emessa in regime di IVA ordinaria", leftMargin, y);
        
        y += 40;
        g2d.drawString("Condizioni di pagamento:", leftMargin, y);
        y += 20;
        g2d.drawString("Bonifico bancario a 30 giorni data fattura", leftMargin, y);
        
        // Riferimenti bancari
        y += 40;
        g2d.drawString("IBAN: IT60X0542811101000000123456", leftMargin, y);
        y += 20;
        g2d.drawString("BIC/SWIFT: BNPIITRRXXX", leftMargin, y);
        
        // Piè di pagina
        y = (int)height - 40;
        g2d.setFont(new Font("Arial", Font.ITALIC, 10));
        g2d.drawString("Documento generato elettronicamente", leftMargin, y);
        
        return PAGE_EXISTS;
    }
}
