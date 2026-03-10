package org.loger.util;

import java.util.ArrayList;
import java.util.List;

public class CircularBuffer<T> {
    private final Object[] buffer;
    private final int capacity;
    private int head;
    private int size;

    public CircularBuffer(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1");
        }
        this.capacity = capacity;
        this.buffer = new Object[capacity];
        this.head = 0;
        this.size = 0;
    }

    public void add(T item) {
        this.buffer[this.head] = item;
        this.head = (this.head + 1) % this.capacity;
        if (this.size < this.capacity) {
            this.size++;
        }
    }

    public T get(int i) {
        if (i < 0 || i >= this.size) {
            throw new IndexOutOfBoundsException("Index: " + i + ", Size: " + this.size);
        }
        return (T) this.buffer[(((this.head - this.size) + i) + this.capacity) % this.capacity];
    }

    public List<T> toList() {
        List<T> result = new ArrayList<>(this.size);
        for (int i = 0; i < this.size; i++) {
            result.add(get(i));
        }
        return result;
    }

    public void clear() {
        for (int i = 0; i < this.capacity; i++) {
            this.buffer[i] = null;
        }
        this.head = 0;
        this.size = 0;
    }

    public int size() {
        return this.size;
    }

    public int capacity() {
        return this.capacity;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public boolean isFull() {
        return this.size == this.capacity;
    }
}
