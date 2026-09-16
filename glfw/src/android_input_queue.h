//
// Created by maks on 11.04.2026.
//

#ifndef POJAVLAUNCHER_ANDROID_INPUT_QUEUE_H
#define POJAVLAUNCHER_ANDROID_INPUT_QUEUE_H

#include <stdint.h>
#include <jni.h>
#include <pthread.h>
#include <stdbool.h>

typedef struct {
    int32_t type;
    union {
        struct { int32_t glfw_code, code, state, mods; jchar codepoint; } k;
        struct { int32_t button, state, mods; } m;
        struct { int32_t length, mods; jchar *codepoints; } u;
        struct { double xscroll, yscroll; } s;
    };
} input_event_t;

typedef struct {
    _Atomic uint16_t events_total;
    input_event_t events[512];
} queue_t;

typedef struct {
    pthread_mutex_t wait_mutex;
    pthread_cond_t wait_cond;
    queue_t queues[2];
    _Atomic unsigned char index;
} queue_top_t;

typedef void (*dequeue_callback_t)(input_event_t*);

bool _input_queue_init(queue_top_t* top);
void _input_queue_destroy(queue_top_t* top);
void _input_queue_push(queue_top_t* top, input_event_t* event);
void _input_queue_dequeue(queue_top_t* top, dequeue_callback_t cb);
void _input_queue_timedwait(queue_top_t* top, dequeue_callback_t cb, struct timespec* timeout);
void _input_queue_wait(queue_top_t* top, dequeue_callback_t cb);
void _input_queue_wait_unlock(queue_top_t* top);


#endif //POJAVLAUNCHER_ANDROID_INPUT_QUEUE_H
