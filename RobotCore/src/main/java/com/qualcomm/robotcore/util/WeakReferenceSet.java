package com.qualcomm.robotcore.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * WeakReferenceSet has set behaviour but contains weak references, not strong ones.
 * WeakReferenceSet is thread-safe. It's designed primarily for relatively small sets, as
 * the implementation employed is inefficient on large sets.
 */
public class WeakReferenceSet<E> implements Set<E>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    WeakHashMap<E,Integer> members = new WeakHashMap<E,Integer>();

    //----------------------------------------------------------------------------------------------
    // Primitive Operations
    //----------------------------------------------------------------------------------------------

    @Override
    public boolean add(E o)
        {
        synchronized (members)
            {
            return members.put(o, 1) == null;
            }
        }

    @Override
    public boolean remove(Object o)
        {
        synchronized (members)
            {
            return members.remove(o) != null;
            }
        }

    @Override
    public boolean contains(Object o)
        {
        synchronized (members)
            {
            return members.containsKey(o);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    @Override
    public boolean addAll(Collection<? extends E> collection)
        {
        synchronized (members)
            {
            boolean modified = false;
            for (E o : collection)
                {
                if (this.add(o)) modified = true;
                }
            return modified;
            }
        }

    @Override
    public void clear()
        {
        synchronized (members)
            {
            members.clear();
            }
        }

    @Override
    public boolean containsAll(Collection<?> collection)
        {
        synchronized (members)
            {
            for (Object o : collection)
                {
                if (!contains(o)) return false;
                }
            return true;
            }
        }

    @Override
    public boolean isEmpty()
        {
        return this.size()==0;
        }

    @Override
    public int size()
        {
        synchronized (members)
            {
            return members.size();
            }
        }

    @Override
    public Object[] toArray()
        {
        synchronized (members)
            {
            List<Object> list = new LinkedList<>();
            for (Object o : members.keySet())
                {
                list.add(o);
                }
            return list.toArray();
            }
        }

    @Override
    public Iterator<E> iterator()
    // NOTE: copies the set in order to iterate
        {
        synchronized (members)
            {
            List<E> list = new LinkedList<>();
            for (E o : members.keySet())
                {
                list.add(o);
                }
            return list.iterator();
            }
        }

    @Override
    public boolean removeAll(Collection<?> collection)
        {
        synchronized (members)
            {
            boolean modified = false;
            for (Object o : collection)
                {
                if (remove(o)) modified = true;
                }
            return modified;
            }
        }

    @Override
    public boolean retainAll(Collection<?> collection)
        {
        synchronized (members)
            {
            boolean modified = false;
            for (Object o : this)
                {
                if (!collection.contains(o))
                    {
                    if (remove(o)) modified = true;
                    }
                }
            return modified;
            }
        }

    @Override
    public Object[] toArray(Object[] array)
        {
        synchronized (members)
            {
            Object[] cur = this.toArray();
            Object[] result = cur.length > array.length ? (new Object[cur.length]) : array;
            int i = 0;
            for (;i < cur.length; i++)
                {
                result[i] = cur[i];
                }
            for (; i < result.length; i++)
                {
                result[i] = null;
                }
            return result;
            }
        }
    }



























