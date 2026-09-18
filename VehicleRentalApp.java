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
 
                new LineBorder(BORDER_C, 1, true), new EmptyBorder(7, 10, 7, 10));
        final javax.swing.border.Border focus = BorderFactory.createCompoundBorder(
                new LineBorder(PRIMARY, 2, true), new EmptyBorder(6, 9, 6, 9));
        t.setBorder(normal);
        t.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { t.setBorder(focus); }
            @Override public void focusLost(FocusEvent e) { t.setBorder(normal); }
        });
    }
 
    private static JTextField tf(String text) {
        JTextField t = new JTextField(text);
        styleField(t);
        return t;
    }
 
    private static JTextField tf(int cols) {
        JTextField t = new JTextField(cols);
        styleField(t);
        return t;
    }
 
    private static JPasswordField pf(String text) {
        JPasswordField p = new JPasswordField(text);
        styleField(p);
        return p;
    }
 
    private static JComboBox<String> styleCombo(JComboBox<String> cb) {
        cb.setFont(f(Font.PLAIN, 13));
        cb.setBackground(Color.WHITE);
        cb.setPreferredSize(new Dimension(150, 36));
        return cb;
    }
 
    private static JButton btn(String text, Color color) {
        return new RoundedButton(text, color);
    }
 
    /** Builds a neat grid form. comps = alternating label (String or Component) and field. */
    private static JPanel buildForm(int pairsPerRow, Object... comps) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 10);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.WEST;
        for (int i = 0; i + 1 < comps.length; i += 2) {
            int idx = i / 2;
            int col = (idx % pairsPerRow) * 2;
            int rowN = idx / pairsPerRow;
            Component label = comps[i] instanceof String ? lbl((String) comps[i]) : (Component) comps[i];
            gc.gridx = col; gc.gridy = rowN; gc.weightx = 0;
            p.add(label, gc);
            gc.gridx = col + 1; gc.weightx = 1;
            p.add((Component) comps[i + 1], gc);
        }
        return p;
    }
 
    private static JPanel actionRow(Component... buttons) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        p.setOpaque(false);
        for (Component c : buttons) p.add(c);
        return p;
    }
 
    /** White card containing a title, optional search row, a form and an action row. */
    private static RoundedPanel formCard(String title, JComponent search, JComponent form, JComponent actions) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10));
        JPanel head = new JPanel(new BorderLayout(0, 8));
        head.setOpaque(false);
        head.add(heading(title), BorderLayout.NORTH);
        if (search != null) head.add(search, BorderLayout.CENTER);
        card.add(head, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        if (actions != null) card.add(actions, BorderLayout.SOUTH);
        return card;
    }
 
    private static RoundedPanel tableCard(String title, JComponent content) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10));
        card.add(heading(title), BorderLayout.NORTH);
        card.add(scroll(content), BorderLayout.CENTER);
        return card;
    }
 
    private static JPanel newPage() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setOpaque(false);
        return p;
    }
 
    private static JPanel searchRow(String label, JTextField field, Component... buttons) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.add(lbl(label));
        row.add(field);
        for (Component b : buttons) row.add(b);
        return row;
    }
 
    private static void styleArea(JTextArea a, int size) {
        a.setFont(new Font("Monospaced", Font.PLAIN, size));
        a.setEditable(false);
        a.setForeground(TEXT);
        a.setBackground(new Color(251, 252, 254));
        a.setBorder(new EmptyBorder(12, 14, 12, 14));
    }
 
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
 
    // ==========================================
    // 1. LOGIN & AUTHENTICATION DIALOG
    // ==========================================
    private static void showLoginDialog() {
        JDialog loginDialog = new JDialog((Frame) null, "Vehicle Rental - Admin & Staff Login", true);
        loginDialog.setSize(780, 450);
        loginDialog.setResizable(false);
        loginDialog.setLocationRelativeTo(null);
        loginDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
 
        JPanel root = new JPanel(new GridLayout(1, 2));
 
        // ----- Left branding panel (gradient + drawn car) -----
        JPanel left = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(12, 24, 52), getWidth(), getHeight(), new Color(37, 99, 235)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillOval(-80, getHeight() - 200, 300, 300);
                g2.fillOval(getWidth() - 120, -60, 220, 220);
 
                int cx = getWidth() / 2 - 85;
                int cy = getHeight() - 135;
                g2.setColor(new Color(255, 255, 255, 235));
                g2.fillRoundRect(cx, cy + 22, 170, 42, 26, 26);
                g2.fillPolygon(new int[]{cx + 32, cx + 54, cx + 112, cx + 138}, new int[]{cy + 26, cy, cy, cy + 26}, 4);
                g2.setColor(new Color(37, 99, 235, 200));
                g2.fillPolygon(new int[]{cx + 42, cx + 58, cx + 80, cx + 80}, new int[]{cy + 24, cy + 6, cy + 6, cy + 24}, 4);
                g2.fillPolygon(new int[]{cx + 86, cx + 86, cx + 108, cx + 124}, new int[]{cy + 6, cy + 24, cy + 24, cy + 6}, 4);
                g2.setColor(new Color(12, 24, 52));
                g2.fillOval(cx + 22, cy + 46, 34, 34);
                g2.fillOval(cx + 114, cy + 46, 34, 34);
                g2.setColor(new Color(220, 228, 240));
                g2.fillOval(cx + 32, cy + 56, 14, 14);
                g2.fillOval(cx + 124, cy + 56, 14, 14);
                g2.dispose();
            }
        };
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(new EmptyBorder(40, 36, 0, 20));
        JLabel b1 = new JLabel("Vehicle Rental");
        b1.setFont(f(Font.BOLD, 30));
        b1.setForeground(Color.WHITE);
        JLabel b2 = new JLabel("Management System");
        b2.setFont(f(Font.PLAIN, 18));
        b2.setForeground(new Color(191, 219, 254));
        JLabel b3 = new JLabel("<html>Manage your fleet, customers, bookings,<br>returns and invoices in one place.</html>");
        b3.setFont(f(Font.PLAIN, 13));
        b3.setForeground(new Color(203, 213, 225));
        brand.add(b1);
        brand.add(Box.createVerticalStrut(2));
        brand.add(b2);
        brand.add(Box.createVerticalStrut(18));
        brand.add(b3);
        left.add(brand, BorderLayout.NORTH);
 
        // ----- Right login form -----
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(Color.WHITE);
        right.setBorder(new EmptyBorder(30, 40, 30, 40));
 
        JTextField txtUser = tf("admin");
        JPasswordField txtPass = pf("admin123");
        JCheckBox chkShow = new JCheckBox("Show password");
        chkShow.setOpaque(false);
        chkShow.setFont(f(Font.PLAIN, 12));
        chkShow.setForeground(MUTED);
        final char defaultEcho = txtPass.getEchoChar();
        chkShow.addActionListener(e -> txtPass.setEchoChar(chkShow.isSelected() ? (char) 0 : defaultEcho));
 
        JButton btnLogin = btn("Sign In", PRIMARY);
        JButton btnExit = btn("Exit", NEUTRAL);
        btnLogin.setFont(f(Font.BOLD, 14));
 
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(3, 0, 3, 0);
 
        JLabel welcome = new JLabel("Welcome Back");
        welcome.setFont(f(Font.BOLD, 24));
        welcome.setForeground(TEXT);
        JLabel sub = new JLabel("Sign in with your Admin / Staff account");
        sub.setFont(f(Font.PLAIN, 12));
        sub.setForeground(MUTED);
 
        gc.gridy = 0; right.add(welcome, gc);
        gc.gridy = 1; gc.insets = new Insets(0, 0, 18, 0); right.add(sub, gc);
        gc.insets = new Insets(3, 0, 3, 0);
        gc.gridy = 2; right.add(lbl("USERNAME"), gc);
        gc.gridy = 3; right.add(txtUser, gc);
        gc.gridy = 4; gc.insets = new Insets(10, 0, 3, 0); right.add(lbl("PASSWORD"), gc);
        gc.insets = new Insets(3, 0, 3, 0);
        gc.gridy = 5; right.add(txtPass, gc);
        gc.gridy = 6; right.add(chkShow, gc);
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 10, 0));
        btnRow.setOpaque(false);
        btnRow.add(btnLogin);
        btnRow.add(btnExit);
        gc.gridy = 7; gc.insets = new Insets(16, 0, 0, 0); right.add(btnRow, gc);
 
        root.add(left);
        root.add(right);
        loginDialog.setContentPane(root);
        loginDialog.getRootPane().setDefaultButton(btnLogin);
 
        btnLogin.addActionListener(e -> {
            String user = txtUser.getText().trim();
            String pass = new String(txtPass.getPassword()).trim();
 
            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(loginDialog, "Please enter both username and password.", "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
 
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT user_id, full_name, role FROM users WHERE username = ? AND password = ? AND status = 'Active'")) {
                stmt.setString(1, user);
                stmt.setString(2, pass);
                ResultSet rs = stmt.executeQuery();
 
                if (rs.next()) {
                    loggedInUserId = rs.getInt("user_id");
                    loggedInUser = rs.getString("full_name");
                    loggedInRole = rs.getString("role");
                    loginDialog.dispose();
                    showMainAdminPanel();
                } else {
                    JOptionPane.showMessageDialog(loginDialog, "Invalid username, password, or inactive account.", "Access Denied", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(loginDialog, "Database Connection Failed!\nEnsure MySQL is running in XAMPP.\n\nError: " + ex.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        });
 
        btnExit.addActionListener(e -> System.exit(0));
        loginDialog.setVisible(true);
    }
 
    // ==========================================
}
