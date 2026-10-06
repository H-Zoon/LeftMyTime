package com.devidea.timeleft.widget

/** Encoding version is independent of the Android SDK and the host's document version. */
internal enum class WidgetSecondsProfile { V6, V7 }

internal data class WidgetSecondsCapability(
    val hostVersion: Long?,
    val validationProfile: WidgetSecondsProfile?,
    val productionProfile: WidgetSecondsProfile?,
) {
    companion object {
        fun forHost(sdkInt: Int, documentVersion: Long?): WidgetSecondsCapability {
            val profile = if (sdkInt < 35) null else when {
                documentVersion == 6L -> WidgetSecondsProfile.V6
                documentVersion == 8L || (documentVersion != null && documentVersion >= 9) -> WidgetSecondsProfile.V7
                else -> null // Unknown/older hosts need their own protocol and runtime evidence.
            }
            // V6/Launcher3 and V8/Pixel Launcher passed live widget checks on 2026-10-06.
            // Keep the explicit version list: an untested V7 host is not implied by V6 support.
            val released = profile != null && (documentVersion == 6L || documentVersion == 8L ||
                (documentVersion != null && documentVersion >= 9))
            return WidgetSecondsCapability(documentVersion, profile, profile.takeIf { released })
        }
    }
}
