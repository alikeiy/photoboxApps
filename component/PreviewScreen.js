import React from 'react';
import {View, Image, TouchableOpacity, Text, StyleSheet} from 'react-native';

export default function PreviewScreen({route, navigation}) {
  const {photo} = route.params;

  return (
    <View style={styles.container}>
      <Image source={{uri: 'file://' + photo.path}} style={styles.image} />
      
      <View style={styles.buttons}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.btn}>
          <Text>Ulangi</Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={() => console.log('Cetak foto')} style={styles.btn}>
          <Text>Cetak</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {flex: 1, backgroundColor: 'black'},
  image: {flex: 1, resizeMode: 'contain'},
  buttons: {flexDirection: 'row', justifyContent: 'space-around', padding: 20},
  btn: {backgroundColor: 'white', padding: 15, borderRadius: 10},
});
