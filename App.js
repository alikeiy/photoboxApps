import * as React from 'react';
import {NavigationContainer} from '@react-navigation/native';
import {createNativeStackNavigator} from '@react-navigation/native-stack';

import CameraScreen from '../photoboxApps/component/CameraScreen';
import PreviewScreen from '../photoboxApps/component/PreviewScreen';

const Stack = createNativeStackNavigator();

export default function App() {
  return (
    <NavigationContainer>
      <Stack.Navigator initialRouteName="Camera" screenOptions={{headerShown: false}}>
        <Stack.Screen name="Camera" component={CameraScreen} />
        <Stack.Screen name="PreviewScreen" component={PreviewScreen} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
