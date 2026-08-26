"use strict";

import RNSkiaVideoModule from "./RNSkiaVideoModule.js";
import { Platform } from 'react-native';
export { RNSkiaVideoModule as __RNSkiaVideoPrivateAPI };
export * from "./types.js";
export * from "./videoPlayer.js";
export * from "./videoCompositionPlayer.js";
export * from "./exportVideoComposition.js";
export const getValidEncoderConfigurations = (...args) => {
  if (Platform.OS === 'android') {
    return RNSkiaVideoModule.getValidEncoderConfigurations(...args);
  } else {
    throw new Error('getValidEncoderConfigurations is only available on Android');
  }
};
export const getDecodingCapabilitiesFor = (...args) => {
  if (Platform.OS === 'android') {
    return RNSkiaVideoModule.getDecodingCapabilitiesFor(...args);
  } else {
    throw new Error('getDecodingCapabilitiesFor is only available on Android');
  }
};
//# sourceMappingURL=index.js.map