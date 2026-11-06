package com.maverick.tetris_mobile.benchmark

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.coroutines.EmptyCoroutineContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Headless JVM composition benchmark, deliberately excluding layout, drawing and device startup. */
@OptIn(ExperimentalComposeApi::class)
class ViewModelLookupBenchmark {
    private class ModelA : ViewModel()
    private class ModelB : ViewModel()
    private class ModelC : ViewModel()
    private class ModelD : ViewModel()
    private class ModelE : ViewModel()
    private object Marker : CreationExtras.Key<Any>

    private class Owner(override val viewModelStore: ViewModelStore = ViewModelStore()) :
        ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
        val marker = Any()
        var created = 0
        override val defaultViewModelCreationExtras = MutableCreationExtras().apply { this[Marker] = marker }
        override val defaultViewModelProviderFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                assertSame(marker, extras[Marker])
                assertTrue(requireNotNull(extras[ViewModelProvider.VIEW_MODEL_KEY]).endsWith(requireNotNull(modelClass.canonicalName)))
                created++
                val model = when (modelClass) {
                    ModelA::class.java -> ModelA()
                    ModelB::class.java -> ModelB()
                    ModelC::class.java -> ModelC()
                    ModelD::class.java -> ModelD()
                    ModelE::class.java -> ModelE()
                    else -> error("Unexpected model: $modelClass")
                }
                return modelClass.cast(model)!!
            }
        }
    }

    private class EmptyApplier : AbstractApplier<Unit>(Unit) {
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun onClear() = Unit
    }

    private class Models(owner: Owner) {
        private val provider = ViewModelProvider(owner)
        val a = provider[ModelA::class.java]
        val b = provider[ModelB::class.java]
        val c = provider[ModelC::class.java]
        val d = provider[ModelD::class.java]
        val e = provider[ModelE::class.java]
    }

    @Composable
    private fun Baseline(consume: (ViewModel, ViewModel, ViewModel, ViewModel, ViewModel) -> Unit) {
        consume(viewModel<ModelA>(), viewModel<ModelB>(), viewModel<ModelC>(), viewModel<ModelD>(), viewModel<ModelE>())
    }

    @Composable
    private fun Candidate(provider: ViewModelProvider, consume: (ViewModel, ViewModel, ViewModel, ViewModel, ViewModel) -> Unit) {
        val a = remember { provider[ModelA::class.java] }
        val b = remember { provider[ModelB::class.java] }
        val c = remember { provider[ModelC::class.java] }
        val d = remember { provider[ModelD::class.java] }
        val e = remember { provider[ModelE::class.java] }
        consume(a, b, c, d, e)
    }

    @Test fun firstCompositionParityAndTiming() {
        println("VIEWMODEL_JVM version=${System.getProperty("java.version")} home=${System.getProperty("java.home")} vm=${System.getProperty("java.vm.name")}")
        fun sample(useBaseline: Boolean, retained: Boolean, iterations: Int): Double {
            var elapsed = 0L
            repeat(iterations) {
                val original = Owner()
                val expected = if (retained) Models(original) else null
                val owner = if (retained) Owner(original.viewModelStore) else original
                val recomposer = Recomposer(EmptyCoroutineContext)
                val composition = ControlledComposition(EmptyApplier(), recomposer)
                var actual = emptyList<ViewModel>()
                val consume: (ViewModel, ViewModel, ViewModel, ViewModel, ViewModel) -> Unit = { a, b, c, d, e ->
                    actual = listOf(a, b, c, d, e)
                }
                try {
                    // Include the candidate's one provider construction, as in onCreate.
                    val start = System.nanoTime()
                    val provider = if (useBaseline) null else ViewModelProvider(owner)
                    composition.setContent {
                        CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                            if (useBaseline) Baseline(consume) else Candidate(requireNotNull(provider), consume)
                        }
                    }
                    elapsed += System.nanoTime() - start
                    val reference = expected ?: Models(owner)
                    val expectedList = listOf(reference.a, reference.b, reference.c, reference.d, reference.e)
                    expectedList.zip(actual).forEach { (a, b) -> assertSame(a, b) }
                    assertEquals(5, actual.size)
                    assertEquals(if (retained) 0 else 5, owner.created)
                } finally {
                    composition.dispose()
                    recomposer.cancel()
                    owner.viewModelStore.clear()
                }
            }
            return elapsed.toDouble() / iterations
        }
        for (retained in listOf(false, true)) {
            repeat(6) { sample(it % 2 == 0, retained, 5_000) }
            repeat(12) { pair ->
                val order = if (pair % 2 == 0) listOf(true, false) else listOf(false, true)
                for (variant in order) {
                    val nanos = sample(variant, retained, 1_000)
                    println("VIEWMODEL_BENCH workload=${if (retained) "recreation" else "first"} variant=${if (variant) "baseline" else "candidate"} pair=$pair ns=$nanos")
                }
            }
        }
    }

    @Test fun lookupParityAndTiming() {
        val owner = Owner()
        val expected = Models(owner)
        val provider = ViewModelProvider(owner)
        val state = mutableIntStateOf(0)
        var observed = -1
        var renders = 0
        var baseline = true
        val consume: (ViewModel, ViewModel, ViewModel, ViewModel, ViewModel) -> Unit = { a, b, c, d, e ->
            assertSame(expected.a, a); assertSame(expected.b, b); assertSame(expected.c, c)
            assertSame(expected.d, d); assertSame(expected.e, e)
            observed = state.intValue
            renders++
        }
        val recomposer = Recomposer(EmptyCoroutineContext)
        val composition = ControlledComposition(EmptyApplier(), recomposer)
        try {
            composition.setContent {
                CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                    // Read here so every update re-executes the lookup/capture path.
                    state.intValue
                    if (baseline) Baseline(consume)
                    else Candidate(provider, consume)
                }
            }
            fun recompose() {
                // Match Recomposer's snapshot observers so subsequent reads stay observable.
                val snapshot = Snapshot.takeMutableSnapshot(
                    readObserver = { composition.recordReadOf(it) },
                    writeObserver = { composition.recordWriteOf(it) }
                )
                val changed = try {
                    snapshot.enter { composition.recompose() }.also { snapshot.apply().check() }
                } finally {
                    snapshot.dispose()
                }
                if (changed) composition.applyChanges()
            }
            fun sample(useBaseline: Boolean, iterations: Int): Double {
                baseline = useBaseline
                // Reinstall to switch branches before measuring.
                composition.invalidateAll()
                recompose()
                val before = renders
                val start = System.nanoTime()
                repeat(iterations) {
                    state.intValue++
                    composition.recordModificationsOf(setOf(state))
                    recompose()
                }
                val elapsed = System.nanoTime() - start
                assertEquals(iterations, renders - before)
                assertEquals(state.intValue, observed)
                assertEquals(5, owner.created)
                return elapsed.toDouble() / iterations
            }
            repeat(6) { sample(it % 2 == 0, 5_000) }
            repeat(12) { pair ->
                val order = if (pair % 2 == 0) listOf(true, false) else listOf(false, true)
                for (variant in order) {
                    val nanos = sample(variant, 20_000)
                    println("VIEWMODEL_BENCH workload=recomposition variant=${if (variant) "baseline" else "candidate"} pair=$pair ns=$nanos")
                }
            }
            // The default key and retained store also agree across a new owner/provider.
            val recreated = Owner(owner.viewModelStore)
            val retained = Models(recreated)
            assertSame(expected.a, retained.a); assertSame(expected.b, retained.b)
            assertSame(expected.c, retained.c); assertSame(expected.d, retained.d); assertSame(expected.e, retained.e)
            assertEquals(0, recreated.created)
        } finally {
            composition.dispose()
            recomposer.cancel()
            owner.viewModelStore.clear()
        }
    }
}
