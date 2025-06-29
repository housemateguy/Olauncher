package app.olauncher.helper

import android.util.Log
import org.json.JSONObject
import org.json.JSONException

data class WidgetSize(
    val width: Int,
    val height: Int
)

data class WidgetPosition(
    val leftMargin: Int,
    val topMargin: Int
)

class WidgetSizeHelper {
    companion object {
        private const val TAG = "WidgetSizeHelper"
        
        /**
         * Save widget size to JSON string
         */
        fun saveWidgetSizes(widgetSizes: Map<Int, WidgetSize>): String {
            return try {
                val json = JSONObject()
                widgetSizes.forEach { (widgetId, size) ->
                    val sizeJson = JSONObject().apply {
                        put("width", size.width)
                        put("height", size.height)
                    }
                    json.put(widgetId.toString(), sizeJson)
                }
                json.toString()
            } catch (e: JSONException) {
                Log.e(TAG, "Error saving widget sizes: ${e.message}")
                "{}"
            }
        }
        
        /**
         * Load widget sizes from JSON string
         */
        fun loadWidgetSizes(jsonString: String): Map<Int, WidgetSize> {
            return try {
                val json = JSONObject(jsonString)
                val sizes = mutableMapOf<Int, WidgetSize>()
                
                val keys = json.keys()
                while (keys.hasNext()) {
                    val widgetId = keys.next()
                    val sizeJson = json.getJSONObject(widgetId)
                    val size = WidgetSize(
                        width = sizeJson.getInt("width"),
                        height = sizeJson.getInt("height")
                    )
                    sizes[widgetId.toInt()] = size
                }
                sizes
            } catch (e: JSONException) {
                Log.e(TAG, "Error loading widget sizes: ${e.message}")
                emptyMap()
            }
        }
        
        /**
         * Save widget positions to JSON string
         */
        fun saveWidgetPositions(widgetPositions: Map<Int, WidgetPosition>): String {
            return try {
                val json = JSONObject()
                widgetPositions.forEach { (widgetId, position) ->
                    val positionJson = JSONObject().apply {
                        put("leftMargin", position.leftMargin)
                        put("topMargin", position.topMargin)
                    }
                    json.put(widgetId.toString(), positionJson)
                }
                json.toString()
            } catch (e: JSONException) {
                Log.e(TAG, "Error saving widget positions: ${e.message}")
                "{}"
            }
        }
        
        /**
         * Load widget positions from JSON string
         */
        fun loadWidgetPositions(jsonString: String): Map<Int, WidgetPosition> {
            return try {
                val json = JSONObject(jsonString)
                val positions = mutableMapOf<Int, WidgetPosition>()
                
                val keys = json.keys()
                while (keys.hasNext()) {
                    val widgetId = keys.next()
                    val positionJson = json.getJSONObject(widgetId)
                    val position = WidgetPosition(
                        leftMargin = positionJson.getInt("leftMargin"),
                        topMargin = positionJson.getInt("topMargin")
                    )
                    positions[widgetId.toInt()] = position
                }
                positions
            } catch (e: JSONException) {
                Log.e(TAG, "Error loading widget positions: ${e.message}")
                emptyMap()
            }
        }
    }
} 