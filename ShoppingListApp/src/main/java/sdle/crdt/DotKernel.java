package sdle.crdt;

import javafx.util.Pair;

import java.io.Serializable;
import java.util.*;

public class DotKernel implements Serializable {
    private HashMap<Pair<String, Integer>, Integer> ds = new HashMap<>();
    private DotContext c;

    // Constructors
    public DotKernel() {
        this.c = new DotContext();
    }

    public DotKernel(DotContext jointc) {
        this.c = jointc;
    }

    public DotKernel copy(DotKernel adk) {
        if (this == adk) return this;
        this.c = adk.c;
        this.ds = new HashMap<>(adk.ds);
        return this;
    }

    public void updateContext(DotContext c) {
        this.c.join(c);
    }


    public DotContext getContext() {
        return c;
    }

    public HashMap<Pair<String, Integer>, Integer> getDs() {
        return ds;
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder("Kernel: DS ( ");
        for (Map.Entry<Pair<String, Integer>, Integer> entry : ds.entrySet()) {
            output.append(entry.getKey().getKey())
                    .append(":")
                    .append(entry.getKey().getValue())
                    .append("->")
                    .append(entry.getValue())
                    .append(" ");
        }
        output.append(") ");
        output.append(c.toString());
        return output.toString();
    }

    public void join(DotKernel o) {
        if (this == o) return; // Join is idempotent, but just don't do it.

        Iterator<Map.Entry<Pair<String, Integer>, Integer>> it = ds.entrySet().iterator(), it2 = ds.entrySet().iterator();
        Iterator<Map.Entry<Pair<String, Integer>, Integer>> ito = o.ds.entrySet().iterator(), ito2 = o.ds.entrySet().iterator();

        Map.Entry<Pair<String, Integer>, Integer> currentEntryItValue = null;
        Map.Entry<Pair<String, Integer>, Integer> currentEntryItoValue = null;
        List<Pair<String, Integer>> dotRemove = new ArrayList<>();
        List<Map.Entry<Pair<String, Integer>, Integer>> dotAdd = new ArrayList<>();

        if(it.hasNext()){
            currentEntryItValue = it2.next();
        }
        if(ito.hasNext()){
            currentEntryItoValue = ito2.next();
        }


        while (it.hasNext() || ito.hasNext()){
            if (it.hasNext() && (!ito.hasNext() || comparePairs(currentEntryItValue.getKey(), currentEntryItoValue.getKey()))) {
                if (o.c.dotin(currentEntryItValue.getKey())) {
                    dotRemove.add(currentEntryItValue.getKey());
                }
                if(it2.hasNext()) currentEntryItValue = it2.next();
                it.next();


            }
            else if (ito.hasNext() && (!it.hasNext() || comparePairs(currentEntryItoValue.getKey(), currentEntryItValue.getKey()))) {
                if (!c.dotin(currentEntryItoValue.getKey())) {
                    // If I don't know, import
                    dotAdd.add(currentEntryItoValue);

                }
                if(ito2.hasNext()) currentEntryItoValue = ito2.next();
                ito.next();
            } else if (it.hasNext() && ito.hasNext()) {
                // Dot in both
                if(ito2.hasNext()) currentEntryItoValue = ito2.next();
                if(it2.hasNext()) currentEntryItValue = it2.next();
                it.next();
                ito.next();
            }
        }
        for(Pair<String, Integer> dot : dotRemove){
            ds.remove(dot);
        }

        for(Map.Entry<Pair<String, Integer>, Integer> dot:dotAdd){
            ds.put(dot.getKey(), dot.getValue());
        }
        this.c.join(o.c);

    }

    private boolean comparePairs(Pair<String, Integer> p1, Pair<String, Integer> p2) {
        if(p1.getKey().equals(p2.getKey())) {
            return p1.getValue() < p2.getValue();
        }
        return p1.getKey().compareTo(p2.getKey()) < 0;
    }


    public DotKernel add(String id, Integer val) {
        DotKernel res = new DotKernel();
        // Get new dot
        Pair<String, Integer> dot = c.makedot(id);
        // Add under new dot
        ds.put(dot, val);
        // Make delta
        res.ds.put(dot, val);
        res.c.insertdot(dot, true);
        return res;
    }

    public Pair<String, Integer> dotadd(String id, Integer val) {
        // Get new dot
        Pair<String, Integer> dot = c.makedot(id);
        // Add under new dot
        ds.put(dot, val);
        return dot;
    }

    public DotKernel rmv(Integer val) {
        DotKernel res = new DotKernel();
        Iterator<Map.Entry<Pair<String, Integer>, Integer>> dsIterator = ds.entrySet().iterator();
        while (dsIterator.hasNext()) {
            Map.Entry<Pair<String, Integer>, Integer> entry = dsIterator.next();
            if (entry.getValue().equals(val)) {
                res.c.insertdot(entry.getKey(), false);
                dsIterator.remove();
            }
        }
        res.c.compact();
        return res;
    }


    public DotKernel rmv(Pair<String, Integer> dot) {
        DotKernel res = new DotKernel();
        Iterator<Map.Entry<Pair<String, Integer>, Integer>> dsit = ds.entrySet().iterator();

        while (dsit.hasNext()) {
            Map.Entry<Pair<String, Integer>, Integer> entry = dsit.next();
            if (entry.getKey().equals(dot)) { // found it
                res.c.insertdot(entry.getKey(), false); // result knows removed dots
                dsit.remove();
            }
        }

        res.c.compact(); // Attempt compactation
        return res;
    }

    public DotKernel rmv() {
        DotKernel res = new DotKernel();

        for (Map.Entry<Pair<String, Integer>, Integer> entry : ds.entrySet()) {
            res.c.insertdot(entry.getKey(), false);
        }

        res.c.compact();
        ds.clear(); // Clear the payload, but remember context
        return res;
    }

}
