#include <stddef.h>

void *flite_init(void) { return NULL; }
int flite_voice_select(const char *name) { (void)name; return 0; }
int flite_file_to_speech(const char *filename, void *voice, const char *outtype) { (void)filename; (void)voice; (void)outtype; return 0; }
int flite_text_to_speech(const char *text, void *voice, const char *outtype) { (void)text; (void)voice; (void)outtype; return 0; }
