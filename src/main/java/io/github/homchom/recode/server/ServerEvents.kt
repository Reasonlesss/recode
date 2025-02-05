package io.github.homchom.recode.server

import io.github.homchom.recode.event.*
import io.github.homchom.recode.mc
import io.github.homchom.recode.render.RenderThreadContext
import io.github.homchom.recode.server.state.*
import io.github.homchom.recode.ui.matchEntireUnstyled
import io.github.homchom.recode.ui.matchesUnstyled
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.network.chat.Component

object JoinServerEvent :
    WrappedEvent<ServerJoinContext, Join> by
        wrapFabricEvent(ClientPlayConnectionEvents.JOIN, { listener ->
            Join { handler, sender, client -> listener(ServerJoinContext(handler, sender, client)) }
        })

object DisconnectFromServerEvent :
    WrappedEvent<ServerDisconnectContext, Disconnect> by
        wrapFabricEvent(ClientPlayConnectionEvents.DISCONNECT, { listener ->
            Disconnect { handler, client -> listener(ServerDisconnectContext(handler, client)) }
        })

data class ServerJoinContext(val handler: ClientPacketListener, val sender: PacketSender, val client: Minecraft)
data class ServerDisconnectContext(val handler: ClientPacketListener, val client: Minecraft)

private val welcomeRegex = Regex("""$DIAMOND_CHAR Welcome (?:back )?to DiamondFire! $DIAMOND_CHAR""")
private val patchRegex = Regex("""Current patch: (.+). See the patch notes with /patch!""")

object JoinDFDetector :
    Detector<Unit, JoinDFInfo> by detector(nullaryTrial(JoinServerEvent) {
        requireFalse(isOnDF) // if already on DF, this is a node switch and should not be tested
        requireTrue(ipMatchesDF)

        // pre-register TipMessage as an implicit dependency
        TipMessage.getNotificationsFrom(module)

        suspending {
            enforceOn<_, Unit>(DisconnectFromServerEvent) { null } // TODO: nicer syntax?

            val patch = withContext(RenderThreadContext) {
                // 3 extra attempts: new player, alert, chat
                +testBooleanOn(ReceiveChatMessageEvent, 4u) { (text) ->
                    welcomeRegex.matchesUnstyled(text)
                }
                // 1 extra attempt: alert
                +testOn(ReceiveChatMessageEvent, 2u) { (text) ->
                    patchRegex.matchEntireUnstyled(text)?.groupValues?.get(1)
                }
            }

            coroutineScope {
                // TODO: this nuance should be strengthened/abstracted somehow (race condition?)
                // so the test starts before the tip message is processed
                val canTip = async(RenderThreadContext) {
                    testBy(TipMessage, null).value?.canTip ?: false
                }

                val request = HideableStateRequest(mc.player!!.username, true)
                val message = +awaitBy(LocateMessage, request)
                JoinDFInfo(message.state.node, patch, canTip.await())
            }
        }
    })

data class JoinDFInfo(val node: Node, val patch: String, val canTip: Boolean)

object ReceiveChatMessageEvent :
    SimpleValidatedEvent<Component> by createValidatedEvent()

object SendCommandEvent :
    SimpleValidatedEvent<String> by createValidatedEvent()