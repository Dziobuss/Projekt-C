#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <time.h>
#include "algorithms.h"

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

#ifndef MIN_DISTANCE
#define MIN_DISTANCE 0.0001
#endif

double min_d(double a, double b) {
	return (a < b) ? a : b;
}

// Test planarnosci
// Zwraca 1 jesli zakladamy ze graf jest planarny (nie zawsze skuteczne) lub 0 jesli na 100% nie jest planarny
int check_planarity(Graph* g) {
	int v = g->vertex_count; // Liczba wierzcholkow
	int e = g->count; // Liczba krawedzi

	// Grafy o 3 lub mniej wierzcholkach zawsze sa planarne
	if (v <= 3) return 1;

	// Test z twierdzenia Eulera
	if (e > 3 * v - 6) return 0;

	// Heurystyka z twierdzenia Kuratowskiego
	int deg_3_plus = 0;
	int deg_4_plus = 0;
	int* degrees = calloc(v, sizeof(int));

	// Zliczenie stopni wierzcholkow
	for (int i = 0; i < e; i++) {
		int idx1 = get_or_add_vertex(g, g->edges[i].v1);
		int idx2 = get_or_add_vertex(g, g->edges[i].v2);
		degrees[idx1]++;
		degrees[idx2]++;
	}

	// Sprawdzenie ile jest kandydatow do nieplanarnych struktur
	for (int i = 0; i < v; i++) {
		if (degrees[i] >= 3) deg_3_plus++;
		if (degrees[i] >= 4) deg_4_plus++;
	}
	free(degrees);

	// Jesli graf przeszedl test Eulera i nie ma materialu na stworzenie struktur K5 i K3,3 matematycznie na 100% jest planarny
	if (deg_4_plus < 5 && deg_3_plus < 6) return 1;
	// W reszcie przypadkow graf jest podejrzany ale zakladamy ze jest planarny
	return 1;
}

void algo_1_fruchterman_reingold(Graph* g, double width, double height, int iterations) {
	if (g->vertex_count == 0) return;
	srand((unsigned int)time(NULL));
	for (int i = 0; i < g->vertex_count; i++) {
		g->vertices[i].x = ((double)rand() / RAND_MAX) * width;
		g->vertices[i].y = ((double)rand() / RAND_MAX) * height;
	}
	double area = width * height;
	double k = sqrt(area / g->vertex_count);
	double t = width / 10.0;
	for (int iter = 0; iter < iterations; iter++) {
		for (int i = 0; i < g->vertex_count; i++) {
			g->vertices[i].dx = 0.0;
			g->vertices[i].dy = 0.0;
		}
		for (int v = 0; v < g->vertex_count; v++) {
			for (int u = 0; u < g->vertex_count; u++) {
				if (v == u) continue;

				double delta_x = g->vertices[v].x - g->vertices[u].x;
				double delta_y = g->vertices[v].y - g->vertices[u].y;
				double distance = sqrt(delta_x * delta_x + delta_y * delta_y);

				if (distance > MIN_DISTANCE) {
					double repulsion_force = (k * k) / distance;
					g->vertices[v].dx += (delta_x / distance) * repulsion_force;
					g->vertices[v].dy += (delta_y / distance) * repulsion_force;
				}
				else {
					// FIX 3: Jitter - zapobiega ugrzęźnięciu nałożonych na siebie wierzchołków
					g->vertices[v].dx += (((double)rand() / RAND_MAX) - 0.5) * k * 0.1;
					g->vertices[v].dy += (((double)rand() / RAND_MAX) - 0.5) * k * 0.1;
				}
			}
		}
		for (int e = 0; e < g->count; e++) {

			int v_idx = get_or_add_vertex(g, g->edges[e].v1);
			int u_idx = get_or_add_vertex(g, g->edges[e].v2);

			double delta_x = g->vertices[v_idx].x - g->vertices[u_idx].x;
			double delta_y = g->vertices[v_idx].y - g->vertices[u_idx].y;
			double distance = sqrt(delta_x * delta_x + delta_y * delta_y);

			if (distance > MIN_DISTANCE) {
				double attraction_force = (distance * distance) / k;
				g->vertices[v_idx].dx -= (delta_x / distance) * attraction_force;
				g->vertices[v_idx].dy -= (delta_y / distance) * attraction_force;
				g->vertices[u_idx].dx += (delta_x / distance) * attraction_force;
				g->vertices[u_idx].dy += (delta_y / distance) * attraction_force;
			}
		}
		for (int v = 0; v < g->vertex_count; v++) {
			double distance = sqrt(g->vertices[v].dx * g->vertices[v].dx + g->vertices[v].dy * g->vertices[v].dy);

			if (distance > MIN_DISTANCE) {
				double displacement = min_d(distance, t);
				g->vertices[v].x += (g->vertices[v].dx / distance) * displacement;
				g->vertices[v].y += (g->vertices[v].dy / distance) * displacement;
			}

			// Ograniczenie, żeby wierzchołek nie wyszedł poza pole (width x height)
			g->vertices[v].x = min_d(width - 10, g->vertices[v].x);
			g->vertices[v].x = (g->vertices[v].x < 10) ? 10 : g->vertices[v].x;

			g->vertices[v].y = min_d(height - 10, g->vertices[v].y);
			g->vertices[v].y = (g->vertices[v].y < 10) ? 10 : g->vertices[v].y;
		}

		t *= 0.95;
	}
}

