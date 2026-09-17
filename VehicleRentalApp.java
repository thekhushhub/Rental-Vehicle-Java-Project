import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
 
public class VehicleRentalApp {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/vehicle_rental_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = ""; // Default XAMPP password is empty
 
    private static int loggedInUserId = 0;
    private static String loggedInUser = "";
    private static String loggedInRole = "";
 
    // Global refresh delegates for tab synchronization
    private static Runnable refreshRentalsTab = null;
    private static Runnable refreshDashboardTab = null;
    private static Runnable refreshVehiclesTab = null;
    private static Runnable refreshCustomersTab = null;
    private static Runnable refreshReturnsTab = null;
    private static Runnable refreshMaintenanceTab = null;
    private static Runnable refreshAlertsTab = null;
 
    // ==========================================
    // THEME / DESIGN CONSTANTS
    // ==========================================
    private static final Color BG = new Color(243, 246, 251);
    private static final Color SIDEBAR = new Color(15, 30, 60);
    private static final Color SIDEBAR_HOVER = new Color(30, 52, 92);
    private static final Color PRIMARY = new Color(37, 99, 235);
    private static final Color SUCCESS = new Color(22, 163, 74);
    private static final Color DANGER = new Color(220, 38, 38);
    private static final Color WARNING = new Color(234, 138, 12);
    private static final Color NEUTRAL = new Color(100, 116, 139);
    private static final Color PURPLE = new Color(124, 58, 237);
    private static final Color TEXT = new Color(30, 41, 59);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color BORDER_C = new Color(215, 222, 234);
    private static final Color ROW_ALT = new Color(247, 249, 253);
    private static final Color ROW_SELECT = new Color(219, 234, 254);
 
    private static Font f(int style, int size) {
        return new Font("SansSerif", style, size);
    }
 
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }
 
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            applyGlobalFonts();
            showLoginDialog();
        });
    }
 
    private static void applyGlobalFonts() {
        Font base = f(Font.PLAIN, 13);
        String[] keys = {"Label.font", "TextField.font", "PasswordField.font", "ComboBox.font", "CheckBox.font",
                "OptionPane.messageFont", "OptionPane.buttonFont", "Table.font", "ToolTip.font"};
        for (String k : keys) UIManager.put(k, base);
        UIManager.put("Button.font", f(Font.BOLD, 13));
        UIManager.put("TableHeader.font", f(Font.BOLD, 12));
    }
 
    // ==========================================
    // UI TOOLKIT (reusable styled components)
    // ==========================================
    static class RoundedButton extends JButton {
        private final Color base;
        private boolean hover = false;
 
        RoundedButton(String text, Color base) {
            super(text);
            this.base = base;
            setFont(f(Font.BOLD, 13));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 18, 9, 18));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }
 
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = base;
            if (!isEnabled()) c = new Color(190, 198, 210);
            else if (getModel().isPressed()) c = base.darker().darker();
            else if (hover) c = base.darker();
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }
 
    static class NavButton extends JButton {
        private boolean selectedNav = false;
        private boolean hover = false;
 
        NavButton(String text) {
            super(text);
            setHorizontalAlignment(SwingConstants.LEFT);
            setFont(f(Font.BOLD, 13));
            setForeground(new Color(203, 213, 225));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(11, 20, 11, 10));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }
 
        void setSelectedNav(boolean s) {
            selectedNav = s;
            setForeground(s ? Color.WHITE : new Color(203, 213, 225));
            repaint();
        }
 
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (selectedNav) {
                g2.setColor(PRIMARY);
                g2.fillRoundRect(10, 2, getWidth() - 20, getHeight() - 4, 12, 12);
            } else if (hover) {
                g2.setColor(SIDEBAR_HOVER);
                g2.fillRoundRect(10, 2, getWidth() - 20, getHeight() - 4, 12, 12);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
 
    static class RoundedPanel extends JPanel {
        RoundedPanel(LayoutManager lm) {
            super(lm);
            setOpaque(false);
            setBorder(new EmptyBorder(16, 18, 16, 18));
        }
 
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(15, 30, 60, 14));
            g2.fillRoundRect(1, 3, getWidth() - 2, getHeight() - 3, 18, 18);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 3, 18, 18);
            g2.setColor(BORDER_C);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 4, 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }
 
}
