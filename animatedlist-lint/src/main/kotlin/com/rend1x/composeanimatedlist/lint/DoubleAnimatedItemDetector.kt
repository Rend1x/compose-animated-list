package com.rend1x.composeanimatedlist.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor

class DoubleAnimatedItemDetector :
    Detector(),
    SourceCodeScanner {

    companion object {
        private const val ANIMATED_COLUMN = "AnimatedColumn"
        private const val ANIMATED_ROW = "AnimatedRow"
        private const val ANIMATED_ITEM = "animatedItem"
        private const val ANIMATED_ITEM_OWNER = "com.rend1x.composeanimatedlist.AnimatedItemModifierKt"
        private const val ANIMATED_ITEM_DEFAULTS = "com.rend1x.composeanimatedlist.animation.AnimatedItemDefaults"
        private const val ISSUE_PRIORITY = 6
        private const val MESSAGE = "AnimatedColumn/AnimatedRow already applies fade/slide with its default or " +
            "non-none transitionSpec. Use AnimatedItemDefaults.none() on the list or remove Modifier.animatedItem " +
            "from this item to avoid compounded opacity/offset."
        private val NONE_TRANSITION_SPEC_REGEX = Regex(
            pattern = """transitionSpec\s*=.*(?:AnimatedItemDefaults|\.)\.?none\s*\(""",
            option = RegexOption.DOT_MATCHES_ALL,
        )
        private val EXPLICIT_NON_NONE_REGEX = Regex(
            """Animated(?:Column|Row)\s*\([\s\S]*transitionSpec\s*=\s*(?![^\n,)]*\.?none\s*\()[\s\S]*?animatedItem\s*\(""",
        )

        val ISSUE: Issue = Issue.create(
            id = "ComposeAnimatedListDoubleAnimation",
            briefDescription = "Animated list shell and Modifier.animatedItem both animate the same item",
            explanation = "AnimatedColumn and AnimatedRow default to AnimatedItemDefaults.fadeSlide(), which drives item alpha and " +
                "offset from the list shell. Modifier.animatedItem drives alpha and offset from the item scope too. " +
                "Using both on the same item compounds the values and can make items flicker or move too far.",
            category = Category.CORRECTNESS,
            priority = ISSUE_PRIORITY,
            severity = Severity.WARNING,
            implementation = Implementation(
                DoubleAnimatedItemDetector::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }

    override fun beforeCheckFile(context: Context) {
        val source = context.getContents()?.toString() ?: return
        if ((!source.contains(ANIMATED_COLUMN) && !source.contains(ANIMATED_ROW)) || !source.contains(ANIMATED_ITEM)) return

        EXPLICIT_NON_NONE_REGEX.findAll(source).forEach { match ->
            val animatedItemIndex = match.value.indexOf("$ANIMATED_ITEM(")
            if (animatedItemIndex < 0) return@forEach
            val start = match.range.first + animatedItemIndex
            context.report(
                ISSUE,
                Location.create(context.file, source, start, start + ANIMATED_ITEM.length),
                MESSAGE,
            )
        }
    }

    override fun getApplicableUastTypes(): List<Class<out org.jetbrains.uast.UElement>> = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler = object : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            visitAnimatedList(context, node)
        }
    }

    private fun visitAnimatedList(context: JavaContext, node: UCallExpression) {
        val shouldInspect = node.isAnimatedListCall() && !node.usesNoneTransitionSpec()
        if (!shouldInspect) return

        val animatedItemCall = node.findAnimatedItemCall()
        val reportTarget = animatedItemCall ?: node.takeIf { it.asSourceString().contains("$ANIMATED_ITEM(") }

        if (reportTarget != null) {
            context.report(
                issue = ISSUE,
                scope = reportTarget,
                location = context.getLocation(reportTarget),
                message = MESSAGE,
            )
        }
    }

    private fun UCallExpression.isAnimatedListCall(): Boolean = methodName == ANIMATED_COLUMN ||
        methodName == ANIMATED_ROW ||
        resolve()?.name == ANIMATED_COLUMN ||
        resolve()?.name == ANIMATED_ROW ||
        asSourceString().contains("$ANIMATED_COLUMN(") ||
        asSourceString().contains("$ANIMATED_ROW(")

    private fun UCallExpression.usesNoneTransitionSpec(): Boolean = NONE_TRANSITION_SPEC_REGEX.containsMatchIn(asSourceString()) ||
        valueArguments
            .firstOrNull { it.sourcePsi?.text?.startsWith("transitionSpec") == true }
            ?.asSourceString()
            ?.isNoneTransitionSpecSource()
            .orFalse()

    private fun UCallExpression.findAnimatedItemCall(): UCallExpression? {
        var result: UCallExpression? = null
        accept(
            object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean = when {
                    node == this@findAnimatedItemCall -> super.visitCallExpression(node)
                    node.isAnimatedItemCandidate() && node.isAnimatedItemCall() -> {
                        result = node
                        true
                    }
                    else -> super.visitCallExpression(node)
                }
            },
        )
        return result
    }

    private fun UCallExpression.isAnimatedItemCandidate(): Boolean = methodName == ANIMATED_ITEM ||
        resolve()?.name == ANIMATED_ITEM ||
        asSourceString().contains(".$ANIMATED_ITEM(")

    private fun UCallExpression.isAnimatedItemCall(): Boolean {
        val owner = resolve()?.containingClass?.qualifiedName
        if (owner == ANIMATED_ITEM_OWNER) return true
        val receiverText = (uastParent as? UQualifiedReferenceExpression)?.receiver?.asSourceString()
        return owner == null ||
            receiverText?.endsWith("Modifier") == true ||
            asSourceString().contains("Modifier.$ANIMATED_ITEM(")
    }

    private fun String.isNoneTransitionSpecSource(): Boolean =
        contains("$ANIMATED_ITEM_DEFAULTS.none()") || contains("AnimatedItemDefaults.none()")

    private fun Boolean?.orFalse(): Boolean = this == true
}
