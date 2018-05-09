package com.merxury.blocker.ui.component

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.ComponentInfo
import android.content.pm.PackageManager
import android.util.Log
import com.merxury.blocker.core.ApplicationComponents
import com.merxury.blocker.core.IController
import com.merxury.blocker.core.root.ComponentControllerProxy
import com.merxury.blocker.core.root.EControllerMethod
import com.merxury.blocker.entity.getSimpleName
import com.merxury.blocker.ui.strategy.entity.view.ComponentBriefInfo
import com.merxury.blocker.ui.strategy.service.ApiClient
import com.merxury.blocker.ui.strategy.service.IClientServer
import com.merxury.ifw.IntentFirewall
import com.merxury.ifw.IntentFirewallImpl
import com.merxury.ifw.entity.ComponentType
import io.reactivex.Single
import io.reactivex.SingleOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiConsumer
import io.reactivex.schedulers.Schedulers

class ComponentPresenter(val context: Context, val view: ComponentContract.View, val packageName: String) : ComponentContract.Presenter, IController {
    private val pm: PackageManager

    private val controller: IController by lazy {
        ComponentControllerProxy.getInstance(EControllerMethod.PM, null)
    }
    private val componentClient: IClientServer by lazy {
        ApiClient.createClient()
    }

    private val ifwController: IntentFirewall by lazy {
        IntentFirewallImpl(context, packageName)
    }

    init {
        view.presenter = this
        pm = context.packageManager
    }

