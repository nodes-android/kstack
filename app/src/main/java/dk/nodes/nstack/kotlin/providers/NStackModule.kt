package dk.nodes.nstack.kotlin.providers

import android.content.Context
import dk.nodes.nstack.kotlin.managers.AppOpenSettingsManager
import dk.nodes.nstack.kotlin.managers.PrefManager
import dk.nodes.nstack.kotlin.util.Preferences
import dk.nodes.nstack.kotlin.util.PreferencesImpl
import kotlin.reflect.KClass

/**
 * Provides dependencies for NStack
 */
class NStackModule(private val context: Context) {

    /**
     * Creates new AppOpenSettingsManager
     */
    fun provideAppOpenSettingsManager(): AppOpenSettingsManager {
        return getLazyDependency(AppOpenSettingsManager::class) {
            AppOpenSettingsManager(
                context,
                providePreferences()
            )
        }
    }

    /**
     * Creates new PrefManager
     */
    fun providePrefManager(): PrefManager {
        return getLazyDependency(PrefManager::class) { PrefManager(providePreferences()) }
    }

    private fun providePreferences(): Preferences {
        return getLazyDependency(PreferencesImpl::class) { PreferencesImpl(context) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> getLazyDependency(klass: KClass<T>, block: () -> T): T {
        if (!dependenciesMap.containsKey(klass)) {
            dependenciesMap[klass] = block()
        }
        return dependenciesMap[klass] as T
    }

    private val dependenciesMap = mutableMapOf<KClass<*>, Any>()
}