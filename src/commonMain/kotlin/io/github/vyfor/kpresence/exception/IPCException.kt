@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package io.github.vyfor.kpresence.exception

/** Exception thrown when a pipe does not exist. */
class PipeNotFoundException : Exception()

/**
 * Exception thrown when there is a connection issue.
 *
 * @param exception The underlying exception that caused the connection issue.
 */
class ConnectionException(val exception: Exception) : Exception(exception)

/**
 * Exception thrown when a connection is unexpectedly closed.
 *
 * @param message The message indicating why the connection was closed.
 */
class ConnectionClosedException(message: String) : Exception(message)

/**
 * Exception thrown when an invalid client ID is provided.
 *
 * @param message The message containing the invalid client ID.
 */
class InvalidClientIdException(message: String) : Exception(message)

/** Exception thrown when trying to perform an operation without connecting to Discord. */
class NotConnectedException : Exception("The connection has not yet been established")

/**
 * Exception thrown when there is an issue reading from a pipe.
 *
 * @param message The message indicating the specific read issue.
 */
class PipeReadException(message: String) : Exception(message)

/**
 * Exception thrown when there is an issue writing to a pipe.
 *
 * @param message The message indicating the specific write issue.
 */
class PipeWriteException(message: String) : Exception(message)
