@file:Suppress("ArrayInDataClass", "unused")

package io.github.reblast.kpresence.rpc

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import io.github.reblast.kpresence.rpc.ActivityType

/**
 * Represents a user's activity on Discord.
 * Most fields have a maximum length of 128 characters.
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
  val timestamps: ActivityTimestamps? = null,
  val details: String? = null,
  val state: String? = null,
  val party: ActivityParty? = null,
  val assets: ActivityAssets? = null,
  val secrets: ActivitySecrets? = null,
  val instance: Boolean? = null,
  val buttons: Array<ActivityButton>? = null
) {
  init {
    require(details == null || details.length in 2..128) { "Details must be between 2 and 128 characters." }
    require(state == null || state.length in 2..128) { "State must be between 2 and 128 characters." }
    require(buttons == null || buttons.size <= 2) { "Buttons have a maximum size of 2, received ${buttons?.size}." }
  }
}

/**
 * Represents the type of activity.
 */
@Serializable(ActivityTypeSerializer::class)
enum class ActivityType(val value: Short) {
  GAME(0),
  LISTENING(2),
  WATCHING(3),
}

/**
 * Represents the timestamps for an activity.
 * @property start Unix time (in milliseconds) of when the activity started.
 * @property end Unix time (in milliseconds) of when the activity ends.
 */
@Serializable
data class ActivityTimestamps(
  val start: Long? = null,
  val end: Long? = null
) {
  init {
    require(!(start != null && end != null)) { "Only one of start or end timestamps should be provided, not both." }
  }
}

/**
 * Represents the party for an activity.
 * @property id ID of the party.
 * @property size Used to show the party's current and maximum size up to 5.
 */
@Serializable
data class ActivityParty(
  val id: String? = null,
  val size: IntArray? = null
) {
  init {
    require(id == null || id.length in 2..128) { "ID must be between 2 and 128 characters." }
    require(size == null || size.size == 2) { "Size must be an array of 2 integers." }
    require(size == null || size.all { it in 0..5 } && size[0] <= size[1]) { "Size must be an array of 2 integers between 0 and 5, with the former not exceeding the latter." }
  }
}

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
) {
  init {
    require(largeImage == null || largeImage.length in 2..256) { "Large image must be between 2 and 256 characters." }
    require(largeText == null || largeText.length in 2..128) { "Large text must be between 2 and 128 characters." }
    require(smallImage == null || smallImage.length in 2..256) { "Small image must be between 2 and 256 characters." }
    require(smallText == null || smallText.length in 2..128) { "Small text must be between 2 and 128 characters." }
  }
}

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
) {
  init {
    require(join == null || join.length in 2..128) { "Join secret must be between 2 and 128 characters." }
    require(spectate == null || spectate.length in 2..128) { "Spectate secret must be between 2 and 128 characters." }
    require(match == null || match.length in 2..128) { "Match secret must be between 2 and 128 characters." }
  }
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
) {
  init {
    require(label.length in 2..32) { "Label must be between 2 and 32 characters." }
    require(url.length in 2..512) { "URL must be between 2 and 512 characters." }
  }
}

/**
 * Serializer for the ActivityType enum.
 */
private object ActivityTypeSerializer : KSerializer<ActivityType> {
  override val descriptor = PrimitiveSerialDescriptor("ActivityType", PrimitiveKind.SHORT)
  
  override fun serialize(encoder: Encoder, value: ActivityType) {
    encoder.encodeShort(value.value)
  }
  
  override fun deserialize(decoder: Decoder): ActivityType {
    return ActivityType.values().first { it.value == decoder.decodeShort() }
  }
}
