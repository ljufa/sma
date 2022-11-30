package com.github.ljufa.sma.tw.server.db

import com.github.ljufa.sma.tw.server.api.TopTweetsRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DbRepositoryTest : DbSetupTest() {

    @Test
    fun test_findAllMatchedRules() {
        val rules = dbRepository.findAllMatchedRules()
        assertThat(rules.ruleList).hasSize(2)
    }

    @Test
    fun test_getTopTweets_byRule() {
        val list = dbRepository.getTopTweets(
            TopTweetsRequest.newBuilder()
                .setLimit(20)
                .setDaysFromNow(Int.MAX_VALUE)
                .addIncludeRuleIds("1")
                .build()
        ).statsList
        assertThat(list).hasSize(3)
        assertThat(list[0].numberOfRefs).isEqualTo(2)
        assertThat(list)
            .isSortedAccordingTo { o1, o2 -> o1.numberOfRefs.compareTo(o2.numberOfRefs) * -1 }
    }

    @Test
    fun test_getTopTweets_excludeSensitive() {
        val list = dbRepository.getTopTweets(
            TopTweetsRequest.newBuilder()
                .setLimit(20)
                .setDaysFromNow(Int.MAX_VALUE)
                .addIncludeRuleIds("2")
                .setExcludePossiblySensitive(true)
                .build()
        ).statsList
        assertThat(list).hasSize(1)
        assertThat(list[0].tweetId).isNotEqualTo("5")
    }

    @Test
    fun test_getTopTweets_limit_is_applied() {
        val list = dbRepository.getTopTweets(
            TopTweetsRequest.newBuilder()
                .setLimit(2)
                .setDaysFromNow(Int.MAX_VALUE)
                .addIncludeRuleIds("1")
                .addIncludeRuleIds("2")
                .build()
        ).statsList
        assertThat(list).hasSize(2)
    }
}


