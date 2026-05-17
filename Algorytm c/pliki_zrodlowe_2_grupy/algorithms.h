#ifndef ALGORITHMS_H
#define ALGORITHMS_H

#include "parser.h"

// Struktura do przechowywania informacji o znalezionym cyklu do algorytmu Tutte - Embedding
typedef struct {
    int* nodes;    // Tablica indeksów wierzchołków w cyklu
    int length;    // Długość cyklu
} Cycle;

void algo_1_fruchterman_reingold(Graph* g, double width, double height, int iterations);

void algo_2_tutte_embedding(Graph* g, double width, double height);

int check_planarity(Graph* g);

#endif