// Funkcja pomocnicza do algorytmu Tutte Embedding - wyszukuje indeksy sasiadow wierzcholka v_idx
// Zwraca liczbe znalezionych sasiadow 
int get_neighbours(Graph* g, int v_idx, int* neighbours) {
	int count = 0;
	// Pobranie ID wierzcholka ktory analizujemy
	int v_id = g->vertices[v_idx].id;
	for (int i = 0; i < g->count; i++) {
		// Sprawdzenie czy analizowany wierzcholek jest jednym z krawedzi
		if (g->edges[i].v1 == v_id) {
			neighbours[count++] = get_or_add_vertex(g, g->edges[i].v2);
		}
		else if (g->edges[i].v2 == v_id) {
			neighbours[count++] = get_or_add_vertex(g, g->edges[i].v1);
		}
	}
	return count;
}

// Funkcja szukajaca cykli (naszej ramki grafu)
void dfs_find_cycle(Graph* g, int current_idx, int parent_idx, int* visited, int* path, int depth, Cycle* best_cycle) {
	// FIX 1: Ograniczenie głębokości rekurencji (zapobiega zawieszaniu się przy gęstych grafach)
	if (depth >= 20 || depth >= g->vertex_count) return;

	visited[current_idx] = 1;
	path[depth] = current_idx;

	int* neighbours = malloc(sizeof(int) * g->vertex_count);
	if (!neighbours) {
		visited[current_idx] = 0;
		return;
	}

	int n_count = get_neighbours(g, current_idx, neighbours);
	for (int i = 0; i < n_count; i++) {
		int neighbour_idx = neighbours[i];
		// Jesli sasiad to nasz "rodzic", pomijamy go
		if (neighbour_idx == parent_idx) continue;

		if (visited[neighbour_idx]) {
			int start_in_path = -1;
			for (int j = 0; j <= depth; j++) {
				if (path[j] == neighbour_idx) {
					start_in_path = j;
					break;
				}
			}
			if (start_in_path != -1) {
				int current_cycle_len = depth - start_in_path + 1;
				if (current_cycle_len > best_cycle->length) {
					best_cycle->length = current_cycle_len;
					for (int k = 0; k < current_cycle_len; k++) {
						best_cycle->nodes[k] = path[start_in_path + k];
					}
				}
			}
		}
		else {
			dfs_find_cycle(g, neighbour_idx, current_idx, visited, path, depth + 1, best_cycle);
		}
	}
	free(neighbours);
	visited[current_idx] = 0;
}

