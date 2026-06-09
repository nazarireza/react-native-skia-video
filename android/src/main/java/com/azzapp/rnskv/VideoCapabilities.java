package com.azzapp.rnskv;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.util.Range;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class VideoCapabilities {

    public static class DecoderInfo {

        private int maxInstances;
        private int maxWidth;
        private int maxHeight;

        public DecoderInfo(int maxInstances, int maxWidth, int maxHeight) {
            this.maxInstances = maxInstances;
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
        }

        public int getMaxInstances() {
            return maxInstances;
        }

        public int getMaxWidth() {
            return maxWidth;
        }

        public int getMaxHeight() {
            return maxHeight;
        }
    }

    public static DecoderInfo getDecodingCapabilitiesFor(String mimeType) {
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] codecInfos = codecList.getCodecInfos();

        for (MediaCodecInfo codecInfo : codecInfos) {
            if (!codecInfo.isEncoder()) {
                continue;
            }
            MediaCodecInfo.CodecCapabilities capabilities;
            try {
                capabilities = codecInfo.getCapabilitiesForType(mimeType);
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (capabilities != null) {
                MediaCodecInfo.VideoCapabilities videoCapabilities = capabilities.getVideoCapabilities();
                if (videoCapabilities != null) {
                    int maxInstances = capabilities.getMaxSupportedInstances();
                    int maxWidth = videoCapabilities.getSupportedWidths().getUpper();
                    int maxHeight = videoCapabilities.getSupportedHeights().getUpper();
                    return new DecoderInfo(maxInstances, maxWidth, maxHeight);
                }
            }
        }
        return null;
    }

    public static class EncoderInfo {

        private final String encoderName;

        private final boolean hardwareAccelerated;

        private final int width;

        private final int height;

        private final int frameRate;

        private final int bitrate;

        public EncoderInfo(
                String encoderName,
                boolean hardWareAccelerated,
                int width,
                int height,
                int frameRate,
                int bitrate
        ) {
            this.encoderName = encoderName;
            this.hardwareAccelerated = hardWareAccelerated;
            this.width = width;
            this.height = height;
            this.frameRate = frameRate;
            this.bitrate = bitrate;
        }

        public String getEncoderName() {
            return encoderName;
        }

        public boolean getHardwareAccelerated() {
            return hardwareAccelerated;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getFrameRate() {
            return frameRate;
        }

        public int getBitrate() {
            return bitrate;
        }
    }

    public static List<EncoderInfo> getValidEncoderConfigurations(
            int width,
            int height,
            int frameRate,
            int bitRate
    ) {
        List<EncoderInfo> encoderInfos = new ArrayList<>();

        boolean rotated = height > width;

        List<MediaCodecInfoWithOverrides> mediaCodecInfoWithOverrides
                = getPotentialEncoders(VideoEncoder.MIME_TYPE,
                        rotated ? height : width, rotated ? width : height,
                        frameRate, bitRate);

        for (MediaCodecInfoWithOverrides codecInfoWithOverrides : mediaCodecInfoWithOverrides) {
            MediaFormat format;
            if (rotated) {
                format = MediaFormat.createVideoFormat(VideoEncoder.MIME_TYPE,
                        codecInfoWithOverrides.height, codecInfoWithOverrides.width);
            } else {
                format = MediaFormat.createVideoFormat(VideoEncoder.MIME_TYPE,
                        codecInfoWithOverrides.width, codecInfoWithOverrides.height);
            }
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, codecInfoWithOverrides.bitrate);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VideoEncoder.DEFAULT_I_FRAME_INTERVAL_SECONDS);

            // While framerate can be marked as supported it can in fact not be the case
            // so we need to create the codec to see if there is no error and progressively decrease the frame rate
            int foundFrameRate = codecInfoWithOverrides.frameRate;
            while (true) {
                MediaCodec encoder = null;
                format.setInteger(MediaFormat.KEY_FRAME_RATE, foundFrameRate);
                try {
                    encoder = MediaCodec.createByCodecName(codecInfoWithOverrides.codecInfo.getName());
                    encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                } catch (Exception e) {
                    if (encoder != null) {
                        encoder = null;
                    }
                }
                if (encoder != null) {
                    encoder.release();
                    encoderInfos.add(new EncoderInfo(
                            codecInfoWithOverrides.codecInfo.getName(),
                            codecInfoWithOverrides.hardwareAccelerated == 1,
                            rotated ? codecInfoWithOverrides.height : codecInfoWithOverrides.width,
                            rotated ? codecInfoWithOverrides.width : codecInfoWithOverrides.height,
                            foundFrameRate,
                            codecInfoWithOverrides.bitrate
                    ));
                    break;
                } else if (foundFrameRate > 45) {
                    foundFrameRate = 45;
                } else if (foundFrameRate > 30) {
                    foundFrameRate = 30;
                } else {
                    break;
                }
            }
        }
        return encoderInfos;
    }

    private record MediaCodecInfoWithOverrides(
            MediaCodecInfo codecInfo,
            int width,
            int height,
            int bitrate,
            int frameRate,
            int hardwareAccelerated,
            int aspectRatioOverride,
            int resolutionOverride,
            int frameRateOverride,
            int bitrateOverride
    ) {
    }

    private static List<MediaCodecInfoWithOverrides> getPotentialEncoders(
            String mimeType, int width, int height, int frameRate, int bitrate) {

        // 1. Initial Input Validation
        if (mimeType == null || mimeType.isEmpty() || width <= 0 || height <= 0 || frameRate <= 0 || bitrate <= 0) {
            Log.e("VideoEncoderSelector", "Invalid encoding parameters provided.");
            return new ArrayList<>();
        }

        MediaCodecList codecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        MediaCodecInfo[] codecInfos = codecList.getCodecInfos();
        List<MediaCodecInfoWithOverrides> validEncoders = new ArrayList<>();

        final float ASPECT_RATIO_TOLERANCE = 0.001f;

        for (MediaCodecInfo codecInfo : codecInfos) {
            if (!codecInfo.isEncoder()) {
                continue;
            }

            MediaCodecInfo.CodecCapabilities capabilities;
            try {
                capabilities = codecInfo.getCapabilitiesForType(mimeType);
            } catch (IllegalArgumentException e) {
                continue; // Codec does not support this mimeType
            }

            MediaCodecInfo.VideoCapabilities videoCapabilities = capabilities.getVideoCapabilities();
            if (videoCapabilities == null) {
                continue;
            }

            // Initialize mutable tracking variables
            int currentWidth = width;
            int currentHeight = height;
            int currentBitrate = bitrate;
            int currentFrameRate = frameRate;
            int resolutionOverride = 0;
            int aspectRatioOverride = 0;
            int frameRateOverride = 0;
            int bitrateOverride = 0;

            // Assuming your isHardwareAccelerated check exists locally
            int hardwareAccelerated = isHardwareAccelerated(codecInfo) ? 1 : 0;

            // 2. Adjust Resolution (Handling interdependent constraints)
            if (!videoCapabilities.isSizeSupported(currentWidth, currentHeight)) {
                resolutionOverride = 1;
                float aspectRatio = (float) currentWidth / currentHeight;

                // Strategy A: Clamp Width, calculate ideal Height
                int resizedWidth = videoCapabilities.getSupportedWidths().clamp(currentWidth);
                int resizedWidthHeight = Math.round(resizedWidth / aspectRatio);
                resizedWidthHeight = resizedWidthHeight - (resizedWidthHeight % 2); // Align to even

                int potentialInvalidHeight = resizedWidthHeight;
                float widthBasedArDiff = Float.MAX_VALUE;
                try {
                    potentialInvalidHeight = videoCapabilities.getSupportedHeightsFor(resizedWidth).clamp(resizedWidthHeight);
                    widthBasedArDiff = Math.abs(aspectRatio - ((float) resizedWidth / potentialInvalidHeight));
                } catch (IllegalArgumentException ignored) {
                    // Width is fully unsupported for height queries
                }

                // Strategy B: Clamp Height, calculate ideal Width
                int resizedHeight = videoCapabilities.getSupportedHeights().clamp(currentHeight);
                int resizedHeightWidth = Math.round(resizedHeight * aspectRatio);
                resizedHeightWidth = resizedHeightWidth - (resizedHeightWidth % 2); // Align to even

                int potentialInvalidWidth = resizedHeightWidth;
                float heightBasedArDiff = Float.MAX_VALUE;
                try {
                    potentialInvalidWidth = videoCapabilities.getSupportedWidthsFor(resizedHeight).clamp(resizedHeightWidth);
                    heightBasedArDiff = Math.abs(aspectRatio - ((float) potentialInvalidWidth / resizedHeight));
                } catch (IllegalArgumentException ignored) {
                    // Height is fully unsupported for width queries
                }

                // Evaluate the best fit
                if (videoCapabilities.isSizeSupported(resizedWidth, resizedWidthHeight)) {
                    currentWidth = resizedWidth;
                    currentHeight = resizedWidthHeight;
                } else if (videoCapabilities.isSizeSupported(resizedHeightWidth, resizedHeight)) {
                    currentWidth = resizedHeightWidth;
                    currentHeight = resizedHeight;
                } else if (widthBasedArDiff < ASPECT_RATIO_TOLERANCE || heightBasedArDiff < ASPECT_RATIO_TOLERANCE) {
                    aspectRatioOverride = 1;
                    if (widthBasedArDiff < heightBasedArDiff) {
                        currentWidth = resizedWidth;
                        currentHeight = potentialInvalidHeight;
                    } else {
                        currentWidth = potentialInvalidWidth;
                        currentHeight = resizedHeight;
                    }
                } else {
                    continue; // No valid resolution combination exists, skip this codec
                }
            }

            // 3. Adjust Frame Rate
            if (!videoCapabilities.areSizeAndRateSupported(currentWidth, currentHeight, currentFrameRate)) {
                try {
                    Range<Double> frameRateRange = videoCapabilities.getSupportedFrameRatesFor(currentWidth, currentHeight);
                    currentFrameRate = (int) Math.floor(frameRateRange.clamp((double) currentFrameRate));
                    frameRateOverride = 1;
                } catch (IllegalArgumentException ignored) {
                    // Safety net if size/rate query fails despite our previous checks
                }
            }

            // 4. Adjust Bitrate
            Range<Integer> bitrateRange = videoCapabilities.getBitrateRange();
            if (!bitrateRange.contains(currentBitrate)) {
                currentBitrate = bitrateRange.clamp(currentBitrate);
                bitrateOverride = 1;
            }

            // 5. Add to our valid list
            validEncoders.add(new MediaCodecInfoWithOverrides(
                    codecInfo,
                    currentWidth,
                    currentHeight,
                    currentBitrate,
                    currentFrameRate,
                    hardwareAccelerated,
                    aspectRatioOverride,
                    resolutionOverride,
                    frameRateOverride,
                    bitrateOverride
            ));
        }

        // 6. Sort results based on the least number of overrides required
        validEncoders.sort(Comparator
                .comparingInt(MediaCodecInfoWithOverrides::aspectRatioOverride)
                .thenComparingInt(MediaCodecInfoWithOverrides::hardwareAccelerated).reversed()
                .thenComparingInt(MediaCodecInfoWithOverrides::resolutionOverride)
                .thenComparingInt(MediaCodecInfoWithOverrides::frameRateOverride)
                .thenComparingInt(MediaCodecInfoWithOverrides::bitrateOverride));

        return validEncoders;
    }

    private static boolean isHardwareAccelerated(MediaCodecInfo codecInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return codecInfo.isHardwareAccelerated();
        } else {
            // Use heuristics for older Android versions
            String name = codecInfo.getName().toLowerCase();
            return name.contains("qcom") || name.contains("intel") || name.contains("nvidia") || name.contains("mtk");
        }
    }
}
