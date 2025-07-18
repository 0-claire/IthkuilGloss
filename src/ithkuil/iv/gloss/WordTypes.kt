package ithkuil.iv.gloss

fun wordTypeOf(word: Word): WordType {

    return when {
        pattern(word) {
            consonant()
        } -> WordType.BIAS_ADJUNCT

        pattern(word) {
            +"hr"
            vowel()
        } -> WordType.MOOD_CASESCOPE_ADJUNCT

        pattern(word) {
            +"h"
            vowel()
        } -> WordType.REGISTER_ADJUNCT

        pattern(word) {
            maybe("w", "y")
            // confirm { it !in setOf("ë", "äi") }
            vowel()
            maybe {
                oneOf(CN_CONSONANTS)
                maybe {
                    vowel()
                    oneOf("ň", "n")
                }
                vowel()
            }
        } -> WordType.MODULAR_ADJUNCT

        pattern(word) {
            either(
                {
                    maybe("ë")
                    consonant()
                },
                {
                    +"a"
                    oneOf(CP_CONSONANTS)
                }
            )
            vowel()
            oneOf(COMBINATION_REFERENTIAL_SPECIFICATION)
            test(slots.none { it.isConsonant() && it.isGeminateCa() })
            tail()
        } -> WordType.COMBINATION_REFERENTIAL

        pattern(word) {
            confirm { it != "ë" }
            vowel()
            consonant()
            maybe { vowel() }
        } -> WordType.AFFIXUAL_ADJUNCT

        pattern(word) {
            maybe("ë")
            consonant()
            val czGlottal = current()?.endsWith("'") ?: false
            if (czGlottal) modify { it.removeSuffix("'") }
            vowel()
            if (czGlottal) modify { "'$it" }
            oneOf(CZ_CONSONANTS)
            vowel()
            consonant()
            tail()
        } -> WordType.MULTIPLE_AFFIX_ADJUNCT

        pattern(word) {
            maybe("ë", "äi")
            referentialConsonant()
            while (current() == "ë") {
                +"ë"
                referentialConsonant()
            }
            vowel()
            maybe {
                oneOf("w", "y")
                vowel()
                maybe {
                    referentialConsonant()
                    maybe("ë")
                }
            }
        } -> WordType.REFERENTIAL

        else -> WordType.FORMATIVE
    }
}


fun pattern(word: Word, pattern: Matcher.() -> Unit): Boolean {
    val matcher = Matcher(word)
    matcher.pattern()
    if (matcher.slots.isNotEmpty()) {
        matcher.matching = false
    }
    return matcher.matching
}

class Matcher(var slots: List<String>, var matching: Boolean = true) {

    fun current() = slots.getOrNull(0)

    private fun fulfills(c: (String) -> Boolean) {
        if (!matching) return

        if (current()?.let { c(it) } == true) {
            slots = slots.drop(1)
        } else matching = false
    }

    operator fun String.unaryPlus() = fulfills { it == this }

    fun vowel() = fulfills { it.isVowel() }

    fun consonant() = fulfills { it !in CN_CONSONANTS && it.isConsonant() }

    fun referentialConsonant() = fulfills { it in CP_CONSONANTS || (it.isConsonant() && it !in CN_CONSONANTS) }

    fun oneOf(set: Collection<String>) = fulfills { it in set }

    fun oneOf(vararg options: String) = fulfills { it in options }

    fun maybe(pattern: Matcher.() -> Unit) {
        if (!matching) return

        val fork = Matcher(slots, true)

        fork.pattern()

        if (fork.matching) this.slots = fork.slots
    }

    fun maybe(vararg options: String) = maybe { oneOf(*options) }

    fun either(first: Matcher.() -> Unit, second: Matcher.() -> Unit) {
        if (!matching) return

        val fork1 = Matcher(slots, true)
        val fork2 = Matcher(slots, true)

        fork1.first()
        fork2.second()

        when {
            fork1.matching -> this.slots = fork1.slots
            fork2.matching -> this.slots = fork2.slots
            else -> matching = false
        }
    }

    fun tail() {
        if (!matching) return

        slots = emptyList()
    }

    fun confirm(predicate: (String) -> Boolean) {
        if (!matching) return

        if (current()?.let { predicate(it) } != true) matching = false
    }

    fun test(claim: Boolean) {
        if (!matching) return

        if (!claim) matching = false
    }

    fun modify(transform: (String) -> String) {
        if (!matching) return

        val cur = current()

        if (cur != null) {
            slots = listOf(transform(cur)) + slots.drop(1)
        } else {
            matching = false
        }

    }
}




