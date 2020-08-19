package mu

import mu.internal.MarkerMingw

actual object KMarkerFactory {

    actual fun getMarker(name: String): Marker = MarkerMingw(name)
}
