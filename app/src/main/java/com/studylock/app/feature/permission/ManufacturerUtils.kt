package com.studylock.app.feature.permission

import android.content.Context
import android.content.Intent
import android.os.Build

enum class ManufacturerType {
    XIAOMI,
    HUAWEI,
    OPPO,
    VIVO,
    SAMSUNG,
    ONEPLUS,
    MEIZU,
    OTHER
}

data class ManufacturerGuideInfo(
    val manufacturer: ManufacturerType,
    val displayName: String,
    val guideSteps: List<String>,
    val settingsIntent: Intent? = null
)

object ManufacturerUtils {

    fun detectManufacturer(): ManufacturerType {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND?.lowercase() ?: ""

        return when {
            manufacturer.contains("xiaomi") || brand.contains("redmi") || brand.contains("mi") -> ManufacturerType.XIAOMI
            manufacturer.contains("huawei") || brand.contains("honor") -> ManufacturerType.HUAWEI
            manufacturer.contains("oneplus") || brand.contains("oneplus") -> ManufacturerType.ONEPLUS
            manufacturer.contains("oppo") || brand.contains("realme") -> ManufacturerType.OPPO
            manufacturer.contains("vivo") -> ManufacturerType.VIVO
            manufacturer.contains("samsung") -> ManufacturerType.SAMSUNG
            manufacturer.contains("meizu") -> ManufacturerType.MEIZU
            else -> ManufacturerType.OTHER
        }
    }

    fun getGuideInfo(context: Context): ManufacturerGuideInfo {
        val type = detectManufacturer()
        return when (type) {
            ManufacturerType.XIAOMI -> ManufacturerGuideInfo(
                manufacturer = ManufacturerType.XIAOMI,
                displayName = "小米/Redmi",
                guideSteps = listOf(
                    "1. 打开手机管家 → 应用管理 → 权限 → 自启动管理 → 找到 StudyLock 并开启",
                    "2. 设置 → 应用设置 → 授权管理 → 应用权限管理 → 找到 StudyLock → 后台弹出界面 → 允许",
                    "3. 设置 → 省电与电池 → 右上角齿轮 → 应用智能省电 → 找到 StudyLock → 选择'无限制'",
                    "4. 设置 → 更多设置 → 开发者选项 → 关闭 MIUI 优化（可选）"
                ),
                settingsIntent = try {
                    Intent().setClassName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                } catch (_: Exception) {
                    null
                }
            )
            ManufacturerType.HUAWEI -> ManufacturerGuideInfo(
                manufacturer = ManufacturerType.HUAWEI,
                displayName = "华为/Honor",
                guideSteps = listOf(
                    "1. 设置 → 电池 → 更多电池设置 → 找到 StudyLock → 关闭'自动管理'",
                    "2. 设置 → 应用和服务 → 应用启动管理 → 找到 StudyLock → 关闭'自动管理' → 允许自启动和后台活动",
                    "3. 打开手机管家 → 应用启动管理 → 找到 StudyLock → 允许自启动和关联启动",
                    "4. 设置 → 应用 → 权限管理 → 找到 StudyLock → 允许所有必要权限"
                ),
                settingsIntent = try {
                    Intent().setClassName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                } catch (_: Exception) {
                    null
                }
            )
            ManufacturerType.OPPO -> ManufacturerGuideInfo(
                manufacturer = ManufacturerType.OPPO,
                displayName = "OPPO/realme",
                guideSteps = listOf(
                    "1. 设置 → 电池 → 更多设置 → 耗电保护 → 找到 StudyLock → 允许后台运行",
                    "2. 设置 → 应用管理 → 应用列表 → 找到 StudyLock → 耗电管理 → 允许完全后台行为",
                    "3. 设置 → 电池 → 应用速冻 → 找到 StudyLock → 关闭速冻",
                    "4. 设置 → 权限与隐私 → 权限管理 → 找到 StudyLock → 允许自启动"
                ),
                settingsIntent = try {
                    Intent().setClassName(
                        "com.coloros.oppoguardelf",
                        "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"
                    )
                } catch (_: Exception) {
                    null
                }
            )
            ManufacturerType.VIVO -> ManufacturerGuideInfo(
                manufacturer = ManufacturerType.VIVO,
                displayName = "vivo",
                guideSteps = listOf(
                    "1. 设置 → 电池 → 后台高耗电 → 找到 StudyLock → 允许",
                    "2. 设置 → 应用与权限 → 权限管理 → 权限 → 自启动 → 找到 StudyLock → 允许",
                    "3. 打开 i管家 → 应用管理 → 权限管理 → 自启动 → 找到 StudyLock → 允许",
                    "4. 设置 → 电池 → 后台耗电管理 → 找到 StudyLock → 允许后台高耗电"
                ),
                settingsIntent = try {
                    Intent().setClassName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.SoftPermissionDetailActivity"
                    )
                } catch (_: Exception) {
                    null
                }
            )
            ManufacturerType.SAMSUNG -> ManufacturerGuideInfo(
                manufacturer = ManufacturerType.SAMSUNG,
                displayName = "三星",
                guideSteps = listOf(
                    "1. 设置 → 电池和设备维护 → 电池 → 后台使用限制 → 找到 StudyLock → 取消限制",
                    "2. 设置 → 应用程序 → 找到 StudyLock → 电池 → 不受限制",
                    "3. 设置 → 常规管理 → 电池 → 更多电池设置 → 休眠 → 从不休眠的应用程序 → 添加 StudyLock",
                    "4. 智能管理器 → 自动优化 → 关闭自动优化（可选）"
                ),
                settingsIntent = try {
                    Intent().setClassName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"
                    )
                } catch (_: Exception) {
                    null
                }
            )
            ManufacturerType.ONEPLUS -> ManufacturerGuideInfo(
                manufacturer = ManufacturerType.ONEPLUS,
                displayName = "一加",
                guideSteps = listOf(
                    "1. 设置 → 电池 → 电池优化 → 找到 StudyLock → 不优化",
                    "2. 设置 → 应用 → 找到 StudyLock → 耗电管理 → 允许后台活动",
                    "3. 设置 → 应用 → 找到 StudyLock → 自启动 → 允许"
                ),
                settingsIntent = try {
                    Intent().setClassName(
                        "com.oneplus.security",
                        "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                    )
                } catch (_: Exception) {
                    null
                }
            )
            ManufacturerType.MEIZU -> ManufacturerGuideInfo(
                manufacturer = ManufacturerType.MEIZU,
                displayName = "魅族",
                guideSteps = listOf(
                    "1. 设置 → 应用管理 → 权限管理 → 自启动管理 → 找到 StudyLock → 允许",
                    "2. 设置 → 应用管理 → 找到 StudyLock → 权限管理 → 后台管理 → 允许后台运行",
                    "3. 设置 → 电池 → 找到 StudyLock → 省电策略 → 无限制"
                ),
                settingsIntent = null
            )
            ManufacturerType.OTHER -> ManufacturerGuideInfo(
                manufacturer = ManufacturerType.OTHER,
                displayName = "其他品牌",
                guideSteps = listOf(
                    "1. 在系统设置中找到'电池'或'应用管理'",
                    "2. 找到 StudyLock 应用",
                    "3. 在电池/耗电管理中选择'无限制'或'不优化'",
                    "4. 在自启动管理中允许 StudyLock 自启动",
                    "5. 不同品牌手机的具体路径可能有所不同，请参考手机说明书"
                ),
                settingsIntent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }
    }
}
