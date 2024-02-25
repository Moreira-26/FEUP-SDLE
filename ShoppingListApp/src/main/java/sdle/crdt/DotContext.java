package sdle.crdt;
import java.io.Serializable;
import java.util.*;

import javafx.util.Pair;

import static java.lang.Math.max;


public class DotContext  implements Serializable {
    private Map<String, Integer> cc;    // Compact causal context
    private Set<Pair<String, Integer>> dc;  // Dot cloud

    // Default constructor
    public DotContext() {
        cc = new HashMap();
        dc = new HashSet();
    }

    // Copy constructor
    public DotContext(DotContext other) {
        this.cc = new HashMap(other.cc);
        this.dc = new HashSet(other.dc);
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder("Context:");
        output.append(" CC ( ");
        for (Map.Entry<String, Integer> entry : cc.entrySet()) {
            output.append(entry.getKey()).append(":").append(entry.getValue()).append(" ");
        }
        output.append(") DC ( ");
        for (Pair<String, Integer> pair : dc) {
            output.append(pair.getKey()).append(":").append(pair.getValue()).append(" ");
        }
        output.append(")");
        return output.toString();
    }

    public boolean dotin(Pair<String, Integer> d) {
        Integer ccValue = cc.get(d.getKey());
        return (ccValue != null && d.getValue() <= ccValue) || dc.contains(d);
    }

    // compact equivalent
    public void compact() {
        boolean flag;
        do {
            flag = false;
            Iterator<Pair<String, Integer>> iterator = dc.iterator();
            while (iterator.hasNext()) {
                Pair<String, Integer> pair = iterator.next();
                Integer ccValue = cc.get(pair.getKey());
                if (ccValue == null) {
                    if (pair.getValue() == 1) {
                        cc.put(pair.getKey(), pair.getValue());
                        iterator.remove();
                        flag = true;
                    }
                } else if (pair.getValue() == ccValue + 1) {
                    cc.put(pair.getKey(), ccValue + 1);
                    iterator.remove();
                    flag = true;
                } else if (pair.getValue() <= ccValue) {
                    iterator.remove();
                } else {
                    iterator.next();
                }
            }
        } while (flag);
    }

    public Pair<String, Integer> makedot(String id) {
        Integer ccValue = cc.get(id);
        if (ccValue == null) {
            cc.put(id, 1);
        } else {
            cc.put(id, ccValue + 1);
        }
        return new Pair(id, cc.get(id));
    }

    public void insertdot(Pair<String, Integer> d, boolean compactnow) {
        dc.add(d);
        if (compactnow) compact();
    }

    public void join(DotContext o) {
        if (this == o) return;  // Join is idempotent, but just don't do it.


        Iterator<Map.Entry<String, Integer>> mit= cc.entrySet().iterator() , mit2 = cc.entrySet().iterator();
        Iterator<Map.Entry<String, Integer>> mito = o.cc.entrySet().iterator(), mito2 = o.cc.entrySet().iterator();
        Map.Entry<String, Integer> currentEntryMitValue = null;
        Map.Entry<String, Integer> currentEntryMitoValue = null;
        List<Map.Entry<String, Integer>> ccAdd = new ArrayList<>();
        if(mit.hasNext()){
            currentEntryMitValue = mit2.next();
        }
        if(mito.hasNext()){
            currentEntryMitoValue = mito2.next();
        }

        while(mit.hasNext() || mito.hasNext()){

            if (mit.hasNext() && (!mito.hasNext() || (currentEntryMitValue.getKey().compareTo(currentEntryMitoValue.getKey()) < 0 ))) {
                if(mit2.hasNext()) currentEntryMitValue = mit2.next();
                mit.next();

            } else if (mito.hasNext() && (!mit.hasNext() || (currentEntryMitoValue.getKey().compareTo(currentEntryMitValue.getKey()) < 0 ))) {
                ccAdd.add(currentEntryMitoValue);
                if(mito2.hasNext()) currentEntryMitoValue = mito2.next();
                mito.next();

            } else if (mit.hasNext() && mito.hasNext()) {
                cc.put(currentEntryMitValue.getKey(), max(currentEntryMitoValue.getValue(), currentEntryMitValue.getValue()));
                if(mit2.hasNext()) currentEntryMitValue = mit2.next();
                if(mito2.hasNext()) currentEntryMitoValue = mito2.next();
                mit.next();
                mito.next();
            }
        }

        for(Map.Entry<String, Integer> dot: ccAdd){
            cc.put(dot.getKey(),dot.getValue());
        }

        for (Pair<String, Integer> e : o.dc) {
            insertdot(e, false);
        }

        compact();

    }
}
