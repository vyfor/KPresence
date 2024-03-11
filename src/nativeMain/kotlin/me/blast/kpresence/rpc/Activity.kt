@file:Suppress("ArrayInDataClass", "unused")

package me.blast.kpresence.rpc

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import me.blast.kpresence.epochMillis

@Serializable
data class Activity(
  // https://github.com/discord/discord-rpc/issues/375
  // val name: String? = null,
  val type: ActivityType = ActivityType.GAME,
  val url: String? = null,
  @SerialName("created_at")
  val createdAt: Int = epochMillis(),
  val timestamps: ActivityTimestamps? = null,
  @SerialName("application_id")
  val applicationId: Long? = null,
  val details: String? = null,
  val state: String? = null,
  val emoji: ActivityEmoji? = null,
  val party: ActivityParty? = null,
  val assets: ActivityAssets? = null,
  val secrets: ActivitySecrets? = null,
  val instance: Boolean? = null,
  val flags: UInt? = null,
  val buttons: Array<ActivityButton>? = null
)

@Serializable(ActivityTypeSerializer::class)
enum class ActivityType {
  GAME,
  STREAMING,
  LISTENING,
  WATCHING,
  CUSTOM,
  COMPETING
}

@Serializable
data class ActivityTimestamps(
  val start: Long? = null,
  val end: Long? = null
)

@Serializable
data class ActivityEmoji(
  val name: String,
  val id: Long? = null,
  val animated: Boolean = false
)

@Serializable
data class ActivityParty(
  val id: String? = null,
  val size: IntArray? = null
)

@Serializable
data class ActivityAssets(
  @SerialName("large_image")
  val largeImage: String? = null,
  @SerialName("large_text")
  val largeText: String? = null,
  @SerialName("small_image")
  val smallImage: String? = null,
  @SerialName("small_text")
  val smallText: String? = null
)

@Serializable
data class ActivitySecrets(
  val join: String? = null,
  val spectate: String? = null,
  val match: String? = null
)

enum class ActivityFlags(val value: UInt) {
  INSTANCE(1u),
  JOIN(1u shl 1),
  SPECTATE(1u shl 2),
  JOIN_REQUEST(1u shl 3),
  SYNC(1u shl 4),
  PLAY(1u shl 5),
  PARTY_PRIVACY_FRIENDS(1u shl 6),
  PARTY_PRIVACY_VOICE_CHANNEL(1u shl 7),
  EMBEDDED(1u shl 8)
}


@Serializable
data class ActivityButton(
  val label: String,
  val url: String
)

private object ActivityTypeSerializer : KSerializer<ActivityType> {
  override val descriptor = PrimitiveSerialDescriptor("me.blast.ActivityType", PrimitiveKind.INT)
  
  override fun serialize(encoder: Encoder, value: ActivityType) {
    encoder.encodeInt(value.ordinal)
  }
  
  override fun deserialize(decoder: Decoder): ActivityType {
    return ActivityType.entries[decoder.decodeInt()]
  }
}
