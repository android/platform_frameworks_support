/*
 * Copyright (C) 2011-2012 The Android Open Source Project
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

#define LOG_TAG "libRS_jni"

#include <stdlib.h>
#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>
#include <math.h>
#include <android/bitmap.h>
#include <android/log.h>

#include <rsEnv.h>
#include "rsDispatch.h"
#include <dlfcn.h>

//#define LOG_API ALOG
#define LOG_API(...)

#define NELEM(m) (sizeof(m) / sizeof((m)[0]))

class AutoJavaStringToUTF8 {
public:
    AutoJavaStringToUTF8(JNIEnv* env, jstring str) : fEnv(env), fJStr(str) {
        fCStr = env->GetStringUTFChars(str, NULL);
        fLength = env->GetStringUTFLength(str);
    }
    ~AutoJavaStringToUTF8() {
        fEnv->ReleaseStringUTFChars(fJStr, fCStr);
    }
    const char* c_str() const { return fCStr; }
    jsize length() const { return fLength; }

private:
    JNIEnv*     fEnv;
    jstring     fJStr;
    const char* fCStr;
    jsize       fLength;
};

class AutoJavaStringArrayToUTF8 {
public:
    AutoJavaStringArrayToUTF8(JNIEnv* env, jobjectArray strings, jsize stringsLength)
    : mEnv(env), mStrings(strings), mStringsLength(stringsLength) {
        mCStrings = NULL;
        mSizeArray = NULL;
        if (stringsLength > 0) {
            mCStrings = (const char **)calloc(stringsLength, sizeof(char *));
            mSizeArray = (size_t*)calloc(stringsLength, sizeof(size_t));
            for (jsize ct = 0; ct < stringsLength; ct ++) {
                jstring s = (jstring)mEnv->GetObjectArrayElement(mStrings, ct);
                mCStrings[ct] = mEnv->GetStringUTFChars(s, NULL);
                mSizeArray[ct] = mEnv->GetStringUTFLength(s);
            }
        }
    }
    ~AutoJavaStringArrayToUTF8() {
        for (jsize ct=0; ct < mStringsLength; ct++) {
            jstring s = (jstring)mEnv->GetObjectArrayElement(mStrings, ct);
            mEnv->ReleaseStringUTFChars(s, mCStrings[ct]);
        }
        free(mCStrings);
        free(mSizeArray);
    }
    const char **c_str() const { return mCStrings; }
    size_t *c_str_len() const { return mSizeArray; }
    jsize length() const { return mStringsLength; }

private:
    JNIEnv      *mEnv;
    jobjectArray mStrings;
    const char **mCStrings;
    size_t      *mSizeArray;
    jsize        mStringsLength;
};


// ---------------------------------------------------------------------------
static dispatchTable dispatchTab;

static jboolean nLoadSO(JNIEnv *_env, jobject _this, jboolean useNative) {
    void* handle = NULL;
    if (useNative) {
        handle = dlopen("libRS.so", RTLD_LAZY | RTLD_LOCAL);
    } else {
        handle = dlopen("libRSSupport.so", RTLD_LAZY | RTLD_LOCAL);
    }
    if (handle == NULL) {
        LOG_API("couldn't dlopen %s, %s", filename, dlerror());
        return false;
    }

    if (loadSymbols(handle, dispatchTab) == false) {
        LOG_API("%s init failed!", filename);
        return false;
    }
    LOG_API("Successfully loaded %s", filename);
    return true;
}

static ioSuppDT ioDispatch;
static jboolean nLoadIOSO(JNIEnv *_env, jobject _this) {
    void* handleIO = NULL;
    handleIO = dlopen("libRSSupportIO.so", RTLD_LAZY | RTLD_LOCAL);
    if (handleIO == NULL) {
        LOG_API("Couldn't load libRSSupportIO.so");
        return false;
    }
    if (loadIOSuppSyms(handleIO, ioDispatch) == false) {
        LOG_API("libRSSupportIO init failed!");
        return false;
    }
    return true;
}

// Incremental Support lib
static dispatchTable dispatchTabInc;
//static RsContext incCon = NULL;
//static RsDevice incDev = NULL;
//static bool incLoaded = false;

// ---------------------------------------------------------------------------

static void
nContextFinish(JNIEnv *_env, jobject _this, jlong con)
{
    LOG_API("nContextFinish, con(%p)", (RsContext)con);
    dispatchTab.ContextFinish((RsContext)con);
}

static jlong
nClosureCreate(JNIEnv *_env, jobject _this, jlong con, jlong kernelID,
               jlong returnValue, jlongArray fieldIDArray,
               jlongArray valueArray, jintArray sizeArray,
               jlongArray depClosureArray, jlongArray depFieldIDArray) {
  LOG_API("nClosureCreate: con(%p)", con);
  jlong* jFieldIDs = _env->GetLongArrayElements(fieldIDArray, nullptr);
  jsize fieldIDs_length = _env->GetArrayLength(fieldIDArray);
  RsScriptFieldID* fieldIDs =
      (RsScriptFieldID*)alloca(sizeof(RsScriptFieldID) * fieldIDs_length);
  for (int i = 0; i< fieldIDs_length; i++) {
    fieldIDs[i] = (RsScriptFieldID)jFieldIDs[i];
  }

  jlong* jValues = _env->GetLongArrayElements(valueArray, nullptr);
  jsize values_length = _env->GetArrayLength(valueArray);
  uintptr_t* values = (uintptr_t*)alloca(sizeof(uintptr_t) * values_length);
  for (int i = 0; i < values_length; i++) {
    values[i] = (uintptr_t)jValues[i];
  }

  jint* sizes = _env->GetIntArrayElements(sizeArray, nullptr);
  jsize sizes_length = _env->GetArrayLength(sizeArray);

  jlong* jDepClosures =
      _env->GetLongArrayElements(depClosureArray, nullptr);
  jsize depClosures_length = _env->GetArrayLength(depClosureArray);
  RsClosure* depClosures =
      (RsClosure*)alloca(sizeof(RsClosure) * depClosures_length);
  for (int i = 0; i < depClosures_length; i++) {
    depClosures[i] = (RsClosure)jDepClosures[i];
  }

  jlong* jDepFieldIDs =
      _env->GetLongArrayElements(depFieldIDArray, nullptr);
  jsize depFieldIDs_length = _env->GetArrayLength(depFieldIDArray);
  RsScriptFieldID* depFieldIDs =
      (RsScriptFieldID*)alloca(sizeof(RsScriptFieldID) * depFieldIDs_length);
  for (int i = 0; i < depClosures_length; i++) {
    depFieldIDs[i] = (RsClosure)jDepFieldIDs[i];
  }

  return (jlong)(uintptr_t)dispatchTab.ClosureCreate(
      (RsContext)con, (RsScriptKernelID)kernelID, (RsAllocation)returnValue,
      fieldIDs, (size_t)fieldIDs_length, values, (size_t)values_length,
      (size_t*)sizes, (size_t)sizes_length,
      depClosures, (size_t)depClosures_length,
      depFieldIDs, (size_t)depFieldIDs_length);
}

static void
nClosureSetArg(JNIEnv *_env, jobject _this, jlong con, jlong closureID,
               jint index, jlong value, jint size) {
  dispatchTab.ClosureSetArg((RsContext)con, (RsClosure)closureID,
                            (uint32_t)index, (uintptr_t)value, (size_t)size);
}

static void
nClosureSetGlobal(JNIEnv *_env, jobject _this, jlong con, jlong closureID,
                  jlong fieldID, jlong value, jint size) {
  dispatchTab.ClosureSetGlobal((RsContext)con, (RsClosure)closureID,
                               (RsScriptFieldID)fieldID, (uintptr_t)value,
                               (size_t)size);
}

static long
nScriptGroup2Create(JNIEnv *_env, jobject _this, jlong con,
                    jlongArray closureArray) {
  jlong* jClosures = _env->GetLongArrayElements(closureArray, nullptr);
  jsize numClosures = _env->GetArrayLength(closureArray);
  RsClosure* closures = (RsClosure*)alloca(sizeof(RsClosure) * numClosures);
  for (int i = 0; i < numClosures; i++) {
    closures[i] = (RsClosure)jClosures[i];
  }

  return (jlong)(uintptr_t)dispatchTab.ScriptGroup2Create((RsContext)con,
                                                          closures,
                                                          numClosures);
}

static void
nObjDestroy(JNIEnv *_env, jobject _this, jlong con, jlong obj)
{
    LOG_API("nObjDestroy, con(%p) obj(%p)", (RsContext)con, (void *)obj);
    dispatchTab.ObjDestroy((RsContext)con, (void *)obj);
}

// ---------------------------------------------------------------------------

static jlong
nDeviceCreate(JNIEnv *_env, jobject _this)
{
    LOG_API("nDeviceCreate");
    return (jlong)(uintptr_t)dispatchTab.DeviceCreate();
}

static void
nDeviceDestroy(JNIEnv *_env, jobject _this, jlong dev)
{
    LOG_API("nDeviceDestroy");
    return dispatchTab.DeviceDestroy((RsDevice)dev);
}

static void
nDeviceSetConfig(JNIEnv *_env, jobject _this, jlong dev, jint p, jint value)
{
    LOG_API("nDeviceSetConfig  dev(%p), param(%i), value(%i)", (void *)dev, p, value);
    return dispatchTab.DeviceSetConfig((RsDevice)dev, (RsDeviceParam)p, value);
}

static jlong
nContextCreate(JNIEnv *_env, jobject _this, jlong dev, jint ver, jint sdkVer, jint ct)
{
    LOG_API("nContextCreate");
    return (jlong)(uintptr_t)dispatchTab.ContextCreate((RsDevice)dev, ver, sdkVer, (RsContextType)ct, 0);
}


static void
nContextSetPriority(JNIEnv *_env, jobject _this, jlong con, jint p)
{
    LOG_API("ContextSetPriority, con(%p), priority(%i)", (RsContext)con, p);
    dispatchTab.ContextSetPriority((RsContext)con, p);
}



static void
nContextDestroy(JNIEnv *_env, jobject _this, jlong con)
{
    LOG_API("nContextDestroy, con(%p)", (RsContext)con);
    dispatchTab.ContextDestroy((RsContext)con);
}

static void
nContextDump(JNIEnv *_env, jobject _this, jlong con, jint bits)
{
    LOG_API("nContextDump, con(%p)  bits(%i)", (RsContext)con, bits);
    dispatchTab.ContextDump((RsContext)con, bits);
}


static jstring
nContextGetErrorMessage(JNIEnv *_env, jobject _this, jlong con)
{
    LOG_API("nContextGetErrorMessage, con(%p)", (RsContext)con);
    char buf[1024];

    size_t receiveLen;
    uint32_t subID;
    int id = dispatchTab.ContextGetMessage((RsContext)con,
                                 buf, sizeof(buf),
                                 &receiveLen, sizeof(receiveLen),
                                 &subID, sizeof(subID));
    if (!id && receiveLen) {
        //        __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
        //            "message receive buffer too small.  %zu", receiveLen);
    }
    return _env->NewStringUTF(buf);
}

static jint
nContextGetUserMessage(JNIEnv *_env, jobject _this, jlong con, jintArray data)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nContextGetMessage, con(%p), len(%i)", (RsContext)con, len);
    jint *ptr = _env->GetIntArrayElements(data, NULL);
    size_t receiveLen;
    uint32_t subID;
    int id = dispatchTab.ContextGetMessage((RsContext)con,
                                 ptr, len * 4,
                                 &receiveLen, sizeof(receiveLen),
                                 &subID, sizeof(subID));
    if (!id && receiveLen) {
        //        __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
        //            "message receive buffer too small.  %zu", receiveLen);
    }
    _env->ReleaseIntArrayElements(data, ptr, 0);
    return (jint)id;
}

static jint
nContextPeekMessage(JNIEnv *_env, jobject _this, jlong con, jintArray auxData)
{
    LOG_API("nContextPeekMessage, con(%p)", (RsContext)con);
    jint *auxDataPtr = _env->GetIntArrayElements(auxData, NULL);
    size_t receiveLen;
    uint32_t subID;
    int id = dispatchTab.ContextPeekMessage((RsContext)con, &receiveLen, sizeof(receiveLen),
                                  &subID, sizeof(subID));
    auxDataPtr[0] = (jint)subID;
    auxDataPtr[1] = (jint)receiveLen;
    _env->ReleaseIntArrayElements(auxData, auxDataPtr, 0);
    return (jint)id;
}

static void nContextInitToClient(JNIEnv *_env, jobject _this, jlong con)
{
    LOG_API("nContextInitToClient, con(%p)", (RsContext)con);
    dispatchTab.ContextInitToClient((RsContext)con);
}

static void nContextDeinitToClient(JNIEnv *_env, jobject _this, jlong con)
{
    LOG_API("nContextDeinitToClient, con(%p)", (RsContext)con);
    dispatchTab.ContextDeinitToClient((RsContext)con);
}

static void
nContextSendMessage(JNIEnv *_env, jobject _this, jlong con, jint id, jintArray data)
{
    jint *ptr = NULL;
    jint len = 0;
    if (data) {
        len = _env->GetArrayLength(data);
        jint *ptr = _env->GetIntArrayElements(data, NULL);
    }
    LOG_API("nContextSendMessage, con(%p), id(%i), len(%i)", (RsContext)con, id, len);
    dispatchTab.ContextSendMessage((RsContext)con, id, (const uint8_t *)ptr, len * sizeof(int));
    if (data) {
        _env->ReleaseIntArrayElements(data, ptr, JNI_ABORT);
    }
}



static jlong
nElementCreate(JNIEnv *_env, jobject _this, jlong con, jlong type, jint kind, jboolean norm, jint size)
{
    LOG_API("nElementCreate, con(%p), type(%i), kind(%i), norm(%i), size(%i)", (RsContext)con,
            type, kind, norm, size);
    return (jlong)(uintptr_t)dispatchTab.ElementCreate((RsContext)con, (RsDataType)type, (RsDataKind)kind,
                                                       norm, size);
}

static jlong
nElementCreate2(JNIEnv *_env, jobject _this, jlong con,
                jlongArray _ids, jobjectArray _names, jintArray _arraySizes)
{
    int fieldCount = _env->GetArrayLength(_ids);
    LOG_API("nElementCreate2, con(%p)", (RsContext)con);

    jlong *jIds = _env->GetLongArrayElements(_ids, NULL);
    jint *jArraySizes = _env->GetIntArrayElements(_arraySizes, NULL);

    RsElement *ids = (RsElement*)malloc(fieldCount * sizeof(RsElement));
    uint32_t *arraySizes = (uint32_t *)malloc(fieldCount * sizeof(uint32_t));

    for(int i = 0; i < fieldCount; i ++) {
        ids[i] = (RsElement)jIds[i];
        arraySizes[i] = (uint32_t)jArraySizes[i];
    }

    AutoJavaStringArrayToUTF8 names(_env, _names, fieldCount);

    const char **nameArray = names.c_str();
    size_t *sizeArray = names.c_str_len();

    jlong id = (jlong)(uintptr_t)dispatchTab.ElementCreate2((RsContext)con,
                                     (RsElement *)ids, fieldCount,
                                     nameArray, fieldCount * sizeof(size_t),  sizeArray,
                                     (const uint32_t *)arraySizes, fieldCount);

    free(ids);
    free(arraySizes);
    _env->ReleaseLongArrayElements(_ids, jIds, JNI_ABORT);
    _env->ReleaseIntArrayElements(_arraySizes, jArraySizes, JNI_ABORT);
    return id;
}




static void
nElementGetSubElements(JNIEnv *_env, jobject _this, jlong con, jlong id,
                       jlongArray _IDs,
                       jobjectArray _names,
                       jintArray _arraySizes)
{
    uint32_t dataSize = _env->GetArrayLength(_IDs);
    LOG_API("nElementGetSubElements, con(%p)", (RsContext)con);

    uintptr_t *ids = (uintptr_t *)malloc(dataSize * sizeof(uintptr_t));
    const char **names = (const char **)malloc((uint32_t)dataSize * sizeof(const char *));
    uint32_t *arraySizes = (uint32_t *)malloc((uint32_t)dataSize * sizeof(uint32_t));

    dispatchTab.ElementGetSubElements((RsContext)con, (RsElement)id, ids, names, arraySizes,
                                      (uint32_t)dataSize);

    for(uint32_t i = 0; i < dataSize; i++) {
        const jlong id = (jlong)(uintptr_t)ids[i];
        const jint arraySize = (jint)arraySizes[i];
        _env->SetObjectArrayElement(_names, i, _env->NewStringUTF(names[i]));
        _env->SetLongArrayRegion(_IDs, i, 1, &id);
        _env->SetIntArrayRegion(_arraySizes, i, 1, &arraySize);
    }

    free(ids);
    free(names);
    free(arraySizes);
}

// -----------------------------------

static jlong
nTypeCreate(JNIEnv *_env, jobject _this, jlong con, jlong eid,
            jint dimx, jint dimy, jint dimz, jboolean mips, jboolean faces, jint yuv)
{
    LOG_API("nTypeCreate, con(%p) eid(%p), x(%i), y(%i), z(%i), mips(%i), faces(%i), yuv(%i)",
            (RsContext)con, eid, dimx, dimy, dimz, mips, faces, yuv);

    return (jlong)(uintptr_t)dispatchTab.TypeCreate((RsContext)con, (RsElement)eid, dimx, dimy,
                                                    dimz, mips, faces, yuv);
}

// -----------------------------------

static jlong
nAllocationCreateTyped(JNIEnv *_env, jobject _this, jlong con, jlong type, jint mips, jint usage,
                       jlong pointer)
{
    LOG_API("nAllocationCreateTyped, con(%p), type(%p), mip(%i), usage(%i), ptr(%p)",
            (RsContext)con, (RsElement)type, mips, usage, (void *)pointer);
    return (jlong)(uintptr_t) dispatchTab.AllocationCreateTyped((RsContext)con, (RsType)type,
                                                                (RsAllocationMipmapControl)mips,
                                                                (uint32_t)usage, (uintptr_t)pointer);
}

static void
nAllocationSyncAll(JNIEnv *_env, jobject _this, jlong con, jlong a, jint bits)
{
    LOG_API("nAllocationSyncAll, con(%p), a(%p), bits(0x%08x)", (RsContext)con, (RsAllocation)a, bits);
    dispatchTab.AllocationSyncAll((RsContext)con, (RsAllocation)a, (RsAllocationUsageType)bits);
}

static void
nAllocationSetSurface(JNIEnv *_env, jobject _this, jlong con, jlong alloc, jobject sur)
{
    ioDispatch.sAllocationSetSurface(_env, _this, (RsContext)con, (RsAllocation)alloc, sur, dispatchTab);
}

static void
nAllocationIoSend(JNIEnv *_env, jobject _this, jlong con, jlong alloc)
{
    dispatchTab.AllocationIoSend((RsContext)con, (RsAllocation)alloc);
}

static void
nAllocationGenerateMipmaps(JNIEnv *_env, jobject _this, jlong con, jlong alloc)
{
    LOG_API("nAllocationGenerateMipmaps, con(%p), a(%p)", (RsContext)con, (RsAllocation)alloc);
    dispatchTab.AllocationGenerateMipmaps((RsContext)con, (RsAllocation)alloc);
}

static size_t GetBitmapSize(JNIEnv *env, jobject jbitmap) {
    AndroidBitmapInfo info;
    memset(&info, 0, sizeof(info));
    AndroidBitmap_getInfo(env, jbitmap, &info);
    size_t s = info.width * info.height;
    switch (info.format) {
        case ANDROID_BITMAP_FORMAT_RGBA_8888: s *= 4; break;
        case ANDROID_BITMAP_FORMAT_RGB_565: s *= 2; break;
        case ANDROID_BITMAP_FORMAT_RGBA_4444: s *= 2; break;
    }
    return s;
}

static jlong
nAllocationCreateFromBitmap(JNIEnv *_env, jobject _this, jlong con, jlong type, jint mip,
                            jobject jbitmap, jint usage)
{
    jlong id = 0;
    void *pixels = NULL;
    AndroidBitmap_lockPixels(_env, jbitmap, &pixels);

    if (pixels != NULL) {
        id = (jlong)(uintptr_t)dispatchTab.AllocationCreateFromBitmap((RsContext)con,
                                                                      (RsType)type,
                                                                      (RsAllocationMipmapControl)mip,
                                                                      pixels,
                                                                      GetBitmapSize(_env, jbitmap),
                                                                      usage);
        AndroidBitmap_unlockPixels(_env, jbitmap);
    }
    return id;
}

static jlong
nAllocationCreateBitmapBackedAllocation(JNIEnv *_env, jobject _this, jlong con, jlong type,
                                        jint mip, jobject jbitmap, jint usage)
{
    jlong id = 0;
    void *pixels = NULL;
    AndroidBitmap_lockPixels(_env, jbitmap, &pixels);

    if (pixels != NULL) {
        id = (jlong)(uintptr_t)dispatchTab.AllocationCreateTyped((RsContext)con,
                                                                 (RsType)type,
                                                                 (RsAllocationMipmapControl)mip,
                                                                 (uint32_t)usage,
                                                                 (uintptr_t)pixels);
        AndroidBitmap_unlockPixels(_env, jbitmap);
    }
    return id;
}

static jlong
nAllocationCubeCreateFromBitmap(JNIEnv *_env, jobject _this, jlong con, jlong type,
                                jint mip, jobject jbitmap, jint usage)
{
    void *pixels = NULL;
    AndroidBitmap_lockPixels(_env, jbitmap, &pixels);

    jlong id = 0;
    if (pixels != NULL) {
        id = (jlong)(uintptr_t)dispatchTab.AllocationCubeCreateFromBitmap((RsContext)con,
                                                                          (RsType)type,
                                                                          (RsAllocationMipmapControl)mip,
                                                                          pixels,
                                                                          GetBitmapSize(_env, jbitmap),
                                                                          usage);
        AndroidBitmap_unlockPixels(_env, jbitmap);
    }
    return id;
}

static void
nAllocationCopyFromBitmap(JNIEnv *_env, jobject _this, jlong con, jlong alloc, jobject jbitmap)
{
    AndroidBitmapInfo info;
    memset(&info, 0, sizeof(info));
    AndroidBitmap_getInfo(_env, jbitmap, &info);

    void *pixels = NULL;
    AndroidBitmap_lockPixels(_env, jbitmap, &pixels);

    if (pixels != NULL) {
        dispatchTab.Allocation2DData((RsContext)con, (RsAllocation)alloc, 0, 0, 0,
                                     RS_ALLOCATION_CUBEMAP_FACE_POSITIVE_X, info.width,
                                     info.height, pixels, GetBitmapSize(_env, jbitmap), 0);
        AndroidBitmap_unlockPixels(_env, jbitmap);
    }
}

static void
nAllocationCopyToBitmap(JNIEnv *_env, jobject _this, jlong con, jlong alloc, jobject jbitmap)
{
    AndroidBitmapInfo info;
    memset(&info, 0, sizeof(info));
    AndroidBitmap_getInfo(_env, jbitmap, &info);

    void *pixels = NULL;
    AndroidBitmap_lockPixels(_env, jbitmap, &pixels);

    if (pixels != NULL) {
        dispatchTab.AllocationCopyToBitmap((RsContext)con, (RsAllocation)alloc, pixels,
                                           GetBitmapSize(_env, jbitmap));
        AndroidBitmap_unlockPixels(_env, jbitmap);
    }
    //bitmap.notifyPixelsChanged();
}


static void
nAllocationData1D_l(JNIEnv *_env, jobject _this, jlong con, jlong alloc, jint offset,
                    jint lod, jint count, jlongArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation1DData_i, con(%p), adapter(%p), offset(%i), count(%i), len(%i), sizeBytes(%i)",
            (RsContext)con, (RsAllocation)alloc, offset, count, len, sizeBytes);
    jlong *ptr = _env->GetLongArrayElements(data, NULL);
    dispatchTab.Allocation1DData((RsContext)con, (RsAllocation)alloc, offset, lod, count,
                                 ptr, sizeBytes);
    _env->ReleaseLongArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData1D_i(JNIEnv *_env, jobject _this, jlong con, jlong alloc, jint offset,
                    jint lod, jint count, jintArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation1DData_i, con(%p), adapter(%p), offset(%i), count(%i), len(%i), sizeBytes(%i)",
            (RsContext)con, (RsAllocation)alloc, offset, count, len, sizeBytes);
    jint *ptr = _env->GetIntArrayElements(data, NULL);
    dispatchTab.Allocation1DData((RsContext)con, (RsAllocation)alloc, offset, lod, count,
                                 ptr, sizeBytes);
    _env->ReleaseIntArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData1D_s(JNIEnv *_env, jobject _this, jlong con, jlong alloc, jint offset,
                    jint lod, jint count, jshortArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation1DData_s, con(%p), adapter(%p), offset(%i), count(%i), len(%i), sizeBytes(%i)",
            (RsContext)con, (RsAllocation)alloc, offset, count, len, sizeBytes);
    jshort *ptr = _env->GetShortArrayElements(data, NULL);
    dispatchTab.Allocation1DData((RsContext)con, (RsAllocation)alloc, offset, lod, count,
                                 ptr, sizeBytes);
    _env->ReleaseShortArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData1D_b(JNIEnv *_env, jobject _this, jlong con, jlong alloc, jint offset,
                    jint lod, jint count, jbyteArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation1DData_b, con(%p), adapter(%p), offset(%i), count(%i), len(%i), sizeBytes(%i)",
            (RsContext)con, (RsAllocation)alloc, offset, count, len, sizeBytes);
    jbyte *ptr = _env->GetByteArrayElements(data, NULL);
    dispatchTab.Allocation1DData((RsContext)con, (RsAllocation)alloc, offset, lod, count,
                                 ptr, sizeBytes);
    _env->ReleaseByteArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData1D_f(JNIEnv *_env, jobject _this, jlong con, jlong alloc, jint offset,
                    jint lod, jint count, jfloatArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation1DData_f, con(%p), adapter(%p), offset(%i), count(%i), len(%i), sizeBytes(%i)",
            (RsContext)con, (RsAllocation)alloc, offset, count, len, sizeBytes);
    jfloat *ptr = _env->GetFloatArrayElements(data, NULL);
    dispatchTab.Allocation1DData((RsContext)con, (RsAllocation)alloc, offset, lod, count,
                                 ptr, sizeBytes);
    _env->ReleaseFloatArrayElements(data, ptr, JNI_ABORT);
}

static void
//    native void rsnAllocationElementData1D(int con, int id, int xoff, int compIdx, byte[] d, int sizeBytes);
nAllocationElementData1D(JNIEnv *_env, jobject _this, jlong con, jlong alloc, jint offset,
                         jint lod, jint compIdx, jbyteArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocationElementData1D, con(%p), alloc(%p), offset(%i), comp(%i), len(%i), sizeBytes(%i)",
            (RsContext)con, (RsAllocation)alloc, offset, compIdx, len, sizeBytes);
    jbyte *ptr = _env->GetByteArrayElements(data, NULL);
    dispatchTab.Allocation1DElementData((RsContext)con, (RsAllocation)alloc, offset, lod,
                                        ptr, sizeBytes, compIdx);
    _env->ReleaseByteArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData2D_s(JNIEnv *_env, jobject _this, jlong con, jlong alloc, jint xoff, jint yoff,
                    jint lod, jint face, jint w, jint h, jshortArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation2DData_s, con(%p), adapter(%p), xoff(%i), yoff(%i), w(%i), h(%i), len(%i)",
            (RsContext)con, (RsAllocation)alloc, xoff, yoff, w, h, len);
    jshort *ptr = _env->GetShortArrayElements(data, NULL);
    dispatchTab.Allocation2DData((RsContext)con, (RsAllocation)alloc, xoff, yoff, lod,
                                 (RsAllocationCubemapFace)face, w, h, ptr, sizeBytes, 0);
    _env->ReleaseShortArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData2D_b(JNIEnv *_env, jobject _this, jlong con, jlong alloc, jint xoff, jint yoff,
                    jint lod, jint face, jint w, jint h, jbyteArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation2DData_b, con(%p), adapter(%p), xoff(%i), yoff(%i), w(%i), h(%i), len(%i)",
            (RsContext)con, (RsAllocation)alloc, xoff, yoff, w, h, len);
    jbyte *ptr = _env->GetByteArrayElements(data, NULL);
    dispatchTab.Allocation2DData((RsContext)con, (RsAllocation)alloc, xoff, yoff, lod,
                                 (RsAllocationCubemapFace)face, w, h, ptr, sizeBytes, 0);
    _env->ReleaseByteArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData2D_l(JNIEnv *_env, jobject _this, jlong con, jlong alloc, jint xoff, jint yoff,
                    jint lod, jint face, jint w, jint h, jlongArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation2DData_i, con(%p), adapter(%p), xoff(%i), yoff(%i), w(%i), h(%i), len(%i)",
            (RsContext)con, (RsAllocation)alloc, xoff, yoff, w, h, len);
    jlong *ptr = _env->GetLongArrayElements(data, NULL);
    dispatchTab.Allocation2DData((RsContext)con, (RsAllocation)alloc, xoff, yoff, lod,
                                 (RsAllocationCubemapFace)face, w, h, ptr, sizeBytes, 0);
    _env->ReleaseLongArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData2D_i(JNIEnv *_env, jobject _this, jlong con, jlong alloc, jint xoff, jint yoff,
                    jint lod, jint face, jint w, jint h, jintArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation2DData_i, con(%p), adapter(%p), xoff(%i), yoff(%i), w(%i), h(%i), len(%i)",
            (RsContext)con, (RsAllocation)alloc, xoff, yoff, w, h, len);
    jint *ptr = _env->GetIntArrayElements(data, NULL);
    dispatchTab.Allocation2DData((RsContext)con, (RsAllocation)alloc, xoff, yoff, lod,
                                 (RsAllocationCubemapFace)face, w, h, ptr, sizeBytes, 0);
    _env->ReleaseIntArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData2D_f(JNIEnv *_env, jobject _this, jlong con, jlong alloc, jint xoff, jint yoff,
                    jint lod, jint face, jint w, jint h, jfloatArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation2DData_i, con(%p), adapter(%p), xoff(%i), yoff(%i), w(%i), h(%i), len(%i)",
            (RsContext)con, (RsAllocation)alloc, xoff, yoff, w, h, len);
    jfloat *ptr = _env->GetFloatArrayElements(data, NULL);
    dispatchTab.Allocation2DData((RsContext)con, (RsAllocation)alloc, xoff, yoff, lod,
                                 (RsAllocationCubemapFace)face, w, h, ptr, sizeBytes, 0);
    _env->ReleaseFloatArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData2D_alloc(JNIEnv *_env, jobject _this, jlong con,
                        jlong dstAlloc, jint dstXoff, jint dstYoff,
                        jint dstMip, jint dstFace,
                        jint width, jint height,
                        jlong srcAlloc, jint srcXoff, jint srcYoff,
                        jint srcMip, jint srcFace)
{
    LOG_API("nAllocation2DData_s, con(%p), dstAlloc(%p), dstXoff(%i), dstYoff(%i),"
            " dstMip(%i), dstFace(%i), width(%i), height(%i),"
            " srcAlloc(%p), srcXoff(%i), srcYoff(%i), srcMip(%i), srcFace(%i)",
            (RsContext)con, (RsAllocation)dstAlloc, dstXoff, dstYoff, dstMip, dstFace,
            width, height, (RsAllocation)srcAlloc, srcXoff, srcYoff, srcMip, srcFace);

    dispatchTab.AllocationCopy2DRange((RsContext)con,
                                      (RsAllocation)dstAlloc,
                                      dstXoff, dstYoff,
                                      dstMip, dstFace,
                                      width, height,
                                      (RsAllocation)srcAlloc,
                                      srcXoff, srcYoff,
                                      srcMip, srcFace);
}

static void
nAllocationData3D_s(JNIEnv *_env, jobject _this, jlong con, jlong alloc,
                    jint xoff, jint yoff, jint zoff,
                    jint lod, jint w, jint h, jint d, jshortArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation3DData_s, con(%p), adapter(%p), xoff(%i), yoff(%i), w(%i), h(%i), len(%i)",
            (RsContext)con, (RsAllocation)alloc, xoff, yoff, zoff, w, h, d, len);
    jshort *ptr = _env->GetShortArrayElements(data, NULL);
    dispatchTab.Allocation3DData((RsContext)con, (RsAllocation)alloc, xoff, yoff, zoff,
                                 lod, w, h, d, ptr, sizeBytes, 0);
    _env->ReleaseShortArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData3D_b(JNIEnv *_env, jobject _this, jlong con, jlong alloc,
                    jint xoff, jint yoff, jint zoff,
                    jint lod, jint w, jint h, jint d, jbyteArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation3DData_b, con(%p), adapter(%p), xoff(%i), yoff(%i), w(%i), h(%i), len(%i)",
            (RsContext)con, (RsAllocation)alloc, xoff, yoff, zoff, w, h, d, len);
    jbyte *ptr = _env->GetByteArrayElements(data, NULL);
    dispatchTab.Allocation3DData((RsContext)con, (RsAllocation)alloc, xoff, yoff, zoff,
                                 lod, w, h, d, ptr, sizeBytes, 0);
    _env->ReleaseByteArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData3D_l(JNIEnv *_env, jobject _this, jlong con, jlong alloc,
                    jint xoff, jint yoff, jint zoff,
                    jint lod, jint w, jint h, jint d, jlongArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation3DData_i, con(%p), adapter(%p), xoff(%i), yoff(%i), w(%i), h(%i), len(%i)",
            (RsContext)con, (RsAllocation)alloc, xoff, yoff, zoff, w, h, d, len);
    jlong *ptr = _env->GetLongArrayElements(data, NULL);
    dispatchTab.Allocation3DData((RsContext)con, (RsAllocation)alloc, xoff, yoff, zoff,
                                 lod, w, h, d, ptr, sizeBytes, 0);
    _env->ReleaseLongArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData3D_i(JNIEnv *_env, jobject _this, jlong con, jlong alloc,
                    jint xoff, jint yoff, jint zoff,
                    jint lod, jint w, jint h, jint d, jintArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation3DData_i, con(%p), adapter(%p), xoff(%i), yoff(%i), w(%i), h(%i), len(%i)",
            (RsContext)con, (RsAllocation)alloc, xoff, yoff, zoff, w, h, d, len);
    jint *ptr = _env->GetIntArrayElements(data, NULL);
    dispatchTab.Allocation3DData((RsContext)con, (RsAllocation)alloc, xoff, yoff, zoff,
                                 lod, w, h, d, ptr, sizeBytes, 0);
    _env->ReleaseIntArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData3D_f(JNIEnv *_env, jobject _this, jlong con, jlong alloc, jint xoff, jint yoff,
                    jint zoff, jint lod, jint w, jint h, jint d, jfloatArray data, int sizeBytes)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocation3DData_f, con(%p), adapter(%p), xoff(%i), yoff(%i), w(%i), h(%i), len(%i)",
            (RsContext)con, (RsAllocation)alloc, xoff, yoff, zoff, w, h, d, len);
    jfloat *ptr = _env->GetFloatArrayElements(data, NULL);
    dispatchTab.Allocation3DData((RsContext)con, (RsAllocation)alloc, xoff, yoff, zoff,
                                 lod, w, h, d, ptr, sizeBytes, 0);
    _env->ReleaseFloatArrayElements(data, ptr, JNI_ABORT);
}

static void
nAllocationData3D_alloc(JNIEnv *_env, jobject _this, jlong con,
                        jlong dstAlloc, jint dstXoff, jint dstYoff, jint dstZoff,
                        jint dstMip,
                        jint width, jint height, jint depth,
                        jlong srcAlloc, jint srcXoff, jint srcYoff, jint srcZoff,
                        jint srcMip)
{
    LOG_API("nAllocationData3D_alloc, con(%p), dstAlloc(%p), dstXoff(%i), dstYoff(%i),"
            " dstMip(%i), width(%i), height(%i),"
            " srcAlloc(%p), srcXoff(%i), srcYoff(%i), srcMip(%i)",
            (RsContext)con, (RsAllocation)dstAlloc, dstXoff, dstYoff, dstMip, dstFace,
            width, height, (RsAllocation)srcAlloc, srcXoff, srcYoff, srcMip, srcFace);

    dispatchTab.AllocationCopy3DRange((RsContext)con,
                                      (RsAllocation)dstAlloc,
                                      dstXoff, dstYoff, dstZoff, dstMip,
                                      width, height, depth,
                                      (RsAllocation)srcAlloc,
                                      srcXoff, srcYoff, srcZoff, srcMip);
}

static void
nAllocationRead_l(JNIEnv *_env, jobject _this, jlong con, jlong alloc, jlongArray data)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocationRead_i, con(%p), alloc(%p), len(%i)", (RsContext)con,
            (RsAllocation)alloc, len);
    jlong *ptr = _env->GetLongArrayElements(data, NULL);
    jsize length = _env->GetArrayLength(data);
    dispatchTab.AllocationRead((RsContext)con, (RsAllocation)alloc, ptr, length * sizeof(int));
    _env->ReleaseLongArrayElements(data, ptr, 0);
}

static void
nAllocationRead_i(JNIEnv *_env, jobject _this, jlong con, jlong alloc, jintArray data)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocationRead_i, con(%p), alloc(%p), len(%i)", (RsContext)con,
            (RsAllocation)alloc, len);
    jint *ptr = _env->GetIntArrayElements(data, NULL);
    jsize length = _env->GetArrayLength(data);
    dispatchTab.AllocationRead((RsContext)con, (RsAllocation)alloc, ptr, length * sizeof(int));
    _env->ReleaseIntArrayElements(data, ptr, 0);
}

static void
nAllocationRead_s(JNIEnv *_env, jobject _this, jlong con, jlong alloc, jshortArray data)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocationRead_i, con(%p), alloc(%p), len(%i)", (RsContext)con,
            (RsAllocation)alloc, len);
    jshort *ptr = _env->GetShortArrayElements(data, NULL);
    jsize length = _env->GetArrayLength(data);
    dispatchTab.AllocationRead((RsContext)con, (RsAllocation)alloc, ptr, length * sizeof(short));
    _env->ReleaseShortArrayElements(data, ptr, 0);
}

static void
nAllocationRead_b(JNIEnv *_env, jobject _this, jlong con, jlong alloc, jbyteArray data)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocationRead_i, con(%p), alloc(%p), len(%i)", (RsContext)con,
            (RsAllocation)alloc, len);
    jbyte *ptr = _env->GetByteArrayElements(data, NULL);
    jsize length = _env->GetArrayLength(data);
    dispatchTab.AllocationRead((RsContext)con, (RsAllocation)alloc, ptr, length * sizeof(char));
    _env->ReleaseByteArrayElements(data, ptr, 0);
}

static void
nAllocationRead_f(JNIEnv *_env, jobject _this, jlong con, jlong alloc, jfloatArray data)
{
    jint len = _env->GetArrayLength(data);
    LOG_API("nAllocationRead_f, con(%p), alloc(%p), len(%i)", (RsContext)con,
            (RsAllocation)alloc, len);
    jfloat *ptr = _env->GetFloatArrayElements(data, NULL);
    jsize length = _env->GetArrayLength(data);
    dispatchTab.AllocationRead((RsContext)con, (RsAllocation)alloc, ptr, length * sizeof(float));
    _env->ReleaseFloatArrayElements(data, ptr, 0);
}

static jlong
nAllocationGetType(JNIEnv *_env, jobject _this, jlong con, jlong a)
{
    LOG_API("nAllocationGetType, con(%p), a(%p)", (RsContext)con, (RsAllocation)a);
    return (jlong)(uintptr_t) dispatchTab.AllocationGetType((RsContext)con, (RsAllocation)a);
}

static void
nAllocationResize1D(JNIEnv *_env, jobject _this, jlong con, jlong alloc, jint dimX)
{
    LOG_API("nAllocationResize1D, con(%p), alloc(%p), sizeX(%i)", (RsContext)con,
            (RsAllocation)alloc, dimX);
    dispatchTab.AllocationResize1D((RsContext)con, (RsAllocation)alloc, dimX);
}

// -----------------------------------

static void
nScriptBindAllocation(JNIEnv *_env, jobject _this, jlong con, jlong script, jlong alloc, jint slot, jboolean mUseInc)
{
    LOG_API("nScriptBindAllocation, con(%p), script(%p), alloc(%p), slot(%i)",
            (RsContext)con, (RsScript)script, (RsAllocation)alloc, slot);
    if (mUseInc) {
        dispatchTabInc.ScriptBindAllocation((RsContext)con, (RsScript)script, (RsAllocation)alloc, slot);
    } else {
        dispatchTab.ScriptBindAllocation((RsContext)con, (RsScript)script, (RsAllocation)alloc, slot);
    }
}

static void
nScriptSetVarI(JNIEnv *_env, jobject _this, jlong con, jlong script, jint slot, jint val, jboolean mUseInc)
{
    LOG_API("nScriptSetVarI, con(%p), s(%p), slot(%i), val(%i)", (RsContext)con,
            (void *)script, slot, val);
    if (mUseInc) {
        dispatchTabInc.ScriptSetVarI((RsContext)con, (RsScript)script, slot, val);
    } else {
        dispatchTab.ScriptSetVarI((RsContext)con, (RsScript)script, slot, val);
    }
}

static void
nScriptSetVarObj(JNIEnv *_env, jobject _this, jlong con, jlong script, jint slot, jlong val, jboolean mUseInc)
{
    LOG_API("nScriptSetVarObj, con(%p), s(%p), slot(%i), val(%i)", (RsContext)con,
            (void *)script, slot, val);
    if (mUseInc) {
        dispatchTabInc.ScriptSetVarObj((RsContext)con, (RsScript)script, slot, (RsObjectBase)val);
    } else {
        dispatchTab.ScriptSetVarObj((RsContext)con, (RsScript)script, slot, (RsObjectBase)val);
    }
}

static void
nScriptSetVarJ(JNIEnv *_env, jobject _this, jlong con, jlong script, jint slot, jlong val, jboolean mUseInc)
{
    LOG_API("nScriptSetVarJ, con(%p), s(%p), slot(%i), val(%lli)", (RsContext)con,
            (void *)script, slot, val);
    if (mUseInc) {
        dispatchTabInc.ScriptSetVarJ((RsContext)con, (RsScript)script, slot, val);
    } else {
        dispatchTab.ScriptSetVarJ((RsContext)con, (RsScript)script, slot, val);
    }
}

static void
nScriptSetVarF(JNIEnv *_env, jobject _this, jlong con, jlong script, jint slot, float val, jboolean mUseInc)
{
    LOG_API("nScriptSetVarF, con(%p), s(%p), slot(%i), val(%f)", (RsContext)con,
            (void *)script, slot, val);
    if (mUseInc) {
        dispatchTabInc.ScriptSetVarF((RsContext)con, (RsScript)script, slot, val);
    } else {
        dispatchTab.ScriptSetVarF((RsContext)con, (RsScript)script, slot, val);
    }
}

static void
nScriptSetVarD(JNIEnv *_env, jobject _this, jlong con, jlong script, jint slot, double val, jboolean mUseInc)
{
    LOG_API("nScriptSetVarD, con(%p), s(%p), slot(%i), val(%lf)", (RsContext)con,
            (void *)script, slot, val);
    if (mUseInc) {
        dispatchTabInc.ScriptSetVarD((RsContext)con, (RsScript)script, slot, val);
    } else { 
        dispatchTab.ScriptSetVarD((RsContext)con, (RsScript)script, slot, val);
    }
}

static void
nScriptSetVarV(JNIEnv *_env, jobject _this, jlong con, jlong script, jint slot, jbyteArray data, jboolean mUseInc)
{
    LOG_API("nScriptSetVarV, con(%p), s(%p), slot(%i)", (RsContext)con, (void *)script, slot);
    jint len = _env->GetArrayLength(data);
    jbyte *ptr = _env->GetByteArrayElements(data, NULL);
    if (mUseInc) {
        dispatchTabInc.ScriptSetVarV((RsContext)con, (RsScript)script, slot, ptr, len);
    } else {
        dispatchTab.ScriptSetVarV((RsContext)con, (RsScript)script, slot, ptr, len);
    }
    _env->ReleaseByteArrayElements(data, ptr, JNI_ABORT);
}

static void
nScriptSetVarVE(JNIEnv *_env, jobject _this, jlong con, jlong script, jint slot, jbyteArray data,
                jlong elem, jintArray dims, jboolean mUseInc)
{
    LOG_API("nScriptSetVarVE, con(%p), s(%p), slot(%i)", (RsContext)con, (void *)script, slot);
    jint len = _env->GetArrayLength(data);
    jbyte *ptr = _env->GetByteArrayElements(data, NULL);
    jint dimsLen = _env->GetArrayLength(dims) * sizeof(int);
    jint *dimsPtr = _env->GetIntArrayElements(dims, NULL);
    if (mUseInc) {
        dispatchTabInc.ScriptSetVarVE((RsContext)con, (RsScript)script, slot, ptr, len, (RsElement)elem,
                         (const uint32_t *)dimsPtr, dimsLen);    
    } else {
        dispatchTab.ScriptSetVarVE((RsContext)con, (RsScript)script, slot, ptr, len, (RsElement)elem,
                         (const uint32_t *)dimsPtr, dimsLen);
    }
    _env->ReleaseByteArrayElements(data, ptr, JNI_ABORT);
    _env->ReleaseIntArrayElements(dims, dimsPtr, JNI_ABORT);
}


static void
nScriptSetTimeZone(JNIEnv *_env, jobject _this, jlong con, jlong script, jbyteArray timeZone, jboolean mUseInc)
{
    LOG_API("nScriptCSetTimeZone, con(%p), s(%p), timeZone(%s)", (RsContext)con,
            (void *)script, (const char *)timeZone);

    jint length = _env->GetArrayLength(timeZone);
    jbyte* timeZone_ptr;
    timeZone_ptr = (jbyte *) _env->GetPrimitiveArrayCritical(timeZone, (jboolean *)0);
    if (mUseInc) {
        dispatchTabInc.ScriptSetTimeZone((RsContext)con, (RsScript)script, (const char *)timeZone_ptr, length);
    } else {
        dispatchTab.ScriptSetTimeZone((RsContext)con, (RsScript)script, (const char *)timeZone_ptr, length);
    }

    if (timeZone_ptr) {
        _env->ReleasePrimitiveArrayCritical(timeZone, timeZone_ptr, 0);
    }
}

static void
nScriptInvoke(JNIEnv *_env, jobject _this, jlong con, jlong obj, jint slot, jboolean mUseInc)
{
    LOG_API("nScriptInvoke, con(%p), script(%p)", (RsContext)con, (void *)obj);
    if (mUseInc) {
        dispatchTabInc.ScriptInvoke((RsContext)con, (RsScript)obj, slot);
    } else {
        dispatchTab.ScriptInvoke((RsContext)con, (RsScript)obj, slot);
    }
}

static void
nScriptInvokeV(JNIEnv *_env, jobject _this, jlong con, jlong script, jint slot, jbyteArray data, jboolean mUseInc)
{
    LOG_API("nScriptInvokeV, con(%p), s(%p), slot(%i)", (RsContext)con, (void *)script, slot);
    jint len = _env->GetArrayLength(data);
    jbyte *ptr = _env->GetByteArrayElements(data, NULL);
    if (mUseInc) {
        dispatchTabInc.ScriptInvokeV((RsContext)con, (RsScript)script, slot, ptr, len);
    } else {
        dispatchTab.ScriptInvokeV((RsContext)con, (RsScript)script, slot, ptr, len);
    }
    _env->ReleaseByteArrayElements(data, ptr, JNI_ABORT);
}

static void
nScriptForEach(JNIEnv *_env, jobject _this, jlong con, jlong incCon,
               jlong script, jint slot, jlong ain, jlong aout, jboolean mUseInc)
{
    LOG_API("nScriptForEach, con(%p), s(%p), slot(%i)", (RsContext)con, (void *)script, slot);
    if (mUseInc) {
        dispatchTab.ContextFinish((RsContext)con);
        dispatchTabInc.ScriptForEach((RsContext)incCon, (RsScript)script, slot,
                                  (RsAllocation)ain, (RsAllocation)aout,
                                  NULL, 0, NULL, 0);
        dispatchTabInc.ContextFinish((RsContext)incCon);
    } else {
        dispatchTab.ScriptForEach((RsContext)con, (RsScript)script, slot,
                                  (RsAllocation)ain, (RsAllocation)aout,
                                  NULL, 0, NULL, 0);
    }
}
static void
nScriptForEachV(JNIEnv *_env, jobject _this, jlong con, jlong incCon,
                jlong script, jint slot, jlong ain, jlong aout, jbyteArray params, jboolean mUseInc)
{
    LOG_API("nScriptForEach, con(%p), s(%p), slot(%i)", (RsContext)con, (void *)script, slot);
    jint len = _env->GetArrayLength(params);
    jbyte *ptr = _env->GetByteArrayElements(params, NULL);
    if (mUseInc) {
        dispatchTab.ContextFinish((RsContext)con);
        dispatchTabInc.ScriptForEach((RsContext)incCon, (RsScript)script, slot,
                                  (RsAllocation)ain, (RsAllocation)aout,
                                  ptr, len, NULL, 0);
        dispatchTabInc.ContextFinish((RsContext)incCon);
    } else {
        dispatchTab.ScriptForEach((RsContext)con, (RsScript)script, slot,
                                  (RsAllocation)ain, (RsAllocation)aout,
                                  ptr, len, NULL, 0);
    }
    _env->ReleaseByteArrayElements(params, ptr, JNI_ABORT);
}

static void
nScriptForEachClipped(JNIEnv *_env, jobject _this, jlong con, jlong incCon,
                      jlong script, jint slot, jlong ain, jlong aout,
                      jint xstart, jint xend,
                      jint ystart, jint yend, jint zstart, jint zend, jboolean mUseInc)
{
    LOG_API("nScriptForEachClipped, con(%p), s(%p), slot(%i)", (RsContext)con, (void *)script, slot);
    RsScriptCall sc;
    sc.xStart = xstart;
    sc.xEnd = xend;
    sc.yStart = ystart;
    sc.yEnd = yend;
    sc.zStart = zstart;
    sc.zEnd = zend;
    sc.strategy = RS_FOR_EACH_STRATEGY_DONT_CARE;
    sc.arrayStart = 0;
    sc.arrayEnd = 0;
    if (mUseInc) {
        dispatchTab.ContextFinish((RsContext)con);
        dispatchTabInc.ScriptForEach((RsContext)incCon, (RsScript)script, slot,
                                  (RsAllocation)ain, (RsAllocation)aout,
                                  NULL, 0, &sc, sizeof(sc));
        dispatchTabInc.ContextFinish((RsContext)incCon);
    } else {
        dispatchTab.ScriptForEach((RsContext)con, (RsScript)script, slot,
                                  (RsAllocation)ain, (RsAllocation)aout,
                                  NULL, 0, &sc, sizeof(sc));
    }
}

static void
nScriptForEachClippedV(JNIEnv *_env, jobject _this, jlong con, jlong incCon,
                       jlong script, jint slot, jlong ain, jlong aout,
                       jbyteArray params, jint xstart, jint xend,
                       jint ystart, jint yend, jint zstart, jint zend, jboolean mUseInc)
{
    LOG_API("nScriptForEachClipped, con(%p), s(%p), slot(%i)", (RsContext)con, (void *)script, slot);
    jint len = _env->GetArrayLength(params);
    jbyte *ptr = _env->GetByteArrayElements(params, NULL);
    RsScriptCall sc;
    sc.xStart = xstart;
    sc.xEnd = xend;
    sc.yStart = ystart;
    sc.yEnd = yend;
    sc.zStart = zstart;
    sc.zEnd = zend;
    sc.strategy = RS_FOR_EACH_STRATEGY_DONT_CARE;
    sc.arrayStart = 0;
    sc.arrayEnd = 0;
    if (mUseInc) {
        dispatchTab.ContextFinish((RsContext)con);
        dispatchTabInc.ScriptForEach((RsContext)incCon, (RsScript)script, slot,
                                  (RsAllocation)ain, (RsAllocation)aout,
                                  ptr, len, &sc, sizeof(sc));
        dispatchTabInc.ContextFinish((RsContext)(RsContext)con);
    } else {
        dispatchTab.ScriptForEach((RsContext)con, (RsScript)script, slot,
                                  (RsAllocation)ain, (RsAllocation)aout,
                                  ptr, len, &sc, sizeof(sc));
    }
    _env->ReleaseByteArrayElements(params, ptr, JNI_ABORT);
}

// -----------------------------------

static jlong
nScriptCCreate(JNIEnv *_env, jobject _this, jlong con,
               jstring resName, jstring cacheDir,
               jbyteArray scriptRef, jint length)
{
    LOG_API("nScriptCCreate, con(%p)", (RsContext)con);

    AutoJavaStringToUTF8 resNameUTF(_env, resName);
    AutoJavaStringToUTF8 cacheDirUTF(_env, cacheDir);
    jlong ret = 0;
    jbyte* script_ptr = NULL;
    jint _exception = 0;
    jint remaining;
    if (!scriptRef) {
        _exception = 1;
        //jniThrowException(_env, "java/lang/IllegalArgumentException", "script == null");
        goto exit;
    }
    if (length < 0) {
        _exception = 1;
        //jniThrowException(_env, "java/lang/IllegalArgumentException", "length < 0");
        goto exit;
    }
    remaining = _env->GetArrayLength(scriptRef);
    if (remaining < length) {
        _exception = 1;
        //jniThrowException(_env, "java/lang/IllegalArgumentException",
        //        "length > script.length - offset");
        goto exit;
    }
    script_ptr = (jbyte *)
        _env->GetPrimitiveArrayCritical(scriptRef, (jboolean *)0);

    //rsScriptCSetText(con, (const char *)script_ptr, length);

    ret = (jlong)(uintptr_t)dispatchTab.ScriptCCreate((RsContext)con,
                                                          resNameUTF.c_str(), resNameUTF.length(),
                                                          cacheDirUTF.c_str(), cacheDirUTF.length(),
                                                          (const char *)script_ptr, length);

exit:
    if (script_ptr) {
        _env->ReleasePrimitiveArrayCritical(scriptRef, script_ptr,
                _exception ? JNI_ABORT: 0);
    }

    return (jlong)(uintptr_t)ret;
}

static jlong
nScriptIntrinsicCreate(JNIEnv *_env, jobject _this, jlong con, jint id, jlong eid, jboolean mUseInc)
{
    LOG_API("nScriptIntrinsicCreate, con(%p) id(%i) element(%p)", (RsContext)con, id, (void *)eid);
    if (mUseInc) {
        return (jlong)(uintptr_t)dispatchTabInc.ScriptIntrinsicCreate((RsContext)con, id, (RsElement)eid);
    } else {
        return (jlong)(uintptr_t)dispatchTab.ScriptIntrinsicCreate((RsContext)con, id, (RsElement)eid);
    }
}

static jlong
nScriptKernelIDCreate(JNIEnv *_env, jobject _this, jlong con, jlong sid, jint slot, jint sig, jboolean mUseInc)
{
    LOG_API("nScriptKernelIDCreate, con(%p) script(%p), slot(%i), sig(%i)", (RsContext)con,
            (void *)sid, slot, sig);
    if (mUseInc) {
        return (jlong)(uintptr_t)dispatchTabInc.ScriptKernelIDCreate((RsContext)con, (RsScript)sid,
                                                                         slot, sig);    
    } else {
        return (jlong)(uintptr_t)dispatchTab.ScriptKernelIDCreate((RsContext)con, (RsScript)sid,
                                                                         slot, sig);
    }
}

static jlong
nScriptFieldIDCreate(JNIEnv *_env, jobject _this, jlong con, jlong sid, jint slot, jboolean mUseInc)
{
    LOG_API("nScriptFieldIDCreate, con(%p) script(%p), slot(%i)", (RsContext)con, (void *)sid, slot);
    if (mUseInc) {
        return (jlong)(uintptr_t)dispatchTab.ScriptFieldIDCreate((RsContext)con, (RsScript)sid, slot);
    } else {
        return (jlong)(uintptr_t)dispatchTab.ScriptFieldIDCreate((RsContext)con, (RsScript)sid, slot);
    }
}

static jlong
nScriptGroupCreate(JNIEnv *_env, jobject _this, jlong con, jlongArray _kernels, jlongArray _src,
    jlongArray _dstk, jlongArray _dstf, jlongArray _types)
{
    LOG_API("nScriptGroupCreate, con(%p)", (RsContext)con);

    jint kernelsLen = _env->GetArrayLength(_kernels);
    jlong *jKernelsPtr = _env->GetLongArrayElements(_kernels, nullptr);
    RsScriptKernelID* kernelsPtr = (RsScriptKernelID*) malloc(sizeof(RsScriptKernelID) * kernelsLen);
    for(int i = 0; i < kernelsLen; ++i) {
        kernelsPtr[i] = (RsScriptKernelID)jKernelsPtr[i];
    }

    jint srcLen = _env->GetArrayLength(_src);
    jlong *jSrcPtr = _env->GetLongArrayElements(_src, nullptr);
    RsScriptKernelID* srcPtr = (RsScriptKernelID*) malloc(sizeof(RsScriptKernelID) * srcLen);
    for(int i = 0; i < srcLen; ++i) {
        srcPtr[i] = (RsScriptKernelID)jSrcPtr[i];
    }

    jint dstkLen = _env->GetArrayLength(_dstk);
    jlong *jDstkPtr = _env->GetLongArrayElements(_dstk, nullptr);
    RsScriptKernelID* dstkPtr = (RsScriptKernelID*) malloc(sizeof(RsScriptKernelID) * dstkLen);
    for(int i = 0; i < dstkLen; ++i) {
        dstkPtr[i] = (RsScriptKernelID)jDstkPtr[i];
    }

    jint dstfLen = _env->GetArrayLength(_dstf);
    jlong *jDstfPtr = _env->GetLongArrayElements(_dstf, nullptr);
    RsScriptKernelID* dstfPtr = (RsScriptKernelID*) malloc(sizeof(RsScriptKernelID) * dstfLen);
    for(int i = 0; i < dstfLen; ++i) {
        dstfPtr[i] = (RsScriptKernelID)jDstfPtr[i];
    }

    jint typesLen = _env->GetArrayLength(_types);
    jlong *jTypesPtr = _env->GetLongArrayElements(_types, nullptr);
    RsType* typesPtr = (RsType*) malloc(sizeof(RsType) * typesLen);
    for(int i = 0; i < typesLen; ++i) {
        typesPtr[i] = (RsType)jTypesPtr[i];
    }

    jlong id = (jlong)(uintptr_t) dispatchTab.ScriptGroupCreate((RsContext)con,
                               (RsScriptKernelID *)kernelsPtr, kernelsLen * sizeof(RsScriptKernelID),
                               (RsScriptKernelID *)srcPtr, srcLen * sizeof(RsScriptKernelID),
                               (RsScriptKernelID *)dstkPtr, dstkLen * sizeof(RsScriptKernelID),
                               (RsScriptFieldID *)dstfPtr, dstfLen * sizeof(RsScriptKernelID),
                               (RsType *)typesPtr, typesLen * sizeof(RsType));

    free(kernelsPtr);
    free(srcPtr);
    free(dstkPtr);
    free(dstfPtr);
    free(typesPtr);
    _env->ReleaseLongArrayElements(_kernels, jKernelsPtr, 0);
    _env->ReleaseLongArrayElements(_src, jSrcPtr, 0);
    _env->ReleaseLongArrayElements(_dstk, jDstkPtr, 0);
    _env->ReleaseLongArrayElements(_dstf, jDstfPtr, 0);
    _env->ReleaseLongArrayElements(_types, jTypesPtr, 0);
    return id;
}

static void
nScriptGroupSetInput(JNIEnv *_env, jobject _this, jlong con, jlong gid, jlong kid, jlong alloc)
{
    LOG_API("nScriptGroupSetInput, con(%p) group(%p), kernelId(%p), alloc(%p)", (RsContext)con,
            (void *)gid, (void *)kid, (void *)alloc);
    dispatchTab.ScriptGroupSetInput((RsContext)con, (RsScriptGroup)gid, (RsScriptKernelID)kid,
                                    (RsAllocation)alloc);
}

static void
nScriptGroupSetOutput(JNIEnv *_env, jobject _this, jlong con, jlong gid, jlong kid, jlong alloc)
{
    LOG_API("nScriptGroupSetOutput, con(%p) group(%p), kernelId(%p), alloc(%p)", (RsContext)con,
            (void *)gid, (void *)kid, (void *)alloc);
    dispatchTab.ScriptGroupSetOutput((RsContext)con, (RsScriptGroup)gid, (RsScriptKernelID)kid,
                                     (RsAllocation)alloc);
}

static void
nScriptGroupExecute(JNIEnv *_env, jobject _this, jlong con, jlong gid)
{
    LOG_API("nScriptGroupSetOutput, con(%p) group(%p)", (RsContext)con, (void *)gid);
    dispatchTab.ScriptGroupExecute((RsContext)con, (RsScriptGroup)gid);
}

// ---------------------------------------------------------------------------

static jlong
nSamplerCreate(JNIEnv *_env, jobject _this, jlong con, jint magFilter, jint minFilter,
               jint wrapS, jint wrapT, jint wrapR, jfloat aniso)
{
    LOG_API("nSamplerCreate, con(%p)", (RsContext)con);
    return (jlong)(uintptr_t)dispatchTab.SamplerCreate((RsContext)con,
                                                       (RsSamplerValue)magFilter,
                                                       (RsSamplerValue)minFilter,
                                                       (RsSamplerValue)wrapS,
                                                       (RsSamplerValue)wrapT,
                                                       (RsSamplerValue)wrapR,
                                                       aniso);
}

static jint
nSystemGetPointerSize(JNIEnv *_env, jobject _this) {
    return (jint)sizeof(void*);
}

// ---------------------------------------------------------------------------
// For Incremental Intrinsic Support
static bool nIncLoadSO() {
    void* handle = NULL;
    handle = dlopen("libRSSupport.so", RTLD_LAZY | RTLD_LOCAL);
    if (handle == NULL) {
        LOG_API("couldn't dlopen %s, %s", filename, dlerror());
        return false;
    }

    if (loadSymbols(handle, dispatchTabInc) == false) {
        LOG_API("%s init failed!", filename);
        return false;
    }
    LOG_API("Successfully loaded %s", filename);
    return true;
}

// -----------------------------------
// To create a dummy context
static void
nIncObjDestroy(JNIEnv *_env, jobject _this, jlong con, jlong obj)
{
    LOG_API("nObjDestroy, con(%p) obj(%p)", (RsContext)con, (void *)obj);
    dispatchTabInc.ObjDestroy((RsContext)con, (void *)obj);
}


static jlong
nIncDeviceCreate(JNIEnv *_env, jobject _this)
{
    LOG_API("nDeviceCreate");
    return (jlong)(uintptr_t)dispatchTabInc.DeviceCreate();
}

static void
nIncDeviceDestroy(JNIEnv *_env, jobject _this, jlong dev)
{
    LOG_API("nDeviceDestroy");
    return dispatchTabInc.DeviceDestroy((RsDevice)dev);
}

static jlong
nIncContextCreate(JNIEnv *_env, jobject _this, jlong dev, jint ver, jint sdkVer, jint ct)
{
    LOG_API("nContextCreate");
    return (jlong)(uintptr_t)dispatchTabInc.ContextCreate((RsDevice)dev, ver, sdkVer, (RsContextType)ct, 0);
}

static void
nIncContextFinish(JNIEnv *_env, jobject _this, jlong con)
{
    LOG_API("nContextFinish, con(%p)", (RsContext)con);
    dispatchTabInc.ContextFinish((RsContext)con);
}

static void
nIncContextDestroy(JNIEnv *_env, jobject _this, jlong con)
{
    LOG_API("nContextDestroy, con(%p)", (RsContext)con);
    dispatchTabInc.ContextDestroy((RsContext)con);
}

// -----------------------------------
static jlong
nIncElementCreate(JNIEnv *_env, jobject _this, jlong con, jlong type, jint kind, jboolean norm, jint size)
{
    LOG_API("nElementCreate, con(%p), type(%i), kind(%i), norm(%i), size(%i)", (RsContext)con,
            type, kind, norm, size);
    return (jlong)(uintptr_t)dispatchTabInc.ElementCreate((RsContext)con, (RsDataType)type, (RsDataKind)kind,
                                                       norm, size);
}
// -----------------------------------

static jlong
nIncTypeCreate(JNIEnv *_env, jobject _this, jlong con, jlong eid,
            jint dimx, jint dimy, jint dimz, jboolean mips, jboolean faces, jint yuv)
{
    LOG_API("nTypeCreate, con(%p) eid(%p), x(%i), y(%i), z(%i), mips(%i), faces(%i), yuv(%i)",
            incCon, eid, dimx, dimy, dimz, mips, faces, yuv);

    return (jlong)(uintptr_t)dispatchTabInc.TypeCreate((RsContext)con, (RsElement)eid, dimx, dimy,
                                                    dimz, mips, faces, yuv);
}

// -----------------------------------
// Create Allocation from pointer
static jlong
nIncAllocationCreateTyped(JNIEnv *_env, jobject _this, jlong con, jlong incCon, jlong alloc, jlong type)
{
    LOG_API("nAllocationCreateTyped, con(%p), type(%p), mip(%i), usage(%i), ptr(%p)",
            incCon, (RsElement)type, mips, usage, (void *)pointer);
    size_t strideIn;
    void* pIn = NULL;
    RsAllocation ainI = NULL;
    if (alloc != 0) {
        pIn = dispatchTab.AllocationGetPointer((RsContext)con, (RsAllocation)alloc, 0, (RsAllocationCubemapFace)0, 0, 0, &strideIn, sizeof(size_t));
        ainI = dispatchTabInc.AllocationCreateTyped((RsContext)incCon, (RsType)type,
                                         (RsAllocationMipmapControl)1,
                                         0x0081, (uintptr_t)pIn);
    }
    return (jlong)(uintptr_t) ainI;
}

// ---------------------------------------------------------------------------



static const char *classPathName = "android/support/v8/renderscript/RenderScript";

static JNINativeMethod methods[] = {
{"nLoadSO",                        "(Z)Z",                                    (bool*)nLoadSO },
{"nLoadIOSO",                      "()Z",                                     (bool*)nLoadIOSO },
{"nDeviceCreate",                  "()J",                                     (void*)nDeviceCreate },
{"nDeviceDestroy",                 "(J)V",                                    (void*)nDeviceDestroy },
{"nDeviceSetConfig",               "(JII)V",                                  (void*)nDeviceSetConfig },
{"nContextGetUserMessage",         "(J[I)I",                                  (void*)nContextGetUserMessage },
{"nContextGetErrorMessage",        "(J)Ljava/lang/String;",                   (void*)nContextGetErrorMessage },
{"nContextPeekMessage",            "(J[I)I",                                  (void*)nContextPeekMessage },
{"nContextInitToClient",           "(J)V",                                    (void*)nContextInitToClient },
{"nContextDeinitToClient",         "(J)V",                                    (void*)nContextDeinitToClient },


// All methods below are thread protected in java.
{"rsnContextCreate",                 "(JIII)J",                               (void*)nContextCreate },
{"rsnContextFinish",                 "(J)V",                                  (void*)nContextFinish },
{"rsnContextSetPriority",            "(JI)V",                                 (void*)nContextSetPriority },
{"rsnContextDestroy",                "(J)V",                                  (void*)nContextDestroy },
{"rsnContextDump",                   "(JI)V",                                 (void*)nContextDump },
{"rsnContextSendMessage",            "(JI[I)V",                               (void*)nContextSendMessage },
//{"rsnClosureCreate",                 "(JJJ[J[J[I[J[J)J",                      (void*)nClosureCreate },
//{"rsnClosureSetArg",                 "(JJIJI)V",                              (void*)nClosureSetArg },
//{"rsnClosureSetGlobal",              "(JJJJI)V",                              (void*)nClosureSetGlobal },
{"rsnObjDestroy",                    "(JJ)V",                                 (void*)nObjDestroy },

{"rsnElementCreate",                 "(JJIZI)J",                              (void*)nElementCreate },
{"rsnElementCreate2",                "(J[J[Ljava/lang/String;[I)J",           (void*)nElementCreate2 },
{"rsnElementGetSubElements",         "(JJ[J[Ljava/lang/String;[I)V",          (void*)nElementGetSubElements },

{"rsnTypeCreate",                    "(JJIIIZZI)J",                           (void*)nTypeCreate },

{"rsnAllocationCreateTyped",         "(JJIIJ)J",                              (void*)nAllocationCreateTyped },
{"rsnAllocationCreateFromBitmap",    "(JJILandroid/graphics/Bitmap;I)J",      (void*)nAllocationCreateFromBitmap },
{"rsnAllocationCreateBitmapBackedAllocation",    "(JJILandroid/graphics/Bitmap;I)J",      (void*)nAllocationCreateBitmapBackedAllocation },
{"rsnAllocationCubeCreateFromBitmap","(JJILandroid/graphics/Bitmap;I)J",      (void*)nAllocationCubeCreateFromBitmap },

{"rsnAllocationCopyFromBitmap",      "(JJLandroid/graphics/Bitmap;)V",        (void*)nAllocationCopyFromBitmap },
{"rsnAllocationCopyToBitmap",        "(JJLandroid/graphics/Bitmap;)V",        (void*)nAllocationCopyToBitmap },

{"rsnAllocationSyncAll",             "(JJI)V",                                (void*)nAllocationSyncAll },
{"rsnAllocationSetSurface",          "(JJLandroid/view/Surface;)V",           (void*)nAllocationSetSurface },
{"rsnAllocationIoSend",              "(JJ)V",                                 (void*)nAllocationIoSend },
{"rsnAllocationData1D",              "(JJIII[JI)V",                           (void*)nAllocationData1D_l },
{"rsnAllocationData1D",              "(JJIII[II)V",                           (void*)nAllocationData1D_i },
{"rsnAllocationData1D",              "(JJIII[SI)V",                           (void*)nAllocationData1D_s },
{"rsnAllocationData1D",              "(JJIII[BI)V",                           (void*)nAllocationData1D_b },
{"rsnAllocationData1D",              "(JJIII[FI)V",                           (void*)nAllocationData1D_f },
{"rsnAllocationElementData1D",       "(JJIII[BI)V",                           (void*)nAllocationElementData1D },
{"rsnAllocationData2D",              "(JJIIIIII[JI)V",                        (void*)nAllocationData2D_l },
{"rsnAllocationData2D",              "(JJIIIIII[II)V",                        (void*)nAllocationData2D_i },
{"rsnAllocationData2D",              "(JJIIIIII[SI)V",                        (void*)nAllocationData2D_s },
{"rsnAllocationData2D",              "(JJIIIIII[BI)V",                        (void*)nAllocationData2D_b },
{"rsnAllocationData2D",              "(JJIIIIII[FI)V",                        (void*)nAllocationData2D_f },
{"rsnAllocationData2D",              "(JJIIIIIIJIIII)V",                      (void*)nAllocationData2D_alloc },
{"rsnAllocationData3D",              "(JJIIIIIII[JI)V",                       (void*)nAllocationData3D_l },
{"rsnAllocationData3D",              "(JJIIIIIII[II)V",                       (void*)nAllocationData3D_i },
{"rsnAllocationData3D",              "(JJIIIIIII[SI)V",                       (void*)nAllocationData3D_s },
{"rsnAllocationData3D",              "(JJIIIIIII[BI)V",                       (void*)nAllocationData3D_b },
{"rsnAllocationData3D",              "(JJIIIIIII[FI)V",                       (void*)nAllocationData3D_f },
{"rsnAllocationData3D",              "(JJIIIIIIIJIIII)V",                     (void*)nAllocationData3D_alloc },
{"rsnAllocationRead",                "(JJ[I)V",                               (void*)nAllocationRead_i },
{"rsnAllocationRead",                "(JJ[S)V",                               (void*)nAllocationRead_s },
{"rsnAllocationRead",                "(JJ[B)V",                               (void*)nAllocationRead_b },
{"rsnAllocationRead",                "(JJ[F)V",                               (void*)nAllocationRead_f },
{"rsnAllocationGetType",             "(JJ)J",                                 (void*)nAllocationGetType},
{"rsnAllocationResize1D",            "(JJI)V",                                (void*)nAllocationResize1D },
{"rsnAllocationGenerateMipmaps",     "(JJ)V",                                 (void*)nAllocationGenerateMipmaps },

{"rsnScriptBindAllocation",          "(JJJIZ)V",                              (void*)nScriptBindAllocation },
{"rsnScriptSetTimeZone",             "(JJ[BZ)V",                              (void*)nScriptSetTimeZone },
{"rsnScriptInvoke",                  "(JJIZ)V",                               (void*)nScriptInvoke },
{"rsnScriptInvokeV",                 "(JJI[BZ)V",                             (void*)nScriptInvokeV },
{"rsnScriptForEach",                 "(JJJIJJZ)V",                            (void*)nScriptForEach },
{"rsnScriptForEach",                 "(JJJIJJ[BZ)V",                          (void*)nScriptForEachV },
{"rsnScriptForEachClipped",          "(JJJIJJIIIIIIZ)V",                      (void*)nScriptForEachClipped },
{"rsnScriptForEachClipped",          "(JJJIJJ[BIIIIIIZ)V",                    (void*)nScriptForEachClippedV },
{"rsnScriptSetVarI",                 "(JJIIZ)V",                              (void*)nScriptSetVarI },
{"rsnScriptSetVarJ",                 "(JJIJZ)V",                              (void*)nScriptSetVarJ },
{"rsnScriptSetVarF",                 "(JJIFZ)V",                              (void*)nScriptSetVarF },
{"rsnScriptSetVarD",                 "(JJIDZ)V",                              (void*)nScriptSetVarD },
{"rsnScriptSetVarV",                 "(JJI[BZ)V",                             (void*)nScriptSetVarV },
{"rsnScriptSetVarVE",                "(JJI[BJ[IZ)V",                          (void*)nScriptSetVarVE },
{"rsnScriptSetVarObj",               "(JJIJZ)V",                              (void*)nScriptSetVarObj },

{"rsnScriptCCreate",                 "(JLjava/lang/String;Ljava/lang/String;[BI)J",  (void*)nScriptCCreate },
{"rsnScriptIntrinsicCreate",         "(JIJZ)J",                               (void*)nScriptIntrinsicCreate },
{"rsnScriptKernelIDCreate",          "(JJIIZ)J",                              (void*)nScriptKernelIDCreate },
{"rsnScriptFieldIDCreate",           "(JJIZ)J",                               (void*)nScriptFieldIDCreate },
{"rsnScriptGroupCreate",             "(J[J[J[J[J[J)J",                        (void*)nScriptGroupCreate },
//{"rsnScriptGroup2Create",            "(J[J)J",                                (void*)nScriptGroup2Create },
{"rsnScriptGroupSetInput",           "(JJJJ)V",                               (void*)nScriptGroupSetInput },
{"rsnScriptGroupSetOutput",          "(JJJJ)V",                               (void*)nScriptGroupSetOutput },
{"rsnScriptGroupExecute",            "(JJ)V",                                 (void*)nScriptGroupExecute },

{"rsnSamplerCreate",                 "(JIIIIIF)J",                            (void*)nSamplerCreate },

{"rsnSystemGetPointerSize",          "()I",                                   (void*)nSystemGetPointerSize },

// Entry points for Inc libRSSupport
{"nIncLoadSO",                       "()Z",                                   (bool*)nIncLoadSO },
{"nIncDeviceCreate",                 "()J",                                   (void*)nIncDeviceCreate },
{"nIncDeviceDestroy",                "(J)V",                                  (void*)nIncDeviceDestroy },
{"rsnIncContextCreate",              "(JIII)J",                               (void*)nIncContextCreate },
{"rsnIncContextFinish",              "(J)V",                                  (void*)nIncContextFinish },
{"rsnIncContextDestroy",             "(J)V",                                  (void*)nIncContextDestroy },
{"rsnIncObjDestroy",                 "(JJ)V",                                 (void*)nIncObjDestroy },
{"rsnIncElementCreate",              "(JJIZI)J",                              (void*)nIncElementCreate },
{"rsnIncTypeCreate",                 "(JJIIIZZI)J",                           (void*)nIncTypeCreate },
{"rsnIncAllocationCreateTyped",      "(JJJJ)J",                               (void*)nIncAllocationCreateTyped },
};

// ---------------------------------------------------------------------------

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jclass clazz = NULL;
    jint result = -1;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        //        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG,
        //            "ERROR: GetEnv failed\n");
        goto bail;
    }
    if (env == NULL) {
        //        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "ERROR: env == NULL");
        goto bail;
    }

    clazz = env->FindClass(classPathName);
    if (clazz == NULL) {
        goto bail;
    }

    if (env->RegisterNatives(clazz, methods, NELEM(methods)) < 0) {
        //        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG,
        //            "ERROR: MediaPlayer native registration failed\n");
        goto bail;
    }

    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

bail:
    return result;
}
