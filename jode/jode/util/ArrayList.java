// This class is taken from the Classpath project.  
// Please note the different copyright holder!  
// The changes I did is this comment, the package line, some
// imports from java.util and some minor jdk12 -> jdk11 fixes.
// -- Jochen Hoenicke <jochen@gnu.org>

/////////////////////////////////////////////////////////////////////////////
// ArrayList.java -- JDK1.2's answer to Vector; this is an array-backed
//                   implementation of the List interface
//
// This is a JDK 1.2 compliant version of ArrayList.java
//
// Copyright (c) 1998 by Jon A. Zeppieri (jon@eease.com),
//                    Free Software Foundation, Inc.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU Library General Public License as published
// by the Free Software Foundation, version 2. (see COPYING.LIB)
//
// This program is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Library General Public License for more details.
//
// You should have received a copy of the GNU Library General Public License
// along with this program; if not, write to the Free Software Foundation
// Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
/////////////////////////////////////////////////////////////////////////////
package jode.util;

import java.lang.reflect.Array;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * An array-backed implementation of the List interface.  ArrayList
 * performs well on simple tasks:  random access into a list, appending
 * to or removing from the end of a list, checking the size, &c.
 *
 * @author        Jon A. Zeppieri
 * @version       $Id$
 * @see           java.util.AbstractList
 * @see           java.util.List
 */
