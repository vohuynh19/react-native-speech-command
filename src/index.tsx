import type { EmitterSubscription } from 'react-native';
import { NativeEventEmitter } from 'react-native';
import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-speech-command' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const SpeechCommand = NativeModules.SpeechCommand
  ? NativeModules.SpeechCommand
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

type Result = {
  inferenceTime: number;
  categories: Category[];
};

type Category = {
  label: string;
  score: number;
};

const eventEmitter = new NativeEventEmitter();

const eventGarbageCollector: EmitterSubscription[] = [];

const Package = {
  init: () =>
    Platform.OS === 'ios' ? SpeechCommand.initialize() : SpeechCommand.init(),
  start: SpeechCommand.start,
  stop: SpeechCommand.stop,
  addResultListener: (listener: (result: Result) => void) => {
    const event = eventEmitter.addListener('onResult', listener);
    eventGarbageCollector.push(event);
  },
  addErrorListener: (listener: (error: string) => void) => {
    const event = eventEmitter.addListener('onError', listener);
    eventGarbageCollector.push(event);
  },
  removeAllListeners: () => {
    eventGarbageCollector.forEach((event) => event.remove());
    eventGarbageCollector.length = 0;
  },
};

export default Package;
