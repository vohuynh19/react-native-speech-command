# react-native-speech-command

React Native module for speech command classification.

## Installation

```sh
npm install react-native-speech-command  
```
or 
```sh
yarn add react-native-speech-command
```

# Pre installing

## Android

```
 dependencies {
        ...
        classpath("de.undercouch:gradle-download-task:4.1.2")
    }
```

Create download_model.gradle in android/app/ folder
```gradle
task downloadAudioClassifierModel(type: Download) {
    src 'https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/audio_classification/android/lite-model_yamnet_classification_tflite_1.tflite'
    dest project.ext.ASSET_DIR + '/yamnet.tflite'
    overwrite false
}

task downloadSpeechClassifierModel(type: Download) {
    // This model is custom made using Model Maker. A detailed guide can be found here:
    // https://www.tensorflow.org/lite/models/modify/model_maker/speech_recognition
    src 'https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/audio_classification/android/speech_commands.tflite'
    dest project.ext.ASSET_DIR + '/speech.tflite'
    overwrite false
}

preBuild.dependsOn downloadAudioClassifierModel, downloadSpeechClassifierModel
```

Add below lines to android/app/build.gradle
```gradle
apply plugin: 'de.undercouch.download' 

...
...

// import DownloadModels task
project.ext.ASSET_DIR = projectDir.toString() + '/src/main/assets'
project.ext.TEST_ASSET_DIR = projectDir.toString() + '/src/androidTest/assets'
apply from:'download_model.gradle'

```

Add below lines to android/build.gradle

```
minSdkVersion = 23

dependencies {
    ...
    classpath("de.undercouch:gradle-download-task:4.1.2")
}
```

## IOS
Add speech_commands.tflite to xcode project
## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
