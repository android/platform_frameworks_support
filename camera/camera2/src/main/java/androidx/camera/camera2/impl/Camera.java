/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.camera2.impl;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.WorkerThread;
import androidx.camera.core.BaseCamera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraDeviceStateCallbacks;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.DeferrableSurface;
import androidx.camera.core.ImmediateSurface;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.SessionConfig.ValidatingBuilder;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseAttachState;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A camera which is controlled by the change of state in use cases.
 *
 * <p>The camera needs to be in an open state in order for use cases to control the camera. Whenever
 * there is a non-zero number of use cases in the online state the camera will either have a capture
 * session open or be in the process of opening up one. If the number of uses cases in the online
 * state changes then the capture session will be reconfigured.
 *
 * <p>Capture requests will be issued only for use cases which are in both the online and active
 * state.
 */
final class Camera implements BaseCamera {
    private static final String TAG = "Camera";

    private final Object mAttachedUseCaseLock = new Object();

    /** Map of the use cases to the information on their state. */
    @GuardedBy("mAttachedUseCaseLock")
    private final UseCaseAttachState mUseCaseAttachState;

    /** The identifier for the {@link CameraDevice} */
    private final String mCameraId;

    /** Handle to the camera service. */
    private final CameraManager mCameraManager;

    private final Object mCameraInfoLock = new Object();
    /** The handler for camera callbacks and use case state management calls. */

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Handler mHandler;
    private final Executor mExecutor;

    /**
     * State variable for tracking state of the camera.
     *
     * <p>Is an atomic reference because it is initialized in the constructor which is not called on
     * same thread as any of the other methods and callbacks.
     */
    final AtomicReference<State> mState = new AtomicReference<>(State.UNINITIALIZED);
    /** The camera control shared across all use cases bound to this Camera. */
    private final CameraControl mCameraControl;
    private final StateCallback mStateCallback = new StateCallback();
    /** Information about the characteristics of this camera */
    // Nullable because this is lazily instantiated
    @GuardedBy("mCameraInfoLock")
    @Nullable
    private CameraInfo mCameraInfo;
    /** The handle to the opened camera. */
    @Nullable
    CameraDevice mCameraDevice;
    /** The configured session which handles issuing capture requests. */
    private CaptureSession mCaptureSession;
    /** The session configuration of camera control. */
    private SessionConfig mCameraControlSessionConfig = SessionConfig.defaultEmptySessionConfig();

    private final Object mPendingLock = new Object();
    @GuardedBy("mPendingLock")
    final List<UseCase> mPendingForAddOnline = new ArrayList<>();
    @GuardedBy("mClosedCaptureSessions")
    private List<CaptureSession> mClosedCaptureSessions = new ArrayList<>();

    // Used to debug number of requests to release camera
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final AtomicInteger mReleaseRequestCount = new AtomicInteger(0);
    // Should only be accessed on handler thread
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    ListenableFuture<Void> mUserReleaseFuture;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    CallbackToFutureAdapter.Completer<Void> mUserReleaseNotifier;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Map<CaptureSession, ListenableFuture<Void>> mReleasedCaptureSessions = new HashMap<>();

    /**
     * Constructor for a camera.
     *
     * @param cameraManager the camera service used to retrieve a camera
     * @param cameraId      the name of the camera as defined by the camera service
     * @param handler       the handler for the thread on which all camera operations run
     */
    Camera(CameraManager cameraManager, String cameraId, Handler handler) {
        mCameraManager = cameraManager;
        mCameraId = cameraId;
        mHandler = handler;
        ScheduledExecutorService executorScheduler = CameraXExecutors.newHandlerExecutor(mHandler);
        mExecutor = executorScheduler;
        mUseCaseAttachState = new UseCaseAttachState(cameraId);
        mState.set(State.INITIALIZED);
        mCameraControl = new Camera2CameraControl(this, executorScheduler, executorScheduler);
        mCaptureSession = new CaptureSession(mExecutor);
    }

