package dev.olshevski.navigation.reimagined

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStore

/**
 * An animated navigation host that selects UI for every destination and provides saved state and
 * Android architecture components (Lifecycle, ViewModelStore, SavedStateRegistry) through
 * [CompositionLocalProvider] for every unique [NavEntry] in the [controller's][controller]
 * backstack.
 *
 * This composable uses animated transitions to switch between destinations. You may set a custom
 * [NavTransitionSpec] to specify the desired transitions.
 *
 * If you don't need animated transitions use [NavHost] instead.
 *
 * @param controller the navigation controller that will provide its backstack to this
 * AnimatedNavHost. The last entry of the backstack is always the currently displayed entry.
 * You should do all backstack modifications through the same instance of [NavController],
 * but setting a different [NavController] will be handled correctly.
 *
 * @param modifier the modifier to be applied to AnimatedNavHost
 *
 * @param transitionSpec specifies the desired transitions. If not set, the default transition
 * will be a simple crossfade.
 *
 * @param transitionQueueing the strategy of processing incoming transitions when transition
 * animations run slower than being added
 *
 * @param contentAlignment the alignment inside AnimatedNavHost
 *
 * @param emptyBackstackPlaceholder an optional placeholder composable that will
 * be displayed when the backstack is empty. In the majority of cases you don't need
 * to set this. Note that the provided composable wouldn't get its own scoped components.
 *
 * @param contentSelector provides a composable that corresponds to the current last destination
 * in the backstack. In other words, here is where you select UI to show (e.g. a screen). Also,
 * provides additional functionality of the AnimatedNavHost through the [AnimatedNavHostScope].
 */
@Composable
fun <T> AnimatedNavHost(
    controller: NavController<T>,
    modifier: Modifier = Modifier,
    transitionSpec: NavTransitionSpec<T> = CrossfadeTransitionSpec,
    transitionQueueing: NavTransitionQueueing = NavTransitionQueueing.InterruptCurrent,
    contentAlignment: Alignment = Alignment.TopStart,
    emptyBackstackPlaceholder: @Composable AnimatedVisibilityScope.() -> Unit = {},
    contentSelector: @Composable AnimatedNavHostScope<T>.(destination: T) -> Unit
) = @OptIn(ExperimentalReimaginedApi::class) ScopingAnimatedNavHost(
    state = rememberScopingNavHostState(controller.backstack, EmptyScopeSpec),
    modifier = modifier,
    transitionSpec = transitionSpec,
    transitionQueueing = transitionQueueing,
    contentAlignment = contentAlignment,
    emptyBackstackPlaceholder = emptyBackstackPlaceholder,
    contentSelector = contentSelector
)

/**
 * An animated navigation host that selects UI for every destination and provides saved state and
 * Android architecture components (Lifecycle, ViewModelStore, SavedStateRegistry) through
 * [CompositionLocalProvider] for every unique [NavEntry] in the [backstack].
 *
 * This composable uses animated transitions to switch between destinations. You may set a custom
 * [NavTransitionSpec] to specify the desired transitions.
 *
 * If you don't need animated transitions use [NavHost] instead.
 *
 * @param backstack the backstack from a [NavController] that will be used to observe navigation
 * changes. The last entry of the backstack is always the currently displayed entry.
 * You should do all backstack modifications through the same instance of [NavController],
 * but using a different [NavController] and setting its backstack will be handled correctly.
 *
 * @param modifier the modifier to be applied to AnimatedNavHost
 *
 * @param transitionSpec specifies the desired transitions. If not set, the default transition
 * will be a simple crossfade.
 *
 * @param transitionQueueing the strategy of processing incoming transitions when transition
 * animations run slower than being added
 *
 * @param contentAlignment the alignment inside AnimatedNavHost
 *
 * @param emptyBackstackPlaceholder an optional placeholder composable that will
 * be displayed when the backstack is empty. In the majority of cases you don't need
 * to set this. Note that the provided composable wouldn't get its own scoped components.
 *
 * @param contentSelector provides a composable that corresponds to the current last destination
 * in the backstack. In other words, here is where you select UI to show (e.g. a screen). Also,
 * provides additional functionality of the AnimatedNavHost through the [AnimatedNavHostScope].
 */
