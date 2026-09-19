package com.maisor.caleap

data class GlobalFoodProfile(
    val iso2: String,
    val country: String,
    val region: String,
    val cuisine: String,
    val representativeFoods: List<String>
)

data class FoodCandidate(
    val name: String,
    val cuisine: String,
    val countryIso2: String,
    val confidence: Double
)

object GlobalFoodEngine {
    private val profiles = listOf(
        GlobalFoodProfile("IN","India","South Asian","Indian",listOf("idli","dosa","sambar","roti","biryani","poha","upma")),
        GlobalFoodProfile("JP","Japan","East Asian","Japanese",listOf("sushi","miso soup","onigiri","ramen","tempura")),
        GlobalFoodProfile("CN","China","East Asian","Chinese",listOf("fried rice","dumplings","noodles","congee","mapo tofu")),
        GlobalFoodProfile("KR","South Korea","East Asian","Korean",listOf("bibimbap","kimchi","tteokbokki","bulgogi")),
        GlobalFoodProfile("TH","Thailand","Southeast Asian","Thai",listOf("pad thai","tom yum","green curry","mango sticky rice")),
        GlobalFoodProfile("IT","Italy","Southern European","Italian",listOf("pasta","pizza","risotto","minestrone")),
        GlobalFoodProfile("MX","Mexico","North American","Mexican",listOf("tacos","tamales","enchiladas","pozole")),
        GlobalFoodProfile("BR","Brazil","South American","Brazilian",listOf("feijoada","moqueca","pão de queijo","acarajé")),
        GlobalFoodProfile("NG","Nigeria","West African","Nigerian",listOf("jollof rice","egusi soup","pounded yam","suya")),
        GlobalFoodProfile("AU","Australia","Oceania","Australian",listOf("meat pie","avocado toast","lamington","pavlova")),
    )

    fun profileForIso2(iso2: String): GlobalFoodProfile? =
        profiles.firstOrNull { it.iso2.equals(iso2, ignoreCase = true) }

    fun search(query: String, iso2: String? = null): List<FoodCandidate> {
        val q = query.trim().lowercase()
        val scoped = if (iso2.isNullOrBlank()) profiles else
            profiles.filter { it.iso2.equals(iso2, ignoreCase = true) }

        return scoped.flatMap { profile ->
            profile.representativeFoods.mapNotNull { food ->
                val name = food.lowercase()
                val score = when {
                    name == q -> 1.0
                    name.startsWith(q) -> 0.92
                    name.contains(q) -> 0.82
                    profile.cuisine.lowercase().contains(q) -> 0.65
                    profile.country.lowercase().contains(q) -> 0.60
                    else -> return@mapNotNull null
                }
                FoodCandidate(food, profile.cuisine, profile.iso2, score)
            }
        }.sortedByDescending { it.confidence }
    }
}
