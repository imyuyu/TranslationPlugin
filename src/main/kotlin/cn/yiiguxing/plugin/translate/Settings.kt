package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.trans.GoogleTranslator
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.util.PasswordSafeDelegate
import cn.yiiguxing.plugin.translate.util.SelectionMode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import kotlin.properties.Delegates

/**
 * Settings
 */
@State(name = "Settings", storages = [(Storage("yiiguxing.translation.xml"))])
class Settings : PersistentStateComponent<Settings> {

    /**
     * 翻译API
     */
    var translator: String
            by Delegates.observable(GoogleTranslator.TRANSLATOR_ID) { _, oldValue: String, newValue: String ->
                if (oldValue != newValue) {
                    settingsChangePublisher.onTranslatorChanged(this, newValue)
                }
            }

    /**
     * 谷歌翻译选项
     */
    var googleTranslateSettings: GoogleTranslateSettings = GoogleTranslateSettings()
    /**
     * 有道翻译选项
     */
    var youdaoTranslateSettings: YoudaoTranslateSettings = YoudaoTranslateSettings()

    /**
     * 是否覆盖默认字体
     */
    var isOverrideFont: Boolean by Delegates.observable(false) { _, oldValue: Boolean, newValue: Boolean ->
        if (oldValue != newValue) {
            settingsChangePublisher.onOverrideFontChanged(this)
        }
    }
    /**
     * 主要字体
     */
    var primaryFontFamily: String? by Delegates.observable(null) { _, oldValue: String?, newValue: String? ->
        if (oldValue != newValue) {
            settingsChangePublisher.onOverrideFontChanged(this)
        }
    }
    /**
     * 音标字体
     */
    var phoneticFontFamily: String?  by Delegates.observable(null) { _, oldValue: String?, newValue: String? ->
        if (oldValue != newValue) {
            settingsChangePublisher.onOverrideFontChanged(this)
        }
    }

    var showStatusIcon: Boolean by Delegates.observable(true) { _, oldValue: Boolean, newValue: Boolean ->
        if (oldValue != newValue) {
            settingsChangePublisher.onWindowOptionsChanged(this, WindowOption.STATUS_ICON)
        }
    }

    /**
     * 是否关闭设置APP KEY通知
     */
    var isDisableAppKeyNotification = false
    /**
     * 自动取词模式
     */
    var autoSelectionMode: SelectionMode = SelectionMode.INCLUSIVE

    @Transient
    private val settingsChangePublisher: SettingsChangeListener =
            ApplicationManager.getApplication().messageBus.syncPublisher(SettingsChangeListener.TOPIC)

    override fun getState(): Settings = this

    override fun loadState(state: Settings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {

        /**
         * Get the instance of this service.
         *
         * @return the unique [Settings] instance.
         */
        val instance: Settings
            get() = ServiceManager.getService(Settings::class.java)

    }
}

private const val PASSWORD_SERVICE_NAME = "YIIGUXING.TRANSLATION"
private const val YOUDAO_APP_KEY = "YOUDAO_APP_KEY"

/**
 * 谷歌翻译选项
 *
 * @property primaryLanguage 主要语言
 */
@Tag("google-translate")
data class GoogleTranslateSettings(var primaryLanguage: Lang = Lang.CHINESE)

/**
 * 有道翻译选项
 *
 * @property primaryLanguage    主要语言
 * @property appId              应用ID
 * @property isAppKeyConfigured 应用密钥设置标志
 */
@Tag("youdao-translate")
data class YoudaoTranslateSettings(
        var primaryLanguage: Lang = Lang.AUTO,
        var appId: String = "",
        var isAppKeyConfigured: Boolean = false
) {
    private var _appKey: String?  by PasswordSafeDelegate(PASSWORD_SERVICE_NAME, YOUDAO_APP_KEY)
        @Transient get
        @Transient set

    /** 获取应用密钥. */
    @Transient
    fun getAppKey(): String = _appKey?.trim() ?: ""

    /** 设置应用密钥. */
    @Transient
    fun setAppKey(value: String?) {
        isAppKeyConfigured = !value.isNullOrBlank()
        _appKey = if (value.isNullOrBlank()) null else value
    }
}

enum class WindowOption {
    STATUS_ICON
}

interface SettingsChangeListener {

    fun onTranslatorChanged(settings: Settings, translatorId: String) {}

    fun onOverrideFontChanged(settings: Settings) {}

    fun onWindowOptionsChanged(settings: Settings, option: WindowOption) {}

    companion object {
        val TOPIC: Topic<SettingsChangeListener> = Topic.create(
                "TranslationSettingsChanged",
                SettingsChangeListener::class.java
        )
    }
}
