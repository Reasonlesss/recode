package io.github.homchom.recode.render

import io.github.homchom.recode.util.MixinPrivate
import kotlin.coroutines.CoroutineContext

/**
 * A [CoroutineContext] confined to Minecraft's render thread.
 */
object RenderThreadContext : CoroutineContext {
    private lateinit var delegate: CoroutineContext
    
    @MixinPrivate
    fun init(context: CoroutineContext) {
        if (::delegate.isInitialized) error("Render thread context has already been initialized")
        delegate = context
    }
    
    override fun <R> fold(initial: R, operation: (R, CoroutineContext.Element) -> R) =
        delegate.fold(initial, operation)

    override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>) = delegate[key]

    override fun minusKey(key: CoroutineContext.Key<*>) = delegate.minusKey(key)
}