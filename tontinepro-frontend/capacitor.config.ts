import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId:   'com.tontinepro.app',
  appName: 'TontinePro',

  // Angular 19 application builder → dist/browser/
  webDir: 'dist/tontinepro-frontend/browser',

  server: {
    androidScheme: 'http',
    // Cleartext autorisé pour le réseau LAN (192.168.x.x non-HTTPS)
    cleartext: true,
  },

  android: {
    // Permet l'affichage sous la status bar et la navigation bar
    // (géré via safe-area-inset-* dans le CSS)
    useLegacyBridge: false,
  },

  plugins: {
    SplashScreen: {
      launchShowDuration:        2000,
      launchAutoHide:            true,
      backgroundColor:           '#4f46e5',   // primary-600
      androidSplashResourceName: 'splash',
      androidScaleType:          'CENTER_CROP',
      showSpinner:               false,
    },

    StatusBar: {
      style:           'DARK',
      backgroundColor: '#ffffff',
    },

    Keyboard: {
      resize:              'BODY',
      style:               'DARK',
      resizeOnFullScreen:  true,
    },
  },
};

export default config;
