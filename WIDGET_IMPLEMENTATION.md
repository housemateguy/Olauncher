# Enhanced Widget Implementation for Olauncher

This document explains the advanced widget system implemented in this Olauncher fork, featuring resizable, movable widgets with persistent sizing and positioning.

## Overview

This enhanced widget system transforms Olauncher from a basic widget host into a powerful, flexible widget management platform. Users can now resize, move, and customize widgets with persistent settings that survive app restarts and device reboots.

## 🆕 Enhanced Features

### **Resizable & Movable Widgets**
- **Drag to Resize**: Grab the bottom-right corner to resize widgets
- **Drag to Move**: Move widgets anywhere within the widget area
- **Visual Feedback**: Color-coded borders show current operation (gray=resize, blue=move, red=delete)
- **Touch-Friendly**: Large resize handles (60px) for easy interaction

### **Persistent Widget Sizes & Positions**
- **Automatic Saving**: Widget dimensions and positions saved automatically
- **JSON Storage**: Efficient JSON-based storage system
- **Cross-Session Persistence**: Settings survive app restarts and device reboots
- **Smart Cleanup**: Removed widgets don't leave orphaned data

### **Advanced Widget Management**
- **Individual Widget Deletion**: Long-press specific widgets to delete them
- **Widget List Management**: View and manage all widgets from settings
- **Smart Layout Adaptation**: Widget area adjusts based on position setting
- **Visual Indicators**: Clear feedback for all widget operations

### **Enhanced UI/UX**
- **Widget Hint System**: Shows helpful hints when no widgets are present
- **Smart Visibility**: Hints automatically hide when widgets are added
- **Improved Spacing**: Better layout management and spacing
- **Position Control**: Choose widget area position (top, middle, bottom)

## Implementation Details

### **Core Components**

#### 1. **ResizableMovableLayout.kt** (New)
```kotlin
class ResizableMovableLayout : FrameLayout {
    // Handles touch events for resize and move operations
    // Visual feedback with colored borders
    // Automatic size/position saving
}
```
**Features:**
- Custom touch handling for resize and move operations
- Visual indicators for resize handles and delete options
- Automatic saving of widget dimensions and positions
- Loads saved sizes/positions on initialization

#### 2. **WidgetSizeHelper.kt** (New)
```kotlin
class WidgetSizeHelper {
    // JSON-based storage for widget sizes and positions
    // Efficient serialization/deserialization
    // Error handling and validation
}
```
**Features:**
- JSON serialization for widget size and position data
- Data classes for `WidgetSize` and `WidgetPosition`
- Error handling for corrupted data
- Efficient storage and retrieval

#### 3. **Enhanced WidgetHelper.kt**
```kotlin
class WidgetHelper {
    // Widget creation and management
    // Widget picker integration
    // Widget lifecycle management
}
```
**Features:**
- Widget creation and hosting
- Widget picker launcher
- Widget information retrieval
- Error handling and validation

#### 4. **Enhanced HomeFragment.kt**
```kotlin
class HomeFragment {
    // Widget area management
    // Widget lifecycle handling
    // Layout adaptation
}
```
**Features:**
- Dynamic widget area positioning
- Widget size and position restoration
- Smart layout adjustments
- Individual widget deletion

### **Files Modified/Created**

1. **New Files:**
   - `ResizableMovableLayout.kt`: Custom layout for resizable/movable widgets
   - `WidgetSizeHelper.kt`: JSON-based widget data storage
   - `dialog_password.xml`: Password dialog layout
   - `dialog_password_verification.xml`: Password verification dialog
   - `dialog_password_change.xml`: Password change dialog

2. **Enhanced Files:**
   - `HomeFragment.kt`: Advanced widget management and lifecycle
   - `SettingsFragment.kt`: Enhanced widget settings and password protection
   - `Prefs.kt`: Widget size/position storage preferences
   - `fragment_home.xml`: Enhanced widget area layout
   - `fragment_settings.xml`: Widget and password protection settings
   - `strings.xml`: New string resources

