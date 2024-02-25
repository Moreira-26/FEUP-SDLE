package sdle.crdt;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ORMap implements Serializable {
    private Map<String, CCounter> m;
    private DotContext cbase;
    private DotContext cont;
    private String id; //Replica Id
    private String shoppingListId;

    public ORMap() {
        // Only for deltas and those should not be mutated
        cont = new DotContext();
        cbase = new DotContext();
        m = new HashMap<>();
    }

    public ORMap(String replicaId, String shoppingListId) {
        // Mutable replicas need a unique id
        this.shoppingListId = shoppingListId;
        id = replicaId;
        cont = new DotContext();
        cbase = new DotContext();
        m = new HashMap<>();
    }

    public ORMap(String i, DotContext jointc) {
        id = i;
        cont = jointc;
        cbase = new DotContext();
        m = new HashMap<>();
    }

    public DotContext context() {
        return cont;
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder("Map:" + cont + "\n");
        for (Map.Entry<String, CCounter> entry : m.entrySet()) {
            output.append(entry.getKey()).append("->").append(entry.getValue()).append("\n");
        }
        return output.toString();
    }

    public String toStringPretty(){
        StringBuilder output = new StringBuilder("ShoppingList:" + this.shoppingListId + "\n");
        for (Map.Entry<String, CCounter> entry : m.entrySet()) {
            if(entry.getValue().read() != 0){
                output.append(entry.getKey()).append("->").append(entry.getValue().read()).append("\n");
            }
        }
        return output.toString();
    }

    public CCounter get(String n) {
        CCounter v = m.get(n);
        if (v == null) {
            // 1st key access
            DotContext newContext= new DotContext(cont);
            CCounter empty = new CCounter(id, newContext);
            m.put(n, empty);
            return empty;
        }
        return v;
    }

    public void inc(String n, Integer val) {
        CCounter v = get(n);
        v.inc(val);
        this.cont.join(v.context());
        updateContext();
    }

    public void dec(String n, Integer val) {
        CCounter v = get(n);
        v.dec(val);
        this.cont.join(v.context());
        updateContext();
    }

    public void updateContext() {
        for(Map.Entry<String, CCounter> entry : m.entrySet()) {
            entry.getValue().updateContext(cont);
        }
    }

    public String getShoppingListId(){
        return this.shoppingListId;
    }


    public ORMap erase(String n) {
        ORMap r = new ORMap();
        if (m.containsKey(n)) {
            CCounter v = m.get(n).reset();
            r.cont = v.context();
            m.remove(n);
        }
        return r;
    }

    public ORMap reset() {
        ORMap r = new ORMap();
        if (!m.isEmpty()) {
            for (Map.Entry<String, CCounter> entry : m.entrySet()) {
                CCounter v = entry.getValue().reset();
                r.cont.join(v.context());
            }
            m.clear();
        }
        return r;
    }

    public void setReplicaId(String id){
        this.id = id;
    }


    public void join(ORMap o) {
        DotContext ic = new DotContext(cont);

        Iterator<Map.Entry<String, CCounter>> mit = m.entrySet().iterator(), mit2 = m.entrySet().iterator();
        Iterator<Map.Entry<String, CCounter>> mito = o.m.entrySet().iterator(), mito2 = o.m.entrySet().iterator();
        Map.Entry<String, CCounter> currentEntryMitValue = null;
        Map.Entry<String, CCounter> currentEntryMitoValue = null;

        if(mit.hasNext()){
            currentEntryMitValue = mit2.next();
        }
        if(mito.hasNext()){
            currentEntryMitoValue = mito2.next();
        }

        while (mit.hasNext() || mito.hasNext()){

            if (mit.hasNext() && (!mito.hasNext() || (currentEntryMitValue.getKey().compareTo(currentEntryMitoValue.getKey()) < 0 ))) {

                CCounter empty = new CCounter(id, o.context());
                currentEntryMitValue.getValue().join(empty);
                cont = new DotContext(ic);
                if(mit2.hasNext()) currentEntryMitValue = mit2.next();
                mit.next();

            } else if (mito.hasNext() && (!mit.hasNext() || (currentEntryMitoValue.getKey().compareTo(currentEntryMitValue.getKey()) < 0 ))) {
                CCounter otherKernel = this.get(currentEntryMitoValue.getKey()).join(currentEntryMitoValue.getValue());
                m.put(currentEntryMitoValue.getKey(), otherKernel);
                cont = new DotContext(ic);
                if(mito2.hasNext()) currentEntryMitoValue = mito2.next();
                mito.next();
            } else if (mit.hasNext() && mito.hasNext()) {
                CCounter mitKernel = this.get(currentEntryMitValue.getKey());
                CCounter mitoKernel = o.get(currentEntryMitoValue.getKey());
                CCounter result = mitKernel.join(mitoKernel);

                //CCounter otherKernel = this.get(currentEntryMitoValue.getKey()).join(currentEntryMitoValue.getValue());

                m.put(currentEntryMitoValue.getKey(), result);
                cont = new DotContext(ic);
                if(mito2.hasNext()) currentEntryMitoValue = mito2.next();
                mito.next();
                if(mit2.hasNext()) currentEntryMitValue = mit2.next();
                mit.next();
            }


        }

        cont.join(o.cont);
    }

    public static void main(String[] args) {
        ORMap mx = new ORMap("x", "shop1");
        ORMap my = new ORMap("y", "shop1");
        ORMap mz = new ORMap("z", "shop1");

        mx.inc("pao", 8);
        my.inc("pao", 8);
        mz.join(mx);
        mz.join(my);
        mx.dec("pao", 4);
        my.join(mx);

        System.out.println(my.toStringPretty());
    }
}
