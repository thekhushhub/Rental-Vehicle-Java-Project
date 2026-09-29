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
 
    static class StatCard extends JPanel {
        private final JLabel value = new JLabel();
        private final Color accent;
 
        StatCard(String title, String initial, Color accent) {
            this.accent = accent;
            setLayout(new BorderLayout(0, 6));
            setOpaque(false);
            setBorder(new EmptyBorder(18, 26, 18, 18));
            JLabel t = new JLabel(title.toUpperCase());
            t.setFont(f(Font.BOLD, 11));
            t.setForeground(MUTED);
            value.setText(initial);
            value.setFont(f(Font.BOLD, 28));
            value.setForeground(TEXT);
            add(t, BorderLayout.NORTH);
            add(value, BorderLayout.CENTER);
        }
 
        void setValue(String s) { value.setText(s); }
 
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(15, 30, 60, 14));
            g2.fillRoundRect(1, 3, getWidth() - 2, getHeight() - 3, 18, 18);
            g2.setColor(accent);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 3, 18, 18);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(7, 0, getWidth() - 8, getHeight() - 3, 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }
 
    // ---------- Table renderers ----------
    static class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            l.setBackground(new Color(30, 50, 90));
            l.setForeground(Color.WHITE);
            l.setFont(f(Font.BOLD, 12));
            l.setBorder(new EmptyBorder(0, 10, 0, 10));
            l.setHorizontalAlignment(SwingConstants.LEFT);
            l.setOpaque(true);
            return l;
        }
    }
 
    static class CellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            Object shown = v;
            if (v instanceof Double) shown = String.format("%,.2f", (Double) v);
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, shown, sel, false, row, col);
            l.setBorder(new EmptyBorder(0, 10, 0, 10));
            l.setHorizontalAlignment(SwingConstants.LEFT);
            l.setForeground(TEXT);
            l.setBackground(sel ? ROW_SELECT : (row % 2 == 0 ? Color.WHITE : ROW_ALT));
            return l;
        }
    }
 
    static Color[] statusColors(String s) {
        String x = s == null ? "" : s.toLowerCase();
        if (x.equals("available") || x.equals("active")) return new Color[]{new Color(220, 252, 231), new Color(21, 128, 61)};
        if (x.equals("rented")) return new Color[]{new Color(219, 234, 254), new Color(29, 78, 216)};
        if (x.equals("under maintenance")) return new Color[]{new Color(254, 243, 199), new Color(180, 83, 9)};
        if (x.equals("reserved")) return new Color[]{new Color(237, 233, 254), new Color(109, 40, 217)};
        if (x.equals("cancelled") || x.equals("inactive")) return new Color[]{new Color(254, 226, 226), new Color(185, 28, 28)};
        if (x.equals("returned")) return new Color[]{new Color(226, 232, 240), new Color(51, 65, 85)};
        return new Color[]{new Color(226, 232, 240), new Color(51, 65, 85)};
    }
 
    static class StatusRenderer extends DefaultTableCellRenderer {
        private Color pill = Color.LIGHT_GRAY;
 
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, false, row, col);
            Color[] c = statusColors(v == null ? "" : v.toString());
            pill = c[0];
            l.setForeground(c[1]);
            l.setFont(f(Font.BOLD, 12));
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setBackground(sel ? ROW_SELECT : (row % 2 == 0 ? Color.WHITE : ROW_ALT));
            return l;
        }
 
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            String text = getText() == null ? "" : getText();
            FontMetrics fm = g2.getFontMetrics(getFont());
            int tw = fm.stringWidth(text) + 26;
            int th = 22;
            int x = (getWidth() - tw) / 2;
            int y = (getHeight() - th) / 2;
            g2.setColor(pill);
            g2.fillRoundRect(x, y, tw, th, 22, 22);
            g2.setColor(getForeground());
            g2.setFont(getFont());
            g2.drawString(text, x + 13, y + (th + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }
 
    // ---------- Component factories ----------
    private static JTable makeTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(34);
        t.setFont(f(Font.PLAIN, 13));
        t.setForeground(TEXT);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(new Color(236, 240, 246));
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setSelectionBackground(ROW_SELECT);
        t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
 
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(0, 38));
        h.setDefaultRenderer(new HeaderRenderer());
 
        CellRenderer cr = new CellRenderer();
        t.setDefaultRenderer(Object.class, cr);
        t.setDefaultRenderer(Number.class, cr);
        t.setDefaultRenderer(Integer.class, cr);
        t.setDefaultRenderer(Double.class, cr);
        t.setDefaultRenderer(String.class, cr);
 
        for (int i = 0; i < model.getColumnCount(); i++) {
            if ("Status".equals(model.getColumnName(i))) {
                t.getColumnModel().getColumn(i).setCellRenderer(new StatusRenderer());
            }
        }
        return t;
    }
 
    private static JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(new LineBorder(BORDER_C, 1));
        sp.getViewport().setBackground(Color.WHITE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }
 
    private static JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(f(Font.BOLD, 12));
        l.setForeground(MUTED);
        return l;
    }
 
    private static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(f(Font.BOLD, 15));
        l.setForeground(TEXT);
        return l;
    }
 
    private static void styleField(JTextField t) {
        t.setFont(f(Font.PLAIN, 13));
        t.setForeground(TEXT);
        t.setBackground(Color.WHITE);
        final javax.swing.border.Border normal = BorderFactory.createCompoundBorder(
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
    // 2. MAIN APPLICATION FRAME & NAVIGATION
    // ==========================================
    private static void showMainAdminPanel() {
        JFrame frame = new JFrame("Vehicle Rental System | User: " + loggedInUser + " (" + loggedInRole + ")");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1280, 800);
        frame.setMinimumSize(new Dimension(1100, 700));
        frame.setLocationRelativeTo(null);
 
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
 
        // ----- Pages (created in the same order as before) -----
        Map<String, JPanel> pages = new LinkedHashMap<>();
        Map<String, String> subtitles = new HashMap<>();
        pages.put("Dashboard", createDashboardPanel());
        subtitles.put("Dashboard", "Live overview of your fleet, rentals and revenue");
        pages.put("Vehicles", createVehiclePanel());
        subtitles.put("Vehicles", "Add, update and track every vehicle in your fleet");
        pages.put("Categories", createCategoryPanel());
        subtitles.put("Categories", "Organise vehicles into categories");
        pages.put("Customers", createCustomerPanel());
        subtitles.put("Customers", "Register and manage your customers");
        pages.put("Rentals & Bookings", createRentalPanel());
        subtitles.put("Rentals & Bookings", "Create new bookings and manage existing ones");
        pages.put("Returns & Invoicing", createReturnPanel());
        subtitles.put("Returns & Invoicing", "Process vehicle returns and generate invoices");
        pages.put("Maintenance", createMaintenancePanel());
        subtitles.put("Maintenance", "Log services and repairs for your vehicles");
        pages.put("Alerts & Notifications", createAlertsPanel());
        subtitles.put("Alerts & Notifications", "Overdue returns, due-today returns and workshop status");
        pages.put("Reports", createReportsPanel());
        subtitles.put("Reports", "Financial, maintenance and fleet reports");
        if ("Admin".equalsIgnoreCase(loggedInRole)) {
            pages.put("Staff Management", createStaffPanel());
            subtitles.put("Staff Management", "Create staff accounts and control access");
            pages.put("System Settings", createSettingsPanel());
            subtitles.put("System Settings", "Company name, tax and late fee configuration");
        }
 
        CardLayout cards = new CardLayout();
        JPanel content = new JPanel(cards);
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(6, 22, 20, 22));
 
        // ----- Page header -----
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);
        header.setBorder(new EmptyBorder(18, 26, 6, 26));
        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel lblPageTitle = new JLabel("Dashboard");
        lblPageTitle.setFont(f(Font.BOLD, 24));
        lblPageTitle.setForeground(TEXT);
        JLabel lblPageSub = new JLabel(" ");
        lblPageSub.setFont(f(Font.PLAIN, 13));
        lblPageSub.setForeground(MUTED);
        titleBox.add(lblPageTitle);
        titleBox.add(lblPageSub);
        JLabel lblDate = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")));
        lblDate.setFont(f(Font.BOLD, 13));
        lblDate.setForeground(MUTED);
        header.add(titleBox, BorderLayout.WEST);
        header.add(lblDate, BorderLayout.EAST);
 
        // ----- Sidebar -----
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(SIDEBAR);
        sidebar.setPreferredSize(new Dimension(250, 0));
 
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(new EmptyBorder(24, 22, 18, 10));
        JLabel brandTitle = new JLabel("Vehicle Rental");
        brandTitle.setFont(f(Font.BOLD, 20));
        brandTitle.setForeground(Color.WHITE);
        JLabel brandSub = new JLabel("Management System");
        brandSub.setFont(f(Font.PLAIN, 12));
        brandSub.setForeground(new Color(147, 197, 253));
        brand.add(brandTitle);
        brand.add(brandSub);
        sidebar.add(brand, BorderLayout.NORTH);
 
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBackground(SIDEBAR);
        navPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
 
        List<NavButton> navButtons = new ArrayList<>();
 
        // Page switching + AUTO-SYNC DATA (same logic as the original tab listener)
        java.util.function.Consumer<String> showPage = title -> {
            cards.show(content, title);
            lblPageTitle.setText(title);
            lblPageSub.setText(subtitles.get(title));
            for (NavButton nb : navButtons) nb.setSelectedNav(nb.getText().equals(title));
 
            if ("Dashboard".equals(title) && refreshDashboardTab != null) refreshDashboardTab.run();
            if ("Vehicles".equals(title) && refreshVehiclesTab != null) refreshVehiclesTab.run();
            if ("Customers".equals(title) && refreshCustomersTab != null) refreshCustomersTab.run();
            if ("Rentals & Bookings".equals(title) && refreshRentalsTab != null) refreshRentalsTab.run();
            if ("Returns & Invoicing".equals(title) && refreshReturnsTab != null) refreshReturnsTab.run();
            if ("Maintenance".equals(title) && refreshMaintenanceTab != null) refreshMaintenanceTab.run();
            if ("Alerts & Notifications".equals(title) && refreshAlertsTab != null) refreshAlertsTab.run();
        };
 
        for (Map.Entry<String, JPanel> entry : pages.entrySet()) {
            String title = entry.getKey();
            content.add(entry.getValue(), title);
            NavButton nb = new NavButton(title);
            nb.addActionListener(e -> showPage.accept(title));
            navButtons.add(nb);
            navPanel.add(nb);
        }
 
        JScrollPane navScroll = new JScrollPane(navPanel);
        navScroll.setBorder(null);
        navScroll.getViewport().setBackground(SIDEBAR);
        navScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebar.add(navScroll, BorderLayout.CENTER);
 
        // ----- Sidebar footer: user info + quick actions -----
        JPanel userBox = new JPanel();
        userBox.setOpaque(false);
        userBox.setLayout(new BoxLayout(userBox, BoxLayout.Y_AXIS));
        userBox.setBorder(new EmptyBorder(14, 18, 18, 18));
        JLabel lblUserInfo = new JLabel("Logged in as: " + loggedInUser);
        lblUserInfo.setFont(f(Font.BOLD, 13));
        lblUserInfo.setForeground(Color.WHITE);
        JLabel lblRole = new JLabel("Role: " + loggedInRole);
        lblRole.setFont(f(Font.PLAIN, 12));
        lblRole.setForeground(new Color(147, 197, 253));
        JButton btnChangePass = btn("Change Password", PRIMARY);
        JButton btnLogout = btn("Logout", DANGER);
        btnChangePass.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogout.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnChangePass.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btnLogout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        lblUserInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblRole.setAlignmentX(Component.LEFT_ALIGNMENT);
        userBox.add(lblUserInfo);
        userBox.add(lblRole);
        userBox.add(Box.createVerticalStrut(12));
        userBox.add(btnChangePass);
        userBox.add(Box.createVerticalStrut(8));
        userBox.add(btnLogout);
        sidebar.add(userBox, BorderLayout.SOUTH);
 
        btnChangePass.addActionListener(e -> showChangePasswordDialog(frame));
        btnLogout.addActionListener(e -> {
            frame.dispose();
            showLoginDialog();
        });
 
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(BG);
        center.add(header, BorderLayout.NORTH);
        center.add(content, BorderLayout.CENTER);
 
        root.add(sidebar, BorderLayout.WEST);
        root.add(center, BorderLayout.CENTER);
        frame.setContentPane(root);
 
        showPage.accept("Dashboard");
        frame.setVisible(true);
    }
 
    // ==========================================
    // 3. CHANGE PASSWORD DIALOG
    // ==========================================
    private static void showChangePasswordDialog(JFrame parent) {
        JDialog dlg = new JDialog(parent, "Change Password", true);
        dlg.setSize(420, 300);
        dlg.setResizable(false);
        dlg.setLocationRelativeTo(parent);
 
        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(22, 28, 22, 28));
 
        JLabel title = new JLabel("Change Password");
        title.setFont(f(Font.BOLD, 20));
        title.setForeground(TEXT);
 
        JPasswordField txtOld = pf("");
        JPasswordField txtNew = pf("");
        JButton btnSubmit = btn("Update Password", PRIMARY);
        JButton btnCancel = btn("Cancel", NEUTRAL);
 
        // label stacked above each field for a cleaner look
        JPanel stacked = new JPanel(new GridLayout(4, 1, 0, 4));
        stacked.setOpaque(false);
        stacked.add(lbl("CURRENT PASSWORD"));
        stacked.add(txtOld);
        stacked.add(lbl("NEW PASSWORD"));
        stacked.add(txtNew);
 
        body.add(title, BorderLayout.NORTH);
        body.add(stacked, BorderLayout.CENTER);
        body.add(actionRow(btnCancel, btnSubmit), BorderLayout.SOUTH);
        dlg.setContentPane(body);
        dlg.getRootPane().setDefaultButton(btnSubmit);
 
        btnCancel.addActionListener(e -> dlg.dispose());
        btnSubmit.addActionListener(e -> {
            String oldP = new String(txtOld.getPassword()).trim();
            String newP = new String(txtNew.getPassword()).trim();
 
            if (newP.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "New password cannot be blank.");
                return;
            }
 
            try (Connection conn = getConnection();
                 PreparedStatement check = conn.prepareStatement("SELECT user_id FROM users WHERE user_id = ? AND password = ?");
                 PreparedStatement upd = conn.prepareStatement("UPDATE users SET password = ? WHERE user_id = ?")) {
                check.setInt(1, loggedInUserId);
                check.setString(2, oldP);
                ResultSet rs = check.executeQuery();
 
                if (rs.next()) {
                    upd.setString(1, newP);
                    upd.setInt(2, loggedInUserId);
                    upd.executeUpdate();
                    JOptionPane.showMessageDialog(dlg, "Password updated successfully!");
                    dlg.dispose();
                } else {
                    JOptionPane.showMessageDialog(dlg, "Incorrect current password.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dlg, "Error updating password: " + ex.getMessage());
            }
        });
 
        dlg.setVisible(true);
    }
 
    // ==========================================
    // 4. DASHBOARD PANEL
    // ==========================================
    private static JPanel createDashboardPanel() {
        JPanel panel = newPage();
 
        StatCard lblTotalVehicles = new StatCard("Total Fleet", "0", PRIMARY);
        StatCard lblAvailableVehicles = new StatCard("Available Fleet", "0", SUCCESS);
        StatCard lblRentedVehicles = new StatCard("Active Rentals", "0", PURPLE);
        StatCard lblTotalCustomers = new StatCard("Registered Customers", "0", new Color(6, 148, 162));
        StatCard lblMaintenance = new StatCard("Under Maintenance", "0", WARNING);
        StatCard lblRevenue = new StatCard("Total Net Revenue", "₹0.00", DANGER);
 
        JPanel statsGrid = new JPanel(new GridLayout(2, 3, 16, 16));
        statsGrid.setOpaque(false);
        statsGrid.setPreferredSize(new Dimension(0, 250));
        statsGrid.add(lblTotalVehicles);
        statsGrid.add(lblAvailableVehicles);
        statsGrid.add(lblRentedVehicles);
        statsGrid.add(lblTotalCustomers);
        statsGrid.add(lblMaintenance);
        statsGrid.add(lblRevenue);
 
        JButton btnRefresh = btn("Refresh Dashboard Metrics", PRIMARY);
 
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel welcome = new JLabel("Welcome back, " + loggedInUser + "!");
        welcome.setFont(f(Font.BOLD, 16));
        welcome.setForeground(TEXT);
        top.add(welcome, BorderLayout.WEST);
        top.add(btnRefresh, BorderLayout.EAST);
 
        DefaultTableModel recentModel = new DefaultTableModel(new String[]{"Rental ID", "Customer", "Vehicle", "Pickup", "Return Due", "Amount (₹)", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable recentTable = makeTable(recentModel);
 
        refreshDashboardTab = () -> {
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM vehicles");
                if (rs.next()) lblTotalVehicles.setValue(String.valueOf(rs.getInt(1)));
 
                rs = stmt.executeQuery("SELECT COUNT(*) FROM vehicles WHERE status = 'Available'");
                if (rs.next()) lblAvailableVehicles.setValue(String.valueOf(rs.getInt(1)));
 
                rs = stmt.executeQuery("SELECT COUNT(*) FROM rentals WHERE status = 'Active'");
                if (rs.next()) lblRentedVehicles.setValue(String.valueOf(rs.getInt(1)));
 
                rs = stmt.executeQuery("SELECT COUNT(*) FROM customers");
                if (rs.next()) lblTotalCustomers.setValue(String.valueOf(rs.getInt(1)));
 
                rs = stmt.executeQuery("SELECT COUNT(*) FROM vehicles WHERE status = 'Under Maintenance'");
                if (rs.next()) lblMaintenance.setValue(String.valueOf(rs.getInt(1)));
 
                rs = stmt.executeQuery("SELECT IFNULL(SUM(final_amount), 0) FROM rentals WHERE status = 'Returned'");
                if (rs.next()) lblRevenue.setValue("₹" + String.format("%,.2f", rs.getDouble(1)));
 
                recentModel.setRowCount(0);
                rs = stmt.executeQuery("SELECT r.rental_id, c.full_name, CONCAT(v.make, ' ', v.model), r.pickup_date, r.return_date, r.final_amount, r.status FROM rentals r JOIN customers c ON r.customer_id = c.customer_id JOIN vehicles v ON r.vehicle_id = v.vehicle_id ORDER BY r.rental_id DESC LIMIT 8");
                while (rs.next()) {
                    recentModel.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getDouble(6), rs.getString(7)});
                }
            } catch (SQLException ex) {
                System.err.println("Dashboard refresh error: " + ex.getMessage());
            }
        };
 
        btnRefresh.addActionListener(e -> refreshDashboardTab.run());
 
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(statsGrid, BorderLayout.NORTH);
        body.add(tableCard("Recent Rentals", recentTable), BorderLayout.CENTER);
 
        panel.add(top, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        refreshDashboardTab.run();
 
        return panel;
    }
 
    // ==========================================
    // 5. VEHICLE MANAGEMENT PANEL
    // ==========================================
    private static JPanel createVehiclePanel() {
        JPanel panel = newPage();
 
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Reg Number", "Make", "Model", "Year", "Category", "Rate/Day (₹)", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = makeTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        table.getColumnModel().getColumn(0).setMaxWidth(70);
 
        JTextField txtSearch = tf(18);
        JButton btnSearch = btn("Filter / Search", PRIMARY);
        JButton btnClearSearch = btn("Clear Filter", NEUTRAL);
        JPanel searchPanel = searchRow("Search Vehicles:", txtSearch, btnSearch, btnClearSearch);
 
        Runnable doSearch = () -> {
            String text = txtSearch.getText().trim();
            if (text.isEmpty()) sorter.setRowFilter(null);
            else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        };
        btnSearch.addActionListener(e -> doSearch.run());
        txtSearch.addActionListener(e -> doSearch.run());
 
        btnClearSearch.addActionListener(e -> {
            txtSearch.setText("");
            sorter.setRowFilter(null);
        });
 
        JTextField txtReg = tf("");
        JTextField txtMake = tf("");
        JTextField txtModel = tf("");
        JTextField txtYear = tf("");
        JTextField txtRate = tf("");
        JComboBox<String> cmbCategory = styleCombo(new JComboBox<>());
        JComboBox<String> cmbStatus = styleCombo(new JComboBox<>(new String[]{"Available", "Rented", "Under Maintenance", "Reserved"}));
 
        Runnable loadCategories = () -> {
            cmbCategory.removeAllItems();
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT category_id, category_name FROM categories")) {
                while (rs.next()) {
                    cmbCategory.addItem(rs.getInt(1) + " - " + rs.getString(2));
                }
            } catch (SQLException ignored) {}
        };
 
        refreshVehiclesTab = () -> {
            model.setRowCount(0);
            loadCategories.run();
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT v.vehicle_id, v.reg_number, v.make, v.model, v.year, c.category_name, v.daily_rate, v.status FROM vehicles v LEFT JOIN categories c ON v.category_id = c.category_id ORDER BY v.vehicle_id DESC")) {
                while (rs.next()) {
                    model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5), rs.getString(6), rs.getDouble(7), rs.getString(8)});
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(panel, "Error loading vehicles: " + ex.getMessage());
            }
        };
 
        JPanel form = buildForm(3,
                "Reg Number", txtReg, "Make / Brand", txtMake, "Model Name", txtModel,
                "Year", txtYear, "Daily Rate (₹)", txtRate, "Category", cmbCategory,
                "Status", cmbStatus);
 
        JButton btnAdd = btn("Add Vehicle", SUCCESS);
        JButton btnUpdate = btn("Update Selected Vehicle", PRIMARY);
        JButton btnDelete = btn("Delete Vehicle", DANGER);
        JButton btnRefresh = btn("Refresh", NEUTRAL);
        JButton btnClearForm = btn("Clear Form", WARNING);
 
        table.getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                int modelRow = table.convertRowIndexToModel(selectedRow);
                txtReg.setText(model.getValueAt(modelRow, 1).toString());
                txtMake.setText(model.getValueAt(modelRow, 2).toString());
                txtModel.setText(model.getValueAt(modelRow, 3).toString());
                txtYear.setText(model.getValueAt(modelRow, 4).toString());
                txtRate.setText(model.getValueAt(modelRow, 6).toString());
                cmbStatus.setSelectedItem(model.getValueAt(modelRow, 7).toString());
                Object catName = model.getValueAt(modelRow, 5);
                if (catName != null) {
                    for (int i = 0; i < cmbCategory.getItemCount(); i++) {
                        if (cmbCategory.getItemAt(i).endsWith(" - " + catName)) {
                            cmbCategory.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            }
        });
 
        btnClearForm.addActionListener(e -> {
            table.clearSelection();
            txtReg.setText(""); txtMake.setText(""); txtModel.setText("");
            txtYear.setText(""); txtRate.setText("");
        });
 
        btnAdd.addActionListener(e -> {
            try {
                if (txtReg.getText().trim().isEmpty() || txtMake.getText().trim().isEmpty() || cmbCategory.getSelectedItem() == null) {
                    JOptionPane.showMessageDialog(panel, "Please fill in all vehicle details.");
                    return;
                }
                int catId = Integer.parseInt(cmbCategory.getSelectedItem().toString().split(" - ")[0]);
 
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement("INSERT INTO vehicles (reg_number, make, model, year, category_id, daily_rate, status) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    stmt.setString(1, txtReg.getText().trim().toUpperCase());
                    stmt.setString(2, txtMake.getText().trim());
                    stmt.setString(3, txtModel.getText().trim());
                    stmt.setInt(4, Integer.parseInt(txtYear.getText().trim()));
                    stmt.setInt(5, catId);
                    stmt.setDouble(6, Double.parseDouble(txtRate.getText().trim()));
                    stmt.setString(7, cmbStatus.getSelectedItem().toString());
                    stmt.executeUpdate();
 
                    JOptionPane.showMessageDialog(panel, "Vehicle added successfully!");
                    refreshVehiclesTab.run();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Error adding vehicle: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
 
        btnUpdate.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(panel, "Select a vehicle from the table to update.");
                return;
            }
            int modelRow = table.convertRowIndexToModel(selectedRow);
            int vehicleId = (int) model.getValueAt(modelRow, 0);
 
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement("UPDATE vehicles SET reg_number = ?, make = ?, model = ?, year = ?, daily_rate = ?, status = ? WHERE vehicle_id = ?")) {
                stmt.setString(1, txtReg.getText().trim().toUpperCase());
                stmt.setString(2, txtMake.getText().trim());
                stmt.setString(3, txtModel.getText().trim());
                stmt.setInt(4, Integer.parseInt(txtYear.getText().trim()));
                stmt.setDouble(5, Double.parseDouble(txtRate.getText().trim()));
                stmt.setString(6, cmbStatus.getSelectedItem().toString());
                stmt.setInt(7, vehicleId);
                stmt.executeUpdate();
 
                JOptionPane.showMessageDialog(panel, "Vehicle updated successfully!");
                refreshVehiclesTab.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Error updating vehicle: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
 
        btnDelete.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(panel, "Select a vehicle to delete.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(panel, "Are you sure you want to delete this vehicle?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
 
            int modelRow = table.convertRowIndexToModel(selectedRow);
            int vehicleId = (int) model.getValueAt(modelRow, 0);
 
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM vehicles WHERE vehicle_id = ?")) {
                stmt.setInt(1, vehicleId);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(panel, "Vehicle deleted.");
                refreshVehiclesTab.run();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(panel, "Cannot delete vehicle if it has associated rental records: " + ex.getMessage());
            }
        });
 
        btnRefresh.addActionListener(e -> refreshVehiclesTab.run());
 
        panel.add(formCard("Vehicle Details", searchPanel, form,
                actionRow(btnRefresh, btnClearForm, btnAdd, btnUpdate, btnDelete)), BorderLayout.NORTH);
        panel.add(tableCard("Fleet Inventory", table), BorderLayout.CENTER);
 
        refreshVehiclesTab.run();
        return panel;
    }
 
    // ==========================================
    // 6. CATEGORY MANAGEMENT PANEL
    // ==========================================
    private static JPanel createCategoryPanel() {
        JPanel panel = newPage();
 
        DefaultTableModel model = new DefaultTableModel(new String[]{"Category ID", "Category Name", "Description"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = makeTable(model);
 
        JTextField txtName = tf("");
        JTextField txtDesc = tf("");
        JPanel form = buildForm(2, "Category Name", txtName, "Description", txtDesc);
 
        JButton btnAdd = btn("Add Category", SUCCESS);
        JButton btnDelete = btn("Delete Category", DANGER);
 
        Runnable loadData = () -> {
            model.setRowCount(0);
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM categories")) {
                while (rs.next()) {
                    model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3)});
                }
            } catch (SQLException ignored) {}
        };
 
        btnAdd.addActionListener(e -> {
            String name = txtName.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Category name cannot be empty.");
                return;
            }
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO categories (category_name, description) VALUES (?, ?)")) {
                stmt.setString(1, name);
                stmt.setString(2, txtDesc.getText().trim());
                stmt.executeUpdate();
                loadData.run();
                txtName.setText(""); txtDesc.setText("");
                JOptionPane.showMessageDialog(panel, "Category added.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(panel, "Error: " + ex.getMessage());
            }
        });
 
        btnDelete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(panel, "Select a category to delete.");
                return;
            }
            int catId = (int) model.getValueAt(row, 0);
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM categories WHERE category_id = ?")) {
                stmt.setInt(1, catId);
                stmt.executeUpdate();
                loadData.run();
                JOptionPane.showMessageDialog(panel, "Category deleted.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(panel, "Error: " + ex.getMessage());
            }
        });
 
        panel.add(formCard("Category Details", null, form, actionRow(btnAdd, btnDelete)), BorderLayout.NORTH);
        panel.add(tableCard("All Categories", table), BorderLayout.CENTER);
        loadData.run();
        return panel;
    }
 
    // ==========================================
    // 7. CUSTOMER MANAGEMENT PANEL
    // ==========================================
    private static JPanel createCustomerPanel() {
}
