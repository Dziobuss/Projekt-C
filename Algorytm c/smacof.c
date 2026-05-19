#include "graph.h"

// Bezpieczna odległość
double safe_dist(double x1, double y1, double x2, double y2) {
    double dx = x1 - x2;
    double dy = y1 - y2;
    double d = sqrt(dx*dx + dy*dy);
    return (d < EPSILON) ? EPSILON : d; // Nigdy nie zwracaj 0
}

// --- ALGORYTM 1: SMACOF ---
void calculate_smacof(Vertex* vertices, int num_v, Edge* edges, int num_e, int iterations) {
    // Alokacja pamięci dla macierzy odległości i wag
    double **d_target = malloc(num_v * sizeof(double*));
    double **w = malloc(num_v * sizeof(double*));
    for(int i = 0; i < num_v; i++) {
        d_target[i] = malloc(num_v * sizeof(double));
        w[i] = malloc(num_v * sizeof(double));
        for(int j = 0; j < num_v; j++) {
            d_target[i][j] = (i == j) ? 0.0 : DBL_MAX / 2.0;
        }
    }

    // Wypełnianie bezpośrednich krawędzi poczatkowymi wartosciami
    for(int i = 0; i < num_e; i++) {
        int u_idx = -1, v_idx = -1;
        for(int j = 0; j < num_v; j++) {
            if(vertices[j].id == edges[i].u) u_idx = j;
            if(vertices[j].id == edges[i].v) v_idx = j;
        }
        if(u_idx != -1 && v_idx != -1) {
            // Zabezpieczenie przed ujemnymi lub zerowymi wagami
            double weight = (edges[i].weight <= 0) ? 0.1 : edges[i].weight;
            d_target[u_idx][v_idx] = weight;
            d_target[v_idx][u_idx] = weight;
        }
    }

    // Floyd-Warshall liczy najkrótsze ścieżki dla wszystkich punktów
    for(int k = 0; k < num_v; k++) {
        for(int i = 0; i < num_v; i++) {
            for(int j = 0; j < num_v; j++) {
                if (d_target[i][k] + d_target[k][j] < d_target[i][j]) {
                    d_target[i][j] = d_target[i][k] + d_target[k][j];
                }
            }
        }
    }

    // Jesli nie ma polaczenia z jakas wartoscia wypchnie ją na dużą odległość
    double max_dist = 0;
    for(int i = 0; i < num_v; i++) {
        for(int j = 0; j < num_v; j++) {
            if(d_target[i][j] < DBL_MAX / 4.0 && d_target[i][j] > max_dist) {
                max_dist = d_target[i][j];
            }
        }
    }
    for(int i = 0; i < num_v; i++) {
        for(int j = 0; j < num_v; j++) {
            if(d_target[i][j] >= DBL_MAX / 4.0) {
                d_target[i][j] = max_dist * 2.0; 
            }
        }
    }

    // Inicjalizacja punktów na okręgu (zamienione na współrzędne biegunowe)
    for (int i = 0; i < num_v; i++) {
        vertices[i].x = cos(i * 2.0 * M_PI / num_v) * num_v;
        vertices[i].y = sin(i * 2.0 * M_PI / num_v) * num_v;
    }

    // Liczenie wag
    for(int i = 0; i < num_v; i++) {
        for(int j = 0; j < num_v; j++) {
            if (i == j || d_target[i][j] >= DBL_MAX / 4.0) {
                w[i][j] = 0;
            } else {
                w[i][j] = 1.0 / (d_target[i][j] * d_target[i][j]);
            }
        }
    }

    double *next_x = malloc(num_v * sizeof(double));
    double *next_y = malloc(num_v * sizeof(double));

    /* Transformacja Guttmana, i patrzy na wszystkie inne punkty
    sprawdza desired target i wylicza co iteracje nowy x i y dla kazdego pktu
    jako srednia wazona*/
    for (int iter = 0; iter < iterations; iter++) {
        for (int i = 0; i < num_v; i++) {
            double sum_w = 0, sum_x = 0, sum_y = 0;

            for (int j = 0; j < num_v; j++) {
                if (i == j || w[i][j] == 0) continue;

                double d_curr = safe_dist(vertices[i].x, vertices[i].y, vertices[j].x, vertices[j].y);
                sum_w += w[i][j];
                
                // Formuła Guttmana
                sum_x += w[i][j] * (vertices[j].x + d_target[i][j] * (vertices[i].x - vertices[j].x) / d_curr);
                sum_y += w[i][j] * (vertices[j].y + d_target[i][j] * (vertices[i].y - vertices[j].y) / d_curr);
            }

            if (sum_w > 0) {
                next_x[i] = sum_x / sum_w;
                next_y[i] = sum_y / sum_w;
            } else {
                next_x[i] = vertices[i].x;
                next_y[i] = vertices[i].y;
            }
        }

        // Aktualizacja pozycji
        for (int i = 0; i < num_v; i++) {
            vertices[i].x = next_x[i];
            vertices[i].y = next_y[i];
        }
    }

    // Sprzątanie pamięci algorytmu
    for(int i = 0; i < num_v; i++) {
        free(d_target[i]);
        free(w[i]);
    }
    free(d_target); free(w);
    free(next_x); free(next_y);
}