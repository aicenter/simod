
package cz.cvut.fel.aic.amodsim.ridesharing.ch;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;


public class UnionList<N> extends AbstractList<N> implements List<N> {
    private final List<N> first;
    private final List<N> second;
    private final int size;

    public UnionList(List<N> first, N second) {
        this(first,Collections.singletonList(second));
    }
    
    public UnionList(List<N> first, List<N> second) {
        Preconditions.checkNoneNull(first,second);
        this.first = first;
        this.second = second;
        this.size = first.size() + second.size();
    }
    
    @Override
    public N get(int index) {
        if (index < first.size()) {
            return first.get(index);
        } else {
            return second.get(index-first.size());
        }
    }

    @Override
    public int size() {
        return size;
    }

    public List<N> getFirstSublist() {
        return first;
    }

    public List<N> getSecondSublist() {
        return second;
    }
    
}
