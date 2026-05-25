#include "graph.h"

void save_to_file(const char* fn, Vertex* v, int n) {
    FILE* f = fopen(fn, "w");
    if (!f) return;
    for (int i = 0; i < n; i++) fprintf(f, "%d %f %f\n", v[i].id, v[i].x, v[i].y);
    fclose(f);
}

void save_to_binary(const char* fn, Vertex* v, int n) {
    FILE* f = fopen(fn, "wb");
    if (!f) return;
    fwrite(&n, sizeof(int), 1, f);
    fwrite(v, sizeof(Vertex), n, f);
    fclose(f);
}

int main(int argc, char *argv[]) {
    srand(time(NULL)); // Inicjalizacja generatora liczb losowych
    
    char *input = NULL, *output = NULL, *algo = "smacof", *format = "txt";
    int iter = 1000;

    for (int i = 1; i < argc; i++) { // komenda help
        if (strcmp(argv[i], "-h") == 0) {
            printf("Uzycie: %s -i <in> -o <out> -a <smacof|fr> [-n iter] [-f txt|bin]\n", argv[0]);
            return 0;
        }
        if (strcmp(argv[i], "-i") == 0 && i + 1 < argc) input = argv[++i]; // czytanie flag
        else if (strcmp(argv[i], "-o") == 0 && i + 1 < argc) output = argv[++i];
        else if (strcmp(argv[i], "-a") == 0 && i + 1 < argc) algo = argv[++i];
        else if (strcmp(argv[i], "-n") == 0 && i + 1 < argc) iter = atoi(argv[++i]);
        else if (strcmp(argv[i], "-f") == 0 && i + 1 < argc) format = argv[++i];
    }

    if (!input || !output) return 1; // sprawdzenie czy jest plik wejsciowy i wyjsciowy

    Edge edges[500]; int num_e = 0, node_map[500], num_v = 0; // rezerwacja miejsc na krawedzie i wierzcholki
    FILE* fin = fopen(input, "r");
    if (!fin) {
        fprintf(stderr, "Blad: Nie mozna otworzyc pliku (Kod 1)\n");
        return 1;
    }

    char e_name[20];
    while (fscanf(fin, "%s %d %d %lf", e_name, &edges[num_e].u, &edges[num_e].v, &edges[num_e].weight) == 4) { // wczytywanie danych
        int u_f = 0, v_f = 0;
        for(int j=0; j<num_v; j++) { // przeszukiwanie tablicy w poszukiwaniu wierzchołka aby go nie dublować
            if(node_map[j] == edges[num_e].u) u_f = 1;
            if(node_map[j] == edges[num_e].v) v_f = 1;
        }
        if(!u_f) node_map[num_v++] = edges[num_e].u; // dodawanie wierzcholka do tablicy jesli nie zostal znaleziony
        if(!v_f) node_map[num_v++] = edges[num_e].v;
        num_e++;
        if (num_e >= 500) break;
    }
    fclose(fin);

    if (num_v == 0) { // sprawdzenie czy jakikowliek wierzcholek zostal wczytany
        fprintf(stderr, "Blad: Bledny format danych (Kod 2)\n");
        return 2;
    }

    Vertex* vertices = malloc(num_v * sizeof(Vertex)); // rezerwacja pamieci na wierzcholki
    if (!vertices) return 4;

    for(int i=0; i<num_v; i++) { // nadawanie losowych wspolrzednych wierzchlokom
        vertices[i].id = node_map[i];
        vertices[i].x = (double)(rand() % 1000) - 500.0;
        vertices[i].y = (double)(rand() % 1000) - 500.0;
    }

    if (strcmp(algo, "smacof") == 0) {
        calculate_smacof(vertices, num_v, edges, num_e, iter);
    } else if (strcmp(algo, "fr") == 0) {
        calculate_fruchterman(vertices, num_v, edges, num_e, iter);
    } else {
        fprintf(stderr, "Blad: Nieznany algorytm. Uzyj smacof lub fr (Kod 3)\n");
        free(vertices);
        return 3;
    }

    if (strcmp(format, "bin") == 0) save_to_binary(output, vertices, num_v);
    else save_to_file(output, vertices, num_v);

    free(vertices);
    return 0;
}