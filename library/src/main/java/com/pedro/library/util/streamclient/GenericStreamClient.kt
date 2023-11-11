package com.pedro.library.util.streamclient

import com.pedro.rtmp.flv.video.ProfileIop
import com.pedro.rtmp.rtmp.RtmpClient
import com.pedro.rtsp.rtsp.RtspClient
import com.pedro.srt.srt.SrtClient

/**
 * Created by pedro on 12/10/23.
 */
class GenericStreamClient(
  private val rtmpClient: RtmpStreamClient,
  private val rtspClient: RtspStreamClient,
  private val srtClient: SrtStreamClient,
  streamClientListener: StreamClientListener?
): StreamBaseClient(streamClientListener) {

  private var connectedStreamClient : StreamBaseClient? = null
  /**
   * H264 profile.
   *
   * @param profileIop Could be ProfileIop.BASELINE or ProfileIop.CONSTRAINED
   */
  fun setProfileIop(profileIop: ProfileIop?) {
    rtmpClient.setProfileIop(profileIop!!)
  }

  /**
   * Some Livestream hosts use Akamai auth that requires RTMP packets to be sent with increasing
   * timestamp order regardless of packet type.
   * Necessary with Servers like Dacast.
   * More info here:
   * https://learn.akamai.com/en-us/webhelp/media-services-live/media-services-live-encoder-compatibility-testing-and-qualification-guide-v4.0/GUID-F941C88B-9128-4BF4-A81B-C2E5CFD35BBF.html
   */
  fun forceAkamaiTs(enabled: Boolean) {
    rtmpClient.forceAkamaiTs(enabled)
  }

  /**
   * Must be called before start stream or will be ignored.
   *
   * Default value 128
   * Range value: 1 to 16777215.
   *
   * The most common values example: 128, 4096, 65535
   *
   * @param chunkSize packet's chunk size send to server
   */
  fun setWriteChunkSize(chunkSize: Int) {
    rtmpClient.setWriteChunkSize(chunkSize)
  }

  override fun setAuthorization(user: String?, password: String?) {
    rtmpClient.setAuthorization(user, password)
    rtspClient.setAuthorization(user, password)
    srtClient.setAuthorization(user, password)
  }

  override fun setReTries(reTries: Int) {
    rtmpClient.setReTries(reTries)
    rtspClient.setReTries(reTries)
    srtClient.setReTries(reTries)
  }

  override fun shouldRetry(reason: String): Boolean = connectedStreamClient?.shouldRetry(reason)?:false

  override fun reConnect(delay: Long, backupUrl: String?) {
    connectedStreamClient?.reConnect(delay, backupUrl)
  }

  override fun hasCongestion(percentUsed: Float): Boolean = connectedStreamClient?.hasCongestion(percentUsed)?:false

  override fun setLogs(enabled: Boolean) {
    rtmpClient.setLogs(enabled)
    rtspClient.setLogs(enabled)
    srtClient.setLogs(enabled)
  }

  override fun setCheckServerAlive(enabled: Boolean) {
    rtmpClient.setCheckServerAlive(enabled)
    rtspClient.setCheckServerAlive(enabled)
    srtClient.setCheckServerAlive(enabled)
  }

  override fun resizeCache(newSize: Int) {
    rtmpClient.resizeCache(newSize)
    rtspClient.resizeCache(newSize)
    srtClient.resizeCache(newSize)
  }

  override fun clearCache() {
    rtmpClient.clearCache()
    rtspClient.clearCache()
    srtClient.clearCache()
  }

  override fun getCacheSize(): Int = connectedStreamClient?.getCacheSize()?:0

  override fun getItemsInCache(): Int = connectedStreamClient?.getItemsInCache()?:0

  override fun getSentAudioFrames(): Long = connectedStreamClient?.getSentAudioFrames()?:0

  override fun getSentVideoFrames(): Long = connectedStreamClient?.getSentVideoFrames()?:0

  override fun getDroppedAudioFrames(): Long = connectedStreamClient?.getDroppedAudioFrames()?:0

  override fun getDroppedVideoFrames(): Long = connectedStreamClient?.getDroppedVideoFrames()?:0

  override fun resetSentAudioFrames() {
    rtmpClient.resetSentAudioFrames()
    rtspClient.resetSentAudioFrames()
    srtClient.resetSentAudioFrames()
  }

  override fun resetSentVideoFrames() {
    rtmpClient.resetSentVideoFrames()
    rtspClient.resetSentVideoFrames()
    srtClient.resetSentVideoFrames()
  }

  override fun resetDroppedAudioFrames() {
    rtmpClient.resetDroppedAudioFrames()
  }

  override fun resetDroppedVideoFrames() {
    rtmpClient.resetDroppedVideoFrames()
    rtspClient.resetDroppedVideoFrames()
    srtClient.resetDroppedVideoFrames()
  }

  override fun setOnlyAudio(onlyAudio: Boolean) {
    rtmpClient.setOnlyAudio(onlyAudio)
    rtspClient.setOnlyAudio(onlyAudio)
    srtClient.setOnlyAudio(onlyAudio)
  }

  override fun setOnlyVideo(onlyVideo: Boolean) {
    rtmpClient.setOnlyVideo(onlyVideo)
    rtspClient.setOnlyVideo(onlyVideo)
    srtClient.setOnlyVideo(onlyVideo)
  }

  fun connecting(url: String) {
    connectedStreamClient =
      if (RtmpClient.urlPattern.matcher(url).matches()) {
        rtmpClient
      } else if (RtspClient.urlPattern.matcher(url).matches()) {
        rtspClient
      } else {
        srtClient
      }
  }
}