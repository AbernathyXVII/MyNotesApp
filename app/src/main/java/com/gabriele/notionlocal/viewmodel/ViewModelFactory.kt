package com.gabriele.notionlocal.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository

/**
 * Factory manuale per i ViewModel. Niente Hilt/Koin di proposito: per
 * un progetto di questa dimensione una factory scritta a mano è più
 * semplice da leggere e capire, e in futuro si può sempre migrare a un
 * framework DI se il progetto cresce molto.
 */
class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    private val database by lazy { AppDatabase.getInstance(context) }

    /**
     * Visibile anche fuori perché qualche lavoro non appartiene a nessuna
     * schermata: all'avvio si svuota il cestino e si controlla che la
     * pagina da aprire esista ancora, prima che una schermata ci sia.
     */
    val pageRepository by lazy { PageRepository(database) }
    private val databaseRepository by lazy { DatabaseRepository(database) }
    val imageStore by lazy { PageImageStore(context) }

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when {
            modelClass.isAssignableFrom(PageEditorViewModel::class.java) ->
                PageEditorViewModel(pageRepository) as T

            modelClass.isAssignableFrom(DatabaseViewModel::class.java) ->
                DatabaseViewModel(databaseRepository, pageRepository) as T

            modelClass.isAssignableFrom(SearchViewModel::class.java) ->
                SearchViewModel(pageRepository) as T

            modelClass.isAssignableFrom(SidebarViewModel::class.java) ->
                SidebarViewModel(pageRepository) as T

            modelClass.isAssignableFrom(PagePickerViewModel::class.java) ->
                PagePickerViewModel(pageRepository) as T

            modelClass.isAssignableFrom(FavoritesViewModel::class.java) ->
                FavoritesViewModel(pageRepository) as T

            modelClass.isAssignableFrom(TrashViewModel::class.java) ->
                TrashViewModel(pageRepository, imageStore) as T

            modelClass.isAssignableFrom(StartupViewModel::class.java) ->
                StartupViewModel(pageRepository) as T

            else -> throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
        }
    }
}
