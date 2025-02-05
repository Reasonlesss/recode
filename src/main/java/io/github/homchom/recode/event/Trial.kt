package io.github.homchom.recode.event

import io.github.homchom.recode.lifecycle.ExposedModule
import io.github.homchom.recode.lifecycle.RModule
import kotlinx.coroutines.flow.map

/**
 * Creates a [DetectorTrial] with the given [basis] and [tests].
 */
fun <T : Any, B, R : Any> trial(basis: Listenable<B>, tests: DetectorTrial.Tester<T, B, R>): DetectorTrial<T, R> =
    BasedDetectorTrial(basis, tests)

/**
 * Creates a [RequesterTrial] with the given [basis], [start], and [tests].
 */
fun <T : Any, B, R : Any> trial(
    basis: Listenable<B>,
    start: suspend (input: T) -> Unit,
    tests: RequesterTrial.Tester<T, B, R>
): RequesterTrial<T, R> {
    return shortCircuitTrial(basis, { start(it); null }, tests)
}

/**
 * Creates a short-circuit [RequesterTrial] that can optionally return without requesting when [start] returns a
 * non-null result.
 *
 * @see trial
 */
fun <T : Any, B, R : Any> shortCircuitTrial(
    basis: Listenable<B>,
    start: suspend (input: T) -> R?,
    tests: RequesterTrial.Tester<T, B, R>
): RequesterTrial<T, R> {
    return BasedRequesterTrial(basis, start, tests)
}

/**
 * Creates a [DetectorTrial] with a nullary tester.
 *
 * @see trial
 * @see DetectorTrial.NullaryTester
 */
fun <B, R : Any> nullaryTrial(
    basis: Listenable<B>,
    tests: DetectorTrial.NullaryTester<B, R>
): DetectorTrial<Unit, R> {
    return trial(basis, tests.toUnary())
}

/**
 * Creates a [RequesterTrial] with a nullary tester.
 *
 * @see trial
 * @see RequesterTrial.NullaryTester
 */
fun <B, R : Any> nullaryTrial(
    basis: Listenable<B>,
    start: suspend () -> Unit,
    tests: RequesterTrial.NullaryTester<B, R>
): RequesterTrial<Unit, R> {
    return trial(basis, { start() }, tests.toUnary())
}

/**
 * An object that can run tests and supply its results from an [RModule]. For an explanation of the Trial DSL,
 * see [TrialScope].
 *
 * @property basis The event that the trial is based on, and the starting point of each trial run.
 *
 * @see trial
 */
sealed interface Trial<S> {
    val basis: Listenable<*>

    fun supplyResultsFrom(module: ExposedModule): Listenable<S>
}

/**
 * A [Trial] that supplies to [Detector] events.
 */
sealed interface DetectorTrial<T : Any, R : Any> : Trial<DetectorTrial.ResultSupplier<T, R>> {
    /**
     * @see runTests
     */
    fun interface Tester<T : Any, B, R : Any> {
        fun TrialScope.runTests(input: T?, baseContext: B): TrialResult<R>?

        fun runTestsIn(scope: TrialScope, input: T?, baseContext: B) = scope.runTests(input, baseContext)
    }

    /**
     * @see runTests
     */
    fun interface NullaryTester<B, R : Any> {
        fun TrialScope.runTests(baseContext: B): TrialResult<R>?

        /**
         * Converts this NullaryTester to a unary [Tester].
         */
        @Suppress("RemoveRedundantQualifierName")
        fun toUnary() = DetectorTrial.Tester<Unit, B, R> { _, baseContext -> runTests(baseContext) }
    }

    /**
     * @see supply
     */
    fun interface ResultSupplier<T : Any, R : Any> {
        fun TrialScope.supply(input: T?): TrialResult<R>?

        fun supplyIn(scope: TrialScope, input: T?) = scope.supply(input)
    }
}

/**
 * A [Trial] that supplies to [Requester] events.
 *
 * @property start The executor function that starts the request.
 */
sealed interface RequesterTrial<T : Any, R : Any> : Trial<RequesterTrial.ResultSupplier<T, R>> {
    val start: suspend (input: T) -> R?

    /**
     * @see runTests
     */
    fun interface Tester<T : Any, B, R : Any> {
        fun TrialScope.runTests(input: T?, baseContext: B, isRequest: Boolean): TrialResult<R>?

        fun runTestsIn(scope: TrialScope, input: T?, baseContext: B, isRequest: Boolean) =
            scope.runTests(input, baseContext, isRequest)
    }

    /**
     * @see runTests
     */
    fun interface NullaryTester<B, R : Any> {
        fun TrialScope.runTests(baseContext: B, isRequest: Boolean): TrialResult<R>?

        /**
         * Converts this NullaryTester to a unary [Tester].
         */
        @Suppress("RemoveRedundantQualifierName")
        fun toUnary() = RequesterTrial.Tester<Unit, B, R> { _, baseContext, isRequest ->
            runTests(baseContext, isRequest)
        }
    }

    /**
     * @see supply
     */
    fun interface ResultSupplier<T : Any, R : Any> {
        fun TrialScope.supply(input: T?, isRequest: Boolean): TrialResult<R>?

        fun supplyIn(scope: TrialScope, input: T?, isRequest: Boolean) =
            scope.supply(input, isRequest)
    }
}

private class BasedDetectorTrial<T : Any, B, R : Any>(
    override val basis: Listenable<B>,
    private val tests: DetectorTrial.Tester<T, B, R>
) : DetectorTrial<T, R> {
    override fun supplyResultsFrom(module: ExposedModule) =
        FlowListenable(basis.getNotificationsFrom(module).map { baseContext ->
            DetectorTrial.ResultSupplier<T, R> { input ->
                tests.runTestsIn(this, input, baseContext)
            }
        })
}

private class BasedRequesterTrial<T : Any, B, R : Any>(
    override val basis: Listenable<B>,
    override val start: suspend (input: T) -> R?,
    private val tests: RequesterTrial.Tester<T, B, R>
): RequesterTrial<T, R> {
    override fun supplyResultsFrom(module: ExposedModule) =
        FlowListenable(basis.getNotificationsFrom(module).map { baseContext ->
            RequesterTrial.ResultSupplier<T, R> { input, isRequest ->
                tests.runTestsIn(this, input, baseContext, isRequest)
            }
        })
}