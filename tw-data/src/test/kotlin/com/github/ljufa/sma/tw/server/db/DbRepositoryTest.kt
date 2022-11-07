package com.github.ljufa.sma.tw.server.db

import com.github.ljufa.sma.tw.SetupDataExtension
import com.github.ljufa.sma.tw.server.grpc.TopTweetsRequest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SetupDataExtension::class)

class DbRepositoryTest {

    @Test
    fun test_findAllHashTags() {
        Assertions.assertThat(DbRepository.findAllMatchedRules().ruleList).hasSize(1)
    }

    @Test
    fun test_findAllLanguages() {
        Assertions.assertThat(DbRepository.findAllLanguages().languageList).hasSize(2)
    }

    @Test
    fun test_getTopTweets_byRuleAndLanguage() {
        val list = DbRepository.getTopTweets(
            TopTweetsRequest.newBuilder()
                .setLimit(20)
                .setDaysFromNow(Int.MAX_VALUE)
                .addIncludeRuleIds("1")
                .addIncludeLanguages("en")
                .addIncludeLanguages("fr")
                .build()
        ).statsList
        Assertions.assertThat(list).hasSize(5)
        Assertions.assertThat(list)
            .isSortedAccordingTo { o1, o2 -> o1.numberOfRefs.compareTo(o2.numberOfRefs) * -1 }
    }

    @Test
    fun test_getTopTweets_excludeSensitive() {
        val list = DbRepository.getTopTweets(
            TopTweetsRequest.newBuilder()
                .setLimit(20)
                .setDaysFromNow(Int.MAX_VALUE)
                .addIncludeRuleIds("1")
                .setExcludePossiblySensitive(true)
                .build()
        ).statsList
        Assertions.assertThat(list).hasSize(3)
    }
}


