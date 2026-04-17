package dev.olshevski.navigation.reimagined

import android.app.Application
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import kotlinx.parcelize.Parcelize

/**
 * Remembers [NavHostState]. This allows you to hoist the state of NavHost and
 * conditionally remove it from composition without losing saved states and created architecture
 * components (Lifecycle, ViewModelStore, SavedStateRegistry) of every NavHost entry.
 *
 * If you do want to remove NavHost from composition and clear all its state, use
 * [NavHostVisibility] or [NavHostAnimatedVisibility] instead.
 *
 * @param backstack the backstack from a [NavController] that will be used to observe navigation
 * changes. The last entry of the backstack is always the currently displayed entry.
 * You should do all backstack modifications through the same instance of [NavController],
 * but using a different [NavController] and setting its backstack will be handled correctly.
 */
@ExperimentalReimaginedApi
@Composable
fun <T> rememberNavHostState(
    backstack: NavBackstack<T>
): NavHostState<T> = rememberNavHostStateImpl(backstack, EmptyScopeSpec)

/**
 * Remembers [ScopingNavHostState]. This allows you to hoist the state of ScopingNavHost and
 * conditionally remove it from composition without losing saved states and created architecture
 * components (Lifecycle, ViewModelStore, SavedStateRegistry) of every ScopingNavHost entry.
 *
 * If you do want to remove ScopingNavHost from composition and clear all its state, use
 * [NavHostVisibility] or [NavHostAnimatedVisibility] instead.
 *
 * @param backstack the backstack from a [NavController] that will be used to observe navigation
 * changes. The last entry of the backstack is always the currently displayed entry.
 * You should do all backstack modifications through the same instance of [NavController],
 * but using a different [NavController] and setting its backstack will be handled correctly.
 *
 * @param scopeSpec specifies scopes for every destination. This gives you the ability to easily
 * create and access scoped [ViewModelStores][ViewModelStore].
 */
@ExperimentalReimaginedApi
@Composable
fun <T, S> rememberScopingNavHostState(
    backstack: NavBackstack<T>,
    scopeSpec: NavScopeSpec<T, S>
): ScopingNavHostState<T, S> = rememberNavHostStateImpl(backstack, scopeSpec)

@VisibleForTesting
@Composable
internal fun <T, S> rememberNavHostStateImpl(
    backstack: NavBackstack<T>,
    scopeSpec: NavScopeSpec<T, S>,
    onHostEntryCreated: ((NavHostEntry<T>) -> Unit)? = null
): NavHostStateImpl<T, S> {
    val saveableStateHolder = rememberSaveableStateHolder()
    val viewModelStoreOwner = LocalViewModelStoreOwner.current!!
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val savedStateRegistry = LocalSavedStateRegistryOwner.current.savedStateRegistry

    // applicationContext may be not Application in IDE preview. Handle it gracefully here.
    val application = LocalContext.current.applicationContext as? Application

    val state = rememberSaveable(
        saver = Saver(
            save = { it.saveState() },
            restore = { savedState ->
                NavHostStateImpl(
                    savedState = savedState,
                    initialBackstack = backstack,
                    scopeSpec = scopeSpec,
                    saveableStateHolder = saveableStateHolder,
                    hostViewModelStoreOwner = viewModelStoreOwner,
                    hostLifecycle = lifecycle,
                    hostSavedStateRegistry = savedStateRegistry,
                    application = application,
                    onHostEntryCreated = onHostEntryCreated
                )
            }
        )
    ) {
        NavHostStateImpl(
            initialBackstack = backstack,
            scopeSpec = scopeSpec,
            saveableStateHolder = saveableStateHolder,
            hostViewModelStoreOwner = viewModelStoreOwner,
            hostLifecycle = lifecycle,
            hostSavedStateRegistry = savedStateRegistry,
            application = application,
            onHostEntryCreated = onHostEntryCreated
        )
    }
    state.backstack = backstack

    DisposableEffect(Unit) {
        state.onCreate()
        onDispose {
            state.onDispose()
        }
    }

    return state
}

/**
 * Stores and manages saved state and all Android architecture components (Lifecycle,
 * ViewModelStore, SavedStateRegistry) for every entry.
 */
@Stable
sealed interface NavHostState<out T> {

    /**
     * List of all current [NavHostEntries][NavHostEntry] in the same order their associated
     * [entries][NavEntry] appear in the backstack.
     *
     * The last entry of this list is always the currently displayed entry.
     */
    @ExperimentalReimaginedApi
    val hostEntries: List<NavHostEntry<T>>

}

/**
 * Stores and manages saved state and all Android architecture components (Lifecycle,
 * ViewModelStore, SavedStateRegistry) for every entry and every scope.
 */
