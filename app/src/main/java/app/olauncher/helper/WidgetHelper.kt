package app.olauncher.helper

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout

class WidgetHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "WidgetHelper"
        private const val WIDGET_HOST_ID = 1024
    }
    
    private val appWidgetManager = AppWidgetManager.getInstance(context)
    private val appWidgetHost = AppWidgetHost(context, WIDGET_HOST_ID)
    
    fun startListening() {
        appWidgetHost.startListening()
    }
    
    fun stopListening() {
        appWidgetHost.stopListening()
    }
    
    fun allocateAppWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }
    
    fun deleteAppWidgetId(appWidgetId: Int) {
        appWidgetHost.deleteAppWidgetId(appWidgetId)
    }
    
    fun createWidgetView(appWidgetId: Int, parent: ViewGroup? = null): AppWidgetHostView? {
        return try {
            val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
            val hostView = appWidgetHost.createView(context, appWidgetId, appWidgetInfo)
            
            // Set layout parameters
            val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            hostView.layoutParams = layoutParams
            
            // Only add to parent if explicitly requested
            parent?.addView(hostView)
            hostView
        } catch (e: Exception) {
            Log.e(TAG, "Error creating widget view: ${e.message}")
            null
        }
    }
    
    fun getWidgetInfo(appWidgetId: Int): AppWidgetProviderInfo? {
        return try {
            appWidgetManager.getAppWidgetInfo(appWidgetId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting widget info: ${e.message}")
            null
        }
    }
    
    fun isWidgetInstalled(appWidgetId: Int): Boolean {
        return try {
            appWidgetManager.getAppWidgetInfo(appWidgetId) != null
        } catch (e: Exception) {
            false
        }
    }
    
    fun getAvailableWidgets(): List<AppWidgetProviderInfo> {
        return try {
            appWidgetManager.installedProviders
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available widgets: ${e.message}")
            emptyList()
        }
    }
    
    fun areWidgetsAvailable(): Boolean {
        return try {
            val availableWidgets = getAvailableWidgets()
            availableWidgets.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking widget availability: ${e.message}")
            false
        }
    }
    
    fun createWidgetPickerIntent(): Intent {
        val appWidgetId = allocateAppWidgetId()
        return Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            // Add additional flags to ensure the picker opens
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }
    
    fun createWidgetConfigureIntent(appWidgetId: Int): Intent? {
        val appWidgetInfo = getWidgetInfo(appWidgetId) ?: return null
        
        return if (appWidgetInfo.configure != null) {
            Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = appWidgetInfo.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
        } else {
            null
        }
    }
} 