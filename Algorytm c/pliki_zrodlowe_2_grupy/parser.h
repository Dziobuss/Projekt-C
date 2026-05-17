#ifndef PARSER_H
#define PARSER_H

#include <stdio.h>
#include "config.h" 

#define MAX_EDGE_NAME 64

//Struktura pojedynczego wierzchołka
typedef struct {
    int id;         
    double x;         
    double y;         
    double dx;        
    double dy;      
} Vertex;

// Struktura pojedynczej krawędzi
typedef struct {
    char name[MAX_EDGE_NAME];
    int v1;
    int v2;
    double weight;
} Edge;

// Struktura przechowująca wszystkie wczytane krawędzie
typedef struct {
    Edge *edges;      
    int count;        
    int capacity;  
    Vertex* vertices;
    int vertex_count;
    int vertex_capacity;
} Graph;

int get_or_add_vertex(Graph* g, int id);
void init_graph(Graph *g);
int load_graph(FILE *file, Graph *g);
int save_graph(const char *filename, Graph *g, int binary_mode);
void free_graph(Graph *g);

#endif 
