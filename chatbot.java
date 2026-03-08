import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Stack;
import java.util.Random;

public class chatbot extends JFrame {

    // --- AESTHETIC COLOR PALETTE ---
    private final Color BACKGROUND_COLOR = new Color(28, 30, 43);    // Deep Space Blue
    private final Color SIDEBAR_COLOR = new Color(35, 37, 54);      // Lighter Navy
    private final Color ACCENT_COLOR = new Color(138, 122, 255);    // Soft Lavender
    private final Color BOT_BUBBLE = new Color(48, 52, 77);         // Muted Blue-Grey
    private final Color TEXT_WHITE = new Color(245, 245, 250);      // Off-white
    private final Color SUCCESS_GREEN = new Color(46, 204, 113);

    // --- BACKEND DATA STRUCTURES ---
    static class Book {
        int id;
        String title, author;
        boolean borrowed;
        Book next; // Linked List Pointer

        Book(int id, String title, String author, Book next) {
            this.id = id;
            this.title = title;
            this.author = author;
            this.borrowed = false;
            this.next = next;
        }
    }

    enum ActionType { BORROW, RETURN }
    
    static class Action {
        ActionType type;
        int bookId;
        Action(ActionType type, int bookId) {
            this.type = type;
            this.bookId = bookId;
        }
    }

    private static Book head = null;
    private static Stack<Action> undoStack = new Stack<>();

    // --- UI COMPONENTS ---
    private JPanel chatPanel;
    private JTextField inputField;
    private JScrollPane scrollPane;

