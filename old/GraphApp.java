import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

// klasa przechowujaca dane pojedynczego wierzcholka
class Node {
    int id; double x, y;
    public Node(int id, double x, double y) { this.id = id; this.x = x; this.y = y; }
}

// klasa definiujaca polaczenie miedzy dwoma wierzcholkami
class Edge {
    String name; int u, v; double weight;
    public Edge(String name, int u, int v, double weight) { this.name = name; this.u = u; this.v = v; this.weight = weight; }
}

// glowna klasa aplikacji z interfejsem graficznym
public class GraphApp extends JFrame {
    private GraphPanel graphPanel;
    private JComboBox<String> algoSelector;
    private String currentTopoFile = "dane.txt";
    private JTextField idField, xField, yField;
    private Node currentlySelectedNode = null;
    private boolean isUpdatingFields = false;
    
    // zmienna przechowujaca sciezke do programu obliczeniowego
    private String enginePath = null;

    // definicje kolorow wykorzystywanych w elementach interfejsu
    private final Color PANEL_BG = new Color(230, 242, 255); 
    private final Color WIDGET_BG = Color.WHITE; 
    private final Color BTN_PRIMARY = new Color(30, 136, 229); 
    private final Color BTN_SUCCESS = new Color(0, 150, 136); 
    private final Color BTN_NEUTRAL = new Color(144, 164, 174); 
    
    // deklaracje krojow pisma
    private final Font MAIN_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font BOLD_FONT = new Font("Segoe UI", Font.BOLD, 14);

    // konstruktor inicjalizujacy okno glowne
    public GraphApp() {
        super("Wizualizacja Grafów Planarnych");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.WHITE);
        
