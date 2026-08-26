"use strict";

import { Skia } from '@shopify/react-native-skia';
import { useSharedValue, useFrameCallback, runOnUI } from 'react-native-reanimated';
import { useCallback, useEffect, useMemo, useState } from 'react';
import RNSkiaVideoModule from "./RNSkiaVideoModule.js";
import useEventListener from "./utils/useEventListener.js";
import { PixelRatio } from 'react-native';
/**
 * A hook that creates a video composition player.
 */
export const useVideoCompositionPlayer = ({
  composition,
  drawFrame,
  beforeDrawFrame,
  afterDrawFrame,
  width,
  height,
  autoPlay = false,
  isLooping = false,
  onReadyToPlay,
  onComplete,
  onError
}) => {
  const [isErrored, setIsErrored] = useState(false);
  const framesExtractor = useMemo(() => {
    if (composition && !isErrored) {
      return RNSkiaVideoModule.createVideoCompositionFramesExtractor(composition);
    }
    return null;
  }, [isErrored, composition]);
  useEffect(() => {
    runOnUI(() => {
      framesExtractor?.prepare();
    })();
  }, [framesExtractor]);
  const currentFrame = useSharedValue(null);
  useEffect(() => () => {
    currentFrame.value = null;
    framesExtractor?.dispose();
  }, [currentFrame, framesExtractor]);
  const retry = useCallback(() => {
    setIsErrored(false);
  }, []);
  const errorHandler = useCallback(error => {
    onError?.(error, retry);
    setIsErrored(true);
  }, [onError, retry]);
  useEffect(() => {
    if (framesExtractor) {
      framesExtractor.isLooping = isLooping;
    }
  }, [framesExtractor, isLooping]);
  useEventListener(framesExtractor, 'ready', onReadyToPlay);
  useEventListener(framesExtractor, 'complete', onComplete);
  useEventListener(framesExtractor, 'error', errorHandler);
  useEffect(() => {
    if (autoPlay) {
      framesExtractor?.play();
    }
  }, [framesExtractor, autoPlay]);
  const surfaceSharedValue = useSharedValue(null);
  const pixelRatio = PixelRatio.get();
  useFrameCallback(() => {
    'worklet';

    if (!framesExtractor) {
      return;
    }
    let surface = surfaceSharedValue.value;
    if (!surface) {
      surface = Skia.Surface.MakeOffscreen(width * pixelRatio, height * pixelRatio);
      surfaceSharedValue.value = surface;
    }
    if (!surface) {
      console.warn('Failed to create surface');
      return;
    }
    const canvas = surface.getCanvas();
    const context = beforeDrawFrame?.();
    drawFrame({
      canvas,
      context,
      videoComposition: composition,
      currentTime: framesExtractor.currentTime,
      frames: framesExtractor.decodeCompositionFrames(),
      width: width * pixelRatio,
      height: height * pixelRatio
    });
    surface.flush();
    const previousFrame = currentFrame.value;
    try {
      // Recycle the previous SkImage (outputImage) to avoid allocating a new
      // JSI object on every frame.
      const nextFrame = Skia.Image.MakeImageFromNativeTextureUnstable(surface.getNativeTextureUnstable(), width * pixelRatio, height * pixelRatio, false);
      if (nextFrame === previousFrame) {
        // The recycled image keeps the same identity, so listeners (the Skia
        // canvas) must be forced to re-run.
        currentFrame.modify(undefined, true);
      } else {
        currentFrame.value = nextFrame;
      }
    } catch (error) {
      console.warn('Failed to create image from texture', error);
      return;
    }
    afterDrawFrame?.(context);
  }, true);
  return {
    currentFrame,
    player: framesExtractor
  };
};
//# sourceMappingURL=videoCompositionPlayer.js.map