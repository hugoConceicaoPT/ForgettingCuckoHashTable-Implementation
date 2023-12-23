package aed.tables;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

import aed.utils.TimeAnalysisUtils;

public class ForgettingCuckooHashTable<Key,Value> implements ISymbolTable<Key,Value> {


    private static int[] primesTable0 = {
            7, 17, 37, 79, 163, 331,
            673, 1361, 2729, 5471, 10949,
            21911, 43853, 87719, 175447, 350899,
            701819, 1403641, 2807303, 5614657,
            11229331, 22458671, 44917381, 89834777, 179669557
    };

    private static int[] primesTable1 = {
            11, 19, 41, 83, 167, 337,
            677, 1367, 2731, 5477, 10957,
            21929, 43867, 87721, 175453, 350941,
            701837, 1403651, 2807323, 5614673,
            11229341, 22458677, 44917399, 89834821, 179669563
    };

    private int mT0;
    private int mT1;
    private int primeIndex;
    private int sizeT0;
    private int sizeT1;
    private Key[] keysT0;
    private Value[] valuesT0;
    private Key[] keysT1;
    private Value[] valuesT1;
    private boolean swapLogging;
    private List<Integer> swapCounts;
    private boolean advanceTimeStatus;
    private long [] timestampsT0;
    private long [] timestampsT1;
    private long currentTimeMillis;
    private boolean isResizing;

    @SuppressWarnings("unchecked")
    public ForgettingCuckooHashTable(int primeIndex)
    {
        this.primeIndex = primeIndex;
        this.mT0 = ForgettingCuckooHashTable.primesTable0[primeIndex];
        this.mT1 = ForgettingCuckooHashTable.primesTable1[primeIndex];
        this.sizeT0 = 0;
        this.sizeT1 = 0; 
        this.keysT0 = (Key[]) new Object[this.mT0];
        this.valuesT0 = (Value[]) new Object[this.mT0];
        this.keysT1 = (Key[]) new Object[this.mT1];
        this.valuesT1 = (Value[]) new Object[this.mT1];
        this.swapLogging = false;
        this.swapCounts = new ArrayList<>();
        this.advanceTimeStatus = false;
        this.timestampsT0 = new long[this.mT0];
        this.timestampsT1 = new long[this.mT1];
        this.currentTimeMillis = System.currentTimeMillis();
        this.advanceTimeStatus = false;
        this.isResizing = false;
    }

    public ForgettingCuckooHashTable()
    {
        this(0);
    }

    private int h0(Key k) {
        return ((k.hashCode() & 0x7fffffff)) % this.mT0;
    }
    
    private int h1(Key k) {
        int hash = k.hashCode() & 0x7fffffff;
        int prime1 = 31;
        int prime2 = 37; 
        return ((hash % this.mT1) * prime1 + (hash % this.mT1) * prime2) % this.mT1;
    }
    

