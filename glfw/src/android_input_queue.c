//
// Created by maks on 11.04.2026.
//

#include <string.h>
#include <stdatomic.h>

#include "android_input_queue.h"

bool _input_queue_init(queue_top_t* top) {
    top->index = 0;
    for(int i = 0; i < 2; i++) top->queues[i].events_total = 0;
    if(pthread_mutex_init(&top->wait_mutex, NULL) != 0) return false;

    pthread_condattr_t attr;
    if(pthread_condattr_init(&attr) != 0) goto fail;
    if(pthread_condattr_setclock(&attr, CLOCK_MONOTONIC) != 0) goto fail2;

    if(pthread_cond_init(&top->wait_cond, &attr) != 0) goto fail2;

    pthread_condattr_destroy(&attr);
    return true;
    fail2:
    pthread_condattr_destroy(&attr);
    fail:
    pthread_mutex_destroy(&top->wait_mutex);
    return false;
}

void _input_queue_destroy(queue_top_t* top) {
    pthread_mutex_destroy(&top->wait_mutex);
    pthread_cond_destroy(&top->wait_cond);
}

void _input_queue_wait_unlock(queue_top_t* top) {
    pthread_cond_broadcast(&top->wait_cond);
}

void _input_queue_push(queue_top_t* top, input_event_t* event) {
    queue_t *current = &top->queues[atomic_load(&top->index)];
    uint16_t ev_idx = atomic_fetch_add(&current->events_total, 1);
    input_event_t *put_event = &current->events[ev_idx];
    memcpy(put_event, event, sizeof(input_event_t));
    pthread_cond_broadcast(&top->wait_cond);
}

void _input_queue_dequeue(queue_top_t* top, dequeue_callback_t cb) {
    // Atomically switch queue 0 to 1 (or 1 to 0) by flipping the bit
    queue_t *current = &top->queues[atomic_fetch_xor(&top->index, 0b1)];
    for(uint16_t i = 0; i < current->events_total; i++) {
        cb(&current->events[i]);
    }
    current->events_total = 0;
}

void _input_queue_timedwait(queue_top_t* top, dequeue_callback_t cb, struct timespec* timeout) {
    queue_t *current = &top->queues[top->index];
    if(current->events_total == 0) {
        pthread_mutex_lock(&top->wait_mutex);
        pthread_cond_timedwait(&top->wait_cond, &top->wait_mutex, timeout);
        pthread_mutex_unlock(&top->wait_mutex);
    }
    _input_queue_dequeue(top, cb);
}

void _input_queue_wait(queue_top_t* top, dequeue_callback_t cb) {
    queue_t *current = &top->queues[top->index];
    if(current->events_total == 0) {
        pthread_mutex_lock(&top->wait_mutex);
        pthread_cond_wait(&top->wait_cond, &top->wait_mutex);
        pthread_mutex_unlock(&top->wait_mutex);
    }
    _input_queue_dequeue(top, cb);
}