@Stable
sealed interface ScopingNavHostState<out T, S> : NavHostState<T> {

    /**
     * [ScopedNavHostEntries][ScopedNavHostEntry] for all scopes present in the backstack.
     *
     * Note that unlike [ScopingNavHostScope.scopedHostEntries], this property provides access to
     * ALL scopes that are associated with current destinations in the backstack, not just the last
     * (current) destination.
     */
    @ExperimentalReimaginedApi
    val scopedHostEntries: Map<S, ScopedNavHostEntry<S>>

}

@Stable
internal class NavHostStateImpl<T, S>(
    savedState: NavHostSavedState<S>? = null,
    initialBackstack: NavBackstack<T>,
    private val scopeSpec: NavScopeSpec<T, S>,
    private val saveableStateHolder: SaveableStateHolder,
    hostViewModelStoreOwner: ViewModelStoreOwner,
    private val hostLifecycle: Lifecycle,
    private val hostSavedStateRegistry: SavedStateRegistry,
    private val application: Application?,
    private val onHostEntryCreated: ((NavHostEntry<T>) -> Unit)?
) : ScopingNavHostState<T, S> {

    val hostId: NavHostId = savedState?.hostId ?: NavHostId()

    var backstack by mutableStateOf(initialBackstack)

    private val hostEntriesMap = mutableMapOf<NavId, NavHostEntry<T>>()

    private val scopedHostEntriesMap = mutableMapOf<S, ScopedNavHostEntry<S>>()

    private val outdatedHostEntriesQueue = ArrayDeque<OutdatedHostEntriesQueueItem<T, S>>()

    private val viewModelStoreProvider: ViewModelStoreProvider =
        ViewModelProvider(hostViewModelStoreOwner)[viewModelStoreProviderKey(hostId), NavHostViewModel::class.java]

    private var hostLifecycleState: Lifecycle.State = Lifecycle.State.INITIALIZED

    private val lifecycleEventObserver = LifecycleEventObserver { _, event ->
        hostLifecycleState = event.targetState
        getAllHostEntries().forEach {
            it.hostLifecycleState = hostLifecycleState
        }
    }

    init {
        if (savedState != null) {
            restoreState(savedState)
        }
    }

    private fun restoreState(savedState: NavHostSavedState<S>) {
        // remove components of the entries that are no longer present in the backstack
        val backstackEntryIds = backstack.entries.mapTo(hashSetOf()) { it.id }
        savedState.hostEntryIds.filter { it !in backstackEntryIds }.forEach {
            removeComponents(it)
        }
        // all other entries are restored
        val restoredEntryIds = savedState.hostEntryIds.toHashSet()
        backstack.entries.filter { it.id in restoredEntryIds }.forEach { entry ->
            getOrCreateNewHostEntry(entry)
        }

        val backstackEntryScopes = backstack.entries
            .flatMapTo(hashSetOf()) { scopeSpec.getScopes(it.destination) }
        val (scopedRecordsToRestore, scopedRecordsToRemove) =
            savedState.scopedHostEntryRecords.partition { it.scope in backstackEntryScopes }
        scopedRecordsToRemove.forEach {
            removeComponents(it.id)
        }
        scopedRecordsToRestore.forEach {
            getOrCreateNewScopedHostEntry(id = it.id, scope = it.scope)
        }

        savedState.outdatedHostEntryIds.forEach {
            removeComponents(it)
        }
    }

    // Cache the last-returned snapshot keyed by the backstack reference. Compose's
    // `derivedStateOf { createSnapshot() }` can re-invoke this function even when the
    // backstack itself hasn't changed (e.g., when an unrelated observable read inside
    // NavSnapshot construction — like a destination property — reports a change).
    // Returning a fresh NavSnapshot each time bloats Compose's state-record history,
    // retaining NavSnapshotItems and their NavHostEntries across many GC cycles.
    private var cachedSnapshotBackstack: NavBackstack<T>? = null
    private var cachedSnapshot: NavSnapshot<T, S>? = null

    fun createSnapshot(): NavSnapshot<T, S> {
        val currentBackstack = backstack
        cachedSnapshot?.let { existing ->
            if (cachedSnapshotBackstack === currentBackstack) return existing
        }
        val snapshot = NavSnapshot(
            items = currentBackstack.entries.map { entry ->
                NavSnapshotItem(
                    hostEntry = getOrCreateNewHostEntry(entry),
                    scopedHostEntries = scopeSpec.getScopes(entry.destination)
                        .associateWith { scope ->
                            getOrCreateNewScopedHostEntry(id = NavId(), scope = scope)
                        }
                )
            },
            action = currentBackstack.action
        )
        val backstackEntryIds = snapshot.items.mapTo(hashSetOf()) { it.hostEntry.id }
        val outdatedHostEntries = hostEntriesMap.keys
            .filter { it !in backstackEntryIds }
            .mapNotNull { hostEntriesMap.remove(it) }

        val backstackEntryScopes = snapshot.items
            .flatMapTo(hashSetOf()) { it.scopedHostEntries.keys }
        val outdatedScopedHostEntries = scopedHostEntriesMap.keys
            .filter { it !in backstackEntryScopes }
            .mapNotNull { scopedHostEntriesMap.remove(it) }

        // Only queue if there is actual cleanup to do — otherwise we accumulate
        // empty queue items that only serve to pin the NavSnapshot in memory.
        val allOutdated = outdatedHostEntries + outdatedScopedHostEntries
        if (allOutdated.isNotEmpty()) {
            outdatedHostEntriesQueue.addLast(
                OutdatedHostEntriesQueueItem(
                    snapshot = snapshot,
                    outdatedHostEntries = allOutdated
                )
            )
        }
        cachedSnapshotBackstack = currentBackstack
        cachedSnapshot = snapshot
        return snapshot
    }

    private fun getAllHostEntries() = listOf(
        hostEntriesMap.values,
        scopedHostEntriesMap.values,
        outdatedHostEntriesQueue.getAllHostEntries()
    ).flatten()

    private fun getOrCreateNewHostEntry(entry: NavEntry<T>) = hostEntriesMap.getOrPut(entry.id) {
        NavHostEntry(
            id = entry.id,
            destination = entry.destination,
            saveableStateHolder = saveableStateHolder,
            viewModelStore = viewModelStoreProvider.getViewModelStore(entry.id),
            application = application
        ).also {
            initComponents(it)
            onHostEntryCreated?.invoke(it)
        }
    }

    private fun getOrCreateNewScopedHostEntry(
        id: NavId,
        scope: S
    ) = scopedHostEntriesMap.getOrPut(scope) {
        ScopedNavHostEntry(
            id = id,
            scope = scope,
            viewModelStore = viewModelStoreProvider.getViewModelStore(id),
            application = application
        ).also {
            initComponents(it)
        }
    }

    private fun initComponents(baseEntry: BaseNavHostEntry) {
        val savedStateKey = savedStateKey(hostId, baseEntry.id)

        // state should be restored only in INITIALIZED state
        hostSavedStateRegistry.consumeRestoredStateForKey(savedStateKey).let { savedState ->
            baseEntry.restoreState(savedState ?: Bundle())
        }
        hostSavedStateRegistry.unregisterSavedStateProvider(savedStateKey)
        hostSavedStateRegistry.registerSavedStateProvider(
            savedStateKey,
            baseEntry.savedStateProvider
        )

        // apply actual states only after state restoration
        baseEntry.hostLifecycleState = hostLifecycleState
        baseEntry.maxLifecycleState = Lifecycle.State.CREATED
    }

    fun onCreate() {
        hostLifecycle.addObserver(lifecycleEventObserver)
    }

    fun onDispose() {
        hostLifecycle.removeObserver(lifecycleEventObserver)
        getAllHostEntries().forEach {
            it.hostLifecycleState = Lifecycle.State.DESTROYED
        }
    }

    fun onTransitionStart(visibleItems: Set<NavSnapshotItem<T, S>>) {
        val visibleHostEntries = visibleItems.getAllHostEntries()
        val allHostEntries = getAllHostEntries()
        allHostEntries.filter { it !in visibleHostEntries }.forEach {
            it.maxLifecycleState = minOf(it.maxLifecycleState, Lifecycle.State.STARTED)
        }
        // actual entries might have been removed by clear() method
        allHostEntries.filter { it in visibleHostEntries }.forEach {
            it.maxLifecycleState = Lifecycle.State.STARTED
        }
    }

    fun onTransitionFinish(visibleItems: Set<NavSnapshotItem<T, S>>) {
        val visibleHostEntries = visibleItems.getAllHostEntries()
        val allHostEntries = getAllHostEntries()
        allHostEntries.filter { it !in visibleHostEntries }.forEach {
            it.maxLifecycleState = Lifecycle.State.CREATED
        }
        allHostEntries.filter { it in visibleHostEntries }.forEach {
            it.maxLifecycleState = Lifecycle.State.RESUMED
        }
    }

    /**
     * Remove entries that are no longer in the snapshot.
     *
     * Cleanup strategy:
     *  1. If the passed snapshot is found in the queue (referential equality), drain up to and
     *     including it — matching upstream behaviour for in-flight transitions.
     *  2. Otherwise (the common cause of the NavHostEntry leak: `derivedStateOf { createSnapshot() }`
     *     produces spurious snapshots that are appended to the queue but never become a transition
     *     target), treat it as "transition is settled for visible items": drop every queued item
     *     whose outdated entries are not referenced by the current backstack. This reclaims entries
     *     that would otherwise accumulate forever.
     */
    fun removeOutdatedHostEntries(snapshot: NavSnapshot<T, S>) {
        if (outdatedHostEntriesQueue.any { it.snapshot == snapshot }) {
            do {
                val item = outdatedHostEntriesQueue.removeFirst()
                item.outdatedHostEntries.forEach { entry ->
                    entry.maxLifecycleState = Lifecycle.State.DESTROYED
                    removeComponents(entry.id)
                }
            } while (item.snapshot != snapshot)
            return
        }
        // Fallback: drain any queued item whose entries are all absent from the current backstack.
        // Keeps in-flight transition entries alive (they'll be in the current backstack or
        // scopes) while reclaiming truly orphaned entries.
        val backstackEntryIds = backstack.entries.mapTo(hashSetOf()) { it.id }
        val iterator = outdatedHostEntriesQueue.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            val allTrulyOutdated = item.outdatedHostEntries.all { it.id !in backstackEntryIds }
            if (allTrulyOutdated) {
                iterator.remove()
                item.outdatedHostEntries.forEach { entry ->
                    entry.maxLifecycleState = Lifecycle.State.DESTROYED
                    removeComponents(entry.id)
                }
            }
        }
    }

    /**
     * Unregister saved state provider and cleanup view models for the specified entry id.
     */
    private fun removeComponents(entryId: NavId) {
        hostSavedStateRegistry.unregisterSavedStateProvider(savedStateKey(hostId, entryId))
        viewModelStoreProvider.removeViewModelStore(entryId)
        saveableStateHolder.removeState(entryId)
    }

    fun saveState() = NavHostSavedState(
        hostId = hostId,
        hostEntryIds = hostEntriesMap.keys.toList(),
        scopedHostEntryRecords = scopedHostEntriesMap.values.map { it.toScopedHostEntryRecord() },
        outdatedHostEntryIds = outdatedHostEntriesQueue.getAllHostEntries().map { it.id }
    )

    @InternalReimaginedApi
    fun clear() {
        getAllHostEntries().forEach { entry ->
            entry.maxLifecycleState = Lifecycle.State.DESTROYED
            removeComponents(entry.id)
        }
        hostEntriesMap.clear()
        scopedHostEntriesMap.clear()
        outdatedHostEntriesQueue.clear()
    }

    @ExperimentalReimaginedApi
    override val hostEntries: List<NavHostEntry<T>> by derivedStateOf {
        backstack.entries.map { entry ->
            getOrCreateNewHostEntry(entry)
        }
    }

    @ExperimentalReimaginedApi
    override val scopedHostEntries: Map<S, ScopedNavHostEntry<S>> by derivedStateOf {
        backstack.entries.flatMapTo(hashSetOf()) { scopeSpec.getScopes(it.destination) }
            .associateWith { scope ->
                getOrCreateNewScopedHostEntry(id = NavId(), scope = scope)
            }
    }

}

