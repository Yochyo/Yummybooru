package de.yochyo.booruapi.manager

import de.yochyo.booruapi.api.IBooruApi
import java.util.LinkedList

class ManagerFactoryChainElement<E : IManager>(val clazz: Class<E>, val factory: (api: IBooruApi, tagString: String, limit: Int) -> IManager?) {
    val className = clazz.name
    override fun equals(other: Any?): Boolean {
        if (other is ManagerFactoryChainElement<*>)
            return className == other.className
        return false
    }

    override fun hashCode(): Int {
        var result = clazz.hashCode()
        result = 31 * result + factory.hashCode()
        result = 31 * result + (className.hashCode() ?: 0)
        return result
    }
}

object ManagerBuilder {
    private val chain = LinkedList<ManagerFactoryChainElement<out IManager>>()

    fun <E : IManager> removeManager(clazz: Class<E>): Boolean {
        val find = chain.find { it.className == clazz.name } ?: return false
        return chain.remove(find)
    }

    fun <E : IManager, A : IManager> addManagerAfter(chainElement: ManagerFactoryChainElement<E>, after: Class<A>): Boolean {
        if (chain.find { it == chainElement } != null) return false

        val find = chain.find { it.className == after.name } ?: return false
        val index = chain.indexOf(find)
        if (index < chain.size - 1) chain.add(index + 1, chainElement)
        else chain.add(chainElement)

        return true
    }

    fun <E : IManager, A : IManager> addManagerBefore(chainElement: ManagerFactoryChainElement<E>, before: Class<A>): Boolean {
        if (chain.find { it == chainElement } != null) return false

        val find = chain.find { it.className == before.name } ?: return false
        chain.add(chain.indexOf(find), chainElement)
        return true
    }

    init {
        chain += ManagerFactoryChainElement(ManagerEach::class.java) { api: IBooruApi, tagString: String, limit: Int ->
            if (tagString.contains("EACH\\(((?!\\)[ \$]).)*\\)".toRegex())) {
                val foreach = "EACH\\(((?!\\)[ \$]).)*\\)".toRegex().findAll(tagString).toList().map { it.value }
                    .map { it.substring(5, it.length - 1) }.map { it.split(" ") }.flatten().joinToString(" ") { it }.removeMultipleSpaces()
                var tagString = "$foreach " + "EACH\\(((?!\\)[ \$]).)*\\)".toRegex().replace(tagString, "").removeMultipleSpaces()
                ManagerEach.delimiters.forEach { tagString = tagString.replace(it, "$it $foreach") }
                createManagerNonBuffered(api, tagString, limit)
            } else null
        }
        chain += ManagerFactoryChainElement(ManagerThen::class.java) { api: IBooruApi, tagString: String, limit: Int ->
            if (tagString.contains(" THEN ")) ManagerThen(tagString.split(" THEN ").map { createManagerNonBuffered(api, it, limit) })
            else null
        }
        chain += ManagerFactoryChainElement(ManagerOR::class.java) { api: IBooruApi, tagString: String, limit: Int ->
            if (tagString.contains(" OR ")) ManagerOR(tagString.split(" OR ").map { createManagerNonBuffered(api, it, limit) }, limit)
            else null
        }
        chain += ManagerFactoryChainElement(ManagerWithIdLimit::class.java) { api: IBooruApi, tagString: String, limit: Int ->
            if (tagString.contains("id:>")) {
                val find = tagString.split(" ").find { it.contains("id:>") } ?: ""
                ManagerWithIdLimit(
                    createManagerNonBuffered(api, tagString.removeFirstSequenceOfTag(find), limit),
                    find.removeFirstSequenceOfTag("id:>").toIntOrNull() ?: 0
                )
            } else null
        }
        chain += ManagerFactoryChainElement(ManagerNOT::class.java) { api: IBooruApi, tagString: String, limit: Int ->
            if (tagString.contains("NOT\\(((?!\\)[ \$]).)*\\)".toRegex())) "NOT\\(((?!\\)[ \$]).)*\\)".toRegex().let { regex ->
                ManagerNOT(
                    createManagerNonBuffered(api, regex.replace(tagString, "").removeMultipleSpaces(), limit),
                    regex.findAll(tagString).toList().map { it.value }.map { it.substring(4, it.length - 1).filter { char -> char != ' ' } })
            }
            else null
        }
        chain += ManagerFactoryChainElement(Manager::class.java) { api: IBooruApi, tagString: String, limit: Int -> Manager(api, tagString, limit) }
    }


    fun createManagerNonBuffered(api: IBooruApi, tagString: String, limit: Int): IManager {
        val chain = this.chain
        for (c in chain)
            return c.factory(api, tagString, limit) ?: continue
        throw Exception("Could not find a factory in chain to create Manager") //should never happen except if Manager::class.java is removed
    }

    fun createManager(api: IBooruApi, tagString: String, limit: Int): IManager {
        return BufferedManager(createManagerNonBuffered(api, tagString, limit))
    }

    fun toManagerOR(managers: Collection<IManager>, limit: Int) = ManagerOR(managers, limit)

    private fun String.removeFirstSequenceOfTag(sequence: String): String {
        return this.replaceFirst(sequence, "").removeMultipleSpaces()
    }

    private fun String.removeMultipleSpaces(): String {
        return split(" ").filter { it != "" }.joinToString(" ") { it }
    }
}