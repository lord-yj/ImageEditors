package selector;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Models a selection tool that connects each added point with a straight line.
 */
public class PointToPointSelectionModel extends SelectionModel {

    public PointToPointSelectionModel(boolean notifyOnEdt) {
        super(notifyOnEdt);
    }

    public PointToPointSelectionModel(SelectionModel copy) {
        super(copy);
    }

    /**
     * Return a straight line segment from our last point to `p`.
     */
    @Override
    public PolyLine liveWire(Point p) {
        return new PolyLine(this.lastPoint(), p);
    }

    /**
     * Append a straight line segment to the current selection path connecting its end with `p`.
     */
    @Override
    protected void appendToSelection(Point p) {
        this.selection.add(new PolyLine(lastPoint(), p));
        //Remember that they are subclasses of each other
    }

    /**
     * Move the starting point of the segment of our selection with index `index` to `newPos`,
     * connecting to the end of that segment with a straight line and also connecting `newPos` to
     * the start of the previous segment (wrapping around) with a straight line (these straight
     * lines replace both previous segments).  Notify listeners that the "selection" property has
     * changed.
     */
    public void movePoint(int index, Point newPos) {
        if (state() != SelectionState.SELECTED) {
            throw new IllegalStateException("May not move point in state " + state());
        }
        if (index < 0 || index >= selection.size()) {
            throw new IllegalArgumentException("Invalid segment index " + index);
        }

        ListIterator<PolyLine> iterator = this.selection.listIterator();
        int count = 0;  // Initialize the count for index tracking

        PolyLine previousSegment = null; // To keep track of the segment before the current one

        while (iterator.hasNext()) {
            PolyLine current = iterator.next();

            if (count == index) {
                // Set the current segment with the new start position
                iterator.set(new PolyLine(newPos, current.end()));
                if (index == 0) {
                    start = new Point(newPos.x, newPos.y);
                }
                if (index > 0) {
                    // Adjust the previous segment if it's not the first
                    if (previousSegment != null) {
                        iterator.previous();
                        iterator.previous();
                        iterator.set(new PolyLine(previousSegment.start(), newPos));
                        iterator.next(); // Restore iterator position to current
                        iterator.next(); // Move to next after current for correct iteration continuation
                    }
                }
            }

            previousSegment = current;
            count++;

            if (count > index) {
                break;
            }
        }

        if (index == 0 && selection.size() > 1) {
            // Move to the end of the list
            while (iterator.hasNext()) {
                iterator.next();
            }
            iterator.previous();  // Step back to the last element
            iterator.set(new PolyLine(previousSegment.start(), newPos));
        }

        propSupport.firePropertyChange("selection", null, this.selection);
    }

}


