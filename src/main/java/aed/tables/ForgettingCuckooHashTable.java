package aed.tables;

import java.util.Date;
import java.util.Iterator;
//test
import static java.lang.Math.abs;
import static java.lang.Math.floor;

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

    private int primeIndex;    //index of prime number being used in primesTables

    //table0
    private int m0; //size of hashTable0
    private int size0;  //number of elements currently in hashTable0

    private Key[] keys0;    //array of keys of hashTable0
    private Value[] values0;    //array of values of hashTable0

    //table1
    private int m1; //size of hashTable1
    private int size1;  //number of elements currently in hashTable1

    private Key[] keys1;    //array of keys of hashTable1
    private Value[] values1;    //array of values of hashTable1

    private boolean swapLogging;    //true if swapLogging is desired, false if not
    private int[] swaps;    //swap logs
    private int swapI;  //index to the right of the last inserted swap
    private int swapStartI; //index of the first swap to consider

    private long[] timestamps0; //timestamps for hashtable0 saved in miliseconds
    private long[] timestamps1; //timestamps for hashtable1 saved in miliseconds

    long timeOffset;

    @SuppressWarnings("unchecked")
    public ForgettingCuckooHashTable(int primeIndex)
    {
        this.primeIndex = primeIndex;

        this.m0 = primesTable0[primeIndex];
        this.size0 = 0;
        this.keys0 = (Key[]) new Object[this.m0];
        this.values0 = (Value[]) new Object[this.m0];

        this.m1 = primesTable1[primeIndex];
        this.size1 = 0;
        this.keys1 = (Key[]) new Object[this.m1];
        this.values1 = (Value[]) new Object[this.m1];

        this.swapLogging = false;
        this.swaps = new int[10000];
        this.swapI = 0;
        this.swapStartI = 0;

        this.timestamps0 = new long[this.m0];
        this.timestamps1 = new long[this.m1];

        this.timeOffset = 0;
    }

    @SuppressWarnings("unchecked")
    public ForgettingCuckooHashTable()
    {
        this.primeIndex = 0;

        this.m0 = primesTable0[primeIndex];
        this.size0 = 0;
        this.keys0 = (Key[]) new Object[this.m0];
        this.values0 = (Value[]) new Object[this.m0];

        this.m1 = primesTable1[primeIndex];
        this.size1 = 0;
        this.keys1 = (Key[]) new Object[this.m1];
        this.values1 = (Value[]) new Object[this.m1];

        this.swapLogging = false;
        this.swaps = new int[100000];
        this.swapI = 0;
        this.swapStartI = 0;

        this.timestamps0 = new long[this.m0];
        this.timestamps1 = new long[this.m1];

        this.timeOffset = 0;
    }

    private void resize(int primeIndex){
        if(primeIndex < 0 || primeIndex >= primesTable0.length) return;

        ForgettingCuckooHashTable<Key, Value> aux = new ForgettingCuckooHashTable<>(primeIndex);

        int i = 0;
        while(i < this.m1){
            if(i < this.m0){
                if(this.keys0[i] != null){
                    Key key0 = this.keys0[i];
                    Value value0 = this.values0[i];
                    long timestamp0 = this.timestamps0[i];
                    aux.putResize(key0, value0, timestamp0);
                }
            }
            if(this.keys1[i] != null){
                Key key1 = this.keys1[i];
                Value value1 = this.values1[i];
                long timestamp1 = this.timestamps1[i];
                aux.putResize(key1, value1, timestamp1);
            }
            i++;
        }
        this.primeIndex = primeIndex;
        this.keys0 = aux.keys0;
        this.keys1 = aux.keys1;
        this.values0 = aux.values0;
        this.values1 = aux.values1;
        this.m0 = primesTable0[this.primeIndex];
        this.m1 = primesTable1[this.primeIndex];
        this.size0 = aux.size0;
        this.size1 = aux.size1;
        this.timestamps0 = aux.timestamps0;
        this.timestamps1 = aux.timestamps1;
    }

    public void putResize(Key k, Value v, long timestamp){
        int h;
        Key currentK = k;
        Value currentV = v;
        long currentTimestamp = timestamp;
        int p = 0;
        while(true){
            if(p % 2 == 0){
                h = hash0(currentK);
                if(this.keys0[h] == null){
                    this.keys0[h] = currentK;
                    this.values0[h] = currentV;
                    this.timestamps0[h] = currentTimestamp;
                    this.size0++;
                    break;
                }
                else{
                    Key newK = this.keys0[h];
                    Value newV = this.values0[h];
                    long newTimestamp = this.timestamps0[h];
                    this.keys0[h] = currentK;
                    this.values0[h] = currentV;
                    this.timestamps0[h] = currentTimestamp;
                    currentK = newK;
                    currentV = newV;
                    currentTimestamp = newTimestamp;
                    p++;
                }
            }
            else{
                h = hash1(currentK);
                if(this.keys1[h] == null){
                    this.keys1[h] = currentK;
                    this.values1[h] = currentV;
                    this.timestamps1[h] = currentTimestamp;
                    this.size1++;
                    break;
                }
                else{
                    Key newK = this.keys1[h];
                    Value newV = this.values1[h];
                    long newTimestamp = this.timestamps1[h];
                    this.keys1[h] = currentK;
                    this.values1[h] = currentV;
                    this.timestamps1[h] = currentTimestamp;
                    currentK = newK;
                    currentV = newV;
                    currentTimestamp = newTimestamp;
                    p++;
                }
            }
        }
    }

    private int hash0(Key k)
    {
        return (k.hashCode() & 0x7fffffff) % this.m0;
    }

    private int hash1(Key k){
        return (this.m1 - (k.hashCode() & 0x7fffffff) % this.m1)-1;
    }

    public int size()
    {
        return (this.size0 + this.size1);
    }

    @Override
    public boolean isEmpty() {
        return this.size() == 0;
    }

    public int getCapacity()
    {
        return (this.m0 + this.m1);
    }

    public float getLoadFactor()
    {
        return (float)this.size()/(float)this.getCapacity();
    }

    public boolean containsKey(Key k)
    {
        int h0 = hash0(k);
        int h1 = hash1(k);
        if((this.keys0[h0] != null && this.keys0[h0].equals(k))){
            Date currentDate = new Date();
            this.timestamps0[h0] = currentDate.getTime() + this.timeOffset;
            return true;
        }
        else if(this.keys1[h1] != null && this.keys1[h1].equals(k)){
            Date currentDate = new Date();
            this.timestamps1[h1] = currentDate.getTime() + this.timeOffset;
            return true;
        }
        return false;
    }

    public Value get(Key k)
    {
        int h0 = hash0(k);
        int h1 = hash1(k);
        if(this.keys0[h0] != null && this.keys0[h0].equals(k)){
            Date currentDate = new Date();
            this.timestamps0[h0] = currentDate.getTime() + this.timeOffset;
            return this.values0[h0];
        }
        else if(this.keys1[h1] != null && this.keys1[h1].equals(k)){
            Date currentDate = new Date();
            this.timestamps1[h1] = currentDate.getTime() + this.timeOffset;
            return this.values1[h1];
        }
        return null;
    }

    public void updateKeyValue(Key k, Value v){
        int h0 = hash0(k);
        int h1 = hash1(k);
        if(this.keys0[h0].equals(k)){
            this.values0[h0] = v;
        }
        else{
            this.values1[h1] = v;
        }
    }

    //returns false if both tables have a key in hashCode position, true if one of those is empty
    public boolean hashCodeAvailable(Key k){
        int h0 = hash0(k);
        int h1 = hash1(k);
        if(this.keys0[h0] == null || this.keys1[h1] == null){
            return true;
        }
        Key k0 = this.keys0[h0];
        Key k1 = this.keys1[h1];
        int h = k.hashCode() & 0x7fffffff;
        int hk0 = k0.hashCode() & 0x7fffffff;
        int hk1 = k1.hashCode() & 0x7fffffff;
        if(h == hk0 && h == hk1){
            return false;
        }
        else{
            return true;
        }
    }

    public void put(Key k, Value v)
    {
        int s = 0;  //number of swaps
        if(this.getLoadFactor() >= 0.5f){
            this.resize(this.primeIndex+1);
        }
        //k is invalid
        if(k == null){
            throw new IllegalArgumentException();
        }
        //v is null
        else if(v == null){
            this.delete(k);
        }
        //key is already inserted, only updates it
        else if(this.containsKey(k)){
            this.updateKeyValue(k, v);
        }
        //hashcode is unavailable
        else if(!this.hashCodeAvailable(k)){
            throw new IllegalArgumentException();
        }
        else{
            int h;
            Key currentK = k;
            Value currentV = v;
            long currTime = new Date().getTime();   //time of pc
            long currentTime = currTime + this.timeOffset;  //time of pc + offset
            long currentTimestamp = currentTime;
            //p will serve as the variable that will tell us in which hashTable we are in the current iteration
            //if p is even we are in hashtable 0, if its odd we are in hashtable 1
            int p = 0;
            while(true){
                if(s > 1000){
                    this.resize(this.primeIndex+1);
                    this.swaps[this.swapI] += s;
                    s = 0;
                }
                if(p % 2 == 0){
                    h = hash0(currentK);
                    if(this.keys0[h] == null){
                        this.keys0[h] = currentK;
                        this.values0[h] = currentV;
                        this.timestamps0[h] = currentTimestamp;
                        this.size0++;
                        break;
                    }
                    else{
                        long tsTime = this.timestamps0[h];    //time of the timestamp
                        long dif = currentTime-tsTime;  //difference of times
                        //if the key is still good to use
                        if(dif < 8.64e+7){  //8.64e+7 is 24 hours in miliseconds
                            Key newK = this.keys0[h];
                            Value newV = this.values0[h];
                            long newTimestamp = this.timestamps0[h];
                            this.keys0[h] = currentK;
                            this.values0[h] = currentV;
                            this.timestamps0[h] = currentTimestamp;
                            currentK = newK;
                            currentV = newV;
                            currentTimestamp = newTimestamp;
                            p++;
                            s++;
                        }
                        //if we can forget the key
                        else{
                            this.keys0[h] = currentK;
                            this.values0[h] = currentV;
                            this.timestamps0[h] = currentTimestamp;
                            break;
                        }
                    }
                }
                else{
                    h = hash1(currentK);
                    if(this.keys1[h] == null){
                        this.keys1[h] = currentK;
                        this.values1[h] = currentV;
                        this.timestamps1[h] = currentTimestamp;
                        this.size1++;
                        break;
                    }
                    else{
                        long tsTime = this.timestamps1[h];    //time of the timestamp
                        long dif = currentTime-tsTime;  //difference of times
                        //if the key is still good to use
                        if(dif < 8.64e+7){  //8.64e+7 is 24 hours in miliseconds
                            Key newK = this.keys1[h];
                            Value newV = this.values1[h];
                            long newTimestamp = this.timestamps1[h];
                            this.keys1[h] = currentK;
                            this.values1[h] = currentV;
                            this.timestamps1[h] = currentTimestamp;
                            currentK = newK;
                            currentV = newV;
                            currentTimestamp = newTimestamp;
                            p++;
                            s++;
                        }
                        //if we can forget the key
                        else{
                            this.keys1[h] = currentK;
                            this.values1[h] = currentV;
                            this.timestamps1[h] = currentTimestamp;
                            break;
                        }
                    }
                }
            }
            if(this.swapLogging){
                if(this.swapI > 100){
                    this.swapStartI++;
                }
                this.swaps[this.swapI++] += s;
            }
        }
    }

    public void delete(Key k)
    {
        int h0 = hash0(k);
        int h1 = hash1(k);
        if(this.keys0[h0] != null && this.keys0[h0].equals(k)){
            this.keys0[h0] = null;
            this.values0[h0] = null;
            this.timestamps0[h0] = 0;
            this.size0--;
        }
        else if(this.keys1[h1] != null && this.keys1[h1].equals(k)){
            this.keys1[h1] = null;
            this.values1[h1] = null;
            this.timestamps1[h1] = 0;
            this.size1--;
        }
        if(this.getLoadFactor() < 0.125f){
            this.resize(this.primeIndex-1);
        }
    }

    public Iterable<Key> keys() {
        return new KeyIterator();
    }

    private class KeyIterator implements Iterator<Key>,Iterable<Key>
    {
        int i;  //iterator
        Key[] keys; //keys
        @SuppressWarnings("unchecked")
        KeyIterator()
        {
            this.i = 0;
            this.keys = (Key[]) new Object[size()];
            int j = 0;  //tables iterator
            int k = 0;  //keys iterator
            while(j < getCapacity()){
                if(j < m0){
                    if(keys0[j] != null){
                        this.keys[k++] = keys0[j++];
                    }
                    else{
                        j++;
                    }
                }
                else{
                    if(keys1[j-m0] != null){
                        this.keys[k++] = keys1[j-m0];
                        j++;
                    }
                    else{
                        j++;
                    }
                }
            }
        }

        public boolean hasNext() {
            return this.i < this.keys.length;
        }

        public Key next() {
            return this.keys[this.i++];
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
        if(this.swapLogging){
            int n = this.swapI;
            int totalSwaps = 0;
            for(int i = this.swapStartI; i < n; i++){
                totalSwaps += this.swaps[i];
            }
            return (float) totalSwaps/n;
        }
        else{
            return 0.0f;
        }
    }

    public float getSwapVariation()
    {
        if(this.swapLogging){
            float avg = this.getSwapAverage();
            int n = this.swapI;
            float totalVariation = 0;
            for(int i = this.swapStartI; i < n; i++){
                totalVariation += (float) Math.pow(avg-this.swaps[i], 2);
            }
            return totalVariation/n;
        }
        else{
            return 0.0f;
        }
    }

    public void advanceTime(int hours)
    {
        long newTimeOffset = (long) (hours*(3.6e+6));   //3.6e+6 is the number of miliseconds in an hour
        this.timeOffset += newTimeOffset;
    }

    public static void main(String[] args)
    {
        /*
        ForgettingCuckooHashTable test = new ForgettingCuckooHashTable();
        test.put("ABC", "abc");
        test.put("five", "five");
        test.put("102", "102");
        test.put("103", "102");
        test.put("104", "102");
        int s = test.size();
        boolean t = test.containsKey("ABC");
        boolean f = test.containsKey("abc");
        float lf = test.getLoadFactor();
        System.out.println(s);
        System.out.println(lf);
        System.out.println(t);
        System.out.println(f);
        */
    }
}
