package aed.tables;

import java.util.function.Consumer;
import java.util.function.Function;

import aed.utils.TimeAnalysisUtils;

public class OpenAdressingHashTable<Key, Value> {
    private static int[] primes = {17, 37, 79, 163, 331, 673, 1361, 2729, 5471, 10949, 21911,
                        43853, 87719, 175447, 350899, 701819, 1403641, 2807303,
                        5614657, 11229331, 22458671, 44917381, 89834777, 179669557};
    private int m;
    private int primeIndex;
    private int size;
    private float loadFactor;
    private Key[] keys;
    private Value[] values;
    @SuppressWarnings("unchecked")
    private OpenAdressingHashTable(int primeIndex)
    {
        this.primeIndex = primeIndex;
        this.m = this.primes[primeIndex];
        this.size = 0; this.loadFactor = 0;
        this.keys = (Key[]) new Object[this.m];
        this.values = (Value[]) new Object[this.m];
    }
    public OpenAdressingHashTable(){this(0);}

    private int hash(Key k)
    {
        return (k.hashCode() & 0x7fffffff) % this.m;
    }
    public Value get(Key k)
    {
        for(int i = hash(k); this.keys[i] != null; i = (i+1)%this.m)
        {
            //key was found, return its value
            if(this.keys[i].equals(k))
            {
                return this.values[i];
            }
        }
        return null;
    }
    public void put(Key k, Value v)
    {
        if(this.loadFactor >= 0.7f)
            resize(this.primeIndex+1);
        int i = hash(k);
        for(; this.keys[i] != null; i = (i+1)% this.m)
        {
            //key was found, update its value
            if(this.keys[i].equals(k))
            {
                this.values[i] = v;
                return;
            }
        }
        //we've found the right insertion position, insert
        this.keys[i] = k;
        this.values[i] = v;
        this.size++;
        this.loadFactor = this.size/this.m;
    }

    private void resize(int primeIndex)
    {
        //if invalid size do not resize;
        if(primeIndex < 0 || primeIndex >= primes.length) return;
        this.primeIndex = primeIndex;
        OpenAdressingHashTable<Key,Value> aux = new OpenAdressingHashTable<Key,Value>(this.primeIndex);
        //place all existing keys in new table
        for(int i = 0; i < this.m; i++)
        {
            if(keys[i] != null) aux.put(keys[i],values[i]);
        }
        this.keys = aux.keys;
        this.values = aux.values;
        this.m = aux.m;
        this.loadFactor = this.size/this.m;
    }

    private void delete(Key k)
    {
        int i = hash(k);
        while(true)
        {
            //no key to delete, return
            if(this.keys[i] == null) return;
            //if key was found, exit the loop
            if(this.keys[i].equals(k)) break;
                i = (i+1)%this.m;
        }
        //delete the key and value
        this.keys[i] = null;
        this.values[i] = null;
        this.size--;
        //we need to reenter any subsequent keys
        i = (i+1)%this.m;
        while(this.keys[i] != null)
        {
            Key auxKey = this.keys[i];
            Value auxValue = this.values[i];
            //remove from previous position
            this.keys[i] = null;
            this.values[i] = null;
            //temporarily reduce size,
            //next put will increment it
            this.size--;
            //add the key and value again
            this.put(auxKey,auxValue);
            i = (i+1)%this.m;
        }
        this.loadFactor = this.size/this.m;
        if(this.loadFactor < 0.125f)
        resize(this.primeIndex-1);
    }

    public static void main(String[] args) {
        int iterations = 15;

        // Teste para o método 'put'
        Function<Integer, OpenAdressingHashTable<Integer, String>> exampleGenerator = (Integer size) -> {
            OpenAdressingHashTable<Integer, String> table = new OpenAdressingHashTable<>(determinePrimeIndex(size));
            // Adicionar elementos na tabela usando o método 'put'
            for (int i = 0; i < size; i++) {
                table.put(i, "Valor" + i);
            }
            return table;
        };

        Consumer<OpenAdressingHashTable<Integer, String>> testPutMethod = hash -> {
            // Adicione elementos à tabela usando o método 'put'
            int maxSize = hash.size;
            for (int i = 0; i < maxSize; i++) {
                hash.put(i, "NovoValor" + i);
            }
        };

        TimeAnalysisUtils.runDoublingRatioTest(exampleGenerator, testPutMethod, iterations);

        Consumer<OpenAdressingHashTable<Integer, String>> testGetMethod = hash -> hash.get(1);

        TimeAnalysisUtils.runDoublingRatioTest(exampleGenerator, testGetMethod, iterations);
    }

    private static int determinePrimeIndex(int size) {
        int[] primes = OpenAdressingHashTable.primes;
        for (int i = 0; i < primes.length; i++) {
            if (primes[i] >= size) {
                return i;
            }
        }
        return primes.length - 1;
    }

}