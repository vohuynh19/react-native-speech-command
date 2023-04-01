import * as React from 'react';

import {
  StyleSheet,
  View,
  Button,
  NativeEventEmitter,
  Linking,
} from 'react-native';
import { getInstance } from 'react-native-speech-command';

import { PERMISSIONS, request, RESULTS } from 'react-native-permissions';

export default function App() {
  React.useEffect(() => {
    const eventEmitter = new NativeEventEmitter(getInstance());

    const subscription = eventEmitter.addListener('onResults', (event) => {
      console.log('event', event);
    });

    const errorSub = eventEmitter.addListener('onError', (event) => {
      console.log('event', event);
    });

    return () => {
      subscription.remove();
      errorSub.remove();
    };
  }, []);

  const handleInit = () => {
    checkPermission(() => getInstance().initialize());
  };

  const handleStart = () => {
    checkPermission(() => getInstance().startClassifier());
  };

  const handleStop = () => {
    checkPermission(() => getInstance().stopClassifier());
  };

  const checkPermission = (callback: any) => {
    request(PERMISSIONS.IOS.MICROPHONE).then((result) => {
      if (result === RESULTS.GRANTED) {
        callback();
      } else {
        console.log('Permission denied');
        Linking.openSettings();
      }
    });
  };

  const handleTest = () => {
    getInstance().test();
  };

  const loopTimer = () => {
    getInstance().loopTimer();
  };

  return (
    <View style={styles.container}>
      <Button title="Test" onPress={handleTest} />
      <Button title="Loop" onPress={loopTimer} />

      <Button title="Init" onPress={handleInit} />
      <Button title="Start" onPress={handleStart} />
      <Button title="Stop" onPress={handleStop} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'white',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
