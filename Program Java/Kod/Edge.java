
// klasa definiujaca polaczenie miedzy dwoma wierzcholkami
public class Edge {
    String name; 
    int u, v; 
    double weight;
    
    public Edge(String name, int u, int v, double weight) { 
        this.name = name; 
        this.u = u; 
        this.v = v; 
        this.weight = weight; 
    }
}