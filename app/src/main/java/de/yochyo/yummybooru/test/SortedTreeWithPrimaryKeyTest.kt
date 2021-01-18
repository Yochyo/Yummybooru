package de.yochyo.yummybooru.test

import de.yochyo.booruapi.api.TagType
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.eventcollections.TagEventCollection
import org.junit.Assert
import org.junit.Test

class SortedTreeWithPrimaryKeyTest {
    var list: TagEventCollection = TagEventCollection { o1, o2 -> o1.compareTo(o2) }

    /*   @Before
       fun before() {
           list = TagEventCollection()
       }*/


    @Test
    fun addElement() {
        Assert.assertTrue("added element", list.add(tag(1)))
        Assert.assertEquals("size is correct", 1, list.size)
        Assert.assertTrue("contains element", list.contains(tag(1)))
    }

    @Test
    fun removeElement() {
        list.add(tag(1))
        Assert.assertTrue("remove", list.remove(tag(1)))
        Assert.assertFalse("contains not", list.contains(tag(1)))
        Assert.assertEquals("size is correct", 0, list.size)
    }

    @Test
    fun containsAll() {
        list.add(tag(1))
        list.add(tag(2))
        list.add(tag(3))
        list.add(tag(4))
        Assert.assertTrue("contains all is true", list.containsAll(listOf(tag(1), tag(2), tag(3), tag(4))))
        Assert.assertFalse("contains all is false 1", list.containsAll(listOf(tag(1), tag(2), tag(5), tag(4))))
        Assert.assertTrue("contains all is false 2", list.containsAll(listOf(tag(1), tag(2), tag(3))))

    }

    fun tag(name: Any) = Tag(name.toString(), TagType.UNKNOWN)
}