### **Data Storage System**

#### **Widget Sizes Storage**
```kotlin
// JSON format for widget sizes
{
  "widgetId1": {"width": 320, "height": 520},
  "widgetId2": {"width": 280, "height": 440}
}
```

#### **Widget Positions Storage**
```kotlin
// JSON format for widget positions
{
  "widgetId1": {"leftMargin": 10, "topMargin": 10},
  "widgetId2": {"leftMargin": 20, "topMargin": 10}
}
```

### **Widget Lifecycle**

1. **Initialization**: 
   - WidgetHelper starts listening for updates
   - Saved widget sizes and positions are loaded
   - Widget area is positioned based on settings

2. **Widget Addition**:
   - User selects widget from picker
   - Widget is wrapped in ResizableMovableLayout
   - Widget ID is set for size/position tracking
   - Widget is added to container with saved or default size

3. **Widget Interaction**:
   - Touch events handled by ResizableMovableLayout
   - Visual feedback provided during operations
   - Size and position saved automatically on completion

4. **Widget Removal**:
   - Individual widgets can be deleted via long-press
   - Size and position data cleaned up automatically
   - Widget area visibility updated

5. **Cleanup**:
   - Widget host stops listening
   - All data properly saved
   - Memory cleaned up

## Usage Instructions

### **Adding Widgets**
1. Long-press home screen or go to Settings → Widgets
2. Tap "Add widget"
3. Select widget from system picker
4. Configure widget if required
5. Widget appears with resize handles and visual indicators

### **Resizing Widgets**
1. **Visual Indicator**: Gray border appears around widget
2. **Resize Handle**: Bottom-right corner shows diagonal lines
3. **Drag to Resize**: Touch and drag the bottom-right corner
4. **Size Limits**: Minimum 150x100px, maximum based on screen size
5. **Auto-Save**: Size automatically saved when you release

### **Moving Widgets**
1. **Visual Indicator**: Blue border appears during move
2. **Drag to Move**: Touch and drag anywhere except resize handle
3. **Boundary Limits**: Widget stays within widget area bounds
4. **Auto-Save**: Position automatically saved when you release

### **Deleting Widgets**
1. **Individual Deletion**: Long-press specific widget
2. **Confirmation Dialog**: Shows widget name for confirmation
3. **Bulk Deletion**: Settings → Widgets → Remove all widgets
4. **Auto-Cleanup**: Size/position data automatically removed

### **Managing Widget Settings**
1. **Enable/Disable**: Settings → Widgets → Enable widgets
2. **Position Control**: Settings → Widgets → Position (top/middle/bottom)
3. **Widget List**: Settings → Widgets → Manage widgets
4. **Visual Help**: Settings → Widgets → Long press to delete widget

## Technical Implementation

### **Touch Event Handling**
```kotlin
override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.action) {
        MotionEvent.ACTION_DOWN -> {
            // Determine if resizing or moving
            resizing = isInResizeHandle(event.x, event.y)
            moving = !resizing
        }
        MotionEvent.ACTION_MOVE -> {
            if (resizing) {
                // Handle resize operation
                updateWidgetSize(dx, dy)
            } else if (moving) {
                // Handle move operation
                updateWidgetPosition(dx, dy)
            }
        }
        MotionEvent.ACTION_UP -> {
            // Save final size and position
            saveSizeAndPosition()
        }
    }
}
```

### **Size and Position Persistence**
```kotlin
private fun saveSizeAndPosition() {
    val params = layoutParams as ViewGroup.MarginLayoutParams
    
    // Save size
    val currentSizes = WidgetSizeHelper.loadWidgetSizes(prefs.widgetSizes)
    currentSizes[widgetId] = WidgetSize(params.width, params.height)
    prefs.widgetSizes = WidgetSizeHelper.saveWidgetSizes(currentSizes)
    
    // Save position
    val currentPositions = WidgetSizeHelper.loadWidgetPositions(prefs.widgetPositions)
    currentPositions[widgetId] = WidgetPosition(params.leftMargin, params.topMargin)
    prefs.widgetPositions = WidgetSizeHelper.saveWidgetPositions(currentPositions)
}
```

