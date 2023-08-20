package io.github.homchom.recode.lifecycle

/**
 * A type-safe implementation component of an [RModule], made up of one or more (combined) [ModuleDetail] objects.
 */
typealias ModuleFlavor<T> = ModuleDetail<ExposedModule, T>

/**
 * Builds a vanilla [RModule].
 *
 * @see ModuleFlavor
 * @see ModuleDetail.Vanilla
 */
fun module() = module(ModuleDetail.Vanilla)

/**
 * A component of an [RModule] that provides preset implementation and transforms it from one of type [T] into
 * one of type [R]. To combine details, use [plus].
 *
 * @see applyTo
 */
fun interface ModuleDetail<in T : RModule, out R : RModule> {
    fun applyTo(module: T): R

    /**
     * The default [ModuleFlavor] used by [module] and other factory functions.
     */
    object Vanilla : ModuleDetail<ExposedModule, RModule> {
        override fun applyTo(module: ExposedModule): RModule = module
    }

    /**
     * A [ModuleFlavor] used to keep modules exposed after creation.
     */
    object Exposed : ModuleDetail<ExposedModule, ExposedModule> {
        override fun applyTo(module: ExposedModule) = module
    }
}

operator fun <T : RModule, S : RModule, R : RModule> ModuleDetail<T, S>.plus(other: ModuleDetail<S, R>) =
    ModuleDetail<T, R> { other.applyTo(applyTo(it)) }

/**
 * A [ModuleDetail] for modules to be enabled by [entrypoints](https://fabricmc.net/wiki/documentation:entrypoint).
 *
 * @see module
 */
val EntrypointDetail get() = ModuleDetail<ExposedModule, ExposedModule> { module ->
    module.onEnable {
        QuitGameEvent.listenEach { module.unassert() }
    }

    module
}