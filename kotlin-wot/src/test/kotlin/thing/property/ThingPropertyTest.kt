package ai.ancf.lmos.wot.thing.property


import org.junit.jupiter.api.Assertions.assertFalse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThingPropertyTest {
    @Test
    fun testEquals() {
        val property1 = ThingProperty<Any>(title = "title")
        val property2 = ThingProperty<Any>(title = "title")
        assertEquals(property1, property2)
    }

    @Test
    fun testHashCode() {
        val property1 = ThingProperty<Any>(title = "title").hashCode()
        val property2 = ThingProperty<Any>(title = "title").hashCode()
        assertEquals(property1, property2)
    }

    @Test
    fun testConstructor() {
        val property: ThingProperty<Any> = ThingProperty(objectType="saref:Temperature",
            type="integer",
            observable=true,
            readOnly=true,
            writeOnly=false)
        assertEquals("saref:Temperature", property.objectType)
        assertEquals("integer", property.type)
        assertTrue(property.observable)
        assertTrue(property.readOnly)
        assertFalse(property.writeOnly)
    }

}