@Composable
fun <T> AnimatedNavHost(
    backstack: NavBackstack<T>,
    modifier: Modifier = Modifier,
    transitionSpec: NavTransitionSpec<T> = CrossfadeTransitionSpec,
    transitionQueueing: NavTransitionQueueing = NavTransitionQueueing.InterruptCurrent,
    contentAlignment: Alignment = Alignment.TopStart,
    emptyBackstackPlaceholder: @Composable AnimatedVisibilityScope.() -> Unit = {},
    contentSelector: @Composable AnimatedNavHostScope<T>.(destination: T) -> Unit
) = @OptIn(ExperimentalReimaginedApi::class) ScopingAnimatedNavHost(
    state = rememberScopingNavHostState(backstack, EmptyScopeSpec),
    modifier = modifier,
    transitionSpec = transitionSpec,
    transitionQueueing = transitionQueueing,
    contentAlignment = contentAlignment,
    emptyBackstackPlaceholder = emptyBackstackPlaceholder,
    contentSelector = contentSelector
)

/**
 * An animated navigation host that selects UI for every destination and provides saved state and
 * Android architecture components (Lifecycle, ViewModelStore, SavedStateRegistry) through
 * [CompositionLocalProvider] for every unique [NavEntry] in the backstack.
 *
 * This composable uses animated transitions to switch between destinations. You may set a custom
 * [NavTransitionSpec] to specify the desired transitions.
 *
 * If you don't need animated transitions use [NavHost] instead.
 *
 * @param state the holder of all internal AnimatedNavHost state. Stores and manages saved state
 * and all Android architecture components (Lifecycle, ViewModelStore, SavedStateRegistry)
 * for every entry.
 *
 * @param modifier the modifier to be applied to AnimatedNavHost
 *
 * @param transitionSpec specifies the desired transitions. If not set, the default transition
 * will be a simple crossfade.
 *
 * @param transitionQueueing the strategy of processing incoming transitions when transition
 * animations run slower than being added
 *
 * @param contentAlignment the alignment inside AnimatedNavHost
 *
 * @param emptyBackstackPlaceholder an optional placeholder composable that will
 * be displayed when the backstack is empty. In the majority of cases you don't need
 * to set this. Note that the provided composable wouldn't get its own scoped components.
 *
 * @param contentSelector provides a composable that corresponds to the current last destination
 * in the backstack. In other words, here is where you select UI to show (e.g. a screen). Also,
 * provides additional functionality of the AnimatedNavHost through the [AnimatedNavHostScope].
 */
@ExperimentalReimaginedApi
@Composable
fun <T> AnimatedNavHost(
    state: NavHostState<T>,
    modifier: Modifier = Modifier,
    transitionSpec: NavTransitionSpec<T> = CrossfadeTransitionSpec,
    transitionQueueing: NavTransitionQueueing = NavTransitionQueueing.InterruptCurrent,
    contentAlignment: Alignment = Alignment.TopStart,
    emptyBackstackPlaceholder: @Composable AnimatedVisibilityScope.() -> Unit = {},
    contentSelector: @Composable AnimatedNavHostScope<T>.(destination: T) -> Unit
) = @Suppress("UNCHECKED_CAST") ScopingAnimatedNavHost(
    state = state as ScopingNavHostState<T, Nothing>,
    modifier = modifier,
    transitionSpec = transitionSpec,
    transitionQueueing = transitionQueueing,
    contentAlignment = contentAlignment,
    emptyBackstackPlaceholder = emptyBackstackPlaceholder,
    contentSelector = contentSelector
)

