package no.nav.bidrag.admin.produksjonsoppfølging.utils

class Entities<T, ID : Comparable<ID>>(
    private val items: List<T>,
    private val idFunction: (T) -> ID,
) : List<T> by items {
    fun subset(filter: (T) -> Boolean) = Entities(items.filter(filter), idFunction)

    fun withLowestId(): T? = items.minWithOrNull(compareBy(idFunction))

    fun withHighestId(): T? = items.maxWithOrNull(compareBy(idFunction))

    fun withoutId(id: ID) = subset { idFunction(it) != id }

    fun without(item: T?) = subset { it != item }

    fun <V> withProperty(
        propertyFunction: (T) -> V,
        valueMatcher: (V) -> Boolean,
    ) = subset { valueMatcher(propertyFunction(it)) }

    fun <V> withProperty(
        propertyFunction: (T) -> V,
        valueMatcher: Matcher<V>,
    ) = subset { valueMatcher.matches(propertyFunction(it)) }

    fun idList() = Ids(items.map(idFunction).sorted())

    fun <V : Comparable<V>> sorterEtter(propertyFunction: (T) -> V) = sorter(compareBy(propertyFunction))

    fun <V : Comparable<V>> sorterEtterNullsLast(selector: (T) -> V?) = sorter(compareBy<T, V?>(nullsLast()) { selector(it) })

    fun <V : Comparable<V>> sorterEtterNullsFirst(selector: (T) -> V?) = sorter(compareBy<T, V?>(nullsFirst()) { selector(it) })

    fun sorter(comparator: Comparator<in T>) = Entities(items.sortedWith(comparator), idFunction)
}
