#ifndef CONFIG_H
#define CONFIG_H


#define SUCCESS 0
#define ERR_INPUT_READ 1
#define ERR_INVALID_FORMAT 2
#define ERR_OUTPUT_WRITE 3
#define ERR_INVALID_EXT 4

// Struktura przechowująca konfigurację programu
typedef struct {
    char *input_file;
    char *output_file;
    int use_algo_2;  // 0 - domyślny algorytm, 1 - flaga -a
    int binary_mode; // 0 - tekstowy, 1 - flaga -b
} Config;


int check_output(const char *filename, Config *config);
int parse_arguments(int argc, char *argv[], Config *config);

#endif 