/**
 * An animated navigation host that selects UI for every destination and provides saved state and
 * Android architecture components (Lifecycle, ViewModelStore, SavedStateRegistry) through
 * [CompositionLocalProvider] for every unique [NavEntry] in the [controller's][controller]
 * backstack.
 *
 * This composable uses animated transitions to switch between destinations. You may set a custom
 * [NavTransitionSpec] to specify the desired transitions.
 *
 * If you don't need animated transitions use [NavHost] instead.
 *
 * **Scoping:** This version of AnimatedNavHost gives you the ability to define scopes.
 * Read more about it in [NavScopeSpec].
 *
 * @param controller the navigation controller that will provide its backstack to this
 * AnimatedNavHost. The last entry of the backstack is always the currently displayed entry.
 * You should do all backstack modifications through the same instance of [NavController],
 * but setting a different [NavController] will be handled correctly.
 *
 * @param scopeSpec specifies scopes for every destination. This gives you the ability to easily
 * create and access scoped [ViewModelStores][ViewModelStore].
 *
 * @param modifier the modifier to be applied to AnimatedNavHost
 *
 * @param transitionSpec specifies the desired transitions. If not set, the default transition
 * will be a simple crossfade.
 *
 * @param transitionQueueing the strategy of processing incoming transitions when transition
 * animations run slower than being added
 *
 * @param contentAlignment the alignment inside AnimatedNavHost
 *
 * @param emptyBackstackPlaceholder an optional placeholder composable that will
 * be displayed when the backstack is empty. In the majority of cases you don't need
 * to set this. Note that the provided composable wouldn't get its own scoped components.
 *
 * @param contentSelector provides a composable that corresponds to the current last destination
 * in the backstack. In other words, here is where you select UI to show (e.g. a screen). Also,
 * provides additional functionality of the ScopingAnimatedNavHost through
 * the [ScopingAnimatedNavHostScope].
 */
@Composable
fun <T, S> ScopingAnimatedNavHost(
    controller: NavController<T>,
    scopeSpec: NavScopeSpec<T, S>,
    modifier: Modifier = Modifier,
    transitionSpec: NavTransitionSpec<T> = CrossfadeTransitionSpec,
    transitionQueueing: NavTransitionQueueing = NavTransitionQueueing.InterruptCurrent,
    contentAlignment: Alignment = Alignment.TopStart,
    emptyBackstackPlaceholder: @Composable AnimatedVisibilityScope.() -> Unit = {},
    contentSelector: @Composable ScopingAnimatedNavHostScope<T, S>.(destination: T) -> Unit
) = @OptIn(ExperimentalReimaginedApi::class) ScopingAnimatedNavHost(
    state = rememberScopingNavHostState(controller.backstack, scopeSpec),
    modifier = modifier,
    transitionSpec = transitionSpec,
    transitionQueueing = transitionQueueing,
    contentAlignment = contentAlignment,
    emptyBackstackPlaceholder = emptyBackstackPlaceholder,
    contentSelector = contentSelector
)

/**
 * An animated navigation host that selects UI for every destination and provides saved state and
 * Android architecture components (Lifecycle, ViewModelStore, SavedStateRegistry) through
 * [CompositionLocalProvider] for every unique [NavEntry] in the [backstack].
 *
 * This composable uses animated transitions to switch between destinations. You may set a custom
 * [NavTransitionSpec] to specify the desired transitions.
 *
 * If you don't need animated transitions use [NavHost] instead.
 *
 * **Scoping:** This version of AnimatedNavHost gives you the ability to define scopes.
 * Read more about it in [NavScopeSpec].
 *
 * @param backstack the backstack from a [NavController] that will be used to observe navigation
 * changes. The last entry of the backstack is always the currently displayed entry.
 * You should do all backstack modifications through the same instance of [NavController],
 * but using a different [NavController] and setting its backstack will be handled correctly.
 *
 * @param scopeSpec specifies scopes for every destination. This gives you the ability to easily
 * create and access scoped [ViewModelStores][ViewModelStore].
 *
 * @param transitionSpec specifies the desired transitions. If not set, the default transition
 * will be a simple crossfade.
 *
 * @param transitionQueueing the strategy of processing incoming transitions when transition
 * animations run slower than being added
 *
 * @param contentAlignment the alignment inside AnimatedNavHost
 *
 * @param emptyBackstackPlaceholder an optional placeholder composable that will
 * be displayed when the backstack is empty. In the majority of cases you don't need
 * to set this. Note that the provided composable wouldn't get its own scoped components.
 *
 * @param contentSelector provides a composable that corresponds to the current last destination
 * in the backstack. In other words, here is where you select UI to show (e.g. a screen). Also,
 * provides additional functionality of the ScopingAnimatedNavHost through
 * the [ScopingAnimatedNavHostScope].
 */
