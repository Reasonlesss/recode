package io.github.homchom.recode.multiplayer.event

import io.github.homchom.recode.event.Detector
import io.github.homchom.recode.event.trial.detector
import io.github.homchom.recode.event.trial.nullaryTrial
import io.github.homchom.recode.multiplayer.BOOSTER_ARROW
import io.github.homchom.recode.multiplayer.ReceiveChatMessageEvent
import io.github.homchom.recode.multiplayer.TOKEN_NOTCH_CHAR
import io.github.homchom.recode.multiplayer.username
import io.github.homchom.recode.ui.matchEntireUnstyled
import io.github.homchom.recode.ui.matchesUnstyled
import io.github.homchom.recode.util.regex.RegexPatternBuilder
import io.github.homchom.recode.util.regex.regex

data class ActiveBoosterMessage(val player: String, val canTip: Boolean) {
    companion object : Detector<Unit, ActiveBoosterMessage> by detector("active booster", nullaryTrial(
        ReceiveChatMessageEvent,
        tests = { (message) ->
            val player = ActiveBoosterMessage.mainRegex.matchEntireUnstyled(message)!!.groupValues[1]
            val subsequent = ReceiveChatMessageEvent.add()

            suspending {
                val (first) = subsequent.receive()
                val canTip = ActiveBoosterMessage.commandRegex.matchesUnstyled(first)

                if (!ActiveBoosterMessage.timeRegex.matchesUnstyled(first)) {
                    +testBoolean(subsequent) { (second) ->
                        ActiveBoosterMessage.timeRegex.matchesUnstyled(second)
                    }
                }

                ActiveBoosterMessage(player, canTip)
            }
        }
    )) {
        private fun RegexPatternBuilder.boosterArrow() {
            str(BOOSTER_ARROW) * (2..3)
            space
        }

        private val mainRegex = regex {
            // Regex("""$BOOSTER_ARROW_PATTERN ($USERNAME_PATTERN) is using a \d+x booster\.""")
            boosterArrow()
            val player by group { username() }
            str(" is using a ")
            digit.oneOrMore()
            str("x booster.")
        }
        private val commandRegex = regex {
            // Regex("""$BOOSTER_ARROW_PATTERN Use /tip to show your appreciation """ +
            //         """and receive a $TOKEN_NOTCH_CHAR token notch!""")
            boosterArrow()
            str("Use /tip to show your appreciation and receive a $TOKEN_NOTCH_CHAR token notch!")
        }
        private val timeRegex = regex {
            // Regex("""$BOOSTER_ARROW_PATTERN The booster wears off in \d+ (?:day|hour|minute|second)s?\.""")
            boosterArrow()
            str("The booster wears off in ")
            digit.oneOrMore()
            space
            anyStr("day", "hour", "minute", "second"); str("s").optional()
            period
        }
    }
}