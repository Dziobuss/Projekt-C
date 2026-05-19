import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

// panel zarzadzajacy manipulacja i renderowaniem grafiki
public class GraphPanel extends JPanel {
    private Map<Integer, Node> nodes = new HashMap<>();
    private Map<Integer, Point2D.Double> snapshots = new HashMap<>();
    private List<Edge> edges = new ArrayList<>();
    private double zoom = 1.0, px = 0, py = 0;
    private Node selectedNode = null;
    private Node dragNode = null;
    private Point dragStart = null;
    private boolean showLabels = true, showWeights = true;

    // interfejs umozliwiajacy przekazywanie zdarzen na zewnatrz
    public interface NodeSelectionListener { 
        void onNodeSelected(Node n); 
        void onNodeMoved(Node n); 
    }
    private NodeSelectionListener selectionListener;

    // definicje podstawowych akcji dla interakcji urzadzenia wskazujacego
    public GraphPanel() {
        setBackground(Color.WHITE);
        addMouseWheelListener(e -> {
            double z = (e.getWheelRotation() < 0) ? 1.1 : 0.9;
            Point p = e.getPoint(); 
            px = p.x - (p.x - px) * z; 
            py = p.y - (p.y - py) * z;
            zoom *= z; 
            repaint();
        });
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    Point2D w = screenToWorld(e.getPoint());
                    for (Node n : nodes.values()) {
                        if (Math.hypot(n.x - w.getX(), n.y - w.getY()) <= 15/zoom) { 
                            selectedNode = n; 
                            dragNode = n; 
                            if(selectionListener != null) selectionListener.onNodeSelected(n); 
                            repaint(); 
                            return; 
                        }
                    }
                }
                selectedNode = null; 
                if(selectionListener != null) selectionListener.onNodeSelected(null);
                dragStart = e.getPoint(); 
                repaint();
            }
            public void mouseReleased(MouseEvent e) { dragNode = null; }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (dragNode != null) {
                    Point2D w = screenToWorld(e.getPoint());
                    dragNode.x = w.getX(); 
                    dragNode.y = w.getY(); 
                    if(selectionListener != null) selectionListener.onNodeMoved(dragNode); 
                    repaint();
                } else if (dragStart != null) {
                    px += e.getX() - dragStart.getX(); 
                    py += e.getY() - dragStart.getY();
                    dragStart = e.getPoint(); 
                    repaint();
                }
            }
        });
    }

    // rejestrowanie obiektu nasluchujacego
    public void setNodeSelectionListener(NodeSelectionListener l) { this.selectionListener = l; }
    
    // podstawianie nowej mapy danych z zapisywaniem punktu przywracania
    public void setNodes(Map<Integer, Node> n) {
        this.nodes = n; 
        snapshots.clear();
        for(Node node : n.values()) snapshots.put(node.id, new Point2D.Double(node.x, node.y));
        selectedNode = null; 
        if(selectionListener!=null) selectionListener.onNodeSelected(null); 
        resetView();
    }
    
    // wczytywanie zapamietanych stanow wierzcholkow
    public void resetNodePositions() {
        for(Node n : nodes.values()) { 
            Point2D.Double p = snapshots.get(n.id); 
            if(p!=null) { n.x = p.x; n.y = p.y; } 
        }
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
    public void randomizeNodes() { 
        Random r = new Random(); 
        for(Node n : nodes.values()) { 
            n.x = (r.nextDouble()-0.5)*800; 
            n.y = (r.nextDouble()-0.5)*800; 
        } 
        repaint(); 
    }
    
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
        for (Edge e : edges) { 
            if (e.weight > maxW) maxW = e.weight; 
            if (e.weight < minW) minW = e.weight; 
        }
        
        AffineTransform oldAt = g2.getTransform();
        g2.translate(px, py); 
        g2.scale(zoom, zoom);
        
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
            
            if (n == selectedNode) { 
                g2.setColor(new Color(255, 171, 0)); 
                g2.fillOval(cx-r, cy-r, r*2, r*2); 
                g2.setColor(new Color(216, 67, 21)); 
                g2.setStroke(new BasicStroke(3.0f/(float)zoom)); 
            } else { 
                g2.setColor(new Color(66, 165, 245)); 
                g2.fillOval(cx-r, cy-r, r*2, r*2); 
                g2.setColor(new Color(30, 136, 229)); 
                g2.setStroke(new BasicStroke(1.5f/(float)zoom)); 
            }
            g2.drawOval(cx-r, cy-r, r*2, r*2);
            
            if (showLabels) { 
                g2.setColor(Color.BLACK); 
                g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(9, (int)(13/zoom)))); 
                g2.drawString(String.valueOf(n.id), cx-r+2, cy-r-6); 
            }
        }
        g2.setTransform(oldAt);
    }
}