@Composable
fun <T, S> ScopingAnimatedNavHost(
    backstack: NavBackstack<T>,
    scopeSpec: NavScopeSpec<T, S>,
    modifier: Modifier = Modifier,
    transitionSpec: NavTransitionSpec<T> = CrossfadeTransitionSpec,
    transitionQueueing: NavTransitionQueueing = NavTransitionQueueing.InterruptCurrent,
    contentAlignment: Alignment = Alignment.TopStart,
    emptyBackstackPlaceholder: @Composable AnimatedVisibilityScope.() -> Unit = {},
    contentSelector: @Composable ScopingAnimatedNavHostScope<T, S>.(destination: T) -> Unit
) = @OptIn(ExperimentalReimaginedApi::class) ScopingAnimatedNavHost(
    state = rememberScopingNavHostState(backstack, scopeSpec),
    modifier = modifier,
    transitionSpec = transitionSpec,
    transitionQueueing = transitionQueueing,
    contentAlignment = contentAlignment,
    emptyBackstackPlaceholder = emptyBackstackPlaceholder,
    contentSelector = contentSelector
)

/**
 * An animated navigation host that selects UI for every destination and provides saved state and
 * Android architecture components (Lifecycle, ViewModelStore, SavedStateRegistry) through
 * [CompositionLocalProvider] for every unique [NavEntry] in the backstack.
 *
 * This composable uses animated transitions to switch between destinations. You may set a custom
 * [NavTransitionSpec] to specify the desired transitions.
 *
 * If you don't need animated transitions use [NavHost] instead.
 *
 * **Scoping:** This version of AnimatedNavHost gives you the ability to define scopes.
 * Read more about it in [NavScopeSpec].
 *
 * @param state the holder of all internal ScopingAnimatedNavHost state. Stores and manages saved
 * state and all Android architecture components (Lifecycle, ViewModelStore, SavedStateRegistry)
 * for every entry and every scope.
 *
 * @param modifier the modifier to be applied to AnimatedNavHost
 *
 * @param transitionSpec specifies the desired transitions. If not set, the default transition
 * will be a simple crossfade.
 *
 * @param transitionQueueing the strategy of processing incoming transitions when transition
 * animations run slower than being added
 *
 * @param contentAlignment the alignment inside AnimatedNavHost
 *
 * @param emptyBackstackPlaceholder an optional placeholder composable that will
 * be displayed when the backstack is empty. In the majority of cases you don't need
 * to set this. Note that the provided composable wouldn't get its own scoped components.
 *
 * @param contentSelector provides a composable that corresponds to the current last destination
 * in the backstack. In other words, here is where you select UI to show (e.g. a screen). Also,
 * provides additional functionality of the ScopingAnimatedNavHost through
 * the [ScopingAnimatedNavHostScope].
 */
