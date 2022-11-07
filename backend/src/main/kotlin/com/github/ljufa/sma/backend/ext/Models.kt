package com.github.ljufa.sma.backend.ext

data class Rule(val id: String?, val value: String, val tag: String)
data class Rules(val data: List<Rule>)

data class AddRule(val value: String, val tag: String)
data class AddRequest(val add: List<AddRule>)

data class DeleteRequest(val delete: List<String>)