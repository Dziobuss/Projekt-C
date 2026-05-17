#include <stdio.h>
#include <string.h>
#include "config.h" 

int check_output(const char *filename, Config *config) { //Czyta końcówke pliku wyjściowego (.bin/.txt)
    size_t len = strlen(filename);
    
    
    if (len >= 4) {
        const char *ext = filename + len - 4; 
        
        if (strcmp(ext, ".txt") == 0) {
            config->binary_mode = 0;
	    return SUCCESS;
        } else if (strcmp(ext, ".bin") == 0) {
            config->binary_mode = 1;
	    return SUCCESS;
        }
    }
    fprintf(stderr, "Blad: Nieobslugiwany format pliku wyjsciowego. Uzyj .txt lub .bin\n");
    return ERR_INVALID_EXT;
} 

int parse_arguments(int argc, char *argv[], Config *config) {
    if (argc < 2) {
        printf("Uzycie: %s <plikWejsciowy.txt> [-o <sciezka>] [-a] [-b]\n", argv[0]);
        printf("Opcje:\n");
        printf("  -o <sciezka>  Sciezka pliku wyjsciowego.\n");
        printf("  -a            Wybierz alternatywny algorytm obliczania wspolrzednych.\n");
        printf("  -b            Wlacz tryb zapisu binarnego.\n");
        return 0; 
    }

    // Domyślne wartości
    config->input_file = argv[1];
    config->output_file = NULL; 
    config->use_algo_2 = 0;
    config->binary_mode = 0;

    //Opcjonalne flagi
    for (int i = 2; i < argc; i++) {
        if (strcmp(argv[i], "-a") == 0) {
            config->use_algo_2 = 1;
        } else if (strcmp(argv[i], "-b") == 0) {
            config->binary_mode = 1;
        } else if (strcmp(argv[i], "-o") == 0 && i + 1 < argc) {
            config->output_file = argv[i + 1];
            i++; 
        }
    }

    // Ustawienie domyślnych nazw plików wyjściowych
    if (config->output_file != NULL) {
        int check_status = check_output(config->output_file, config);
        if (check_status != SUCCESS) {
            return check_status;
        }
    } else {
    	if (config->binary_mode) {
        	config->output_file = "output.bin";
        } else {
            config->output_file = "output.txt";
        }
    }
    
    return 1;
}
