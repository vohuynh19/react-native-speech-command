import * as React from 'react';
import { StyleSheet, View, Button, Linking, Platform } from 'react-native';
import SpeechCommand from 'react-native-speech-command';
import { PERMISSIONS, request, RESULTS } from 'react-native-permissions';

export default function App() {
  React.useEffect(() => {
    SpeechCommand.init();

    setTimeout(() => {
      SpeechCommand.addResultListener((result) => {
        console.log('addResultListener', result);
      });

      SpeechCommand.addErrorListener((error) => {
        console.error('addErrorListener', error);
      });
    }, 1000);
  }, []);

  const checkPermission = (callback: any) => {
    const permission =
      Platform.select({
        ios: PERMISSIONS.IOS.MICROPHONE,
        android: PERMISSIONS.ANDROID.RECORD_AUDIO,
      }) || PERMISSIONS.IOS.MICROPHONE;

    request(permission).then((result) => {
      if (result === RESULTS.GRANTED) {
        callback();
      } else {
        console.log('Permission denied');
        Linking.openSettings();
      }
    });
  };

  const handleStart = () => {
    checkPermission(() => SpeechCommand.start());
  };

  const handleStop = () => {
    checkPermission(() => SpeechCommand.stop());
  };

  return (
    <View style={styles.container}>
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