    public chatbot() {
        // Window Setup
        setTitle("Library Bot");
        setSize(500, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(BACKGROUND_COLOR);
        setLayout(new BorderLayout());

        // 1. Chat History Area
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(BACKGROUND_COLOR);
        chatPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        scrollPane = new JScrollPane(chatPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0)); // Sleek hidden scrollbar

        // 2. Button Dashboard
        JPanel dashboard = new JPanel(new GridLayout(1, 3, 10, 0));
        dashboard.setBackground(SIDEBAR_COLOR);
        dashboard.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));

        JButton listBtn = createAestheticButton("LIST ALL", ACCENT_COLOR);
        JButton searchBtn = createAestheticButton("SEARCH", new Color(100, 100, 150));
        JButton undoBtn = createAestheticButton("UNDO", new Color(231, 76, 60));

        dashboard.add(listBtn);
        dashboard.add(searchBtn);
        dashboard.add(undoBtn);

        // 3. Control Panel (Input + Action Buttons)
        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.setBackground(SIDEBAR_COLOR);

        JPanel inputArea = new JPanel(new BorderLayout(15, 0));
        inputArea.setBackground(SIDEBAR_COLOR);
        inputArea.setBorder(BorderFactory.createEmptyBorder(5, 20, 25, 20));

        inputField = new JTextField();
        inputField.setBackground(BOT_BUBBLE);
        inputField.setForeground(TEXT_WHITE);
        inputField.setCaretColor(TEXT_WHITE);
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BOT_BUBBLE, 1),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JPanel actionBtnWrapper = new JPanel(new GridLayout(1, 2, 8, 0));
        actionBtnWrapper.setBackground(SIDEBAR_COLOR);
        JButton borrowBtn = createAestheticButton("BORROW", SUCCESS_GREEN);
        JButton returnBtn = createAestheticButton("RETURN", ACCENT_COLOR);
        actionBtnWrapper.add(borrowBtn);
        actionBtnWrapper.add(returnBtn);

        inputArea.add(inputField, BorderLayout.CENTER);
        inputArea.add(actionBtnWrapper, BorderLayout.EAST);

        bottomContainer.add(dashboard, BorderLayout.NORTH);
        bottomContainer.add(inputArea, BorderLayout.SOUTH);

        add(scrollPane, BorderLayout.CENTER);
        add(bottomContainer, BorderLayout.SOUTH);

        // --- BUTTON EVENTS ---
        listBtn.addActionListener(e -> handleCommand("list"));
        undoBtn.addActionListener(e -> handleCommand("undo"));
        searchBtn.addActionListener(e -> {
            addMessage("🤖 Enter a title or keyword in the box and press Enter.", false);
            inputField.requestFocus();
        });

        borrowBtn.addActionListener(e -> handleCommand("borrow " + inputField.getText()));
        returnBtn.addActionListener(e -> handleCommand("return " + inputField.getText()));
        inputField.addActionListener(e -> handleCommand("search " + inputField.getText()));

        // Start Up
        seedData();
        addMessage("✨ <b>Welcome!</b> I've loaded 115 books.<br>Use the buttons or type an ID to begin.", false);
    }

    private JButton createAestheticButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        return btn;
    }

    private void addMessage(String text, boolean isUser) {
        JPanel wrapper = new JPanel(new FlowLayout(isUser ? FlowLayout.RIGHT : FlowLayout.LEFT));
        wrapper.setBackground(BACKGROUND_COLOR);
        
        String bubbleColor = isUser ? "#8A7AFF" : "#30344D";
        
        JLabel label = new JLabel("<html><body style='width: 250px; padding: 12px; background-color: " + bubbleColor + "; color: #F5F5FA; font-family: Segoe UI;'>" + text + "</body></html>");
        label.setOpaque(true);
        
        wrapper.add(label);
        chatPanel.add(wrapper);
        chatPanel.add(Box.createVerticalStrut(12));
        
        chatPanel.revalidate();
        chatPanel.repaint();
        
        // Auto-scroll to bottom
        SwingUtilities.invokeLater(() -> {
            scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
        });
    }

    private void handleCommand(String rawCommand) {
        String cmd = rawCommand.toLowerCase().trim();
        inputField.setText("");
        
        if (cmd.equals("list")) {
            StringBuilder sb = new StringBuilder("<b>📜 Full Catalog:</b><br><br>");
            Book cur = head;
            while (cur != null) {
                String status = cur.borrowed ? "<font color='#E74C3C'>(Out)</font>" : "<font color='#2ECC71'>(Available)</font>";
                sb.append("<font color='#8A7AFF'>#").append(cur.id).append("</font> ").append(cur.title).append("<br>").append(status).append("<br><br>");
                cur = cur.next;
            }
            addMessage(sb.toString(), false);
        } 
        else if (cmd.startsWith("borrow ")) {
            try { 
                int id = Integer.parseInt(cmd.replace("borrow ", "").replaceAll("[^0-9]", ""));
                borrowBook(id); 
            } catch (Exception e) { addMessage("🤖 Enter a numeric ID in the box first!", false); }
        } 
        else if (cmd.startsWith("return ")) {
            try { 
                int id = Integer.parseInt(cmd.replace("return ", "").replaceAll("[^0-9]", ""));
                returnBook(id); 
            } catch (Exception e) { addMessage("🤖 Enter a numeric ID in the box first!", false); }
        } 
        else if (cmd.startsWith("search ")) {
            searchBooks(cmd.replace("search ", ""));
        } 
        else if (cmd.equals("undo")) {
            undo();
        }
    }

    // --- CORE LOGIC METHODS ---
    private void searchBooks(String q) {
        Book cur = head; StringBuilder res = new StringBuilder("<b>🔍 Search Results for '" + q + "':</b><br><br>");
        boolean found = false;
        while (cur != null) {
            if (cur.title.toLowerCase().contains(q)) {
                res.append("• ").append(cur.title).append(" <font color='#8A7AFF'>[ID: ").append(cur.id).append("]</font><br>");
                found = true;
            }
            cur = cur.next;
        }
        addMessage(found ? res.toString() : "🤖 No books found matching that title.", false);
    }

    private void borrowBook(int id) {
        Book b = findBook(id);
        if (b != null && !b.borrowed) {
            b.borrowed = true;
            undoStack.push(new Action(ActionType.BORROW, id));
            addMessage("✅ Enjoy your read! <b>" + b.title + "</b> has been checked out.", false);
        } else {
            addMessage("❌ Sorry, Book ID " + id + " is either out or doesn't exist.", false);
        }
    }

    private void returnBook(int id) {
        Book b = findBook(id);
        if (b != null && b.borrowed) {
            b.borrowed = false;
            undoStack.push(new Action(ActionType.RETURN, id));
            addMessage("📦 Re-stocked! <b>" + b.title + "</b> is back in the library.", false);
        } else {
            addMessage("❌ This book wasn't checked out.", false);
        }
    }

    private void undo() {
        if (undoStack.isEmpty()) { addMessage("🤖 Nothing to undo!", false); return; }
        Action a = undoStack.pop();
        Book b = findBook(a.bookId);
        if (b != null) {
            b.borrowed = (a.type == ActionType.RETURN); // Reverse the state
            addMessage("🔄 <b>Undo:</b> Reverted status for " + b.title, false);
        }
    }

    private Book findBook(int id) {
        Book cur = head;
        while (cur != null) { if (cur.id == id) return cur; cur = cur.next; }
        return null;
    }

    private void seedData() {
        // 1. Classic Books (1-15)
        addBook(15, "Les Misérables", "Victor Hugo");
        addBook(14, "The Odyssey", "Homer");
        addBook(13, "Crime and Punishment", "Dostoevsky");
        addBook(12, "Lord of the Rings", "Tolkien");
        addBook(11, "The Hobbit", "Tolkien");
        addBook(10, "Animal Farm", "Orwell");
        addBook(9, "Catcher in the Rye", "Salinger");
        addBook(8, "Pride and Prejudice", "Austen");
        addBook(7, "War and Peace", "Tolstoy");
        addBook(6, "Moby-Dick", "Melville");
        addBook(5, "The Great Gatsby", "Fitzgerald");
        addBook(4, "To Kill a Mockingbird", "Lee");
        addBook(3, "Fahrenheit 451", "Bradbury");
        addBook(2, "Brave New World", "Huxley");
        addBook(1, "1984", "Orwell");

        // 2. Random Generated Books (16-115)
        String[] pre = {"Advanced", "The Secret", "Origins of", "Intro to", "Mastering", "The Lost", "Digital"};
        String[] sub = {"Java", "Philosophy", "Cybernetics", "Deep Sea", "Astrophysics", "Ancient Myth", "Gardening"};
        String[] aut = {"A. Einstein", "B. Wayne", "C. Kent", "D. Prince", "E. Musk", "F. Underwood"};
        
        Random r = new Random();
        for (int i = 16; i <= 115; i++) {
            String t = pre[r.nextInt(pre.length)] + " " + sub[r.nextInt(sub.length)];
            addBook(i, t, aut[r.nextInt(aut.length)]);
        }
    }

    private void addBook(int id, String title, String author) {
        head = new Book(id, title, author, head);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new chatbot().setVisible(true));
    }
}