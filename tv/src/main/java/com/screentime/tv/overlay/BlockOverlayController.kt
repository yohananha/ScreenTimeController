package com.screentime.tv.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.screentime.tv.ui.BackPressHandler
import com.screentime.tv.ui.BlockOverlayContent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockOverlayController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val codeRedeemer: CodeRedeemer,
    private val requestController: RequestController,
) {
    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var hostedView: View? = null
    private val currentPackage = mutableStateOf<String?>(null)
    private val backPressHandler = BackPressHandler()

    // WindowManager + the Compose lifecycle/SavedState registries must be
    // touched from the main thread, but evaluate() runs on Dispatchers.Default.
    fun show(blockedPackage: String) {
        mainHandler.post {
            currentPackage.value = blockedPackage
            if (hostedView == null) {
                val composeView = ComposeView(context).apply {
                    setContent {
                        val lockout by codeRedeemer.lockout.collectAsState()
                        val requestStatus by requestController.requestStatus.collectAsState()
                        BlockOverlayContent(
                            blockedPackage = currentPackage.value ?: "",
                            lockout = lockout,
                            requestStatus = requestStatus,
                            backPressHandler = backPressHandler,
                            onSubmitCode = { entered -> codeRedeemer.redeem(entered, currentPackage.value) },
                            onSubmitRequest = { minutes ->
                                val pkg = currentPackage.value
                                pkg != null && requestController.submit(pkg, minutes) != null
                            },
                            onLockoutTick = { codeRedeemer.clearExpiredLockout() },
                        )
                    }
                }
                // Wrap in a FrameLayout to intercept the hardware Back key before
                // Compose's focus dispatch swallows it. The Compose lifecycle/
                // SavedState owners must be set on this root view, since
                // getWindowRecomposer() resolves them from the view added to
                // the WindowManager, not from the ComposeView child.
                val container = BackInterceptingContainer(context, backPressHandler).apply {
                    val owner = OverlayLifecycleOwner()
                    owner.start()
                    setViewTreeLifecycleOwner(owner)
                    setViewTreeViewModelStoreOwner(owner)
                    setViewTreeSavedStateRegistryOwner(owner)
                    addView(
                        composeView,
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                        ),
                    )
                }
                windowManager.addView(container, layoutParams())
                hostedView = container
            }
        }
    }

    fun hide() {
        mainHandler.post {
            hostedView?.let { windowManager.removeView(it) }
            hostedView = null
            currentPackage.value = null
        }
    }

    fun isShown(): Boolean = hostedView != null

    private fun layoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
        }
    }
}

private class BackInterceptingContainer(
    context: Context,
    private val backPressHandler: BackPressHandler,
) : FrameLayout(context) {
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            backPressHandler.onBackPressed?.invoke()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}

private class OverlayLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {
    private val registry = androidx.lifecycle.LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val viewModelStore = ViewModelStore()

    init {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
    }

    fun start() {
        registry.currentState = androidx.lifecycle.Lifecycle.State.RESUMED
    }

    override val lifecycle: androidx.lifecycle.Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
}
