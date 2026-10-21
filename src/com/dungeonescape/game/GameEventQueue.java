package com.dungeonescape.game;

/**
 * Hand-written FIFO queue of pending gameplay audio cues.
 *
 * <p>Implemented as a circular buffer over one fixed-size plain array with
 * explicit {@code front}, {@code rear}, and {@code size} fields —
 * deliberately not {@code java.util.Queue}, a {@code LinkedList}, or any
 * other collection. {@link #enqueue} and {@link #dequeue} both run in
 * O(1); the wraparound indexing {@code (index + 1) % capacity} recycles
 * the slots freed by dequeues, so the whole queue uses one allocation for
 * its lifetime.</p>
 *
 * <p><b>Full-queue policy:</b> the buffer is bounded by design so gameplay
 * can never grow it without limit. When full, {@link #enqueue} rejects the
 * new cue (returns {@code false}) and leaves every stored cue untouched,
 * so a busy tick can drop the newest duplicate cue without corrupting or
 * delaying the ones already waiting.</p>
 *
 * <p><b>Why this integration fits:</b> {@code GameSession} already produces
 * an ordered list of one-shot sound cue names ("chest", "key", "door",
 * ...) inside its update tick. Routing them through this FIFO keeps that
 * order exactly, delivers them in one batch outside the rule logic, and
 * bounds memory. Delivery happens on the Swing EDT by design — the game
 * already runs its loop on the EDT and the small batch of
 * {@code Clip.start()} calls is non-blocking — so no extra threads are
 * introduced and gameplay timing is unaffected.</p>
 *
 * <p>Empty {@link #dequeue} is safe: it returns {@code null} rather than
 * throwing or blocking. Pure game logic with no Swing dependency.</p>
 */
public class GameEventQueue {

    /** Default number of cues the queue can hold. */
    public static final int CAPACITY = 16;

    private final String[] events;
    private int front;
    private int rear;
    private int size;

    /** Creates a queue with the default capacity. */
    public GameEventQueue() {
        this(CAPACITY);
    }

    /**
     * Creates a queue with an explicit capacity (used by tests).
     *
     * @throws IllegalArgumentException when capacity is not positive
     */
    public GameEventQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Event queue capacity must be positive");
        }

        this.events = new String[capacity];
        this.front = 0;
        this.rear = 0;
        this.size = 0;
    }

    /**
     * Adds one cue to the rear of the queue.
     *
     * @param event cue name, e.g. {@code "chest"}; null and blank cues
     *              are rejected without touching the stored entries
     * @return false (silent rejection) when the queue is full or the cue
     *         is null/blank; true when the cue is stored
     */
    public boolean enqueue(String event) {
        if (size == events.length || event == null || event.isBlank()) {
            return false;
        }

        events[rear] = event;
        rear = (rear + 1) % events.length;
        size++;

        return true;
    }

    /**
     * Removes and returns the cue at the front of the queue.
     *
     * @return the oldest still-queued cue, or {@code null} when the
     *         queue is empty (safe: no exception, no blocking)
     */
    public String dequeue() {
        if (size == 0) {
            return null;
        }

        String oldest = events[front];
        events[front] = null;
        front = (front + 1) % events.length;
        size--;

        return oldest;
    }

    /** Number of cues currently waiting in the queue. */
    public int size() {
        return size;
    }

    /** True when no cue is waiting. */
    public boolean isEmpty() {
        return size == 0;
    }
}