    @SuppressLint("CheckResult")
    override fun loadComponents(packageName: String, type: EComponentType) {
        Log.i(TAG, "Trying to load components for $packageName, type: $type")
        view.setLoadingIndicator(true)
        Single.create((SingleOnSubscribe<List<ComponentInfo>> { emitter ->
            var componentList = when (type) {
                EComponentType.RECEIVER -> ApplicationComponents.getReceiverList(pm, packageName)
                EComponentType.ACTIVITY -> ApplicationComponents.getActivityList(pm, packageName)
                EComponentType.SERVICE -> ApplicationComponents.getServiceList(pm, packageName)
                EComponentType.PROVIDER -> ApplicationComponents.getProviderList(pm, packageName)
                else -> ArrayList<ComponentInfo>()
            }
            componentList = sortComponentList(componentList, currentComparator)
            emitter.onSuccess(componentList)
        })).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ components ->
                    view.setLoadingIndicator(false)
                    if (components.isEmpty()) {
                        view.showNoComponent()
                    } else {
                        view.showComponentList(components)
                    }
                })
    }

    @SuppressLint("CheckResult")
    override fun switchComponent(packageName: String, componentName: String, state: Int): Boolean {
        Single.create((SingleOnSubscribe<Boolean> { emitter ->
            try {
                val result = controller.switchComponent(packageName, componentName, state)
                emitter.onSuccess(result)
            } catch (e: Exception) {
                emitter.onError(e)
            }
        })).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(BiConsumer { _, error ->
                    view.refreshComponentSwitchState(componentName)
                    error?.apply {
                        Log.e(TAG, message)
                        printStackTrace()
                        view.showAlertDialog()
                    }
                })
        return true
    }

    @SuppressLint("CheckResult")
    override fun enableComponent(componentInfo: ComponentInfo): Boolean {
        Log.i(TAG, "Trying to enable component: ${componentInfo.name}")
        Single.create((SingleOnSubscribe<Boolean> { emitter ->
            try {
                val result = controller.enableComponent(componentInfo)
                emitter.onSuccess(result)
            } catch (e: Exception) {
                emitter.onError(e)
            }
        })).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(BiConsumer { _, error ->
                    view.refreshComponentSwitchState(componentInfo.name)
                    error?.apply {
                        Log.e(TAG, message)
                        printStackTrace()
                        view.showAlertDialog()
                    }
                })
        return true
    }

    @SuppressLint("CheckResult")
    override fun disableComponent(componentInfo: ComponentInfo): Boolean {
        Log.i(TAG, "Trying to disable component: ${componentInfo.name}")
        Single.create((SingleOnSubscribe<Boolean> { emitter ->
            try {
                val result = controller.disableComponent(componentInfo)
                emitter.onSuccess(result)
            } catch (e: Exception) {
                emitter.onError(e)
            }
        })).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(BiConsumer { _, error ->
                    view.refreshComponentSwitchState(componentInfo.name)
                    error?.apply {
                        Log.e(TAG, message)
                        printStackTrace()
                        view.showAlertDialog()
                    }
                })
        return true
    }

    override fun sortComponentList(components: List<ComponentInfo>, type: EComponentComparatorType): List<ComponentInfo> {
        return when (type) {
            EComponentComparatorType.NAME_ASCENDING -> components.sortedBy { it.getSimpleName() }
            EComponentComparatorType.NAME_DESCENDING -> components.sortedByDescending { it.getSimpleName() }
            EComponentComparatorType.PACKAGE_NAME_ASCENDING -> components.sortedBy { it.name }
            EComponentComparatorType.PACKAGE_NAME_DESCENDING -> components.sortedByDescending { it.name }
        }
    }

    override fun checkComponentIsVoted(component: ComponentInfo): Boolean {
        val sharedPreferences = context.getSharedPreferences(component.packageName, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(component.name, false)

    }

    override fun writeComponentVoteState(component: ComponentInfo, like: Boolean) {
        val sharedPreferences = context.getSharedPreferences(component.packageName, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(component.name, like)
        editor.apply()
    }

    @SuppressLint("CheckResult")
    override fun voteForComponent(component: ComponentInfo) {
        componentClient.upVoteForComponent(ComponentBriefInfo(component))
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ result ->

                }, { error ->

                })
    }

    @SuppressLint("CheckResult")
    override fun downVoteForComponent(component: ComponentInfo) {
        componentClient.downVoteForComponent(ComponentBriefInfo(component))
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ result ->

                }, { error ->

                })
    }

    override fun addToIFW(component: ComponentInfo, type: EComponentType) {
        Log.i(TAG, "Disable component via IFW: ${component.name}")
        Single.create((SingleOnSubscribe<Boolean> { emitter ->
            try {
                when (type) {
                    EComponentType.ACTIVITY -> ifwController.addComponent(component, ComponentType.ACTIVITY)
                    EComponentType.RECEIVER -> ifwController.addComponent(component, ComponentType.BROADCAST)
                    EComponentType.SERVICE -> ifwController.addComponent(component, ComponentType.SERVICE)
                    else -> {
                    }
                }
                emitter.onSuccess(true)
                //TODO Duplicated code
            } catch (e: Exception) {
                emitter.onError(e)
            }
        })).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(BiConsumer { _, error ->
                    view.refreshComponentSwitchState(component.name)
                    error?.apply {
                        Log.e(TAG, message)
                        printStackTrace()
                        view.showAlertDialog()
                    }
                })
    }

    override fun removeFromIFW(component: ComponentInfo, type: EComponentType) {
        Log.i(TAG, "Disable component via IFW: ${component.name}")
        Single.create((SingleOnSubscribe<Boolean> { emitter ->
            try {
                when (type) {
                    EComponentType.ACTIVITY -> ifwController.removeComponent(component, ComponentType.ACTIVITY)
                    EComponentType.RECEIVER -> ifwController.removeComponent(component, ComponentType.BROADCAST)
                    EComponentType.SERVICE -> ifwController.removeComponent(component, ComponentType.SERVICE)
                    else -> {
                    }
                }
                emitter.onSuccess(true)
                //TODO Duplicated code
            } catch (e: Exception) {
                emitter.onError(e)
            }
        })).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(BiConsumer { _, error ->
                    view.refreshComponentSwitchState(component.name)
                    error?.apply {
                        Log.e(TAG, message)
                        printStackTrace()
                        view.showAlertDialog()
                    }
                })
    }

    override fun checkComponentEnableState(component: ComponentInfo): Boolean {
        return ApplicationComponents.checkComponentIsEnabled(pm, ComponentName(component.packageName, component.name)) and
                ifwController.getComponentEnableState(component)
    }

    override fun start(context: Context) {

    }

    override fun destroy() {
        try {
            ifwController.saveRules()
        } catch (e: Exception) {
            Log.w(TAG, "Cannot save rules, message is ${e.message}")
        }
    }

    override var currentComparator: EComponentComparatorType = EComponentComparatorType.NAME_ASCENDING

    companion object {
        const val TAG = "ComponentPresenter"
    }
}