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

class DoubleAnimatedItemDetector : Detector(), SourceCodeScanner {

    companion object {
        private const val ANIMATED_COLUMN = "AnimatedColumn"
        private const val ANIMATED_ITEM = "animatedItem"
        private const val ANIMATED_ITEM_OWNER = "com.rend1x.composeanimatedlist.AnimatedItemModifierKt"
        private const val ANIMATED_ITEM_DEFAULTS = "com.rend1x.composeanimatedlist.animation.AnimatedItemDefaults"
        private const val MESSAGE = "AnimatedColumn already applies fade/slide with its default or non-none " +
            "transitionSpec. Use AnimatedItemDefaults.none() on the column or remove Modifier.animatedItem " +
            "from this row to avoid compounded opacity/offset."
        private val NONE_TRANSITION_SPEC_REGEX = Regex(
            pattern = """transitionSpec\s*=.*(?:AnimatedItemDefaults|\.)\.?none\s*\(""",
            option = RegexOption.DOT_MATCHES_ALL,
        )
        private val EXPLICIT_NON_NONE_REGEX = Regex(
            """AnimatedColumn\s*\([\s\S]*transitionSpec\s*=\s*(?![^\n,)]*\.?none\s*\()[\s\S]*?animatedItem\s*\(""",
        )

        val ISSUE: Issue = Issue.create(
            id = "ComposeAnimatedListDoubleAnimation",
            briefDescription = "AnimatedColumn and Modifier.animatedItem both animate the same row",
            explanation = "AnimatedColumn defaults to AnimatedItemDefaults.fadeSlide(), which drives row alpha and " +
                "offset from the list shell. Modifier.animatedItem drives alpha and offset from the item scope too. " +
                "Using both on the same row compounds the values and can make rows flicker or move too far.",
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                DoubleAnimatedItemDetector::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }

    override fun beforeCheckFile(context: Context) {
        val source = context.getContents()?.toString() ?: return
        if (!source.contains(ANIMATED_COLUMN) || !source.contains(ANIMATED_ITEM)) return

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
            visitAnimatedColumn(context, node)
        }
    }

    private fun visitAnimatedColumn(context: JavaContext, node: UCallExpression) {
        if (!node.isAnimatedColumnCall()) return
        if (node.usesNoneTransitionSpec()) return

        val animatedItemCall = node.findAnimatedItemCall()
        if (animatedItemCall == null && !node.asSourceString().contains("$ANIMATED_ITEM(")) return

        context.report(
            issue = ISSUE,
            scope = animatedItemCall ?: node,
            location = context.getLocation(animatedItemCall ?: node),
            message = MESSAGE,
        )
    }

    private fun UCallExpression.isAnimatedColumnCall(): Boolean = methodName == ANIMATED_COLUMN ||
        resolve()?.name == ANIMATED_COLUMN ||
        asSourceString().contains("$ANIMATED_COLUMN(")

    private fun UCallExpression.usesNoneTransitionSpec(): Boolean {
        val callSource = asSourceString()
        if (NONE_TRANSITION_SPEC_REGEX.containsMatchIn(callSource)) return true

        val transitionSpec = valueArguments
            .firstOrNull { it.sourcePsi?.text?.startsWith("transitionSpec") == true }
            ?: return false
        return transitionSpec.asSourceString().contains("$ANIMATED_ITEM_DEFAULTS.none()") ||
            transitionSpec.asSourceString().contains("AnimatedItemDefaults.none()")
    }

    private fun UCallExpression.findAnimatedItemCall(): UCallExpression? {
        var result: UCallExpression? = null
        accept(
            object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    if (node == this@findAnimatedItemCall) return super.visitCallExpression(node)
                    if (
                        (
                            node.methodName == ANIMATED_ITEM ||
                                node.resolve()?.name == ANIMATED_ITEM ||
                                node.asSourceString().contains(".$ANIMATED_ITEM(")
                            ) &&
                        node.isAnimatedItemCall()
                    ) {
                        result = node
                        return true
                    }
                    return super.visitCallExpression(node)
                }
            },
        )
        return result
    }

    private fun UCallExpression.isAnimatedItemCall(): Boolean {
        val owner = resolve()?.containingClass?.qualifiedName
        if (owner == ANIMATED_ITEM_OWNER) return true
        val receiverText = (uastParent as? UQualifiedReferenceExpression)?.receiver?.asSourceString()
        return owner == null ||
            receiverText?.endsWith("Modifier") == true ||
            asSourceString().contains("Modifier.$ANIMATED_ITEM(")
    }
}
