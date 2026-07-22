/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.vision;

import android.util.SparseArray;

import androidx.annotation.GuardedBy;

import org.microg.gms.common.PublicApi;

/**
 * Detector is the base class for implementing specific detector instances, such as a barcode detector or face detector. A detector receives a Frame as input, and produces a number of detected items as output. The Detector implementation is generic, parameterized by T, the type of the detected items.
 */
@PublicApi
public abstract class Detector<T> {
    private final Object processorLock = new Object();
    @GuardedBy("processorLock")
    private Processor<T> processor;

    /**
     * Analyzes the supplied frame to find target item instances (e.g., the face detector finds faces). Subclasses implement this method for calling specific detection code, returning result objects with associated tracking ID mappings.
     *
     * @return mapping of int to detected object, where the int domain represents the ID of the associated item. If tracking is enabled, as the same object is detected in consecutive frames, the detector will return the same ID for that item.
     */
    public abstract SparseArray<T> detect(Frame frame);

    /**
     * Indicates whether the detector has all of the required dependencies available locally in order to do detection.
     * <p>
     * When an app is first installed, it may be necessary to download required files. If this returns false, those files are not yet available. Usually this download is taken care of at application install time, but this is not guaranteed. In some cases the download may have been delayed.
     * <p>
     * If your code has added a processor, an indication of the detector operational state is also indicated with the {@link Detector.Detections#detectorIsOperational()} method. You can check this in your app as it processes detection results, and can convey this state to the user if appropriate.
     *
     * @return true if the detector is operational, false if the dependency download is in progress
     */
    public boolean isOperational() {
        return true;
    }

    /**
     * Pipeline method (see class level documentation above) for receiving frames for detection. Detection results are forwarded onto a processor that was previously registered with this class (see {@link #setProcessor(Detector.Processor)}).
     * <p>
     * Alternatively, if you are just looking to synchronously run the detector on a single frame, use {@link #detect(Frame)} instead.
     */
    public void receiveFrame(Frame frame) {
        Detections<T> detections = new Detections<>(detect(frame), frame.getMetadata().withRotationAppliedToSize(), isOperational());
        synchronized (processorLock) {
            if (processor == null) {
                throw new IllegalStateException("Detector processor must first be set with setProcessor in order to receive detection results.");
            } else {
                processor.receiveDetections(detections);
            }
        }
    }

    /**
     * Shuts down the detector, releasing any underlying resources.
     */
    public void release() {
        synchronized (processorLock) {
            if (processor != null) {
                processor.release();
                processor = null;
            }
        }
    }

    /**
     * Sets the ID of the detected item in which to exclusively track in future use of the detector. This can be used to avoid unnecessary work in detecting all items in future frames, when it's only necessary to receive results for a specific item. After setting this ID, the detector may only return results for the associated tracked item. When that item is no longer present in a frame, the detector will revert back to detecting all items.
     * <p>
     * Optionally, subclasses may override this to support optimized tracking.
     *
     * @param id tracking ID to become the focus for future detections. This is a mapping ID as returned from {@link #detect(Frame)} or received from {@link Detector.Detections#getDetectedItems()}.
     */
    public boolean setFocus(int id) {
        return true;
    }

    /**
     * Pipeline method (see class level documentation above) which sets the {@link Detector.Processor} instance.
     * This is used in creating the pipeline structure, associating a post-processor with the detector.
     */
    public void setProcessor(Processor<T> processor) {
        synchronized (processorLock) {
            if (this.processor != null) {
                this.processor.release();
            }
            this.processor = processor;
        }
    }

    /**
     * Detection result object containing both detected items and the associated frame metadata.
     */
    public static class Detections<T> {
        private final SparseArray<T> detectedItems;
        private final Frame.Metadata frameMetadata;
        private final boolean isOperational;

        @PublicApi(exclude = true)
        public Detections(SparseArray<T> detectedItems, Frame.Metadata frameMetadata, boolean isOperational) {
            this.isOperational = isOperational;
            this.detectedItems = detectedItems;
            this.frameMetadata = frameMetadata;
        }

        /**
         * Returns true if the detector is operational, false if it is not operational. In the non-operational case, the detector will return no results.
         * <p>
         * A detector may be non-operational for a while when starting an app for the first time, if a download is required to obtain the associated library and model files required to do detection.
         */
        public boolean detectorIsOperational() {
            return isOperational;
        }

        /**
         * Returns a collection of the detected items that were identified in the frame.
         *
         * @return mapping of int to detected object, where the int domain represents the consistent tracking ID of the associated item. As the same object is detected in consecutive frames, the detector will return the same ID for that item.
         */
        public SparseArray<T> getDetectedItems() {
            return detectedItems;
        }

        /**
         * Returns the metadata of the associated frame in which the detection originated.
         */
        public Frame.Metadata getFrameMetadata() {
            return frameMetadata;
        }
    }

    /**
     * Interface for defining a post-processing action to be executed for each detection, when using the detector as part of a pipeline (see the class level docs above). An instance of a processor is associated with the detector via the {@link Detector#setProcessor(Detector.Processor)} method.
     */
    public interface Processor<T> {
        /**
         * Called by the detector to deliver detection results to the processor.
         */
        void receiveDetections(Detections<T> detections);

        /**
         * Shuts down and releases associated processor resources.
         */
        void release();
    }
}