// Glowna funkcja algorytmu Tutte Embedding
void algo_2_tutte_embedding(Graph* g, double width, double height) {
	// Sprawdzenie czy graf w ogole ma sens
	if (g->vertex_count < 3) return;

	// Stworzenie struktury na nasz najlepszy cykl
	Cycle best_cycle;
	best_cycle.nodes = malloc(sizeof(int) * g->vertex_count);
	best_cycle.length = 0;

	// Tworzymy tablice pomocnicze dla DFS
	int* visited = malloc(sizeof(int) * g->vertex_count);
	int* path = malloc(sizeof(int) * g->vertex_count);

	// Okreslenie ile prob dajemy na znalezienie najwiekszego cyklu
	int attempts = g->vertex_count > 10 ? 10 : g->vertex_count;
	for (int i = 0; i < attempts; i++) {
		for (int j = 0; j < g->vertex_count; j++) {
			visited[j] = 0;
		}
		dfs_find_cycle(g, i, -1, visited, path, 0, &best_cycle);
	}

	// Sprawdzenie wyniku
	if (best_cycle.length < 3) {
		printf("Blad: Nie znaleziono cyklu w grafie. Algorytm Tutte'a wymaga ramki!\n");
		// Zwolnienie pamieci
		free(visited);
		free(path);
		free(best_cycle.nodes);
		return;
	}

	// Okreslenie wspolrzednych ramki
	int* is_boundary = calloc(g->vertex_count, sizeof(int));
	double center_x = width / 2.0;
	double center_y = height / 2.0;

	// Okreslenie promienia jako 40% wymiaru ekranu
	double radius = (width < height ? width : height) * 0.4;

	for (int i = 0; i < best_cycle.length; i++) {
		int v_idx = best_cycle.nodes[i];
		// Zaznaczenie wierzcholka jako brzegowy (nie bedzie sie ruszal)
		is_boundary[v_idx] = 1;
		// Liczymy pozycje na okregu
		double angle = (2.0 * M_PI * i) / best_cycle.length;
		g->vertices[v_idx].x = center_x + radius * cos(angle);
		g->vertices[v_idx].y = center_y + radius * sin(angle);
	}

	// FIX 4: Inicjalizacja środków dla wierzchołków wewnętrznych (znacznie przyspiesza zbieżność)
	for (int i = 0; i < g->vertex_count; i++) {
		if (is_boundary[i] == 0) {
			g->vertices[i].x = center_x;
			g->vertices[i].y = center_y;
		}
	}

	// FIX 2: Alokacja pamięci dla pętli na stercie zamiast na stosie (VLA)
	int* neighbours = malloc(sizeof(int) * g->vertex_count);
	if (neighbours == NULL) {
		printf("Blad: Brak pamieci na bufor sasiadow!\n");
		free(is_boundary);
		free(visited);
		free(path);
		free(best_cycle.nodes);
		return;
	}

	// Okreslanie wspolrzednych wierzcholkow srodkowych
	int iterations = 100;
	for (int iter = 0; iter < iterations; iter++) {
		for (int i = 0; i < g->vertex_count; i++) {
			if (is_boundary[i] == 1) continue;

			// Przygotowanie do liczenia sredniej
			double sum_x = 0.0;
			double sum_y = 0.0;

			int n_count = get_neighbours(g, i, neighbours);
			if (n_count > 0) {
				for (int j = 0; j < n_count; j++) {
					int neighbour_idx = neighbours[j];
					sum_x += g->vertices[neighbour_idx].x;
					sum_y += g->vertices[neighbour_idx].y;
				}
				// Ustawienie wierzcholka idealnie posrodku jego sasiadow
				g->vertices[i].x = sum_x / n_count;
				g->vertices[i].y = sum_y / n_count;
			}
		}
	}

	// Sprzatanie pamieci
	free(neighbours);
	free(is_boundary);
	free(visited);
	free(path);
	free(best_cycle.nodes);
}
