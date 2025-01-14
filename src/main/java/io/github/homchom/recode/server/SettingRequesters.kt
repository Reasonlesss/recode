package io.github.homchom.recode.server

import io.github.homchom.recode.event.nullaryToggleRequesterGroup
import io.github.homchom.recode.event.nullaryTrial
import io.github.homchom.recode.event.requester
import io.github.homchom.recode.event.trial
import io.github.homchom.recode.mc
import io.github.homchom.recode.mod.features.LagslayerHUD
import io.github.homchom.recode.ui.equalsUnstyled
import io.github.homchom.recode.ui.matchesUnstyled
import io.github.homchom.recode.util.cachedRegexBuilder
import io.github.homchom.recode.util.unitOrNull
import net.minecraft.world.effect.MobEffects

val ChatLocalRequester = requester("/chat local", nullaryTrial(
    ReceiveChatMessageEvent,
    start = { sendCommand("chat local") },
    tests = { (text), _ -> text.equalsUnstyled("Your chat is now set to LOCAL").instantUnitOrNull() }
))

private val timeRegex = cachedRegexBuilder<Long> { time ->
    Regex("""$GREEN_ARROW_CHAR Set your player time to ${time ?: "[0-9+]"}.""")
}

// TODO: support time keywords through command suggestions, not enum
val ClientTimeRequester = requester("/time", trial(
    ReceiveChatMessageEvent,
    start = { time: Long -> sendCommand("time $time") },
    tests = { time, (text), _ ->
        timeRegex(time).matchesUnstyled(text).unitOrNull().let(::instant)
    }
))

// TODO: support time keywords through command suggestions, not enum
val FlightRequesters = nullaryToggleRequesterGroup("/fly",
    ReceiveChatMessageEvent,
    start = { sendCommand("fly") },
    enabledPredicate = { mc.player!!.isFlightEnabled },
    enabledTests = { (text), _ ->
        text.equalsUnstyled("$GREEN_ARROW_CHAR Flight enabled.").instantUnitOrNull()
    },
    disabledTests = { (text), _ ->
        text.equalsUnstyled("$GREEN_ARROW_CHAR Flight disabled.").instantUnitOrNull()
    }
)

private val lsEnabledRegex =
    Regex("""$LAGSLAYER_PATTERN Now monitoring plot (\d+)\. Type /lagslayer to stop monitoring\.""")
private val lsDisabledRegex =
    Regex("""$LAGSLAYER_PATTERN Stopped monitoring plot (\d+)\.""")

// TODO: improve enabledPredicate once arbitrary requesters are able to be invalidated
val LagSlayerRequesters = nullaryToggleRequesterGroup("/lagslayer",
    ReceiveChatMessageEvent,
    start = { sendCommand("lagslayer") },
    enabledPredicate = { LagslayerHUD.lagSlayerEnabled },
    enabledTests = { (text), _ -> lsEnabledRegex.matchesUnstyled(text).instantUnitOrNull() },
    disabledTests = { (text), _ -> lsDisabledRegex.matchesUnstyled(text).instantUnitOrNull() }
)

val NightVisionRequesters = nullaryToggleRequesterGroup("night vision",
    ReceiveChatMessageEvent,
    start = { sendCommand("nightvis") },
    enabledPredicate = { mc.player!!.hasEffect(MobEffects.NIGHT_VISION) },
    enabledTests = { (text), _ ->
        text.equalsUnstyled("$GREEN_ARROW_CHAR Enabled night vision.").instantUnitOrNull()
    },
    disabledTests = { (text), _ ->
        text.equalsUnstyled("$GREEN_ARROW_CHAR Disabled night vision.").instantUnitOrNull()
    }
)