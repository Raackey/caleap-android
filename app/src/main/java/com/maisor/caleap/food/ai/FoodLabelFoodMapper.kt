package com.maisor.caleap.food.ai

import com.maisor.caleap.FoodItem

/**
 * Converts generic vision labels into known food candidates without
 * manufacturing an identity when no catalog term matches.
 */
object FoodLabelFoodMapper {
    fun map(
        vision: List<RankedFoodCandidate>,
        catalog: List<FoodItem>
    ): List<FoodItem> {
        return vision
            .filter { it.confidence >= 0.30f }
            .flatMap { candidate ->
                catalog.filter { item ->
                    val terms = listOf(item.name) + item.aliases
                    terms.any { term ->
                        val t = term.lowercase().trim()
                        val l = candidate.label.lowercase().trim()
                        l == t || l.contains(t) || t.contains(l)
                    }
                }
            }
            .distinctBy { it.name }
    }
}