@ExperimentalReimaginedApi
@Composable
fun <T, S> ScopingAnimatedNavHost(
    state: ScopingNavHostState<T, S>,
    modifier: Modifier = Modifier,
    transitionSpec: NavTransitionSpec<T> = CrossfadeTransitionSpec,
    transitionQueueing: NavTransitionQueueing = NavTransitionQueueing.InterruptCurrent,
    contentAlignment: Alignment = Alignment.TopStart,
    emptyBackstackPlaceholder: @Composable AnimatedVisibilityScope.() -> Unit = {},
    contentSelector: @Composable ScopingAnimatedNavHostScope<T, S>.(T) -> Unit
) = BaseNavHost(
    state = state,
    transitionQueueing = transitionQueueing
) { targetSnapshot ->
    // Key AnimatedContent on the last entry id (a tiny opaque NavId) rather than
    // the full NavSnapshot. AnimatedContent retains its transition-state keys in
    // an internal scatter map (AnimatedContentTransitionScopeImpl.targetSizeMap);
    // using NavSnapshot as S caused that map to pin a full NavHostEntry graph per
    // navigation forever. With NavId as S the map still grows, but only by ~20 B
    // per unique destination.
    val targetKey = targetSnapshot.items.lastOrNull()?.hostEntry?.id

    // Look-up table so the transitionSpec and content lambda can still get the
    // full NavSnapshot for a given key. Kept as a plain remembered map (no
    // MutableStateMap) — writes don't need to invalidate readers, we only read
    // values that we've just written in the same recomposition.
    val snapshotsByKey = remember { HashMap<NavId?, NavSnapshot<T, S>>() }
    snapshotsByKey[targetKey] = targetSnapshot

    val transition = updateTransition(
        targetState = targetKey,
        label = "AnimatedNavHost"
    )

    transition.AnimatedContent(
        modifier = modifier,
        transitionSpec = {
            val initialEntry = snapshotsByKey[initialState]?.items?.lastOrNull()?.hostEntry
            val targetEntry = snapshotsByKey[targetState]?.items?.lastOrNull()?.hostEntry
            val action = snapshotsByKey[targetState]?.action ?: NavAction.Idle
            selectTransition(transitionSpec, action, initialEntry, targetEntry)
        },
        contentAlignment = contentAlignment
    ) { key ->
        // Capture the snapshot for this content instance at first composition.
        // The captured reference lives only as long as this content is retained
        // by AnimatedContent; when the exit animation finishes and the content
        // is disposed, the capture is released along with it.
        val capturedSnapshot = remember { snapshotsByKey[key] }
        val lastSnapshotItem = capturedSnapshot?.items?.lastOrNull()
        if (lastSnapshotItem != null) {
            lastSnapshotItem.ComponentsProvider {
                val animatedVisibilityScope = this@AnimatedContent
                val scope = remember(capturedSnapshot, animatedVisibilityScope) {
                    ScopingAnimatedNavHostScopeImpl(
                        hostEntries = capturedSnapshot.items.map { it.hostEntry },
                        scopedHostEntries = lastSnapshotItem.scopedHostEntries,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
                scope.contentSelector(lastSnapshotItem.hostEntry.destination)
            }
        } else {
            emptyBackstackPlaceholder()
        }
    }

    // Once the transition has settled, drop entries for keys that are no longer
    // showing. Runs after a successful composition, so the exit animations for
    // those keys have already completed and their content has been disposed.
    SideEffect {
        if (transition.currentState == transition.targetState) {
            snapshotsByKey.keys.retainAll(setOf(targetKey))
        }
    }

    return@BaseNavHost snapshotsByKey[transition.currentState] ?: targetSnapshot
}

private fun <T> AnimatedContentTransitionScope<NavId?>.selectTransition(
    transitionSpec: NavTransitionSpec<T>,
    action: NavAction,
    initialStateLastEntry: NavHostEntry<T>?,
    targetStateLastEntry: NavHostEntry<T>?,
): ContentTransform {
    // Request transition spec only when anything actually changes and should be animated.
    // For some reason AnimatedContent calls for transitionSpec even when created initially
    // which doesn't make much sense.
    return if (initialStateLastEntry?.id != targetStateLastEntry?.id) {
        val scope = NavTransitionScopeImpl(this)
        with(transitionSpec) {
            when {
                initialStateLastEntry == null -> scope.fromEmptyBackstack(
                    action = action,
                    to = targetStateLastEntry!!.destination
                )

                targetStateLastEntry == null -> scope.toEmptyBackstack(
                    action = action,
                    from = initialStateLastEntry.destination
                )

                else -> scope.getContentTransform(
                    action = action,
                    from = initialStateLastEntry.destination,
                    to = targetStateLastEntry.destination
                )
            }
        }
    } else {
        EnterTransition.None togetherWith ExitTransition.None
    }
}
