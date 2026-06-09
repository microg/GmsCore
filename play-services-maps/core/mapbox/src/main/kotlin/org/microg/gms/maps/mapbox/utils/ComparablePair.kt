package org.microg.gms.maps.mapbox.utils

data class ComparablePair<T : Comparable<T>, S : Comparable<S>>(val first: T, val second: S) :
    Comparable<ComparablePair<T, S>> {
    override fun compareTo(other: ComparablePair<T, S>): Int {
        // Lexicographical order
        val firstComparison = first.compareTo(other.first)
        return if (firstComparison != 0) {
            firstComparison
        } else {
            second.compareTo(other.second)
        }
    }

    override fun toString(): String {
        return "($first, $second)"
    }
}