    public int size()
    {
        return this.sizeT0 + this.sizeT1;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    public int getCapacity()
    {
        return this.mT0 + this.mT1;
    }

    public float getLoadFactor()
    {
        return (float) size()/getCapacity();
    }

    public boolean containsKey(Key k) {
        int countT0 = 0;
        for (int i = h0(k); this.keysT0[i] != null; i = (i + 1) % this.mT0) {
            if (k.equals(keysT0[i]))
                return true;
            if(countT0 >= this.mT0)
                break;
            countT0++;
        }
    
        int countT1 = 0;
        for (int i = h1(k); this.keysT1[i] != null; i = (i + 1) % this.mT1) {
            if (k.equals(keysT1[i]))
                return true;
            if(countT1 >= this.mT1)
                break;
            countT1++;
        }
        return false;
    }

    public Value get(Key k)
    {
        int countT0 = 0;
        for(int i = h0(k); this.keysT0[i] != null; i = (i+1) % this.mT0)
        {
            if(!this.isResizing)
                this.timestampsT0[i] = this.currentTimeMillis;
            if(this.keysT0[i].equals(k))
                return this.valuesT0[i];
            if(countT0 >= this.mT0)
                break;
            countT0++;
        }

        int countT1 = 0;
        for(int i = h1(k); this.keysT1[i] != null; i = (i+1) % this.mT1)
        {
            if(!this.isResizing)
                this.timestampsT1[i] = this.currentTimeMillis;
            if(this.keysT1[i].equals(k))
                return this.valuesT1[i];
            if(countT1 >= this.mT1)
                break;
            countT1++;
        }
        return null;
    }

    private void resize(int primeIndex)
    {
        if(primeIndex < 0 || primeIndex >= primesTable0.length || primeIndex >= primesTable1.length) return;

        this.primeIndex = primeIndex;
        this.isResizing = true;

        ForgettingCuckooHashTable<Key,Value> aux = new ForgettingCuckooHashTable<Key,Value>(this.primeIndex);

        for(int i = 0; i < this.mT0; i++)
        {
            if(keysT0[i] != null) aux.put(keysT0[i], valuesT0[i]);
        }

        for(int i = 0; i < this.mT1; i++)
        {
            if(keysT1[i] != null) aux.put(keysT1[i], valuesT1[i]);
        }
        
        for (int i = 0; i < this.mT0; i++) {
            if (i < aux.mT0) {
                aux.timestampsT0[i] = this.timestampsT0[i];
            }
        }
    
        for (int i = 0; i < this.mT1; i++) {
            if (i < aux.mT1) {
                aux.timestampsT1[i] = this.timestampsT1[i];
            }
        }

        this.isResizing = false;
        this.keysT0 = aux.keysT0;
        this.keysT1 = aux.keysT1;
        this.valuesT0 = aux.valuesT0;
        this.valuesT1 = aux.valuesT1;
        this.timestampsT0 = aux.timestampsT0;
        this.timestampsT1 = aux.timestampsT1;
        this.mT0 = aux.mT0;
        this.mT1 = aux.mT1;
        this.advanceTimeStatus = aux.advanceTimeStatus;
      
    }

    public void put(Key k, Value v)
    {
        if(k == null) throw new IllegalArgumentException();

        if(v == null)
        {
            delete(k);
            return;
        }

        if(containsKey(k))
        {
            updateValue(k,v);
            return;
        }

        if(getLoadFactor() >= 0.5f)
            resize(this.primeIndex+1);

        int currentTable = 0;
        int swaps = 0;
        int maxSwaps = 1000;

        while(swaps < maxSwaps) {
            int index = (currentTable == 0) ? h0(k) : h1(k);
            Key tempKey = (currentTable == 0) ? keysT0[index] : keysT1[index];
            Value tempValue = (currentTable == 0) ? valuesT0[index] : valuesT1[index];

            if (tempKey != null) {
                if(checkSameHashCode(k))
                    throw new IllegalArgumentException();
                if(currentTable == 0)
                {
                    if(isKeyExpired(currentTable, index) && this.advanceTimeStatus && !this.isResizing)
                    {
                        delete(tempKey);
                        this.keysT0[index] = k;
                        this.valuesT0[index] = v;
                        this.sizeT0++;
                        break;
                    }
                    this.keysT0[index] = k;
                    this.valuesT0[index] = v;
                    k = tempKey;
                    v = tempValue;
                    currentTable = 1;
                }
                else
                {
                    if(isKeyExpired(currentTable, index) && this.advanceTimeStatus && !this.isResizing)
                    {
                        delete(tempKey);
                        this.keysT1[index] = k;
                        this.valuesT1[index] = v;
                        this.sizeT1++;
                        break;
                    }
                    this.keysT1[index] = k;
                    this.valuesT1[index] = v;
                    k = tempKey;
                    v = tempValue;
                    currentTable = 0;
                }
                
            }
            else
            {
                insertTable(k, v, currentTable);
                break;
            }
            
            swaps++;
        }
        if(swaps >= maxSwaps)
        {
            resize(this.primeIndex + 1);
            put(k, v);
            return;
        }
        if(this.swapLogging)
        {
            this.swapCounts.add(swaps);
        }
    }

    private boolean isKeyExpired(int currentTable, int index) {
        long timeStamp = (currentTable == 0) ? timestampsT0[index] : timestampsT1[index];
        long expirationTime = 24 * 3600000; 
    
        return this.currentTimeMillis - timeStamp > expirationTime;
    }
    
    private void insertTable(Key currentKey, Value currentValue, int currentTable)
    {
        int index = (currentTable == 0) ? h0(currentKey) : h1(currentKey);
        
        if(currentTable == 0)
        {
            this.keysT0[index] = currentKey;
            this.valuesT0[index] = currentValue;
            this.sizeT0++;
        }
        else
        {
            this.keysT1[index] = currentKey;
            this.valuesT1[index] = currentValue;
            this.sizeT1++; 
        }
    }
    
    private boolean checkSameHashCode(Key currentKey) {
        int hashCode = currentKey.hashCode();
        
        if (keysT0[h0(currentKey)] != null && keysT1[h1(currentKey)] != null) {
            int hashCodeT0 = keysT0[h0(currentKey)].hashCode();
            int hashCodeT1 = keysT1[h1(currentKey)].hashCode();
            return hashCode == hashCodeT0 && hashCode == hashCodeT1;
        }
        
        return false;
    }
    
    public void updateValue(Key k, Value v)
    {
        int index = 0;
        for(int i = h0(k); keysT0[i] != null; i = (i + 1) % mT0)
        {
            if(k.equals(keysT0[i]))
            {
                index = 1;
                break;
            }
            else
                index = -1;
        }

        if(index == 1)
        {
            int indexT0 = h0(k);
            valuesT0[indexT0] = v;
            if(!this.isResizing)
                this.timestampsT0[indexT0] = this.currentTimeMillis;
        }
        else
        {
            int indexT1 = h1(k);
            valuesT1[indexT1] = v;
            if(!this.isResizing)
                this.timestampsT1[indexT1] = this.currentTimeMillis;
        }
    }

    public void delete(Key k)
    {
        int iT0 = h0(k);
        int iT1 = h1(k);
        int currentTable = 0;
        while(true)
        {
            if(this.keysT0[iT0] == null && this.keysT1[iT1] == null) return;

            if(this.keysT0[iT0] != null) 
            {
                if(this.keysT0[iT0].equals(k))
                {
                    currentTable = 0;
                    break;
                }
            }
            if(this.keysT1[iT1] != null)
            {
                if(this.keysT1[iT1].equals(k))
                { 
                    currentTable = 1;
                    break;
                }
            } 

            iT0 = (iT0+1) % this.mT0;
            iT1 = (iT1+1) % this.mT1;
        }
        if(currentTable == 0)
        {
            this.keysT0[iT0] = null;
            this.valuesT0[iT0] = null;
            this.sizeT0--;
        }
        else
        {
            this.keysT1[iT1] = null;
            this.valuesT1[iT1] = null;
            this.sizeT1--;
        }

        if(getLoadFactor() < 0.125f && (getCapacity() > ForgettingCuckooHashTable.primesTable0[0] && getCapacity() > ForgettingCuckooHashTable.primesTable1[0]))
            resize(this.primeIndex-1);
    }

    public Iterable<Key> keys() {
        return new KeyIterator();
    }

    private class KeyIterator implements Iterator<Key>,Iterable<Key>
    {
        private List<Key> allKeys;
        private int currentIndex;

        KeyIterator()
        {
            allKeys = new ArrayList<>();
            currentIndex = 0;
            for (Key key : keysT0) {
                if (key != null) {
                    allKeys.add(key);
                }
            }
    
            for (Key key : keysT1) {
                if (key != null) {
                    allKeys.add(key);
                }
            }
            
        }

        public boolean hasNext() {
            return this.currentIndex < allKeys.size(); 
        }

        public Key next() {
            if(hasNext())
                return allKeys.get(this.currentIndex++);
            return null;
        }
        
        

        public void remove() {
            throw new UnsupportedOperationException("Iterator doesn't support removal");
        }

        @Override
        public Iterator<Key> iterator() {
            return this;
        }
    }

    public void setSwapLogging(boolean state)
    {
        this.swapLogging = state;
    }

    public float getSwapAverage()
    {
        if(swapCounts.isEmpty() || !swapLogging)
            return 0.0f;
        
        int exchanges = Math.min(swapCounts.size(), 100);
        int sum = 0;
        int count = 0;
        for(int i = swapCounts.size() - 1; count < exchanges;i--)
        {
            sum += swapCounts.get(i);
            count++;
        }

        return (float) sum / exchanges;
    }

    public float getSwapVariation()
    {
        if(swapCounts.isEmpty() || !swapLogging)
            return 0.0f;
        
        int exchanges = Math.min(swapCounts.size(), 100);
        float sum = 0;
        float average = getSwapAverage();
        int count = 0;
        for(int i = swapCounts.size() - 1; count < exchanges;i--)
        {
            float difference = swapCounts.get(i) - average;
            sum += (difference * difference);
            count++;
        }

        return  sum / exchanges;
    }

    public void advanceTime(int hours) {
        this.currentTimeMillis += (hours * 3600000);
        this.advanceTimeStatus = true;
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        int iterations = 15;
        testWithForgetting();
        testWithoutForgetting();

        Function<Integer, ForgettingCuckooHashTable> exampleGenerator = (Integer size) -> {
            ForgettingCuckooHashTable<Integer, String> table = new ForgettingCuckooHashTable<>(determinePrimeIndex(size));
            // Adicionar elementos na tabela usando o método 'put'
            for (int i = 0; i < size; i++) {
                table.put(i, "Valor" + i);
            }
            return table;
        };

        Consumer<ForgettingCuckooHashTable> testPutMethod = hash -> {
            // Adicione elementos à tabela usando o método 'put'
            int maxSize = hash.getCapacity();
            for (int i = 0; i < maxSize; i++) {
                hash.put(i, "NovoValor" + i);
            }
        };
        TimeAnalysisUtils.runDoublingRatioTest(exampleGenerator, testPutMethod, iterations);

        Consumer<ForgettingCuckooHashTable> testGetMethod = hash -> hash.get(1);


        TimeAnalysisUtils.runDoublingRatioTest(exampleGenerator, testGetMethod, iterations);
    }

    private static int determinePrimeIndex(int size) {
        int[] primes = ForgettingCuckooHashTable.primesTable0; 
        for (int i = 0; i < primes.length; i++) {
            if (primes[i] >= size) {
                return i; 
            }
        }
        return primes.length - 1; 
    }

    private static void testWithForgetting() {
        ForgettingCuckooHashTable<Integer, String> table = new ForgettingCuckooHashTable<>();

        // Simula o acesso frequente a 20% das chaves
        simulateFrequentAccess(table, 20);
        table.setSwapLogging(true);
        for (int hours = 2; hours <= 25; hours += 2) {
            // Avança o tempo em 2 horas para esquecer chaves
            table.advanceTime(hours);

            // Insere novas chaves
            long startTime = System.nanoTime();
            insertNewKeys(table, 100);
            long endTime = System.nanoTime();

            // Imprime os resultados
            printResults(table, "Com Esquecimento após " + hours + " horas", endTime - startTime);
        }
    }

    private static void testWithoutForgetting() {
        ForgettingCuckooHashTable<Integer, String> table = new ForgettingCuckooHashTable<>();

        // Simula o acesso frequente a 20% das chaves
        simulateFrequentAccess(table, 20);

        // Desabilita o esquecimento para esta versão
        table.setSwapLogging(true);

        for (int hours = 2; hours <= 25; hours += 2) {
            // Insere novas chaves
            long startTime = System.nanoTime();
            insertNewKeys(table, 100);
            long endTime = System.nanoTime();

            // Imprime os resultados
            printResults(table, "Sem Esquecimento após " + hours + " horas", endTime - startTime);
        }
    }

    private static void simulateFrequentAccess(ForgettingCuckooHashTable<Integer, String> table, int percentage) {
        int totalKeys = table.getCapacity();
        int frequentKeysCount = (int) (totalKeys * (percentage / 100.0));

        Random random = new Random();

        for (int i = 0; i < frequentKeysCount; i++) {
            int key = random.nextInt(totalKeys);
            table.get(key);  
        }
    }

    private static void insertNewKeys(ForgettingCuckooHashTable<Integer, String> table, int count) {

        for (int i = 0; i < count; i++) {
            int key = table.getCapacity() + i; 
            String value = "Value" + i;
            table.put(key, value);
        }
    }

    private static void printResults(ForgettingCuckooHashTable<Integer, String> table, String version, long executionTime) {
        System.out.println("Resultados para a versão: " + version);
        System.out.println("Número médio de trocas: " + table.getSwapAverage());
        System.out.println("Variância das trocas: " + table.getSwapVariation());
        System.out.println("Tempo médio de execução do método put: " + (executionTime / 1E6) + " ms");
        System.out.println("----\n");
    }
}

