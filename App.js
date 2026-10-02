import * as React from 'react';
import {
  NavigationContainer,
  createNavigationContainerRef,
} from '@react-navigation/native';
import {createNativeStackNavigator} from '@react-navigation/native-stack';

import TriggerSetupScreen from './component/TriggerSetupScreen';
import FrameSelectScreen from './component/FrameSelectScreen';
import BoothShareScreen from './component/BoothShareScreen';
import PhotoImportBridge from './src/booth/PhotoImportBridge';

export const navigationRef = createNavigationContainerRef();

const Stack = createNativeStackNavigator();

export default function App() {
  return (
    <>
      <PhotoImportBridge navigationRef={navigationRef} />
      <NavigationContainer ref={navigationRef}>
        <Stack.Navigator
          initialRouteName="TriggerSetup"
          screenOptions={{headerShown: false, contentStyle: {backgroundColor: '#FFF0F5'}}}>
          <Stack.Screen name="TriggerSetup" component={TriggerSetupScreen} />
          <Stack.Screen name="FrameSelect" component={FrameSelectScreen} />
          <Stack.Screen name="BoothShare" component={BoothShareScreen} />
        </Stack.Navigator>
      </NavigationContainer>
    </>
  );
}
