/*
 * Copyright (C) 2024 pedroSG94.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pedro.udp

import android.util.Log
import com.pedro.common.AudioCodec
import com.pedro.common.ConnectChecker
import com.pedro.common.base.BaseSender
import com.pedro.common.frame.MediaFrame
import com.pedro.common.onMainThread
import com.pedro.common.validMessage
import com.pedro.srt.mpeg2ts.MpegTsPacket
import com.pedro.srt.mpeg2ts.MpegTsPacketizer
import com.pedro.srt.mpeg2ts.Pid
import com.pedro.srt.mpeg2ts.packets.AacPacket
import com.pedro.srt.mpeg2ts.packets.BasePacket
import com.pedro.srt.mpeg2ts.packets.H26XPacket
import com.pedro.srt.mpeg2ts.packets.OpusPacket
import com.pedro.srt.mpeg2ts.psi.PsiManager
import com.pedro.srt.mpeg2ts.service.Mpeg2TsService
import com.pedro.srt.utils.Constants
import com.pedro.srt.utils.toCodec
import com.pedro.udp.utils.UdpSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

/**
 * Created by pedro on 6/3/24.
 */
class UdpSender(
  connectChecker: ConnectChecker,
  private val commandManager: CommandManager
): BaseSender(connectChecker, "SrtSender") {

  private val service = Mpeg2TsService()
  private val psiManager = PsiManager(service).apply {
    upgradePatVersion()
    upgradeSdtVersion()
  }
  private val limitSize = Constants.MTU
  private val mpegTsPacketizer = MpegTsPacketizer(psiManager)
  private var audioPacket: BasePacket = AacPacket(limitSize, psiManager)
  private val videoPacket = H26XPacket(limitSize, psiManager)
  var socket: UdpSocket? = null

  private fun setTrackConfig(videoEnabled: Boolean, audioEnabled: Boolean) {
    Pid.reset()
    service.clearTracks()
    if (audioEnabled) service.addTrack(commandManager.audioCodec.toCodec())
    if (videoEnabled) service.addTrack(commandManager.videoCodec.toCodec())
    service.generatePmt()
    psiManager.updateService(service)
  }

  override fun setVideoInfo(sps: ByteBuffer, pps: ByteBuffer?, vps: ByteBuffer?) {
    videoPacket.setVideoCodec(commandManager.videoCodec.toCodec())
    videoPacket.sendVideoInfo(sps, pps, vps)
  }

  override fun setAudioInfo(sampleRate: Int, isStereo: Boolean) {
    audioPacket = when (commandManager.audioCodec) {
      AudioCodec.AAC -> AacPacket(limitSize, psiManager).apply { sendAudioInfo(sampleRate, isStereo) }
      AudioCodec.OPUS -> OpusPacket(limitSize, psiManager)
      AudioCodec.G711 -> {
        throw IllegalArgumentException("Unsupported codec: ${commandManager.audioCodec.name}")
      }
    }
  }

  override suspend fun onRun() = withContext(Dispatchers.IO) {
    setTrackConfig(!commandManager.videoDisabled, !commandManager.audioDisabled)
    var bytesSend = 0L
    val bitrateTask = async {
      while (scope.isActive && running) {
        //bytes to bits
        bitrateManager.calculateBitrate(bytesSend * 8)
        bytesSend = 0
        delay(timeMillis = 1000)
      }
    }
    while (scope.isActive && running) {
      val error = runCatching {
        val mediaFrame = runInterruptible { queue.poll(1, TimeUnit.SECONDS) }
        getMpegTsPackets(mediaFrame) { mpegTsPackets ->
          val isKey = mpegTsPackets[0].isKey
          val psiPackets = psiManager.checkSendInfo(isKey, mpegTsPacketizer)
          bytesSend += sendPackets(psiPackets)
          bytesSend += sendPackets(mpegTsPackets)
        }
      }.exceptionOrNull()
      if (error != null) {
        onMainThread {
          connectChecker.onConnectionFailed("Error send packet, ${error.validMessage()}")
        }
        Log.e(TAG, "send error: ", error)
        return@withContext
      }
    }
  }

  override suspend fun stopImp(clear: Boolean) {
    psiManager.reset()
    service.clear()
    mpegTsPacketizer.reset()
    audioPacket.reset(clear)
    videoPacket.reset(clear)
  }

  private suspend fun sendPackets(packets: List<MpegTsPacket>): Long {
    var bytesSend = 0L
    packets.forEach { mpegTsPacket ->
      var size = 0
      size += commandManager.writeData(mpegTsPacket, socket)
      if (isEnableLogs) {
        Log.i(TAG, "wrote ${mpegTsPacket.type.name} packet, size $size")
      }
      bytesSend += size
    }
    return bytesSend
  }

  private suspend fun getMpegTsPackets(mediaFrame: MediaFrame?, callback: suspend (List<MpegTsPacket>) -> Unit) {
    if (mediaFrame == null) return
    when (mediaFrame.type) {
      MediaFrame.Type.VIDEO -> {
        videoPacket.createAndSendPacket(mediaFrame.data, mediaFrame.info) { packets ->
          callback(packets)
        }
      }
      MediaFrame.Type.AUDIO -> {
        audioPacket.createAndSendPacket(mediaFrame.data, mediaFrame.info) { packets ->
          callback(packets)
        }
      }
    }
  }
}