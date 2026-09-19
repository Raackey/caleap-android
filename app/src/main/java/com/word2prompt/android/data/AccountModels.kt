package com.word2prompt.android.data

data class AccountUser(
    val id: String,
    val email: String,
    val fullName: String = "",
    val mobile: String = "",
    val preferredLanguage: String = "en"
)

data class SessionState(
    val authenticated: Boolean = false,
    val user: AccountUser? = null
)

data class UsageState(
    val authenticated: Boolean = false,
    val freeRemaining: Int = 0,
    val paidCredits: Int = 0,
    val subscriptionPlan: String? = null,
    val subscriptionRenewsAt: String? = null,
    val monthlyPricePaise: Int = 4900,
    val yearlyPricePaise: Int = 49900,
    val paygPricePaise: Int = 100,
    val canUse: Boolean = false
)

data class BillingOrder(
    val orderId: String,
    val kind: String,
    val amountPaise: Int,
    val currency: String,
    val status: String,
    val message: String
)

data class UsageConsumeResult(val ok: Boolean, val source: String, val remaining: Int?, val message: String, val usage: UsageState)
