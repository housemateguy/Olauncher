package app.olauncher.ui

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import app.olauncher.MainViewModel
import app.olauncher.R
import app.olauncher.data.AppModel
import app.olauncher.data.Constants
import app.olauncher.data.Prefs
import app.olauncher.databinding.FragmentHomeBinding
import app.olauncher.helper.appUsagePermissionGranted
import app.olauncher.helper.dpToPx
import app.olauncher.helper.expandNotificationDrawer
import app.olauncher.helper.getChangedAppTheme
import app.olauncher.helper.getUserHandleFromString
import app.olauncher.helper.isPackageInstalled
import app.olauncher.helper.openAlarmApp
import app.olauncher.helper.openCalendar
import app.olauncher.helper.openCameraApp
import app.olauncher.helper.openDialerApp
import app.olauncher.helper.openSearch
import app.olauncher.helper.setPlainWallpaperByTheme
import app.olauncher.helper.showToast
import app.olauncher.listener.OnSwipeTouchListener
import app.olauncher.listener.ViewSwipeTouchListener
import app.olauncher.helper.WidgetHelper
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.LinearLayout
import android.app.AlertDialog
import app.olauncher.ui.ResizableMovableLayout
import android.util.Log
import app.olauncher.helper.WidgetSizeHelper

class HomeFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var widgetHelper: WidgetHelper

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Widget management
    private var widgetPickerLauncher: ActivityResultLauncher<Intent>? = null
    private var widgetConfigureLauncher: ActivityResultLauncher<Intent>? = null
    private val widgetViews = mutableMapOf<Int, AppWidgetHostView>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        deviceManager = context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        widgetHelper = WidgetHelper(requireContext())

        initWidgetLaunchers()
        initObservers()
        setHomeAlignment(prefs.homeAlignment)
        initSwipeTouchListener()
        initClickListeners()

        // Add long-press to widgetArea to remove widgets
        binding.widgetArea?.setOnLongClickListener {
            showRemoveWidgetDialog()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        populateHomeScreen(false)
        loadExistingWidgets()
        widgetHelper.startListening()
        viewModel.isOlauncherDefault()
        if (prefs.showStatusBar) showStatusBar()
        else hideStatusBar()
    }

    override fun onPause() {
        super.onPause()
        widgetHelper.stopListening()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.lock -> {}
            R.id.clock -> openClockApp()
            R.id.date -> openCalendarApp()
            R.id.setDefaultLauncher -> viewModel.resetLauncherLiveData.call()
            R.id.tvScreenTime -> openScreenTimeDigitalWellbeing()

            else -> {
                try { // Launch app
                    val appLocation = view.tag.toString().toInt()
                    homeAppClicked(appLocation)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun openClockApp() {
        if (prefs.clockAppPackage.isBlank())
            openAlarmApp(requireContext())
        else
            launchApp(
                "Clock",
                prefs.clockAppPackage,
                prefs.clockAppClassName,
                prefs.clockAppUser
            )
    }

    private fun openCalendarApp() {
        if (prefs.calendarAppPackage.isBlank())
            openCalendar(requireContext())
        else
            launchApp(
                "Calendar",
                prefs.calendarAppPackage,
                prefs.calendarAppClassName,
                prefs.calendarAppUser
            )
    }

    override fun onLongClick(view: View): Boolean {
        when (view.id) {
            R.id.homeApp1 -> showAppList(Constants.FLAG_SET_HOME_APP_1, prefs.appName1.isNotEmpty(), true)
            R.id.homeApp2 -> showAppList(Constants.FLAG_SET_HOME_APP_2, prefs.appName2.isNotEmpty(), true)
            R.id.homeApp3 -> showAppList(Constants.FLAG_SET_HOME_APP_3, prefs.appName3.isNotEmpty(), true)
            R.id.homeApp4 -> showAppList(Constants.FLAG_SET_HOME_APP_4, prefs.appName4.isNotEmpty(), true)
            R.id.homeApp5 -> showAppList(Constants.FLAG_SET_HOME_APP_5, prefs.appName5.isNotEmpty(), true)
            R.id.homeApp6 -> showAppList(Constants.FLAG_SET_HOME_APP_6, prefs.appName6.isNotEmpty(), true)
            R.id.homeApp7 -> showAppList(Constants.FLAG_SET_HOME_APP_7, prefs.appName7.isNotEmpty(), true)
            R.id.homeApp8 -> showAppList(Constants.FLAG_SET_HOME_APP_8, prefs.appName8.isNotEmpty(), true)
            R.id.clock -> {
                showAppList(Constants.FLAG_SET_CLOCK_APP)
                prefs.clockAppPackage = ""
                prefs.clockAppClassName = ""
                prefs.clockAppUser = ""
            }
            R.id.date -> {
                showAppList(Constants.FLAG_SET_CALENDAR_APP)
                prefs.calendarAppPackage = ""
                prefs.calendarAppClassName = ""
                prefs.calendarAppUser = ""
            }
            R.id.setDefaultLauncher -> {
                prefs.hideSetDefaultLauncher = true
                binding.setDefaultLauncher.visibility = View.GONE
                if (viewModel.isOlauncherDefault.value != true) {
                    requireContext().showToast(R.string.set_as_default_launcher)
                    findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
                }
            }
            R.id.mainLayout -> {
                try {
                    findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
                    viewModel.firstOpen(false)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return true
    }

    private fun initObservers() {
        if (prefs.firstSettingsOpen) {
            binding.firstRunTips.visibility = View.VISIBLE
            binding.setDefaultLauncher.visibility = View.GONE
        } else binding.firstRunTips.visibility = View.GONE

        viewModel.refreshHome.observe(viewLifecycleOwner) {
            populateHomeScreen(it)
        }
        viewModel.isOlauncherDefault.observe(viewLifecycleOwner, Observer {
            if (it != true) {
                if (prefs.dailyWallpaper) {
                    prefs.dailyWallpaper = false
                    viewModel.cancelWallpaperWorker()
                }
                prefs.homeBottomAlignment = false
                setHomeAlignment()
            }
            if (binding.firstRunTips.visibility == View.VISIBLE) return@Observer
            binding.setDefaultLauncher.isVisible = it.not() && prefs.hideSetDefaultLauncher.not()
//            if (it) binding.setDefaultLauncher.visibility = View.GONE
//            else binding.setDefaultLauncher.visibility = View.VISIBLE
        })
        viewModel.homeAppAlignment.observe(viewLifecycleOwner) {
            setHomeAlignment(it)
        }
        viewModel.toggleDateTime.observe(viewLifecycleOwner) {
            populateDateTime()
        }
        viewModel.screenTimeValue.observe(viewLifecycleOwner) {
            it?.let { binding.tvScreenTime.text = it }
        }
    }

    private fun initSwipeTouchListener() {
        val context = requireContext()
        // Remove the swipe touch listener from mainLayout to avoid conflicts with long-press
        // binding.mainLayout.setOnTouchListener(getSwipeGestureListener(context))
        binding.homeApp1.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp1))
        binding.homeApp2.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp2))
        binding.homeApp3.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp3))
        binding.homeApp4.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp4))
        binding.homeApp5.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp5))
        binding.homeApp6.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp6))
        binding.homeApp7.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp7))
        binding.homeApp8.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp8))
    }

    private fun initClickListeners() {
        binding.lock.setOnClickListener(this)
        binding.clock.setOnClickListener(this)
        binding.date.setOnClickListener(this)
        binding.clock.setOnLongClickListener(this)
        binding.date.setOnLongClickListener(this)
        binding.setDefaultLauncher.setOnClickListener(this)
        binding.setDefaultLauncher.setOnLongClickListener(this)
        binding.tvScreenTime.setOnClickListener(this)
        binding.widgetHint?.setOnClickListener {
            addWidget()
        }
        // Restore long-press listener to main layout for opening settings
        binding.mainLayout.setOnLongClickListener(this)
    }

    private fun setHomeAlignment(horizontalGravity: Int = prefs.homeAlignment) {
        val verticalGravity = if (prefs.homeBottomAlignment) Gravity.BOTTOM else Gravity.CENTER_VERTICAL
        binding.homeAppsLayout.gravity = horizontalGravity or verticalGravity
        binding.dateTimeLayout.gravity = horizontalGravity
        binding.homeApp1.gravity = horizontalGravity
        binding.homeApp2.gravity = horizontalGravity
        binding.homeApp3.gravity = horizontalGravity
        binding.homeApp4.gravity = horizontalGravity
        binding.homeApp5.gravity = horizontalGravity
        binding.homeApp6.gravity = horizontalGravity
        binding.homeApp7.gravity = horizontalGravity
        binding.homeApp8.gravity = horizontalGravity
    }

    private fun populateDateTime() {
        binding.dateTimeLayout.isVisible = prefs.dateTimeVisibility != Constants.DateTime.OFF
        binding.clock.isVisible = Constants.DateTime.isTimeVisible(prefs.dateTimeVisibility)
        binding.date.isVisible = Constants.DateTime.isDateVisible(prefs.dateTimeVisibility)

//        var dateText = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date())
        val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
        var dateText = dateFormat.format(Date())

        if (!prefs.showStatusBar) {
            val battery = (requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
                .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (battery > 0)
                dateText = getString(R.string.day_battery, dateText, battery)
        }
        binding.date.text = dateText.replace(".,", ",")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun populateScreenTime() {
        if (requireContext().appUsagePermissionGranted().not()) return

        viewModel.getTodaysScreenTime()
        binding.tvScreenTime.visibility = View.VISIBLE

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val horizontalMargin = if (isLandscape) 64.dpToPx() else 10.dpToPx()
        val marginTop = if (isLandscape) {
            if (prefs.dateTimeVisibility == Constants.DateTime.DATE_ONLY) 36.dpToPx() else 56.dpToPx()
        } else {
            if (prefs.dateTimeVisibility == Constants.DateTime.DATE_ONLY) 45.dpToPx() else 72.dpToPx()
        }
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = marginTop
            marginStart = horizontalMargin
            marginEnd = horizontalMargin
            gravity = if (prefs.homeAlignment == Gravity.END) Gravity.START else Gravity.END
        }
        binding.tvScreenTime.layoutParams = params
        binding.tvScreenTime.setPadding(10.dpToPx())
    }

    private fun populateHomeScreen(appCountUpdated: Boolean) {
        if (appCountUpdated) hideHomeApps()
        populateDateTime()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            populateScreenTime()

        // Show widget hint if widgets are enabled
        if (prefs.widgetsEnabled) {
            binding.widgetHint?.visibility = View.VISIBLE
            // Make hint more prominent
            binding.widgetHint?.alpha = 0.8f
        } else {
            binding.widgetHint?.visibility = View.GONE
        }

        val homeAppsNum = prefs.homeAppsNum
        if (homeAppsNum == 0) return

        binding.homeApp1.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp1, prefs.appName1, prefs.appPackage1, prefs.appUser1)) {
            prefs.appName1 = ""
            prefs.appPackage1 = ""
        }
        if (homeAppsNum == 1) return

        binding.homeApp2.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp2, prefs.appName2, prefs.appPackage2, prefs.appUser2)) {
            prefs.appName2 = ""
            prefs.appPackage2 = ""
        }
        if (homeAppsNum == 2) return

        binding.homeApp3.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp3, prefs.appName3, prefs.appPackage3, prefs.appUser3)) {
            prefs.appName3 = ""
            prefs.appPackage3 = ""
        }
        if (homeAppsNum == 3) return

        binding.homeApp4.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp4, prefs.appName4, prefs.appPackage4, prefs.appUser4)) {
            prefs.appName4 = ""
            prefs.appPackage4 = ""
        }
        if (homeAppsNum == 4) return

        binding.homeApp5.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp5, prefs.appName5, prefs.appPackage5, prefs.appUser5)) {
            prefs.appName5 = ""
            prefs.appPackage5 = ""
        }
        if (homeAppsNum == 5) return

        binding.homeApp6.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp6, prefs.appName6, prefs.appPackage6, prefs.appUser6)) {
            prefs.appName6 = ""
            prefs.appPackage6 = ""
        }
        if (homeAppsNum == 6) return

        binding.homeApp7.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp7, prefs.appName7, prefs.appPackage7, prefs.appUser7)) {
            prefs.appName7 = ""
            prefs.appPackage7 = ""
        }
        if (homeAppsNum == 7) return

        binding.homeApp8.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp8, prefs.appName8, prefs.appPackage8, prefs.appUser8)) {
            prefs.appName8 = ""
            prefs.appPackage8 = ""
        }
    }

    private fun setHomeAppText(textView: TextView, appName: String, packageName: String, userString: String): Boolean {
        if (isPackageInstalled(requireContext(), packageName, userString)) {
            textView.text = appName
            return true
        }
        textView.text = ""
        return false
    }

    private fun hideHomeApps() {
        binding.homeApp1.visibility = View.GONE
        binding.homeApp2.visibility = View.GONE
        binding.homeApp3.visibility = View.GONE
        binding.homeApp4.visibility = View.GONE
        binding.homeApp5.visibility = View.GONE
        binding.homeApp6.visibility = View.GONE
        binding.homeApp7.visibility = View.GONE
        binding.homeApp8.visibility = View.GONE
    }

    private fun homeAppClicked(location: Int) {
        if (prefs.getAppName(location).isEmpty()) showLongPressToast()
        else launchApp(
            prefs.getAppName(location),
            prefs.getAppPackage(location),
            prefs.getAppActivityClassName(location),
            prefs.getAppUser(location)
        )
    }

    private fun launchApp(appName: String, packageName: String, activityClassName: String?, userString: String) {
        viewModel.selectedApp(
            AppModel(
                appName,
                null,
                packageName,
                activityClassName,
                false,
                getUserHandleFromString(requireContext(), userString)
            ),
            Constants.FLAG_LAUNCH_APP
        )
    }

    private fun showAppList(flag: Int, rename: Boolean = false, includeHiddenApps: Boolean = false) {
        viewModel.getAppList(includeHiddenApps)
        try {
            findNavController().navigate(
                R.id.action_mainFragment_to_appListFragment,
                bundleOf(
                    Constants.Key.FLAG to flag,
                    Constants.Key.RENAME to rename
                )
            )
        } catch (e: Exception) {
            findNavController().navigate(
                R.id.appListFragment,
                bundleOf(
                    Constants.Key.FLAG to flag,
                    Constants.Key.RENAME to rename
                )
            )
            e.printStackTrace()
        }
    }

    private fun swipeDownAction() {
        when (prefs.swipeDownAction) {
            Constants.SwipeDownAction.SEARCH -> openSearch(requireContext())
            else -> expandNotificationDrawer(requireContext())
        }
    }

    private fun openSwipeRightApp() {
        if (!prefs.swipeRightEnabled) return
        if (prefs.appPackageSwipeRight.isNotEmpty())
            launchApp(
                prefs.appNameSwipeRight,
                prefs.appPackageSwipeRight,
                prefs.appActivityClassNameRight,
                prefs.appUserSwipeRight
            )
        else openDialerApp(requireContext())
    }

    private fun openSwipeLeftApp() {
        if (!prefs.swipeLeftEnabled) return
        if (prefs.appPackageSwipeLeft.isNotEmpty())
            launchApp(
                prefs.appNameSwipeLeft,
                prefs.appPackageSwipeLeft,
                prefs.appActivityClassNameSwipeLeft,
                prefs.appUserSwipeLeft
            )
        else openCameraApp(requireContext())
    }

    private fun lockPhone() {
        requireActivity().runOnUiThread {
            try {
                deviceManager.lockNow()
            } catch (e: SecurityException) {
                requireContext().showToast(getString(R.string.please_turn_on_double_tap_to_unlock), Toast.LENGTH_LONG)
                findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
            } catch (e: Exception) {
                requireContext().showToast(getString(R.string.launcher_failed_to_lock_device), Toast.LENGTH_LONG)
                prefs.lockModeOn = false
            }
        }
    }

    private fun showStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.show(WindowInsets.Type.statusBars())
        else
            @Suppress("DEPRECATION", "InlinedApi")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.hide(WindowInsets.Type.statusBars())
        else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }
    }

    private fun changeAppTheme() {
        if (prefs.dailyWallpaper.not()) return
        val changedAppTheme = getChangedAppTheme(requireContext(), prefs.appTheme)
        prefs.appTheme = changedAppTheme
        if (prefs.dailyWallpaper) {
            setPlainWallpaperByTheme(requireContext(), changedAppTheme)
            viewModel.setWallpaperWorker()
        }
        requireActivity().recreate()
    }

    private fun openScreenTimeDigitalWellbeing() {
        val intent = Intent()
        try {
            intent.setClassName(
                Constants.DIGITAL_WELLBEING_PACKAGE_NAME,
                Constants.DIGITAL_WELLBEING_ACTIVITY
            )
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                intent.setClassName(
                    Constants.DIGITAL_WELLBEING_SAMSUNG_PACKAGE_NAME,
                    Constants.DIGITAL_WELLBEING_SAMSUNG_ACTIVITY
                )
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showLongPressToast() {
        requireContext().showToast(R.string.long_press_to_set_app)
    }

    private fun textOnClick(view: View) = onClick(view)

    private fun textOnLongClick(view: View) = onLongClick(view)

    private fun getSwipeGestureListener(context: Context): View.OnTouchListener {
        return object : OnSwipeTouchListener(context) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                openSwipeLeftApp()
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                openSwipeRightApp()
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                showAppList(Constants.FLAG_LAUNCH_APP)
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                swipeDownAction()
            }

            override fun onLongClick() {
                super.onLongClick()
                try {
                    findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
                    viewModel.firstOpen(false)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onDoubleClick() {
                super.onDoubleClick()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    binding.lock.performClick()
                else if (prefs.lockModeOn)
                    lockPhone()
            }

            override fun onClick() {
                super.onClick()
                viewModel.checkForMessages.call()
            }
        }
    }

    private fun getViewSwipeTouchListener(context: Context, view: View): View.OnTouchListener {
        return object : ViewSwipeTouchListener(context, view) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                openSwipeLeftApp()
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                openSwipeRightApp()
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                showAppList(Constants.FLAG_LAUNCH_APP)
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                swipeDownAction()
            }

            override fun onLongClick(view: View) {
                super.onLongClick(view)
                textOnLongClick(view)
            }

            override fun onClick(view: View) {
                super.onClick(view)
                textOnClick(view)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        requireContext().showToast("onActivityResult: $requestCode $resultCode")
        
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            val appWidgetId = data?.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
            
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                handleWidgetSelected(appWidgetId)
            }
        }
    }

    // Widget management methods
    private fun initWidgetLaunchers() {
        // Initialize widget picker launcher
        widgetPickerLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val appWidgetId = data?.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
                
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    handleWidgetSelected(appWidgetId)
                }
            }
        }
        
        // Initialize widget configuration launcher
        widgetConfigureLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val appWidgetId = data?.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
                
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    handleWidgetConfigured(appWidgetId)
                }
            }
        }
    }

    private fun handleWidgetSelected(appWidgetId: Int) {
        val appWidgetInfo = widgetHelper.getWidgetInfo(appWidgetId)
        if (appWidgetInfo != null) {
            if (appWidgetInfo.configure != null) {
                // Widget needs configuration
                val configureIntent = widgetHelper.createWidgetConfigureIntent(appWidgetId)
                configureIntent?.let { intent ->
                    widgetConfigureLauncher?.launch(intent)
                }
            } else {
                // Widget doesn't need configuration, add it directly
                addWidgetToHome(appWidgetId)
            }
        }
    }

    private fun handleWidgetConfigured(appWidgetId: Int) {
        addWidgetToHome(appWidgetId)
    }

    private fun addWidgetToHome(appWidgetId: Int) {
        try {
            if (widgetHelper.isWidgetInstalled(appWidgetId)) {
                val widgetArea = binding.widgetArea
                // Create widget view without adding to parent yet
                val widgetView = widgetHelper.createWidgetView(appWidgetId, null)
                if (widgetView != null && widgetArea != null) {
                    // Create a simple wrapper (fallback to FrameLayout if ResizableMovableLayout causes issues)
                    val wrapper = try {
                        ResizableMovableLayout(requireContext()).apply {
                            setWidgetId(appWidgetId)
                        }
                    } catch (e: Exception) {
                        Log.w("HomeFragment", "Using fallback FrameLayout wrapper: ${e.message}")
                        FrameLayout(requireContext())
                    }
                    
                    // Adjust widget size and positioning based on widget position setting
                    val (initialWidth, initialHeight, marginLeft) = when (prefs.widgetPosition) {
                        0 -> { // Top - full size
                            Triple(320, 520, 10)
                        }
                        1 -> { // Middle - smaller size
                            Triple(280, 440, 20)
                        }
                        2 -> { // Bottom - smaller size
                            Triple(260, 400, 30)
                        }
                        else -> Triple(320, 520, 10)
                    }
                    
                    // Use MarginLayoutParams for proper resizing and moving
                    val layoutParams = ViewGroup.MarginLayoutParams(initialWidth, initialHeight)
                    layoutParams.leftMargin = marginLeft
                    layoutParams.topMargin = 10 // Reduced from 25 to 10
                    wrapper.layoutParams = layoutParams
                    
                    // Add long-press listener to delete this specific widget
                    wrapper.setOnLongClickListener {
                        showDeleteWidgetDialog(appWidgetId)
                        true
                    }
                    
                    // Add the widget view to the wrapper
                    wrapper.addView(widgetView)
                    
                    // Add the wrapper to the widget area
                    widgetArea.addView(wrapper)
                    
                    // Store the widget view reference
                    widgetViews[appWidgetId] = widgetView
                    
                    // Save widget ID to preferences
                    val currentWidgetIds = prefs.widgetIds.toMutableSet()
                    currentWidgetIds.add(appWidgetId.toString())
                    prefs.widgetIds = currentWidgetIds
                    
                    // Show widget container and update layout only if this is the first widget
                    if (widgetViews.size == 1) {
                        widgetArea.visibility = View.VISIBLE
                        updateWidgetLayout()
                        // Hide the widget hint when first widget is added
                        binding.widgetHint?.visibility = View.GONE
                        // Show resize instructions for the first widget
                        requireContext().showToast(getString(R.string.widget_resize_help))
                    }
                    
                    requireContext().showToast("Widget added successfully")
                } else {
                    requireContext().showToast("Failed to create widget view")
                }
            } else {
                requireContext().showToast("Widget not installed")
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error adding widget: ${e.message}")
            requireContext().showToast("Error adding widget: ${e.message}")
        }
    }

    private fun removeWidget(appWidgetId: Int) {
        try {
            val widgetView = widgetViews[appWidgetId]
            if (widgetView != null) {
                val widgetArea = binding.widgetArea
                // Find and remove the wrapper (parent of the widget view)
                val wrapper = widgetView.parent as? View
                if (wrapper != null && widgetArea != null) {
                    widgetArea.removeView(wrapper)
                }
                widgetViews.remove(appWidgetId)
                widgetHelper.deleteAppWidgetId(appWidgetId)
                
                // Remove widget ID from preferences
                val currentWidgetIds = prefs.widgetIds.toMutableSet()
                currentWidgetIds.remove(appWidgetId.toString())
                prefs.widgetIds = currentWidgetIds
                
                // Clean up saved sizes and positions for this widget
                val currentSizes = WidgetSizeHelper.loadWidgetSizes(prefs.widgetSizes).toMutableMap()
                currentSizes.remove(appWidgetId)
                prefs.widgetSizes = WidgetSizeHelper.saveWidgetSizes(currentSizes)
                
                val currentPositions = WidgetSizeHelper.loadWidgetPositions(prefs.widgetPositions).toMutableMap()
                currentPositions.remove(appWidgetId)
                prefs.widgetPositions = WidgetSizeHelper.saveWidgetPositions(currentPositions)
                
                // Hide widget container if no widgets
                if (widgetViews.isEmpty()) {
                    widgetArea?.visibility = View.GONE
                    // Show widget hint when no widgets are present
                    binding.widgetHint?.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error removing widget: ${e.message}")
            requireContext().showToast("Error removing widget: ${e.message}")
        }
    }

    private fun updateWidgetLayout() {
        val parent = binding.mainLayout as LinearLayout
        val widgetArea = binding.widgetArea
        if (widgetArea != null && widgetViews.isNotEmpty()) {
            parent.removeView(widgetArea)
            
            // Adjust widget area size based on position
            val layoutParams = widgetArea.layoutParams as LinearLayout.LayoutParams
            when (prefs.widgetPosition) {
                0 -> { // Top - full size
                    layoutParams.height = 580
                    parent.addView(widgetArea, 1) // after widgetHint
                }
                1 -> { // Middle - smaller size
                    layoutParams.height = 500
                    parent.addView(widgetArea, parent.indexOfChild(binding.dateTimeLayout) + 1)
                }
                2 -> { // Bottom - smaller size
                    layoutParams.height = 460
                    parent.addView(widgetArea, parent.indexOfChild(binding.homeAppsLayout))
                }
            }
            widgetArea.layoutParams = layoutParams
            widgetArea.visibility = View.VISIBLE
            
            // Update existing widgets to match the new position
            updateExistingWidgetSizes()
        }
    }

    private fun updateExistingWidgetSizes() {
        // Load saved sizes and positions
        val savedSizes = WidgetSizeHelper.loadWidgetSizes(prefs.widgetSizes)
        val savedPositions = WidgetSizeHelper.loadWidgetPositions(prefs.widgetPositions)
        
        for ((widgetId, widgetView) in widgetViews) {
            val wrapper = widgetView.parent as? View
            if (wrapper != null) {
                val layoutParams = wrapper.layoutParams as? ViewGroup.MarginLayoutParams
                if (layoutParams != null) {
                    // Use saved size if available, otherwise use default based on position
                    val savedSize = savedSizes[widgetId]
                    if (savedSize != null) {
                        layoutParams.width = savedSize.width
                        layoutParams.height = savedSize.height
                    } else {
                        // Use default size based on position
                        val (defaultWidth, defaultHeight) = when (prefs.widgetPosition) {
                            0 -> Pair(320, 520) // Top - full size
                            1 -> Pair(280, 440) // Middle - smaller size
                            2 -> Pair(260, 400) // Bottom - smaller size
                            else -> Pair(320, 520)
                        }
                        layoutParams.width = defaultWidth
                        layoutParams.height = defaultHeight
                    }
                    
                    // Use saved position if available, otherwise use default
                    val savedPosition = savedPositions[widgetId]
                    if (savedPosition != null) {
                        layoutParams.leftMargin = savedPosition.leftMargin
                        layoutParams.topMargin = savedPosition.topMargin
                    } else {
                        // Use default position based on widget position setting
                        val (defaultMarginLeft, defaultMarginTop) = when (prefs.widgetPosition) {
                            0 -> Pair(10, 10) // Top
                            1 -> Pair(20, 10) // Middle
                            2 -> Pair(30, 10) // Bottom
                            else -> Pair(10, 10)
                        }
                        layoutParams.leftMargin = defaultMarginLeft
                        layoutParams.topMargin = defaultMarginTop
                    }
                    
                    wrapper.layoutParams = layoutParams
                }
            }
        }
    }

    private fun loadExistingWidgets() {
        if (prefs.widgetsEnabled) {
            val widgetIds = prefs.widgetIds
            for (widgetIdString in widgetIds) {
                val widgetId = widgetIdString.toIntOrNull()
                if (widgetId != null && widgetHelper.isWidgetInstalled(widgetId)) {
                    addWidgetToHome(widgetId)
                }
            }
            
            // Show hint about widget functionality
            if (widgetIds.isEmpty()) {
                requireContext().showToast(getString(R.string.long_press_to_add_widget))
                // Make widget hint more visible
                binding.widgetHint?.visibility = View.VISIBLE
                binding.widgetHint?.alpha = 1.0f
            } else {
                // Hide widget hint if widgets are already loaded
                binding.widgetHint?.visibility = View.GONE
                // Show resize help when widgets are present
                requireContext().showToast(getString(R.string.widget_resize_help))
            }
        }
    }

    fun addWidget() {
        requireContext().showToast("addWidget called")
        if (prefs.widgetsEnabled) {
            try {
                // Use a simpler approach - directly launch the widget picker
                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
                val appWidgetId = widgetHelper.allocateAppWidgetId()
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                
                // Launch the intent directly
                startActivityForResult(intent, 1001)
            } catch (e: Exception) {
                requireContext().showToast("Error: ${e.message}")
            }
        } else {
            requireContext().showToast("Please enable widgets in settings first")
        }
    }

    fun removeAllWidgets() {
        val widgetIds = widgetViews.keys.toList()
        for (widgetId in widgetIds) {
            removeWidget(widgetId)
        }
        prefs.widgetsEnabled = false
        
        // Clear all saved widget sizes and positions
        prefs.widgetSizes = "{}"
        prefs.widgetPositions = "{}"
        
        // Show widget hint again when all widgets are removed
        binding.widgetHint?.visibility = View.VISIBLE
        binding.widgetHint?.alpha = 1.0f
    }
    
    // Debug function to help troubleshoot widget issues
    fun debugWidgetInfo() {
        val availableWidgets = widgetHelper.getAvailableWidgets()
        requireContext().showToast("Widgets available: ${availableWidgets.size}")
    }

    // Debug function to test widget resizing
    fun debugWidgetResizing() {
        val widgetCount = widgetViews.size
        requireContext().showToast("Active widgets: $widgetCount")
        
        if (widgetCount > 0) {
            val firstWidget = widgetViews.values.first()
            val wrapper = firstWidget.parent
            if (wrapper is ResizableMovableLayout) {
                requireContext().showToast("Widget is resizable")
            } else {
                requireContext().showToast("Widget is not resizable (using fallback)")
            }
        } else {
            requireContext().showToast("No widgets to resize")
        }
    }

    // Handle widget position changes
    fun onWidgetPositionChanged() {
        if (widgetViews.isNotEmpty()) {
            updateWidgetLayout()
        }
    }

    // Show list of all widgets with delete options
    fun showWidgetList() {
        if (widgetViews.isEmpty()) {
            requireContext().showToast("No widgets to manage")
            return
        }

        val widgetNames = mutableListOf<String>()
        val widgetIds = mutableListOf<Int>()
        
        for ((widgetId, _) in widgetViews) {
            val widgetInfo = widgetHelper.getWidgetInfo(widgetId)
            val widgetName = widgetInfo?.loadLabel(requireContext().packageManager)?.toString() ?: "Unknown Widget"
            widgetNames.add(widgetName)
            widgetIds.add(widgetId)
        }

        val items = widgetNames.toTypedArray()
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Manage Widgets")
            .setItems(items) { _, which ->
                val selectedWidgetId = widgetIds[which]
                val selectedWidgetName = widgetNames[which]
                showDeleteWidgetDialog(selectedWidgetId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRemoveWidgetDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Remove Widget")
            .setMessage("Do you want to remove all widgets?")
            .setPositiveButton("Remove") { _, _ ->
                removeAllWidgets()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteWidgetDialog(appWidgetId: Int) {
        val widgetInfo = widgetHelper.getWidgetInfo(appWidgetId)
        val widgetName = widgetInfo?.loadLabel(requireContext().packageManager)?.toString() ?: "Widget"
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Remove Widget")
            .setMessage("Do you want to remove \"$widgetName\"?")
            .setPositiveButton("Remove") { _, _ ->
                removeWidget(appWidgetId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}