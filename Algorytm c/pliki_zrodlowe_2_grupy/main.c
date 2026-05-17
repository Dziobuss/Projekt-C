#include <stdio.h>
#include <stdlib.h>
#include "config.h"
#include "parser.h" 
#include "algorithms.h"

int main(int argc, char *argv[]) {
    Config config;

    int parse_status = parse_arguments(argc, argv, &config);
    if (parse_status == 0) {
        return SUCCESS;
    } else if (parse_status != 1) {
        return parse_status;
    }

    FILE *in_file = fopen(config.input_file, "r");
    if (in_file == NULL) {
        fprintf(stderr, "Blad: Nie mozna otworzyc pliku wejsciowego: %s\n", config.input_file);
        return ERR_INPUT_READ;
    }

    //wczytywanie danych
    Graph my_graph;
    init_graph(&my_graph); //inicjalizacja pamieci

    int load_status = load_graph(in_file, &my_graph); // Czytanie plik
    fclose(in_file); 

    if (load_status != SUCCESS) {
        free_graph(&my_graph); 
        return load_status;    
    }

    printf("Sukces! Wczytano %d krawedzi z pliku.\n", my_graph.count);
    if (check_planarity(&my_graph) == 0) {
        printf("\n[OSTRZEZENIE] Graf zlamal warunek Eulera (jest za gesty)!\n");
        printf("Na 100%% nie jest planarny. Krawedzie beda sie przecinac na rysunku.\n\n");
    } else {
        printf("Test planarnosci: Pozytywny (zakladamy brak przeciec).\n");
    }
    printf("Obliczam wspolrzedne algorytmem...\n"); //Uruchomienie algorytm�w
    if (config.use_algo_2) {
        // Odpalamy algorytm 2 - Tutte Embedding (flaga -a)
        printf("Uzywam algorytmu alternatywnego Tutte Embedding.\n");
	algo_2_tutte_embedding(&my_graph, 800.0, 600.0);
    }
    else {
        // Domy�lny algorytm: Fruchterman-Reingold
        // Przyjmujemy plansz� 800x600 i robimy 100 krok�w symulacji
        algo_1_fruchterman_reingold(&my_graph, 800.0, 600.0, 100);
    }

    // Testujemy
    printf("--- WYNIKI WSPOLRZEDNYCH ---\n");
    for (int i = 0; i < my_graph.vertex_count; i++) {
        printf("Wierzcholek %d: X=%.2f, Y=%.2f\n",
            my_graph.vertices[i].id,
            my_graph.vertices[i].x,
            my_graph.vertices[i].y);
    }
    printf("----------------------------\n");
    int save_status = save_graph(config.output_file, &my_graph, config.binary_mode);
    if (save_status != SUCCESS) {
        free_graph(&my_graph);
        return save_status;
    }
    free_graph(&my_graph);

    return SUCCESS;
}