        // dodanie przestrzeni do rysowania grafu
        graphPanel = new GraphPanel();
        graphPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(180, 200, 220)));
        add(graphPanel, BorderLayout.CENTER);
        
        // przypisanie akcji na klikniecie i przesuniecie wierzcholka
        graphPanel.setNodeSelectionListener(new GraphPanel.NodeSelectionListener() {
            @Override
            public void onNodeSelected(Node n) { currentlySelectedNode = n; updatePropertiesPanel(n); }
            @Override
            public void onNodeMoved(Node n) { if (currentlySelectedNode == n) updatePropertiesPanel(n); }
        });
        createMenuBar();
        createSidePanel();
    }

    // metoda tworzaca gorny pasek nawigacyjny
    private void createMenuBar() {
        JMenuBar mb = new JMenuBar();
        mb.setBackground(Color.WHITE);
        mb.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 210, 225))); 
        
        JMenu m1 = new JMenu(" Plik "); m1.setFont(MAIN_FONT);
        JMenuItem i1 = new JMenuItem("Otwórz strukturę (.txt)..."); i1.addActionListener(e -> loadTopology());
        JMenuItem i2 = new JMenuItem("Wczytaj współrzędne (.txt)..."); i2.addActionListener(e -> loadCoordinates());
        JMenuItem i3 = new JMenuItem("Zapisz współrzędne..."); i3.addActionListener(e -> saveCoordinates());
        JMenuItem i4 = new JMenuItem("Eksportuj obraz..."); i4.addActionListener(e -> exportToPNG());
        m1.add(i1); m1.add(i2); m1.addSeparator(); m1.add(i3); m1.add(i4);
        m1.addSeparator(); m1.add(new JMenuItem("Zakończ")).addActionListener(e -> System.exit(0));
        
        JMenu m2 = new JMenu(" Widok "); m2.setFont(MAIN_FONT);
        m2.add(new JMenuItem("Przybliż")).addActionListener(e -> graphPanel.zoomIn());
        m2.add(new JMenuItem("Oddal")).addActionListener(e -> graphPanel.zoomOut());
        m2.add(new JMenuItem("Resetuj widok")).addActionListener(e -> graphPanel.resetView());
        
        JMenu m3 = new JMenu(" Narzędzia "); m3.setFont(MAIN_FONT);
        m3.add(new JMenuItem("Rozmieść węzły losowo")).addActionListener(e -> graphPanel.randomizeNodes());
        
        mb.add(m1); mb.add(m2); mb.add(m3);
        setJMenuBar(mb);
    }

    // budowanie prawego panelu z opcjami i parametrami
    private void createSidePanel() {
        JPanel sp = new JPanel();
        sp.setLayout(new BoxLayout(sp, BoxLayout.Y_AXIS));
        sp.setBorder(new EmptyBorder(25, 20, 25, 20));
        sp.setBackground(PANEL_BG);
        sp.setPreferredSize(new Dimension(300, 0));

        JLabel l1 = new JLabel("Wybór algorytmu:");
        l1.setFont(BOLD_FONT); l1.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        algoSelector = new JComboBox<>(new String[]{"Fruchterman-Reingold", "Tutte Embedding"});
        algoSelector.setFont(MAIN_FONT); algoSelector.setBackground(Color.WHITE);
        algoSelector.setMaximumSize(new Dimension(260, 35)); algoSelector.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JButton runBtn = new JButton("Przelicz układ (Silnik C)");
        styleButton(runBtn, BTN_PRIMARY);
        runBtn.addActionListener(e -> runCalculation());

        JLabel lProps = new JLabel("Właściwości węzła:");
        lProps.setFont(BOLD_FONT); lProps.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JPanel propsBox = new JPanel(new GridLayout(3, 2, 8, 8));
        propsBox.setBackground(WIDGET_BG);
        propsBox.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(200, 215, 230), 1, true), 
            new EmptyBorder(15, 15, 15, 15) 
        ));
        propsBox.setMaximumSize(new Dimension(260, 110)); 
        propsBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        propsBox.add(new JLabel("ID:")).setFont(MAIN_FONT); 
        idField = new JTextField(); idField.setEditable(false); idField.setHorizontalAlignment(JTextField.CENTER); propsBox.add(idField);
        
        propsBox.add(new JLabel("Oś X:")).setFont(MAIN_FONT); 
        xField = new JTextField(); xField.setEditable(false); xField.setHorizontalAlignment(JTextField.CENTER); propsBox.add(xField);
        
        propsBox.add(new JLabel("Oś Y:")).setFont(MAIN_FONT); 
        yField = new JTextField(); yField.setEditable(false); yField.setHorizontalAlignment(JTextField.CENTER); propsBox.add(yField);
        
        ActionListener editL = e -> applyManualCoordinates();
        xField.addActionListener(editL); yField.addActionListener(editL);
        xField.addFocusListener(new FocusAdapter() { public void focusLost(FocusEvent e) { applyManualCoordinates(); } });
        yField.addFocusListener(new FocusAdapter() { public void focusLost(FocusEvent e) { applyManualCoordinates(); } });

        JLabel lHint = new JLabel("Zmień pozycję ręcznie i wciśnij Enter");
        lHint.setFont(new Font("Segoe UI", Font.ITALIC, 11)); lHint.setForeground(new Color(100, 120, 140));
        lHint.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel l2 = new JLabel("Wyświetlanie:");
        l2.setFont(BOLD_FONT); l2.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JCheckBox c1 = new JCheckBox("Pokaż etykiety", true);
        c1.setFont(MAIN_FONT); c1.setBackground(PANEL_BG); c1.setFocusPainted(false); c1.setAlignmentX(Component.CENTER_ALIGNMENT);
        c1.addActionListener(e -> graphPanel.setShowLabels(c1.isSelected()));
        
        JCheckBox c2 = new JCheckBox("Pokaż wagi", true);
        c2.setFont(MAIN_FONT); c2.setBackground(PANEL_BG); c2.setFocusPainted(false); c2.setAlignmentX(Component.CENTER_ALIGNMENT);
        c2.addActionListener(e -> graphPanel.setShowWeights(c2.isSelected()));
        
        JButton resPosBtn = new JButton("Przywróć pozycje węzłów");
        styleButton(resPosBtn, BTN_SUCCESS);
        resPosBtn.addActionListener(e -> { graphPanel.resetNodePositions(); if(currentlySelectedNode!=null) updatePropertiesPanel(currentlySelectedNode); });
        
        JButton resViewBtn = new JButton("Resetuj kamerę");
        styleButton(resViewBtn, BTN_NEUTRAL);
        resViewBtn.addActionListener(e -> graphPanel.resetView());

        sp.add(l1); sp.add(Box.createRigidArea(new Dimension(0, 8))); sp.add(algoSelector); sp.add(Box.createRigidArea(new Dimension(0, 15))); sp.add(runBtn);
        sp.add(Box.createRigidArea(new Dimension(0, 35))); 
        sp.add(lProps); sp.add(Box.createRigidArea(new Dimension(0, 8))); sp.add(propsBox); sp.add(Box.createRigidArea(new Dimension(0, 5))); sp.add(lHint);
        sp.add(Box.createRigidArea(new Dimension(0, 35))); 
        sp.add(l2); sp.add(Box.createRigidArea(new Dimension(0, 8))); sp.add(c1); sp.add(c2);
        sp.add(Box.createRigidArea(new Dimension(0, 20))); sp.add(resPosBtn); sp.add(Box.createRigidArea(new Dimension(0, 10))); sp.add(resViewBtn);
        sp.add(Box.createVerticalGlue());
        
        add(sp, BorderLayout.EAST);
    }
    
    // nakladanie wygladu na przyciski
    private void styleButton(JButton btn, Color bg) {
        btn.setFont(BOLD_FONT); btn.setBackground(bg); btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false); btn.setBorderPainted(false); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(260, 45)); btn.setAlignmentX(Component.CENTER_ALIGNMENT);
    }

    // wyswietlanie aktualnych wspolrzednych dla wskazanego elementu
    private void updatePropertiesPanel(Node n) {
        isUpdatingFields = true;
        if (n == null) { 
            idField.setText("-"); xField.setText("-"); yField.setText("-"); 
            xField.setEditable(false); yField.setEditable(false); 
        } else { 
            idField.setText(String.valueOf(n.id)); 
            xField.setText(String.format(Locale.US, "%.2f", n.x)); 
            yField.setText(String.format(Locale.US, "%.2f", n.y)); 
            xField.setEditable(true); yField.setEditable(true); 
        }
        isUpdatingFields = false;
    }

    // zmiana polozenia na podstawie recznego wpisu uzytkownika ze sprawdzeniami bezpieczenstwa
    private void applyManualCoordinates() {
        if (currentlySelectedNode != null && !isUpdatingFields) {
            try { 
                // zamiana ewentualnych przecinkow wpisanych przez uzytkownika na kropki
                String textX = xField.getText().replace(",", ".");
                String textY = yField.getText().replace(",", ".");
                
                double newX = Double.parseDouble(textX); 
                double newY = Double.parseDouble(textY); 
                
                // definicja granicy wartosci dla osi w celu ochrony przed zepsuciem widoku
                double maxLimit = 1000000.0;
                
                // odrzucanie zbyt oddalonych pozycji i przywracanie bezpiecznych stanow
                if (Math.abs(newX) > maxLimit || Math.abs(newY) > maxLimit) {
                    JOptionPane.showMessageDialog(this, 
                        "Wprowadzona wartość przekracza dozwolony bezpieczny limit.\nSystem odrzucił zmianę pozycji.", 
                        "Blokada bezpieczeństwa", 
                        JOptionPane.WARNING_MESSAGE);
                        
                    updatePropertiesPanel(currentlySelectedNode);
                    return; 
                }
                
                // zastosowanie nowej pozycji i aktualizacja okna
                currentlySelectedNode.x = newX; 
                currentlySelectedNode.y = newY; 
                graphPanel.repaint(); 
                
            } catch (NumberFormatException ex) {
                // lapanie liter lub symboli w tekscie i blokowanie bledu systemu
                JOptionPane.showMessageDialog(this, 
                    "Współrzędne muszą być poprawnego formatu liczbowego.\nUżyj cyfr oraz pojedynczej kropki dziesiętnej.", 
                    "Błąd formatu", 
                    JOptionPane.ERROR_MESSAGE);
                    
                updatePropertiesPanel(currentlySelectedNode);
            }
        }
    }

    // przygotowanie i wywolanie procesu z zewnetrznym programem obliczeniowym
    private void runCalculation() {
        if(graphPanel.getEdges().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Najpierw wczytaj strukturę krawędzi z pliku tektsowego!", "Brak danych", JOptionPane.WARNING_MESSAGE); 
            return;
        }

        // proces szukania aplikacji wewnatrz folderow projektu
        if (enginePath == null) {
            String exeName = "graph_layout.exe";
            File dataDir = new File(currentTopoFile).getAbsoluteFile().getParentFile();
            
            // predefiniowane pozycje na dysku
            File[] possiblePaths = {
                new File(exeName),                                      
                new File("Algorytm c/pliki_zrodlowe_2_grupy/" + exeName), 
                new File("../Algorytm c/pliki_zrodlowe_2_grupy/" + exeName),                   
                new File("../../Algorytm c/pliki_zrodlowe_2_grupy/" + exeName),                
                dataDir != null ? new File(dataDir, exeName) : new File(exeName) 
            };

            // weryfikacja poprawnosci znaleziska
            for (File path : possiblePaths) {
                if (path.exists() && !path.isDirectory()) {
                    enginePath = path.getAbsolutePath();
                    break; 
                }
            }

            // aktywowanie okna dialogowego w przypadku niepowodzenia
            if (enginePath == null) {
                JOptionPane.showMessageDialog(this, 
                    "Nie udało się automatycznie znaleźć pliku obliczeniowego w folderze.\nWskaż go ręcznie z poziomu dysku.", 
                    "Wskaż silnik C", JOptionPane.INFORMATION_MESSAGE);
                JFileChooser ch = new JFileChooser();
                ch.setDialogTitle("Wybierz plik wykonywalny obliczen");
                if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    enginePath = ch.getSelectedFile().getAbsolutePath();
                } else {
                    return;
                }
            }
        }

        // przechwytywanie decyzji z pola wyboru algorytmu
        boolean isTutte = algoSelector.getSelectedItem().equals("Tutte Embedding");
        
        try {
            File inputFile = new File(currentTopoFile).getAbsoluteFile();
            File outputFile = new File(inputFile.getParentFile(), "wynik.txt");

            // skladanie sekwencji dla wiersza polecen
            List<String> command = new ArrayList<>();
            command.add(enginePath);
            command.add(inputFile.getAbsolutePath()); 
            command.add("-o");
            command.add(outputFile.getAbsolutePath());
            
            // dolaczenie parametru dla specyficznego algorytmu
            if (isTutte) {
                command.add("-a"); 
            }

            // definicja wywolywania procesu
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(enginePath).getParentFile());
            Process p = pb.start();
            
            // nadzrowanie procesu i zamykanie go w przypadku usterki petli
            boolean finished = p.waitFor(15, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly(); 
                JOptionPane.showMessageDialog(this, 
                    "Program obliczeniowy zawiesił się i przekroczył dozwolony limit czasu oczekiwania.\nOperacja została siłowo zatrzymana.", 
                    "Przekroczenie limitu czasu", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // odczytywanie ewentualnych komunikatow usterki z dzialajacego programu
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            StringBuilder errors = new StringBuilder(); String line;
            while ((line = errorReader.readLine()) != null) errors.append(line).append("\n");
            
            // potwierdzanie zakonczenia dzialania zewnetrznego programu i ladowanie wylkresow
            int exitCode = p.exitValue(); 
            if (exitCode == 0) { 
                loadCoordinatesFromFile(outputFile.getAbsolutePath()); 
                graphPanel.resetView(); 
                JOptionPane.showMessageDialog(this, "Silnik zewnetrzny poprawnie obliczył układ grafu."); 
            } else {
                String errorMsg = errors.toString().trim();
                if (errorMsg.isEmpty()) errorMsg = "Program zwrocil kod awaryjny o numerze " + exitCode;
                JOptionPane.showMessageDialog(this, "Napotkano problem na etapie logiki:\n" + errorMsg, "Awaria obliczen", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) { 
            JOptionPane.showMessageDialog(this, "Błąd systemu glownego podczas uruchamiania procedury:\n" + ex.getMessage(), "Błąd Systemu", JOptionPane.ERROR_MESSAGE); 
            enginePath = null; 
        }
    }

    // pobieranie dokumentu definiujacego ksztalt i filtrowanie zepsutych danych
    private void loadTopology() {
        JFileChooser ch = new JFileChooser();
        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentTopoFile = ch.getSelectedFile().getAbsolutePath();
            List<Edge> edges = new ArrayList<>();
            boolean skippedFaultyWeights = false;
            
            // limit powstrzymujacy ladowanie ogromnych plikow do pamieci urzadzenia
            int edgeLimit = 50000;

            try (BufferedReader br = new BufferedReader(new FileReader(currentTopoFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    
                    // zatrzymanie ladowania jesli plik przekroczy bezpieczny limit rozmiaru
                    if (edges.size() >= edgeLimit) {
                        JOptionPane.showMessageDialog(this, 
                            "Plik zawiera zbyt dużą ilość linii tekstu.\nWczytano tylko bezpieczną pule krawędzi, chroniąc system przed brakiem pamięci ram.", 
                            "Limit rozmiaru", 
                            JOptionPane.WARNING_MESSAGE);
                        break;
                    }
                    
                    String[] p = line.trim().split("\\s+");
                    if (p.length >= 4) {
                        double weight = Double.parseDouble(p[3]);
                        
                        // badanie wagi krawedzi i ignorowanie psujacych logike liczb ujemnych oraz zer
                        if (weight > 0) {
                            edges.add(new Edge(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), weight));
                        } else {
                            skippedFaultyWeights = true;
                        }
                    }
                }
                graphPanel.setEdges(edges);
                
                // powiadomienie po wczytaniu o ignorowaniu uszkodzonych krawedzi
                if (skippedFaultyWeights) {
                    JOptionPane.showMessageDialog(this, 
                        "Odrzucono niektóre krawędzie z powodu zerowej lub ujemnej wartości ich wagi.\nOchrona stabilności modeli odległościowych.", 
                        "Raport filtrowania", 
                        JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Błąd struktury w trakcie wczytywania dokumentu", "Awaria analizy tekstu", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // wskazywanie pliku z gotowymi pozycjami dla elementow
    private void loadCoordinates() {
        JFileChooser ch = new JFileChooser();
        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) loadCoordinatesFromFile(ch.getSelectedFile().getAbsolutePath());
    }

    // parsowanie dokumentu i przypisywanie wartosci do obiektow w pamieci
    private void loadCoordinatesFromFile(String path) {
        Map<Integer, Node> nodes = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.trim().split("\\s+");
                if (p.length >= 3) nodes.put(Integer.parseInt(p[0]), new Node(Integer.parseInt(p[0]), Double.parseDouble(p[1]), Double.parseDouble(p[2])));
            }
            graphPanel.setNodes(nodes);
        } catch (Exception ex) {}
    }

    // generowanie pliku z zapisanymi danymi geometrycznymi
    private void saveCoordinates() {
        if(graphPanel.getNodes().isEmpty()) return;
        JFileChooser ch = new JFileChooser();
        if (ch.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(ch.getSelectedFile() + ".txt"))) {
                for (Node n : graphPanel.getNodes().values()) pw.printf(Locale.US, "%d %.6f %.6f\n", n.id, n.x, n.y);
            } catch (Exception ex) {}
        }
    }

    // przechwytywanie obszaru roboczego i tworzenie grafiki
    private void exportToPNG() {
        if(graphPanel.getNodes().isEmpty()) return;
        JFileChooser ch = new JFileChooser(); ch.setFileFilter(new FileNameExtensionFilter("PNG", "png"));
        if (ch.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = ch.getSelectedFile(); if(!f.getName().toLowerCase().endsWith(".png")) f = new File(f.getAbsolutePath() + ".png");
            BufferedImage img = new BufferedImage(graphPanel.getWidth(), graphPanel.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = img.createGraphics(); graphPanel.paint(g2); g2.dispose();
            try { ImageIO.write(img, "png", f); } catch (Exception ex) {}
        }
    }

    // metoda wywolywana podczas wlaczania aplikacji
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new GraphApp().setVisible(true));
    }
}