### **Visual Feedback System**
```kotlin
override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    
    // Resize handle indicator (gray)
    val handleRect = RectF(width - resizeHandleSize, height - resizeHandleSize, width, height)
    canvas.drawRect(handleRect, resizePaint)
    
    // Delete indicator (red X)
    canvas.drawLine(deleteX, deleteY, deleteX + deleteSize, deleteY + deleteSize, deletePaint)
    
    // Move indicator (blue border)
    if (moving) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), movePaint)
    }
}
```

## Performance Considerations

### **Memory Management**
- Widget size/position data stored efficiently as JSON
- Automatic cleanup of orphaned data
- Proper lifecycle management to prevent memory leaks

### **Touch Performance**
- Large touch targets (60px resize handle)
- Efficient touch event handling
- Minimal redraw operations

### **Storage Efficiency**
- JSON compression for widget data
- Only essential data stored (width, height, margins)
- Automatic cleanup on widget removal

## Error Handling

### **Widget Creation Failures**
- Graceful fallback to FrameLayout if ResizableMovableLayout fails
- Error logging for debugging
- User-friendly error messages

### **Data Corruption**
- JSON parsing error handling
- Fallback to default sizes/positions
- Automatic data validation

### **Touch Event Errors**
- Exception handling in touch event processing
- Graceful degradation of functionality
- Error logging for debugging

## Testing

### **Manual Testing Checklist**
1. **Widget Addition**: Add various widget types
2. **Resize Operations**: Test resize handles and size limits
3. **Move Operations**: Test movement within boundaries
4. **Delete Operations**: Test individual and bulk deletion
5. **Persistence**: Restart app and verify saved settings
6. **Position Changes**: Test different widget area positions
7. **Error Scenarios**: Test with corrupted data, missing widgets

### **Automated Testing**
```kotlin
@Test
fun testWidgetSizePersistence() {
    // Test widget size saving and loading
}

@Test
fun testWidgetPositionPersistence() {
    // Test widget position saving and loading
}

@Test
fun testWidgetCleanup() {
    // Test cleanup of removed widget data
}
```

## Future Enhancements

### **Planned Features**
- **Widget Templates**: Save and reuse widget configurations
- **Widget Backup/Restore**: Export/import widget settings
- **Advanced Layouts**: Grid-based widget arrangements
- **Widget Themes**: Customizable widget appearance
- **Gesture Controls**: Pinch-to-resize, double-tap actions

### **Performance Improvements**
- **Lazy Loading**: Load widget data on demand
- **Caching**: Cache frequently accessed widget data
- **Optimization**: Reduce JSON parsing overhead

## Troubleshooting

### **Common Issues**

1. **Widgets not resizing**:
   - Check if ResizableMovableLayout is being used
   - Verify touch event handling is working
   - Check for conflicting touch listeners

2. **Sizes not persisting**:
   - Verify JSON storage is working
   - Check widget ID assignment
   - Validate save/load operations

3. **Visual indicators not showing**:
   - Check onDraw method implementation
   - Verify paint configurations
   - Ensure proper canvas operations

### **Debug Information**
```kotlin
Log.d("WidgetDebug", "Widget ID: $widgetId, Size: ${params.width}x${params.height}")
Log.d("WidgetDebug", "Position: ${params.leftMargin}, ${params.topMargin}")
Log.d("WidgetDebug", "Saved sizes: ${prefs.widgetSizes}")
```

This enhanced widget system provides a professional, user-friendly widget management experience while maintaining Olauncher's minimal design philosophy. 