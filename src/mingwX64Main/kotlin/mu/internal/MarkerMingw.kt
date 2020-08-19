package mu.internal

import mu.Marker

internal class MarkerMingw(private val name: String) : Marker {

    override fun getName(): String = this.name
}
