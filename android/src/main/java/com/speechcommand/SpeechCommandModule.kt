package com.speechcommand

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments
import com.facebook.react.modules.core.DeviceEventManagerModule

import android.content.Context
import android.media.AudioRecord
import android.os.SystemClock
import android.util.Log
import android.Manifest
import androidx.core.content.ContextCompat
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.support.label.Category

interface AudioClassificationListener {
    fun onError(error: String)
    fun onResult(results: List<Category>, inferenceTime: Long)
}

class SpeechCommandModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  companion object {
    const val DELEGATE_CPU = 0
    const val DELEGATE_NNAPI = 1
    const val DISPLAY_THRESHOLD = 0.3f
    const val DEFAULT_NUM_OF_RESULTS = 2
    const val DEFAULT_NUM_OF_THREADS = 2
    const val DEFAULT_OVERLAP_VALUE = 0.5f
    const val YAMNET_MODEL = "yamnet.tflite"
    const val SPEECH_COMMAND_MODEL = "speech.tflite"
  }

  var currentModel: String = SPEECH_COMMAND_MODEL
  var classificationThreshold: Float = DISPLAY_THRESHOLD
  var overlap: Float = DEFAULT_OVERLAP_VALUE
  var numOfResults: Int = DEFAULT_NUM_OF_RESULTS
  var currentDelegate: Int = DELEGATE_CPU
  var numThreads: Int = DEFAULT_NUM_OF_THREADS

  private lateinit var classifier: AudioClassifier
  private lateinit var tensorAudio: TensorAudio
  private lateinit var recorder: AudioRecord
  private lateinit var executor: ScheduledThreadPoolExecutor

  private val classifyRunnable = Runnable {
    classifyAudio()
  }

  private fun sendEvent(reactContext: ReactContext, eventName: String, params: WritableMap?) {
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, params)
  }

  private val listener = object : AudioClassificationListener {
    override fun onResult(results: List<Category>, inferenceTime: Long) {
      val params = Arguments.createMap()
      val categoriesArray = Arguments.createArray()

      for (category in results) {
        val categoryParams = Arguments.createMap()
        categoryParams.putString("label", category.getLabel())
        categoryParams.putDouble("score", category.getScore().toDouble())
        categoriesArray.pushMap(categoryParams)
      }

      params.putArray("categories", categoriesArray)
      params.putDouble("inferenceTime", inferenceTime.toDouble())
      sendEvent(reactContext, "onResult", params)
    }

    override fun onError(error: String) {
      val params = Arguments.createMap()
      params.putString("error", error)
      sendEvent(reactContext, "onError", params)
    }
  }

  override fun getName(): String {
    return "SpeechCommand"
  }

  @ReactMethod
  fun init() {
    val baseOptionsBuilder = BaseOptions.builder()
        .setNumThreads(numThreads)

    when (currentDelegate) {
        DELEGATE_CPU -> {
            // Default
        }
        DELEGATE_NNAPI -> {
            baseOptionsBuilder.useNnapi()
        }
    }

    val options = AudioClassifier.AudioClassifierOptions.builder()
        .setScoreThreshold(classificationThreshold)
        .setMaxResults(numOfResults)
        .setBaseOptions(baseOptionsBuilder.build())
        .build()

    try {
        classifier = AudioClassifier.createFromFileAndOptions(reactApplicationContext, currentModel, options)
        tensorAudio = classifier.createInputTensorAudio()
        recorder = classifier.createAudioRecord()
    } catch (e: IllegalStateException) {
        listener.onError(
            "Audio Classifier failed to initialize. See error logs for details"
        )
        Log.e("AudioClassification", "TFLite failed to load with error: " + e.message)
    }
  }

  @ReactMethod
  fun start() {
    if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
        return
    }
    recorder.startRecording()
    executor = ScheduledThreadPoolExecutor(1)
    val lengthInMilliSeconds = ((classifier.requiredInputBufferSize * 1.0f) / classifier.requiredTensorAudioFormat.sampleRate) * 1000
    val interval = (lengthInMilliSeconds * (1 - overlap)).toLong()
    executor.scheduleAtFixedRate(classifyRunnable, 0, interval, TimeUnit.MILLISECONDS)
  }

  @ReactMethod
  fun stop() {
    recorder.stop()
    executor.shutdownNow()
  }

  private fun classifyAudio() {
    tensorAudio.load(recorder)
    var inferenceTime = SystemClock.uptimeMillis()
    val output = classifier.classify(tensorAudio)

    inferenceTime = SystemClock.uptimeMillis() - inferenceTime
    listener.onResult(output[0].categories, inferenceTime)
  }
}
