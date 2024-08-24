package graph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.IntBinaryOperator;

/**
 * A min priority queue of distinct elements of type `KeyType` associated with (extrinsic) integer
 * priorities, implemented using a binary heap paired with a hash table.
 */
public class HeapMinQueue<KeyType> implements MinQueue<KeyType> {

    /**
     * Pairs an element `key` with its associated priority `priority`.
     */
    private record Entry<KeyType>(KeyType key, int priority) {
        // Note: This is equivalent to declaring a static nested class with fields `key` and
        //  `priority` and a corresponding constructor and observers, overriding `equals()` and
        //  `hashCode()` to depend on the fields, and overriding `toString()` to print their values.
        // https://docs.oracle.com/en/java/javase/17/language/records.html
    }

    /**
     * Associates each element in the queue with its index in `heap`.  Satisfies
     * `heap.get(index.get(e)).key().equals(e)` if `e` is an element in the queue. Only maps
     * elements that are in the queue (`index.size() == heap.size()`).
     */
    private final Map<KeyType, Integer> index;

    /**
     * Sequence representing a min-heap of element-priority pairs.  Satisfies
     * `heap.get(i).priority() >= heap.get((i-1)/2).priority()` for all `i` in `[1..heap.size()]`.
     */
    private final ArrayList<Entry<KeyType>> heap;

    /**
     * Assert that our class invariant is satisfied.  Returns true if it is (or if assertions are
     * disabled).
     */
    private boolean checkInvariant() {
        for (int i = 1; i < heap.size(); ++i) {
            int p = (i - 1) / 2;
            assert heap.get(i).priority() >= heap.get(p).priority();

            assert index.get(heap.get(i).key()) == i;
        }
        assert index.size() == heap.size();
        return true;
    }

    /**
     * Create an empty queue.
     */
    public HeapMinQueue() {
        index = new HashMap<>();
        heap = new ArrayList<>();
        assert checkInvariant();
    }

    /**
     * Return whether this queue contains no elements.
     */
    @Override
    public boolean isEmpty() {
        return heap.isEmpty();
    }

    /**
     * Return the number of elements contained in this queue.
     */
    @Override
    public int size() {
        return heap.size();
    }

    /**
     * Return an element associated with the smallest priority in this queue.  This is the same
     * element that would be removed by a call to `remove()` (assuming no mutations in between).
     * Throws NoSuchElementException if this queue is empty.
     */
    @Override
    public KeyType get() {
        // Propagate exception from `List::getFirst()` if empty.
        return heap.getFirst().key();
    }

    /**
     * Return the minimum priority associated with an element in this queue.  Throws
     * NoSuchElementException if this queue is empty.
     */
    @Override
    public int minPriority() {
        return heap.getFirst().priority();
    }

    /**
     * If `key` is already contained in this queue, change its associated priority to `priority`.
     * Otherwise, add it to this queue with that priority.
     */
    @Override
    public void addOrUpdate(KeyType key, int priority) {
        if (!index.containsKey(key)) {
            add(key, priority);
        } else {
            update(key, priority);
        }
    }

    /**
     * Remove and return the element associated with the smallest priority in this queue.  If
     * multiple elements are tied for the smallest priority, an arbitrary one will be removed.
     * Throws NoSuchElementException if this queue is empty.
     */
    @Override
    public KeyType remove() {
        if (heap.isEmpty()) {
            throw new NoSuchElementException("The heap is empty");
        }

        //Get the current min key
        KeyType min = heap.get(0).key();
        //Remove the last element from the heap
        Entry<KeyType> lastElement = heap.remove(size() - 1);
        index.remove(min);
        //Check whether it is empty
        if (!heap.isEmpty()) {
            //Set the last element to the start of the heap
            heap.set(0, lastElement);
            //Update the index
            index.put(lastElement.key(), 0);
            //We bubble down the element that we misplaces
            bubbleDown(0);
        }

        //Then return the min that we saved
        return min;
    }

    /**
     * Remove all elements from this queue (making it empty).
     */
    @Override
    public void clear() {
        index.clear();
        heap.clear();
        assert checkInvariant();
    }

    /**
     * Swap the Entries at indices `i` and `j` in `heap`, updating `index` accordingly.  Requires `0
     * <= i,j < heap.size()`.
     */
    private void swap(int i, int j) {
        // Swap the entries in the heap
        Entry<KeyType> iEntry = heap.get(i);
        Entry<KeyType> jEntry = heap.get(j);

        heap.set(i, jEntry);
        heap.set(j, iEntry);

        // Update the index map correctly
        index.put(iEntry.key(), j);
        index.put(jEntry.key(), i);
    }


    /**
     * Add element `key` to this queue, associated with priority `priority`.  Requires `key` is not
     * contained in this queue.
     */
    private void add(KeyType key, int priority) {
        assert !index.containsKey(key);

        //Add the new Entry to the heap
        heap.add(new Entry<>(key, priority));
        //Get the last index
        int pos = heap.size() - 1;
        //Put the new Entry to the index
        index.put(key, pos);
        //Bubble up to restore the invariant
        bubbleUp(pos);

        //Check invariant
        assert checkInvariant();
    }

    /**
     * Change the priority associated with element `key` to `priority`.  Requires that `key` is
     * contained in this queue.
     */
    private void update(KeyType key, int priority) {
        assert index.containsKey(key);

        //Get the position of the key from the 'index'
        int pos = index.get(key);
        //Save the old priority
        int oldPriority = heap.get(pos).priority();
        //Set the new entry with the new priority at the required position
        heap.set(pos, new Entry<>(key, priority));
        //Check if the heap is now not following the order invaraint
        if (oldPriority < priority) {
            bubbleDown(pos);
        } else {
            bubbleUp(pos);
        }
        //Finally check the invariant
        assert checkInvariant();
    }

    /**
     * Bubbles up the element at index `k` in the heap to its correct position. Precondition:  0 <=
     * k < size and every subtree, except possibly the one rooted at `k`, adheres to the min-heap's
     * ordering rules.
     */
    private void bubbleUp(int k) {

        //Initialize a new variable, precaution
        int j = k;
        //Get the parent
        int parent = (j - 1) / 2;
        while (j > 0 && heap.get(j).priority() < heap.get(parent).priority()) {
            swap(j, parent);
            j = parent;
            parent = (j - 1) / 2;
        }
    }
    //Helpers for add and remove

    /**
     * Bubbles down the element at index `k` in the heap to its correct position. If both children
     * have the same priority, the left child is chosen for bubbling down. Precondition:  0 <= k <
     * size and every subtree, except possibly the one rooted at `k`, adheres to the min-heap's
     * ordering rules.
     */
    private void bubbleDown(int k) {
        int left = 2 * k + 1;
        while (left < heap.size()) {
            //We must check if the right child exists before setting it
            if (left + 1 < heap.size() && heap.get(left).priority() > heap.get(left + 1)
                    .priority()) {
                left += 1;
            }
            //If by chance the order invariant is already satisfied, no need to check more
            if (heap.get(k).priority() <= heap.get(left).priority()) {
                return;
            }
            //If not, swap the values
            swap(k, left);
            //Now the 'k' would be equal to the 'left' of the previous iteration
            k = left;
            //Finally, the left would be set as the next iteration's left child
            left = 2 * k + 1;
        }

    }
}
