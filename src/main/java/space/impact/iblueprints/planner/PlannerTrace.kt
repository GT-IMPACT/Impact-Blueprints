package space.impact.iblueprints.planner

import java.io.File
import net.minecraftforge.common.config.Configuration
import org.apache.logging.log4j.LogManager

private val plannerTraceLogger = LogManager.getLogger("ImpactBlueprints")

@Volatile
internal var plannerTraceEnabled: Boolean = true
    private set

internal fun loadPlannerTraceConfig(file: File) {
    val config = Configuration(file)
    plannerTraceEnabled = config.getBoolean(
        "plannerTrace",
        "diagnostics",
        true,
        "Log scene and planner operations. Restart Minecraft or the dedicated server after changing this.",
    )
    if (config.hasChanged()) {
        config.save()
    }
}

internal fun logPlannerTrace(message: String) {
    if (plannerTraceEnabled) {
        plannerTraceLogger.info("[PlannerTrace] $message")
    }
}

internal fun logPlannerTraceFailure(
    message: String,
    failure: Throwable,
) {
    if (plannerTraceEnabled && (failure is Exception || failure is LinkageError)) {
        plannerTraceLogger.warn(
            "[PlannerTrace] $message",
            failure,
        )
    }
}

internal inline fun <T> tracePlannerStep(
    step: String,
    details: String,
    action: () -> T,
): T {
    if (!plannerTraceEnabled) {
        return action()
    }
    val start = System.nanoTime()
    logPlannerTrace("$step begin $details")
    val result = try {
        action()
    } catch (failure: Throwable) {
        logPlannerTraceFailure(
            message = "$step failed $details",
            failure = failure,
        )
        throw failure
    }
    val elapsedMs = (System.nanoTime() - start) / 1_000_000
    logPlannerTrace("$step end elapsedMs=$elapsedMs $details")
    return result
}