public class ArrayList extends AbstractList 
  implements List, Cloneable, Serializable
{
  /** the default capacity for new ArrayLists */
  private static final int DEFAULT_CAPACITY = 16;

  /** the number of elements in this list */
  int size;

  /** where the data is stored */
  transient Object[] _arData;

  /** 
   * Construct a new ArrayList with the supplied initial capacity. 
   *
   * @param     iCapacity
   */
  public ArrayList(int iCapacity)
  {
    _arData = new Object[iCapacity];
  }


  /**
   * Construct a new ArrayList with the default capcity 
   */
  public ArrayList()
  {
    this(DEFAULT_CAPACITY);
  }

  /** 
   * Construct a new ArrayList, and initialize it with the elements
   * in the supplied Collection; Sun specs say that the initial 
   * capacity is 110% of the Collection's size.
   *
   * @param        oCollection     the collection whose elements will
   *                               initialize this list
   */
  public ArrayList(Collection oCollection)
  {
    this((int) (oCollection.size() * 1.1));
    addAll(oCollection);
  }

  /**
   * Guarantees that this list will have at least enough capacity to
   * hold iMinCapacity elements.
   *
   * @param      iMinCapacity     the minimum guaranteed capacity
   */
  public void ensureCapacity(int iMinCapacity)
  {
    Object[] arNewData;
    int iCapacity = _arData.length;

    if (iMinCapacity > iCapacity)
    {
      arNewData = new Object[Math.max((iCapacity * 2), iMinCapacity)];
      System.arraycopy(_arData, 0, arNewData, 0, iCapacity);
      _arData = arNewData;
    }
  }

  /**
   * Appends the supplied element to the end of this list.
   *
   * @param       oElement      the element to be appended to this list
   */
  public boolean add(Object oElement)
  {
    ensureCapacity(size + 1);
    _arData[size++] = oElement;
    modCount++;
    return true;
  }

  /**
   * Retrieves the element at the user-supplied index.
   *
   * @param    iIndex        the index of the element we are fetching
   * @throws   IndexOutOfBoundsException  (iIndex < 0) || (iIndex >= size())
   */
  public Object get(int iIndex)
  {
    if (iIndex >= size)
      throw new IndexOutOfBoundsException("ArrayList size=" +
                                          String.valueOf(size) + "; " +
                                          "index=" + String.valueOf(iIndex));
    return _arData[iIndex];
  }

  /**
   * Returns the number of elements in this list 
   */
  public int size()
  {
    return size;
  }

  /**
   * Removes the element at the user-supplied index
   *
   * @param     iIndex      the index of the element to be removed
   * @return    the removed Object
   * @throws    IndexOutOfBoundsException  (iIndex < 0) || (iIndex >= size())
   */
  public Object remove(int iIndex)
  {
    Object oResult;

    if (iIndex >= size)
      throw new IndexOutOfBoundsException("ArrayList size=" +
                                          String.valueOf(size) + "; " +
                                          "index=" + String.valueOf(iIndex));

    oResult = _arData[iIndex];

    if (iIndex != --size)
      System.arraycopy(_arData, (iIndex + 1), _arData, iIndex, 
                       (size - iIndex));
  
    modCount++;
    _arData[size] = null;

    return oResult;
  }

  /**
   * Removes all elements in the half-open interval [iFromIndex, iToIndex).
   *
   * @param     iFromIndex   the first index which will be removed
   * @param     iToIndex     one greater than the last index which will be 
   *                         removed
   */
  public void removeRange(int iFromIndex, int iToIndex)
  {
    int iReduction;
    int i;

    if ((iFromIndex >= size) || (iToIndex >= size))
    {
      throw new IndexOutOfBoundsException("ArrayList size=" +
                                          String.valueOf(size) + "; " +
                                          "indices=" + 
                                          String.valueOf(iFromIndex) + "," +
                                          String.valueOf(iToIndex));
    }
    else if (iFromIndex > iToIndex)
    {
      throw new IllegalArgumentException("fromIndex(" + 
                                         String.valueOf(iFromIndex) + 
                                         ") > toIndex(" +
                                         String.valueOf(iToIndex) + ")");
    }
    else if (iFromIndex != iToIndex)
    {
      iReduction = iToIndex - iFromIndex;
      System.arraycopy(_arData, (iFromIndex + iReduction), _arData,
                       iFromIndex, (size - iFromIndex - iReduction));
      modCount++;

      for (i = (iFromIndex + iReduction); i < size; i++)
        _arData[i] = null;

      size -= iReduction;
    }
  }

  /**
   * Adds the supplied element at the specified index, shifting all
   * elements currently at that index or higher one to the right.
   *
   * @param     iIndex      the index at which the element is being added
   * @param     oElement    the element being added
   */
  public void add(int iIndex, Object oElement)
  {
    if (iIndex > size)
      throw new IndexOutOfBoundsException("ArrayList size=" +
                                          String.valueOf(size) + "; " +
                                          "index=" + String.valueOf(iIndex));

    ensureCapacity(size + 1);
    System.arraycopy(_arData, iIndex, _arData, 
                      (iIndex + 1), (size - iIndex));
    _arData[iIndex] = oElement;
    size++;
    modCount++;
  }

  /** 
   * Add each element in the supplied Collection to this List.
   *
   * @param        oCollection     a Collection containing elements to be 
   *                               added to this List
   */
  public boolean addAll(Collection oCollection)
  {
    Iterator itElements;
    int iLen = oCollection.size();

    if (iLen > 0)
    {
      ensureCapacity(size + iLen);
      modCount++;

      itElements = oCollection.iterator();

      while (itElements.hasNext())
        _arData[size++] = itElements.next();

      return true;
    }
    return false;
  }

  /** 
   * Add all elements in the supplied collection, inserting them beginning
   * at the specified index.
   *
   * @param     iIndex       the index at which the elements will be inserted
   * @param     oCollection  the Collection containing the elements to be
   *                         inserted
   */
  public boolean addAll(int iIndex, Collection oCollection)
  {
    Iterator itElements;
    int iLen;

    if (iIndex > size)
      throw new IndexOutOfBoundsException("ArrayList size=" +
                                          String.valueOf(size) + "; " +
                                          "index=" + String.valueOf(iIndex));

    iLen = oCollection.size();

    if (iLen > 0)
    {
      ensureCapacity(size + iLen);

      System.arraycopy(_arData, iIndex, _arData, 
                       (iIndex + iLen), (size - iIndex));
      modCount++;
      size += iLen;

      itElements = oCollection.iterator();
      while (itElements.hasNext())
        _arData[iIndex++] = itElements.next();

      return true;
   }
    return false;
  }

  /**
   * Creates a shallow copy of this ArrayList
   */
  public Object clone()
  {
    ArrayList oClone;

    try
    {
      oClone = (ArrayList) super.clone();
      oClone._arData = _arData;
      oClone.size = size;
    }
    catch(CloneNotSupportedException e)
    {
      oClone = null;
    }
    return oClone;
  }

  /** 
   * Returns true iff oElement is in this ArrayList.
   *
   * @param     oElement     the element whose inclusion in the List is being
   *                         tested
   */
  public boolean contains(Object oElement)
  {
    return (indexOf(oElement) != -1);
  }

  /**
   * Returns the lowest index at which oElement appears in this List, or 
   * -1 if it does not appear.
   *
   * @param    oElement       the element whose inclusion in the List is being
   *                          tested
   */
  public int indexOf(Object oElement)
  {
    int i;

    for (i = 0; i < size; i++)
    {
      if (doesEqual(oElement, _arData[i]))
        return i;
    }
    return -1;
  }

  /**
   * Returns the highest index at which oElement appears in this List, or 
   * -1 if it does not appear.
   *
   * @param    oElement       the element whose inclusion in the List is being
   *                          tested
   */
  public int lastIndexOf(Object oElement)
  {
    int i;

    for (i = size - 1; i >= 0; i--)
    {
      if (doesEqual(oElement, _arData[i]))
        return i;
    }
    return -1;
  }

  /**
   * Removes all elements from this List
   */
  public void clear()
  {
    int i;

    if (size > 0)
    {
      modCount++;
      size = 0;

      for (i = 0; i < size; i++)
        _arData[i] = null;
    }      
  }

  /**
   * Sets the element at the specified index.
   *
   * @param     iIndex     the index at which the element is being set
   * @param     oElement   the element to be set
   * @return    the element previously at the specified index, or null if
   *            none was there
   */
  public Object set(int iIndex, Object oElement)
  {
    Object oResult;

    if (iIndex >= size)
      throw new IndexOutOfBoundsException("ArrayList size=" +
                                          String.valueOf(size) + "; " +
                                          "index=" + String.valueOf(iIndex));
    oResult = _arData[iIndex];
    modCount++;
    _arData[iIndex] = oElement;

    return oResult;
  }

  /**
   * Returns an Object Array containing all of the elements in this ArrayList
   */
  public Object[] toArray()
  {
    Object[] arObjects = new Object[size];
    System.arraycopy(_arData, 0, arObjects, 0, size);
    return arObjects;
  }

  /**
   * Returns an Array whse component type is the runtime component type of
   * the passes-in Array.  The returned Array is populated with all of the
   * elements in this ArrayList.  If the passed-in Array is not large enough
   * to store all of the elements in this List, a new Array will be created 
   * and returned; if the passed-in Array is <i>larger</i> than the size
   * of this List, then size() + 1 index will be set to null.
   *
   * @param      arObjects      the passed-in Array
   */
  public Object[] toArray(Object[] arObjects)
  {
    Object[] arReturn = (arObjects.length >= size)
      ? arObjects 
      : (Object[])
      Array.newInstance(arObjects.getClass().getComponentType(), size);

    System.arraycopy(_arData, 0, arReturn, 0, size);

    if (arReturn.length > size)
      arReturn[size] = null;

    return arReturn;
  }

  /**
   * Trims the capacity of tjis List to be equal to its size; 
   * a memory saver. 
   */
  public void trimToSize()
  {
    Object[] arNewData = new Object[size];
    System.arraycopy(_arData, 0, arNewData, 0, size);
    modCount++;
    _arData = arNewData;
  }

  private void writeObject(ObjectOutputStream oOut)
    throws IOException
  {
    int i;

    oOut.defaultWriteObject();

    oOut.writeInt(_arData.length);
    for (i = 0; i < _arData.length; i++)
      oOut.writeObject(_arData[i]);
  }

  private void readObject(ObjectInputStream oIn)
    throws IOException, ClassNotFoundException
  {
    int i;
    int iCapacity;
    oIn.defaultReadObject();

    iCapacity = oIn.readInt();
    _arData = new Object[iCapacity];

    for (i = 0; i < iCapacity; i++)
      _arData[i] = oIn.readObject();
  }

  private static final boolean doesEqual(Object oOne, Object oTwo)
  {
    return ((oOne == null) ? (oTwo == null) : oOne.equals(oTwo));
  }
}

