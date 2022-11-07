package com.github.ljufa.sma.backend.api


data class MatchedRule(val id: String, val tag: String, val numberOfMatches: Int)

data class TopTweetsApiRequest(val daysInPast: Int = 7, val ruleIds: List<String>? = null, val limit: Int = 12)

data class TopTweetsApiResponse(val tweetId: String, val numberOfRefs: Long)