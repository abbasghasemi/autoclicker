package ghasemi.abbas.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Test

class GestureWaypointsTest {
    @Test fun convertsPercentagesToScreenCoordinates() {
        assertEquals(listOf(50f to 80f, 140f to 110f),
            GestureWaypoints.parse("25,40;70,55", 200f, 200f))
    }

    @Test fun ignoresMalformedOrOutOfBoundsPoints() {
        assertEquals(listOf(100f to 0f),
            GestureWaypoints.parse("bad;101,50;50,0", 200f, 200f))
    }
}
