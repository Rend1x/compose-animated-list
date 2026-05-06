package com.rend1x.composeanimatedlist.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class ComposeAnimatedListIssueRegistry : IssueRegistry() {

    private companion object {
        const val MIN_LINT_API = 8
    }

    override val api: Int = CURRENT_API
    override val minApi: Int = MIN_LINT_API
    override val issues: List<Issue> = listOf(DoubleAnimatedItemDetector.ISSUE)
}
