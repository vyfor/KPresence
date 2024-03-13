@file:Suppress("ArrayInDataClass", "unused")

package io.github.reblast.kpresence.rpc

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Represents a user's activity on Discord.
 * @property type Activity type.
 * @property url Stream URL, is validated when type is [ActivityType.STREAMING].
 * @property timestamps Unix timestamps for start and/or end of the game.
 * @property applicationId Application ID for the game.
 * @property details What the player is currently doing.
 * @property state User's current party status, or text used for a custom status.
 * @property emoji Emoji used for a custom status, is validated when type is [ActivityType.CUSTOM].
 * @property party Information for the current party of the player.
 * @property assets Images for the presence and their hover texts.
 * @property secrets Secrets for Rich Presence joining and spectating.
 * @property instance Whether the activity is an instanced game session.
 * @property flags Activity flags.
 * @property buttons Custom buttons shown in the Rich Presence (max 2).
 */
@Serializable
data class Activity(
  val type: ActivityType = ActivityType.GAME,
  val url: String? = null,
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

/**
 * Represents the type of activity.
 * The [ActivityType.STREAMING] type currently only supports Twitch and YouTube. Only https://twitch.tv/ and https://youtube.com/ urls will work.
 * If set to [ActivityType.CUSTOM], emoji and state can be used for setting custom status.
 */
@Serializable(ActivityTypeSerializer::class)
enum class ActivityType {
  GAME,
  STREAMING,
  LISTENING,
  WATCHING,
  CUSTOM,
  COMPETING
}

/**
 * Represents the timestamps for an activity.
 * @property start Unix time (in milliseconds) of when the activity started.
 * @property end Unix time (in milliseconds) of when the activity ends.
 */
@Serializable
data class ActivityTimestamps(
  val start: Int? = null,
  val end: Int? = null
)

/**
 * Represents the emoji for an activity.
 * @property name Name of the emoji.
 * @property id ID of the emoji.
 * @property animated Whether the emoji is animated.
 */
@Serializable
data class ActivityEmoji(
  val name: String,
  val id: Long? = null,
  val animated: Boolean = false
)

/**
 * Represents the party for an activity.
 * @property id ID of the party.
 * @property size Used to show the party's current and maximum size.
 */
@Serializable
data class ActivityParty(
  val id: String? = null,
  val size: IntArray? = null
)

/**
 * Represents the assets for an activity.
 * @property largeImage ID of the large image, or a URL.
 * @property largeText Text displayed when hovering over the large image of the activity.
 * @property smallImage ID of the small image, or a URL.
 * @property smallText Text displayed when hovering over the small image of the activity.
 */
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

/**
 * Represents the secrets for an activity.
 * @property join Secret for joining a party.
 * @property spectate Secret for spectating a game.
 * @property match Secret for a specific instanced match.
 */
@Serializable
data class ActivitySecrets(
  val join: String? = null,
  val spectate: String? = null,
  val match: String? = null
)

/**
 * Represents the flags for an activity.
 * @property value The value of the flag.
 */
enum class ActivityFlags(private val value: UInt) {
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

/**
 * Represents a button for an activity.
 * @property label Text shown on the button (1-32 characters).
 * @property url URL opened when clicking the button (1-512 characters).
 */
@Serializable
data class ActivityButton(
  val label: String,
  val url: String
)

/**
 * Serializer for the ActivityType enum.
 */
private object ActivityTypeSerializer : KSerializer<ActivityType> {
  override val descriptor = PrimitiveSerialDescriptor("me.blast.ActivityType", PrimitiveKind.INT)
  
  override fun serialize(encoder: Encoder, value: ActivityType) {
    encoder.encodeInt(value.ordinal)
  }
  
  override fun deserialize(decoder: Decoder): ActivityType {
    return ActivityType.entries[decoder.decodeInt()]
  }
}