    /**
     * Open the camera asynchronously.
     *
     * <p>Once the camera has been opened use case state transitions can be used to control the
     * camera pipeline.
     */
    @Override
    public void open() {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera.this.open();
                }
            });
            return;
        }

        switch (mState.get()) {
            case INITIALIZED:
                openCameraDevice();
                break;
            case CLOSING:
                mState.set(State.REOPENING);
                break;
            default:
                Log.d(TAG, "open() ignored due to being in state: " + mState.get());
        }
    }

    /**
     * Close the camera asynchronously.
     *
     * <p>Once the camera is closed the camera will no longer produce data. The camera must be
     * reopened for it to produce data again.
     */
    @Override
    public void close() {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera.this.close();
                }
            });
            return;
        }

        Log.d(TAG, "Closing camera: " + mCameraId);
        switch (mState.get()) {
            case OPENED:
                mState.set(State.CLOSING);
                closeCamera(/*abortInFlightCaptures=*/false);
                break;
            case OPENING:
            case REOPENING:
                mState.set(State.CLOSING);
                break;
            default:
                Log.d(TAG, "close() ignored due to being in state: " + mState.get());
        }
    }

    @WorkerThread
    private void configAndClose() {
        // Configure the camera with a dummy capture session in order to clear the
        // previous session. This should be released immediately after being configured.
        final CaptureSession dummySession = new CaptureSession(mExecutor);

        final SurfaceTexture surfaceTexture = new SurfaceTexture(0);
        surfaceTexture.setDefaultBufferSize(640, 480);
        final Surface surface = new Surface(surfaceTexture);
        final Runnable closeAndCleanupRunner = new Runnable() {
            @Override
            public void run() {
                surface.release();
                surfaceTexture.release();
            }
        };

        SessionConfig.Builder builder = new SessionConfig.Builder();
        builder.addNonRepeatingSurface(new ImmediateSurface(surface));
        builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        try {
            Log.d(TAG, "Start configAndClose.");
            dummySession.open(builder.build(), mCameraDevice);

            // Don't need to abort captures since there are none submitted for this session.
            ListenableFuture<Void> releaseFuture = releaseSession(
                    dummySession, /*abortInFlightCaptures=*/false);

            // Add a listener to clear the dummy surfaces
            releaseFuture.addListener(closeAndCleanupRunner,
                    CameraXExecutors.directExecutor());

        } catch (CameraAccessException e) {
            Log.d(TAG, "Unable to configure camera " + mCameraId + " due to "
                    + e.getMessage());
            closeAndCleanupRunner.run();
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    boolean isSessionCloseComplete() {
        return mReleasedCaptureSessions.isEmpty();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void finishClose() {
        Preconditions.checkState(mState.get() == State.RELEASING || mState.get() == State.CLOSING);
        Preconditions.checkState(mReleasedCaptureSessions.isEmpty());

        mCameraDevice = null;
        if (mState.get() == State.CLOSING) {
            mState.set(State.INITIALIZED);
        } else {
            mState.set(State.RELEASED);

            if (mUserReleaseNotifier != null) {
                mUserReleaseNotifier.set(null);
                mUserReleaseNotifier = null;
            }
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @WorkerThread
    void closeCamera(boolean abortInFlightCaptures) {
        Preconditions.checkState(mState.get() == State.CLOSING || mState.get() == State.RELEASING,
                "closeCamera should only be called in a CLOSING or RELEASING state.");

        boolean isLegacyDevice = false;
        try {
            Camera2CameraInfo camera2CameraInfo = (Camera2CameraInfo) getCameraInfo();
            isLegacyDevice = camera2CameraInfo.getSupportedHardwareLevel()
                    == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
        } catch (CameraInfoUnavailableException e) {
            Log.w(TAG, "Check legacy device failed.", e);
        }

        // TODO: Check if any sessions have been previously configured. We can probably skip
        // configAndClose if there haven't been any sessions configured yet.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M && Build.VERSION.SDK_INT < 29
                && isLegacyDevice) {
            // To configure surface again before close camera. This step would
            // disconnect previous connected surface in some legacy device to prevent exception.
            configAndClose();
        }

        // Release the current session and replace with a new uninitialized session in case the
        // camera enters a REOPENING state during session closing.
        resetCaptureSession(abortInFlightCaptures);
    }

    @WorkerThread
    private ListenableFuture<Void> releaseSession(@NonNull final CaptureSession captureSession,
            boolean abortInFlightCaptures) {
        ListenableFuture<Void> releaseFuture = captureSession.release(abortInFlightCaptures);

        mReleasedCaptureSessions.put(captureSession, releaseFuture);

        // Add a callback to clear the future and notify if the camera and all capture sessions
        // are released
        Futures.addCallback(releaseFuture, new FutureCallback<Void>() {
            @WorkerThread
            @Override
            public void onSuccess(@Nullable Void result) {
                mReleasedCaptureSessions.remove(captureSession);
                switch (mState.get()) {
                    case CLOSING:
                    case RELEASING:
                        Preconditions.checkState(mCameraDevice != null,
                                "Camera Device should not be null while in state " + mState.get());
                        if (isSessionCloseComplete()) {
                            mCameraDevice.close();
                        }
                        break;
                    case REOPENING:
                        // Camera should not have yet been closed, so move back to opened state.
                        Preconditions.checkState(mCameraDevice != null);
                        mState.set(State.OPENED);
                        openCaptureSession();
                        break;
                    default:
                        // Ignore all other states
                }
            }

            @Override
            public void onFailure(Throwable t) {
                // Don't reset the internal release future as we want to keep track of the error
                // TODO: The camera should be put into an error state at this point
            }
            // Should always be called on the same handler thread, so directExecutor is OK here.
        }, CameraXExecutors.directExecutor());

        return releaseFuture;
    }

    /**
     * Release the camera.
     *
     * <p>Once the camera is released it is permanently closed. A new instance must be created to
     * access the camera.
     */
    @Override
    public ListenableFuture<Void> release() {
        ListenableFuture<Void> releaseFuture = CallbackToFutureAdapter.getFuture(
                new CallbackToFutureAdapter.Resolver<Void>() {
                    @Override
                    public Object attachCompleter(
                            @NonNull final CallbackToFutureAdapter.Completer<Void> completer) {

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Futures.propagate(getOrCreateUserReleaseFuture(), completer);
                            }
                        });
                        return "Release[request=" + mReleaseRequestCount.getAndIncrement() + "]";
                    }
                });

        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera.this.releaseInternal();
                }
            });
        } else {
            releaseInternal();
        }

        return releaseFuture;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @WorkerThread
    void releaseInternal() {
        switch (mState.get()) {
            case INITIALIZED:
                mState.set(State.RELEASING);
                Preconditions.checkState(isSessionCloseComplete());
                finishClose();
                break;
            case OPENED:
                mState.set(State.RELEASING);
                closeCamera(/*abortInFlightCaptures=*/true);
                break;
            case OPENING:
            case CLOSING:
            case REOPENING:
            case RELEASING:
                // Wait for the camera async callback to finish releasing
                mState.set(State.RELEASING);
                break;
            default:
                Log.d(TAG, "release() ignored due to being in state: " + mState.get());
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @WorkerThread
    ListenableFuture<Void> getOrCreateUserReleaseFuture() {
        if (mUserReleaseFuture == null) {
            if (mState.get() != State.RELEASED) {
                mUserReleaseFuture = CallbackToFutureAdapter.getFuture(
                        new CallbackToFutureAdapter.Resolver<Void>() {
                            @Override
                            public Object attachCompleter(
                                    @NonNull CallbackToFutureAdapter.Completer<Void> completer) {
                                Preconditions.checkState(mUserReleaseNotifier == null,
                                        "Camera can only be released once, so release completer "
                                                + "should be null on creation.");
                                mUserReleaseNotifier = completer;
                                return "Release[camera=" + Camera.this + "]";
                            }
                        });
            } else {
                // Set to an immediately successful future if already in the released state.
                mUserReleaseFuture = Futures.immediateFuture(null);
            }
        }

        return mUserReleaseFuture;
    }

    /**
     * Sets the use case in a state to issue capture requests.
     *
     * <p>The use case must also be online in order for it to issue capture requests.
     */
    @Override
    public void onUseCaseActive(final UseCase useCase) {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera.this.onUseCaseActive(useCase);
                }
            });
            return;
        }

        Log.d(TAG, "Use case " + useCase + " ACTIVE for camera " + mCameraId);

        synchronized (mAttachedUseCaseLock) {
            reattachUseCaseSurfaces(useCase);
            mUseCaseAttachState.setUseCaseActive(useCase);
        }
        updateCaptureSessionConfig();
    }

    /** Removes the use case from a state of issuing capture requests. */
    @Override
    public void onUseCaseInactive(final UseCase useCase) {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera.this.onUseCaseInactive(useCase);
                }
            });
            return;
        }

        Log.d(TAG, "Use case " + useCase + " INACTIVE for camera " + mCameraId);
        synchronized (mAttachedUseCaseLock) {
            mUseCaseAttachState.setUseCaseInactive(useCase);
        }

        updateCaptureSessionConfig();
    }

    /** Updates the capture requests based on the latest settings. */
    @Override
    public void onUseCaseUpdated(final UseCase useCase) {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera.this.onUseCaseUpdated(useCase);
                }
            });
            return;
        }

        Log.d(TAG, "Use case " + useCase + " UPDATED for camera " + mCameraId);
        synchronized (mAttachedUseCaseLock) {
            reattachUseCaseSurfaces(useCase);
            mUseCaseAttachState.updateUseCase(useCase);
        }

        updateCaptureSessionConfig();
    }

    @Override
    public void onUseCaseReset(final UseCase useCase) {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera.this.onUseCaseReset(useCase);
                }
            });
            return;
        }

        Log.d(TAG, "Use case " + useCase + " RESET for camera " + mCameraId);
        synchronized (mAttachedUseCaseLock) {
            reattachUseCaseSurfaces(useCase);
            mUseCaseAttachState.updateUseCase(useCase);
        }

        resetCaptureSession(/*abortInFlightCaptures=*/false);
        updateCaptureSessionConfig();
        openCaptureSession();
    }

    // Re-attaches use case's surfaces if surfaces are changed when use case is online.
    @GuardedBy("mAttachedUseCaseLock")
    private void reattachUseCaseSurfaces(UseCase useCase) {
        // if use case is offline, then DeferrableSurface attaching will happens when the use
        // case is addOnlineUsecase()'d.   So here we don't need to do the attaching.
        if (!isUseCaseOnline(useCase)) {
            return;
        }
        SessionConfig sessionConfig = mUseCaseAttachState.getUseCaseSessionConfig(useCase);
        SessionConfig newSessionConfig = useCase.getSessionConfig(mCameraId);

        List<DeferrableSurface> currentSurfaces = sessionConfig.getSurfaces();
        List<DeferrableSurface> newSurfaces = newSessionConfig.getSurfaces();

        // New added DeferrableSurfaces need to be attached.
        for (DeferrableSurface newSurface : newSurfaces) {
            if (!currentSurfaces.contains(newSurface)) {
                newSurface.notifySurfaceAttached();
            }
        }

        // Removed DeferrableSurfaces need to be detached.
        for (DeferrableSurface currentSurface : currentSurfaces) {
            if (!newSurfaces.contains(currentSurface)) {
                currentSurface.notifySurfaceDetached();
            }
        }
    }

    void notifyAttachToUseCaseSurfaces(UseCase useCase) {
        for (DeferrableSurface surface : useCase.getSessionConfig(
                mCameraId).getSurfaces()) {
            surface.notifySurfaceAttached();
        }
    }

    void notifyDetachFromUseCaseSurfaces(UseCase useCase) {
        for (DeferrableSurface surface : useCase.getSessionConfig(
                mCameraId).getSurfaces()) {
            surface.notifySurfaceDetached();
        }
    }

    public boolean isUseCaseOnline(UseCase useCase) {
        synchronized (mAttachedUseCaseLock) {
            return mUseCaseAttachState.isUseCaseOnline(useCase);
        }
    }

    /**
     * Sets the use case to be in the state where the capture session will be configured to handle
     * capture requests from the use case.
     */
    @Override
    public void addOnlineUseCase(final Collection<UseCase> useCases) {
        if (useCases.isEmpty()) {
            return;
        }

        // Attaches the surfaces of use case to the Camera (prevent from surface abandon crash)
        // addOnlineUseCase could be called with duplicate use case, so we need to filter out
        // use cases that are either pending for addOnline or are already online.
        // It's ok for two thread to run here, since it‘ll do nothing if use case is already
        // pending.
        synchronized (mPendingLock) {
            for (UseCase useCase : useCases) {
                boolean isOnline = isUseCaseOnline(useCase);
                if (mPendingForAddOnline.contains(useCase) || isOnline) {
                    continue;
                }

                notifyAttachToUseCaseSurfaces(useCase);
                mPendingForAddOnline.add(useCase);
            }
        }

        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera.this.addOnlineUseCase(useCases);
                }
            });
            return;
        }

        Log.d(TAG, "Use cases " + useCases + " ONLINE for camera " + mCameraId);
        synchronized (mAttachedUseCaseLock) {
            for (UseCase useCase : useCases) {
                mUseCaseAttachState.setUseCaseOnline(useCase);
            }
        }

        synchronized (mPendingLock) {
            mPendingForAddOnline.removeAll(useCases);
        }

        open();
        updateCaptureSessionConfig();
        resetCaptureSession(/*abortInFlightCaptures=*/false);

        if (mState.get() == State.OPENED) {
            openCaptureSession();
        }
    }

    /**
     * Removes the use case to be in the state where the capture session will be configured to
     * handle capture requests from the use case.
     */
    @Override
    public void removeOnlineUseCase(final Collection<UseCase> useCases) {
        if (useCases.isEmpty()) {
            return;
        }

        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera.this.removeOnlineUseCase(useCases);
                }
            });
            return;
        }

        Log.d(TAG, "Use cases " + useCases + " OFFLINE for camera " + mCameraId);
        synchronized (mAttachedUseCaseLock) {
            List<UseCase> toDetach = new ArrayList<>();
            for (UseCase useCase : useCases) {
                if (mUseCaseAttachState.isUseCaseOnline(useCase)) {
                    toDetach.add(useCase);
                }
                mUseCaseAttachState.setUseCaseOffline(useCase);
            }

            for (UseCase detach : toDetach) {
                notifyDetachFromUseCaseSurfaces(detach);
            }

            if (mUseCaseAttachState.getOnlineUseCases().isEmpty()) {
                resetCaptureSession(/*abortInFlightCaptures=*/true);
                close();
                return;
            }
        }

        updateCaptureSessionConfig();
        resetCaptureSession(/*abortInFlightCaptures=*/false);

        if (mState.get() == State.OPENED) {
            openCaptureSession();
        }

    }

    /** Returns an interface to retrieve characteristics of the camera. */
    @Override
    public CameraInfo getCameraInfo() throws CameraInfoUnavailableException {
        synchronized (mCameraInfoLock) {
            if (mCameraInfo == null) {
                // Lazily instantiate camera info
                mCameraInfo = new Camera2CameraInfo(mCameraManager, mCameraId);
            }

            return mCameraInfo;
        }
    }

    /** Opens the camera device */
    // TODO(b/124268878): Handle SecurityException and require permission in manifest.
    @SuppressLint("MissingPermission")
    void openCameraDevice() {
        mState.set(State.OPENING);

        Log.d(TAG, "Opening camera: " + mCameraId);

        try {
            mCameraManager.openCamera(mCameraId, createDeviceStateCallback(), mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Unable to open camera " + mCameraId + " due to " + e.getMessage());
            mState.set(State.INITIALIZED);
        }
    }

    /** Updates the capture request configuration for the current capture session. */
    private void updateCaptureSessionConfig() {
        ValidatingBuilder validatingBuilder;
        synchronized (mAttachedUseCaseLock) {
            validatingBuilder = mUseCaseAttachState.getActiveAndOnlineBuilder();
        }

        if (validatingBuilder.isValid()) {
            // Apply CameraControl's SessionConfig to let CameraControl be able to control
            // Repeating Request and process results.
            validatingBuilder.add(mCameraControlSessionConfig);

            SessionConfig sessionConfig = validatingBuilder.build();
            mCaptureSession.setSessionConfig(sessionConfig);
        }
    }

    /**
     * Opens a new capture session.
     *
     * <p>The previously opened session will be safely disposed of before the new session opened.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void openCaptureSession() {
        Preconditions.checkState(mState.get() == State.OPENED);

        ValidatingBuilder validatingBuilder;
        synchronized (mAttachedUseCaseLock) {
            validatingBuilder = mUseCaseAttachState.getOnlineBuilder();
        }
        if (!validatingBuilder.isValid()) {
            Log.d(TAG, "Unable to create capture session due to conflicting configurations");
            return;
        }

        try {
            mCaptureSession.open(validatingBuilder.build(), mCameraDevice);
        } catch (CameraAccessException e) {
            Log.d(TAG, "Unable to configure camera " + mCameraId + " due to " + e.getMessage());
        }
    }

    /**
     * Replaces the old session with a new session initialized with the old session's configuration.
     *
     * <p>This does not close the previous session. The previous session should be
     * explicitly released before calling this method so the camera can track the state of
     * closing that session.
     */
    @WorkerThread
    private void resetCaptureSession(boolean abortInFlightCaptures) {
        Preconditions.checkState(mCaptureSession != null);
        Log.d(TAG, "Resetting Capture Session");
        releaseSession(mCaptureSession, /*abortInFlightCaptures=*/abortInFlightCaptures);

        // Recreate an initialized (but not opened) capture session from the previous configuration
        SessionConfig previousSessionConfig = mCaptureSession.getSessionConfig();
        List<CaptureConfig> unissuedCaptureConfigs = mCaptureSession.getCaptureConfigs();
        mCaptureSession = new CaptureSession(mExecutor);
        mCaptureSession.setSessionConfig(previousSessionConfig);
        mCaptureSession.issueCaptureRequests(unissuedCaptureConfigs);
    }

    private CameraDevice.StateCallback createDeviceStateCallback() {
        synchronized (mAttachedUseCaseLock) {
            SessionConfig config = mUseCaseAttachState.getOnlineBuilder().build();

            List<CameraDevice.StateCallback> configuredStateCallbacks =
                    config.getDeviceStateCallbacks();
            List<CameraDevice.StateCallback> allStateCallbacks =
                    new ArrayList<>(configuredStateCallbacks);
            allStateCallbacks.add(mStateCallback);
            return CameraDeviceStateCallbacks.createComboCallback(allStateCallbacks);
        }
    }

    /**
     * If the {@link CaptureConfig.Builder} hasn't had a surface attached, attaches all valid
     * repeating surfaces to it.
     *
     * @param captureConfigBuilder the configuration builder to attach repeating surfaces.
     * @return true if repeating surfaces have been successfully attached, otherwise false.
     */
    private boolean checkAndAttachRepeatingSurface(CaptureConfig.Builder captureConfigBuilder) {
        if (!captureConfigBuilder.getSurfaces().isEmpty()) {
            Log.w(TAG, "The capture config builder already has surface inside.");
            return false;
        }

        Collection<UseCase> activeUseCases;
        synchronized (mAttachedUseCaseLock) {
            activeUseCases = mUseCaseAttachState.getActiveAndOnlineUseCases();
        }

        for (UseCase useCase : activeUseCases) {
            SessionConfig sessionConfig = useCase.getSessionConfig(mCameraId);
            // Query the repeating surfaces attached to this use case, then add them to the builder.
            List<DeferrableSurface> surfaces =
                    sessionConfig.getRepeatingCaptureConfig().getSurfaces();
            if (!surfaces.isEmpty()) {
                for (DeferrableSurface surface : surfaces) {
                    captureConfigBuilder.addSurface(surface);
                }
            }
        }

        if (captureConfigBuilder.getSurfaces().isEmpty()) {
            Log.w(TAG, "Unable to find a repeating surface to attach to CaptureConfig");
            return false;
        }

        return true;
    }

    /** Returns the Camera2CameraControl attached to Camera */
    @Override
    public CameraControl getCameraControl() {
        return mCameraControl;
    }

    /**
     * Submits capture requests
     *
     * @param captureConfigs capture configuration used for creating CaptureRequest
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void submitCaptureRequests(final List<CaptureConfig> captureConfigs) {
        if (Looper.myLooper() != mHandler.getLooper()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Camera.this.submitCaptureRequests(captureConfigs);
                }
            });
            return;
        }

        List<CaptureConfig> captureConfigsWithSurface = new ArrayList<>();
        for (CaptureConfig captureConfig : captureConfigs) {
            // Recreates the Builder to add extra config needed
            CaptureConfig.Builder builder = CaptureConfig.Builder.from(captureConfig);

            if (captureConfig.getSurfaces().isEmpty() && captureConfig.isUseRepeatingSurface()) {
                // Checks and attaches repeating surface to the request if there's no surface
                // has been already attached. If there's no valid repeating surface to be
                // attached, skip this capture request.
                if (!checkAndAttachRepeatingSurface(builder)) {
                    continue;
                }
            }
            captureConfigsWithSurface.add(builder.build());
        }

        Log.d(TAG, "issue capture request for camera " + mCameraId);

        mCaptureSession.issueCaptureRequests(captureConfigsWithSurface);
    }

    /** {@inheritDoc} */
    @Override
    public void onCameraControlUpdateSessionConfig(SessionConfig sessionConfig) {
        mCameraControlSessionConfig = sessionConfig;
        updateCaptureSessionConfig();
    }

    /** {@inheritDoc} */
    @Override
    public void onCameraControlCaptureRequests(List<CaptureConfig> captureConfigs) {
        submitCaptureRequests(captureConfigs);
    }

    enum State {
        /** The default state of the camera before construction. */
        UNINITIALIZED,
        /**
         * Stable state once the camera has been constructed.
         *
         * <p>At this state the {@link CameraDevice} should be invalid, but threads should be still
         * in a valid state. Whenever a camera device is fully closed the camera should return to
         * this state.
         *
         * <p>After an error occurs the camera returns to this state so that the device can be
         * cleanly reopened.
         */
        INITIALIZED,
        /**
         * A transitional state where the camera device is currently opening.
         *
         * <p>At the end of this state, the camera should move into either the OPENED or CLOSING
         * state.
         */
        OPENING,
        /**
         * A stable state where the camera has been opened.
         *
         * <p>During this state the camera device should be valid. It is at this time a valid
         * capture session can be active. Capture requests should be issued during this state only.
         */
        OPENED,
        /**
         * A transitional state where the camera device is currently closing.
         *
         * <p>At the end of this state, the camera should move into the INITIALIZED state.
         */
        CLOSING,
        /**
         * A transitional state where the camera was previously closing, but not fully closed before
         * a call to open was made.
         *
         * <p>At the end of this state, the camera should move into one of two states. The OPENING
         * state if the device becomes fully closed, since it must restart the process of opening a
         * camera. The OPENED state if the device becomes opened, which can occur if a call to close
         * had been done during the OPENING state.
         */
        REOPENING,
        /**
         * A transitional state where the camera will be closing permanently.
         *
         * <p>At the end of this state, the camera should move into the RELEASED state.
         */
        RELEASING,
        /**
         * A stable state where the camera has been permanently closed.
         *
         * <p>During this state all resources should be released and all operations on the camera
         * will do nothing.
         */
        RELEASED
    }

    final class StateCallback extends CameraDevice.StateCallback {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            Log.d(TAG, "CameraDevice.onOpened(): " + cameraDevice.getId());
            Camera.this.mCameraDevice = cameraDevice;
            switch (mState.get()) {
                case CLOSING:
                case RELEASING:
                    // No session should have yet been opened, so close camera directly here.
                    Preconditions.checkState(isSessionCloseComplete());
                    mCameraDevice.close();
                    break;
                case OPENING:
                case REOPENING:
                    mState.set(State.OPENED);
                    openCaptureSession();
                    break;
                default:
                    throw new IllegalStateException(
                            "onOpened() should not be possible from state: " + mState.get());
            }
        }

        @Override
        public void onClosed(CameraDevice cameraDevice) {
            Log.d(TAG, "CameraDevice.onClosed(): " + cameraDevice.getId());
            Preconditions.checkState(mCameraDevice == cameraDevice);
            switch (mState.get()) {
                case CLOSING:
                case RELEASING:
                    Preconditions.checkState(isSessionCloseComplete());
                    finishClose();
                    break;
                case REOPENING:
                    mState.set(State.OPENING);
                    openCameraDevice();
                    break;
                default:
                    CameraX.postError(
                            CameraX.ErrorCode.CAMERA_STATE_INCONSISTENT,
                            "Camera closed while in state: " + mState.get());
            }
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            Log.d(TAG, "CameraDevice.onDisconnected(): " + cameraDevice.getId());

            // onDisconnected could be called before onOpened if the camera becomes disconnected
            // during initialization, so keep track of it here.
            mCameraDevice = cameraDevice;

            switch (mState.get()) {
                case CLOSING:
                case REOPENING:
                case OPENED:
                case OPENING:
                    // TODO: Create a "DISCONNECTED" state so camera can recover once available.
                    mState.set(State.RELEASING);
                    break;
                case RELEASING:
                    // State will be set to RELEASED once camera finishes closing.
                    break;
                default:
                    throw new IllegalStateException(
                            "onDisconnected() should not be possible from state: " + mState.get());
            }

            closeCamera(/*abortInFlightCaptures=*/true);
        }

        private String getErrorMessage(int errorCode) {
            switch (errorCode) {
                case CameraDevice.StateCallback.ERROR_CAMERA_DEVICE:
                    return "ERROR_CAMERA_DEVICE";
                case CameraDevice.StateCallback.ERROR_CAMERA_DISABLED:
                    return "ERROR_CAMERA_DISABLED";
                case CameraDevice.StateCallback.ERROR_CAMERA_IN_USE:
                    return "ERROR_CAMERA_IN_USE";
                case CameraDevice.StateCallback.ERROR_CAMERA_SERVICE:
                    return "ERROR_CAMERA_SERVICE";
                case CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE:
                    return "ERROR_MAX_CAMERAS_IN_USE";
                default: // fall out
            }
            return "UNKNOWN ERROR";
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            Log.e(
                    TAG,
                    "CameraDevice.onError(): "
                            + cameraDevice.getId()
                            + " with error: "
                            + getErrorMessage(error));

            // onError could be called before onOpened if there is an error opening the camera
            // during initialization, so keep track of it here.
            mCameraDevice = cameraDevice;

            switch (mState.get()) {
                case RELEASING:
                    break;
                case CLOSING:
                case REOPENING:
                case OPENED:
                case OPENING:
                    mState.set(State.RELEASING);
                    break;
                default:
                    throw new IllegalStateException(
                            "onError() should not be possible from state: " + mState.get());
            }

            // TODO: Handle errors or put camera into an "ERROR" state.
            closeCamera(/*abortInFlightCaptures=*/true);
        }
    }
}
