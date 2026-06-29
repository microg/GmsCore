package org.microg.gms.maps.mapbox.utils

data class ComparableTriple<T : Comparable<T>, S : Comparable<S>, R : Comparable<R>>(
    val first: T,
    val second: S,
    val third: R
) :
    Comparable<ComparableTriple<T, S, R>> {
    override fun compareTo(other: ComparableTriple<T, S, R>): Int {
        // Lexicographical order
        return if (first.compareTo(other.first) != 0) {
            first.compareTo(other.first)
        } else if (second.compareTo(other.second) != 0) {
            second.compareTo(other.second)
        } else {
            third.compareTo(other.third)
        }
    }

    override fun toString(): String {
        return "($first, $second, $third)"
    }
}