package sdle.crdt;

import javafx.util.Pair;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.max;

public class CCounter implements Serializable {
    private DotKernel dk;
    private String id;

    public CCounter() {
        // Only for deltas and those should not be mutated
        dk = new DotKernel();
    }

    public CCounter(String k) {
        // Mutable replicas need a unique id
        dk = new DotKernel();
        id = k;
    }

    public CCounter(String k, DotContext jointc) {
        id = k;
        dk = new DotKernel(jointc);
    }

    public DotContext context() {
        return dk.getContext();
    }

    public void updateContext(DotContext c) {
        dk.updateContext(c);
    }

    @Override
    public String toString() {
        return "CausalCounter:" + dk.toString();
    }

    public CCounter inc(Integer val) {
        CCounter r = new CCounter();
        Set<Pair<String, Integer>> dots = new HashSet<>();
        Integer base = Integer.MIN_VALUE; // typically 0
        for (Map.Entry<Pair<String, Integer>, Integer> dsit : dk.getDs().entrySet()) {
            if (dsit.getKey().getKey().equals(id)) {
                base = max(base, dsit.getValue());
                dots.add(dsit.getKey());
            }
        }
        for (Pair<String, Integer> dot : dots) {
            r.dk.join(dk.rmv(dot));
        }
        if(base == Integer.MIN_VALUE)
            base = 0;
        r.dk.join(dk.add(id, base + val));
        return r;
    }

    public CCounter dec(Integer val) {
        CCounter r = new CCounter();
        Set<Pair<String, Integer>> dots = new HashSet<>();
        Integer base = Integer.MIN_VALUE; // typically 0
        for (Map.Entry<Pair<String, Integer>, Integer> dsit : dk.getDs().entrySet()) {
            if (dsit.getKey().getKey().equals(id)) {
                base = max(base, dsit.getValue());
                dots.add(dsit.getKey());
            }
        }
        for (Pair<String, Integer> dot : dots) {
            r.dk.join(dk.rmv(dot));
        }
        if(base == Integer.MIN_VALUE)
            base = 0;
        r.dk.join(dk.add(id, base - val));
        return r;
    }

    public CCounter reset() {
        CCounter r = new CCounter();
        r.dk = dk.rmv();
        return r;
    }

    public Integer read() {
        Integer v = 0; // Usually 0
        for (Map.Entry<Pair<String, Integer>, Integer> dse : dk.getDs().entrySet()) {
            v += dse.getValue();
        }
        return v;
    }

    public CCounter join(CCounter o) {
        dk.join(o.dk);
        return this;
    }

    public static void main(String[] args) {
        CCounter mx = new CCounter("a");
        CCounter my = new CCounter("b");

        mx.inc(3);
        my.inc(1);
        mx.dec(1);

        System.out.println(mx);

        mx.join(my);
        System.out.println(mx);
        System.out.println(my);


    }
}