private fun <T, S> ArrayDeque<OutdatedHostEntriesQueueItem<T, S>>.getAllHostEntries() =
    flatMap { it.outdatedHostEntries }

private fun <T, S> Set<NavSnapshotItem<T, S>>.getAllHostEntries() =
    (map { it.hostEntry } + flatMap { it.scopedHostEntries.values }).toSet()

internal data class OutdatedHostEntriesQueueItem<out T, S>(
    val snapshot: NavSnapshot<T, S>,
    val outdatedHostEntries: List<BaseNavHostEntry>
)

@Parcelize
internal data class NavHostSavedState<out S>(
    val hostId: NavHostId,
    val hostEntryIds: List<NavId>,
    val scopedHostEntryRecords: List<ScopedNavHostEntryRecord<S>>,
    val outdatedHostEntryIds: List<NavId>
) : Parcelable

private interface ViewModelStoreProvider {

    fun getViewModelStore(id: NavId): ViewModelStore
    fun removeViewModelStore(id: NavId)

}

internal class NavHostViewModel : ViewModel(), ViewModelStoreProvider {

    private val viewModelStores = mutableMapOf<NavId, ViewModelStore>()

    override fun getViewModelStore(id: NavId) = viewModelStores.getOrPut(id) {
        ViewModelStore()
    }

    override fun removeViewModelStore(id: NavId) {
        viewModelStores.remove(id)?.also { it.clear() }
    }

    override fun onCleared() {
        for (store in viewModelStores.values) {
            store.clear()
        }
        viewModelStores.clear()
    }

}