// panel zarzadzajacy manipulacja i renderowaniem grafiki
class GraphPanel extends JPanel {
    private Map<Integer, Node> nodes = new HashMap<>();
    private Map<Integer, Point2D.Double> snapshots = new HashMap<>();
    private List<Edge> edges = new ArrayList<>();
    private double zoom = 1.0, px = 0, py = 0;
    private Node selectedNode = null;
    private Node dragNode = null;
    private Point dragStart = null;
    private boolean showLabels = true, showWeights = true;

    // interfejs umozliwiajacy przekazywanie zdarzen na zewnatrz
    public interface NodeSelectionListener { void onNodeSelected(Node n); void onNodeMoved(Node n); }
    private NodeSelectionListener selectionListener;

    // definicje podstawowych akcji dla interakcji urzadzenia wskazujacego
    public GraphPanel() {
        setBackground(Color.WHITE);
        addMouseWheelListener(e -> {
            double z = (e.getWheelRotation() < 0) ? 1.1 : 0.9;
            Point p = e.getPoint(); px = p.x - (p.x - px) * z; py = p.y - (p.y - py) * z;
            zoom *= z; repaint();
        });
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    Point2D w = screenToWorld(e.getPoint());
                    for (Node n : nodes.values()) if (Math.hypot(n.x - w.getX(), n.y - w.getY()) <= 15/zoom) { 
                        selectedNode = n; dragNode = n; if(selectionListener != null) selectionListener.onNodeSelected(n); repaint(); return; 
                    }
                }
                selectedNode = null; if(selectionListener != null) selectionListener.onNodeSelected(null);
                dragStart = e.getPoint(); repaint();
            }
            public void mouseReleased(MouseEvent e) { dragNode = null; }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (dragNode != null) {
                    Point2D w = screenToWorld(e.getPoint());
                    dragNode.x = w.getX(); dragNode.y = w.getY(); if(selectionListener != null) selectionListener.onNodeMoved(dragNode); repaint();
                } else if (dragStart != null) {
                    px += e.getX() - dragStart.getX(); py += e.getY() - dragStart.getY();
                    dragStart = e.getPoint(); repaint();
                }
            }
        });
    }

    // rejestrowanie obiektu nasluchujacego
    public void setNodeSelectionListener(NodeSelectionListener l) { this.selectionListener = l; }
    
    // podstawianie nowej mapy danych z zapisywaniem punktu przywracania
    public void setNodes(Map<Integer, Node> n) {
        this.nodes = n; snapshots.clear();
        for(Node node : n.values()) snapshots.put(node.id, new Point2D.Double(node.x, node.y));
        selectedNode = null; if(selectionListener!=null) selectionListener.onNodeSelected(null); resetView();
    }
    
    // wczytywanie zapamietanych stanow wierzcholkow
    public void resetNodePositions() {
        for(Node n : nodes.values()) { Point2D.Double p = snapshots.get(n.id); if(p!=null) { n.x = p.x; n.y = p.y; } }
        repaint();
    }
    
    // ladowanie listy krawedzi i odswiezanie
    public void setEdges(List<Edge> e) { this.edges = e; repaint(); }
    public void zoomIn() { zoom *= 1.2; repaint(); }
    public void zoomOut() { zoom /= 1.2; repaint(); }
    
    // obliczanie proporcji grafu i srodkowanie go na polu roboczym
    public void resetView() {
        zoom = 1.0;
        if (nodes.isEmpty()) {
            px = getWidth() / 2.0;
            py = getHeight() / 2.0;
        } else {
            // szukanie ostatecznych granic rysunku
            double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
            double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            
            for (Node n : nodes.values()) {
                if (n.x < minX) minX = n.x;
                if (n.x > maxX) maxX = n.x;
                if (n.y < minY) minY = n.y;
                if (n.y > maxY) maxY = n.y;
            }
            
            // wyliczanie punktu bedacego centrum struktury
            double centerX = (minX + maxX) / 2.0;
            double centerY = (minY + maxY) / 2.0;
            
            // pobieranie mnoznika korekcyjnego
            double s = getDynamicScale();
            
            // stosowanie korekty wspolrzednych i centrowanie
            px = (getWidth() / 2.0) - (centerX * s);
            py = (getHeight() / 2.0) - (centerY * s);
        }
        repaint();
    }
    
    // rozrzucanie punktow w granicach ekranu dla ulatwienia podgladu
    public void randomizeNodes() { Random r = new Random(); for(Node n : nodes.values()) { n.x = (r.nextDouble()-0.5)*800; n.y = (r.nextDouble()-0.5)*800; } repaint(); }
    public void setShowLabels(boolean s) { this.showLabels = s; repaint(); }
    public void setShowWeights(boolean s) { this.showWeights = s; repaint(); }
    public Map<Integer, Node> getNodes() { return nodes; }
    public List<Edge> getEdges() { return edges; }
    
    // adaptacja pozycji kursora do wielkosci i przesuniecia elementow
    private Point2D screenToWorld(Point p) { 
        double autoScale = getDynamicScale();
        return new Point2D.Double(((p.x-px)/zoom) / autoScale, ((p.y-py)/zoom) / autoScale); 
    }

    // wyznaczanie prawidlowego powiekszenia dla liczb ulokowanych blisko zera
    private double getDynamicScale() {
        double maxDist = 0;
        for (Node n : nodes.values()) {
            maxDist = Math.max(maxDist, Math.abs(n.x));
            maxDist = Math.max(maxDist, Math.abs(n.y));
        }
        return (maxDist > 0 && maxDist < 15) ? 150.0 : 1.0;
    }

    // metoda nadpisujaca rysowanie grafiki w panelu
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // wyznaczanie rozstwow grubosci dla uzytych wag
        double maxW = Double.MIN_VALUE, minW = Double.MAX_VALUE;
        for (Edge e : edges) { if (e.weight > maxW) maxW = e.weight; if (e.weight < minW) minW = e.weight; }
        
        AffineTransform oldAt = g2.getTransform();
        g2.translate(px, py); g2.scale(zoom, zoom);
        
        // aplikowanie wczesniej wyliczonej korekty wielkosci
        double s = getDynamicScale();

        g2.setStroke(new BasicStroke(2.0f / (float)zoom));
        
        // proces malowania linii miedzy punktami
        for (Edge e : edges) {
            Node n1 = nodes.get(e.u), n2 = nodes.get(e.v);
            if (n1 != null && n2 != null) {
                float r = (maxW == minW) ? 0.5f : (float)((e.weight - minW) / (maxW - minW));
                g2.setColor(new Color(r, 1.0f - r, 0.0f, 0.6f));
                
                g2.drawLine((int)(n1.x * s), (int)(n1.y * s), (int)(n2.x * s), (int)(n2.y * s));
                
                if (showWeights) { 
                    g2.setColor(Color.DARK_GRAY); 
                    g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(8, (int)(12/zoom)))); 
                    g2.drawString(String.format(Locale.US, "%.1f", e.weight), (int)(((n1.x+n2.x)/2)*s), (int)(((n1.y+n2.y)/2)*s)-5); 
                }
            }
        }
        
        int r = Math.max(3, (int)(15 / zoom));
        
        // nanoszenie ksztaltow geometrycznych oznaczajacych glowne elementy
        for (Node n : nodes.values()) {
            int cx = (int)(n.x * s);
            int cy = (int)(n.y * s);
            
            if (n == selectedNode) { g2.setColor(new Color(255, 171, 0)); g2.fillOval(cx-r, cy-r, r*2, r*2); g2.setColor(new Color(216, 67, 21)); g2.setStroke(new BasicStroke(3.0f/(float)zoom)); }
            else { g2.setColor(new Color(66, 165, 245)); g2.fillOval(cx-r, cy-r, r*2, r*2); g2.setColor(new Color(30, 136, 229)); g2.setStroke(new BasicStroke(1.5f/(float)zoom)); }
            g2.drawOval(cx-r, cy-r, r*2, r*2);
            
            if (showLabels) { g2.setColor(Color.BLACK); g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(9, (int)(13/zoom)))); g2.drawString(String.valueOf(n.id), cx-r+2, cy-r-6); }
        }
        g2.setTransform(oldAt);
    }
}