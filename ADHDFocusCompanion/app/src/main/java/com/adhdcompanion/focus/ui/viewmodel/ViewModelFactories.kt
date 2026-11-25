package com.adhdcompanion.focus.ui.viewmodel

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.adhdcompanion.focus.di.AppContainer

/**
 * One factory per screen ViewModel, built from the shared AppContainer.
 * Avoids any DI framework while still giving each screen a properly scoped,
 * configuration-change-surviving ViewModel.
 */
object ViewModelFactories {

    fun now(container: AppContainer) = viewModelFactory {
        initializer { NowViewModel(container) }
    }

    fun stuck(container: AppContainer) = viewModelFactory {
        initializer { StuckViewModel(container) }
    }

    fun planner(container: AppContainer) = viewModelFactory {
        initializer { PlannerViewModel(container) }
    }

    fun brainDump(container: AppContainer) = viewModelFactory {
        initializer { BrainDumpViewModel(container) }
    }
}
