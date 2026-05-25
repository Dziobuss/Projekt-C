#include "graph.h"

// --- ALGORYTM 2: FRUCHTERMAN-REINGOLD (Siłowy) ---
void calculate_fruchterman(Vertex* vertices, int num_v, Edge* edges, int num_e, int iterations) {
    double area = 1000000.0; 
    double k = sqrt(area / num_v); // ustawienie optymalnej odleglosci miedzy wierzcholkami 
    for (int iter = 0; iter < iterations; iter++) {
        // 1. Odpychanie
        for (int i = 0; i < num_v; i++) { 
            for (int j = 0; j < num_v; j++) {
                if (i == j) continue;
                double dx = vertices[i].x - vertices[j].x;
                double dy = vertices[i].y - vertices[j].y;
                double dist = sqrt(dx * dx + dy * dy) + 0.01;
                double force = (k * k) / dist; // sila odpychania
                vertices[i].x += (dx / dist) * force * 0.01; // aktualizacja polozenia wierzcholka
                vertices[i].y += (dy / dist) * force * 0.01;
            }
        }
        // 2. Przyciąganie
        for (int i = 0; i < num_e; i++) {
            int u_idx = -1, v_idx = -1;
            for(int j=0; j < num_v; j++) {
                if(vertices[j].id == edges[i].u) u_idx = j;
                if(vertices[j].id == edges[i].v) v_idx = j;
            }
            if (u_idx != -1 && v_idx != -1) {
                double dx = vertices[v_idx].x - vertices[u_idx].x;
                double dy = vertices[v_idx].y - vertices[u_idx].y;
                double dist = sqrt(dx * dx + dy * dy) + 0.01;
                double force = (dist * dist) / k; // sila przyciagania
                double factor = 0.005 * edges[i].weight;
                vertices[u_idx].x += (dx / dist) * force * factor; // oddzialywanie wierzhoclkow na siebie
                vertices[u_idx].y += (dy / dist) * force * factor;
                vertices[v_idx].x -= (dx / dist) * force * factor;
                vertices[v_idx].y -= (dy / dist) * force * factor;
            }
        }
    }
}