import React, {useEffect, useRef, useState} from 'react';
import {View, Text, TouchableOpacity, StyleSheet} from 'react-native';
import {Camera, useCameraDevices} from 'react-native-vision-camera';
import CameraModule from './CameraModule'; // pastikan path sesuai

// Mock camera untuk development tanpa kamera fisik
const MockCamera = ({isActive, ...props}) => (
  <View style={{flex:1, backgroundColor:'gray', justifyContent:'center', alignItems:'center'}}>
    <Text style={{color:'white'}}>Camera Mock</Text>
  </View>
);

export default function CameraScreen({navigation}) {
  const [hasPermission, setHasPermission] = useState(false);
  const camera = useRef(null);
  const devices = useCameraDevices();
  const device = devices.back;

  useEffect(() => {
    (async () => {
      try {
        const status = await CameraModule.requestCameraPermission();
        setHasPermission(status === 'authorized');
      } catch (e) {
        console.error('Error minta permission kamera', e);
      }
    })();
  }, []);

  const takePhoto = async () => {
    if (camera.current) {
      const photo = await camera.current.takePhoto?.({
        flash: 'off',
      });
      navigation.navigate('PreviewScreen', {photo});
    }
  };

  if (!hasPermission) return <Text>No permission</Text>;

  const CameraComponent = device ? Camera : MockCamera;

  return (
    <View style={styles.container}>
      <CameraComponent
        style={StyleSheet.absoluteFill}
        device={device}
        isActive={true}
        ref={camera}
        photo={true}
      />
      <TouchableOpacity style={styles.captureButton} onPress={takePhoto}>
        <Text style={{color: 'white'}}>📸</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {flex: 1, backgroundColor: 'black'},
  captureButton: {
    position: 'absolute',
    bottom: 40,
    alignSelf: 'center',
    backgroundColor: 'red',
    padding: 20,
    borderRadius: 50,
  },
});
